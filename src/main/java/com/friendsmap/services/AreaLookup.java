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
 * consists of several of them. Entries may also carry an {@code overworld}
 * point - the entrance of an interior that has no place on the surface map -
 * which {@link #entranceFor} exposes so world-map renderers can draw off-map
 * players at their entrance. {@code transposeX/Y} only serves map drawing and
 * is ignored here.</p>
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
					WorldPoint overworld = entry.overworld == null
						? null
						: new WorldPoint(entry.overworld.x, entry.overworld.y, entry.overworld.plane);
					areas.add(new Area(entry.name, entry.area, overworld));
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
		Area best = bestFor(point);
		return best == null ? null : best.name;
	}

	/**
	 * Where the containing area opens onto the surface map (the data file's
	 * {@code overworld}), or null when the point is in the open world or the
	 * area has no recorded entrance. World-map renderers use this to place
	 * players in caves, lairs and instances at their entrance.
	 */
	public WorldPoint entranceFor(WorldPoint point)
	{
		Area best = bestFor(point);
		return best == null ? null : best.overworld;
	}

	/** Smallest (most specific) area containing the point, or null. */
	private Area bestFor(WorldPoint point)
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
		return best;
	}

	/** One named box from the data file. */
	private static final class Area
	{
		private final String name;
		private final JsonBox box;
		private final WorldPoint overworld;

		private Area(String name, JsonBox box, WorldPoint overworld)
		{
			this.name = name;
			this.box = box;
			this.overworld = overworld;
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
		private JsonOverworld overworld;
	}

	private static final class JsonBox
	{
		private int x;
		private int y;
		private int width;
		private int height;
		private int plane;
	}

	private static final class JsonOverworld
	{
		private int x;
		private int y;
		private int plane;
	}
}
