package com.lightdust.config;
import net.minecraftforge.common.ForgeConfigSpec;
import java.util.Arrays;
import java.util.List;

public class LightDustColorConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue TINT_STRENGTH;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CUSTOM_TINTS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CUSTOM_BIOME_TINTS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CAVE_BIOME_TRIGGERS;

    public static final List<String> DEFAULT_CUSTOM_TINTS = Arrays.asList(
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
    );

    public static final List<String> DEFAULT_BIOME_TINTS = Arrays.asList(
        "minecraft:lush_caves=#8FCE00",
        "minecraft:dripstone_caves=#8B6B4A",
        "minecraft:deep_dark=#006666",
        "minecraft:forest=#70924D",
        "minecraft:flower_forest=#9AB96D",
        "minecraft:birch_forest=#84A65D",
        "minecraft:old_growth_birch_forest=#84A65D",
        "minecraft:dark_forest=#3E5B2D",
        "minecraft:jungle=#537B2F",
        "minecraft:sparse_jungle=#628D37",
        "minecraft:bamboo_jungle=#659832",
        "minecraft:taiga=#526E54",
        "minecraft:snowy_taiga=#FFFFFF",
        "minecraft:old_growth_pine_taiga=#445C45",
        "minecraft:old_growth_spruce_taiga=#3F5640",
        "minecraft:plains=#A8D080",
        "minecraft:sunflower_plains=#BDE491",
        "minecraft:snowy_plains=#FFFFFF",
        "minecraft:desert=#E4D5A7",
        "minecraft:savanna=#B3A25E",
        "minecraft:savanna_plateau=#A99958",
        "minecraft:windswept_savanna=#9E9156",
        "minecraft:badlands=#B26344",
        "minecraft:eroded_badlands=#C26845",
        "minecraft:wooded_badlands=#A35A3D",
        "minecraft:swamp=#4C5E35",
        "minecraft:mangrove_swamp=#54683C",
        "minecraft:river=#3F76E4",
        "minecraft:frozen_river=#CCFFFF",
        "minecraft:warm_ocean=#00AAAA",
        "minecraft:ocean=#00008B",
        "minecraft:cold_ocean=#202070",
        "minecraft:frozen_ocean=#CCFFFF",
        "minecraft:meadow=#98C874",
        "minecraft:cherry_grove=#FFB6C1",
        "minecraft:grove=#7A9C86",
        "minecraft:snowy_slopes=#FFFFFF",
        "minecraft:jagged_peaks=#F0F5F5",
        "minecraft:frozen_peaks=#FFFFFF",
        "minecraft:stony_peaks=#9E9E9E",
        "minecraft:windswept_hills=#8B9E8B",
        "minecraft:windswept_gravelly_hills=#8A948A",
        "minecraft:windswept_forest=#6C8B6C",
        "minecraft:ice_spikes=#CCFFFF",
        "minecraft:mushroom_fields=#807080",
        "minecraft:beach=#FADE55",
        "minecraft:snowy_beach=#FFFFFF",
        "minecraft:stony_shore=#8C8C8C",
        "minecraft:nether_wastes=#702B2B",
        "minecraft:crimson_forest=#821A1A",
        "minecraft:warped_forest=#1A8275",
        "minecraft:soul_sand_valley=#453531",
        "minecraft:basalt_deltas=#4A4A52",
        "minecraft:the_end=#C0A0C0",
        "minecraft:small_end_islands=#B090B0",
        "minecraft:end_midlands=#C0A0C0",
        "minecraft:end_highlands=#C0A0C0",
        "minecraft:end_barrens=#B090B0"
    );

    public static final List<String> DEFAULT_CAVE_TRIGGERS = Arrays.asList(
        "minecraft:moss_block=#8FCE00",
        "minecraft:cave_vines=#8FCE00",
        "minecraft:cave_vines_plant=#8FCE00",
        "minecraft:spore_blossom=#8FCE00",
        "minecraft:pointed_dripstone=#8B6B4A",
        "minecraft:dripstone_block=#8B6B4A",
        "minecraft:sculk=#006666",
        "minecraft:sculk_vein=#006666",
        "minecraft:sculk_sensor=#006666",
        "minecraft:snow=#FFFFFF",
        "minecraft:snow_block=#FFFFFF",
        "minecraft:powder_snow=#FFFFFF",
        "minecraft:ice=#CCFFFF",
        "minecraft:packed_ice=#CCFFFF",
        "minecraft:blue_ice=#CCFFFF"
    );

    static {
        BUILDER.push("Color and Tinting");
        
        TINT_STRENGTH = BUILDER.comment("How strongly colored lights (like soul fire) tint the dust. 0.0 = no tint, 1.0 = full color")
            .defineInRange("tintStrength", 0.6, 0.0, 1.0);
            
        CUSTOM_TINTS = BUILDER.comment(
            "List of blocks and their hex colors for dust tinting.",
            "Format: 'modid:block_name=#RRGGBB'",
            "Note: The block must actually emit light for the tint to apply. Make sure to include the '#' before the hex code!"
        ).defineList("customTints",
            DEFAULT_CUSTOM_TINTS,
            obj -> obj instanceof String && ((String) obj).contains("=") && ((String) obj).contains("#")
        );

        CUSTOM_BIOME_TINTS = BUILDER.comment(
            "List of biomes and their base ambient dust colors.",
            "Format: 'modid:biome_name=#RRGGBB'",
            "These act as the base color of the dust before any block lights affect it."
        ).defineList("customBiomeTints",
            DEFAULT_BIOME_TINTS,
            obj -> obj instanceof String && ((String) obj).contains("=") && ((String) obj).contains("#")
        );

        CAVE_BIOME_TRIGGERS = BUILDER.comment(
            "List of blocks that trigger a specific ambient dust color when found underground.",
            "Useful for modded caves or overriding vanilla ones.",
            "Format: 'modid:block_name=#RRGGBB'",
            "The mod will scan for these blocks and change the dust color if enough are nearby. Blocks sharing the same hex color are counted together!"
        ).defineList("caveBiomeTriggers",
            DEFAULT_CAVE_TRIGGERS,
            obj -> obj instanceof String && ((String) obj).contains("=") && ((String) obj).contains("#")
        );
        BUILDER.pop();
        
        SPEC = BUILDER.build();
    }
}