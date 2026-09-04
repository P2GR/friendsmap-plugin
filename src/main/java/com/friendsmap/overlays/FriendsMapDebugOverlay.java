/*
 * Copyright (c) 2026, P2GR
 * All rights reserved.
 */
package com.friendsmap.overlays;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import com.friendsmap.FriendsMapConfig;
import com.friendsmap.FriendsMapPlugin;
import com.friendsmap.model.FriendLocation;
import com.friendsmap.model.RosterEntry;
import com.friendsmap.model.RosterSnapshot;
import com.friendsmap.services.FriendDataCollector;

/**
 * Debug overlay. Shows rosters (friends, clan, friends chat), incoming backend
 * data and the currently displayed friend data. Enabled via Advanced > Debug.
 */
public class FriendsMapDebugOverlay extends Overlay
{
	private static final int MAX_LINES_PER_SECTION = 25;

	private final PanelComponent panelComponent = new PanelComponent();

	private final FriendsMapConfig config;
	private final FriendsMapPlugin plugin;
	private final FriendDataCollector collector;

	@Inject
	public FriendsMapDebugOverlay(FriendsMapConfig config, FriendsMapPlugin plugin, FriendDataCollector collector)
	{
		this.config = config;
		this.plugin = plugin;
		this.collector = collector;
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.debug())
		{
			return null;
		}

		panelComponent.getChildren().clear();
		panelComponent.setPreferredSize(new Dimension(320, 0));

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("FriendsMap Debug")
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Mode")
			.right(plugin.getModeLabel())
			.build());

		RosterSnapshot roster = collector.collect();

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Friends (" + roster.getFriends().size() + ")")
			.build());
		addEntries(roster.getFriends());

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Clan: " + (roster.getClanName().isEmpty() ? "(none)" : roster.getClanName())
				+ " (" + roster.getClanMembers().size() + ")")
			.build());
		addEntries(roster.getClanMembers());

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Friends Chat: " + (roster.getFriendsChatName().isEmpty() ? "(none)" : roster.getFriendsChatName())
				+ " (" + roster.getFriendsChatMembers().size() + ")")
			.build());
		addEntries(roster.getFriendsChatMembers());

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Incoming (backend)")
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Request sent")
			.right(truncate(plugin.getLastRequestLog(), 55))
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Response received")
			.right(truncate(plugin.getLastResponseLog(), 55))
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Health probe")
			.right(plugin.getLastHealthStatus())
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Raw body")
			.right(truncate(plugin.getLastHealthBody(), 60))
			.build());

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Displayed (" + plugin.getCurrentFriends().size() + ")")
			.build());
		for (FriendLocation friend : plugin.getCurrentFriends())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left(friend.getName())
				.right("W" + friend.getWorld() + " p" + friend.getLocation().getPlane()
					+ " " + friend.getRelation().getLabel())
				.build());
		}

		return panelComponent.render(graphics);
	}

	private void addEntries(List<RosterEntry> entries)
	{
		int count = 0;
		for (RosterEntry entry : entries)
		{
			if (count++ >= MAX_LINES_PER_SECTION)
			{
				panelComponent.getChildren().add(LineComponent.builder()
					.left("+ " + (entries.size() - MAX_LINES_PER_SECTION) + " more")
					.build());
				return;
			}
			panelComponent.getChildren().add(LineComponent.builder()
				.left(entry.getName() + " (W" + entry.getWorld() + ")")
				.build());
		}
	}

	private static String truncate(String value, int max)
	{
		if (value == null)
		{
			return "-";
		}
		return value.length() > max ? value.substring(0, max) + "..." : value;
	}
}
