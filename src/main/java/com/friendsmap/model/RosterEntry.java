/*
 * Copyright (c) 2026, P2GR
 * All rights reserved.
 */
package com.friendsmap.model;

/**
 * One roster member: display name + world.
 */
public final class RosterEntry
{
	private final String name;
	private final int world;

	public RosterEntry(String name, int world)
	{
		this.name = name;
		this.world = world;
	}

	public String getName()
	{
		return name;
	}

	public int getWorld()
	{
		return world;
	}
}
