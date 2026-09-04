/*
 * Copyright (c) 2026, P2GR
 * All rights reserved.
 */
package com.friendsmap.model;

/**
 * How a visible player relates to the local player.
 * Determines icon colors on map and minimap.
 */
public enum Relation
{
	FRIEND("friend"),
	CLAN("clan"),
	FRIENDS_CHAT("friends chat");

	private final String label;

	Relation(String label)
	{
		this.label = label;
	}

	public String getLabel()
	{
		return label;
	}
}
