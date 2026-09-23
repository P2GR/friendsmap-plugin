/*
 * Copyright (c) 2026, P2GR
 * All rights reserved.
 */
package com.friendsmap;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(FriendsMapConfig.GROUP)
public interface FriendsMapConfig extends Config
{
	String GROUP = "friendsmap";

	@ConfigSection(
		name = "Display",
		description = "Where friends are drawn",
		position = 0
	)
	String displaySection = "display";

	@ConfigSection(
		name = "Visibility",
		description = "Who is allowed to see your location",
		position = 1
	)
	String visibilitySection = "visibility";

	@ConfigSection(
		name = "Colors & Sizes",
		description = "Dot appearance on map and minimap",
		position = 2
	)
	String colorsSection = "colors";

	@ConfigSection(
		name = "Advanced",
		description = "Debugging and polling",
		position = 3
	)
	String advancedSection = "advanced";

	@ConfigItem(
		keyName = "showOnWorldMap",
		name = "Show on world map",
		description = "Draw friend icons on the world map (surface only).",
		section = displaySection,
		position = 0
	)
	default boolean showOnWorldMap()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showWorldMapNames",
		name = "Show names on world map",
		description = "Render the friend's name in small text next to their world map dot.",
		section = displaySection,
		position = 1
	)
	default boolean showWorldMapNames()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showWorldMapWorld",
		name = "Show world on map",
		description = "Show the player's current world in their map label (for example Playername - W366).",
		section = displaySection,
		position = 2
	)
	default boolean showWorldMapWorld()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPlayerList",
		name = "Show player list",
		description = "Show a scrollable list of the players currently visible, in the top-left corner of the world map.",
		section = displaySection,
		position = 3
	)
	default boolean showPlayerList()
	{
		return true;
	}

	@ConfigItem(
		keyName = "playerListFilter",
		name = "Player list filter",
		description = "Which players the world map list shows. Show All groups the list under Friends, Clan and Friends Chat headers.",
		section = displaySection,
		position = 4
	)
	default PlayerListFilter playerListFilter()
	{
		return PlayerListFilter.SHOW_ALL;
	}

	@ConfigItem(
		keyName = "playerListRows",
		name = "Player list height",
		description = "How many rows the world map list shows at once. Longer lists scroll with the mouse wheel.",
		section = displaySection,
		position = 5
	)
	@Range(min = 3, max = 25)
	default int playerListRows()
	{
		return 8;
	}

	@ConfigItem(
		keyName = "playerListFontSize",
		name = "Player list text size",
		description = "Text size of the player names in the world map list.",
		section = displaySection,
		position = 6
	)
	@Range(min = 10, max = 24)
	default int playerListFontSize()
	{
		return 16;
	}

	@ConfigItem(
		keyName = "visibilityClan",
		name = "Visible to clan",
		description = "Share your location with members of the same clan channel.",
		section = visibilitySection,
		position = 0
	)
	default boolean visibilityClan()
	{
		return true;
	}

	@ConfigItem(
		keyName = "visibilityFriends",
		name = "Visible to friends",
		description = "Share your location with mutual friends.",
		section = visibilitySection,
		position = 1
	)
	default boolean visibilityFriends()
	{
		return true;
	}

	@ConfigItem(
		keyName = "visibilityFriendsChat",
		name = "Visible to friends chat",
		description = "Share your location with members of the same friends chat.",
		section = visibilitySection,
		position = 2
	)
	default boolean visibilityFriendsChat()
	{
		return true;
	}

	@ConfigItem(
		keyName = "sendLocationWilderness",
		name = "Send location in Wilderness",
		description = "Send your location while inside the Wilderness. When off, heartbeats continue (you still see friends) but your position is never shared from the Wilderness.",
		section = visibilitySection,
		position = 3
	)
	default boolean sendLocationWilderness()
	{
		return false;
	}

	@ConfigItem(
		keyName = "sendLocationPvpWorlds",
		name = "Send location on PvP worlds",
		description = "Send your location while on PvP worlds. When off, heartbeats continue (you still see friends) but your position is never shared from PvP worlds.",
		section = visibilitySection,
		position = 4
	)
	default boolean sendLocationPvpWorlds()
	{
		return false;
	}

	@ConfigItem(
		keyName = "dotColorFriend",
		name = "Friend dot color",
		description = "Dot color for friends.",
		section = colorsSection,
		position = 0
	)
	default Color dotColorFriend()
	{
		return new Color(72, 217, 126);
	}

	@ConfigItem(
		keyName = "dotColorClan",
		name = "Clan dot color",
		description = "Dot color for clan members.",
		section = colorsSection,
		position = 1
	)
	default Color dotColorClan()
	{
		return new Color(255, 144, 64);
	}

	@ConfigItem(
		keyName = "dotColorFriendsChat",
		name = "Friends chat dot color",
		description = "Dot color for friends chat members.",
		section = colorsSection,
		position = 2
	)
	default Color dotColorFriendsChat()
	{
		return new Color(186, 85, 211);
	}

	@ConfigItem(
		keyName = "dotSize",
		name = "Dot size",
		description = "Diameter in pixels for world map and minimap dots.",
		section = colorsSection,
		position = 3
	)
	@Range(min = 4, max = 24)
	default int dotSize()
	{
		return 8;
	}

	@ConfigItem(
		keyName = "debug",
		name = "Debug overlay",
		description = "Show the server connection state and the friends currently displayed, grouped by relation, in an overlay.",
		section = advancedSection,
		position = 0
	)
	default boolean debug()
	{
		return false;
	}
}
