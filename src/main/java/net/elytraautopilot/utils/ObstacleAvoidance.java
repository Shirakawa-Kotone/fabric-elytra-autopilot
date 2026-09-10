package net.elytraautopilot.utils;

import net.elytraautopilot.config.ModConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Forward-looking terrain and obstacle detection for the elytra autopilot.
 *
 * <p>
 * Every client tick a small fan of ray casts is fired along the flight path. If
 * the path is blocked, the smallest pitch change that clears the obstacle is
 * selected by pitching the nose up (the preferred escape) or, when there is no
 * usable climb - for example underneath a Nether ceiling - by pitching the nose
 * down.
 *
 * <p>
 * The class only <em>decides</em> what to do; applying the pitch is left to the
 * autopilot so that obstacle avoidance can be layered on top of both the
 * classic and the strategy flight controllers.
 */
public final class ObstacleAvoidance {

    /** What the aircraft should do about the obstacle it sees. */
    public enum Action {
        /** No obstacle in the way. */
        NONE("text.elytraautopilot.hud.avoidance.none"),
        /** Pitch the nose up. */
        CLIMB("text.elytraautopilot.hud.avoidance.climb"),
        /** Pitch the nose down. */
        DESCEND("text.elytraautopilot.hud.avoidance.descend"),
        /** Boxed in: hold whichever pitch buys the most room. */
        EVADE("text.elytraautopilot.hud.avoidance.evade");

        private final String translationKey;

        Action(String translationKey) {
            this.translationKey = translationKey;
        }

        public String translationKey() {
            return this.translationKey;
        }
    }

    /** Below this speed (blocks per tick) obstacle detection is pointless. */
    private static final double MIN_SPEED = 0.12;
    /** Never look more than this short/long distance ahead, in blocks. */
    private static final double MIN_LOOKAHEAD = 24.0;
    private static final double MAX_LOOKAHEAD = 320.0;
    /** Angular resolution of the escape search, in degrees. */
    private static final double SEARCH_STEP = 7.5;
    /**
     * Hits closer than this are ignored: the aircraft is already inside a block.
     */
    private static final double MIN_HIT_DISTANCE = 0.75;
    /** Lateral half-width of the ray fan, capped in degrees. */
    private static final double MIN_FAN_ANGLE = 1.0;
    private static final double MAX_FAN_ANGLE = 12.0;

    private static Action action = Action.NONE;
    private static float targetPitch = 0.0f;
    private static double obstacleDistance = 0.0;
    private static double lookaheadDistance = 0.0;
    private static boolean active = false;

    private ObstacleAvoidance() {
    }

    /**
     * Looks for obstacles in the flight path and picks an escape pitch.
     *
     * @param player
     *            the flying player
     * @param speedPerTick
     *            the last measured speed in blocks per tick
     * @param lookaheadCap
     *            upper bound for the scan distance. A straight-in landing uses this
     *            to shorten the scan so that the ground it intends to touch down on
     *            is not mistaken for an obstacle.
     */
    public static void tick(Player player, double speedPerTick, double lookaheadCap) {
        clear();

        if (player == null || !ModConfig.INSTANCE.obstacleAvoidance) {
            return;
        }

        Level level = player.level();
        if (level == null) {
            return;
        }

        Vec3 velocity = player.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        double speed = Math.max(speedPerTick, horizontalSpeed);
        if (speed < MIN_SPEED) {
            return;
        }

        double lookahead = Mth.clamp(speed * ModConfig.INSTANCE.avoidanceLookahead * 20.0, MIN_LOOKAHEAD,
                MAX_LOOKAHEAD);
        if (lookaheadCap < lookahead) {
            lookahead = Math.max(4.0, lookaheadCap);
        }
        lookaheadDistance = lookahead;

        Vec3 origin = player.getEyePosition().subtract(0.0, 0.5, 0.0);
        float yaw = player.getYRot();
        float pitch = player.getXRot();

        double currentClearance = clearance(level, player, origin, yaw, pitch, lookahead);
        if (currentClearance >= lookahead) {
            // The path is already clear - leave the flight controls alone.
            return;
        }
        obstacleDistance = currentClearance;

        // 1) Pull up. The shallowest climb that fully clears the obstacle wins,
        // so the aircraft deviates as little as possible.
        double bestClimbPitch = Double.NaN;
        double bestClimbClearance = -1.0;
        double maxClimb = Mth.clamp(ModConfig.INSTANCE.avoidanceMaxClimbAngle, 0.0, 85.0);
        for (double delta = SEARCH_STEP; delta <= maxClimb + 1.0e-6; delta += SEARCH_STEP) {
            float candidate = (float) (pitch - delta);
            if (candidate < -89.0f) {
                break;
            }
            double candidateClearance = clearance(level, player, origin, yaw, candidate, lookahead);
            if (candidateClearance > bestClimbClearance) {
                bestClimbClearance = candidateClearance;
                bestClimbPitch = candidate;
            }
            if (candidateClearance >= lookahead) {
                break;
            }
        }
        if (bestClimbClearance >= lookahead) {
            set(Action.CLIMB, bestClimbPitch);
            return;
        }

        // 2) Nothing to climb over (or the ceiling is too low): dive under it.
        double bestDescentPitch = Double.NaN;
        double bestDescentClearance = -1.0;
        double maxDescent = Mth.clamp(ModConfig.INSTANCE.avoidanceMaxDescentAngle, 0.0, 85.0);
        for (double delta = SEARCH_STEP; delta <= maxDescent + 1.0e-6; delta += SEARCH_STEP) {
            float candidate = (float) (pitch + delta);
            if (candidate > 89.0f) {
                break;
            }
            double candidateClearance = clearance(level, player, origin, yaw, candidate, lookahead);
            if (candidateClearance > bestDescentClearance) {
                bestDescentClearance = candidateClearance;
                bestDescentPitch = candidate;
            }
            if (candidateClearance >= lookahead) {
                break;
            }
        }
        if (bestDescentClearance >= lookahead) {
            set(Action.DESCEND, bestDescentPitch);
            return;
        }

        // 3) Boxed in: keep whatever buys the most room, still preferring a climb.
        if (!Double.isNaN(bestClimbPitch) && bestClimbClearance >= bestDescentClearance) {
            set(Action.CLIMB, bestClimbPitch);
        } else if (!Double.isNaN(bestDescentPitch)) {
            set(Action.DESCEND, bestDescentPitch);
        } else {
            set(Action.EVADE, pitch);
        }
    }

    private static void set(Action newAction, double pitch) {
        action = newAction;
        targetPitch = Mth.clamp((float) pitch, -90.0f, 90.0f);
        active = newAction != Action.NONE;
    }

    /**
     * Distance along a straight flight path before terrain is hit. Unlike the scan
     * used by {@link #tick} no ray is aimed below the path, so the ground a landing
     * approach is aiming for is not reported as an obstacle.
     */
    public static double landingPathClearance(Player player, float yaw, double pitchDegrees, double distance) {
        if (player == null || distance <= 0.0) {
            return Math.max(distance, 0.0);
        }
        Level level = player.level();
        if (level == null) {
            return distance;
        }
        Vec3 origin = player.getEyePosition().subtract(0.0, 0.5, 0.0);
        float pitch = (float) pitchDegrees;
        double sideAngle = Mth.clamp(Math.toDegrees(Math.atan2(4.0, Math.max(distance, 1.0))), 1.0, 5.0);
        double minimum = rayClearance(level, player, origin, yaw, pitch, distance);
        if (minimum < distance) {
            minimum = Math.min(minimum, rayClearance(level, player, origin, yaw - (float) sideAngle, pitch, distance));
            minimum = Math.min(minimum, rayClearance(level, player, origin, yaw + (float) sideAngle, pitch, distance));
        }
        return minimum;
    }

    /**
     * Casts the ray fan along a flight path and returns the smallest distance at
     * which terrain is hit.
     */
    private static double clearance(Level level, Player player, Vec3 origin, float yaw, float pitch, double distance) {
        double lateralMargin = Mth.clamp(ModConfig.INSTANCE.avoidanceClearance, 0.5, 32.0);
        double sideAngle = Mth.clamp(Math.toDegrees(Math.atan2(lateralMargin, Math.max(distance, 1.0))), MIN_FAN_ANGLE,
                MAX_FAN_ANGLE);

        double minimum = rayClearance(level, player, origin, yaw, pitch, distance);
        if (minimum < distance) {
            minimum = Math.min(minimum, rayClearance(level, player, origin, yaw - (float) sideAngle, pitch, distance));
            minimum = Math.min(minimum, rayClearance(level, player, origin, yaw + (float) sideAngle, pitch, distance));
        }

        // Probe slightly below the flight path so terrain rising into it is caught.
        double belowAngle = Mth.clamp(Math.toDegrees(Math.atan2(lateralMargin, Math.max(distance, 1.0))), MIN_FAN_ANGLE,
                15.0);
        minimum = Math.min(minimum, rayClearance(level, player, origin, yaw, pitch + (float) belowAngle, distance));
        return minimum;
    }

    private static double rayClearance(Level level, Player player, Vec3 origin, float yaw, float pitch,
            double distance) {
        Vec3 direction = Vec3.directionFromRotation(pitch, yaw);
        Vec3 end = origin.add(direction.scale(distance));
        BlockHitResult hit = level
                .clip(new ClipContext(origin, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return distance;
        }
        double hitDistance = origin.distanceTo(hit.getLocation());
        if (hitDistance <= MIN_HIT_DISTANCE) {
            // Already inside a block - not something a pitch change can fix.
            return distance;
        }
        return hitDistance;
    }

    /**
     * Moves the player's pitch towards {@link #getTargetPitch()} by at most
     * {@code maxDegrees}.
     */
    public static void steer(Player player, double maxDegrees) {
        if (!active || player == null || maxDegrees <= 0.0) {
            return;
        }
        float pitch = player.getXRot();
        float difference = targetPitch - pitch;
        if (Math.abs(difference) <= maxDegrees) {
            player.setXRot(targetPitch);
        } else {
            player.setXRot((float) (pitch + Math.copySign(maxDegrees, difference)));
        }
    }

    /** Forgets any previously detected obstacle. */
    public static void clear() {
        action = Action.NONE;
        targetPitch = 0.0f;
        obstacleDistance = 0.0;
        lookaheadDistance = 0.0;
        active = false;
    }

    public static boolean isActive() {
        return active;
    }

    public static Action getAction() {
        return action;
    }

    public static float getTargetPitch() {
        return targetPitch;
    }

    /** Distance to the obstacle that triggered the escape, in blocks. */
    public static double getObstacleDistance() {
        return obstacleDistance;
    }

    /** How far ahead the aircraft is looking, in blocks. */
    public static double getLookaheadDistance() {
        return lookaheadDistance;
    }
}
