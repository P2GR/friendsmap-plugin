/*
 * Copyright (c) 2026, P2GR
 * All rights reserved.
 */
package com.friendsmap.overlays;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import com.friendsmap.FriendsMapConfig;
import com.friendsmap.FriendsMapPlugin;
import com.friendsmap.model.FriendLocation;
import com.friendsmap.services.AreaLookup;
import com.friendsmap.util.WorldPoints;

/**
 * Minimal debug overlay: server connection state + displayed friends
 * grouped by relation. Enabled via Advanced > Debug.
 */
public class FriendsMapDebugOverlay extends Overlay
{
	private static final int MAX_LINES_PER_SECTION = 25;

	private final PanelComponent panelComponent = new PanelComponent();

	private final FriendsMapConfig config;
	private final FriendsMapPlugin plugin;
	private final Client client;
	private final AreaLookup areaLookup;

	@Inject
	public FriendsMapDebugOverlay(FriendsMapConfig config, FriendsMapPlugin plugin, Client client, AreaLookup areaLookup)
	{
		this.config = config;
		this.plugin = plugin;
		this.client = client;
		this.areaLookup = areaLookup;
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.debug())
		{
			return null;
		}

		panelComponent.getChildren().clear();
		panelComponent.setPreferredSize(new Dimension(320, 0));

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("FriendsMap Debug")
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Server Connection")
			.right(serverStatus())
			.build());
		addCurationLines();

		List<FriendLocation> friends = new ArrayList<>();
		List<FriendLocation> clan = new ArrayList<>();
		List<FriendLocation> friendsChat = new ArrayList<>();
		for (FriendLocation friend : plugin.getCurrentFriends())
		{
			switch (friend.getRelation())
			{
				case CLAN:
					clan.add(friend);
					break;
				case FRIENDS_CHAT:
					friendsChat.add(friend);
					break;
				default:
					friends.add(friend);
			}
		}

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Displayed (" + plugin.getCurrentFriends().size() + ")")
			.build());
		addSection("Friends (" + friends.size() + ")", friends);
		addSection("Clan (" + clan.size() + ")", clan);
		addSection("Friends Chat (" + friendsChat.size() + ")", friendsChat);

		return panelComponent.render(graphics);
	}

	private String serverStatus()
	{
		return plugin.isBackendOnline() ? "Live" : "Offline";
	}

	/**
	 * Curation readout for areas.json: the location in static map space with its
	 * region id and matched area, so new areas can be authored by standing in
	 * them. Inside an instance the raw placement copy is listed as well.
	 */
	private void addCurationLines()
	{
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return;
		}
		WorldPoint raw = localPlayer.getWorldLocation();
		if (raw == null)
		{
			return;
		}

		WorldPoint real = WorldPoints.realLocation(client, localPlayer);
		panelComponent.getChildren().add(LineComponent.builder()
			.left("You")
			.right(describe(real != null ? real : raw))
			.build());
		if (WorldPoints.inInstance(client, localPlayer))
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Raw (instance)")
				.right(describe(raw))
				.build());
		}
	}

	private String describe(WorldPoint point)
	{
		String area = areaLookup.nameFor(point);
		return point.getX() + "," + point.getY() + "," + point.getPlane()
			+ " r" + point.getRegionID()
			+ (area == null ? "" : " [" + area + "]");
	}

	private void addSection(String title, List<FriendLocation> friends)
	{
		panelComponent.getChildren().add(TitleComponent.builder()
			.text(title)
			.build());
		int count = 0;
		for (FriendLocation friend : friends)
		{
			if (count++ >= MAX_LINES_PER_SECTION)
			{
				panelComponent.getChildren().add(LineComponent.builder()
					.left("+ " + (friends.size() - MAX_LINES_PER_SECTION) + " more")
					.build());
				return;
			}
			panelComponent.getChildren().add(LineComponent.builder()
				.left(friend.getName())
				.right("W" + friend.getWorld())
				.build());
		}
	}
}
