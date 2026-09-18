/*
 * Copyright (c) 2026, P2GR
 * All rights reserved.
 */
package com.friendsmap.model;

import java.util.Collections;
import java.util.List;

/**
 * Immutable snapshot of the local player's rosters, built once per poll and
 * used to assemble the heartbeat payload.
 */
public final class RosterSnapshot
{
	private final List<RosterEntry> friends;
	private final String clanName;
	private final String friendsChatName;

	public RosterSnapshot(List<RosterEntry> friends, String clanName, String friendsChatName)
	{
		this.friends = Collections.unmodifiableList(friends);
		this.clanName = clanName;
		this.friendsChatName = friendsChatName;
	}

	public List<RosterEntry> getFriends()
	{
		return friends;
	}

	public String getClanName()
	{
		return clanName;
	}

	public String getFriendsChatName()
	{
		return friendsChatName;
	}
}
