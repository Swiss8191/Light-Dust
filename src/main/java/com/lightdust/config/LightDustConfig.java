package com.lightdust.config;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.Arrays;
import java.util.List;

public class LightDustConfig {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    //  CORE SPAWNING 
    public static final ForgeConfigSpec.IntValue AMBIENT_RADIUS;
    public static final ForgeConfigSpec.IntValue AMBIENT_HARD_CAP;
    public static final ForgeConfigSpec.IntValue AMBIENT_BLOCK_CAP;
    public static final ForgeConfigSpec.BooleanValue ENABLE_MAX_Y_LEVEL;
    public static final ForgeConfigSpec.IntValue MAX_Y_LEVEL;

    //  LIGHT & CULLING 
    public static final ForgeConfigSpec.IntValue MIN_BLOCK_LIGHT;
    public static final ForgeConfigSpec.IntValue DAYTIME_LIGHT_DIFF;
    public static final ForgeConfigSpec.IntValue FALLOFF_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue FALLOFF_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue ENABLE_OCCLUSION_CULLING;

    //  VISUAL PROPERTIES 
    public static final ForgeConfigSpec.BooleanValue USE_HD_PARTICLES;
    public static final ForgeConfigSpec.DoubleValue AMBIENT_DUST_OPACITY;
    public static final ForgeConfigSpec.DoubleValue PARTICLE_SIZE;
    public static final ForgeConfigSpec.IntValue PARTICLE_LIFETIME;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DYNAMIC_BLOCK_COLORS;

    //  WEATHER & WIND 
    public static final ForgeConfigSpec.DoubleValue WIND_SPEED_CLEAR;
    public static final ForgeConfigSpec.DoubleValue WIND_SPEED_RAIN;
    public static final ForgeConfigSpec.DoubleValue WIND_SPEED_THUNDER;
    public static final ForgeConfigSpec.BooleanValue DISABLE_DURING_RAIN;
    public static final ForgeConfigSpec.BooleanValue DISABLE_DURING_THUNDER;

    //  BLOCK BREAKING PHYSICS 
    public static final ForgeConfigSpec.DoubleValue PLAYER_INTERACT_RADIUS;
    public static final ForgeConfigSpec.IntValue BREAK_PARTICLE_COUNT;
    public static final ForgeConfigSpec.DoubleValue BREAK_PARTICLE_SPEED;
    public static final ForgeConfigSpec.DoubleValue ACTION_DUST_GRAVITY;
    public static final ForgeConfigSpec.DoubleValue ACTION_DUST_BOUNCE;
    public static final ForgeConfigSpec.DoubleValue BREAK_VACUUM_RADIUS;
    public static final ForgeConfigSpec.DoubleValue BREAK_VACUUM_FORCE;

    //  HEAVY LANDING PHYSICS 
    public static final ForgeConfigSpec.IntValue HEAVY_LANDING_MAX_PARTICLES;
    public static final ForgeConfigSpec.IntValue HEAVY_LANDING_PARTICLE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue HEAVY_LANDING_UPWARD_SPEED;
    public static final ForgeConfigSpec.DoubleValue HEAVY_LANDING_OUTWARD_SPEED;
    public static final ForgeConfigSpec.DoubleValue HEAVY_LANDING_AMBIENT_PUSH;
    public static final ForgeConfigSpec.DoubleValue HEAVY_LANDING_AMBIENT_RADIUS;

    //  EXPLOSIONS & CAVE TREMORS 
    public static final ForgeConfigSpec.IntValue CAVE_TREMOR_PARTICLE_COUNT;
    public static final ForgeConfigSpec.DoubleValue TREMOR_EPICENTER_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue TREMOR_FALLOFF_EXPONENT;
    public static final ForgeConfigSpec.DoubleValue TREMOR_CEILING_BIAS;
    public static final ForgeConfigSpec.DoubleValue TREMOR_FLOOR_KICK_FORCE;
    public static final ForgeConfigSpec.DoubleValue TREMOR_WAVEFRONT_THICKNESS;
    public static final ForgeConfigSpec.DoubleValue TREMOR_MAX_GRAVITY;
    public static final ForgeConfigSpec.DoubleValue TREMOR_MIN_GRAVITY;
    public static final ForgeConfigSpec.DoubleValue TREMOR_ROOF_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue TREMOR_FLOOR_MULTIPLIER;

    //  THERMAL DYNAMICS 
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> HEAT_BLOCKS;

    //  HANDHELD LIGHTS
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
            "#minecraft:torches=14,#FFDB8A",
            "#minecraft:campfires=15,#FF9933"
    );

    public static final List<String> DEFAULT_HEAT_BLOCKS = Arrays.asList(
            "minecraft:torch=0.015,2,0.4",
            "minecraft:wall_torch=0.015,2,0.4",
            "minecraft:soul_torch=0.015,2,0.4",
            "minecraft:soul_wall_torch=0.015,2,0.4",
            "minecraft:magma_block=0.02,3,0.7",
            "minecraft:campfire=0.035,5,0.5",
            "minecraft:soul_campfire=0.035,5,0.5",
            "minecraft:lava=0.045,5,0.7",
            "minecraft:fire=0.035,4,0.6",
            "minecraft:soul_fire=0.035,4,0.6"
    );

    static {
        BUILDER.push("Spawning Rules");
        AMBIENT_RADIUS = BUILDER.comment("Spawn Radius: How far away from the player (in blocks) dust will attempt to spawn.")
                .defineInRange("ambientRadius", 10, 1, 32);
        AMBIENT_HARD_CAP = BUILDER.comment("Hard Cap Radius: The absolute max distance dust can exist before being deleted.")
                .defineInRange("ambientHardCapRadius", 12, 1, 48);
        AMBIENT_BLOCK_CAP = BUILDER.comment("Max dust particles allowed per single block.")
                .defineInRange("ambientBlockCap", 12, 1, 32);
        ENABLE_MAX_Y_LEVEL = BUILDER.comment("If true, ambient dust will stop spawning above the maxYLevel. (False recommended to keep outdoor wind dust)")
                .define("enableMaxYLevel", false);
        MAX_Y_LEVEL = BUILDER.comment("Maximum Y-level (height) where ambient dust is allowed to spawn. Set to ~60 to avoid surface houses.")
                .defineInRange("maxYLevel", 60, -64, 320);
        BUILDER.pop();

        BUILDER.push("Lighting & Performance");
        MIN_BLOCK_LIGHT = BUILDER.comment("Minimum block light level required for ambient dust to spawn.")
                .defineInRange("minBlockLight", 6, 0, 15);
        DAYTIME_LIGHT_DIFF = BUILDER.comment("The minimum difference between Block Light and Sky Light required for dust to spawn during the day.")
                .defineInRange("daytimeLightDiff", 5, 0, 15);
        FALLOFF_DISTANCE = BUILDER.comment("Distance from the player where dust density starts to rapidly decrease (saves performance).")
                .defineInRange("falloffDistance", 6, 1, 32);
        FALLOFF_MULTIPLIER = BUILDER.comment("Multiplier applied to the max particle cap beyond the falloff distance.")
                .defineInRange("falloffMultiplier", 0.3, 0.0, 1.0);
        ENABLE_OCCLUSION_CULLING = BUILDER.comment("If true, uses raytracing to prevent dust from spawning behind walls you can't see. Highly recommended for performance.")
                .define("enableOcclusionCulling", true);
        BUILDER.pop();

        BUILDER.push("Visuals");
        USE_HD_PARTICLES = BUILDER.comment("If true, the mod will use the HD versions of the dust textures. No performance impact.")
        .define("useHdParticles", true);
        AMBIENT_DUST_OPACITY = BUILDER.comment("Base opacity for ambient dust. (0.22 looks good in vanilla, 0.45+ is better for shader) for HD dust lower these values slightly")
                .defineInRange("ambientDustOpacity", 0.18, 0.0, 1.0);
        PARTICLE_SIZE = BUILDER.comment("Base size of the dust particles.")
                .defineInRange("particleSize", 0.016, 0.001, 0.1);
        PARTICLE_LIFETIME = BUILDER.comment("Base lifetime of the dust particles in ticks. (20 ticks = 1 second)")
                .defineInRange("particleLifetime", 200, 20, 1000);
        ENABLE_DYNAMIC_BLOCK_COLORS = BUILDER.comment(
                "If true, block-breaking dust and heavy landing dust will inherit the exact color of the block being interacted with.",
                "Provides a massive visual upgrade for physics interactions.")
                .define("enableDynamicBlockColors", true);
        BUILDER.pop();

        BUILDER.push("Weather & Wind");
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

        BUILDER.push("Player & Block Breaking Physics");
        PLAYER_INTERACT_RADIUS = BUILDER.comment("Radius squared for player physical interaction (slashing swords, moving quickly).")
                .defineInRange("playerInteractRadius", 4.0, 0.0, 16.0);
        BREAK_PARTICLE_COUNT = BUILDER.comment("Number of dust particles spawned when a block is broken.")
                .defineInRange("breakParticleCount", 12, 0, 50);
        BREAK_PARTICLE_SPEED = BUILDER.comment("How fast the dust shoots outward from the broken block face.")
                .defineInRange("breakParticleSpeed", 0.1, 0.0, 1.0);
        ACTION_DUST_GRAVITY = BUILDER.comment("How fast block-breaking dust falls downwards over time.")
                .defineInRange("actionDustGravity", 2.0, 0.0, 50.0);
        ACTION_DUST_BOUNCE = BUILDER.comment("How bouncy block breaking dust is when hitting walls/floors. (0.0 = no bounce, 1.0 = super bouncy)")
                .defineInRange("actionDustBounce", 0.2, 0.0, 1.0);
        BREAK_VACUUM_RADIUS = BUILDER.comment(
                "When a block turns to air, it creates a momentary vacuum.",
                "This defines how far away (in blocks) ambient dust will be sucked toward the broken block.")
                .defineInRange("breakVacuumRadius", 1.2, 0.0, 16.0);
        BREAK_VACUUM_FORCE = BUILDER.comment("How strong the dust is pulled toward the broken block.")
                .defineInRange("breakVacuumForce", 10.0, 0.0, 1000.0);
        BUILDER.pop();

        BUILDER.push("Heavy Landing Physics");
        HEAVY_LANDING_MAX_PARTICLES = BUILDER.comment("The absolute maximum number of dust particles that can spawn from a hard fall.")
                .defineInRange("heavyLandingMaxParticles", 96, 0, 300);
        HEAVY_LANDING_PARTICLE_MULTIPLIER = BUILDER.comment("Multiplier for landing dust. (Blocks fallen * Multiplier = Particles spawned).")
                .defineInRange("heavyLandingParticleMultiplier", 12, 0, 50);
        HEAVY_LANDING_UPWARD_SPEED = BUILDER.comment("Base upward velocity for dust kicked up by a heavy landing.")
                .defineInRange("heavyLandingUpwardSpeed", 0.2, 0.0, 2.0);
        HEAVY_LANDING_OUTWARD_SPEED = BUILDER.comment("Base outward ring velocity for dust kicked up by a heavy landing.")
                .defineInRange("heavyLandingOutwardSpeed", 0.12, 0.0, 2.0);
        HEAVY_LANDING_AMBIENT_PUSH = BUILDER.comment("How strongly a heavy landing violently pushes preexisting floating ambient dust away.")
                .defineInRange("heavyLandingAmbientPush", 1.0, 0.0, 100.0);
        HEAVY_LANDING_AMBIENT_RADIUS = BUILDER.comment("The radius (in blocks) that ambient dust is pushed away from the landing site.")
                .defineInRange("heavyLandingAmbientRadius", 4.0, 1.0, 16.0);
        BUILDER.pop();

        BUILDER.push("Explosions & Cave Tremors");
        CAVE_TREMOR_PARTICLE_COUNT = BUILDER.comment(
                "Controls the volume of falling dust during explosions or Warden sonic booms.",
                "Note: This is a multiplier against the expanding wave circumference, not a hard particle cap.")
                .defineInRange("caveTremorParticleCount", 64, 0, 1024);
        TREMOR_EPICENTER_MULTIPLIER = BUILDER.comment(
                "Multiplies particle density at the epicenter (within 10% of the max radius).")
                .defineInRange("tremorEpicenterMultiplier", 5.0, 1.0, 20.0);
        TREMOR_FALLOFF_EXPONENT = BUILDER.comment(
                "Exponential Falloff. Controls how fast the tremor loses intensity as it travels outward.",
                "A value of 6.0 means the center is incredibly violent, but it rapidly drops to a gentle dusting further away.")
                .defineInRange("tremorFalloffExponent", 6.0, 1.0, 10.0);
        TREMOR_CEILING_BIAS = BUILDER.comment(
                "The probability that dust drops from the ceiling instead of kicking up from the floor.",
                "0.85 means 85% of particles simulate debris dust falling from above.")
                .defineInRange("tremorCeilingBias", 0.75, 0.0, 1.0);
        TREMOR_FLOOR_KICK_FORCE = BUILDER.comment("Multiplier for the upward force applied to tremor dust that spawns on the ground.")
                .defineInRange("tremorFloorKickForce", 2.1, 0.0, 5.0);
        TREMOR_WAVEFRONT_THICKNESS = BUILDER.comment("How thick (in blocks) the expanding ring of dust is. Higher = deeper 3D effect.")
                .defineInRange("tremorWavefrontThickness", 6.0, 1.0, 16.0);
        TREMOR_MAX_GRAVITY = BUILDER.comment("Gravity applied to dust at the epicenter (makes center dust fall like heavy rocks).")
                .defineInRange("tremorMaxGravity", 40.0, 1.0, 500.0);
        TREMOR_MIN_GRAVITY = BUILDER.comment("Gravity applied to dust at the edge of the blast (makes distant dust float like mist).")
                .defineInRange("tremorMinGravity", 1.0, 0.0, 100.0);
        TREMOR_ROOF_MULTIPLIER = BUILDER.comment(
                "Multiplier for tremor gravity when dust falls from the ceiling.",
                "0.85 = 85% of the base gravity. (lower is lower gravity ex: falls slower")
                .defineInRange("tremorRoofMultiplier", 0.4, 0.0, 2.0);
        TREMOR_FLOOR_MULTIPLIER = BUILDER.comment(
                "Multiplier for tremor gravity when dust is kicked up from the floor.",
                "0.40 = 40% of the base gravity.")
                .defineInRange("tremorFloorMultiplier", 0.2, 0.0, 2.0);
        BUILDER.pop();

        BUILDER.push("Thermal updrafts");
        HEAT_BLOCKS = BUILDER.comment(
                "List of blocks that emit heat, causing dust to swirl upwards.",
                "Format: 'modid:block_name=upward_speed,reach_height,radius'")
                .defineList("heatBlocks", DEFAULT_HEAT_BLOCKS, obj -> obj instanceof String && ((String) obj).split(",").length == 3);
        BUILDER.pop();

        BUILDER.push("Handheld Lights (Dynamic Lights Compat)");
        ENABLE_HANDHELD_LIGHTS = BUILDER.comment(
                "If true, items held in your hand will act as fake light sources, allowing ambient dust to spawn and be colored by the item.",
                "Note: You still need to install a dedicated Dynamic Lights mod for actual block illumination. This only affects the dust.")
                .define("enableHandheldLights", true);
        HANDHELD_LIGHT_ITEMS = BUILDER.comment(
                "List of items/tags and their light properties.",
                "Format: 'modid:item_name=radius,#HEXCOLOR' or '#modid:tag=radius,#HEXCOLOR'.")
        .defineList("handheldLightItems", DEFAULT_HANDHELD_LIGHTS,
                obj -> obj instanceof String && ((String) obj).contains("=") && ((String) obj).contains(","));
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}