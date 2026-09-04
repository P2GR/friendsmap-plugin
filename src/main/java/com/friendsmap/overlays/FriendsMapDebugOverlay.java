/*
 * Copyright (c) 2026, P2GR
 * All rights reserved.
 */
package com.friendsmap.overlays;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.ArrayList;
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

/**
 * Minimal debug overlay: server connection state + displayed friends
 * grouped by relation. Enabled via Advanced > Debug.
 */
public class FriendsMapDebugOverlay extends Overlay
{
	private static final int MAX_LINES_PER_SECTION = 25;

	private final PanelComponent panelComponent = new PanelComponent();

	private final FriendsMapConfig config;
	private final FriendsMapPlugin plugin;

	@Inject
	public FriendsMapDebugOverlay(FriendsMapConfig config, FriendsMapPlugin plugin)
	{
		this.config = config;
		this.plugin = plugin;
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
			.left("Server Connection")
			.right(serverStatus())
			.build());

		List<FriendLocation> friends = new ArrayList<>();
		List<FriendLocation> clan = new ArrayList<>();
		List<FriendLocation> friendsChat = new ArrayList<>();
		for (FriendLocation friend : plugin.getCurrentFriends())
		{
			switch (friend.getRelation())
			{
				case CLAN:
					clan.add(friend);
					break;
				case FRIENDS_CHAT:
					friendsChat.add(friend);
					break;
				default:
					friends.add(friend);
			}
		}

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Displayed (" + plugin.getCurrentFriends().size() + ")")
			.build());
		addSection("Friends (" + friends.size() + ")", friends);
		addSection("Clan (" + clan.size() + ")", clan);
		addSection("Friends Chat (" + friendsChat.size() + ")", friendsChat);

		return panelComponent.render(graphics);
	}

	private String serverStatus()
	{
		String mode = plugin.getModeLabel();
		if (mode.contains("offline"))
		{
			return "Offline";
		}
		if (mode.contains("SIMULATED"))
		{
			return "Simulated";
		}
		return "Live";
	}

	private void addSection(String title, List<FriendLocation> friends)
	{
		panelComponent.getChildren().add(TitleComponent.builder()
			.text(title)
			.build());
		int count = 0;
		for (FriendLocation friend : friends)
		{
			if (count++ >= MAX_LINES_PER_SECTION)
			{
				panelComponent.getChildren().add(LineComponent.builder()
					.left("+ " + (friends.size() - MAX_LINES_PER_SECTION) + " more")
					.build());
				return;
			}
			panelComponent.getChildren().add(LineComponent.builder()
				.left(friend.getName())
				.right("W" + friend.getWorld())
				.build());
		}
	}
}
