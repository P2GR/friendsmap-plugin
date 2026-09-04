/*
 * Copyright (c) 2026, P2GR
 * All rights reserved.
 */
package com.friendsmap.model;

/**
 * Visibility toggles JSON payload. Field names match the backend JSON contract.
 */
public class TogglesPayload
{
	public boolean visibleToClan;
	public boolean visibleToFriends;
	public boolean visibleToFriendsChat;
	public boolean showAlways;
}
