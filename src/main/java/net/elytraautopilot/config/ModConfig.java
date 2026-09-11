package net.elytraautopilot.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ModConfig {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    public static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("fabric-elytra-autopilot.json");

    public static ModConfig INSTANCE = loadConfig(CONFIG_FILE.toFile());

    // Gui defaults
    public static final boolean showGuiDefault = true;
    public static final int guiScaleDefault = 100;
    public static final int minGuiScale = 0;
    public static final int maxGuiScale = 1000;
    public static final int guiXDefault = 5;
    public static final int minGuiX = 0;
    public static final int maxGuiX = 1000;
    public static final int guiYDefault = 5;
    public static final int minGuiY = 0;
    public static final int maxGuiY = 1000;

    // Info lines
    public static final boolean showEnabledDefault = true;
    public static final boolean showAltitudeDefault = true;
    public static final boolean showHeightDefault = true;
    public static final boolean showHeightReqDefault = true;
    public static final boolean showSpeedDefault = true;
    public static final boolean showAvgSpeedDefault = true;
    public static final boolean showHorizontalSpeedDefault = true;
    public static final boolean showFlyToDefault = true;
    public static final boolean showEtaDefault = true;
    public static final boolean showAutoLandDefault = true;
    public static final boolean showLandingStatusDefault = true;
    public static final boolean useCycleAvgSpeedDefault = true;
    public static final boolean smoothEtaDefault = true;

    // Flight profile defaults
    public static final int maxHeightDefault = 360;
    public static final int minHeightDefault = 180;
    public static final int maxMaxHeight = 10000;
    public static final int minMaxHeight = 0;
    public static final int maxMinHeight = 10000;
    public static final int minMinHeight = 0;
    public static final boolean autoLandingDefault = true;
    public static final String playSoundOnLandingDefault = "minecraft:block.note_block.pling";
    public static final double autoLandSpeedDefault = 3.0;
    public static final double turningSpeedDefault = 3.0;
    public static final double takeOffPullDefault = 10.0;
    public static final boolean riskyLandingDefault = false;
    public static final boolean poweredFlightDefault = false;
    public static final boolean elytraHotswapDefault = true;
    public static final boolean fireworkHotswapDefault = true;
    public static final int elytraReplaceDurabilityDefault = 20;
    public static final boolean emergencyLandDefault = true;
    public static final boolean elytraAutoSwapDefault = false;
    public static final boolean cameraDecoupledDefault = false;

    // Trajectory prediction defaults
    public static final boolean trajectoryPredictionDefault = true;
    public static final boolean showTrajectoryDefault = true;
    public static final int trajectoryPreviewTicksDefault = 120;
    public static final int minTrajectoryPreviewTicks = 20;
    public static final int maxTrajectoryPreviewTicks = 400;

    // Obstacle avoidance defaults
    public static final boolean obstacleAvoidanceDefault = true;
    public static final double avoidanceLookaheadDefault = 5.0;
    public static final double avoidanceClearanceDefault = 6.0;
    public static final double avoidanceMaxClimbAngleDefault = 45.0;
    public static final double avoidanceMaxDescentAngleDefault = 35.0;
    public static final double avoidancePitchRateDefault = 3.0;
    public static final boolean avoidanceEnergyAwareDefault = true;
    public static final double avoidanceClimbMinSpeedDefault = 1.45;
    public static final boolean avoidanceGiveUpDefault = true;
    public static final int avoidanceGiveUpTicksDefault = 200;
    public static final double minAvoidanceLookahead = 0.5;
    public static final double maxAvoidanceLookahead = 30.0;
    public static final double minAvoidanceClearance = 0.5;
    public static final double maxAvoidanceClearance = 32.0;
    public static final double minAvoidanceAngle = 0.0;
    public static final double maxAvoidanceAngle = 85.0;
    public static final double minAvoidancePitchRate = 0.1;
    public static final double maxAvoidancePitchRate = 15.0;
    public static final double minAvoidanceClimbSpeed = 0.25;
    public static final double maxAvoidanceClimbSpeed = 3.0;
    public static final int minAvoidanceGiveUpTicks = 20;
    public static final int maxAvoidanceGiveUpTicks = 1200;
    public static final boolean showAvoidanceStatusDefault = true;

    // Dynamic activation defaults
    public static final boolean dynamicActivationDefault = false;

    // Direct landing defaults
    public static final boolean directLandingDefault = true;
    public static final double directLandingGlideAngleDefault = 12.0;
    public static final double directLandingMaxAngleDefault = 20.0;
    public static final double directLandingMinDistanceDefault = 40.0;
    public static final double directLandingFlareHeightDefault = 10.0;
    public static final double directLandingFlareAngleDefault = -5.0;
    public static final double minDirectLandingAngle = 0.0;
    public static final double maxDirectLandingAngle = 80.0;
    public static final double minDirectLandingDistance = 0.0;
    public static final double maxDirectLandingDistance = 10000.0;
    public static final double minDirectLandingFlareHeight = 0.0;
    public static final double maxDirectLandingFlareHeight = 200.0;
    public static final double minDirectLandingFlareAngle = -85.0;
    public static final double maxDirectLandingFlareAngle = 85.0;

    // Strategy mode defaults
    public static final boolean strategyModeDefault = false;
    public static final int cruiseAltitudeMinDefault = 320;
    public static final int cruiseAltitudeMaxDefault = 480;
    public static final int maxCruiseAltitude = 10000;
    public static final int minCruiseAltitude = 0;

    // Advanced defaults
    public static final int groundCheckTicksDefault = 1;
    public static final double pullUpAngleDefault = -46.63;
    public static final double pullDownAngleDefault = 37.20;
    public static final double pullUpMinVelocityDefault = 1.91;
    public static final double pullDownMaxVelocityDefault = 2.33;
    public static final double pullUpSpeedDefault = 2.16;
    public static final double pullDownSpeedDefault = 0.21;
    public static List<String> flyLocationsDefault = new ArrayList<>();

    // Gui values
    public boolean showGui = showGuiDefault;
    public int guiScale = guiScaleDefault;
    public int guiX = guiXDefault;
    public int guiY = guiYDefault;

    // Info lines
    public boolean showEnabled = showEnabledDefault;
    public boolean showAltitude = showAltitudeDefault;
    public boolean showHeight = showHeightDefault;
    public boolean showHeightReq = showHeightReqDefault;
    public boolean showSpeed = showSpeedDefault;
    public boolean showAvgSpeed = showAvgSpeedDefault;
    public boolean showHorizontalSpeed = showHorizontalSpeedDefault;
    public boolean showFlyTo = showFlyToDefault;
    public boolean showEta = showEtaDefault;
    public boolean showAutoLand = showAutoLandDefault;
    public boolean showLandingStatus = showLandingStatusDefault;
    public boolean showAvoidanceStatus = showAvoidanceStatusDefault;
    public boolean useCycleAvgSpeed = useCycleAvgSpeedDefault;
    public boolean smoothEta = smoothEtaDefault;

    // Flight profile values
    public int maxHeight = maxHeightDefault;
    public int minHeight = minHeightDefault;
    public boolean autoLanding = autoLandingDefault;
    public String playSoundOnLanding = playSoundOnLandingDefault;
    public double autoLandSpeed = autoLandSpeedDefault;
    public double turningSpeed = turningSpeedDefault;
    public double takeOffPull = takeOffPullDefault;
    public boolean riskyLanding = riskyLandingDefault;
    public boolean directLanding = directLandingDefault;
    public double directLandingGlideAngle = directLandingGlideAngleDefault;
    public double directLandingMaxAngle = directLandingMaxAngleDefault;
    public double directLandingMinDistance = directLandingMinDistanceDefault;
    public double directLandingFlareHeight = directLandingFlareHeightDefault;
    public double directLandingFlareAngle = directLandingFlareAngleDefault;
    public boolean poweredFlight = poweredFlightDefault;
    public boolean elytraHotswap = elytraHotswapDefault;
    public boolean fireworkHotswap = fireworkHotswapDefault;
    public boolean emergencyLand = emergencyLandDefault;
    public boolean elytraAutoSwap = elytraAutoSwapDefault;
    public boolean cameraDecoupled = cameraDecoupledDefault;

    // Obstacle avoidance values
    public boolean trajectoryPrediction = trajectoryPredictionDefault;
    public boolean showTrajectory = showTrajectoryDefault;
    public int trajectoryPreviewTicks = trajectoryPreviewTicksDefault;
    public boolean obstacleAvoidance = obstacleAvoidanceDefault;
    public double avoidanceLookahead = avoidanceLookaheadDefault;
    public double avoidanceClearance = avoidanceClearanceDefault;
    public double avoidanceMaxClimbAngle = avoidanceMaxClimbAngleDefault;
    public double avoidanceMaxDescentAngle = avoidanceMaxDescentAngleDefault;
    public double avoidancePitchRate = avoidancePitchRateDefault;
    public boolean avoidanceEnergyAware = avoidanceEnergyAwareDefault;
    public double avoidanceClimbMinSpeed = avoidanceClimbMinSpeedDefault;
    public boolean avoidanceGiveUp = avoidanceGiveUpDefault;
    public int avoidanceGiveUpTicks = avoidanceGiveUpTicksDefault;
    public boolean dynamicActivation = dynamicActivationDefault;

    // Strategy mode values
    public boolean strategyMode = strategyModeDefault;
    public int cruiseAltitudeMin = cruiseAltitudeMinDefault;
    public int cruiseAltitudeMax = cruiseAltitudeMaxDefault;

    // Advanced values
    public int groundCheckTicks = groundCheckTicksDefault;
    public double pullUpAngle = pullUpAngleDefault;
    public double pullDownAngle = pullDownAngleDefault;
    public double pullUpMinVelocity = pullUpMinVelocityDefault;
    public double pullDownMaxVelocity = pullDownMaxVelocityDefault;
    public double pullUpSpeed = pullUpSpeedDefault;
    public double pullDownSpeed = pullDownSpeedDefault;
    public int elytraReplaceDurability = elytraReplaceDurabilityDefault;
    public List<String> flyLocations = flyLocationsDefault;

    public static Screen createConfigScreen(Screen parent) {
        return YetAnotherConfigLib.createBuilder().title(Component.translatable("config.elytraautopilot.gui")).category(
                ConfigCategory.createBuilder().name(Component.translatable("config.elytraautopilot.gui")).option(Option
                        .<Boolean>createBuilder().name(Component.translatable("config.elytraautopilot.gui.showgui"))
                        .description(
                                OptionDescription.of(Component.translatable("config.elytraautopilot.gui.showgui.desc")))
                        .binding(showGuiDefault, () -> ModConfig.INSTANCE.showGui,
                                newVal -> ModConfig.INSTANCE.showGui = newVal)
                        .controller(BooleanControllerBuilder::create).build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.gui.guiScale"))
                                .description(OptionDescription
                                        .of(Component.translatable("config.elytraautopilot.gui.guiScale.desc")))
                                .binding(guiScaleDefault, () -> ModConfig.INSTANCE.guiScale,
                                        newVal -> ModConfig.INSTANCE.guiScale = newVal)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                        .range(minGuiScale, maxGuiScale).step(1))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.gui.guiX"))
                                .description(OptionDescription
                                        .of(Component.translatable("config.elytraautopilot.gui.guiX.desc")))
                                .binding(guiXDefault, () -> ModConfig.INSTANCE.guiX,
                                        newVal -> ModConfig.INSTANCE.guiX = newVal)
                                .controller(opt -> IntegerFieldControllerBuilder.create(opt).min(minGuiX).max(maxGuiX))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.gui.guiY"))
                                .description(OptionDescription
                                        .of(Component.translatable("config.elytraautopilot.gui.guiY.desc")))
                                .binding(guiYDefault, () -> ModConfig.INSTANCE.guiY,
                                        newVal -> ModConfig.INSTANCE.guiY = newVal)
                                .controller(opt -> IntegerFieldControllerBuilder.create(opt).min(minGuiY).max(maxGuiY))
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.gui.enabled"))
                                .description(OptionDescription
                                        .of(Component.translatable("config.elytraautopilot.gui.enabled.desc")))
                                .binding(showEnabledDefault, () -> ModConfig.INSTANCE.showEnabled,
                                        newVal -> ModConfig.INSTANCE.showEnabled = newVal)
                                .controller(BooleanControllerBuilder::create).build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.gui.altitude"))
                                .description(OptionDescription
                                        .of(Component.translatable("config.elytraautopilot.gui.altitude.desc")))
                                .binding(showAltitudeDefault, () -> ModConfig.INSTANCE.showAltitude,
                                        newVal -> ModConfig.INSTANCE.showAltitude = newVal)
                                .controller(BooleanControllerBuilder::create).build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.gui.height"))
                                .description(OptionDescription
                                        .of(Component.translatable("config.elytraautopilot.gui.height.desc")))
                                .binding(showHeightDefault, () -> ModConfig.INSTANCE.showHeight,
                                        newVal -> ModConfig.INSTANCE.showHeight = newVal)
                                .controller(BooleanControllerBuilder::create).build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.gui.heightreq"))
                                .description(OptionDescription
                                        .of(Component.translatable("config.elytraautopilot.gui.heightreq.desc")))
                                .binding(showHeightReqDefault, () -> ModConfig.INSTANCE.showHeightReq,
                                        newVal -> ModConfig.INSTANCE.showHeightReq = newVal)
                                .controller(BooleanControllerBuilder::create).build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.gui.speed"))
                                .description(OptionDescription
                                        .of(Component.translatable("config.elytraautopilot.gui.speed.desc")))
                                .binding(showSpeedDefault, () -> ModConfig.INSTANCE.showSpeed,
                                        newVal -> ModConfig.INSTANCE.showSpeed = newVal)
                                .controller(BooleanControllerBuilder::create).build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.gui.avgspeed"))
                                .description(OptionDescription
                                        .of(Component.translatable("config.elytraautopilot.gui.avgspeed.desc")))
                                .binding(showAvgSpeedDefault, () -> ModConfig.INSTANCE.showAvgSpeed,
                                        newVal -> ModConfig.INSTANCE.showAvgSpeed = newVal)
                                .controller(BooleanControllerBuilder::create).build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.gui.horizontalspeed"))
                                .description(OptionDescription
                                        .of(Component.translatable("config.elytraautopilot.gui.horizontalspeed.desc")))
                                .binding(showHorizontalSpeedDefault, () -> ModConfig.INSTANCE.showHorizontalSpeed,
                                        newVal -> ModConfig.INSTANCE.showHorizontalSpeed = newVal)
                                .controller(BooleanControllerBuilder::create).build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.gui.flyto"))
                                .description(OptionDescription
                                        .of(Component.translatable("config.elytraautopilot.gui.flyto.desc")))
                                .binding(showFlyToDefault, () -> ModConfig.INSTANCE.showFlyTo,
                                        newVal -> ModConfig.INSTANCE.showFlyTo = newVal)
                                .controller(BooleanControllerBuilder::create).build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.gui.eta"))
                                .description(OptionDescription
                                        .of(Component.translatable("config.elytraautopilot.gui.eta.desc")))
                                .binding(showEtaDefault, () -> ModConfig.INSTANCE.showEta,
                                        newVal -> ModConfig.INSTANCE.showEta = newVal)
                                .controller(BooleanControllerBuilder::create).build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.gui.useCycleAvgSpeed"))
                                .description(OptionDescription
                                        .of(Component.translatable("config.elytraautopilot.gui.useCycleAvgSpeed.desc")))
                                .binding(useCycleAvgSpeedDefault, () -> ModConfig.INSTANCE.useCycleAvgSpeed,
                                        newVal -> ModConfig.INSTANCE.useCycleAvgSpeed = newVal)
                                .controller(BooleanControllerBuilder::create).build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.gui.smoothEta"))
                                .description(OptionDescription
                                        .of(Component.translatable("config.elytraautopilot.gui.smoothEta.desc")))
                                .binding(smoothEtaDefault, () -> ModConfig.INSTANCE.smoothEta,
                                        newVal -> ModConfig.INSTANCE.smoothEta = newVal)
                                .controller(BooleanControllerBuilder::create).build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.gui.autoland"))
                                .description(OptionDescription
                                        .of(Component.translatable("config.elytraautopilot.gui.autoland.desc")))
                                .binding(showAutoLandDefault, () -> ModConfig.INSTANCE.showAutoLand,
                                        newVal -> ModConfig.INSTANCE.showAutoLand = newVal)
                                .controller(BooleanControllerBuilder::create).build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.gui.landingstatus"))
                                .description(OptionDescription
                                        .of(Component.translatable("config.elytraautopilot.gui.landingstatus.desc")))
                                .binding(showLandingStatusDefault, () -> ModConfig.INSTANCE.showLandingStatus,
                                        newVal -> ModConfig.INSTANCE.showLandingStatus = newVal)
                                .controller(BooleanControllerBuilder::create).build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.gui.avoidanceStatus"))
                                .description(OptionDescription
                                        .of(Component.translatable("config.elytraautopilot.gui.avoidanceStatus.desc")))
                                .binding(showAvoidanceStatusDefault, () -> ModConfig.INSTANCE.showAvoidanceStatus,
                                        newVal -> ModConfig.INSTANCE.showAvoidanceStatus = newVal)
                                .controller(BooleanControllerBuilder::create).build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("config.elytraautopilot.flightprofile"))
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.flightprofile.maxHeight"))
                                .description(OptionDescription.of(
                                        Component.translatable("config.elytraautopilot.flightprofile.maxHeight.desc")))
                                .binding(maxHeightDefault, () -> ModConfig.INSTANCE.maxHeight,
                                        newVal -> ModConfig.INSTANCE.maxHeight = newVal)
                                .controller(opt -> IntegerFieldControllerBuilder.create(opt).min(minMaxHeight)
                                        .max(maxMaxHeight))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.flightprofile.minHeight"))
                                .description(OptionDescription.of(
                                        Component.translatable("config.elytraautopilot.flightprofile.minHeight.desc")))
                                .binding(minHeightDefault, () -> ModConfig.INSTANCE.minHeight,
                                        newVal -> ModConfig.INSTANCE.minHeight = newVal)
                                .controller(opt -> IntegerFieldControllerBuilder.create(opt).min(minMinHeight)
                                        .max(maxMinHeight))
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.flightprofile.dynamicActivation"))
                                .description(OptionDescription.of(Component
                                        .translatable("config.elytraautopilot.flightprofile.dynamicActivation.desc")))
                                .binding(dynamicActivationDefault, () -> ModConfig.INSTANCE.dynamicActivation,
                                        newVal -> ModConfig.INSTANCE.dynamicActivation = newVal)
                                .controller(BooleanControllerBuilder::create).build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.flightprofile.autoLanding"))
                                .description(OptionDescription.of(Component
                                        .translatable("config.elytraautopilot.flightprofile.autoLanding.desc")))
                                .binding(autoLandingDefault, () -> ModConfig.INSTANCE.autoLanding,
                                        newVal -> ModConfig.INSTANCE.autoLanding = newVal)
                                .controller(BooleanControllerBuilder::create).build())
                        .option(Option.<String>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.flightprofile.playSoundOnLanding"))
                                .description(OptionDescription.of(Component
                                        .translatable("config.elytraautopilot.flightprofile.playSoundOnLanding.desc")))
                                .binding(playSoundOnLandingDefault, () -> ModConfig.INSTANCE.playSoundOnLanding,
                                        newVal -> ModConfig.INSTANCE.playSoundOnLanding = newVal)
                                .controller(StringControllerBuilder::create).build())
                        .option(Option.<Double>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.flightprofile.autoLandSpeed"))
                                .description(OptionDescription.of(Component
                                        .translatable("config.elytraautopilot.flightprofile.autoLandSpeed.desc")))
                                .binding(autoLandSpeedDefault, () -> ModConfig.INSTANCE.autoLandSpeed,
                                        newVal -> ModConfig.INSTANCE.autoLandSpeed = newVal)
                                .controller(DoubleFieldControllerBuilder::create).build())
                        .option(Option.<Double>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.flightprofile.turningSpeed"))
                                .description(OptionDescription.of(Component
                                        .translatable("config.elytraautopilot.flightprofile.turningSpeed.desc")))
                                .binding(turningSpeedDefault, () -> ModConfig.INSTANCE.turningSpeed,
                                        newVal -> ModConfig.INSTANCE.turningSpeed = newVal)
                                .controller(DoubleFieldControllerBuilder::create).build())
                        .option(Option.<Double>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.flightprofile.takeOffPull"))
                                .description(OptionDescription.of(Component
                                        .translatable("config.elytraautopilot.flightprofile.takeOffPull.desc")))
                                .binding(takeOffPullDefault, () -> ModConfig.INSTANCE.takeOffPull,
                                        newVal -> ModConfig.INSTANCE.takeOffPull = newVal)
                                .controller(DoubleFieldControllerBuilder::create).build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.flightprofile.riskyLanding"))
                                .description(OptionDescription.of(Component
                                        .translatable("config.elytraautopilot.flightprofile.riskyLanding.desc")))
                                .binding(riskyLandingDefault, () -> ModConfig.INSTANCE.riskyLanding,
                                        newVal -> ModConfig.INSTANCE.riskyLanding = newVal)
                                .controller(BooleanControllerBuilder::create).build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.flightprofile.directLanding"))
                                .description(OptionDescription.of(Component
                                        .translatable("config.elytraautopilot.flightprofile.directLanding.desc")))
                                .binding(directLandingDefault, () -> ModConfig.INSTANCE.directLanding,
                                        newVal -> ModConfig.INSTANCE.directLanding = newVal)
                                .controller(BooleanControllerBuilder::create).build())
                        .option(Option.<Double>createBuilder()
                                .name(Component
                                        .translatable("config.elytraautopilot.flightprofile.directLandingGlideAngle"))
                                .description(OptionDescription.of(Component.translatable(
                                        "config.elytraautopilot.flightprofile.directLandingGlideAngle.desc")))
                                .binding(directLandingGlideAngleDefault,
                                        () -> ModConfig.INSTANCE.directLandingGlideAngle,
                                        newVal -> ModConfig.INSTANCE.directLandingGlideAngle = newVal)
                                .controller(opt -> DoubleSliderControllerBuilder.create(opt)
                                        .range(minDirectLandingAngle, maxDirectLandingAngle).step(1.0))
                                .build())
                        .option(Option.<Double>createBuilder()
                                .name(Component
                                        .translatable("config.elytraautopilot.flightprofile.directLandingMaxAngle"))
                                .description(OptionDescription.of(Component.translatable(
                                        "config.elytraautopilot.flightprofile.directLandingMaxAngle.desc")))
                                .binding(directLandingMaxAngleDefault, () -> ModConfig.INSTANCE.directLandingMaxAngle,
                                        newVal -> ModConfig.INSTANCE.directLandingMaxAngle = newVal)
                                .controller(opt -> DoubleSliderControllerBuilder.create(opt)
                                        .range(minDirectLandingAngle, maxDirectLandingAngle).step(1.0))
                                .build())
                        .option(Option.<Double>createBuilder()
                                .name(Component
                                        .translatable("config.elytraautopilot.flightprofile.directLandingMinDistance"))
                                .description(OptionDescription.of(Component.translatable(
                                        "config.elytraautopilot.flightprofile.directLandingMinDistance.desc")))
                                .binding(directLandingMinDistanceDefault,
                                        () -> ModConfig.INSTANCE.directLandingMinDistance,
                                        newVal -> ModConfig.INSTANCE.directLandingMinDistance = newVal)
                                .controller(opt -> DoubleSliderControllerBuilder.create(opt)
                                        .range(minDirectLandingDistance, maxDirectLandingDistance).step(5.0))
                                .build())
                        .option(Option.<Double>createBuilder()
                                .name(Component
                                        .translatable("config.elytraautopilot.flightprofile.directLandingFlareHeight"))
                                .description(OptionDescription.of(Component.translatable(
                                        "config.elytraautopilot.flightprofile.directLandingFlareHeight.desc")))
                                .binding(directLandingFlareHeightDefault,
                                        () -> ModConfig.INSTANCE.directLandingFlareHeight,
                                        newVal -> ModConfig.INSTANCE.directLandingFlareHeight = newVal)
                                .controller(opt -> DoubleSliderControllerBuilder.create(opt)
                                        .range(minDirectLandingFlareHeight, maxDirectLandingFlareHeight).step(1.0))
                                .build())
                        .option(Option.<Double>createBuilder()
                                .name(Component
                                        .translatable("config.elytraautopilot.flightprofile.directLandingFlareAngle"))
                                .description(OptionDescription.of(Component.translatable(
                                        "config.elytraautopilot.flightprofile.directLandingFlareAngle.desc")))
                                .binding(directLandingFlareAngleDefault,
                                        () -> ModConfig.INSTANCE.directLandingFlareAngle,
                                        newVal -> ModConfig.INSTANCE.directLandingFlareAngle = newVal)
                                .controller(opt -> DoubleSliderControllerBuilder.create(opt)
                                        .range(minDirectLandingFlareAngle, maxDirectLandingFlareAngle).step(1.0))
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.flightprofile.poweredFlight"))
                                .description(OptionDescription.of(Component
                                        .translatable("config.elytraautopilot.flightprofile.poweredFlight.desc")))
                                .binding(poweredFlightDefault, () -> ModConfig.INSTANCE.poweredFlight,
                                        newVal -> ModConfig.INSTANCE.poweredFlight = newVal)
                                .controller(BooleanControllerBuilder::create).build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.flightprofile.elytraHotswap"))
                                .description(OptionDescription.of(Component
                                        .translatable("config.elytraautopilot.flightprofile.elytraHotswap.desc")))
                                .binding(elytraHotswapDefault, () -> ModConfig.INSTANCE.elytraHotswap,
                                        newVal -> ModConfig.INSTANCE.elytraHotswap = newVal)
                                .controller(BooleanControllerBuilder::create).build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.flightprofile.fireworkHotswap"))
                                .description(OptionDescription.of(Component
                                        .translatable("config.elytraautopilot.flightprofile.fireworkHotswap.desc")))
                                .binding(fireworkHotswapDefault, () -> ModConfig.INSTANCE.fireworkHotswap,
                                        newVal -> ModConfig.INSTANCE.fireworkHotswap = newVal)
                                .controller(BooleanControllerBuilder::create).build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.flightprofile.emergencyLand"))
                                .description(OptionDescription.of(Component
                                        .translatable("config.elytraautopilot.flightprofile.emergencyLand.desc")))
                                .binding(emergencyLandDefault, () -> ModConfig.INSTANCE.emergencyLand,
                                        newVal -> ModConfig.INSTANCE.emergencyLand = newVal)
                                .controller(BooleanControllerBuilder::create).build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component
                                        .translatable("config.elytraautopilot.flightprofile.elytraReplaceDurability"))
                                .description(OptionDescription.of(Component.translatable(
                                        "config.elytraautopilot.flightprofile.elytraReplaceDurability.desc")))
                                .binding(elytraReplaceDurabilityDefault,
                                        () -> ModConfig.INSTANCE.elytraReplaceDurability,
                                        newVal -> ModConfig.INSTANCE.elytraReplaceDurability = newVal)
                                .controller(opt -> IntegerFieldControllerBuilder.create(opt).min(3).max(430)).build())
                        .option(Option.<Boolean>createBuilder().name(Component.nullToEmpty("Elytra Auto equip/swap"))
                                .description(OptionDescription.of(Component.nullToEmpty(
                                        "Enable automatic elytra equip or chestplate swap when double jump (try to glide)")))
                                .binding(elytraAutoSwapDefault, () -> ModConfig.INSTANCE.elytraAutoSwap,
                                        newVal -> ModConfig.INSTANCE.elytraAutoSwap = newVal)
                                .controller(BooleanControllerBuilder::create).build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.flightprofile.cameraDecoupled"))
                                .description(OptionDescription.of(Component
                                        .translatable("config.elytraautopilot.flightprofile.cameraDecoupled.desc")))
                                .binding(cameraDecoupledDefault, () -> ModConfig.INSTANCE.cameraDecoupled,
                                        newVal -> ModConfig.INSTANCE.cameraDecoupled = newVal)
                                .controller(BooleanControllerBuilder::create).build())
                        .build())
                .category(ConfigCategory.createBuilder().name(Component.translatable("config.elytraautopilot.strategy"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.strategy.strategyMode"))
                                .description(OptionDescription.of(
                                        Component.translatable("config.elytraautopilot.strategy.strategyMode.desc")))
                                .binding(strategyModeDefault, () -> ModConfig.INSTANCE.strategyMode,
                                        newVal -> ModConfig.INSTANCE.strategyMode = newVal)
                                .controller(BooleanControllerBuilder::create).build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.strategy.cruiseAltitudeMin"))
                                .description(OptionDescription.of(Component
                                        .translatable("config.elytraautopilot.strategy.cruiseAltitudeMin.desc")))
                                .binding(cruiseAltitudeMinDefault, () -> ModConfig.INSTANCE.cruiseAltitudeMin,
                                        newVal -> ModConfig.INSTANCE.cruiseAltitudeMin = newVal)
                                .controller(opt -> IntegerFieldControllerBuilder.create(opt).min(minCruiseAltitude)
                                        .max(maxCruiseAltitude))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.strategy.cruiseAltitudeMax"))
                                .description(OptionDescription.of(Component
                                        .translatable("config.elytraautopilot.strategy.cruiseAltitudeMax.desc")))
                                .binding(cruiseAltitudeMaxDefault, () -> ModConfig.INSTANCE.cruiseAltitudeMax,
                                        newVal -> ModConfig.INSTANCE.cruiseAltitudeMax = newVal)
                                .controller(opt -> IntegerFieldControllerBuilder.create(opt).min(minCruiseAltitude)
                                        .max(maxCruiseAltitude))
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("config.elytraautopilot.trajectory"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.trajectory.prediction"))
                                .description(OptionDescription.of(
                                        Component.translatable("config.elytraautopilot.trajectory.prediction.desc")))
                                .binding(trajectoryPredictionDefault, () -> ModConfig.INSTANCE.trajectoryPrediction,
                                        newVal -> ModConfig.INSTANCE.trajectoryPrediction = newVal)
                                .controller(BooleanControllerBuilder::create).build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.trajectory.show"))
                                .description(OptionDescription
                                        .of(Component.translatable("config.elytraautopilot.trajectory.show.desc")))
                                .binding(showTrajectoryDefault, () -> ModConfig.INSTANCE.showTrajectory,
                                        newVal -> ModConfig.INSTANCE.showTrajectory = newVal)
                                .controller(BooleanControllerBuilder::create).build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.trajectory.previewTicks"))
                                .description(OptionDescription.of(
                                        Component.translatable("config.elytraautopilot.trajectory.previewTicks.desc")))
                                .binding(trajectoryPreviewTicksDefault, () -> ModConfig.INSTANCE.trajectoryPreviewTicks,
                                        newVal -> ModConfig.INSTANCE.trajectoryPreviewTicks = newVal)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                        .range(minTrajectoryPreviewTicks, maxTrajectoryPreviewTicks).step(20))
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("config.elytraautopilot.avoidance"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.avoidance.obstacleAvoidance"))
                                .description(OptionDescription.of(Component
                                        .translatable("config.elytraautopilot.avoidance.obstacleAvoidance.desc")))
                                .binding(obstacleAvoidanceDefault, () -> ModConfig.INSTANCE.obstacleAvoidance,
                                        newVal -> ModConfig.INSTANCE.obstacleAvoidance = newVal)
                                .controller(BooleanControllerBuilder::create).build())
                        .option(Option.<Double>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.avoidance.lookahead"))
                                .description(OptionDescription
                                        .of(Component.translatable("config.elytraautopilot.avoidance.lookahead.desc")))
                                .binding(avoidanceLookaheadDefault, () -> ModConfig.INSTANCE.avoidanceLookahead,
                                        newVal -> ModConfig.INSTANCE.avoidanceLookahead = newVal)
                                .controller(opt -> DoubleSliderControllerBuilder.create(opt)
                                        .range(minAvoidanceLookahead, maxAvoidanceLookahead).step(0.5))
                                .build())
                        .option(Option.<Double>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.avoidance.clearance"))
                                .description(OptionDescription
                                        .of(Component.translatable("config.elytraautopilot.avoidance.clearance.desc")))
                                .binding(avoidanceClearanceDefault, () -> ModConfig.INSTANCE.avoidanceClearance,
                                        newVal -> ModConfig.INSTANCE.avoidanceClearance = newVal)
                                .controller(opt -> DoubleSliderControllerBuilder.create(opt)
                                        .range(minAvoidanceClearance, maxAvoidanceClearance).step(0.5))
                                .build())
                        .option(Option.<Double>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.avoidance.maxClimbAngle"))
                                .description(OptionDescription.of(
                                        Component.translatable("config.elytraautopilot.avoidance.maxClimbAngle.desc")))
                                .binding(avoidanceMaxClimbAngleDefault, () -> ModConfig.INSTANCE.avoidanceMaxClimbAngle,
                                        newVal -> ModConfig.INSTANCE.avoidanceMaxClimbAngle = newVal)
                                .controller(opt -> DoubleSliderControllerBuilder.create(opt)
                                        .range(minAvoidanceAngle, maxAvoidanceAngle).step(1.0))
                                .build())
                        .option(Option.<Double>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.avoidance.maxDescentAngle"))
                                .description(OptionDescription.of(Component
                                        .translatable("config.elytraautopilot.avoidance.maxDescentAngle.desc")))
                                .binding(avoidanceMaxDescentAngleDefault,
                                        () -> ModConfig.INSTANCE.avoidanceMaxDescentAngle,
                                        newVal -> ModConfig.INSTANCE.avoidanceMaxDescentAngle = newVal)
                                .controller(opt -> DoubleSliderControllerBuilder.create(opt)
                                        .range(minAvoidanceAngle, maxAvoidanceAngle).step(1.0))
                                .build())
                        .option(Option.<Double>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.avoidance.pitchRate"))
                                .description(OptionDescription
                                        .of(Component.translatable("config.elytraautopilot.avoidance.pitchRate.desc")))
                                .binding(avoidancePitchRateDefault, () -> ModConfig.INSTANCE.avoidancePitchRate,
                                        newVal -> ModConfig.INSTANCE.avoidancePitchRate = newVal)
                                .controller(opt -> DoubleSliderControllerBuilder.create(opt)
                                        .range(minAvoidancePitchRate, maxAvoidancePitchRate).step(0.1))
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.avoidance.energyAware"))
                                .description(OptionDescription.of(
                                        Component.translatable("config.elytraautopilot.avoidance.energyAware.desc")))
                                .binding(avoidanceEnergyAwareDefault, () -> ModConfig.INSTANCE.avoidanceEnergyAware,
                                        newVal -> ModConfig.INSTANCE.avoidanceEnergyAware = newVal)
                                .controller(BooleanControllerBuilder::create).build())
                        .option(Option.<Double>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.avoidance.climbMinSpeed"))
                                .description(OptionDescription.of(
                                        Component.translatable("config.elytraautopilot.avoidance.climbMinSpeed.desc")))
                                .binding(avoidanceClimbMinSpeedDefault, () -> ModConfig.INSTANCE.avoidanceClimbMinSpeed,
                                        newVal -> ModConfig.INSTANCE.avoidanceClimbMinSpeed = newVal)
                                .controller(opt -> DoubleSliderControllerBuilder.create(opt)
                                        .range(minAvoidanceClimbSpeed, maxAvoidanceClimbSpeed).step(0.05))
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.avoidance.giveUp"))
                                .description(OptionDescription
                                        .of(Component.translatable("config.elytraautopilot.avoidance.giveUp.desc")))
                                .binding(avoidanceGiveUpDefault, () -> ModConfig.INSTANCE.avoidanceGiveUp,
                                        newVal -> ModConfig.INSTANCE.avoidanceGiveUp = newVal)
                                .controller(BooleanControllerBuilder::create).build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.avoidance.giveUpTicks"))
                                .description(OptionDescription.of(
                                        Component.translatable("config.elytraautopilot.avoidance.giveUpTicks.desc")))
                                .binding(avoidanceGiveUpTicksDefault, () -> ModConfig.INSTANCE.avoidanceGiveUpTicks,
                                        newVal -> ModConfig.INSTANCE.avoidanceGiveUpTicks = newVal)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                        .range(minAvoidanceGiveUpTicks, maxAvoidanceGiveUpTicks).step(20))
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder().name(Component.translatable("config.elytraautopilot.advanced"))
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.advanced.groundCheckTicks"))
                                .description(OptionDescription.of(Component
                                        .translatable("config.elytraautopilot.advanced.groundCheckTicks.desc")))
                                .binding(groundCheckTicksDefault, () -> ModConfig.INSTANCE.groundCheckTicks,
                                        newVal -> ModConfig.INSTANCE.groundCheckTicks = newVal)
                                .controller(IntegerFieldControllerBuilder::create).build())
                        .option(Option.<Double>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.advanced.pullUpAngle"))
                                .description(OptionDescription
                                        .of(Component.translatable("config.elytraautopilot.advanced.pullUpAngle.desc")))
                                .binding(pullUpAngleDefault, () -> ModConfig.INSTANCE.pullUpAngle,
                                        newVal -> ModConfig.INSTANCE.pullUpAngle = newVal)
                                .controller(DoubleFieldControllerBuilder::create).build())
                        .option(Option.<Double>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.advanced.pullDownAngle"))
                                .description(OptionDescription.of(
                                        Component.translatable("config.elytraautopilot.advanced.pullDownAngle.desc")))
                                .binding(pullDownAngleDefault, () -> ModConfig.INSTANCE.pullDownAngle,
                                        newVal -> ModConfig.INSTANCE.pullDownAngle = newVal)
                                .controller(DoubleFieldControllerBuilder::create).build())
                        .option(Option.<Double>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.advanced.pullUpMinVelocity"))
                                .description(OptionDescription.of(Component
                                        .translatable("config.elytraautopilot.advanced.pullUpMinVelocity.desc")))
                                .binding(pullUpMinVelocityDefault, () -> ModConfig.INSTANCE.pullUpMinVelocity,
                                        newVal -> ModConfig.INSTANCE.pullUpMinVelocity = newVal)
                                .controller(DoubleFieldControllerBuilder::create).build())
                        .option(Option.<Double>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.advanced.pullDownMaxVelocity"))
                                .description(OptionDescription.of(Component
                                        .translatable("config.elytraautopilot.advanced.pullDownMaxVelocity.desc")))
                                .binding(pullDownMaxVelocityDefault, () -> ModConfig.INSTANCE.pullDownMaxVelocity,
                                        newVal -> ModConfig.INSTANCE.pullDownMaxVelocity = newVal)
                                .controller(DoubleFieldControllerBuilder::create).build())
                        .option(Option.<Double>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.advanced.pullUpSpeed"))
                                .description(OptionDescription
                                        .of(Component.translatable("config.elytraautopilot.advanced.pullUpSpeed.desc")))
                                .binding(pullUpSpeedDefault, () -> ModConfig.INSTANCE.pullUpSpeed,
                                        newVal -> ModConfig.INSTANCE.pullUpSpeed = newVal)
                                .controller(DoubleFieldControllerBuilder::create).build())
                        .option(Option.<Double>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.advanced.pullDownSpeed"))
                                .description(OptionDescription.of(
                                        Component.translatable("config.elytraautopilot.advanced.pullDownSpeed.desc")))
                                .binding(pullDownSpeedDefault, () -> ModConfig.INSTANCE.pullDownSpeed,
                                        newVal -> ModConfig.INSTANCE.pullDownSpeed = newVal)
                                .controller(DoubleFieldControllerBuilder::create).build())
                        .option(ListOption.<String>createBuilder()
                                .name(Component.translatable("config.elytraautopilot.advanced.flyLocations"))
                                .binding(flyLocationsDefault, () -> ModConfig.INSTANCE.flyLocations,
                                        newVal -> ModConfig.INSTANCE.flyLocations = newVal)
                                .controller(StringControllerBuilder::create).initial("Location;0;0").build())
                        .build())
                .save(() -> INSTANCE.saveConfig(CONFIG_FILE.toFile())).build().generateScreen(parent);
    }

    public void saveConfig(File file) {
        Logger logger = LoggerFactory.getLogger("ElytraAutoPilot");
        logger.info(GSON.toString());
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static ModConfig loadConfig(File file) {
        ModConfig config = null;

        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                config = GSON.fromJson(reader, ModConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (config == null) {
            config = new ModConfig();
        }

        if (config.flyLocations == null) {
            config.flyLocations = new ArrayList<>();
        }

        config.saveConfig(file);
        return config;
    }
}
