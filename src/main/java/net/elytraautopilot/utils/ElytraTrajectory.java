package net.elytraautopilot.utils;

import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Predicts where an elytra will actually fly instead of extending the current
 * heading in a straight line.
 *
 * <p>
 * {@link #scan} and {@link #endPosition} are direct ports of the vanilla
 * movement code ({@code LivingEntity.updateFallFlyingMovement} in 26.2, called
 * from {@code travelFallFlying}), so a predicted path has the real behaviour of
 * an elytra: it sinks while the attitude is held level, it trades speed for
 * height when the nose comes up, and it accelerates when it dives.
 *
 * <p>
 * That difference matters. Measured with this model, a 30 degree nose-down
 * attitude only descends at about 9 degrees at cruise speed, and a nose-up
 * attitude bleeds speed until the aircraft stops climbing at all. Anything that
 * aims by treating the attitude as the flight path angle - as a straight line
 * extension does - lands long or flies into terrain.
 *
 * <p>
 * The port is validated against the mod's own precomputed strategy waveforms:
 * replaying {@code climb.csv} gives a net +1.55 m/s and {@code cruise.csv}
 * gives 33 m/s, matching the numbers the waveforms were optimised for.
 */
public final class ElytraTrajectory {

    // Constants copied from LivingEntity.updateFallFlyingMovement (26.2).
    private static final double HORIZONTAL_DRAG = 0.99;
    private static final double VERTICAL_DRAG = 0.98;
    private static final double LIFT_FACTOR = 0.75;
    private static final double DIVE_CONVERSION = 0.1;
    private static final double CLIMB_CONVERSION = 0.04;
    private static final double CLIMB_VERTICAL = 3.2;
    private static final double ALIGNMENT = 0.1;

    /** Hard upper bound on the simulated horizon. */
    public static final int MAX_TICKS = 400;
    /**
     * Terrain is sampled every this many ticks along a predicted path. Every
     * segment is swept in full, so a thin wall cannot be stepped over; a coarser
     * step only means the impact is noticed a little later along the path.
     */
    private static final int COLLISION_STEP = 4;
    /** The player's body is approximated by two points at these heights. */
    private static final double BODY_LOW = 0.6;
    private static final double BODY_HIGH = 1.5;
    /** Slow falling clamps gravity to this value. */
    private static final double SLOW_FALLING_GRAVITY = 0.01;
    private ElytraTrajectory() {
    }

    /** Outcome of flying a fixed attitude for a while. */
    public static final class Result {
        /** Whether the path ran into terrain (or the bottom of the world). */
        public final boolean collision;
        /**
         * Whether the path covered the requested distance. Informational only: a climb
         * bleeds speed and may not cover the whole scan, which is not a collision.
         */
        public final boolean reachedGoal;
        /** Distance flown, in blocks. */
        public final double travelledDistance;
        /** Where the path stopped: the impact point, or the end of the horizon. */
        public final Vec3 endPosition;

        private Result(boolean collision, boolean reachedGoal, double travelledDistance, Vec3 endPosition) {
            this.collision = collision;
            this.reachedGoal = reachedGoal;
            this.travelledDistance = travelledDistance;
            this.endPosition = endPosition;
        }

        /**
         * True when the simulated path never hit terrain. A path that used up the
         * simulated ticks counts as clear: nothing was in the way for as far as the
         * aircraft could be flown, and the scan is redone every tick.
         */
        public boolean isClear() {
            return !this.collision;
        }
    }

    /**
     * A predicted flight path kept as a polyline, so it can be drawn on screen.
     */
    public static final class Path {
        /** Sampled positions along the path, starting at the aircraft. */
        public final List<Vec3> points;
        /** Whether the path ran into terrain (or the bottom of the world). */
        public final boolean collision;
        /** Distance flown along the path, in blocks. */
        public final double travelledDistance;

        private Path(List<Vec3> points, boolean collision, double travelledDistance) {
            this.points = points;
            this.collision = collision;
            this.travelledDistance = travelledDistance;
        }
    }

    /** Internal working set shared by {@link #scan} and {@link #trace}. */
    private static final class Simulation {
        private List<Vec3> points;
        private boolean collision;
        private boolean reachedGoal;
        private double travelled;
        private Vec3 endPosition = Vec3.ZERO;
    }

    /**
     * Flies the player forward with a fixed attitude and reports what the path runs
     * into.
     *
     * @param player
     *            the flying player
     * @param pitchDegrees
     *            the attitude to hold, in the usual Minecraft sense (positive is
     *            nose down)
     * @param maxTicks
     *            hard limit on simulated ticks
     * @param maxDistance
     *            stop once this much distance has been covered, in blocks
     * @param clearance
     *            extra head room kept above the player, in blocks
     */
    public static Result scan(Player player, float pitchDegrees, int maxTicks, double maxDistance, double clearance) {
        Simulation simulation = simulate(player, pitchDegrees, maxTicks, maxDistance, clearance, false);
        return new Result(simulation.collision, simulation.reachedGoal, simulation.travelled, simulation.endPosition);
    }

    /**
     * The same simulation as {@link #scan}, but the flown positions are kept as a
     * polyline. Used to show the predicted trajectory on screen.
     *
     * @param player
     *            the flying player
     * @param pitchDegrees
     *            the attitude to hold, in the usual Minecraft sense (positive is
     *            nose down)
     * @param maxTicks
     *            hard limit on simulated ticks
     * @param maxDistance
     *            stop once this much distance has been covered, in blocks
     * @param clearance
     *            extra head room kept above the player, in blocks (0 for the real
     *            flight path)
     */
    public static Path trace(Player player, float pitchDegrees, int maxTicks, double maxDistance, double clearance) {
        Simulation simulation = simulate(player, pitchDegrees, maxTicks, maxDistance, clearance, true);
        return new Path(simulation.points, simulation.collision, simulation.travelled);
    }

    private static Simulation simulate(Player player, float pitchDegrees, int maxTicks, double maxDistance,
            double clearance, boolean collectPoints) {
        Simulation simulation = new Simulation();
        if (collectPoints) {
            simulation.points = new ArrayList<>();
            simulation.points.add(player.position());
        }

        int limit = Mth.clamp(maxTicks, 1, MAX_TICKS);
        Vec3 velocity = player.getDeltaMovement();
        double positionX = player.getX();
        double positionY = player.getY();
        double positionZ = player.getZ();
        float yaw = player.getYRot();
        double gravity = effectiveGravity(player);

        Level level = player.level();
        double minY = level.getMinY();

        double checkX = positionX;
        double checkY = positionY;
        double checkZ = positionZ;

        for (int tick = 1; tick <= limit; tick++) {
            velocity = advance(velocity, pitchDegrees, yaw, gravity);
            double nextX = positionX + velocity.x;
            double nextY = positionY + velocity.y;
            double nextZ = positionZ + velocity.z;
            simulation.travelled += Math.sqrt(
                    Mth.square(nextX - positionX) + Mth.square(nextY - positionY) + Mth.square(nextZ - positionZ));
            positionX = nextX;
            positionY = nextY;
            positionZ = nextZ;

            if (nextY < minY) {
                simulation.collision = true;
                break;
            }
            if (tick % COLLISION_STEP == 0) {
                if (hitsTerrain(level, player, checkX, checkY, checkZ, nextX, nextY, nextZ, clearance)) {
                    simulation.collision = true;
                    break;
                }
                checkX = nextX;
                checkY = nextY;
                checkZ = nextZ;
                if (collectPoints) {
                    simulation.points.add(new Vec3(nextX, nextY, nextZ));
                }
            }
            if (simulation.travelled >= maxDistance) {
                simulation.reachedGoal = true;
                break;
            }
        }

        simulation.endPosition = new Vec3(positionX, positionY, positionZ);
        if (collectPoints) {
            Vec3 last = simulation.points.get(simulation.points.size() - 1);
            if (last.distanceToSqr(simulation.endPosition) > 1.0e-6) {
                simulation.points.add(simulation.endPosition);
            }
        }
        return simulation;
    }

    /**
     * Flight path angle, in degrees below horizontal, that the aircraft would
     * descend at while holding an attitude.
     *
     * <p>
     * The simulation stops when the path reaches the ground, so the number reflects
     * the descent the aircraft can actually make rather than an average taken over
     * a window that extends through the terrain.
     */
    public static double descentAngle(Player player, float pitchDegrees, int maxTicks) {
        int limit = Mth.clamp(maxTicks, 1, MAX_TICKS);
        Vec3 velocity = player.getDeltaMovement();
        double startX = player.getX();
        double startY = player.getY();
        double startZ = player.getZ();
        double x = startX;
        double y = startY;
        double z = startZ;
        float yaw = player.getYRot();
        double gravity = effectiveGravity(player);
        Level level = player.level();

        for (int tick = 1; tick <= limit; tick++) {
            velocity = advance(velocity, pitchDegrees, yaw, gravity);
            x += velocity.x;
            y += velocity.y;
            z += velocity.z;
            if (y <= groundHeight(level, x, z)) {
                break;
            }
        }

        double horizontal = Math.hypot(x - startX, z - startZ);
        return Math.toDegrees(Math.atan2(startY - y, Math.max(horizontal, 1.0e-3)));
    }

    /** Terrain height at a position, using the client height map. */
    private static double groundHeight(Level level, double x, double z) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
    }

    /**
     * One tick of elytra movement: a port of
     * {@code LivingEntity.updateFallFlyingMovement}.
     */
    private static Vec3 advance(Vec3 velocity, float pitchDegrees, float yawDegrees, double gravity) {
        double lean = Math.toRadians(pitchDegrees);
        Vec3 look = viewVector(pitchDegrees, yawDegrees);
        double lookHorizontal = Math.sqrt(look.x * look.x + look.z * look.z);
        double movedHorizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        double lift = Math.cos(lean) * Math.cos(lean);

        double moveX = velocity.x;
        double moveY = velocity.y + gravity * (-1.0 + lift * LIFT_FACTOR);
        double moveZ = velocity.z;

        // Falling converts some downwards speed into forwards speed.
        if (moveY < 0.0 && lookHorizontal > 0.0) {
            double convert = moveY * -DIVE_CONVERSION * lift;
            moveX += look.x * convert / lookHorizontal;
            moveY += convert;
            moveZ += look.z * convert / lookHorizontal;
        }
        // A nose-up attitude trades horizontal speed for height.
        if (lean < 0.0 && lookHorizontal > 0.0) {
            double convert = movedHorizontal * -Math.sin(lean) * CLIMB_CONVERSION;
            moveX -= look.x * convert / lookHorizontal;
            moveY += convert * CLIMB_VERTICAL;
            moveZ -= look.z * convert / lookHorizontal;
        }
        // The velocity is pulled towards the direction the player is looking.
        if (lookHorizontal > 0.0) {
            moveX += (look.x / lookHorizontal * movedHorizontal - moveX) * ALIGNMENT;
            moveZ += (look.z / lookHorizontal * movedHorizontal - moveZ) * ALIGNMENT;
        }

        return new Vec3(moveX * HORIZONTAL_DRAG, moveY * VERTICAL_DRAG, moveZ * HORIZONTAL_DRAG);
    }

    /** A port of {@code Entity.calculateViewVector}. */
    private static Vec3 viewVector(float xRot, float yRot) {
        double realXRot = Math.toRadians(xRot);
        double realYRot = Math.toRadians(-yRot);
        return new Vec3(Math.sin(realYRot) * Math.cos(realXRot), -Math.sin(realXRot),
                Math.cos(realYRot) * Math.cos(realXRot));
    }

    /** A port of {@code LivingEntity.getEffectiveGravity}. */
    private static double effectiveGravity(Player player) {
        double gravity = player.getAttributeValue(Attributes.GRAVITY);
        boolean falling = player.getDeltaMovement().y <= 0.0;
        if (falling && player.hasEffect(MobEffects.SLOW_FALLING)) {
            return Math.min(gravity, SLOW_FALLING_GRAVITY);
        }
        return gravity;
    }

    private static boolean hitsTerrain(Level level, Player player, double fromX, double fromY, double fromZ, double toX,
            double toY, double toZ, double clearance) {
        if (segmentHits(level, player, fromX, fromY + BODY_LOW, fromZ, toX, toY + BODY_LOW, toZ)
                || segmentHits(level, player, fromX, fromY + BODY_HIGH, fromZ, toX, toY + BODY_HIGH, toZ)) {
            return true;
        }
        // A safety clearance is kept as extra head room above the aircraft.
        return clearance > 0.0 && segmentHits(level, player, fromX, fromY + BODY_HIGH + clearance, fromZ, toX,
                toY + BODY_HIGH + clearance, toZ);
    }

    private static boolean segmentHits(Level level, Player player, double fromX, double fromY, double fromZ, double toX,
            double toY, double toZ) {
        BlockHitResult hit = level.clip(new ClipContext(new Vec3(fromX, fromY, fromZ), new Vec3(toX, toY, toZ),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK;
    }
}
