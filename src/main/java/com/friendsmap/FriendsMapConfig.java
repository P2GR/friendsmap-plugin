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
		name = "Simulation",
		description = "Fake friends for development without a backend",
		position = 3
	)
	String simulationSection = "simulation";

	@ConfigSection(
		name = "Advanced",
		description = "Debugging and polling",
		position = 4
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
		keyName = "maxTrackedFriends",
		name = "Max tracked friends",
		description = "Upper bound on friends rendered and sent to the backend. Prevents huge rosters from flooding the map and network.",
		section = displaySection,
		position = 2
	)
	@Range(min = 10, max = 500)
	default int maxTrackedFriends()
	{
		return 200;
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
		keyName = "simulateFriends",
		name = "Simulate friends",
		description = "Always use fake scripted friends. No network traffic. Development only.",
		section = simulationSection,
		position = 0
	)
	default boolean simulateFriends()
	{
		return false;
	}

	@ConfigItem(
		keyName = "simulateWhenOffline",
		name = "Simulate when server offline",
		description = "When the backend cannot be reached, fall back to simulated friends so map and minimap stay testable.",
		section = simulationSection,
		position = 1
	)
	default boolean simulateWhenOffline()
	{
		return true;
	}

	@ConfigItem(
		keyName = "debug",
		name = "Debug overlay",
		description = "Show rosters (friends, clan, friends chat), incoming backend data and displayed friend data in an overlay.",
		section = advancedSection,
		position = 0
	)
	default boolean debug()
	{
		return false;
	}
}
