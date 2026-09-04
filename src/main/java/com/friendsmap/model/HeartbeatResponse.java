/*
 * Copyright (c) 2026, P2GR
 * All rights reserved.
 */
package com.friendsmap.model;

import java.util.List;

/**
 * Heartbeat response. Field names match the backend JSON contract.
 */
public class HeartbeatResponse
{
	public long now;
	public List<VisibleFriend> visible;
}
