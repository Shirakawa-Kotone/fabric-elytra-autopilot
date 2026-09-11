package net.elytraautopilot;

import net.elytraautopilot.commands.ClientCommands;
import net.elytraautopilot.config.ModConfig;
import net.elytraautopilot.strategy.FlightPhase;
import net.elytraautopilot.strategy.FlightStrategy;
import net.elytraautopilot.utils.ElytraManager;
import net.elytraautopilot.utils.ElytraTrajectory;
import net.elytraautopilot.utils.FreeCameraState;
import net.elytraautopilot.utils.Hud;
import net.elytraautopilot.utils.HudRenderer;
import net.elytraautopilot.utils.KeyBindings;
import net.elytraautopilot.utils.ObstacleAvoidance;
import net.elytraautopilot.utils.TrajectoryRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.elytraautopilot.utils.ElytraManager.*;

public class ElytraAutoPilot implements ClientModInitializer {
    public static final String MODID = "elytraautopilot";
    public static final Logger LOGGER = LoggerFactory.getLogger("ElytraAutoPilot");
    private static boolean configPressed = false;
    private static boolean landPressed = false;
    private static boolean takeoffPressed = false;
    public static Minecraft minecraftClient;
    public static boolean calculateHud;
    public static boolean autoFlight;
    private static final int TAKEOFF_COOLDOWN_TICKS = 5;
    private static int takeoffCooldown = 0;
    private static boolean onTakeoff;
    public static double pitchMod = 1f;

    public static Vec3 previousPosition;
    public static double currentVelocity;
    public static double currentVelocityHorizontal;

    public static boolean isDescending;
    public static boolean pullUp;
    public static boolean pullDown;

    private static double velHigh = 0f;
    private static double velLow = 0f;

    public static int argXpos;
    public static int argZpos;
    public static boolean isChained = false;
    public static boolean isflytoActive = false;
    public static boolean forceLand = false;
    public static boolean isLanding = false;
    public static boolean directLanding = false;
    public static float GLIDE_ANGLE = 0.0f;
    public static boolean doGlide = false;
    public static double distance = 0f;
    public static double groundheight;

    /**
     * How far ahead a straight-in landing path is checked for terrain, in blocks.
     */
    private static final double MAX_DIRECT_LANDING_CHECK = 220.0;
    /** Extra descent angle tolerated before giving up on a straight-in landing. */
    private static final double DIRECT_LANDING_ANGLE_HYSTERESIS = 12.0;
    /**
     * Margin below the maximum angle required before starting a straight-in
     * landing.
     */
    private static final double DIRECT_LANDING_ENTRY_MARGIN = 5.0;
    /**
     * Steepest attitude the approach solver may command. The attitude needed to
     * descend is far larger than the descent angle itself.
     */
    private static final double DIRECT_LANDING_MAX_ATTITUDE = 80.0;
    /**
     * Horizon used to measure what an attitude actually descends at, in ticks. The
     * measurement stops at the ground, so this is only an upper bound.
     */
    private static final int DIRECT_LANDING_SOLVER_TICKS = 120;
    /** Bisection steps used to invert the elytra movement model. */
    private static final int DIRECT_LANDING_SOLVER_ITERATIONS = 9;

    /**
     * How far ahead the dynamic activation check looks, in blocks. Long enough to
     * cover a few seconds of flight, which is what the aircraft has to fly before
     * its own controllers or obstacle avoidance can act.
     */
    private static final double ACTIVATION_LOOKAHEAD = 120.0;

    /** Cached result of the activatable-path check for the current tick. */
    private static int activationCheckTick = Integer.MIN_VALUE;
    private static boolean activationCheckResult = false;

    /** Cached result of the straight-in landing path check for the current tick. */
    private static int directLandingCheckTick = Integer.MIN_VALUE;
    private static boolean directLandingCheckResult = false;
    /** Cached approach attitude for the current tick. */
    private static int directLandingAimTick = Integer.MIN_VALUE;
    private static double directLandingAimResult = 0.0;

    // Strategy mode fields
    private static FlightStrategy climbStrategy;
    private static FlightStrategy cruiseStrategy;
    public static FlightPhase strategyPhase = FlightPhase.CLIMB;
    public static int climbTick = 0;
    public static int cruiseTick = 0;
    public static boolean strategyActive = false;

    @Override
    public void onInitializeClient() {
        minecraftClient = Minecraft.getInstance();

        // Load precomputed pitch strategies
        climbStrategy = FlightStrategy.loadResource("climb.csv", 254);
        cruiseStrategy = FlightStrategy.loadResource("cruise.csv", 357);
        if (climbStrategy == null || cruiseStrategy == null) {
            LOGGER.warn("Strategy mode unavailable — falling back to classic mode. "
                    + "Check that strategy CSV resources are present in the mod JAR.");
        }

        // Work out the speed obstacle avoidance needs to climb with here, at
        // startup, instead of in the middle of a flight: finding it simulates every
        // pull-up attitude at every speed up to the threshold.
        double climbThreshold = ElytraTrajectory.minimumClimbSpeed(0.08, ModConfig.INSTANCE.avoidanceMaxClimbAngle);
        LOGGER.info("Minimum climb speed: {} blocks/tick ({} m/s): below it avoidance dives for speed first",
                String.format("%.2f", climbThreshold), String.format("%.1f", climbThreshold * 20.0));

        KeyBindings.init();
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MODID, "hud"), (context, tickCounter) -> {
            ElytraAutoPilot.this.onScreenTick();
            HudRenderer.drawHud(context, tickCounter);
        });

        ClientTickEvents.END_CLIENT_TICK.register(e -> this.onClientTick());

        ClientCommands.register(minecraftClient);
    }

    public static String getModId() {
        return MODID;
    }

    public static void takeoff() {
        LocalPlayer player = minecraftClient.player;
        if (!onTakeoff) {
            if (player != null) {
                if (ModConfig.INSTANCE.elytraAutoSwap) {
                    int elytraSlot = getElytraIndex(player);
                    if (elytraSlot == -100) {
                        player.sendOverlayMessage(
                                Component.translatable("text." + MODID + ".takeoffFail.noElytraInInventory")
                                        .withStyle(ChatFormatting.RED));
                        return;
                    }
                    equipElytra(player);
                } else {
                    ItemStack itemStack = ElytraManager.getChestplateSlot(player);

                    if (itemStack.getItem() != Items.ELYTRA) {
                        player.sendOverlayMessage(
                                Component.translatable("text." + MODID + ".takeoffFail.noElytraEquipped")
                                        .withStyle(ChatFormatting.RED));
                        return;
                    }
                    int elytraDamage = itemStack.getMaxDamage() - itemStack.getDamageValue();
                    if (elytraDamage == 1) {
                        player.sendOverlayMessage(Component.translatable("text." + MODID + ".takeoffFail.elytraBroken")
                                .withStyle(ChatFormatting.RED));
                        return;
                    }
                }
                Item itemMain = player.getMainHandItem().getItem();
                Item itemOff = player.getOffhandItem().getItem();
                var chestplateSlot = ElytraManager.getChestplateSlot(player);
                Item itemChest = chestplateSlot.getItem();
                int elytraDamage = chestplateSlot.getMaxDamage() - chestplateSlot.getDamageValue();
                if (itemChest != Items.ELYTRA) {
                    player.sendOverlayMessage(
                            Component.translatable("text.elytraautopilot.takeoffFail.noElytraEquipped")
                                    .withStyle(ChatFormatting.RED));
                    return;
                }
                if (elytraDamage == 1) {
                    player.sendOverlayMessage(Component.translatable("text.elytraautopilot.takeoffFail.elytraBroken")
                            .withStyle(ChatFormatting.RED));
                    return;
                }
                if (itemMain != Items.FIREWORK_ROCKET && itemOff != Items.FIREWORK_ROCKET) {
                    player.sendOverlayMessage(
                            Component.translatable("text.elytraautopilot.takeoffFail.fireworkRequired")
                                    .withStyle(ChatFormatting.RED));
                    return;
                }

                Level world = player.level();
                Vec3 clientPos = player.position();
                int l = world.getMaxY();
                int n = 2;
                double c = clientPos.y();
                for (double i = c; i < l; i++) {
                    BlockPos blockPos = BlockPos.containing(clientPos.x(), clientPos.y() + n, clientPos.z());
                    if (!world.getBlockState(blockPos).isAir()) {
                        player.sendOverlayMessage(
                                Component.translatable("text.elytraautopilot.takeoffFail.clearSkyNeeded")
                                        .withStyle(ChatFormatting.RED));
                        return;
                    }
                    n++;
                }
                takeoffCooldown = TAKEOFF_COOLDOWN_TICKS;
                minecraftClient.options.keyJump.setDown(true);
            }
            return;
        }
        if (player != null) {
            if (groundheight > ModConfig.INSTANCE.minHeight) {
                onTakeoff = false;
                minecraftClient.options.keyUse.setDown(false);
                minecraftClient.options.keyJump.setDown(false);
                autoFlight = true;
                pitchMod = 3f;
                // Initialize strategy phase based on current altitude
                if (ModConfig.INSTANCE.strategyMode && climbStrategy != null && cruiseStrategy != null) {
                    strategyPhase = player.position().y >= ModConfig.INSTANCE.cruiseAltitudeMax
                            ? FlightPhase.CRUISE
                            : FlightPhase.CLIMB;
                    climbTick = 0;
                    cruiseTick = 0;
                }
                FreeCameraState.init();
                if (isChained) {
                    isLanding = false;
                    forceLand = false;
                    directLanding = false;
                    isflytoActive = true;
                    isChained = false;
                    minecraftClient.player
                            .sendOverlayMessage(Component.translatable("text.elytraautopilot.flyto", argXpos, argZpos)
                                    .withStyle(ChatFormatting.GREEN));
                }
                return;
            }
            if (!player.isFallFlying())
                minecraftClient.options.keyJump.setDown(!minecraftClient.options.keyJump.isDown());
            Item itemMain = player.getMainHandItem().getItem();
            Item itemOff = player.getOffhandItem().getItem();
            boolean hasFirework = (itemMain == Items.FIREWORK_ROCKET || itemOff == Items.FIREWORK_ROCKET);
            if (!hasFirework) {
                if (!tryRestockFirework(player)) {
                    minecraftClient.options.keyUse.setDown(false);
                    minecraftClient.options.keyJump.setDown(false);
                    onTakeoff = false;
                    player.sendOverlayMessage(Component.translatable("text.elytraautopilot.takeoffAbort.noFirework")
                            .withStyle(ChatFormatting.RED));
                    doGlide = true;
                }
            } else
                minecraftClient.options.keyUse.setDown(currentVelocity < 0.75f && player.getXRot() == -90f);
        }
    }

    private void onScreenTick() // Once every screen frame
    {
        // Stops logic when paused.
        if (minecraftClient.isPaused()) {
            doGlide = false;
            if (minecraftClient.isLocalServer())
                return;
        }

        // Player is null when it isn't currently in a world. Optimization spot here.
        Player player = minecraftClient.player;
        if (player == null)
            return;

        // Fps adaptation (not perfect but works nicely most of the time)
        float fps_delta = minecraftClient.getDeltaTracker().getGameTimeDeltaTicks();
        float fps_result = 20 / fps_delta;
        double speedMod = 60 / fps_result; // Adapt to base 60 FPS

        // Calculate hard coded flight modes based on pitch.
        float pitch = player.getXRot();
        // if (doGlide) {
        // if (pitch < GLIDE_ANGLE) {
        // player.setPitch((float) (pitch +
        // ModConfig.INSTANCE.pullDownSpeed*speedMod*3));
        // pitch = player.getPitch();
        // if (pitch >= GLIDE_ANGLE) {
        // player.setPitch(GLIDE_ANGLE);
        // doGlide = false;
        // }
        // }
        // else if (pitch > GLIDE_ANGLE){
        // player.setPitch((float) (pitch -
        // ModConfig.INSTANCE.pullDownSpeed*speedMod)*3);
        // pitch = player.getPitch();
        // if (pitch <= GLIDE_ANGLE) {
        // player.setPitch(GLIDE_ANGLE);
        // doGlide = false;
        // }
        // }
        // }
        if (onTakeoff) {
            if (pitch > -90f) {
                player.setXRot((float) (pitch - ModConfig.INSTANCE.takeOffPull * speedMod));
                pitch = player.getXRot();
            }
            if (pitch <= -90f)
                player.setXRot(-90f); // Very stiff and unnatural movement
        }
        if (autoFlight) {
            // Flyto behavior
            if (isflytoActive || forceLand) {
                if (isLanding || forceLand) {
                    if (!forceLand && !ModConfig.INSTANCE.autoLanding) {
                        isflytoActive = false;
                        isLanding = false;
                        directLanding = false;
                        return;
                    }
                    isDescending = true;
                    if (updateDirectLanding(player)) {
                        // There is enough room for a straight-in landing: keep the
                        // nose pointed at the landing spot instead of circling down.
                        directLanding = true;
                        directLandingApproach(player, speedMod);
                    } else {
                        // Not enough room (too steep, or terrain in the way): fall
                        // back to the original rotating descent.
                        directLanding = false;
                        if (ModConfig.INSTANCE.riskyLanding && groundheight > 60) {
                            riskyLanding(player, speedMod);
                        } else {
                            smoothLanding(player, speedMod);
                        }
                    }
                } else {
                    Vec3 playerPosition = player.position();
                    double f = (double) argXpos - playerPosition.x;
                    double d = (double) argZpos - playerPosition.z;
                    steerTowards(player, bearingToTarget(player), speedMod);
                    distance = Math.sqrt(f * f + d * d);
                    if (canStartDirectLanding(player)) {
                        // Start the descent while there is still room to do it in a
                        // straight line instead of flying level and then circling.
                        announceLanding(player);
                        player.sendOverlayMessage(Component.translatable("text.elytraautopilot.directLanding")
                                .withStyle(ChatFormatting.GREEN));
                        isLanding = true;
                        directLanding = true;
                        directLandingApproach(player, speedMod);
                    } else if (distance < 20) {
                        announceLanding(player);
                        isLanding = true;
                        directLanding = false;
                    }
                }
            }
            // Flight pitch behavior (classic mode only — strategy mode controls
            // pitch in onClientTick at 20 TPS). Obstacle avoidance always wins.
            boolean obstacleAvoidance = ObstacleAvoidance.isActive();
            if (pullUp && !(isLanding || forceLand) && !strategyActive && !obstacleAvoidance) {
                player.setXRot((float) (pitch - ModConfig.INSTANCE.pullUpSpeed * speedMod));
                pitch = player.getXRot();
                if (pitch <= ModConfig.INSTANCE.pullUpAngle) {
                    player.setXRot((float) ModConfig.INSTANCE.pullUpAngle);
                }
                // Powered flight behavior
                minecraftClient.options.keyUse.setDown(ModConfig.INSTANCE.poweredFlight && currentVelocity < 1.25f);
            }
            if (pullDown && !(isLanding || forceLand) && !strategyActive && !obstacleAvoidance) {
                player.setXRot((float) (pitch + ModConfig.INSTANCE.pullDownSpeed * pitchMod * speedMod));
                pitch = player.getXRot();
                if (pitch >= ModConfig.INSTANCE.pullDownAngle) {
                    player.setXRot((float) ModConfig.INSTANCE.pullDownAngle);
                }
                // Powered flight behavior
                minecraftClient.options.keyUse.setDown(ModConfig.INSTANCE.poweredFlight && currentVelocity < 1.25f);
            }
            if (obstacleAvoidance && !strategyActive) {
                // Classic mode owns its pitch here; strategy mode already steered in
                // onClientTick, so only one of them may touch the pitch.
                ObstacleAvoidance.steer(player, ModConfig.INSTANCE.avoidancePitchRate * speedMod / 3.0);
                pitch = player.getXRot();
                minecraftClient.options.keyUse
                        .setDown(ModConfig.INSTANCE.poweredFlight && currentVelocity < 1.25f && pitch < -10f);
            }
        } else {
            velHigh = 0f;
            velLow = 0f;
            isLanding = false;
            forceLand = false;
            isflytoActive = false;
            directLanding = false;
            pullUp = false;
            pitchMod = 1f;
            pullDown = false;
            strategyActive = false;
            ObstacleAvoidance.clear();
            FreeCameraState.reset();
        }
    }

    private void onClientTick() // 20 times a second, before first screen tick
    {
        if (!(minecraftClient.isPaused() && minecraftClient.isLocalServer()))
            Hud.tick();
        double velMod;

        if (ClientCommands.bufferSave) {
            ModConfig.INSTANCE.saveConfig(ModConfig.CONFIG_FILE.toFile());
            ClientCommands.bufferSave = false;
        }

        LocalPlayer player = minecraftClient.player;

        if (player == null) {
            autoFlight = false;
            onTakeoff = false;
            return;
        }

        if (player.isFallFlying())
            calculateHud = true;
        else {
            calculateHud = false;
            autoFlight = false;
            groundheight = -1f;
            climbTick = 0;
            cruiseTick = 0;
            strategyActive = false;
        }

        double altitude;
        if (autoFlight) {
            // Attitude the mode controllers want to fly, handed to obstacle
            // avoidance so it can tell whether releasing the pitch is safe.
            float normalAttitude = Float.NaN;
            var durability = getElytraDurability(player);
            if (ModConfig.INSTANCE.emergencyLand && durability < ModConfig.INSTANCE.elytraReplaceDurability) {
                if (ModConfig.INSTANCE.elytraAutoSwap) {
                    if (canRestockElytra(player)) {
                        forceLand = !tryRestockElytra(player);
                    } else {
                        forceLand = true;
                    }
                } else {
                    forceLand = true;
                }
            }

            altitude = player.position().y;

            if (player.isInWater() || player.isInLava()) {
                isflytoActive = false;
                isLanding = false;
                directLanding = false;
                ObstacleAvoidance.clear();
                autoFlight = false;
                return;
            }

            if (ModConfig.INSTANCE.strategyMode && climbStrategy != null && cruiseStrategy != null && !onTakeoff
                    && !isLanding && !forceLand) {
                // Strategy mode: altitude-based phase switching + precomputed pitch waveform
                strategyActive = true;

                // Hysteresis phase switching based on absolute altitude
                if (strategyPhase == FlightPhase.CLIMB && altitude >= ModConfig.INSTANCE.cruiseAltitudeMax) {
                    strategyPhase = FlightPhase.CRUISE;
                    cruiseTick = 0;
                } else if (strategyPhase == FlightPhase.CRUISE && altitude <= ModConfig.INSTANCE.cruiseAltitudeMin) {
                    strategyPhase = FlightPhase.CLIMB;
                    climbTick = 0;
                }

                // Pitch the waveform wants for the current phase
                FlightStrategy strategy;
                int tick;
                if (strategyPhase == FlightPhase.CLIMB) {
                    strategy = climbStrategy;
                    tick = climbTick;
                } else {
                    strategy = cruiseStrategy;
                    tick = cruiseTick;
                }
                double angle = strategy.angleAt(tick);
                normalAttitude = (float) Math.max(-90.0, Math.min(90.0, -angle));

                // Advance the waveform tick index
                if (strategyPhase == FlightPhase.CLIMB) {
                    climbTick = strategy.nextTick(climbTick);
                } else {
                    cruiseTick = strategy.nextTick(cruiseTick);
                }
            } else {
                // Classic mode: velocity-thresholded hysteresis controller
                strategyActive = false;
                if (isDescending) {
                    pullUp = false;
                    pullDown = true;
                    if (altitude > ModConfig.INSTANCE.maxHeight) {
                        velHigh = 0.3f;
                    } else if (altitude > ModConfig.INSTANCE.maxHeight - 10) {
                        velLow = 0.28475f;
                    }
                    velMod = Math.max(velHigh, velLow);
                    if (currentVelocity >= ModConfig.INSTANCE.pullDownMaxVelocity + velMod) {
                        isDescending = false;
                        pullDown = false;
                        pullUp = true;
                        pitchMod = 1f;
                    }
                } else {
                    velHigh = 0f;
                    velLow = 0f;
                    pullUp = true;
                    pullDown = false;
                    if (currentVelocity <= ModConfig.INSTANCE.pullUpMinVelocity
                            || altitude > ModConfig.INSTANCE.maxHeight - 10) {
                        isDescending = true;
                        pullDown = true;
                        pullUp = false;
                    }
                }
                // The attitude this controller is heading for, used to decide when
                // obstacle avoidance may hand the pitch back.
                normalAttitude = (float) (pullDown ? ModConfig.INSTANCE.pullDownAngle : ModConfig.INSTANCE.pullUpAngle);
            }

            // Terrain protection runs after the mode controllers so it knows what they
            // want to do, and before any pitch is applied.
            if (!onTakeoff && (!(isLanding || forceLand) || directLanding)) {
                double lookaheadCap = Double.MAX_VALUE;
                if (directLanding) {
                    double pathLength = Math.hypot(directLandingDistance(player),
                            Math.max(directLandingHeight(player), 0.0));
                    lookaheadCap = pathLength * 0.7;
                }
                ObstacleAvoidance.tick(player, currentVelocity, lookaheadCap, normalAttitude);
            } else {
                ObstacleAvoidance.clear();
            }

            boolean avoidanceGaveUp = ObstacleAvoidance.consumeLandingRequest();
            if (avoidanceGaveUp && !isLanding && !forceLand) {
                // Avoidance has run out of ways around the terrain: put the aircraft
                // on the ground under control instead of holding a pitch that is
                // going to end in it.
                player.sendOverlayMessage(
                        Component.translatable("text.elytraautopilot.avoidanceLanding").withStyle(ChatFormatting.RED));
                announceLanding(player);
                minecraftClient.options.keyUse.setDown(false);
                forceLand = true;
            }

            // Strategy mode applies its own pitch here; the classic controller applies
            // it every frame in onScreenTick.
            if (strategyActive && !ObstacleAvoidance.isActive()) {
                player.setXRot(normalAttitude);
            }
        }
        if (!takeoffPressed && KeyBindings.takeoffBinding.isDown()) {
            if (onTakeoff) {
                onTakeoff = false;
                minecraftClient.options.keyUse.setDown(false);
                minecraftClient.options.keyJump.setDown(false);
                doGlide = true;
            } else {
                takeoff();
            }
        }

        if (!landPressed && KeyBindings.landBinding.isDown() && autoFlight) {
            player.sendOverlayMessage(
                    Component.translatable("text.elytraautopilot.landing").withStyle(ChatFormatting.BLUE));
            SoundEvent soundEvent = SoundEvent
                    .createVariableRangeEvent(Identifier.parse(ModConfig.INSTANCE.playSoundOnLanding));
            player.playSound(soundEvent, 1.3f, 1f);
            minecraftClient.options.keyUse.setDown(false);
            forceLand = true;
        }

        if (!configPressed && KeyBindings.configBinding.isDown()) {
            if (player.isFallFlying()) {
                if (!autoFlight && !activationAllowed(player)) {
                    player.sendOverlayMessage(Component.translatable("text.elytraautopilot.autoFlightFail.tooLow")
                            .withStyle(ChatFormatting.RED));
                    doGlide = true;
                } else {
                    // If the player is flying an elytra, we start the auto flight
                    autoFlight = !autoFlight;
                    minecraftClient.options.keyUse.setDown(false);
                    if (autoFlight) {
                        isDescending = true;
                        pitchMod = 3f;
                        FreeCameraState.init();
                        // Initialize strategy phase based on current altitude
                        if (ModConfig.INSTANCE.strategyMode && climbStrategy != null && cruiseStrategy != null) {
                            strategyPhase = player.position().y >= ModConfig.INSTANCE.cruiseAltitudeMax
                                    ? FlightPhase.CRUISE
                                    : FlightPhase.CLIMB;
                            climbTick = 0;
                            cruiseTick = 0;
                        }
                    }
                }
            } else {
                // Otherwise, we open the settings if cloth is loaded
                Screen configScreen = ModConfig.createConfigScreen(minecraftClient.gui.screen());
                minecraftClient.gui.setScreen(configScreen);
            }
        }
        configPressed = KeyBindings.configBinding.isDown();
        landPressed = KeyBindings.landBinding.isDown();
        takeoffPressed = KeyBindings.takeoffBinding.isDown();

        if (takeoffCooldown > 0) {
            if (--takeoffCooldown == 0)
                onTakeoff = true;
        }

        if (onTakeoff) {
            takeoff();
        }

        if (calculateHud) {
            computeVelocity();
            Hud.drawHud(player);
            TrajectoryRenderer.render(player);
        } else {
            previousPosition = null;
            Hud.clearHud();
        }
    }

    private static boolean tryRestockFirework(Player player) {
        if (ModConfig.INSTANCE.fireworkHotswap) {
            ItemStack newFirework = null;
            for (ItemStack itemStack : player.getInventory().getNonEquipmentItems()) {
                if (itemStack.getItem() == Items.FIREWORK_ROCKET) {
                    newFirework = itemStack;
                    break;
                }
            }
            if (newFirework != null) {
                int handSlot;
                if (player.getOffhandItem().isEmpty()) {
                    handSlot = 45; // Offhand slot refill
                } else {
                    handSlot = 36 + player.getInventory().getSelectedSlot(); // Mainhand slot refill
                }

                assert minecraftClient.gameMode != null;
                minecraftClient.gameMode.handleContainerInput(player.inventoryMenu.containerId, handSlot,
                        player.getInventory().getNonEquipmentItems().indexOf(newFirework), ContainerInput.SWAP, player);
                return true;
            }
        }
        return false;
    }

    private static boolean tryRestockElytra(LocalPlayer player) {
        if (ModConfig.INSTANCE.elytraHotswap) {
            return equipElytra(player);
        }
        return false;
    }

    private static boolean canRestockElytra(LocalPlayer player) {
        var result = getElytraIndex(player);
        return result != -100;
    }

    private void computeVelocity() {
        Vec3 newPosition;
        Player player = minecraftClient.player;
        if (player != null && !(minecraftClient.isPaused() && minecraftClient.isLocalServer())) {
            newPosition = player.position();
            if (previousPosition == null)
                previousPosition = newPosition;

            Vec3 difference = new Vec3(newPosition.x - previousPosition.x, newPosition.y - previousPosition.y,
                    newPosition.z - previousPosition.z);
            Vec3 difference_horizontal = new Vec3(newPosition.x - previousPosition.x, 0,
                    newPosition.z - previousPosition.z);
            previousPosition = newPosition;

            currentVelocity = difference.length();
            currentVelocityHorizontal = difference_horizontal.length();
        }
    }

    private void smoothLanding(Player player, double speedMod) {
        float yaw = Mth.wrapDegrees(player.getYRot());
        float pitch = Mth.wrapDegrees(player.getXRot());
        float fallPitchMax = 50f;
        float fallPitchMin = 30f;
        float fallPitch;
        if (groundheight > 50) {
            fallPitch = fallPitchMax;
        } else if (groundheight < 20) {
            fallPitch = fallPitchMin;
        } else {
            fallPitch = (float) ((groundheight - 20) / 30) * 20 + fallPitchMin;
        }
        pitchMod = 3f;
        player.setYRot((float) (yaw + ModConfig.INSTANCE.autoLandSpeed * speedMod));
        player.setXRot((float) (pitch + ModConfig.INSTANCE.pullDownSpeed * pitchMod * speedMod));
        pitch = player.getXRot();
        if (pitch >= fallPitch) {
            player.setXRot(fallPitch);
        }
    }

    /**
     * Turns the aircraft towards a heading at the configured turning speed, taking
     * the short way around.
     */
    private void steerTowards(Player player, float targetYawDegrees, double speedMod) {
        float yaw = Mth.wrapDegrees(player.getYRot());
        float difference = Mth.wrapDegrees(Mth.wrapDegrees(targetYawDegrees) - yaw);
        double rate = ModConfig.INSTANCE.turningSpeed * speedMod;
        if (Math.abs(difference) < rate * 2.0) {
            player.setYRot(Mth.wrapDegrees(targetYawDegrees));
        } else {
            player.setYRot(yaw + (float) Math.copySign(rate, difference));
        }
    }

    /** Heading from the aircraft to the current fly-to destination. */
    private static float bearingToTarget(Player player) {
        Vec3 position = player.position();
        double f = (double) argXpos - position.x;
        double d = (double) argZpos - position.z;
        return Mth.wrapDegrees((float) (Mth.atan2(d, f) * 57.2957763671875D) - 90.0F);
    }

    private void announceLanding(Player player) {
        player.sendOverlayMessage(
                Component.translatable("text.elytraautopilot.landing").withStyle(ChatFormatting.BLUE));
        SoundEvent soundEvent = SoundEvent
                .createVariableRangeEvent(Identifier.parse(ModConfig.INSTANCE.playSoundOnLanding));
        player.playSound(soundEvent, 1.3f, 1f);
    }

    /**
     * May auto-flight be switched on from where the aircraft is now?
     *
     * <p>
     * By default the aircraft must be above the configured minimum height. With
     * dynamic activation that is not required: a path that is predicted to be clear
     * for the next few seconds is enough, which is what makes it possible to start
     * the autopilot in terrain that is high but not dangerous - the aircraft only
     * has to get past what is in front of it, not climb into the sky first.
     */
    public static boolean activationAllowed(Player player) {
        if (groundheight > ModConfig.INSTANCE.minHeight) {
            return true;
        }
        if (!ModConfig.INSTANCE.dynamicActivation) {
            return false;
        }
        return dynamicActivationAllowed(player);
    }

    /**
     * Whether the flight path ahead is clear, which is the whole activation
     * criterion in dynamic mode. Cached per tick: the HUD and the key handler both
     * ask for it, and the controllers run once per frame.
     */
    public static boolean dynamicActivationAllowed(Player player) {
        if (player == null) {
            return false;
        }
        int tick = player.tickCount;
        if (tick == activationCheckTick) {
            return activationCheckResult;
        }
        activationCheckTick = tick;
        activationCheckResult = computeDynamicActivation(player);
        return activationCheckResult;
    }

    private static boolean computeDynamicActivation(Player player) {
        if (!player.isFallFlying()) {
            return false;
        }
        double clearance = Mth.clamp(ModConfig.INSTANCE.avoidanceSeparation, 0.0, 32.0);
        if (ModConfig.INSTANCE.trajectoryPrediction) {
            ElytraTrajectory.Result result = ElytraTrajectory.scan(player, player.getXRot(), ElytraTrajectory.MAX_TICKS,
                    ACTIVATION_LOOKAHEAD, ElytraTrajectory.Envelope.of(clearance));
            return !result.collision;
        }
        return ObstacleAvoidance.landingPathClearance(player, player.getYRot(), player.getXRot(),
                ACTIVATION_LOOKAHEAD) >= ACTIVATION_LOOKAHEAD;
    }

    /** Height of the terrain at the fly-to destination, when it is known. */
    private static double targetGroundY(Player player) {
        Level level = player.level();
        if (level.isLoaded(new BlockPos(argXpos, 0, argZpos))) {
            return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, argXpos, argZpos);
        }
        // The destination chunk is not loaded yet: use the terrain underneath
        // the aircraft as a first guess until the real height is available.
        return player.position().y - groundheight;
    }

    /** Height the aircraft still has to lose before touchdown. */
    private static double directLandingHeight(Player player) {
        if (isflytoActive) {
            return player.position().y - targetGroundY(player);
        }
        return groundheight;
    }

    /**
     * Horizontal room available for the descent: the distance to the destination,
     * or - for a manual landing without a destination - the distance needed to lose
     * the remaining height at the preferred glide angle.
     */
    private static double directLandingDistance(Player player) {
        if (isflytoActive) {
            Vec3 position = player.position();
            double f = (double) argXpos - position.x;
            double d = (double) argZpos - position.z;
            return Math.sqrt(f * f + d * d);
        }
        double glide = Math.toRadians(preferredGlideAngle());
        return Math.max(0.0, directLandingHeight(player)) / Math.tan(glide);
    }

    /**
     * Descent angle a straight-in landing aims for, kept within the angle limit.
     */
    private static double preferredGlideAngle() {
        return Mth.clamp(ModConfig.INSTANCE.directLandingGlideAngle, 1.0,
                Math.max(1.0, ModConfig.INSTANCE.directLandingMaxAngle));
    }

    /** Descent angle needed to reach the landing spot from the current position. */
    private static double directLandingAngle(Player player) {
        double height = directLandingHeight(player);
        if (height <= 0.0) {
            return 0.0;
        }
        return Math.toDegrees(Math.atan2(height, Math.max(directLandingDistance(player), 1.0)));
    }

    /**
     * Attitude the straight-in approach should hold right now.
     *
     * <p>
     * The wanted descent angle comes from the geometry, but it cannot be used as an
     * attitude directly: measured with the vanilla movement model, a 30 degree
     * nose-down attitude only descends at about 9 degrees at cruise speed. Holding
     * the geometric angle would therefore undershoot the descent badly and fly far
     * past the landing spot, so the attitude is found by inverting the movement
     * model instead.
     */
    private static double directLandingAimAngle(Player player) {
        // The solver runs several simulations, so only redo it when the world has
        // moved; the flight controllers run once per frame.
        int tick = player.tickCount;
        if (tick == directLandingAimTick) {
            return directLandingAimResult;
        }
        directLandingAimTick = tick;

        double wanted = isflytoActive ? directLandingAngle(player) : preferredGlideAngle();
        wanted = Mth.clamp(wanted, 0.0, ModConfig.INSTANCE.directLandingMaxAngle);
        if (!ModConfig.INSTANCE.trajectoryPrediction) {
            // Without the physics model the attitude is all we can guess.
            directLandingAimResult = wanted;
            return wanted;
        }
        directLandingAimResult = solveAttitude(player, wanted);
        return directLandingAimResult;
    }

    /**
     * Finds the attitude whose predicted flight path descends at the wanted angle.
     * The relationship is monotonic - more nose down descends more steeply - so a
     * bisection over the attitude range converges in a few cheap simulations.
     */
    private static float solveAttitude(Player player, double wantedPathAngle) {
        double low = -Math.min(25.0, ModConfig.INSTANCE.avoidanceMaxClimbAngle);
        double high = DIRECT_LANDING_MAX_ATTITUDE;
        for (int i = 0; i < DIRECT_LANDING_SOLVER_ITERATIONS; i++) {
            double middle = (low + high) / 2.0;
            if (ElytraTrajectory.descentAngle(player, (float) middle, DIRECT_LANDING_SOLVER_TICKS) < wantedPathAngle) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return (float) ((low + high) / 2.0);
    }

    /**
     * True when the glide path towards the landing spot is not blocked by terrain.
     * The answer only depends on the aircraft's position, so it is cached for the
     * current tick: the flight controllers run once per frame but the world only
     * moves once per tick.
     */
    private static boolean directLandingPathClear(Player player) {
        int tick = player.tickCount;
        if (tick == directLandingCheckTick) {
            return directLandingCheckResult;
        }
        directLandingCheckTick = tick;
        directLandingCheckResult = computeDirectLandingPathClear(player);
        return directLandingCheckResult;
    }

    private static boolean computeDirectLandingPathClear(Player player) {
        double height = Math.max(directLandingHeight(player), 0.0);
        double pathLength = Math.hypot(directLandingDistance(player), height);
        double checkLength = Math.min(pathLength, MAX_DIRECT_LANDING_CHECK);
        if (checkLength < 8.0) {
            return true;
        }
        float yaw = isflytoActive ? bearingToTarget(player) : player.getYRot();
        double angle = Mth.clamp(directLandingAimAngle(player), 0.0, ModConfig.INSTANCE.directLandingMaxAngle + 5.0);
        // The path is meant to end on the ground at the landing spot, so only
        // terrain that shows up well before it counts as blocking.
        return ObstacleAvoidance.landingCorridorClear(player, yaw, angle, checkLength);
    }

    /**
     * Can a straight-in landing be started right now?
     *
     * <p>
     * The descent only starts once the destination is close enough for the required
     * glide path to have reached the preferred glide angle, so a normal cruise is
     * not turned into one long shallow descent: the circling descent is only
     * replaced once the aircraft is actually on approach.
     */
    private static boolean canStartDirectLanding(Player player) {
        if (!ModConfig.INSTANCE.directLanding) {
            return false;
        }
        if (directLandingHeight(player) < 4.0) {
            return false;
        }
        if (directLandingDistance(player) < ModConfig.INSTANCE.directLandingMinDistance) {
            return false;
        }
        double angle = directLandingAngle(player);
        double glide = preferredGlideAngle();
        double lowestEntry = Math.max(0.0, glide - DIRECT_LANDING_ENTRY_MARGIN);
        double highestEntry = Math.max(lowestEntry,
                ModConfig.INSTANCE.directLandingMaxAngle - DIRECT_LANDING_ENTRY_MARGIN);
        if (angle < lowestEntry || angle > highestEntry) {
            return false;
        }
        return directLandingPathClear(player);
    }

    /**
     * Keeps a straight-in landing going while it is still safe, and gives up
     * (falling back to the circling descent) when it is not.
     */
    private static boolean updateDirectLanding(Player player) {
        if (!ModConfig.INSTANCE.directLanding) {
            return false;
        }
        if (!directLanding) {
            return canStartDirectLanding(player);
        }
        if (directLandingHeight(player) < 2.0) {
            // Settling onto the ground: keep flying the flare.
            return true;
        }
        if (directLandingAngle(player) > ModConfig.INSTANCE.directLandingMaxAngle + DIRECT_LANDING_ANGLE_HYSTERESIS) {
            return false;
        }
        return directLandingPathClear(player);
    }

    /**
     * Flies a straight-in approach: hold the heading towards the landing spot,
     * track the glide path and flare shortly before touchdown.
     */
    private void directLandingApproach(Player player, double speedMod) {
        if (isflytoActive) {
            steerTowards(player, bearingToTarget(player), speedMod);
        }

        double target = Mth.clamp(directLandingAimAngle(player), -ModConfig.INSTANCE.avoidanceMaxClimbAngle,
                DIRECT_LANDING_MAX_ATTITUDE);

        // Flare: bleed speed and soften the touchdown as the ground comes up.
        double flareHeight = ModConfig.INSTANCE.directLandingFlareHeight;
        if (flareHeight > 0.0 && groundheight < flareHeight) {
            double blend = 1.0 - Mth.clamp(groundheight / flareHeight, 0.0, 1.0);
            target = Mth.lerp(blend, target, ModConfig.INSTANCE.directLandingFlareAngle);
        }

        double maxChange = Math.max(0.05, ModConfig.INSTANCE.avoidancePitchRate * speedMod / 3.0);
        float pitch = player.getXRot();
        float difference = (float) (target - pitch);
        if (Math.abs(difference) <= maxChange) {
            player.setXRot((float) target);
        } else {
            player.setXRot((float) (pitch + Math.copySign(maxChange, difference)));
        }
        pitchMod = 1f;
        minecraftClient.options.keyUse.setDown(ModConfig.INSTANCE.poweredFlight && currentVelocity < 1.25f);
    }

    private void riskyLanding(Player player, double speedMod) {
        float pitch = player.getXRot();
        player.setXRot((float) (pitch + ModConfig.INSTANCE.takeOffPull * speedMod));
        pitch = player.getXRot();
        if (pitch > 90f)
            player.setXRot(90f);
    }
}
