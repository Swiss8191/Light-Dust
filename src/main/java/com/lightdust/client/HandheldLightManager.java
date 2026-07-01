package com.lightdust.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.lightdust.config.LightDustConfig;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class HandheldLightManager {

    public static class LightData {
        public final int radius;
        public final float[] color;

        public LightData(int radius, float[] color) {
            this.radius = radius;
            this.color = color;
        }
    }

    private static final Map<Item, LightData> ITEM_LIGHTS = new HashMap<>();
    private static final Map<TagKey<Item>, LightData> TAG_LIGHTS = new HashMap<>();
    private static boolean loaded = false;

    private static boolean accChecked = false;
    private static boolean hasAcc = false;
    private static java.lang.reflect.Method getAccMethod;

    private static final Map<Class<?>, java.util.List<java.lang.reflect.Method>> itemStackGetters = new HashMap<>();
    private static final Map<Class<?>, java.util.List<java.lang.reflect.Method>> iterableGetters = new HashMap<>();

    private static long lastUpdateTick = -1;
    private static LightData cachedLight = null;

    public static void reload() {
        ITEM_LIGHTS.clear();
        TAG_LIGHTS.clear();
        lastUpdateTick = -1;

        for (String entry : LightDustConfig.COMMON.handheldLightItems.get()) {
            try {
                String[] parts = entry.split("=");
                if (parts.length == 2) {
                    String[] vals = parts[1].split(",");
                    if (vals.length == 2 && vals[1].contains("#")) {
                        int radius = Integer.parseInt(vals[0].trim());
                        String hex = vals[1].substring(vals[1].indexOf("#") + 1).trim();

                        if (hex.length() == 6) {
                            float r = Integer.parseInt(hex.substring(0, 2), 16) / 255f;
                            float g = Integer.parseInt(hex.substring(2, 4), 16) / 255f;
                            float b = Integer.parseInt(hex.substring(4, 6), 16) / 255f;
                            LightData data = new LightData(radius, new float[]{r, g, b});

                            String id = parts[0].trim();
                            String cleanId = id.startsWith("#") ? id.substring(1) : id;
                            ResourceLocation resLoc = ResourceLocation.parse(cleanId);

                            if (id.startsWith("#")) {
                                TagKey<Item> tag = TagKey.create(Registries.ITEM, resLoc);
                                TAG_LIGHTS.put(tag, data);
                            } else {
                                Optional<Item> itemOpt = BuiltInRegistries.ITEM.getOptional(resLoc);
                                itemOpt.ifPresent(item -> ITEM_LIGHTS.put(item, data));
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        loaded = true;
    }

    private static void initCompatReflection() {
        if (!accChecked) {
            accChecked = true;
            if (net.neoforged.fml.ModList.get().isLoaded("accessories")) {
                try {
                    Class<?> apiClass = Class.forName("io.wispforest.accessories.api.AccessoriesCapability");
                    getAccMethod = apiClass.getMethod("getOptionally", net.minecraft.world.entity.LivingEntity.class);
                    hasAcc = true;
                } catch (Exception e) {
                    try {
                        Class<?> apiClass = Class.forName("io.wispforest.accessories.api.AccessoriesCapability");
                        getAccMethod = apiClass.getMethod("get", net.minecraft.world.entity.LivingEntity.class);
                        hasAcc = true;
                    } catch (Exception e2) {
                        hasAcc = false;
                    }
                }
            }
        }
    }

    private static void extractStacksFrom(Object obj, int depth, java.util.List<ItemStack> found) {
        if (obj == null || depth > 4) return;

        if (obj instanceof ItemStack stack) {
            found.add(stack);
            return;
        }
        if (obj instanceof Iterable<?> iter) {
            for (Object o : iter) extractStacksFrom(o, depth + 1, found);
            return;
        }
        if (obj instanceof java.util.Map<?, ?> map) {
            for (Object o : map.values()) extractStacksFrom(o, depth + 1, found);
            return;
        }
        if (obj instanceof java.util.Optional<?> opt) {
            opt.ifPresent(o -> extractStacksFrom(o, depth + 1, found));
            return;
        }

        Class<?> clazz = obj.getClass();
        String className = clazz.getName();
        if (!className.startsWith("io.wispforest.accessories")) return;

        if (!itemStackGetters.containsKey(clazz)) {
            java.util.List<java.lang.reflect.Method> itemMethods = new java.util.ArrayList<>();
            java.util.List<java.lang.reflect.Method> iterMethods = new java.util.ArrayList<>();

            for (java.lang.reflect.Method m : clazz.getMethods()) {
                if (m.getParameterCount() == 0 && m.getDeclaringClass() != Object.class) {
                    if (m.getReturnType() == ItemStack.class) itemMethods.add(m);
                    else if (Iterable.class.isAssignableFrom(m.getReturnType())
                            || java.util.Map.class.isAssignableFrom(m.getReturnType())
                            || java.util.Optional.class.isAssignableFrom(m.getReturnType())) {
                        iterMethods.add(m);
                    }
                }
            }
            itemStackGetters.put(clazz, itemMethods);
            iterableGetters.put(clazz, iterMethods);
        }

        for (java.lang.reflect.Method m : itemStackGetters.get(clazz)) {
            try {
                ItemStack stack = (ItemStack) m.invoke(obj);
                if (stack != null && !stack.isEmpty()) found.add(stack);
            } catch (Exception ignored) {}
        }

        for (java.lang.reflect.Method m : iterableGetters.get(clazz)) {
            try {
                extractStacksFrom(m.invoke(obj), depth + 1, found);
            } catch (Exception ignored) {}
        }
    }

    public static LightData getHeldLight(Player player) {
        if (!loaded) return null;
        if (player.level().getGameTime() - lastUpdateTick < 5) return cachedLight;
        lastUpdateTick = player.level().getGameTime();

        LightData strongestLight = null;

        LightData mainHandLight = getLightData(player.getMainHandItem());
        if (mainHandLight != null) strongestLight = mainHandLight;

        LightData offHandLight = getLightData(player.getOffhandItem());
        if (offHandLight != null && (strongestLight == null || offHandLight.radius > strongestLight.radius)) {
            strongestLight = offHandLight;
        }

        initCompatReflection();
        if (hasAcc) {
            try {
                Object container = getAccMethod.invoke(null, player);
                java.util.List<ItemStack> accStacks = new java.util.ArrayList<>();
                extractStacksFrom(container, 0, accStacks);

                for (ItemStack stack : accStacks) {
                    LightData data = getLightData(stack);
                    if (data != null && (strongestLight == null || data.radius > strongestLight.radius)) {
                        strongestLight = data;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        cachedLight = strongestLight;
        return strongestLight;
    }

    private static LightData getLightData(ItemStack stack) {
        if (stack.isEmpty()) return null;
        
        Item item = stack.getItem();
        if (ITEM_LIGHTS.containsKey(item)) return ITEM_LIGHTS.get(item);
        
        for (Map.Entry<TagKey<Item>, LightData> entry : TAG_LIGHTS.entrySet()) {
            if (stack.is(entry.getKey())) return entry.getValue();
        }
        return null;
    }
}