package com.lightdust.config;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.Arrays;
import java.util.List;

public class LightDustConfig {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // Spawning & Performance
    public static final ForgeConfigSpec.IntValue AMBIENT_RADIUS;
    public static final ForgeConfigSpec.IntValue AMBIENT_HARD_CAP;
    public static final ForgeConfigSpec.IntValue AMBIENT_BLOCK_CAP;
    public static final ForgeConfigSpec.IntValue MIN_BLOCK_LIGHT;
    public static final ForgeConfigSpec.IntValue DAYTIME_LIGHT_DIFF;
    public static final ForgeConfigSpec.IntValue FALLOFF_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue FALLOFF_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue ENABLE_OCCLUSION_CULLING;

    // Visuals & Environment
    public static final ForgeConfigSpec.DoubleValue AMBIENT_DUST_OPACITY;
    public static final ForgeConfigSpec.DoubleValue PARTICLE_SIZE;
    public static final ForgeConfigSpec.IntValue PARTICLE_LIFETIME;
    public static final ForgeConfigSpec.DoubleValue WIND_SPEED_CLEAR;
    public static final ForgeConfigSpec.DoubleValue WIND_SPEED_RAIN;
    public static final ForgeConfigSpec.DoubleValue WIND_SPEED_THUNDER;
    public static final ForgeConfigSpec.BooleanValue DISABLE_DURING_RAIN;
    public static final ForgeConfigSpec.BooleanValue DISABLE_DURING_THUNDER;

    // Player Interactions & World Actions
    public static final ForgeConfigSpec.DoubleValue PLAYER_INTERACT_RADIUS;
    public static final ForgeConfigSpec.IntValue BREAK_PARTICLE_COUNT;
    public static final ForgeConfigSpec.DoubleValue BREAK_PARTICLE_SPEED;
    public static final ForgeConfigSpec.DoubleValue ACTION_DUST_GRAVITY;
    public static final ForgeConfigSpec.DoubleValue ACTION_DUST_BOUNCE;

    public static final ForgeConfigSpec.IntValue HEAVY_LANDING_MAX_PARTICLES;
    public static final ForgeConfigSpec.IntValue HEAVY_LANDING_PARTICLE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue HEAVY_LANDING_UPWARD_SPEED;
    public static final ForgeConfigSpec.DoubleValue HEAVY_LANDING_OUTWARD_SPEED;
    public static final ForgeConfigSpec.DoubleValue HEAVY_LANDING_AMBIENT_PUSH;
    public static final ForgeConfigSpec.DoubleValue HEAVY_LANDING_AMBIENT_RADIUS;

    // Handheld lights
    public static final ForgeConfigSpec.BooleanValue ENABLE_HANDHELD_LIGHTS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> HANDHELD_LIGHT_ITEMS;

    public static final List<String> DEFAULT_HANDHELD_LIGHTS = Arrays.asList(
            "minecraft:torch=14,#FFDB8A",
            "minecraft:lantern=15,#FFDB8A",
            "minecraft:campfire=15,#FFDB8A",
            "minecraft:glowstone=15,#FFD273",
            "minecraft:shroomlight=15,#FF9933",
            "minecraft:jack_o_lantern=15,#FF9933",
            "minecraft:soul_torch=10,#4CBAFF",
            "minecraft:soul_lantern=10,#4CBAFF",
            "minecraft:soul_campfire=10,#4CBAFF",
            "minecraft:sea_lantern=15,#8CD9C9",
            "minecraft:conduit=15,#66E5CC",
            "minecraft:end_rod=14,#FFE6FF",
            "minecraft:beacon=15,#99FFFF",
            "minecraft:crying_obsidian=10,#B300FF",
            "minecraft:amethyst_cluster=5,#B266FF",
            "minecraft:redstone_torch=7,#FF0000",
            "minecraft:redstone_lamp=15,#FF3300",
            "minecraft:ochre_froglight=15,#FFDCA8",
            "minecraft:pearlescent_froglight=15,#FFE8E8",
            "minecraft:verdant_froglight=15,#E8FFD4",
            "minecraft:glow_berries=14,#FFB233",
            "minecraft:glow_lichen=7,#99CCB3",
            "minecraft:magma_block=3,#FF3300",
            // tags
            "#minecraft:torches=14,#FFDB8A",
            "#minecraft:campfires=15,#FF9933"
    );

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> HEAT_BLOCKS;

    public static final List<String> DEFAULT_HEAT_BLOCKS = Arrays.asList(
            "minecraft:torch=0.015,2,0.4",
            "minecraft:wall_torch=0.015,2,0.4",
            "minecraft:soul_torch=0.015,2,0.4",
            "minecraft:soul_wall_torch=0.015,2,0.4",
            "minecraft:magma_block=0.02,3,0.7",
            "minecraft:campfire=0.035,5,0.5",
            "minecraft:soul_campfire=0.035,5,0.5",
            "minecraft:lava=0.045,5,0.7"
    );

    static {
        BUILDER.push("Spawning and Performance");
        AMBIENT_RADIUS = BUILDER.comment("Spawn Radius: How far away from the player (in blocks) dust will attempt to spawn.")
                .defineInRange("ambientRadius", 10, 1, 32);
        AMBIENT_HARD_CAP = BUILDER.comment("Hard Cap Radius: The absolute max distance dust can exist.")
                .defineInRange("ambientHardCapRadius", 12, 1, 48);
        AMBIENT_BLOCK_CAP = BUILDER.comment("Max dust particles allowed per single block.")
                .defineInRange("ambientBlockCap", 8, 1, 32);
        MIN_BLOCK_LIGHT = BUILDER.comment("Minimum block light level required for ambient dust to spawn.")
                .defineInRange("minBlockLight", 6, 0, 15);
        DAYTIME_LIGHT_DIFF = BUILDER.comment("The minimum difference between Block Light and Sky Light required for dust to spawn during the day.")
                .defineInRange("daytimeLightDiff", 5, 0, 15);
        FALLOFF_DISTANCE = BUILDER.comment("Distance from the player where dust density starts to rapidly decrease.")
                .defineInRange("falloffDistance", 6, 1, 32);
        FALLOFF_MULTIPLIER = BUILDER.comment("Multiplier applied to the max particle cap beyond the falloff distance.")
                .defineInRange("falloffMultiplier", 0.3, 0.0, 1.0);
        ENABLE_OCCLUSION_CULLING = BUILDER.comment("If true, uses raytracing to prevent dust from spawning behind walls you can't see. Highly recommended for performance.")
                .define("enableOcclusionCulling", true);
        BUILDER.pop();

        BUILDER.push("Visuals and Environment");
        AMBIENT_DUST_OPACITY = BUILDER.comment("Base opacity for ambient dust (0.22 for vanilla, 0.45+ for shaders)")
                .defineInRange("ambientDustOpacity", 0.22, 0.0, 1.0);
        PARTICLE_SIZE = BUILDER.comment("Base size of the dust particles.")
                .defineInRange("particleSize", 0.022, 0.001, 0.1);
        PARTICLE_LIFETIME = BUILDER.comment("Base lifetime of the dust particles in ticks.")
                .defineInRange("particleLifetime", 200, 20, 1000);
        WIND_SPEED_CLEAR = BUILDER.comment("Base wind speed modifier during clear weather.")
                .defineInRange("windSpeedClear", 0.25, 0.0, 1.0);
        WIND_SPEED_RAIN = BUILDER.comment("Base wind speed modifier during rain.")
                .defineInRange("windSpeedRain", 0.35, 0.0, 1.0);
        WIND_SPEED_THUNDER = BUILDER.comment("Base wind speed modifier during thunderstorms.")
                .defineInRange("windSpeedThunder", 0.5, 0.0, 1.0);
        DISABLE_DURING_RAIN = BUILDER.comment("If true, outdoor dust will despawn and stop spawning during rain.")
                .define("disableDuringRain", false);
        DISABLE_DURING_THUNDER = BUILDER.comment("If true, outdoor dust will despawn and stop spawning during thunderstorms.")
                .define("disableDuringThunder", false);
        BUILDER.pop();

        BUILDER.push("Interactions and Actions");
        PLAYER_INTERACT_RADIUS = BUILDER.comment("Radius squared for player interaction (slashing/moving).")
                .defineInRange("playerInteractRadius", 4.0, 0.0, 16.0);

        BREAK_PARTICLE_COUNT = BUILDER.comment("Number of dust particles spawned when a block is broken.")
                .defineInRange("breakParticleCount", 12, 0, 50);
        BREAK_PARTICLE_SPEED = BUILDER.comment("How fast the dust shoots out from a broken block.")
                .defineInRange("breakParticleSpeed", 0.1, 0.0, 1.0);
        ACTION_DUST_GRAVITY = BUILDER.comment("How fast block-breaking dust falls downwards over time.")
                .defineInRange("actionDustGravity", 0.002, 0.0, 0.05);
        ACTION_DUST_BOUNCE = BUILDER.comment("How bouncy block-breaking dust is when hitting walls or ceilings. (0.0 = no bounce, 1.0 = super bouncy)")
                .defineInRange("actionDustBounce", 0.2, 0.0, 1.0);

        HEAVY_LANDING_MAX_PARTICLES = BUILDER.comment("The absolute maximum number of dust particles that can spawn from a heavy landing.")
                .defineInRange("heavyLandingMaxParticles", 96, 0, 300);
        HEAVY_LANDING_PARTICLE_MULTIPLIER = BUILDER.comment("Multiplier for landing dust. (Blocks fallen * Multiplier = Particles spawned).")
                .defineInRange("heavyLandingParticleMultiplier", 12, 0, 50);
        HEAVY_LANDING_UPWARD_SPEED = BUILDER.comment("Base upward speed for heavy landing action dust.")
                .defineInRange("heavyLandingUpwardSpeed", 0.2, 0.0, 2.0);
        HEAVY_LANDING_OUTWARD_SPEED = BUILDER.comment("Base outward speed for heavy landing action dust.")
                .defineInRange("heavyLandingOutwardSpeed", 0.12, 0.0, 2.0);
        HEAVY_LANDING_AMBIENT_PUSH = BUILDER.comment("How strongly a heavy landing pushes preexisting ambient dust away.")
                .defineInRange("heavyLandingAmbientPush", 0.001, 0.0, 0.1);
        HEAVY_LANDING_AMBIENT_RADIUS = BUILDER.comment("The radius (in blocks) that ambient dust is pushed away from a heavy landing.")
                .defineInRange("heavyLandingAmbientRadius", 4.0, 1.0, 16.0);

        HEAT_BLOCKS = BUILDER.comment("List of blocks that emit heat, causing dust particles to swirl upwards in a thermal updraft.")
                .defineList("heatBlocks", DEFAULT_HEAT_BLOCKS, obj -> obj instanceof String && ((String) obj).split(",").length == 3);
        BUILDER.pop();

        BUILDER.push("Handheld lights (dynamic lights 'compat')");
        ENABLE_HANDHELD_LIGHTS = BUILDER.comment("If true, items held in your hand will act as fake light sources, allowing dust to spawn and coloring the dust. You still need to install a dynamic lights mod. This only spawns dust")
                .define("enableHandheldLights", false);
        HANDHELD_LIGHT_ITEMS = BUILDER.comment(
                "List of items/tags and their light properties.",
                "Format: 'modid:item_name=radius,#HEXCOLOR' or '#modid:tag=radius,#HEXCOLOR'."
        ).defineList("handheldLightItems", DEFAULT_HANDHELD_LIGHTS,
                obj -> obj instanceof String && ((String) obj).contains("=") && ((String) obj).contains(","));
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
