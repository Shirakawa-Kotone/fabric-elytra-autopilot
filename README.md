# Elytra AutoPilot

**This mod requires [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api).**
***

This is a fork of TheMegax's implementation of the mod. However, TheMegax quit the project. HumanoidSandvichDispenser and R4z0rX contributed to the 1.20 implementation. Many people were involved, so check the mod author list for a full list of contributors.

Here is the link to TheMegax's mod page: [https://www.curseforge.com/minecraft/mc-mods/elytra-autopilot](https://www.curseforge.com/minecraft/mc-mods/elytra-autopilot).

## How to Use
Press the assigned key (default "R") while flying at a sufficient altitude to enable 'Auto Flight'. In Auto Flight mode, the mod will adjust your pitch between ascending and descending, resulting in a net altitude gain.

To open the config screen and enable Mod Menu, go into the mod menu and open the configuration screen there. 

## /flyto Command
**Syntax:** `/flyto X Z` or `/flyto <name>`

While flying, use this command to automatically fly to the specified coordinates. When near the destination, the mod will attempt to slow you down by circling around the target to avoid fall damage; if there is enough room for a straight-in descent it will fly directly to the target instead (see **Landing** below). You can disable this at any time by turning off Auto Flight or toggling the setting in the config.

## /takeoff Command
**Syntax:** `/takeoff` or `/takeoff X Z` or `/takeoff <name>`

If you have an Elytra equipped and fireworks in either your main or off-hand, this command will launch you upwards to a configurable height (default: 180 blocks) before activating Auto Flight. If coordinates are provided, it will then use `/flyto` to navigate to the specified location automatically.

## /flylocation Command
**Syntax:** `/flylocation set <name> X Z` or `/flylocation remove <name>`

Use this command to add or remove quick fly locations.

## /land Command
**Syntax:** `/land`

While flying, use this command to force a landing at any time. Useful for quickly returning to the ground!

### Obstacle Avoidance
Enabled by default and configurable under **Obstacle Avoidance** in the config screen. While Auto Flight is running, the mod looks along the flight path with a fan of ray casts (5 seconds of flight by default). If terrain is in the way it pitches the nose **up** towards the shallowest climb that clears the obstacle; if there is nothing to climb over - for example underneath a Nether ceiling - it pitches the nose **down** instead. The HUD shows what it is doing (`Avoiding terrain: climbing (74 blocks ahead)`). Turn it off, shorten the look-ahead time, change the safety clearance or limit the climb/descent angles if you prefer.

### Landing
`/flyto` and `/land` normally lose height by circling around the target. With **Direct Landing** enabled (default) the mod instead checks whether the destination can be reached in a straight line: the required descent angle has to be within the configured glide/max angle, there has to be enough horizontal room, and the approach corridor must be free of terrain. If all of that holds, the aircraft flies straight in, tracks the glide path and flares shortly before touchdown. If the approach is too steep or blocked, or if the path becomes blocked while descending, it falls back to the original circling descent - so nothing is lost when there is not enough space.

Useful settings for this live under **Flight Profile**: `Direct Landing`, `Direct Landing Glide Angle` (the descent angle a manual `/land` aims for), `Direct Landing Max Angle` (steepest allowed straight-in approach), `Direct Landing Min Distance`, `Direct Landing Flare Height` and `Direct Landing Flare Angle`.

### Risky Landing
Disabled by default but can be enabled in the config. When active, this setting modifies the *circling* landing behavior to a riskier approach, nosediving until the last moment before pulling up. Not recommended for laggy servers or clients!

## Xaero Minimap Support
If you prefer not to use the built-in `/flylocation` command or are already managing waypoints with Xaero Minimap, good news! You can now use `/flyto` and `/takeoff` directly with your Xaero Minimap waypoints.
