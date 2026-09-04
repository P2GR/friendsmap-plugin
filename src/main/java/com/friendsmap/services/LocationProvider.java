/*
 * Copyright (c) 2026, P2GR
 * All rights reserved.
 */
package com.friendsmap.services;

import java.util.List;
import com.friendsmap.model.FriendLocation;

/**
 * Source of visible friend locations.
 *
 * <p>Implemented by:</p>
 * <ul>
 *   <li>{@code SimulatedLocationProvider} — fake friends, no network (development)</li>
 *   <li>{@code LiveLocationProvider} — backend heartbeat/poll (future milestone)</li>
 * </ul>
 *
 * <p>Both implementations feed the same display pipeline, so simulation
 * exercises the identical code path as live data.</p>
 */
public interface LocationProvider
{
	List<FriendLocation> getFriends();
}
