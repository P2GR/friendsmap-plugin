# Friends Map

Shows friends, clan members and friends-chat members as live icons on the OSRS
world map. Each icon is a colored dot (green = friend, orange = clan,
purple = friends chat) with the name in small text next to it. A scrollable
list of the visible players (name and current world) is pinned to the world
map's top-left corner; click an entry to focus the map on that player.

## How it works

- Every ~2 seconds the plugin sends your position, world, friends list, clan and
  friends chat to the backend at `https://map.mss54.com` (hardcoded).
- The backend replies with the people you are allowed to see; the plugin draws
  them on the world map. Server never pushes — data comes back in the heartbeat
  response.
- Auth token is managed internally (auto-register), no user-facing API key.

## Player list

The world map shows a scrollable list of the visible players in its top-left
corner. Rows read `Playername - W366`; left-click a row to focus the map on
that player, same as the map's "Focus on" option. With the `Player list
filter` set to Show All the list is grouped by relation:

    Friends (1)
    Zeep - W366

    Clan (2)
    P2GR - W466
    MCP - W353

Players on your world are listed first, then alphabetically. Sections with
nobody in them (for example Friends Chat) are hidden. The list is built from
the heartbeat data — it adds no network traffic.

## Config

**Display**
- `showOnWorldMap` — draw world map icons (default on)
- `showWorldMapNames` — name in small text next to each dot (default on)
- `showWorldMapWorld` — world in map labels, e.g. `Playername - W366` (default on)
- `showPlayerList` — scrollable player list in the world map's top-left corner
  (default on); rows read `Playername - W366`, players on your world first
- `playerListFilter` — Show All / Show Clan / Show Friends / Show Friends Chat;
  Show All groups under `Friends (n)` / `Clan (n)` / `Friends Chat (n)` headers,
  empty sections hidden (default Show All)
- `playerListRows` — rows shown before the list scrolls (3–25)
- `playerListFontSize` — list text size (10–24)
- `dotColorFriend` / `dotColorClan` / `dotColorFriendsChat` — dot colors
- `dotSize` — dot diameter (4–24)

**Visibility**
- `visibilityClan` / `visibilityFriends` / `visibilityFriendsChat` — who may see you
- `sendLocationWilderness` — share position while in the Wilderness (default off,
  confirmed with a warning dialog when enabled)
- `sendLocationPvpWorlds` — share position on PvP worlds (default off,
  confirmed with a warning dialog when enabled)

**Advanced**
- `debug` — minimal overlay: server connection (Live/Offline) and
  displayed friends grouped by relation

## Privacy

- No in-game consent dialog is shown. Instead the plugin declares a `warning` in
  its **plugin-hub commit descriptor** (`runelite/plugin-hub/plugins/friends-map`),
  which the Plugin Hub displays to users before they install and run the plugin:
  it submits your IP address, RSN, location and friends/clan data to a server not
  controlled by RuneLite.
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

## Development

- `./gradlew build` — compile and package the plugin jar.
- `./gradlew run` — start the RuneLite dev client with the plugin loaded.
