/*
 * Copyright (c) 2026, P2GR
 * All rights reserved.
 */
package com.friendsmap.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.runelite.api.coords.WorldPoint;
import com.friendsmap.model.FriendLocation;
import com.friendsmap.model.Relation;

/**
 * Fake friend roster with scripted paths. Used while no backend exists.
 *
 * <p>Covers every display case:</p>
 * <ul>
 *   <li>moving friend on surface plane (world map + minimap dot + label)</li>
 *   <li>static friend on another world (world map only)</li>
 *   <li>clan / friends-chat relations (different icon colors)</li>
 *   <li>friend on plane 1 (not rendered on the surface world map)</li>
 * </ul>
 *
 * <p>Not thread-safe. Called from the client thread only.</p>
 */
public class SimulatedLocationProvider implements LocationProvider
{
	private static final int TICKS_PER_STEP = 5;

	private final List<FakeFriend> fakeFriends = new ArrayList<>();
	private long tick;

	public SimulatedLocationProvider()
	{
		fakeFriends.add(new FakeFriend(
			"FakeFriend Varrock", 330, Relation.FRIEND,
			Arrays.asList(
				new WorldPoint(3211, 3421, 0),
				new WorldPoint(3214, 3424, 0),
				new WorldPoint(3217, 3421, 0),
				new WorldPoint(3214, 3418, 0))));
		fakeFriends.add(new FakeFriend(
			"FakeFriend Lummy", 301, Relation.FRIEND,
			Arrays.asList(new WorldPoint(3222, 3218, 0))));
		fakeFriends.add(new FakeFriend(
			"FakeClannie Edge", 330, Relation.CLAN,
			Arrays.asList(
				new WorldPoint(3093, 3493, 0),
				new WorldPoint(3097, 3491, 0),
				new WorldPoint(3099, 3488, 0),
				new WorldPoint(3095, 3487, 0))));
		fakeFriends.add(new FakeFriend(
			"FakeFcMate GE", 330, Relation.FRIENDS_CHAT,
			Arrays.asList(new WorldPoint(3164, 3485, 0))));
		fakeFriends.add(new FakeFriend(
			"FakeCaver", 330, Relation.FRIEND,
			Arrays.asList(new WorldPoint(3215, 3420, 1))));
	}

	public void advanceTick()
	{
		tick++;
		for (FakeFriend friend : fakeFriends)
		{
			friend.advance(tick);
		}
	}

	@Override
	public List<FriendLocation> getFriends()
	{
		Instant now = Instant.now();
		List<FriendLocation> friends = new ArrayList<>(fakeFriends.size());
		for (FakeFriend friend : fakeFriends)
		{
			friends.add(new FriendLocation(
				friend.name,
				friend.world,
				friend.currentPoint(),
				friend.relation,
				now));
		}
		return friends;
	}

	private static final class FakeFriend
	{
		private final String name;
		private final int world;
		private final Relation relation;
		private final List<WorldPoint> path;
		private int pathIndex;

		private FakeFriend(String name, int world, Relation relation, List<WorldPoint> path)
		{
			this.name = name;
			this.world = world;
			this.relation = relation;
			this.path = path;
		}

		private void advance(long tick)
		{
			if (path.size() > 1 && tick % TICKS_PER_STEP == 0)
			{
				pathIndex = (pathIndex + 1) % path.size();
			}
		}

		private WorldPoint currentPoint()
		{
			return path.get(pathIndex);
		}
	}
}
