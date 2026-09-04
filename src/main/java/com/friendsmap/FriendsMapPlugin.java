/*
 * Copyright (c) 2026, P2GR
 * All rights reserved.
 */
package com.friendsmap;

import com.google.inject.Injector;
import com.google.inject.Provides;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.WorldType;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.overlay.OverlayManager;
import com.friendsmap.model.FriendLocation;
import com.friendsmap.model.HeartbeatPayload;
import com.friendsmap.model.HeartbeatResponse;
import com.friendsmap.model.PositionPayload;
import com.friendsmap.model.Relation;
import com.friendsmap.model.RosterEntry;
import com.friendsmap.model.RosterSnapshot;
import com.friendsmap.model.TogglesPayload;
import com.friendsmap.model.VisibleFriend;
import com.friendsmap.overlays.FriendsMapDebugOverlay;
import com.friendsmap.services.FriendDataCollector;
import com.friendsmap.services.FriendsMapClient;
import com.friendsmap.services.FriendsMapClient.HealthProbe;
import com.friendsmap.services.FriendsMapClient.HeartbeatResult;
import com.friendsmap.services.MapPointService;
import com.friendsmap.services.SimulatedLocationProvider;

@Slf4j
@PluginDescriptor(
	name = "Friends Map",
	description = "Shows friends, clan members and friends-chat members on the world map and minimap.",
	tags = {"friends", "map", "minimap", "tracking", "clan"}
)
public class FriendsMapPlugin extends Plugin
{
	private static final String LOG_CATEGORY = "friendsmap";
	private static final String INTERNAL_TOKEN_KEY = "internalToken";
	private static final String CONSENT_KEY = "dataConsentShown";
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

	/** Poll every 4 game ticks (~2 seconds). Locked. */
	private static final int POLL_TICKS = 4;

	/** Keep the last known position of an offline friend for this long. */
	private static final Duration OFFLINE_HOLD = Duration.ofSeconds(30);

	@Inject
	private Client client;

	@Inject
	private FriendsMapConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private Injector injector;

	@Inject
	private PluginManager pluginManager;

	@Inject
	private ScheduledExecutorService executor;

	/** Snapshot of visible friends. Single source of truth for all renderers. */
	private final List<FriendLocation> currentFriends = new CopyOnWriteArrayList<>();

	private SimulatedLocationProvider simulatedProvider;
	private FriendsMapClient friendsMapClient;
	private FriendDataCollector collector;
	private MapPointService mapPointService;
	private FriendsMapDebugOverlay debugOverlay;
	private int tickCounter;
	private volatile boolean backendOnline;
	private volatile boolean healthCheckInFlight;
	private volatile boolean livePollInFlight;
	private volatile boolean liveResultReady;
	private volatile List<FriendLocation> pendingLiveFriends = Collections.emptyList();
	private volatile String lastHealthStatus = "-";
	private volatile String lastHealthBody = "-";
	private volatile String lastRequestLog = "-";
	private volatile String lastResponseLog = "-";
	private String modeLabel = "LIVE";
	private String internalToken = "";
	private volatile boolean consentGranted;
	private volatile boolean consentPending;

	/** Offline friends: keep last known position, faded, for OFFLINE_HOLD. Client thread only. */
	private final Map<String, OfflineHold> offlineHolds = new HashMap<>();

	@Provides
	FriendsMapConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FriendsMapConfig.class);
	}

	@Override
	protected void startUp()
	{
		consentGranted = false;
		consentPending = false;

		String consent = configManager.getConfiguration(FriendsMapConfig.GROUP, CONSENT_KEY);
		if ("true".equals(consent))
		{
			consentGranted = true;
		}
		else if ("false".equals(consent))
		{
			// Previously denied: stay disabled.
			pluginManager.setPluginEnabled(this, false);
			return;
		}
		else
		{
			consentPending = true;
			SwingUtilities.invokeLater(this::showDataConsentDialog);
		}

		internalToken = configManager.getConfiguration(FriendsMapConfig.GROUP, INTERNAL_TOKEN_KEY);
		if (internalToken == null)
		{
			internalToken = "";
		}

		if (configManager.getConfiguration(FriendsMapConfig.GROUP, CONSENT_KEY) == null)
		{
			SwingUtilities.invokeLater(this::showDataConsentDialog);
		}

		simulatedProvider = new SimulatedLocationProvider();
		friendsMapClient = new FriendsMapClient();
		collector = injector.getInstance(FriendDataCollector.class);
		mapPointService = injector.getInstance(MapPointService.class);

		debugOverlay = injector.getInstance(FriendsMapDebugOverlay.class);
		overlayManager.add(debugOverlay);

		log.info("FriendsMap started (simulate={})", config.simulateFriends());
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(debugOverlay);
		mapPointService.clear();
		currentFriends.clear();
		log.info("FriendsMap stopped");
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		// Hard gate: without granted consent the plugin must not run.
		if (!consentGranted)
		{
			if (!consentPending)
			{
				pluginManager.setPluginEnabled(this, false);
			}
			return;
		}

		// Single source of truth for simulation pacing.
		if (simulationActive())
		{
			simulatedProvider.advanceTick();
		}

		if (tickCounter++ % POLL_TICKS != 0)
		{
			return;
		}

		if (config.simulateFriends())
		{
			modeLabel = "SIMULATED (manual)";
			publish(simulatedProvider.getFriends());
			return;
		}

		if (config.simulateWhenOffline())
		{
			if (backendOnline)
			{
				modeLabel = "LIVE";
				maybeSubmitLivePoll();
				if (liveResultReady)
				{
					liveResultReady = false;
					publishLive(pendingLiveFriends);
				}
			}
			else
			{
				modeLabel = "SIMULATED (server offline)";
				maybeSubmitHealthCheck();
				publish(simulatedProvider.getFriends());
			}
		}
		else
		{
			modeLabel = "LIVE";
			maybeSubmitLivePoll();
			if (liveResultReady)
			{
				liveResultReady = false;
				publishLive(pendingLiveFriends);
			}
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals(FriendsMapConfig.GROUP))
		{
			return;
		}

		if ("sendLocationWilderness".equals(event.getKey()) && "true".equals(event.getNewValue()))
		{
			SwingUtilities.invokeLater(() ->
			{
				int result = JOptionPane.showConfirmDialog(null,
					"Are you really sure you want to send your wilderness location?",
					"Friends Map", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if (result != JOptionPane.YES_OPTION)
				{
					configManager.setConfiguration(FriendsMapConfig.GROUP, "sendLocationWilderness", false);
				}
			});
		}
		else if ("sendLocationPvpWorlds".equals(event.getKey()) && "true".equals(event.getNewValue()))
		{
			SwingUtilities.invokeLater(() ->
			{
				int result = JOptionPane.showConfirmDialog(null,
					"Are you really sure you want to send your location on PvP worlds?",
					"Friends Map", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if (result != JOptionPane.YES_OPTION)
				{
					configManager.setConfiguration(FriendsMapConfig.GROUP, "sendLocationPvpWorlds", false);
				}
			});
		}

		// Display changes (colors, size, toggles) apply immediately.
		mapPointService.synchronize(currentFriends, config.showOnWorldMap());

		if (!config.simulateFriends() && !config.simulateWhenOffline())
		{
			publish(Collections.emptyList());
		}
	}

	public List<FriendLocation> getCurrentFriends()
	{
		return currentFriends;
	}

	public String getModeLabel()
	{
		return modeLabel;
	}

	public String getLastHealthStatus()
	{
		return lastHealthStatus;
	}

	public String getLastHealthBody()
	{
		return lastHealthBody;
	}

	public String getLastRequestLog()
	{
		return lastRequestLog;
	}

	public String getLastResponseLog()
	{
		return lastResponseLog;
	}

	/** Push a new snapshot through every renderer. */
	private void publish(List<FriendLocation> friends)
	{
		currentFriends.clear();
		currentFriends.addAll(friends);
		mapPointService.synchronize(currentFriends, config.showOnWorldMap());
	}

	/**
	 * Live pipeline publish with offline retention: friends that disappear
	 * (or report world 0) keep their last position, faded, for OFFLINE_HOLD.
	 */
	private void publishLive(List<FriendLocation> incoming)
	{
		Instant now = Instant.now();
		Map<String, FriendLocation> online = new HashMap<>();

		for (FriendLocation friend : incoming)
		{
			if (friend.getWorld() == 0)
			{
				offlineHolds.putIfAbsent(friend.getName(), new OfflineHold(friend, now.plus(OFFLINE_HOLD)));
				continue;
			}
			online.put(friend.getName(), friend);
			offlineHolds.remove(friend.getName());
		}

		// Friends no longer in the response keep their last known position.
		for (FriendLocation friend : currentFriends)
		{
			if (!online.containsKey(friend.getName()))
			{
				offlineHolds.putIfAbsent(friend.getName(), new OfflineHold(
					new FriendLocation(friend.getName(), 0, friend.getLocation(), friend.getRelation(), friend.getLastSeen()),
					now.plus(OFFLINE_HOLD)));
			}
		}

		offlineHolds.entrySet().removeIf(entry -> entry.getValue().until.isBefore(now));

		List<FriendLocation> display = new ArrayList<>(online.values());
		for (OfflineHold hold : offlineHolds.values())
		{
			display.add(hold.location);
		}
		publish(display);
	}

	private int pollTicks()
	{
		return POLL_TICKS;
	}

	/** True when simulated data is the active display source this tick. */
	private boolean simulationActive()
	{
		return config.simulateFriends()
			|| (config.simulateWhenOffline() && !backendOnline);
	}

	/**
	 * Build the heartbeat payload on the client thread, then send it on the
	 * executor. Registers on the backend first if no token is stored yet.
	 * Visible friends come back in the HTTP response (pull model).
	 */
	private void maybeSubmitLivePoll()
	{
		if (livePollInFlight)
		{
			return;
		}

		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return;
		}
		WorldPoint location = localPlayer.getWorldLocation();
		if (location == null)
		{
			return;
		}

		String username = localPlayer.getName();
		int world = client.getWorld();
		RosterSnapshot roster = collector.collect();

		HeartbeatPayload payload = new HeartbeatPayload();
		payload.username = username;
		payload.world = world;
		payload.position = new PositionPayload();
		payload.position.x = location.getX();
		payload.position.y = location.getY();
		payload.position.plane = location.getPlane();
		payload.position.regionId = location.getRegionID();
		payload.instance = false;
		payload.clan = roster.getClanName();
		payload.friendsChat = roster.getFriendsChatName();
		payload.friends = toNames(roster.getFriends());
		payload.toggles = new TogglesPayload();
		payload.toggles.visibleToClan = config.visibilityClan();
		payload.toggles.visibleToFriends = config.visibilityFriends();
		payload.toggles.visibleToFriendsChat = config.visibilityFriendsChat();
		payload.toggles.showAlways = false;

		// Keep heartbeats flowing so we still receive friends, but never share
		// our own position from wilderness/PvP unless explicitly enabled.
		if (!canSendLocation())
		{
			payload.position = null;
			logNet("location hidden: wilderness/PvP toggle off");
		}

		String token = internalToken;
		livePollInFlight = true;

		executor.submit(() ->
		{
			String timestamp = LocalTime.now().format(TIME_FORMAT);
			String sessionToken = token;
			try
			{
				if (sessionToken == null || sessionToken.isEmpty())
				{
					lastRequestLog = timestamp + " | POST " + FriendsMapClient.BASE_URL + "/api/v1/register {username:" + username + ",world:" + world + "}";
					logNet("request sent: POST /api/v1/register");
					sessionToken = friendsMapClient.register(username, world);
					lastResponseLog = timestamp + " | register -> token " + (sessionToken == null ? "FAILED" : "received");
					logNet("register response: token " + (sessionToken == null ? "FAILED" : "received"));
					if (sessionToken == null)
					{
						backendOnline = false;
						return;
					}
					internalToken = sessionToken;
					configManager.setConfiguration(FriendsMapConfig.GROUP, INTERNAL_TOKEN_KEY, sessionToken);
				}

				lastRequestLog = timestamp + " | POST " + FriendsMapClient.BASE_URL + "/api/v1/heartbeat (friends:" + payload.friends.size()
					+ ", clan:" + payload.clan + ", fc:" + payload.friendsChat + ")";
				logNet("request sent: POST /api/v1/heartbeat");
				HeartbeatResult result = friendsMapClient.heartbeat(payload, sessionToken);
				lastResponseLog = timestamp + " | HTTP " + result.getStatusCode() + " | " + result.getRawBody();
				logNet("heartbeat response: HTTP " + result.getStatusCode() + " " + result.getRawBody());

				if (result.isSuccess())
				{
					pendingLiveFriends = toFriendLocations(result.getResponse());
					liveResultReady = true;
				}
				else if (result.getStatusCode() == 401 || result.getStatusCode() == 403)
				{
					// Stale/invalid token: drop it and re-register next poll.
					internalToken = "";
					configManager.setConfiguration(FriendsMapConfig.GROUP, INTERNAL_TOKEN_KEY, "");
				}
				else
				{
					backendOnline = false;
				}
			}
			catch (Exception e)
			{
				backendOnline = false;
				lastResponseLog = timestamp + " | ERROR | " + e.getMessage();
				log.warn("{}: live poll failed", LOG_CATEGORY, e);
			}
			finally
			{
				livePollInFlight = false;
			}
		});
	}

	/** Probe backend health off the client thread; result feeds the fallback. */
	private void maybeSubmitHealthCheck()
	{
		if (healthCheckInFlight)
		{
			return;
		}
		healthCheckInFlight = true;
		executor.submit(() ->
		{
			String timestamp = LocalTime.now().format(TIME_FORMAT);
			String requestLine = "GET " + FriendsMapClient.BASE_URL + "/api/v1/health";
			lastRequestLog = timestamp + " | " + requestLine;
			logNet("request sent: GET /api/v1/health");
			try
			{
				HealthProbe probe = friendsMapClient.probe();
				backendOnline = probe.isReachable();
				lastHealthStatus = probe.isReachable() ? "reachable (200)" : "unreachable (" + probe.getStatusCode() + ")";
				lastHealthBody = probe.getBody();
				lastResponseLog = timestamp + " | HTTP " + probe.getStatusCode() + " | " + probe.getBody();
				logNet("health response: HTTP " + probe.getStatusCode() + " " + probe.getBody());
			}
			catch (Exception e)
			{
				backendOnline = false;
				lastHealthStatus = "error";
				lastHealthBody = e.getMessage();
				lastResponseLog = timestamp + " | ERROR | " + e.getMessage();
				log.warn("{}: health check failed", LOG_CATEGORY, e);
			}
			finally
			{
				healthCheckInFlight = false;
			}
		});
	}

	private void logNet(String message)
	{
		if (config.debug())
		{
			log.debug("{}: {}", LOG_CATEGORY + "/net", message);
		}
	}

	/** One-time consent shown on first enable. Decline disables the plugin. */
	private void showDataConsentDialog()
	{
		try
		{
			int result = JOptionPane.showConfirmDialog(null,
				"This plugin submits your RSN, player location and friends/clan data to a server not controlled or verified by the RuneLite developers.\n\nDo you want to continue?",
				"Friends Map", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (result == JOptionPane.YES_OPTION)
			{
				configManager.setConfiguration(FriendsMapConfig.GROUP, CONSENT_KEY, "true");
				consentGranted = true;
			}
			else
			{
				configManager.setConfiguration(FriendsMapConfig.GROUP, CONSENT_KEY, "false");
				pluginManager.setPluginEnabled(this, false);
			}
		}
		finally
		{
			consentPending = false;
		}
	}

	private static List<String> toNames(List<RosterEntry> entries)
	{
		List<String> names = new ArrayList<>(entries.size());
		for (RosterEntry entry : entries)
		{
			names.add(entry.getName());
		}
		return names;
	}

	private static List<FriendLocation> toFriendLocations(HeartbeatResponse response)
	{
		if (response == null || response.visible == null)
		{
			return Collections.emptyList();
		}
		List<FriendLocation> friends = new ArrayList<>(response.visible.size());
		for (VisibleFriend visible : response.visible)
		{
			if (visible == null || visible.position == null)
			{
				continue;
			}
			WorldPoint point = new WorldPoint(visible.position.x, visible.position.y, visible.position.plane);
			friends.add(new FriendLocation(
				visible.username,
				visible.world,
				point,
				relationFrom(visible.relation),
				Instant.ofEpochSecond(visible.lastSeen)));
		}
		return friends;
	}

	private static Relation relationFrom(String relation)
	{
		if ("clan".equals(relation))
		{
			return Relation.CLAN;
		}
		if ("friendsChat".equals(relation))
		{
			return Relation.FRIENDS_CHAT;
		}
		return Relation.FRIEND;
	}

	private boolean canSendLocation()
	{
		if (!config.sendLocationWilderness() && client.getVarbitValue(VarbitID.INSIDE_WILDERNESS) == 1)
		{
			return false;
		}
		if (!config.sendLocationPvpWorlds() && WorldType.isPvpWorld(client.getWorldType()))
		{
			return false;
		}
		return true;
	}

	private static final class OfflineHold
	{
		private final FriendLocation location;
		private final Instant until;

		private OfflineHold(FriendLocation location, Instant until)
		{
			this.location = location;
			this.until = until;
		}
	}
}
