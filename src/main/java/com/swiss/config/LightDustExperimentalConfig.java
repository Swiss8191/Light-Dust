package com.lightdust.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class LightDustExperimentalConfig {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static ForgeConfigSpec.BooleanValue ENABLE_ADVANCED_WIND_MATH;
    public static ForgeConfigSpec.BooleanValue ENABLE_WIND_DEFLECTION;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DUST_SETTLING;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ENTITY_DISTURBANCE;
    public static final ForgeConfigSpec.DoubleValue ENTITY_PUSH_STRENGTH;

    static {
        BUILDER.push("Experimental Features");

        ENABLE_ADVANCED_WIND_MATH = BUILDER
                .comment("Makes the wind look realistic and natural.",
                        "Instead of blowing in a straight (boring) line, the wind will swirl, sweep, and create gusts of dust.",
                        "[Performance Impact: MODERATE-HIGH] - Uses heavy math to make the wind look good. Turn this off if your game lags when surrounded by blowing dust.")
                .define("enableAdvancedWindMath", true);

        ENABLE_WIND_DEFLECTION = BUILDER
                .comment("Allows wind to blow dust down tunnels and bounce off obstacles.",
                        "If dust blows into a wall, it will slide along it or bounce away instead of just getting stuck and disappearing.",
                        "[Performance Impact: LOW] - Safe to leave on for most computers. Only turn off if you get lag specifically inside tight caves or narrow hallways.")
                .define("enableWindDeflection", true);
        
        ENABLE_DUST_SETTLING = BUILDER
                .comment("If true, dust particles will visually settle and rest when hitting the ground.",
                        "[Performance Impact: NEGLIGIBLE]. Highly recommended to keep on for visual quality.")
                .define("enableDustSettling", true);

        ENABLE_ENTITY_DISTURBANCE = BUILDER
                .comment("If true, non-player entities (mobs/projectiles) will kick up and disturb dust.",
                        "[Performance Impact: LOW/MEDIUM] - Hard capped to only track the 6 closest moving entities.",
                        "Should remain smooth even in crowded mob farms.")
                .define("enableEntityDisturbance", true); 

        ENTITY_PUSH_STRENGTH = BUILDER
                .comment("How strongly non-player entities push dust when moving through it.")
                .defineInRange("entityPushStrength", 0.05, 0.0, 2.0);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
