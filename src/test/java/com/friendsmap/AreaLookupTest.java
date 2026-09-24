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

	@Test
	public void drawsInteriorsAtTheirEntrance()
	{
		// Off-atlas interiors carry their entrance ("overworld") in the data;
		// world-map dots are drawn there so dungeon friends stay findable.
		WorldPoint callisto = new WorldPoint(3352, 10320, 0);
		assertEquals("Callisto's Den", lookup.nameFor(callisto));
		assertEquals(new WorldPoint(3292, 3850, 0), lookup.entranceFor(callisto));

		// Open world, and areas without a recorded entrance, have no fallback.
		assertNull(lookup.entranceFor(new WorldPoint(3100, 3400, 0)));
		assertNull(lookup.entranceFor(new WorldPoint(1900, 5750, 0)));
		assertNull(lookup.entranceFor(null));
	}

	@Test
	public void findsNewBossLairs()
	{
		assertEquals("Obor's Lair", lookup.nameFor(new WorldPoint(3091, 9799, 0)));
		assertEquals("Bryophyta's Lair", lookup.nameFor(new WorldPoint(3220, 9933, 0)));
		assertEquals("Alchemical Hydra's Lair", lookup.nameFor(new WorldPoint(1364, 10265, 0)));
		assertEquals("Sarachnis's Lair", lookup.nameFor(new WorldPoint(1840, 9900, 0)));
		assertEquals("Skotizo's Lair", lookup.nameFor(new WorldPoint(2271, 5661, 0)));
		assertEquals("Scurrius's Lair", lookup.nameFor(new WorldPoint(3299, 9867, 0)));
		assertEquals("Vardorvis's Lair", lookup.nameFor(new WorldPoint(1128, 3417, 0)));
		assertEquals("The Leviathan's Lair", lookup.nameFor(new WorldPoint(2081, 6372, 0)));
		assertEquals("Venenatis's Lair", lookup.nameFor(new WorldPoint(3319, 3798, 0)));
	}

	@Test
	public void findsMinigames()
	{
		assertEquals("Pest Control", lookup.nameFor(new WorldPoint(2658, 2625, 0)));
		assertEquals("Castle Wars", lookup.nameFor(new WorldPoint(2407, 3105, 0)));
		assertEquals("Soul Wars", lookup.nameFor(new WorldPoint(2206, 2900, 0)));
		assertEquals("Wintertodt", lookup.nameFor(new WorldPoint(1630, 3981, 0)));
		assertEquals("Tempoross Cove", lookup.nameFor(new WorldPoint(3035, 2850, 0)));
		assertEquals("Last Man Standing", lookup.nameFor(new WorldPoint(3142, 3636, 0)));
		assertEquals("Mage Training Arena", lookup.nameFor(new WorldPoint(3364, 3312, 0)));
		assertEquals("Pyramid Plunder", lookup.nameFor(new WorldPoint(3289, 2793, 0)));
		assertEquals("Puro-Puro", lookup.nameFor(new WorldPoint(2593, 4320, 0)));
		assertEquals("Sorceress's Garden", lookup.nameFor(new WorldPoint(2912, 5472, 0)));
		assertEquals("Guardians of the Rift", lookup.nameFor(new WorldPoint(3616, 9492, 0)));
		assertEquals("Barbarian Assault", lookup.nameFor(new WorldPoint(2533, 3571, 0)));
		assertEquals("Barbarian Assault", lookup.nameFor(new WorldPoint(2593, 5280, 0)));
	}

	@Test
	public void findsTownsAndNewDungeons()
	{
		assertEquals("Darkmeyer", lookup.nameFor(new WorldPoint(3597, 3360, 0)));
		assertEquals("Meiyerditch", lookup.nameFor(new WorldPoint(3615, 3250, 0)));
		assertEquals("Slepe", lookup.nameFor(new WorldPoint(3724, 3335, 0)));
		assertEquals("Zul-Andra", lookup.nameFor(new WorldPoint(2193, 3060, 0)));
		assertEquals("The Scar", lookup.nameFor(new WorldPoint(2039, 6428, 0)));
		// Nested: the boss arena wins over the surrounding city.
		assertEquals("The Whisperer's Lair", lookup.nameFor(new WorldPoint(2656, 6369, 0)));
		assertEquals("Lassar Undercity", lookup.nameFor(new WorldPoint(2560, 6280, 0)));
		assertEquals("Mourner Tunnels", lookup.nameFor(new WorldPoint(1950, 4630, 0)));
		assertEquals("Dream World", lookup.nameFor(new WorldPoint(1760, 5087, 2)));
		assertEquals("Kurask Lair", lookup.nameFor(new WorldPoint(1182, 9200, 0)));
		assertEquals("Karamjan Temple", lookup.nameFor(new WorldPoint(2848, 9256, 0)));
	}

	@Test
	public void nightmareZoneDreamsDoNotLabelAsKbdLair()
	{
		// Dreams run in the lair's own map (region 9033) above ground level;
		// the sender shifts them one region north into the zone's own box.
		for (int plane = 0; plane < 4; plane++)
		{
			assertEquals("Nightmare Zone", lookup.nameFor(new WorldPoint(2272, 4768, plane)));
		}
		assertEquals("King Black Dragon Lair", lookup.nameFor(new WorldPoint(2260, 4700, 0)));
		// The lobby is real overworld and doubles as the dream's entrance.
		assertEquals("Nightmare Zone", lookup.nameFor(new WorldPoint(2605, 3115, 0)));
		assertEquals(new WorldPoint(2605, 3115, 0), lookup.entranceFor(new WorldPoint(2272, 4768, 1)));
	}

	@Test
	public void findsRenamedAreas()
	{
		assertEquals("Cerberus' Lair", lookup.nameFor(new WorldPoint(1305, 1273, 0)));
		assertEquals("King Black Dragon Lair", lookup.nameFor(new WorldPoint(2260, 4700, 0)));
	}
}
