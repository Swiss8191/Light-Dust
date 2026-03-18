# Light Dust

Light Dust is a lightweight client-side mod that adds floating dust to your world. Built with interactive physics, dust reacts to light, weather, and the exact biome you are exploring.

## Features

*   **Biome Based Dust:** Dust naturally tints and changes behavior based on the biome you are in.<br><br>
*   **Dynamic Dust Tints:** Ambient dust gets tinted by the color of nearby light sources. Fast-moving action dust from breaking blocks or heavy landings also dynamically inherits these local colors.<br><br>
*   **Weather Wind & Global Drafts:** Dust actively drifts in a unified direction. The speed and intensity of this wind dynamically scale based on altitude and current weather conditions (Clear, Rain, or Thunder).<br><br>
*   **Action Dust Physics:** Block-breaking dust explodes outward in a full 360-degree sphere, naturally ricochets off walls and ceilings, and gracefully drifts to the floor.<br><br>
*   **Dynamic Dust Settling:** Airborne dust that lands on a solid surface will stop moving and permanently rest on the floor, allowing dust to visually "accumulate" in undisturbed areas over time.<br><br>
*   **Physics Interactions:** Dust reacts to movement, combat, and items. Swinging a sword, sprinting, or raising a shield pushes particles away within a configurable interaction radius.<br><br>
*   **Dust Glinting:** Dust particles subtly glint when you get close to them. In dark environments, dust near light sources softly illuminates and fades as it drifts away.<br><br>
*   **Thermal Updrafts & Shockwaves:** Particles catch thermal updrafts to swirl upwards over heat sources, and get pushed away by shockwaves from loud noises (like explosions or a Warden's roar).<br><br>
*   **Heavy Landings:** Falling 3 or more blocks kicks up a scaling ring of dust upon impact, pushing preexisting ambient dust away from the landing zone. <br><br>
*   **Configurable:** Almost everything is adjustable, with settings for performance, physics, and visuals.
    

***

## Configuration

All values are adjustable in the `config/lightdust/` folder (split into `main.toml` and `colors.toml`). The `main.toml` is divided into categories:

### Spawning & Performance

*   `ambientRadius` (Default: `10`): How far away from the player (in blocks) dust will attempt to spawn. Keep this lower than the Hard Cap.
    
*   `ambientHardCapRadius` (Default: `12`): The absolute maximum distance dust can exist. Particles further than this are deleted instantly to prevent buildup.
    
*   `ambientBlockCap` (Default: `14`): The maximum density of dust allowed in a single block.
    
*   `minBlockLight` (Default: `6`): The minimum block light level required for ambient dust to spawn.
    
*   `daytimeLightDiff` (Default: `5`): The minimum difference between Block Light and Sky Light required for dust to spawn during the day.
    
*   `falloffDistance` (Default: `6`): Distance from the player where dust density starts to decrease.
    
*   `falloffMultiplier` (Default: `0.3`): Multiplier applied to the max particle cap beyond the falloff distance.
    
*   `enableOcclusionCulling` (Default: `true`): Uses LOS raytracing to prevent dust from spawning behind solid walls or in unseen caves. Highly recommended for performance.
    

### Visuals & Environment

*   `ambientDustOpacity` (Default: `0.22`): Base opacity for ambient dust. (Recommended: `0.22` for Vanilla, `0.45+` if using Shaders).
    
*   `particleSize` (Default: `0.022`): Adjust the visual size of the ambient dust particles.
    
*   `particleLifetime` (Default: `200`): Control exactly how long (in ticks) dust particles stick around before fading away.
    
*   `windSpeedClear` / `windSpeedRain` / `windSpeedThunder`: Adjusts the speed of the global wind drifts based on current weather.
    
*   `disableDuringRain` / `disableDuringThunder`: Toggles to prevent outdoor dust from spawning or existing during specific weather events.
    

### Interactions & Actions

*   `playerInteractRadius` (Default: `4.0`): The radius at which player movements physically interact with and push dust particles.
    
*   `breakParticleCount` / `breakParticleSpeed`: Controls the amount and burst speed of dust when breaking blocks.
    
*   `actionDustGravity` (Default: `0.002`): How fast block breaking dust falls downwards over time.
    
*   `actionDustBounce` (Default: `0.2`): How bouncy block breaking dust is when ricocheting off walls or ceilings.
    
*   `heavyLandingMaxParticles` / `heavyLandingParticleMultiplier`: Controls how many particles spawn when taking fall damage.
    
*   `heavyLandingUpwardSpeed` / `heavyLandingOutwardSpeed`: Controls the speed and shape of the heavy landing dust ring.
    
*   `heavyLandingAmbientPush` / `heavyLandingAmbientRadius`: Controls the shockwave that pushes ambient dust away when you land.
    
*   `heatBlocks`: List of blocks that emit heat, causing dust particles to swirl upwards in a thermal updraft. Format: `modid:block_name=speed,vertical_reach,horizontal_radius`.
    

### Experimental Features

*   `enableDustSettling` (Default: `true`): Allows dust particles to visually settle and rest when hitting the ground.
    
*   `enableEntityDisturbance` (Default: `false`): Allows non player entities (mobs/projectiles) to kick up and disturb dust. (Can be performance-heavy in mob farms).
    
*   `entityPushStrength` (Default: `0.05`): How strongly non player entities push dust when moving through it.
    

### Color & Tinting Settings (`colors.toml`)

*   `tintStrength` (Default: `0.6`): Adjusts how strongly colored lights (like soul fire) tint the dust. `0.0` = no tint, `1.0` = full color.
    
*   `customTints`: A list of blocks and their hex colors for dust tinting. Format: `modid:block_name=#RRGGBB`. Note: The block must actually emit light for the tint to apply.
    
*   `customBiomeTints`: Define base ambient colors for any vanilla or modded biome using hex codes.
    
*   `caveBiomeTriggers`: Map specific blocks (like modded ores or plants) to a hex color. The mod will automatically change the cave's dust to that color when enough of those blocks are nearby underground.
    

***

## Q&A

**Q: Does this work with true dark mods?**

**A:** Yes. It should work with them. However, you may need to change the opacity of the dust.

**Q: Does this work with Shaders?**

**A:** Yes. However, shaders often change how transparency renders. If the dust looks too invisible, increase `ambientDustOpacity` in the config (try `0.45` or higher).

**Q: Will this cause lag in big modpacks?**

**A:** It is designed specifically to be production-ready and lightweight. The mod utilizes a distance falloff (LOD) system, a thread safe `ConcurrentHashMap` for multi threading compatibility (so it should work with ModernFix/ImmediatelyFast), and Occlusion Culling to skip particle rendering behind solid walls. If you still drop frames, you can lower the particle caps or disable `enableEntityDisturbance` in the configs.

**Q: Can I change the color of the dust?**

**A:** Yes! You can change the tint of light sources (`customTints`), the base color of any biome (`customBiomeTints`), and trigger colors underground using specific blocks (`caveBiomeTriggers`) in the `colors.toml` config.

**Q: Does this work with modded biomes and caves?**

**A:** Yep. Just add the modded biome ID or the modded blocks to the respective lists in the config, assign a hex color, and it should work just fine.

**Q: How do I disable the Block Break particles?**

**A:** Set `breakParticleCount` to `0` in the config.
