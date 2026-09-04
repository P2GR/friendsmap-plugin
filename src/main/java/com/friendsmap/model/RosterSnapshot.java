/*
 * Copyright (c) 2026, P2GR
 * All rights reserved.
 */
package com.friendsmap.model;

import java.util.Collections;
import java.util.List;

/**
 * Immutable snapshot of the local player's rosters.
 * Used by the debug overlay and the heartbeat payload builder.
 */
public final class RosterSnapshot
{
	private final List<RosterEntry> friends;
	private final String clanName;
	private final List<RosterEntry> clanMembers;
	private final String friendsChatName;
	private final List<RosterEntry> friendsChatMembers;

	public RosterSnapshot(List<RosterEntry> friends, String clanName, List<RosterEntry> clanMembers,
		String friendsChatName, List<RosterEntry> friendsChatMembers)
	{
		this.friends = Collections.unmodifiableList(friends);
		this.clanName = clanName;
		this.clanMembers = Collections.unmodifiableList(clanMembers);
		this.friendsChatName = friendsChatName;
		this.friendsChatMembers = Collections.unmodifiableList(friendsChatMembers);
	}

	public List<RosterEntry> getFriends()
	{
		return friends;
	}

	public String getClanName()
	{
		return clanName;
	}

	public List<RosterEntry> getClanMembers()
	{
		return clanMembers;
	}

	public String getFriendsChatName()
	{
		return friendsChatName;
	}

	public List<RosterEntry> getFriendsChatMembers()
	{
		return friendsChatMembers;
	}
}
