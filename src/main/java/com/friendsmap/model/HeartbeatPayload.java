/*
 * Copyright (c) 2026, P2GR
 * All rights reserved.
 */
package com.friendsmap.model;

import java.util.List;

/**
 * Heartbeat request body. Field names match the backend JSON contract.
 */
public class HeartbeatPayload
{
	public String username;
	public int world;
	public PositionPayload position;
	public boolean instance;
	public String clan;
	public String friendsChat;
	public List<String> friends;
	public TogglesPayload toggles;
}
