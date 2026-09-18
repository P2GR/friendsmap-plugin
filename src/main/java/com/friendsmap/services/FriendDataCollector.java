/*
 * Copyright (c) 2026, P2GR
 * All rights reserved.
 */
package com.friendsmap.services;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Friend;
import net.runelite.api.FriendContainer;
import net.runelite.api.FriendsChatManager;
import net.runelite.api.clan.ClanSettings;
import com.friendsmap.model.RosterEntry;
import com.friendsmap.model.RosterSnapshot;

/**
 * Reads the local player's rosters from the RuneLite API.
 * Must run on the client thread.
 */
public class FriendDataCollector
{
	private final Client client;

	@Inject
	public FriendDataCollector(Client client)
	{
		this.client = client;
	}

	public RosterSnapshot collect()
	{
		return new RosterSnapshot(collectFriends(), clanName(), friendsChatName());
	}

	private List<RosterEntry> collectFriends()
	{
		List<RosterEntry> entries = new ArrayList<>();
		FriendContainer friends = client.getFriendContainer();
		if (friends != null)
		{
			for (Friend friend : friends.getMembers())
			{
				if (friend != null)
				{
					entries.add(new RosterEntry(friend.getName()));
				}
			}
		}
		return entries;
	}

	private String clanName()
	{
		ClanSettings settings = client.getClanSettings();
		return settings == null ? "" : settings.getName();
	}

	private String friendsChatName()
	{
		FriendsChatManager friendsChatManager = client.getFriendsChatManager();
		return friendsChatManager == null ? "" : friendsChatManager.getName();
	}
}
