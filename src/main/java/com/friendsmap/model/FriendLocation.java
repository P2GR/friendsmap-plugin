/*
 * Copyright (c) 2026, P2GR
 * All rights reserved.
 */
package com.friendsmap.model;

import java.time.Instant;
import net.runelite.api.coords.WorldPoint;

/**
 * Immutable carrier for one visible friend's location snapshot.
 * Single source of truth consumed by world map and minimap.
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
	 * True when the friend can be rendered on the world map.
	 * World map only covers the surface plane; caves, instances and
	 * other planes are skipped.
	 */
	public boolean isOnWorldMap()
	{
		return location != null && location.getPlane() == 0;
	}

	public int getRegionId()
	{
		return location == null ? -1 : location.getRegionID();
	}
}
