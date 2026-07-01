package com.lightdust.config;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraftforge.common.ForgeConfigSpec;

public class LightDustConfig {

    public static final Common COMMON;
    public static final ForgeConfigSpec SPEC;

    public enum SpawningMode {
        FAST,
        FANCY
    }

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
        final Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
        SPEC = specPair.getRight();
        COMMON = specPair.getLeft();
    }
    

    public static class Common {

        // CORE SPAWNING 
        public ForgeConfigSpec.EnumValue<SpawningMode> spawningMode;
        public ForgeConfigSpec.IntValue fastSpawnChecks;
        public ForgeConfigSpec.IntValue fancySpawnChecks;
        public ForgeConfigSpec.IntValue ambientRadius;
        public ForgeConfigSpec.IntValue ambientHardCap;
        public ForgeConfigSpec.IntValue ambientBlockCap;
        public ForgeConfigSpec.BooleanValue enableMaxYLevel;
        public ForgeConfigSpec.IntValue maxYLevel;

        // LIGHT & CULLING 
        public ForgeConfigSpec.IntValue minBlockLight;
        public ForgeConfigSpec.IntValue daytimeLightDiff;
        public ForgeConfigSpec.IntValue falloffDistance;
        public ForgeConfigSpec.DoubleValue falloffMultiplier;
        public ForgeConfigSpec.BooleanValue enableOcclusionCulling;

        // VISUAL PROPERTIES 
        public ForgeConfigSpec.BooleanValue useHdParticles;
        public ForgeConfigSpec.DoubleValue ambientDustOpacity;
        public ForgeConfigSpec.DoubleValue particleSize;
        public ForgeConfigSpec.IntValue particleLifetime;
        public ForgeConfigSpec.BooleanValue enableDynamicBlockColors;

        // WEATHER & WIND 
        public ForgeConfigSpec.BooleanValue enableWind;
        public ForgeConfigSpec.DoubleValue windSpeedClear;
        public ForgeConfigSpec.DoubleValue windSpeedRain;
        public ForgeConfigSpec.DoubleValue windSpeedThunder;
        public ForgeConfigSpec.BooleanValue disableDuringRain;
        public ForgeConfigSpec.BooleanValue disableDuringThunder;

        // BLOCK BREAKING PHYSICS 
        public ForgeConfigSpec.BooleanValue enablePlayerInteraction;
        public ForgeConfigSpec.BooleanValue enableBlockBreakingDust;
        public ForgeConfigSpec.BooleanValue enableBreakVacuum;
        public ForgeConfigSpec.DoubleValue playerInteractRadius;
        public ForgeConfigSpec.IntValue breakParticleCount;
        public ForgeConfigSpec.DoubleValue breakParticleSpeed;
        public ForgeConfigSpec.DoubleValue actionDustGravity;
        public ForgeConfigSpec.DoubleValue actionDustBounce;
        public ForgeConfigSpec.DoubleValue breakVacuumRadius;
        public ForgeConfigSpec.DoubleValue breakVacuumForce;

        // HEAVY LANDING PHYSICS 
        public ForgeConfigSpec.BooleanValue enableHeavyLandingDust;
        public ForgeConfigSpec.IntValue heavyLandingMaxParticles;
        public ForgeConfigSpec.IntValue heavyLandingParticleMultiplier;
        public ForgeConfigSpec.DoubleValue heavyLandingUpwardSpeed;
        public ForgeConfigSpec.DoubleValue heavyLandingOutwardSpeed;
        public ForgeConfigSpec.DoubleValue heavyLandingAmbientPush;
        public ForgeConfigSpec.DoubleValue heavyLandingAmbientRadius;

        // EXPLOSIONS & CAVE TREMORS 
        public ForgeConfigSpec.BooleanValue enableCaveTremors;
        public ForgeConfigSpec.IntValue caveTremorParticleCount;
        public ForgeConfigSpec.DoubleValue tremorEpicenterMultiplier;
        public ForgeConfigSpec.DoubleValue tremorFalloffExponent;
        public ForgeConfigSpec.DoubleValue tremorCeilingBias;
        public ForgeConfigSpec.DoubleValue tremorFloorKickForce;
        public ForgeConfigSpec.DoubleValue tremorWavefrontThickness;
        public ForgeConfigSpec.DoubleValue tremorMaxGravity;
        public ForgeConfigSpec.DoubleValue tremorMinGravity;
        public ForgeConfigSpec.DoubleValue tremorRoofMultiplier;
        public ForgeConfigSpec.DoubleValue tremorFloorMultiplier;

        // THERMAL DYNAMICS 
        public ForgeConfigSpec.BooleanValue enableThermalUpdrafts;
        public ForgeConfigSpec.ConfigValue<List<? extends String>> heatBlocks;

        // HANDHELD LIGHTS
        public ForgeConfigSpec.BooleanValue enableHandheldLights;
        public ForgeConfigSpec.ConfigValue<List<? extends String>> handheldLightItems;

        public Common(ForgeConfigSpec.Builder builder) {
            initSpawning(builder);
            initLighting(builder);
            initVisuals(builder);
            initWeather(builder);
            initPhysics(builder);
            initHeavyLanding(builder);
            initTremors(builder);
            initThermal(builder);
            initHandheldLights(builder);
        }

        private void initSpawning(ForgeConfigSpec.Builder builder) {
            builder.push("Spawning Rules");
            spawningMode = builder.comment(
                    "Spawning Algorithm Mode.",
                    "FANCY: Uses a flood fill search to map complex rooms (Heavier on CPU/Memory).",
                    "FAST: Uses a randomized sampling method to estimate room shape (High FPS, slightly less precise)."
            ).defineEnum("spawningMode", SpawningMode.FAST);
            fastSpawnChecks = builder.comment(
                    "FAST Mode Math Limit: How many random points are sampled.",
                    "Higher = Fills rooms faster. Lower = Better FPS.",
                    "Recommended: 120 (On values >400 I suggest using fancy))"
            ).defineInRange("fastSpawnChecks", 120, 10, 1000);
            fancySpawnChecks = builder.comment(
                    "FANCY Mode Math Limit: Maximum size of the BFS room scan.",
                    "Note: If this budget is too low, the scan will stop before reaching your full Ambient Radius.",
                    "Recommended: 1200"
            ).defineInRange("fancySpawnChecks", 1200, 100, 4000);
            ambientRadius = builder.comment("Spawn Radius: How far away from the player (in blocks) dust will attempt to spawn.")
                    .defineInRange("ambientRadius", 10, 1, 32);
            ambientHardCap = builder.comment("Hard Cap Radius: The absolute max distance dust can exist before being deleted.")
                    .defineInRange("ambientHardCapRadius", 12, 1, 48);
            ambientBlockCap = builder.comment("Max dust particles allowed per single block.")
                    .defineInRange("ambientBlockCap", 12, 1, 32);
            enableMaxYLevel = builder.comment("If true, ambient dust will stop spawning above the maxYLevel. (False recommended to keep outdoor wind dust)")
                    .define("enableMaxYLevel", false);
            maxYLevel = builder.comment("Maximum Y-level (height) where ambient dust is allowed to spawn. Set to ~60 to avoid surface houses.")
                    .defineInRange("maxYLevel", 60, -64, 320);
            builder.pop();
        }

        private void initLighting(ForgeConfigSpec.Builder builder) {
            builder.push("Lighting & Performance");
            minBlockLight = builder.comment("Minimum block light level required for ambient dust to spawn.")
                    .defineInRange("minBlockLight", 6, 0, 15);
            daytimeLightDiff = builder.comment("The minimum difference between Block Light and Sky Light required for dust to spawn during the day.")
                    .defineInRange("daytimeLightDiff", 5, 0, 15);
            falloffDistance = builder.comment("Distance from the player where dust density starts to rapidly decrease (saves performance).")
                    .defineInRange("falloffDistance", 6, 1, 32);
            falloffMultiplier = builder.comment("Multiplier applied to the max particle cap beyond the falloff distance.")
                    .defineInRange("falloffMultiplier", 0.3, 0.0, 1.0);
            enableOcclusionCulling = builder.comment("If true, uses raytracing to prevent dust from spawning behind walls you can't see. Highly recommended for performance.")
                    .define("enableOcclusionCulling", true);
            builder.pop();
        }

        private void initVisuals(ForgeConfigSpec.Builder builder) {
            builder.push("Visuals");
            useHdParticles = builder.comment("If true, the mod will use the HD versions of the dust textures. No performance impact.")
                    .define("useHdParticles", true);
            ambientDustOpacity = builder.comment("Base opacity for ambient dust. (0.22 looks good in vanilla, 0.45+ is better for shader) for HD dust lower these values slightly")
                    .defineInRange("ambientDustOpacity", 0.18, 0.0, 1.0);
            particleSize = builder.comment("Base size of the dust particles.")
                    .defineInRange("particleSize", 0.016, 0.001, 0.1);
            particleLifetime = builder.comment("Base lifetime of the dust particles in ticks. (20 ticks = 1 second)")
                    .defineInRange("particleLifetime", 200, 20, 1000);
            enableDynamicBlockColors = builder.comment(
                    "If true, block-breaking dust and heavy landing dust will inherit the exact color of the block being interacted with.",
                    "Provides a massive visual upgrade for physics interactions.")
                    .define("enableDynamicBlockColors", true);
            builder.pop();
        }

        private void initWeather(ForgeConfigSpec.Builder builder) {
            builder.push("Weather & Wind");
            enableWind = builder.comment("Globally disables all wind and weather based directional pushing.")
                    .define("enableWind", true);
            windSpeedClear = builder.comment("Base wind speed modifier during clear weather.")
                    .defineInRange("windSpeedClear", 0.25, 0.0, 1.0);
            windSpeedRain = builder.comment("Base wind speed modifier during rain.")
                    .defineInRange("windSpeedRain", 0.35, 0.0, 1.0);
            windSpeedThunder = builder.comment("Base wind speed modifier during thunderstorms.")
                    .defineInRange("windSpeedThunder", 0.5, 0.0, 1.0);
            disableDuringRain = builder.comment("If true, outdoor dust will despawn and stop spawning during rain.")
                    .define("disableDuringRain", false);
            disableDuringThunder = builder.comment("If true, outdoor dust will despawn and stop spawning during thunderstorms.")
                    .define("disableDuringThunder", false);
            builder.pop();
        }

        private void initPhysics(ForgeConfigSpec.Builder builder) {
            builder.push("Player & Block Breaking Physics");
            enablePlayerInteraction = builder.comment("Toggles whether player movement, weapon swinging, and shielding disturbs ambient dust.")
                    .define("enablePlayerInteraction", true);
            enableBlockBreakingDust = builder.comment("Toggles the directional burst of dust particles when a block is destroyed.")
                    .define("enableBlockBreakingDust", true);
            enableBreakVacuum = builder.comment("Toggles the inward suction effect on ambient dust when a block is destroyed.")
                    .define("enableBreakVacuum", true);
            playerInteractRadius = builder.comment("Radius squared for player physical interaction (slashing swords, moving quickly).")
                    .defineInRange("playerInteractRadius", 4.0, 0.0, 16.0);
            breakParticleCount = builder.comment("Number of dust particles spawned when a block is broken.")
                    .defineInRange("breakParticleCount", 12, 0, 50);
            breakParticleSpeed = builder.comment("How fast the dust shoots outward from the broken block face.")
                    .defineInRange("breakParticleSpeed", 0.1, 0.0, 1.0);
            actionDustGravity = builder.comment("How fast block-breaking dust falls downwards over time.")
                    .defineInRange("actionDustGravity", 2.0, 0.0, 50.0);
            actionDustBounce = builder.comment("How bouncy block breaking dust is when hitting walls/floors. (0.0 = no bounce, 1.0 = super bouncy)")
                    .defineInRange("actionDustBounce", 0.2, 0.0, 1.0);
            breakVacuumRadius = builder.comment(
                    "When a block turns to air, it creates a momentary vacuum.",
                    "This defines how far away (in blocks) ambient dust will be sucked toward the broken block.")
                    .defineInRange("breakVacuumRadius", 1.2, 0.0, 16.0);
            breakVacuumForce = builder.comment("How strong the dust is pulled toward the broken block.")
                    .defineInRange("breakVacuumForce", 10.0, 0.0, 1000.0);
            builder.pop();
        }

        private void initHeavyLanding(ForgeConfigSpec.Builder builder) {
            builder.push("Heavy Landing Physics");
            enableHeavyLandingDust = builder.comment("Toggles the ring of dust kicked up from heavy landings.")
                    .define("enableHeavyLandingDust", true);
            heavyLandingMaxParticles = builder.comment("The absolute maximum number of dust particles that can spawn from a hard fall.")
                    .defineInRange("heavyLandingMaxParticles", 96, 0, 300);
            heavyLandingParticleMultiplier = builder.comment("Multiplier for landing dust. (Blocks fallen * Multiplier = Particles spawned).")
                    .defineInRange("heavyLandingParticleMultiplier", 12, 0, 50);
            heavyLandingUpwardSpeed = builder.comment("Base upward velocity for dust kicked up by a heavy landing.")
                    .defineInRange("heavyLandingUpwardSpeed", 0.2, 0.0, 2.0);
            heavyLandingOutwardSpeed = builder.comment("Base outward ring velocity for dust kicked up by a heavy landing.")
                    .defineInRange("heavyLandingOutwardSpeed", 0.12, 0.0, 2.0);
            heavyLandingAmbientPush = builder.comment("How strongly a heavy landing violently pushes preexisting floating ambient dust away.")
                    .defineInRange("heavyLandingAmbientPush", 1.0, 0.0, 100.0);
            heavyLandingAmbientRadius = builder.comment("The radius (in blocks) that ambient dust is pushed away from the landing site.")
                    .defineInRange("heavyLandingAmbientRadius", 4.0, 1.0, 16.0);
            builder.pop();
        }

        private void initTremors(ForgeConfigSpec.Builder builder) {
            builder.push("Explosions & Cave Tremors");
            enableCaveTremors = builder.comment("Toggles the ceiling dust falling from Warden sonic booms and explosions.")
                    .define("enableCaveTremors", true);
            caveTremorParticleCount = builder.comment(
                    "Controls the volume of falling dust during explosions or Warden sonic booms.",
                    "Note: This is a multiplier against the expanding wave circumference, not a hard particle cap.")
                    .defineInRange("caveTremorParticleCount", 64, 0, 1024);
            tremorEpicenterMultiplier = builder.comment(
                    "Multiplies particle density at the epicenter (within 10% of the max radius).")
                    .defineInRange("tremorEpicenterMultiplier", 5.0, 1.0, 20.0);
            tremorFalloffExponent = builder.comment(
                    "Exponential Falloff. Controls how fast the tremor loses intensity as it travels outward.",
                    "A value of 6.0 means the center is incredibly violent, but it rapidly drops to a gentle dusting further away.")
                    .defineInRange("tremorFalloffExponent", 6.0, 1.0, 10.0);
            tremorCeilingBias = builder.comment(
                    "The probability that dust drops from the ceiling instead of kicking up from the floor.",
                    "0.85 means 85% of particles simulate debris dust falling from above.")
                    .defineInRange("tremorCeilingBias", 0.75, 0.0, 1.0);
            tremorFloorKickForce = builder.comment("Multiplier for the upward force applied to tremor dust that spawns on the ground.")
                    .defineInRange("tremorFloorKickForce", 2.1, 0.0, 5.0);
            tremorWavefrontThickness = builder.comment("How thick (in blocks) the expanding ring of dust is. Higher = deeper 3D effect.")
                    .defineInRange("tremorWavefrontThickness", 6.0, 1.0, 16.0);
            tremorMaxGravity = builder.comment("Gravity applied to dust at the epicenter (makes center dust fall like heavy rocks).")
                    .defineInRange("tremorMaxGravity", 40.0, 1.0, 500.0);
            tremorMinGravity = builder.comment("Gravity applied to dust at the edge of the blast (makes distant dust float like mist).")
                    .defineInRange("tremorMinGravity", 1.0, 0.0, 100.0);
            tremorRoofMultiplier = builder.comment(
                    "Multiplier for tremor gravity when dust falls from the ceiling.",
                    "0.85 = 85% of the base gravity. (lower is lower gravity ex: falls slower")
                    .defineInRange("tremorRoofMultiplier", 0.4, 0.0, 2.0);
            tremorFloorMultiplier = builder.comment(
                    "Multiplier for tremor gravity when dust is kicked up from the floor.",
                    "0.40 = 40% of the base gravity.")
                    .defineInRange("tremorFloorMultiplier", 0.2, 0.0, 2.0);
            builder.pop();
        }

        private void initThermal(ForgeConfigSpec.Builder builder) {
            builder.push("Thermal updrafts");
            enableThermalUpdrafts = builder.comment("Toggles the rising heat physics and updrafts above heat blocks.")
                    .define("enableThermalUpdrafts", true);
            heatBlocks = builder.comment(
                    "List of blocks that emit heat, causing dust to swirl upwards.",
                    "Format: 'modid:block_name=upward_speed,reach_height,radius'")
                    .defineList("heatBlocks", DEFAULT_HEAT_BLOCKS, obj -> obj instanceof String && ((String) obj).split(",").length == 3);
            builder.pop();
        }

        private void initHandheldLights(ForgeConfigSpec.Builder builder) {
            builder.push("Handheld Lights (Dynamic Lights Compat)");
            enableHandheldLights = builder.comment(
                    "If true, items held in your hand will act as fake light sources, allowing ambient dust to spawn and be colored by the item.",
                    "Note: You still need to install a dedicated Dynamic Lights mod for actual block illumination. This only affects the dust.")
                    .define("enableHandheldLights", false);
            handheldLightItems = builder.comment(
                    "List of items/tags and their light properties.",
                    "Format: 'modid:item_name=radius,#HEXCOLOR' or '#modid:tag=radius,#HEXCOLOR'.")
            .defineList("handheldLightItems", DEFAULT_HANDHELD_LIGHTS,
                    obj -> obj instanceof String && ((String) obj).contains("=") && ((String) obj).contains(","));
            builder.pop();
        }
    }
}