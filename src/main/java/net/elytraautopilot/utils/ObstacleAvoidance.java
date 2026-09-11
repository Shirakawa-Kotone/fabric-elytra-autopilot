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
 * Every client tick the flight path is examined. By default the path is
 * <em>predicted</em> with the vanilla elytra physics (see
 * {@link ElytraTrajectory}), so the aircraft sees where it would really go - a
 * level attitude still sinks, a pull-up trades speed for height - instead of
 * assuming it keeps flying in a straight line. With
 * {@code trajectoryPrediction} disabled a fan of ray casts along the current
 * heading is used instead.
 *
 * <p>
 * If the path is blocked, the smallest pitch change that clears the obstacle is
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
    /** Evaluator result meaning "nothing is in the way". */
    private static final double CLEAR = Double.MAX_VALUE;
    /**
     * How long the escape keeps the pitch after the path clears. Handing control
     * straight back lets the normal controller pitch into the terrain again, which
     * makes the two controllers take turns and the view shake.
     */
    private static final int MIN_HOLD_TICKS = 20;
    /**
     * How far past the obstacle an escape must fly to count. A steep pull-up burns
     * all the speed and stalls: it travels almost nowhere, so it never hits
     * anything, and without this an attitude that just hangs in the air would be
     * mistaken for a way around the obstacle.
     */
    private static final double ESCAPE_MARGIN = 24.0;
    /** Keep the committed escape unless another one is clearly better. */
    private static final double COMMITMENT_KEEP = 0.85;
    /**
     * Ticks between escape searches. The engagement test still runs every tick, but
     * a search simulates a dozen attitudes, and the chosen attitude is absolute, so
     * recomputing it a few times a second is plenty.
     */
    private static final int SEARCH_INTERVAL_TICKS = 3;

    /** What an attitude would do: how far it flies and whether it hit something. */
    private record Probe(double travelled, boolean hit) {
    }

    /** Scores an attitude by how far it flies before hitting terrain. */
    private interface EscapeEvaluator {
        /** Flies the attitude and reports the outcome. */
        Probe evaluate(float pitch);
    }

    /**
     * Scores an attitude for the escape search: the distance it flies before
     * hitting terrain, or {@link #CLEAR} when it gets past the obstacle.
     */
    private interface EscapeScore {
        double evaluate(float pitch);
    }

    private static Action action = Action.NONE;
    private static float targetPitch = 0.0f;
    private static double obstacleDistance = 0.0;
    private static double lookaheadDistance = 0.0;
    private static boolean active = false;
    private static int holdTicks = 0;
    private static int searchCooldown = 0;

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
     * @param normalAttitude
     *            the attitude the autopilot would fly without any avoidance, or
     *            {@link Float#NaN} when there is none. Avoidance only hands the
     *            pitch back once this attitude is safe too.
     */
    public static void tick(Player player, double speedPerTick, double lookaheadCap, float normalAttitude) {
        resetOutputs();

        if (player == null || !ModConfig.INSTANCE.obstacleAvoidance) {
            release();
            return;
        }

        Level level = player.level();
        if (level == null) {
            release();
            return;
        }

        Vec3 velocity = player.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        double speed = Math.max(speedPerTick, horizontalSpeed);
        if (speed < MIN_SPEED) {
            release();
            return;
        }

        double lookahead = Mth.clamp(speed * ModConfig.INSTANCE.avoidanceLookahead * 20.0, MIN_LOOKAHEAD,
                MAX_LOOKAHEAD);
        if (lookaheadCap < lookahead) {
            lookahead = Math.max(4.0, lookaheadCap);
        }
        lookaheadDistance = lookahead;

        if (ModConfig.INSTANCE.trajectoryPrediction) {
            tickPredicted(player, lookahead, normalAttitude);
        } else {
            tickRaycast(level, player, lookahead, normalAttitude);
        }
    }

    /**
     * Scores attitudes by flying them with the vanilla elytra physics and checking
     * the resulting path for terrain.
     */
    private static void tickPredicted(Player player, double lookahead, float normalAttitude) {
        double margin = Mth.clamp(ModConfig.INSTANCE.avoidanceClearance, 0.0, 32.0);
        update(player.getXRot(), normalAttitude, pitch -> {
            ElytraTrajectory.Result result = ElytraTrajectory.scan(player, pitch, ElytraTrajectory.MAX_TICKS, lookahead,
                    margin);
            return new Probe(result.travelledDistance, result.collision);
        });
    }

    /** Scores attitudes with a fan of ray casts along the current heading. */
    private static void tickRaycast(Level level, Player player, double lookahead, float normalAttitude) {
        Vec3 origin = player.getEyePosition().subtract(0.0, 0.5, 0.0);
        float yaw = player.getYRot();
        update(player.getXRot(), normalAttitude, pitch -> {
            double distance = clearance(level, player, origin, yaw, pitch, lookahead);
            return new Probe(distance, distance < lookahead);
        });
    }

    /**
     * Engagement logic shared by both detectors.
     *
     * <p>
     * Avoidance engages as soon as the path is blocked. It only lets go again once
     * the path has been clear <em>and</em> the attitude the autopilot wants to fly
     * is safe as well, held for {@link #MIN_HOLD_TICKS}. Releasing earlier makes
     * the normal controller immediately pitch back into the terrain, so the two
     * take turns and the aircraft nods up and down.
     */
    private static void update(float pitch, float normalAttitude, EscapeEvaluator evaluator) {
        Probe current = evaluator.evaluate(pitch);
        boolean blocked = current.hit();
        if (blocked) {
            obstacleDistance = current.travelled();
        }

        if (!blocked) {
            if (!active) {
                return;
            }
            boolean autopilotSafe = Float.isNaN(normalAttitude) || !evaluator.evaluate(normalAttitude).hit();
            if (autopilotSafe) {
                if (--holdTicks <= 0) {
                    release();
                }
                return;
            }
            // The autopilot would fly into terrain from here: hold the safe attitude
            // the aircraft already has instead of letting it pitch down.
            holdTicks = MIN_HOLD_TICKS;
            set(pitch <= 0.0f ? Action.CLIMB : Action.DESCEND, pitch);
            return;
        }

        holdTicks = MIN_HOLD_TICKS;
        if (active && action != Action.NONE && --searchCooldown > 0) {
            // Keep the attitude already chosen; a new plan a few times a second is
            // enough and keeps the cost of the search off the frame budget.
            return;
        }
        searchCooldown = SEARCH_INTERVAL_TICKS;
        double required = current.travelled() + ESCAPE_MARGIN;
        searchEscape(pitch, candidate -> {
            Probe probe = evaluator.evaluate(candidate);
            if (probe.hit()) {
                return probe.travelled();
            }
            // Getting clear is not enough: the escape has to carry the aircraft past
            // the obstacle it is avoiding.
            return probe.travelled() >= required ? CLEAR : probe.travelled();
        });
    }

    /**
     * Shared escape search: the shallowest attitude that carries the aircraft past
     * the obstacle wins, climbing first, then descending.
     *
     * <p>
     * Candidates are absolute attitudes, not offsets from the current one.
     * Searching relative to the current pitch makes the target chase itself: every
     * tick the chosen attitude comes out a little steeper than the aircraft already
     * is, so it keeps pulling up until it stalls and the nose never settles.
     */
    private static void searchEscape(float currentPitch, EscapeScore score) {
        double maxClimb = Mth.clamp(ModConfig.INSTANCE.avoidanceMaxClimbAngle, 0.0, 85.0);
        double maxDescent = Mth.clamp(ModConfig.INSTANCE.avoidanceMaxDescentAngle, 0.0, 85.0);

        // 1) Pull up: shallowest climb first, so the aircraft deviates least.
        double bestClimbPitch = Double.NaN;
        double bestClimbDistance = -1.0;
        for (double candidate = -SEARCH_STEP; candidate >= -maxClimb - 1.0e-6; candidate -= SEARCH_STEP) {
            double distance = score.evaluate((float) candidate);
            if (distance == CLEAR) {
                set(Action.CLIMB, candidate);
                return;
            }
            if (distance > bestClimbDistance) {
                bestClimbDistance = distance;
                bestClimbPitch = candidate;
            }
        }

        // 2) Nothing to climb over (or the ceiling is too low): dive under it.
        double bestDescentPitch = Double.NaN;
        double bestDescentDistance = -1.0;
        for (double candidate = SEARCH_STEP; candidate <= maxDescent + 1.0e-6; candidate += SEARCH_STEP) {
            double distance = score.evaluate((float) candidate);
            if (distance == CLEAR) {
                set(Action.DESCEND, candidate);
                return;
            }
            if (distance > bestDescentDistance) {
                bestDescentDistance = distance;
                bestDescentPitch = candidate;
            }
        }

        // 3) Boxed in: keep whatever buys the most room, still preferring a climb.
        // Stay with the attitude already committed to unless another one is clearly
        // better, otherwise the escape flickers between candidates every tick and
        // that shows up as the nose shaking.
        double bestPitch = Double.NaN;
        double bestDistance = -1.0;
        Action bestAction = Action.EVADE;
        if (!Double.isNaN(bestClimbPitch) && bestClimbDistance >= bestDescentDistance) {
            bestPitch = bestClimbPitch;
            bestDistance = bestClimbDistance;
            bestAction = Action.CLIMB;
        } else if (!Double.isNaN(bestDescentPitch)) {
            bestPitch = bestDescentPitch;
            bestDistance = bestDescentDistance;
            bestAction = Action.DESCEND;
        }

        if (active && action != Action.NONE && !Double.isNaN(bestPitch)
                && score.evaluate(targetPitch) >= bestDistance * COMMITMENT_KEEP) {
            set(action, targetPitch);
            return;
        }

        if (!Double.isNaN(bestPitch)) {
            set(bestAction, bestPitch);
        } else {
            set(Action.EVADE, currentPitch);
        }
    }

    private static void set(Action newAction, double pitch) {
        action = newAction;
        targetPitch = Mth.clamp((float) pitch, -90.0f, 90.0f);
        active = newAction != Action.NONE;
    }

    /**
     * True when a straight-in approach at the given attitude reaches (or gets close
     * to) the landing spot instead of hitting terrain on the way, using either the
     * predicted flight path or, when prediction is disabled, a ray along the
     * approach.
     */
    public static boolean landingCorridorClear(Player player, float yaw, double pitchDegrees, double pathLength) {
        if (player == null || pathLength <= 0.0) {
            return true;
        }
        if (ModConfig.INSTANCE.trajectoryPrediction) {
            ElytraTrajectory.Result result = ElytraTrajectory.scan(player, (float) pitchDegrees,
                    ElytraTrajectory.MAX_TICKS, pathLength, ModConfig.INSTANCE.avoidanceClearance);
            if (!result.collision) {
                return true;
            }
            // The path is meant to end on the ground at the landing spot; only
            // terrain that shows up well before it counts as blocking.
            return result.travelledDistance >= pathLength * 0.7;
        }
        return landingPathClearance(player, yaw, pitchDegrees, pathLength) >= pathLength * 0.7;
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

    /** Forgets any previously detected obstacle and drops the engagement. */
    public static void clear() {
        resetOutputs();
        release();
    }

    /** Clears the per-tick readouts, keeping the engagement and its target. */
    private static void resetOutputs() {
        obstacleDistance = 0.0;
        lookaheadDistance = 0.0;
    }

    /** Hands the pitch back to the autopilot. */
    private static void release() {
        active = false;
        holdTicks = 0;
        searchCooldown = 0;
        action = Action.NONE;
        targetPitch = 0.0f;
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
