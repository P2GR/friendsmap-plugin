/*
 * Copyright (c) 2026, P2GR
 * All rights reserved.
 */
package com.friendsmap.services;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Point;
import com.friendsmap.FriendsMapConfig;
import com.friendsmap.model.FriendLocation;
import com.friendsmap.util.FriendIconFactory;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

/**
 * Owns the lifecycle of world map points for visible friends.
 *
 * <p>Keeps a name → point registry, adds/updates/removes points so the
 * core {@code WorldMapOverlay} renders them automatically when the map is open.</p>
 */
public class MapPointService
{
	private final WorldMapPointManager worldMapPointManager;
	private final FriendsMapConfig config;
	private final Map<String, WorldMapPoint> pointsByName = new HashMap<>();
	private final Map<String, Integer> worldsByName = new HashMap<>();

	@Inject
	public MapPointService(WorldMapPointManager worldMapPointManager, FriendsMapConfig config)
	{
		this.worldMapPointManager = worldMapPointManager;
		this.config = config;
	}

	/**
	 * Reconcile the point set with the given snapshot.
	 * Only surface-plane friends become points.
	 */
	public synchronized void synchronize(Collection<FriendLocation> friends, boolean worldMapEnabled)
	{
		Set<String> seen = new HashSet<>();
		for (FriendLocation friend : friends)
		{
			seen.add(friend.getName());

			if (!worldMapEnabled || !friend.isOnWorldMap())
			{
				continue;
			}

			WorldMapPoint existing = pointsByName.get(friend.getName());
			Integer knownWorld = worldsByName.get(friend.getName());
			if (existing != null && existing.getWorldPoint() != null
				&& existing.getWorldPoint().equals(friend.getLocation())
				&& knownWorld != null && knownWorld == friend.getWorld())
			{
				continue;
			}

			if (existing != null)
			{
				worldMapPointManager.remove(existing);
			}

			String displayName = config.showWorldMapNames() ? friend.getName() : null;
			WorldMapPoint point = new WorldMapPoint(friend.getLocation(), FriendIconFactory.worldMapDot(friend.getRelation(), config, displayName, friend.getWorld() == 0));
			if (displayName != null)
			{
				// Anchor the dot's center on the world point; the name extends right.
				point.setImagePoint(new Point(config.dotSize() / 2, point.getImage().getHeight() / 2));
			}
			point.setTooltip(buildTooltip(friend));
			point.setName(friend.getName());
			point.setSnapToEdge(true);
			point.setJumpOnClick(true);
			worldMapPointManager.add(point);
			pointsByName.put(friend.getName(), point);
			worldsByName.put(friend.getName(), friend.getWorld());
		}

		Iterator<Map.Entry<String, WorldMapPoint>> it = pointsByName.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry<String, WorldMapPoint> entry = it.next();
			if (!seen.contains(entry.getKey()))
			{
				worldMapPointManager.remove(entry.getValue());
				worldsByName.remove(entry.getKey());
				it.remove();
			}
		}
	}

	public synchronized void clear()
	{
		for (WorldMapPoint point : pointsByName.values())
		{
			worldMapPointManager.remove(point);
		}
		pointsByName.clear();
		worldsByName.clear();
	}

	private String buildTooltip(FriendLocation friend)
	{
		long ageSeconds = Duration.between(friend.getLastSeen(), Instant.now()).getSeconds();
		String header = friend.getWorld() == 0
			? friend.getName() + " (offline)"
			: friend.getName() + " (World " + friend.getWorld() + ")";
		return header
			+ "<br>" + friend.getRelation().getLabel()
			+ "<br>" + ageSeconds + "s ago";
	}
}
