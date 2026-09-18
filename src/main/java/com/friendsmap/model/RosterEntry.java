/*
 * Copyright (c) 2026, P2GR
 * All rights reserved.
 */
package com.friendsmap.model;

/**
 * One roster member: display name only. World is deliberately not carried —
 * the backend matches members by name and resolves presence itself.
 */
public final class RosterEntry
{
	private final String name;

	public RosterEntry(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}
}
