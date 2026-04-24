package com.lightdust.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class LightDustExperimentalConfig {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static ForgeConfigSpec.BooleanValue ENABLE_ADVANCED_WIND_MATH;
    public static ForgeConfigSpec.BooleanValue ENABLE_WIND_DEFLECTION;
    public static ForgeConfigSpec.BooleanValue ENABLE_CAVE_DRAFTS;
    public static final ForgeConfigSpec.DoubleValue CAVE_DRAFT_STRENGTH;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DUST_SETTLING;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ENTITY_DISTURBANCE;
    public static final ForgeConfigSpec.DoubleValue ENTITY_PUSH_STRENGTH;
    public static final ForgeConfigSpec.IntValue ENTITY_SCAN_RATE;

    static {
        BUILDER.push("Experimental Features");
        
        ENABLE_ADVANCED_WIND_MATH = BUILDER
                .comment("Makes the wind look realistic and natural.",
                        "Instead of blowing in a straight (boring) line, the wind will swirl, sweep, and create gusts of dust.",
                        "[Performance Impact: MODERATE-HIGH] - Uses heavy math to make the wind look good.")
                .define("enableAdvancedWindMath", true);
                
        ENABLE_WIND_DEFLECTION = BUILDER
                .comment("Allows wind to blow dust down tunnels and bounce off obstacles.",
                        "[Performance Impact: LOW] - Safe to leave on for most computers.")
                .define("enableWindDeflection", true);

        ENABLE_CAVE_DRAFTS = BUILDER
                .comment("Enables realistic air pressure and drafts inside caves.",
                        "[Performance Impact: LOW] - Uses spatial caching, but can be disabled for older computers.")
                .define("enableCaveDrafts", true);

        CAVE_DRAFT_STRENGTH = BUILDER
                .comment("Multiplier for the speed of wind drafts inside caves.",
                        "1.0 is gentle , 5.0 is strong wind. Set to 0 to disable.")
                .defineInRange("caveDraftStrength", 0.85, 0.0, 20.0);
                
        ENABLE_DUST_SETTLING = BUILDER
                .comment("If true, dust particles will visually settle and rest when hitting the ground.")
                .define("enableDustSettling", true);
                
        ENABLE_ENTITY_DISTURBANCE = BUILDER
                .comment("If true, non-player entities (mobs/projectiles) will kick up and disturb dust.",
                        "[Performance Impact: LOW/MEDIUM] - Hard capped to only track the 6 closest moving entities to save CPU.")
                .define("enableEntityDisturbance", true);
                
        ENTITY_PUSH_STRENGTH = BUILDER
                .comment("How strongly non-player entities push dust when moving through it.")
                .defineInRange("entityPushStrength", 0.05, 0.0, 2.0);
                
        ENTITY_SCAN_RATE = BUILDER
                .comment("How often (in ticks) the mod scans for nearby entities.",
                        "Higher numbers save CPU but make dust react slower. (4 means it updates 5 times a second)")
                .defineInRange("entityScanRate", 4, 1, 20);
                
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}