package com.lightdust.client.gui;

import com.lightdust.client.gui.config.UniversalConfigUI;
import com.lightdust.config.LightDustColorConfig;
import com.lightdust.config.LightDustConfig;
import com.lightdust.config.LightDustExperimentalConfig;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class LightDustConfigMenu {

    public static Screen createScreen(Screen parent) {
        return new UniversalConfigUI.Builder(Component.literal("Light Dust Settings"), parent)
            .withSpec(LightDustConfig.SPEC)
            .withSpec(LightDustColorConfig.SPEC)
            .withSpec(LightDustExperimentalConfig.SPEC)

            
            .beginSection("General", "Spawning Rules", "Adjust where and how much dust spawns.")
            .addEnum("General", "Spawning Mode", "FAST: High FPS.\nFANCY: Perfect precision, lower FPS.", 0, LightDustConfig.COMMON.spawningMode, LightDustConfig.SpawningMode.class)
            .addNumber("General", "FAST Checks", "Math budget for FAST mode.\nRandom sample count.\nLower = Better FPS.", 0, LightDustConfig.COMMON.fastSpawnChecks)
            .addNumber("General", "FANCY Checks", "Math budget for FANCY mode.\nMax blocks mapped by BFS.\nNote: If too low, the scan stops before reaching your full Ambient Radius.\nLower = Better FPS.", 0, LightDustConfig.COMMON.fancySpawnChecks)
            .addNumber("General", "Ambient Radius", "How far away from the player dust will attempt to spawn.", 0, LightDustConfig.COMMON.ambientRadius)
            .addNumber("General", "Ambient Hard Cap", "The absolute max distance dust can exist before being deleted.", 0, LightDustConfig.COMMON.ambientHardCap)
            .addNumber("General", "Max Block Cap", "Max dust particles allowed per single block.", 0, LightDustConfig.COMMON.ambientBlockCap)
            .addBoolean("General", "Enable Max Y Level", "If true, ambient dust will stop spawning above the maxYLevel.", 0, LightDustConfig.COMMON.enableMaxYLevel)
            .pushDependency(LightDustConfig.COMMON.enableMaxYLevel, "Enable Max Y Level")
            .addNumber("General", "Max Y Level", "Maximum Y-level (height) where ambient dust is allowed to spawn.", 0, LightDustConfig.COMMON.maxYLevel)
            .popDependency()

            .beginSection("General", "Lighting & Performance", "Adjust lighting checks and culling.")
            .addNumber("General", "Min Block Light", "Minimum block light level required for ambient dust to spawn.", 0, LightDustConfig.COMMON.minBlockLight)
            .addNumber("General", "Daytime Light Diff", "Minimum diff between Block Light and Sky Light required for dust to spawn.", 0, LightDustConfig.COMMON.daytimeLightDiff)
            .addNumber("General", "Falloff Distance", "Distance from player where dust density starts to rapidly decrease.", 0, LightDustConfig.COMMON.falloffDistance)
            .addNumber("General", "Falloff Multiplier", "Multiplier applied to max particle cap beyond the falloff distance.", 0, LightDustConfig.COMMON.falloffMultiplier)
            .addBoolean("General", "Enable Occlusion Culling", "Uses raytracing to prevent dust from spawning behind walls you can't see.", 0, LightDustConfig.COMMON.enableOcclusionCulling)

            .beginSection("General", "Visuals", "Change the look and size of the dust.")
            .addBoolean("General", "Use HD Particles", "If true, uses HD versions of the dust textures.", 0, LightDustConfig.COMMON.useHdParticles)
            .addNumber("General", "Ambient Dust Opacity", "Base opacity for ambient dust.", 0, LightDustConfig.COMMON.ambientDustOpacity)
            .addNumber("General", "Particle Size", "Base size of the dust particles.", 0, LightDustConfig.COMMON.particleSize)
            .addNumber("General", "Particle Lifetime", "Base lifetime of the dust particles in ticks.", 0, LightDustConfig.COMMON.particleLifetime)
            .addBoolean("General", "Enable Dynamic Block Colors", "Block-breaking/landing dust inherits block color.", 0, LightDustConfig.COMMON.enableDynamicBlockColors)

            .beginSection("General", "Weather & Wind", "Configure weather-based conditions.")
            .addBoolean("General", "Enable Wind", "Globally disables all wind and weather based directional pushing.", 0, LightDustConfig.COMMON.enableWind)
            .pushDependency(LightDustConfig.COMMON.enableWind, "Enable Wind")
            .addNumber("General", "Wind Speed Clear", "Base wind speed modifier during clear weather.", 0, LightDustConfig.COMMON.windSpeedClear)
            .addNumber("General", "Wind Speed Rain", "Base wind speed modifier during rain.", 0, LightDustConfig.COMMON.windSpeedRain)
            .addNumber("General", "Wind Speed Thunder", "Base wind speed modifier during thunderstorms.", 0, LightDustConfig.COMMON.windSpeedThunder)
            .popDependency()
            .addBoolean("General", "Disable During Rain", "Outdoor dust will despawn during rain.", 0, LightDustConfig.COMMON.disableDuringRain)
            .addBoolean("General", "Disable During Thunder", "Outdoor dust will despawn during thunderstorms.", 0, LightDustConfig.COMMON.disableDuringThunder)




            .beginSection("Physics", "Player Interaction", "Player movement, swinging, and shield physics.")
            .addBoolean("Physics", "Enable Player Interaction", "Player movement, swinging, and shielding disturbs ambient dust.", 0, LightDustConfig.COMMON.enablePlayerInteraction)
            .pushDependency(LightDustConfig.COMMON.enablePlayerInteraction, "Enable Player Interaction")
            .addNumber("Physics", "Player Interact Radius", "Radius squared for player physical interaction.", 0, LightDustConfig.COMMON.playerInteractRadius)
            .popDependency()
            .addBoolean("Physics", "Enable Break Dust", "Toggles the directional burst of dust when a block is destroyed.", 0, LightDustConfig.COMMON.enableBlockBreakingDust)
            .pushDependency(LightDustConfig.COMMON.enableBlockBreakingDust, "Enable Break Dust")
            .addNumber("Physics", "Break Particle Count", "Number of dust particles spawned when a block is broken.", 0, LightDustConfig.COMMON.breakParticleCount)
            .addNumber("Physics", "Break Particle Speed", "How fast the dust shoots outward.", 0, LightDustConfig.COMMON.breakParticleSpeed)
            .addNumber("Physics", "Action Dust Gravity", "How fast block-breaking dust falls downwards.", 0, LightDustConfig.COMMON.actionDustGravity)
            .addNumber("Physics", "Action Dust Bounce", "How bouncy block breaking dust is when hitting walls.", 0, LightDustConfig.COMMON.actionDustBounce)
            .popDependency()
            .addBoolean("Physics", "Enable Break Vacuum", "Toggles the inward suction effect when a block is destroyed.", 0, LightDustConfig.COMMON.enableBreakVacuum)
            .pushDependency(LightDustConfig.COMMON.enableBreakVacuum, "Enable Break Vacuum")
            .addNumber("Physics", "Break Vacuum Radius", "How far away ambient dust is sucked toward broken block.", 0, LightDustConfig.COMMON.breakVacuumRadius)
            .addNumber("Physics", "Break Vacuum Force", "How strongly dust is pulled toward the broken block.", 0, LightDustConfig.COMMON.breakVacuumForce)
            .popDependency()

            .beginSection("Physics", "Heavy Landings", "Dust from falling.")
            .addBoolean("Physics", "Enable Landing Dust", "Toggles the ring of dust kicked up from heavy landings.", 0, LightDustConfig.COMMON.enableHeavyLandingDust)
            .pushDependency(LightDustConfig.COMMON.enableHeavyLandingDust, "Enable Landing Dust")
            .addNumber("Physics", "Max Particles", "Absolute maximum number of dust particles from a fall.", 0, LightDustConfig.COMMON.heavyLandingMaxParticles)
            .addNumber("Physics", "Particle Multiplier", "Multiplier for landing dust (Blocks fallen * Multiplier).", 0, LightDustConfig.COMMON.heavyLandingParticleMultiplier)
            .addNumber("Physics", "Upward Speed", "Base upward velocity for dust kicked up.", 0, LightDustConfig.COMMON.heavyLandingUpwardSpeed)
            .addNumber("Physics", "Outward Speed", "Base outward ring velocity for dust kicked up.", 0, LightDustConfig.COMMON.heavyLandingOutwardSpeed)
            .addNumber("Physics", "Ambient Push", "How strongly landing pushes preexisting floating dust away.", 0, LightDustConfig.COMMON.heavyLandingAmbientPush)
            .addNumber("Physics", "Ambient Radius", "Radius that ambient dust is pushed away from landing site.", 0, LightDustConfig.COMMON.heavyLandingAmbientRadius)
            .popDependency()

            .beginSection("Physics", "Tremors & Explosions", "Dust falling from ceilings.")
            .addBoolean("Physics", "Enable Cave Tremors", "Toggles ceiling dust falling from sonic booms/explosions.", 0, LightDustConfig.COMMON.enableCaveTremors)
            .pushDependency(LightDustConfig.COMMON.enableCaveTremors, "Enable Cave Tremors")
            .addNumber("Physics", "Particle Count", "Multiplier for falling dust volume.", 0, LightDustConfig.COMMON.caveTremorParticleCount)
            .addNumber("Physics", "Epicenter Multiplier", "Multiplies particle density near the blast center.", 0, LightDustConfig.COMMON.tremorEpicenterMultiplier)
            .addNumber("Physics", "Falloff Exponent", "How fast the tremor loses intensity outward.", 0, LightDustConfig.COMMON.tremorFalloffExponent)
            .addNumber("Physics", "Ceiling Bias", "Probability dust drops from ceiling instead of ground.", 0, LightDustConfig.COMMON.tremorCeilingBias)
            .addNumber("Physics", "Floor Kick Force", "Upward force applied to ground tremor dust.", 0, LightDustConfig.COMMON.tremorFloorKickForce)
            .addNumber("Physics", "Wavefront Thickness", "How thick the expanding ring of dust is.", 0, LightDustConfig.COMMON.tremorWavefrontThickness)
            .addNumber("Physics", "Max Gravity", "Gravity applied to dust at the epicenter.", 0, LightDustConfig.COMMON.tremorMaxGravity)
            .addNumber("Physics", "Min Gravity", "Gravity applied to dust at the edge of the blast.", 0, LightDustConfig.COMMON.tremorMinGravity)
            .addNumber("Physics", "Roof Multiplier", "Gravity multiplier for ceiling dust.", 0, LightDustConfig.COMMON.tremorRoofMultiplier)
            .addNumber("Physics", "Floor Multiplier", "Gravity multiplier for floor dust.", 0, LightDustConfig.COMMON.tremorFloorMultiplier)
            .popDependency()

            .beginSection("Physics", "Thermal & Handheld", "Heat updrafts and held lights.")
            .addBoolean("Physics", "Enable Thermal Updrafts", "Toggles the rising heat physics above heat blocks.", 0, LightDustConfig.COMMON.enableThermalUpdrafts)
            .addStringList("Physics", "Heat Blocks", 
                "Blocks that emit heat.\n" +
                "Format: modid:block=upward_speed,reach_height,radius\n" +
                "@SUGGEST[0]: Block | BLOCK\n" +
                "@SUGGEST[1]: Upward Speed | FLOAT\n" +
                "@SUGGEST[2]: Reach Height | FLOAT\n" +
                "@SUGGEST[3]: Radius | FLOAT\n" +
                "| Block | Upward Speed | Reach Height | Radius |\n" +
                "|---|---|---|---|\n" +
                "| BLOCK | FLOAT | FLOAT | FLOAT |", 0, LightDustConfig.COMMON.heatBlocks)
            .addBoolean("Physics", "Enable Handheld Lights", "Held items act as fake light sources for dust.", 0, LightDustConfig.COMMON.enableHandheldLights)
            .addStringList("Physics", "Handheld Lights List", 
                "Items/tags and light properties.\n" +
                "Format: item_or_tag=radius,#HEX\n" +
                "@SUGGEST[0]: Item or Tag | ANY\n" +
                "@SUGGEST[1]: Radius | INT\n" +
                "@SUGGEST[2]: Hex Color | ANY\n" +
                "| Item or Tag | Radius | Hex Color |\n" +
                "|---|---|---|\n" +
                "| ITEM / TAG | INT | #HEX |", 0, LightDustConfig.COMMON.handheldLightItems)




            .beginSection("Colors", "Tinting", "Dust coloring based on light sources and biomes.")
            .addNumber("Colors", "Tint Strength", "How strongly colored lights tint the dust.", 0, LightDustColorConfig.COLOR.tintStrength)
            .addStringList("Colors", "Custom Tints", 
                "Blocks and their hex colors for dust tinting.\n" +
                "Format: modid:block=#RRGGBB\n" +
                "@SUGGEST[0]: Block | BLOCK\n" +
                "@SUGGEST[1]: Hex Color | ANY\n" +
                "| Block | Hex Color |\n" +
                "|---|---|\n" +
                "| BLOCK | #HEX |", 0, LightDustColorConfig.COLOR.customTints)
            .addStringList("Colors", "Biome Base Colors", 
                "Biomes and their ambient dust colors.\n" +
                "Format: modid:biome=#RRGGBB\n" +
                "@SUGGEST[0]: Biome | ANY\n" +
                "@SUGGEST[1]: Hex Color | ANY\n" +
                "| Biome | Hex Color |\n" +
                "|---|---|\n" +
                "| BIOME | #HEX |", 0, LightDustColorConfig.COLOR.customBiomeTints)
            .addStringList("Colors", "Cave Biome Triggers", 
                "Blocks that trigger a specific ambient dust color underground.\n" +
                "Format: modid:block=#RRGGBB\n" +
                "@SUGGEST[0]: Block | BLOCK\n" +
                "@SUGGEST[1]: Hex Color | ANY\n" +
                "| Block | Hex Color |\n" +
                "|---|---|\n" +
                "| BLOCK | #HEX |", 0, LightDustColorConfig.COLOR.caveBiomeTriggers)




            .beginSection("Experimental", "Wind & Settling", "Performance-heavy and new mechanics.")
            .addBoolean("Experimental", "Advanced Wind Math", "Makes the wind look realistic and natural.", 0, LightDustExperimentalConfig.EXPERIMENTAL.enableAdvancedWindMath)
            .addBoolean("Experimental", "Wind Deflection", "Allows wind to blow dust down tunnels and bounce off obstacles.", 0, LightDustExperimentalConfig.EXPERIMENTAL.enableWindDeflection)
            .addBoolean("Experimental", "Cave Drafts", "Enables realistic air pressure and drafts inside caves.", 0, LightDustExperimentalConfig.EXPERIMENTAL.enableCaveDrafts)
            .pushDependency(LightDustExperimentalConfig.EXPERIMENTAL.enableCaveDrafts, "Cave Drafts")
            .addNumber("Experimental", "Cave Draft Strength", "Multiplier for the speed of wind drafts inside caves.", 0, LightDustExperimentalConfig.EXPERIMENTAL.caveDraftStrength)
            .popDependency()
            .addBoolean("Experimental", "Dust Settling", "Dust particles will visually settle and rest when hitting the ground.", 0, LightDustExperimentalConfig.EXPERIMENTAL.enableDustSettling)

            .beginSection("Experimental", "Entities", "Entity disturbances.")
            .addBoolean("Experimental", "Entity Disturbance", "Non-player entities will kick up and disturb dust.", 0, LightDustExperimentalConfig.EXPERIMENTAL.enableEntityDisturbance)
            .pushDependency(LightDustExperimentalConfig.EXPERIMENTAL.enableEntityDisturbance, "Entity Disturbance")
            .addNumber("Experimental", "Entity Push Strength", "How strongly non-player entities push dust.", 0, LightDustExperimentalConfig.EXPERIMENTAL.entityPushStrength)
            .addNumber("Experimental", "Entity Scan Rate", "How often (in ticks) the mod scans for nearby entities.", 0, LightDustExperimentalConfig.EXPERIMENTAL.entityScanRate)
            .popDependency()

            .build();
    }
}