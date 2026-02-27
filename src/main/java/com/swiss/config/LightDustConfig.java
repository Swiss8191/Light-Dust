package com.lightdust.config;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.Arrays;
import java.util.List;

public class LightDustConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue AMBIENT_RADIUS;
    public static final ForgeConfigSpec.IntValue AMBIENT_HARD_CAP;
    public static final ForgeConfigSpec.IntValue AMBIENT_BLOCK_CAP;
    public static final ForgeConfigSpec.IntValue MIN_BLOCK_LIGHT;
    public static final ForgeConfigSpec.IntValue DAYTIME_LIGHT_DIFF;

    public static final ForgeConfigSpec.DoubleValue AMBIENT_DUST_OPACITY;
    public static final ForgeConfigSpec.DoubleValue PARTICLE_SIZE;
    public static final ForgeConfigSpec.IntValue PARTICLE_LIFETIME;

    public static final ForgeConfigSpec.DoubleValue TINT_STRENGTH;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CUSTOM_TINTS;

    public static final ForgeConfigSpec.DoubleValue PLAYER_INTERACT_RADIUS;
    public static final ForgeConfigSpec.IntValue BREAK_PARTICLE_COUNT;
    public static final ForgeConfigSpec.DoubleValue BREAK_PARTICLE_SPEED;

    static {

        BUILDER.push("Spawning Settings");
        
        AMBIENT_RADIUS = BUILDER.comment("Spawn Radius: How far away from the player (in blocks) dust will attempt to spawn.",
                                       "Keep this lower than Hard Cap.")
            .defineInRange("ambientRadius", 10, 1, 32);

        AMBIENT_HARD_CAP = BUILDER.comment("Hard Cap Radius: The absolute max distance dust can exist.",
                                         "Particles further than this are deleted instantly.",
                                         "MUST be larger than ambientRadius (recommended +2 or +3 blocks).")
            .defineInRange("ambientHardCapRadius", 12, 1, 48);

        AMBIENT_BLOCK_CAP = BUILDER.comment("Max dust particles allowed per single block.")
            .defineInRange("ambientBlockCap", 14, 1, 20);

        MIN_BLOCK_LIGHT = BUILDER.comment("Minimum block light level required for ambient dust to spawn.")
            .defineInRange("minBlockLight", 6, 0, 15);

        DAYTIME_LIGHT_DIFF = BUILDER.comment("Daytime Light Threshold: The minimum difference between Block Light and Sky Light required for dust to spawn during the day.",
                                             "Increase this to prevent dust spawning in sun-lit houses.",
                                             "Formula: (BlockLight - SkyLight) > Threshold")
            .defineInRange("daytimeLightDiff", 5, 0, 15);
            
        BUILDER.pop();


        BUILDER.push("Visual Settings");
        
        AMBIENT_DUST_OPACITY = BUILDER.comment("Base opacity for ambient dust (0.22 for vanilla, 0.45+ for shaders)")
            .defineInRange("ambientDustOpacity", 0.22, 0.0, 1.0);

        PARTICLE_SIZE = BUILDER.comment("Base size of the dust particles.")
            .defineInRange("particleSize", 0.022, 0.001, 0.1);

        PARTICLE_LIFETIME = BUILDER.comment("Base lifetime of the dust particles in ticks.")
            .defineInRange("particleLifetime", 200, 20, 1000);
            
        BUILDER.pop();

        BUILDER.push("Color and Tinting");
        
        TINT_STRENGTH = BUILDER.comment("How strongly colored lights (like soul fire) tint the dust. 0.0 = no tint, 1.0 = full color")
            .defineInRange("tintStrength", 0.6, 0.0, 1.0);

        CUSTOM_TINTS = BUILDER.comment(
            "List of blocks and their hex colors for dust tinting.",
            "Format: 'modid:block_name=#RRGGBB'",
            "Example 1: 'minecraft:torch=#FFDB8A'",
            "Example 2: 'mymod:custom_lantern=#00FF00'",
            "Note: The block must actually emit light for the tint to apply. Make sure to include the '#' before the hex code!"
        ).defineList("customTints",
            Arrays.asList(
                "minecraft:torch=#FFDB8A",
                "minecraft:lantern=#FFDB8A",
                "minecraft:soul_torch=#4CBAFF",
                "minecraft:soul_lantern=#4CBAFF",
                "minecraft:soul_campfire=#4CBAFF",
                "minecraft:redstone_torch=#FF4C4C",
                "minecraft:redstone_lamp=#FF4C4C",
                "minecraft:amethyst_cluster=#CC66FF",
                "minecraft:glowstone=#FFD670",
                "minecraft:shroomlight=#FF9933"
            ),
            obj -> obj instanceof String && ((String) obj).contains("=") && ((String) obj).contains("#")
        );
        
        BUILDER.pop();

        BUILDER.push("Interaction and Physics");
        
        PLAYER_INTERACT_RADIUS = BUILDER.comment("Radius squared for player interaction (slashing/moving).")
            .defineInRange("playerInteractRadius", 4.0, 0.0, 16.0);

        BREAK_PARTICLE_COUNT = BUILDER.comment("Number of dust particles spawned when a block is broken")
            .defineInRange("breakParticleCount", 12, 0, 50);

        BREAK_PARTICLE_SPEED = BUILDER.comment("How fast the dust shoots out from a broken block")
            .defineInRange("breakParticleSpeed", 0.1, 0.0, 1.0);
            
        BUILDER.pop();
        
        SPEC = BUILDER.build();
    }
}