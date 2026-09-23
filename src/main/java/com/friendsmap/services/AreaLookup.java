/*
 * Copyright (c) 2026, P2GR
 * All rights reserved.
 */
package com.friendsmap.services;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.coords.WorldPoint;

/**
 * Resolves world points to named areas (boss lairs, dungeons, minigames) from
 * the bundled {@code areas.json}.
 *
 * <p>The data keeps the schema of the file vendored from Tibo De Munck's
 * runelite-live-friend-locations-plugin (BSD-2-Clause, see
 * {@code areas-LICENSE.txt} next to {@code areas.json}) so updated files can be
 * dropped in unchanged. Each entry is a name plus one box
 * ({@code x, y, width, height, plane}); names repeat across boxes when an area
 * consists of several of them. The remaining fields in the file ({@code
 * overworld}, {@code transposeX/Y}) only exist to draw map interiors over
 * their entrance and are intentionally ignored here.</p>
 *
 * <p>Matching is x/y/plane containment, not region ids: most areas are
 * smaller than a 64x64 region and plane separates, for example, the Kalphite
 * Lair from the Kalphite Queen's chamber. When several boxes contain a point
 * the smallest one wins (most specific). Points must be in static map space -
 * resolve instanced players first (see {@code WorldPoints.realLocation}).</p>
 */
@Singleton
public class AreaLookup
{
	private final List<Area> areas = new ArrayList<>();

	@Inject
	public AreaLookup(Gson gson)
	{
		try (InputStream in = AreaLookup.class.getResourceAsStream("areas.json"))
		{
			if (in == null)
			{
				throw new IllegalStateException("areas.json is missing from the jar");
			}

			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
			{
				JsonArea[] entries = gson.fromJson(reader, JsonArea[].class);
				if (entries == null)
				{
					throw new IllegalStateException("areas.json is empty");
				}

				for (JsonArea entry : entries)
				{
					if (entry == null || entry.name == null || entry.name.isEmpty()
						|| entry.area == null || entry.area.width <= 0 || entry.area.height <= 0)
					{
						continue; // skip malformed entries rather than failing the plugin
					}
					areas.add(new Area(entry.name, entry.area));
				}
			}
		}
		catch (IOException e)
		{
			throw new IllegalStateException("areas.json is unreadable", e);
		}
	}

	/** Name of the area containing the point, or null when in the open world. */
	public String nameFor(WorldPoint point)
	{
		if (point == null)
		{
			return null;
		}

		Area best = null;
		for (Area area : areas)
		{
			if (area.contains(point) && (best == null || area.tiles() < best.tiles()))
			{
				best = area;
			}
		}
		return best == null ? null : best.name;
	}

	/** One named box from the data file. */
	private static final class Area
	{
		private final String name;
		private final JsonBox box;

		private Area(String name, JsonBox box)
		{
			this.name = name;
			this.box = box;
		}

		private boolean contains(WorldPoint point)
		{
			return point.getPlane() == box.plane
				&& point.getX() >= box.x && point.getX() < box.x + box.width
				&& point.getY() >= box.y && point.getY() < box.y + box.height;
		}

		private int tiles()
		{
			return box.width * box.height;
		}
	}

	/** Gson mapping of one areas.json entry; unknown fields are ignored. */
	private static final class JsonArea
	{
		private String name;
		private JsonBox area;
	}

	private static final class JsonBox
	{
		private int x;
		private int y;
		private int width;
		private int height;
		private int plane;
	}
}
