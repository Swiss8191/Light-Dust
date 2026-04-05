
# Light Dust

Light Dust is a lightweight client side mod that adds floating dust to your world. Built with interactive physics, dust reacts to light, weather, and the exact biome you are exploring.

if you want a more in depth guide as to how to configure this mod check out the new [wiki!](https://github.com/Swiss8191/Light-Dust/wiki).

## Features

*   **Biome specific dust:** Dust tints and changes behavior based on the specific biome you are exploring.<br><br>
*   **Light dust tinting:** Dust gets tinted based off the light sources that are near it, dust near fire are tinted slightly orange and dust near glow stone are tinted a bright pale yellow (works with modded as long as you add it to the list and add a color).<br><br>
*   **Advanced Wind:** A fluid dynamics simulation that causes dust to swarm, sweep, and flow. It utilizes a sky light gradient to allow drafts to work under trees, overhangs, and in tunnels.<br><br>  
*   **Thermal Updrafts:** Particles above heat sources like fire, campfires, and lava, cause them to swirl upward.<br><br>  
*   **Directional Block Breaking:** Debris shoots out from the specific face of a block you hit. This debris can bounce or slide off nearby walls. Breaking a block also creates a vacuum that pulls nearby ambient dust toward the empty space.<br><br>  
*   **Cave Tremors:** Explosions and other loud sounds trigger a shockwave that drops debris from the ceiling. The effect is most intense at the epicenter and fades as it travels outward.<br><br>  
*   **Combat & Movement:** Dust reacts to sprinting, sword swings, and shield raising by being pushed away within a configurable interaction radius.<br><br>  
*   **Heavy Landings:** Falling three or more blocks kicks up a scaling ring of dust upon impact, displacing any existing ambient particles in the landing zone.<br><br>  
*   **Dynamic Block Colors:** Debris, landing dust, and tremor particles use the map color of the block you are interacting wit so dirt will have dirt tinted debris dust and stone will have grey tinted debris dust.<br><br>  
*   **Dynamic Lights Compatibility:** handheld lights (torches, lanterns, etc.) spawn a local area of dust around the player. This is compatible with Curios and Accessories slots.<br><br>  
*   **Dust Glinting:** Particles subtly glint when approached. In dark areas, dust near light sources softly illuminates before fading as it drifts away.<br><br>  
*   **Configurable:** configs are split across 3 files (`main.toml`, `colors.toml`, `experimental.toml`).

***

## Configuration

All values are adjustable in the `config/lightdust/` folder (split into `main.toml`, `colors.toml`, and `experimental.toml`).

### Spawning & Performance

*   `enableMaxYLevel` (Default: `false`): Toggle for the height limit.<br>
*   `maxYLevel` (Default: `60`): The height coordinate where dust stops spawning.<br>  
*   `ambientRadius` (Default: `10`): How far away from the player dust will attempt to spawn.<br>  
*   `ambientHardCapRadius` (Default: `12`): The absolute maximum distance dust can exist. Particles further than this are deleted instantly.<br>  
*   `ambientBlockCap` (Default: `14`): The maximum density of dust allowed in a single block.<br>  
*   `minBlockLight` (Default: `6`): The minimum block light level required for ambient dust to spawn.<br>  
*   `daytimeLightDiff` (Default: `5`): The minimum difference between Block Light and Sky Light required for dust to spawn during the day.<br>
*   `falloffDistance` (Default: `6`): Distance from the player where dust density starts to decrease.<br>
*   `falloffMultiplier` (Default: `0.3`): Multiplier applied to the max particle cap beyond the falloff distance.<br>
*   `enableOcclusionCulling` (Default: `true`): Uses LOS raytracing to prevent dust from spawning behind solid walls.<br>

### Visuals & Environment
*   `enableDynamicBlockColors` (Default: `true`): Toggle for the block color inheritance system.<br>  
*   `ambientDustOpacity` (Default: `0.22`): Base opacity for ambient dust.<br>  
*   `particleSize` (Default: `0.022`): Adjust the visual size of the ambient dust particles.<br>  
*   `particleLifetime` (Default: `200`): Control how long (in ticks) dust particles stick around.<br>  
*   `windSpeedClear` (Default: `0.25`) / `windSpeedRain` (Default: `0.35`) / `windSpeedThunder` (Default: `0.5`): Adjusts global wind drifts based on weather.<br>
*   `disableDuringRain` (Default: `false`) / `disableDuringThunder` (Default: `false`): Toggles to prevent outdoor dust from spawning or existing during specific weather events.<br>

### Interactions & Actions
*   `breakParticleCount`(Default: `12`): / `breakParticleSpeed`(Default: `0.1`): Controls the volume and speed of the block-breaking debris.<br>  
*   `actionDustGravity`(Default: `0.002`): / `actionDustBounce`(Default: `0.2`): Controls the fall speed and bounciness of debris hitting surfaces.<br>  
*   `breakVacuumRadius` (Default: `1.2`): The radius in blocks that ambient dust is pulled toward a broken block.<br>  
*   `breakVacuumForce` (Default: `0.01`): How strong the dust is pulled toward the broken block.<br>
*   `caveTremorParticleCount`(Default: `90`): The base density of falling debris during explosions.<br>  
*   `tremorEpicenterMultiplier`(Default: `5.0`) / `tremorFalloffExponent`(Default: `6.0`): Controls intensity at the blast center and how quickly it fades.<br>  
*   `tremorCeilingBias`(Default: `0.75`) / `tremorFloorKickForce`(Default: `2.1`): Adjusts the ratio of ceiling-to-floor spawns and the upward force for floor dust.<br>  
*   `tremorWavefrontThickness`(Default: `6.0`): Controls the depth of the expanding shockwave zone.<br>  
*   `tremorMaxGravity`(Default: `0.04`) / `tremorMinGravity`(Default: `0.001`): Adjusts the weight of debris at the center versus the outer edge.<br>
*   `playerInteractRadius` (Default: `4.0`): The radius at which player movements physically push dust particles.<br>  
*   `heavyLandingMaxParticles`(Default: `96`)/ `heavyLandingParticleMultiplier`(Default: `12`): Controls how many particles spawn when taking fall damage.<br>
*   `heavyLandingUpwardSpeed` (Default: `0.2`) / `heavyLandingOutwardSpeed` (Default: `0.12`) Controls the speed and shape of the heavy landing dust ring.<br>
*   `heavyLandingAmbientPush` (Default: `0.001`) / `heavyLandingAmbientRadius` (Default: `4.0`): Controls the shockwave that pushes ambient dust away when you land.<br>
*   `heatBlocks`(Default: `DEFAULT_HEAT_BLOCKS`): List of blocks that emit heat for thermal updrafts.<br>
  
### Dynamic Lights Compat
*   `enableHandheldLights` (Default: `false`): If true, items held in your hand will act as fake light sources, allowing dust to spawn and coloring the dust.<br>
*   `handheldLightItems`(Default: `DEFAULT_HANDHELD_LIGHTS`): List of items/tags and their light properties. Format: `'modid:item_name=radius,#HEXCOLOR'` or `'#modid:tag=radius,#HEXCOLOR'`.<br>
  
### Experimental Features (`experimental.toml`)
*   `enableEntityDisturbance` (Default: `true`): Allows non-player entities (mobs/projectiles) to kick up and disturb dust. This is hard capped to only track the 6 closest moving entities.<br>  
*   `entityScanRate` (Default: `4`): How often in ticks the mod scans for nearby entities.<br>  
*   `entityPushStrength` (Default: `0.05`): How strongly entities push dust when moving through it.<br>  
*   `enableAdvancedWindMath` (Default: `true`): Enable complex spatial turbulence and sweeping wind physics.<br>  
*   `enableWindDeflection` (Default: `true`): Allows wind to realistically blow dust down tunnels.<br>  
*   `enableDustSettling` (Default: `true`): Allows dust particles to visually settle when hitting the ground.<br>

### Color & Tinting Settings (`colors.toml`)
*   `tintStrength` (Default: `0.45`): Adjusts how strongly colored lights tint the dust.<br>  
*   `customTints`: A list of light-emitting blocks and their hex colors for dust tinting.<br>  
*   `customBiomeTints`: Define base ambient colors for biomes using hex codes.<br>  
*   `caveBiomeTriggers`: Map specific blocks to a hex color for underground environment changes.<br>

***

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

**A:** Set `breakParticleCount` to `0` in the config.
