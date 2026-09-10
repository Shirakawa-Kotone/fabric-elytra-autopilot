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

### Trajectory Prediction
Enabled by default under **Trajectory Prediction**. The mod does not extend your current heading in a straight line to decide where you are going: it integrates the vanilla elytra movement code (`LivingEntity.updateFallFlyingMovement`) tick by tick from your current position and velocity. That matters, because on an elytra the attitude is not the flight path angle - measured with this model, a **30° nose-down attitude only descends at about 9°** at cruise speed, and a nose-up attitude bleeds speed until it stops climbing. A straight-line guess therefore both misses terrain below the path and mis-aims every descent.

Both systems below use that prediction. Turning the option off falls back to the simpler ray/geometric behaviour.

### Obstacle Avoidance
Enabled by default and configurable under **Obstacle Avoidance** in the config screen. While Auto Flight is running the mod flies the predicted path ahead (5 seconds of flight by default) and, if it ends in terrain, searches for an escape attitude by simulating each candidate with the same model. It pitches the nose **up** towards the shallowest climb that actually clears the obstacle; if there is nothing to climb over - for example underneath a Nether ceiling - it pitches the nose **down** instead. The HUD shows what it is doing (`Avoiding terrain: climbing (74 blocks ahead)`).

Note that a pitch-only escape is limited by physics: from cruise speed an elytra cannot climb very steeply without first trading speed for height, so a wall that rears up immediately in front may be unavoidable by pitching alone.

### Landing
`/flyto` and `/land` normally lose height by circling around the target. With **Direct Landing** enabled (default) the mod instead checks whether the destination can be reached in a straight line: the required descent angle has to be within the configured glide/max angle, there has to be enough horizontal room, and the predicted approach must not run into terrain. If all of that holds, the aircraft flies straight in and flares shortly before touchdown.

The attitude used for the descent is not the geometric angle - it is solved by inverting the movement model (a bisection on "what attitude actually descends at this angle", measured by simulation). With the default settings a straight-in approach touches down within roughly 30 blocks of the destination with a vertical speed under 10 m/s, which is inside the range where vanilla clamps fall distance.

If the approach is too steep or blocked, or if the path becomes blocked while descending, it falls back to the original circling descent - so nothing is lost when there is not enough space.

Useful settings for this live under **Flight Profile**: `Direct Landing`, `Direct Landing Glide Angle` (the descent angle a manual `/land` aims for), `Direct Landing Max Angle` (steepest allowed straight-in approach), `Direct Landing Min Distance`, `Direct Landing Flare Height` and `Direct Landing Flare Angle`.

### Risky Landing
Disabled by default but can be enabled in the config. When active, this setting modifies the *circling* landing behavior to a riskier approach, nosediving until the last moment before pulling up. Not recommended for laggy servers or clients!

## Xaero Minimap Support
If you prefer not to use the built-in `/flylocation` command or are already managing waypoints with Xaero Minimap, good news! You can now use `/flyto` and `/takeoff` directly with your Xaero Minimap waypoints.
