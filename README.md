# Friends Map

Friends Map shows your friends, clan members and friends-chat members as live
dots on the OSRS world map.

- World map: colored icons per relationship (friend / clan / friends chat),
  with the friend's name in small text next to the dot
- Minimap: game renders friend dots natively — plugin adds nothing there
- Customizable dot colors and dot size

## How it works

Each player running the plugin periodically sends their own position to the
backend server (`https://map.mss54.com`), together with friends list, clan
identity and friends-chat identity. The server computes who is allowed to see
whom and returns visible people with positions.

Privacy toggles:

- `visible to clan` — only people in the same clan channel see you
- `visible to friends` — only mutual friends see you
- `visible to friends chat` — only people in the same friends chat see you

## Simulated friends (development)

- `simulateFriends` — always fake friends, zero network
- `simulateWhenOffline` (default on) — automatically falls back to simulated
  friends when the backend cannot be reached, so map and minimap stay testable

## Debug overlay

Advanced > `debug` enables an overlay showing:

- your friends list, clan members and friends chat members
- incoming backend data (health probe result + raw body)
- the friend data currently displayed (name, world, plane, relation)

## Quick setup

1. Enable the plugin.
2. Toggle `simulateFriends` on for instant fake data, or leave it off to use
   the backend at `https://map.mss54.com` (hardcoded for now).
3. Adjust dot colors and size in "Colors & Sizes".
4. Enable Advanced > `debug` to inspect rosters and backend traffic.

## Notes

- The world map only renders the surface plane. Friends in caves, instances or
  other planes are not rendered yet.
- The backend auth token is managed internally by the plugin — no user-facing
  API key setting.
