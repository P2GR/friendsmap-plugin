# Friends Map

Shows friends, clan members and friends-chat members as live icons on the OSRS
world map. Each icon is a colored dot (green = friend, orange = clan,
purple = friends chat) with the name in small text next to it.

## How it works

- Every ~2 seconds the plugin sends your position, world, friends list, clan and
  friends chat to the backend at `https://map.mss54.com` (hardcoded).
- The backend replies with the people you are allowed to see; the plugin draws
  them on the world map. Server never pushes — data comes back in the heartbeat
  response.
- Auth token is managed internally (auto-register), no user-facing API key.

## Config

**Display**
- `showOnWorldMap` — draw world map icons (default on)
- `showWorldMapNames` — name in small text next to each dot (default on)
- `dotColorFriend` / `dotColorClan` / `dotColorFriendsChat` — dot colors
- `dotSize` — dot diameter (4–24)

**Visibility**
- `visibilityClan` / `visibilityFriends` / `visibilityFriendsChat` — who may see you
- `sendLocationWilderness` — share position while in the Wilderness (default off,
  confirmed with a warning dialog when enabled)
- `sendLocationPvpWorlds` — share position on PvP worlds (default off,
  confirmed with a warning dialog when enabled)

**Simulation**
- `simulateFriends` — fake friends, zero network (development)
- `simulateWhenOffline` — automatically fall back to simulated friends when the
  backend is unreachable (default on)

**Advanced**
- `debug` — minimal overlay: server connection (Live/Offline/Simulated) and
  displayed friends grouped by relation

## Privacy

- First enable shows a consent dialog: the plugin submits your RSN, location and
  friends/clan data to a server not controlled by RuneLite. Decline = plugin
  disabled; it cannot run without granted consent.
- Wilderness / PvP worlds: location toggles default off. When off, heartbeats
  continue (you still see friends) but your position is sent as `null` and never
  shared.
- Offline friends: last known position stays on the map, faded, for 30 seconds.
- Visibility is always enforced by the backend: mutual friends only, same clan
  or same friends chat, target toggles respected.

## Notes

- World map only shows the surface plane; caves/instances are not rendered.
- Minimap friend dots are rendered by the game itself; the plugin adds nothing
  there.
