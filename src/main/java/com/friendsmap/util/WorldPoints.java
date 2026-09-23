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
	 * the map the area data is written against. Returns null when the player's
	 * location is not loaded.
	 */
	public static WorldPoint realLocation(Client client, Player player)
	{
		if (player.getLocalLocation() != null)
		{
			WorldPoint template = WorldPoint.fromLocalInstance(client, player.getLocalLocation());
			if (template != null)
			{
				return template;
			}
		}
		return player.getWorldLocation();
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
