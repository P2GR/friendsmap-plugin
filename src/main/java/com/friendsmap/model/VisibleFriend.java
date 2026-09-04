/*
 * Copyright (c) 2026, P2GR
 * All rights reserved.
 */
package com.friendsmap.model;

/**
 * One visible friend in the heartbeat response. Field names match the backend contract.
 */
public class VisibleFriend
{
	public String username;
	public int world;
	public PositionPayload position;
	public boolean instance;
	public long lastSeen;
	public String relation;
}
