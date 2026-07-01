package com.lightdust.config;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraftforge.common.ForgeConfigSpec;

public class LightDustExperimentalConfig {

    public static final Experimental EXPERIMENTAL;
    public static final ForgeConfigSpec SPEC;

    static {
        final Pair<Experimental, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Experimental::new);
        SPEC = specPair.getRight();
        EXPERIMENTAL = specPair.getLeft();
    }

    public static class Experimental {

        public ForgeConfigSpec.BooleanValue enableAdvancedWindMath;
        public ForgeConfigSpec.BooleanValue enableWindDeflection;
        public ForgeConfigSpec.BooleanValue enableCaveDrafts;
        public ForgeConfigSpec.DoubleValue caveDraftStrength;
        public ForgeConfigSpec.BooleanValue enableDustSettling;
        public ForgeConfigSpec.BooleanValue enableEntityDisturbance;
        public ForgeConfigSpec.DoubleValue entityPushStrength;
        public ForgeConfigSpec.IntValue entityScanRate;

        public ForgeConfigSpec.IntValue gridCellsPerTick;
        public ForgeConfigSpec.IntValue gridProbeDepth;

        public Experimental(ForgeConfigSpec.Builder builder) {
            initExperimentalFeatures(builder);
        }

        private void initExperimentalFeatures(ForgeConfigSpec.Builder builder) {
            builder.push("Experimental Features");
            
            enableAdvancedWindMath = builder
                    .comment("Makes the wind look realistic and natural.",
                            "Instead of blowing in a straight (boring) line, the wind will swirl, sweep, and create gusts of dust.",
                            "[Performance Impact: MODERATE-HIGH] - Uses heavy math to make the wind look good.")
                    .define("enableAdvancedWindMath", true);
                    
            enableWindDeflection = builder
                    .comment("Allows wind to blow dust down tunnels and bounce off obstacles.",
                            "[Performance Impact: LOW] - Safe to leave on for most computers.")
                    .define("enableWindDeflection", true);

            enableCaveDrafts = builder
                    .comment("Enables realistic air pressure and drafts inside caves.",
                            "[Performance Impact: LOW] - Uses spatial caching, but can be disabled for older computers.")
                    .define("enableCaveDrafts", true);

            caveDraftStrength = builder
                    .comment("Multiplier for the speed of wind drafts inside caves.",
                            "1.0 is gentle , 5.0 is strong wind. Set to 0 to disable.")
                    .defineInRange("caveDraftStrength", 0.85, 0.0, 20.0);
                    
            gridCellsPerTick = builder
                    .comment("How many blocks update per tick.",
                            "Higher = drafts react faster to broken blocks. Lower = better CPU performance.")
                    .defineInRange("gridCellsPerTick", 600, 10, 5000);
                    
            gridProbeDepth = builder
                    .comment("How far (in blocks) the grid searches for openings.",
                            "Higher = more accurate cave wind detection, but heavier on CPU.")
                    .defineInRange("gridProbeDepth", 6, 2, 16);
                    
            enableDustSettling = builder
                    .comment("If true, dust particles will visually settle and rest when hitting the ground.")
                    .define("enableDustSettling", true);
                    
            enableEntityDisturbance = builder
                    .comment("If true, non-player entities (mobs/projectiles) will kick up and disturb dust.",
                            "[Performance Impact: LOW/MEDIUM] - Hard capped to only track the 6 closest moving entities to save CPU.")
                    .define("enableEntityDisturbance", true);
                    
            entityPushStrength = builder
                    .comment("How strongly non-player entities push dust when moving through it.")
                    .defineInRange("entityPushStrength", 0.05, 0.0, 2.0);
                    
            entityScanRate = builder
                    .comment("How often (in ticks) the mod scans for nearby entities.",
                            "Higher numbers save CPU but make dust react slower. (4 means it updates 5 times a second)")
                    .defineInRange("entityScanRate", 4, 1, 20);
                    
            builder.pop();
        }
    }
}