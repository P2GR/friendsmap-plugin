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
import net.runelite.api.coords.WorldPoint;
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
	private final AreaLookup areaLookup;
	private final Map<String, WorldMapPoint> pointsByName = new HashMap<>();
	private final Map<String, Integer> worldsByName = new HashMap<>();

	@Inject
	public MapPointService(WorldMapPointManager worldMapPointManager, FriendsMapConfig config, AreaLookup areaLookup)
	{
		this.worldMapPointManager = worldMapPointManager;
		this.config = config;
		this.areaLookup = areaLookup;
	}

	/**
	 * Reconcile the point set with the given snapshot.
	 * Surface-plane friends become points at their own position; friends in
	 * caves, lairs and instances are placed at their area's entrance.
	 */
	public synchronized void synchronize(Collection<FriendLocation> friends, boolean worldMapEnabled)
	{
		Set<String> seen = new HashSet<>();
		for (FriendLocation friend : friends)
		{
			seen.add(friend.getName());

			// Interiors have no place on the surface map; when the area data
			// knows their entrance, the dot is drawn there instead so the
			// friend stays findable.
			WorldPoint entrance = areaLookup.entranceFor(friend.getLocation());
			boolean atEntrance = entrance != null;
			WorldPoint mapPoint = atEntrance ? entrance : friend.getLocation();
			if (!worldMapEnabled || (!atEntrance && !friend.isOnWorldMap()))
			{
				continue;
			}

			WorldMapPoint existing = pointsByName.get(friend.getName());
			Integer knownWorld = worldsByName.get(friend.getName());
			if (existing != null && existing.getWorldPoint() != null
				&& existing.getWorldPoint().equals(mapPoint)
				&& knownWorld != null && knownWorld == friend.getWorld())
			{
				continue;
			}

			if (existing != null)
			{
				worldMapPointManager.remove(existing);
			}

			String displayName = mapLabel(friend);
			WorldMapPoint point = new WorldMapPoint(mapPoint, FriendIconFactory.worldMapDot(friend.getRelation(), config, displayName, friend.getWorld() == 0));
			if (displayName != null)
			{
				// Anchor the dot's center on the world point; the name extends right.
				point.setImagePoint(new Point(config.dotSize() / 2, point.getImage().getHeight() / 2));
			}
			point.setTooltip(buildTooltip(friend, atEntrance));
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

	/** Dot label: name and/or world per config; null = dot only. */
	private String mapLabel(FriendLocation friend)
	{
		boolean name = config.showWorldMapNames();
		boolean world = config.showWorldMapWorld();
		if (name && world)
		{
			return friend.getName() + " - " + friend.getWorldLabel();
		}
		if (name)
		{
			return friend.getName();
		}
		return world ? friend.getWorldLabel() : null;
	}

	private String buildTooltip(FriendLocation friend, boolean atEntrance)
	{
		long ageSeconds = Duration.between(friend.getLastSeen(), Instant.now()).getSeconds();
		String header = friend.getWorld() == 0
			? friend.getName() + " (offline)"
			: friend.getName() + " (World " + friend.getWorld() + ")";
		String area = config.showAreaNames() ? areaLookup.nameFor(friend.getLocation()) : null;
		return header
			+ "<br>" + friend.getRelation().getLabel()
			+ (area == null ? "" : "<br>" + area)
			+ (atEntrance ? "<br>Shown at entrance" : "")
			+ "<br>" + ageSeconds + "s ago";
	}
}
