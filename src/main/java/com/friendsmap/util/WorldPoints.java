/*
 * Copyright (c) 2026, P2GR
 * All rights reserved.
 */
package com.friendsmap.util;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

/**
 * Instance-aware location helpers.
 */
public final class WorldPoints
{
	private WorldPoints()
	{
	}

	/**
	 * The player's position in static map space. Inside an instance the raw
	 * {@link Player#getWorldLocation()} is only where the instance copy was
	 * placed, while {@link WorldPoint#fromLocalInstance} resolves the tile back
	 * through the instance template chunks (which can be moved and rotated) to
	 * the map the area data is written against.
	 *
	 * <p>Player-owned houses are the exception: they are assembled inside a
	 * fixed virtual block (map regions 7513/7514/7769/7770/8025/8026 = x
	 * 1856-2047, y 5696-5823), and depending on how the house instance's
	 * chunks are template-mapped either the resolved or the raw point lands
	 * there. Whichever one does is the house location the area data matches
	 * against, so prefer it.</p>
	 */
	public static WorldPoint realLocation(Client client, Player player)
	{
		WorldPoint raw = player.getWorldLocation();
		WorldPoint resolved = raw;
		if (player.getLocalLocation() != null)
		{
			WorldPoint template = WorldPoint.fromLocalInstance(client, player.getLocalLocation());
			if (template != null)
			{
				resolved = template;
			}
		}
		if (!inPlayerOwnedHouse(resolved) && inPlayerOwnedHouse(raw))
		{
			return raw;
		}
		return resolved;
	}

	/** Player-owned house block: map regions 7513/7514/7769/7770/8025/8026. */
	private static final int POH_X = 1856;
	private static final int POH_Y = 5696;
	private static final int POH_WIDTH = 192;
	private static final int POH_HEIGHT = 128;

	private static boolean inPlayerOwnedHouse(WorldPoint point)
	{
		return point != null
			&& point.getX() >= POH_X && point.getX() < POH_X + POH_WIDTH
			&& point.getY() >= POH_Y && point.getY() < POH_Y + POH_HEIGHT;
	}

	/** True when the player is standing in an instanced copy of the map. */
	public static boolean inInstance(Client client, Player player)
	{
		LocalPoint local = player.getLocalLocation();
		if (local == null)
		{
			return false;
		}
		WorldView worldView = client.getWorldView(local.getWorldView());
		return worldView != null && worldView.isInstance();
	}
}
