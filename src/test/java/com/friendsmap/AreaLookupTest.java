/*
 * Copyright (c) 2026, P2GR
 * All rights reserved.
 */
package com.friendsmap;

import com.friendsmap.services.AreaLookup;
import com.google.gson.Gson;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Matching tests against the bundled areas.json: the assertions encode the
 * data file's conventions (boss lairs, plane separation, smallest box wins).
 */
public class AreaLookupTest
{
	private final AreaLookup lookup = new AreaLookup(new Gson());

	@Test
	public void findsBossLairByName()
	{
		assertEquals("Callisto's Den", lookup.nameFor(new WorldPoint(3352, 10320, 0)));
	}

	@Test
	public void planeSeparatesOverlappingAreas()
	{
		assertEquals("Kalphite Lair", lookup.nameFor(new WorldPoint(3460, 9480, 2)));
		assertEquals("Kalphite Queen's Chamber", lookup.nameFor(new WorldPoint(3460, 9480, 0)));
	}

	@Test
	public void smallestAreaWinsOnOverlap()
	{
		// Overlaps Taverley Dungeon (160x192); the crypt (54x38) is more specific.
		assertEquals("White Knights' Castle Crypt", lookup.nameFor(new WorldPoint(2950, 9720, 0)));
	}

	@Test
	public void openWorldHasNoArea()
	{
		assertNull(lookup.nameFor(new WorldPoint(3100, 3400, 0)));
		assertNull(lookup.nameFor(null));
	}

	@Test
	public void findsVampyriumRealmAndBossLair()
	{
		// Vampyrium ("The Blood Moon Rises", 30 June 2026); the coordinates are
		// from the OSRS Wiki's map templates, with region ids cross-checked.
		assertEquals("Vampyrium", lookup.nameFor(new WorldPoint(2604, 7838, 0))); // Sangvesti
		assertEquals("Vampyrium", lookup.nameFor(new WorldPoint(2740, 7850, 0))); // Sotfa Forest
		assertEquals("Maggot King's lair", lookup.nameFor(new WorldPoint(2911, 8036, 0)));
	}

	@Test
	public void findsVorkathAndUngael()
	{
		// Ungael (Dragon Slayer II); pins from the OSRS Wiki's location tables.
		assertEquals("Vorkath's arena", lookup.nameFor(new WorldPoint(2269, 4062, 0)));
		assertEquals("Ungael laboratory", lookup.nameFor(new WorldPoint(2275, 10470, 0)));
	}

	@Test
	public void findsPlayerOwnedHouseOnEveryPlane()
	{
		// Every house (owner or guest) is assembled in one virtual block: map
		// regions 7513/7514/7769/7770/8025/8026 (x 1856-2047, y 5696-5823).
		assertEquals("Player-owned house", lookup.nameFor(new WorldPoint(1900, 5750, 0)));
		assertEquals("Player-owned house", lookup.nameFor(new WorldPoint(2000, 5800, 1)));
		assertEquals("Player-owned house", lookup.nameFor(new WorldPoint(1856, 5696, 2)));
		assertEquals("Player-owned house", lookup.nameFor(new WorldPoint(2047, 5823, 3)));
		assertNull(lookup.nameFor(new WorldPoint(1855, 5695, 0)));
	}
}
