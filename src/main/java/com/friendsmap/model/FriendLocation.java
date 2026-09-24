/*
 * Copyright (c) 2026, P2GR
 * All rights reserved.
 */
package com.friendsmap.model;

import java.time.Instant;
import net.runelite.api.coords.WorldPoint;

/**
 * Immutable carrier for one visible friend's location snapshot.
 * Single source of truth consumed by the world map and player list.
 */
public final class FriendLocation
{
	private final String name;
	private final int world;
	private final WorldPoint location;
	private final Relation relation;
	private final Instant lastSeen;

	public FriendLocation(String name, int world, WorldPoint location, Relation relation, Instant lastSeen)
	{
		this.name = name;
		this.world = world;
		this.location = location;
		this.relation = relation;
		this.lastSeen = lastSeen;
	}

	public String getName()
	{
		return name;
	}

	public int getWorld()
	{
		return world;
	}

	/** World as shown to the user, e.g. {@code W366}; offline holds read {@code offline}. */
	public String getWorldLabel()
	{
		return world == 0 ? "offline" : "W" + world;
	}

	public WorldPoint getLocation()
	{
		return location;
	}

	public Relation getRelation()
	{
		return relation;
	}

	public Instant getLastSeen()
	{
		return lastSeen;
	}

	/**
	 * True when the friend's own position can be rendered on the world map.
	 * The world map only covers the surface plane; players on other planes
	 * are drawn at their area's entrance instead (see MapPointService).
	 */
	public boolean isOnWorldMap()
	{
		return location != null && location.getPlane() == 0;
	}
}
