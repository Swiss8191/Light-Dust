# Light Dust

Light Dust is a lightweight client side mod that adds floating dust to your world. Built with interactive physics, dust reacts to light, weather, and the exact biome you are exploring.

As of v1.9.5+ this mod has an in game config menu (no dependencies) which you can find  by doing: menu -> mods -> lightdust -> config

## Features

- You can see some of them by looking at the config options below! But there really is a lot to list so I suggest looking at in via in game config menu.

### Spawning & Performance
-   `spawnMode` (Default: `FANCY`): Choose between FANCY and FAST for spawning dust.<br>
-   `fastSpawnChecks` (Default: `120`): Math budget for FAST mode. Random sample count. Lower = Better FPS. <br>
-   `fancySpawnChecks` (Default: `1200`): Math budget for FANCY mode. Max blocks mapped by BFS. Restricts radius if too low. Note: If you set `fastSpawnChecks` to any value >400, I would suggest swapping to fancy.<br>
-   `enableMaxYLevel` (Default: `false`): Toggle for the height limit.<br>
-   `maxYLevel` (Default: `60`): The height coordinate where dust stops spawning.<br>
-   `ambientRadius` (Default: `10`): How far away from the player dust will attempt to spawn.<br>
-   `ambientHardCapRadius` (Default: `12`): The absolute maximum distance dust can exist. Particles further than this are deleted instantly.<br>
-   `ambientBlockCap` (Default: `14`): The maximum density of dust allowed in a single block.<br>
-   `minBlockLight` (Default: `6`): The minimum block light level required for ambient dust to spawn.<br>
-   `daytimeLightDiff` (Default: `5`): The minimum difference between Block Light and Sky Light required for dust to spawn during the day.<br><br>
-   `falloffDistance` (Default: `6`): Distance from the player where dust density starts to decrease.<br>
-   `falloffMultiplier` (Default: `0.3`): Multiplier applied to the max particle cap beyond the falloff distance.<br><br>
-   `enableOcclusionCulling` (Default: `true`): Uses LOS raytracing to prevent dust from spawning behind solid walls.<br><br>

### Visuals & Environment
-   `useHdParticles` (Default: `true`): Toggle for the HD dust particles or use the legacy version. Note that if you use this you should lower the values for particle size and opacity slightly. (I suggest 0.016 and 0.18 respectively) <br>
-   `enableDynamicBlockColors` (Default: `true`): Toggle for the block color inheritance system.<br>
-   `ambientDustOpacity` (Default: `0.18`): Base opacity for ambient dust.<br>
-   `particleSize` (Default: `0.016`): Adjust the visual size of the ambient dust particles.<br>
-   `particleLifetime` (Default: `200`): Control how long (in ticks) dust particles stick around.<br><br>
-   `enableWind`: Globally disables all wind and weather based directional pushing.<br>
-   `windSpeedClear` (Default: `0.25`): Adjusts global wind drift during clear weather.<br>
-   `windSpeedRain` (Default: `0.35`): Adjusts global wind drift during rain.<br>
-   `windSpeedThunder` (Default: `0.5`): Adjusts global wind drift during thunder.<br>
-   `disableDuringRain` (Default: `false`): Toggles to prevent outdoor dust from spawning or existing during rain.<br>
-   `disableDuringThunder` (Default: `false`): Toggles to prevent outdoor dust from spawning or existing during thunder.<br><br>

### Interactions & Actions
-   `enableBlockBreakingDust`: Toggles the directional burst of dust particles when a block is destroyed.<br>
-   `breakParticleCount` (Default: `12`): Controls the volume of the block-breaking debris.<br>
-   `breakParticleSpeed` (Default: `0.1`): Controls the speed of the block-breaking debris.<br>
-   `actionDustGravity` (Default: `2.0`): Controls the fall speed of debris hitting surfaces.<br>
-   `actionDustBounce` (Default: `0.2`): Controls the bounciness of debris hitting surfaces.<br><br>
-   `breakVacuumRadius` (Default: `1.2`): The radius in blocks that ambient dust is pulled toward a broken block.<br>
-   `enableBreakVacuum` (Default: true): Toggle for the vacuum effect when a block breaks.<br>
-   `breakVacuumForce` (Default: `10.0`): How strong the dust is pulled toward the broken block.<br><br>
-   `enableCaveTremors`: Toggles the ceiling dust falling from Warden sonic booms and explosions.<br>
-   `caveTremorParticleCount` (Default: `90`): The base density of falling debris during explosions.<br>
-   `tremorEpicenterMultiplier` (Default: `5.0`): Controls intensity at the blast center.<br>
-   `tremorFalloffExponent` (Default: `6.0`): Controls how quickly intensity fades from the blast center.<br>
-   `tremorCeilingBias` (Default: `0.75`): Adjusts the ratio of ceiling-to-floor spawns.<br>
-   `tremorFloorKickForce` (Default: `2.1`): Adjusts the upward force for floor dust.<br>
-   `tremorWavefrontThickness` (Default: `6.0`): Controls the depth of the expanding shockwave zone.<br>
-   `tremorMaxGravity` (Default: `40.0`): Adjusts the weight of debris at the center.<br>
-   `tremorMinGravity` (Default: `1.0`): Adjusts the weight of debris at the outer edge.<br>
-   `tremorRoofMultiplier` (Default: `0.4`): Multiplier for the gravity applied to dust falling from the ceiling.<br>
-   `tremorFloorMultiplier` (Default: `0.2`): Multiplier for the gravity applied to dust kicked up from the floor.<br><br>
-   `enablePlayerInteraction`: Toggles whether player movement, weapon swinging, and shielding disturbs ambient dust.<br>
-   `playerInteractRadius` (Default: `4.0`): The radius at which player movements physically push dust particles.<br><br>
-   `enableHeavyLandingDust`: Toggles the ring of dust kicked up from heavy landings.<br>
-   `heavyLandingMaxParticles` (Default: `96`): Controls how many particles spawn when taking fall damage.<br>
-   `heavyLandingParticleMultiplier` (Default: `12`): Controls the multiplier for particles spawned when taking fall damage.<br>
-   `heavyLandingUpwardSpeed` (Default: `0.2`): Controls the speed of the heavy landing dust ring upward.<br>
-   `heavyLandingOutwardSpeed` (Default: `0.12`): Controls the shape of the heavy landing dust ring outward.<br>
-   `heavyLandingAmbientPush` (Default: `1.0`): Controls the shockwave force that pushes ambient dust away when you land.<br>
-   `heavyLandingAmbientRadius` (Default: `4.0`): Controls the shockwave radius that pushes ambient dust away when you land.<br><br>
-   `enableThermalUpdrafts`: Toggles the rising heat physics and updrafts above heat blocks.<br>
-   `heatBlocks` (Default: `DEFAULT_HEAT_BLOCKS`): List of blocks that emit heat for thermal updrafts.<br><br>

### Dynamic Lights Compat

-   `enableHandheldLights` (Default: `false`): If true, items held in your hand will act as fake light sources, allowing dust to spawn and coloring the dust.<br>
-   `handheldLightItems` (Default: `DEFAULT_HANDHELD_LIGHTS`): List of items/tags and their light properties. Format: `'modid:item_name=radius,#HEXCOLOR'` or `'#modid:tag=radius,#HEXCOLOR'`.<br>

### Experimental Features (`experimental.toml`)
-   `enableCaveDrafts` (Default: `true`): Enables air pressure and drafts inside caves.<br>
-   `caveDraftStrength` (Default: `0.85`): Multiplier for the speed of wind drafts inside caves.<br>
-   `enableEntityDisturbance` (Default: `true`): Allows non-player entities (mobs/projectiles) to kick up and disturb dust. This is hard capped to only track the 6 closest moving entities.<br>
-   `entityScanRate` (Default: `4`): How often in ticks the mod scans for nearby entities.<br>
-   `entityPushStrength` (Default: `0.05`): How strongly entities push dust when moving through it.<br>
-   `enableAdvancedWindMath` (Default: `true`): Enable complex spatial turbulence and sweeping wind physics.<br>
-   `enableWindDeflection` (Default: `true`): Allows wind to realistically blow dust down tunnels.<br>
-   `enableDustSettling` (Default: `true`): Allows dust particles to visually settle when hitting the ground.<br><br>

### Color & Tinting Settings (`colors.toml`)

-   `tintStrength` (Default: `0.75`): Adjusts how strongly colored lights tint the dust. (suggest higher values if using Hd dust ex: 0.75).<br>
-   `customTints`: A list of light-emitting blocks and their hex colors for dust tinting.<br>
-   `customBiomeTints`: Define base ambient colors for biomes using hex codes.<br>
-   `caveBiomeTriggers`: Map specific blocks to a hex color for underground environment changes.<br><br>

----------

## Q&A

**Q: Does this work with true dark mods?**

**A:** Yes. It should work with them. However, you may need to change the opacity of the dust.

**Q: Does this work with Shaders?**

**A:** Yes. However, shaders often change how transparency renders. If the dust looks too invisible, increase `ambientDustOpacity` in the config (try `0.45` or higher).

**Q: Will this cause lag in big modpacks?**

**A:** It is designed specifically to be lightweight. The mod has a ton of optimization options such as the newly added LOS and Hemisphere system aswell as Occlusion Culling to skip particle rendering behind solid walls. If you still drop frames, you can disable `enableAdvancedWindMath` or lower the particle caps in the configs.

**Q: Can I change the color of the dust?**

**A:** Yes! You can change the tint of light sources (`customTints`), the base color of any biome (`customBiomeTints`), and trigger colors underground using specific blocks (`caveBiomeTriggers`) in the `colors.toml` config.

**Q: Does this work with modded biomes and caves?**

**A:** Yep. Just add the modded biome ID or the modded blocks to the respective lists in the config, assign a hex color, and it should work just fine.

**Q: How do I disable the Block Break particles?**

**A:** Set `breakParticleCount` to `0` in the config.-   `breakVacuumRadius` (Default: `1.2`): The radius in blocks that ambient dust is pulled toward a broken block.<br>
