package net.elytraautopilot.utils;

import net.elytraautopilot.ElytraAutoPilot;
import net.elytraautopilot.config.ModConfig;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Draws the predicted flight path in the world.
 *
 * <p>
 * The line is the path the aircraft would really fly with the attitude it is
 * being commanded to hold, worked out by {@link ElytraTrajectory} rather than
 * by extending the current heading. Terrain depth testing is left on, so the
 * line disappears behind hills exactly where the aircraft would.
 *
 * <p>
 * While obstacle avoidance owns the pitch, the line shows the escape attitude
 * it has chosen, so the manoeuvre can be seen instead of guessed at.
 *
 * <p>
 * The points are handed to the vanilla gizmo collector that
 * {@code Minecraft.tick()} opens around the client tick, so they are drawn with
 * the world and vanish on their own after a tick.
 */
public final class TrajectoryRenderer {

    /** Thickness of the drawn line, in pixels. */
    private static final float LINE_WIDTH = 3.0f;
    /** Size of the marker drawn at the end of the path, in pixels. */
    private static final float END_MARKER_SIZE = 5.0f;
    /** Colour at the aircraft, and at the far end of a clear path. */
    private static final int CLEAR_NEAR = 0xFF52FF9A;
    private static final int CLEAR_FAR = 0xFF2FC8FF;
    /** Colour of the end of a path that runs into terrain. */
    private static final int IMPACT = 0xFFFF3B30;
    /** How much of a blocked path is tinted towards {@link #IMPACT}. */
    private static final double WARM_UP_FROM = 0.6;
    /** Shortest preview the renderer accepts, in ticks. */
    public static final int MIN_PREVIEW_TICKS = 20;

    private static boolean warnedAboutMissingCollector = false;

    private TrajectoryRenderer() {
    }

    /**
     * Predicts and draws the flight path for this tick. Does nothing when the
     * trajectory display is switched off or the player is not flying.
     */
    public static void render(Player player) {
        if (player == null || !ModConfig.INSTANCE.showTrajectory || !player.isFallFlying()) {
            return;
        }
        try {
            draw(player);
        } catch (IllegalStateException e) {
            // No gizmo collector is open, so there is nowhere to queue the line for
            // this tick. Draw nothing rather than taking the game down.
            if (!warnedAboutMissingCollector) {
                warnedAboutMissingCollector = true;
                ElytraAutoPilot.LOGGER.warn("Cannot draw the predicted trajectory: no gizmo collector is open", e);
            }
        }
    }

    private static void draw(Player player) {
        float pitch = ObstacleAvoidance.isActive() ? ObstacleAvoidance.getTargetPitch() : player.getXRot();
        int ticks = Mth.clamp(ModConfig.INSTANCE.trajectoryPreviewTicks, MIN_PREVIEW_TICKS, ElytraTrajectory.MAX_TICKS);
        // The drawn line is the path the aircraft really flies, but it is checked
        // with the same safety margin obstacle avoidance uses, so it ends - and
        // turns red - where the autopilot would consider the path too close.
        ElytraTrajectory.Path path = ElytraTrajectory.trace(player, pitch, ticks, Double.MAX_VALUE,
                ElytraTrajectory.Envelope.of(Mth.clamp(ModConfig.INSTANCE.avoidanceSeparation, 0.0, 32.0)));
        List<Vec3> points = path.points;
        if (points.size() < 2) {
            return;
        }

        int lastIndexOfPath = points.size() - 1;
        for (int i = 1; i <= lastIndexOfPath; i++) {
            double along = (double) i / lastIndexOfPath;
            Gizmos.line(points.get(i - 1), points.get(i), colourAt(along, path.collision), LINE_WIDTH);
        }
        Gizmos.point(points.get(lastIndexOfPath), path.collision ? IMPACT : CLEAR_FAR, END_MARKER_SIZE);
    }

    /**
     * Colour of a segment, given how far along the path it is. A path that runs
     * into terrain fades from the clear colours into {@link #IMPACT} so the impact
     * stands out.
     */
    private static int colourAt(double along, boolean collision) {
        if (!collision) {
            return lerpColour(CLEAR_NEAR, CLEAR_FAR, along);
        }
        if (along <= WARM_UP_FROM) {
            return lerpColour(CLEAR_NEAR, CLEAR_FAR, along / WARM_UP_FROM);
        }
        return lerpColour(CLEAR_FAR, IMPACT, (along - WARM_UP_FROM) / (1.0 - WARM_UP_FROM));
    }

    private static int lerpColour(int from, int to, double t) {
        double blend = Mth.clamp(t, 0.0, 1.0);
        int a = (int) Math.round(Mth.lerp(blend, (from >> 24) & 0xFF, (to >> 24) & 0xFF));
        int r = (int) Math.round(Mth.lerp(blend, (from >> 16) & 0xFF, (to >> 16) & 0xFF));
        int g = (int) Math.round(Mth.lerp(blend, (from >> 8) & 0xFF, (to >> 8) & 0xFF));
        int b = (int) Math.round(Mth.lerp(blend, from & 0xFF, to & 0xFF));
        return a << 24 | r << 16 | g << 8 | b;
    }
}
