package com.lightdust.client;

import com.lightdust.config.LightDustConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

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
    
    private static boolean curiosChecked = false;
    private static boolean hasCurios = false;
    private static java.lang.reflect.Method getCuriosHelperMethod;
    private static java.lang.reflect.Method getEquippedCuriosMethod;
    
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
        lastUpdateTick = -1; // Reset cache on reload

        for (String entry : LightDustConfig.HANDHELD_LIGHT_ITEMS.get()) {
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
                            String[] split = cleanId.split(":");
                            String namespace = split.length == 2 ? split[0] : "minecraft";
                            String path = split.length == 2 ? split[1] : split[0];
                            
                            if (id.startsWith("#")) {
                                TagKey<Item> tag = ItemTags.create(new ResourceLocation(namespace, path));
                                TAG_LIGHTS.put(tag, data);
                            } else {
                                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(namespace, path));
                                if (item != null) {
                                    ITEM_LIGHTS.put(item, data);
                                }
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
        if (!curiosChecked) {
            curiosChecked = true;
            if (net.minecraftforge.fml.ModList.get().isLoaded("curios")) {
                try {
                    Class<?> apiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
                    getCuriosHelperMethod = apiClass.getMethod("getCuriosHelper");
                    Class<?> helperClass = getCuriosHelperMethod.getReturnType();
                    getEquippedCuriosMethod = helperClass.getMethod("getEquippedCurios", net.minecraft.world.entity.LivingEntity.class);
                    hasCurios = true;
                } catch (Exception e) {
                    hasCurios = false;
                }
            }
        }

        if (!accChecked) {
            accChecked = true;
            if (net.minecraftforge.fml.ModList.get().isLoaded("accessories")) {
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
        if (obj == null || depth > 4) {
            return;
        }

        if (obj instanceof ItemStack) {
            found.add((ItemStack) obj);
            return;
        }
        if (obj instanceof Iterable) {
            for (Object o : (Iterable<?>) obj) {
                extractStacksFrom(o, depth + 1, found);
            }
            return;
        }
        if (obj instanceof java.util.Map) {
            for (Object o : ((java.util.Map<?, ?>) obj).values()) {
                extractStacksFrom(o, depth + 1, found);
            }
            return;
        }
        if (obj instanceof java.util.Optional) {
            ((java.util.Optional<?>) obj).ifPresent(o -> extractStacksFrom(o, depth + 1, found));
            return;
        }

        Class<?> clazz = obj.getClass();
        String className = clazz.getName();
        if (!className.startsWith("io.wispforest.accessories")) {
            return;
        }

        if (!itemStackGetters.containsKey(clazz)) {
            java.util.List<java.lang.reflect.Method> itemMethods = new java.util.ArrayList<>();
            java.util.List<java.lang.reflect.Method> iterMethods = new java.util.ArrayList<>();

            for (java.lang.reflect.Method m : clazz.getMethods()) {
                if (m.getParameterCount() == 0 && m.getDeclaringClass() != Object.class) {
                    if (m.getReturnType() == ItemStack.class) {
                        itemMethods.add(m);
                    } else if (Iterable.class.isAssignableFrom(m.getReturnType())
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
                if (stack != null && !stack.isEmpty()) {
                    found.add(stack);
                }
            } catch (Exception ignored) {
            }
        }

        for (java.lang.reflect.Method m : iterableGetters.get(clazz)) {
            try {
                extractStacksFrom(m.invoke(obj), depth + 1, found);
            } catch (Exception ignored) {
            }
        }
    }

    public static LightData getHeldLight(Player player) {
        if (!LightDustConfig.ENABLE_HANDHELD_LIGHTS.get()) {
            return null;
        }
        if (!loaded) {
            reload();
        }

        long currentTick = player.level().getGameTime();
        if (currentTick == lastUpdateTick) {
            return cachedLight;
        }
        lastUpdateTick = currentTick;

        LightData strongestLight = null;
        for (ItemStack stack : player.getAllSlots()) {
            LightData data = getLightData(stack);
            if (data != null && (strongestLight == null || data.radius > strongestLight.radius)) {
                strongestLight = data;
            }
        }

        initCompatReflection();
        if (hasCurios) {
            try {
                Object helper = getCuriosHelperMethod.invoke(null);
                java.util.Optional<?> opt = (java.util.Optional<?>) getEquippedCuriosMethod.invoke(helper, player);
                if (opt.isPresent()) {
                    net.minecraftforge.items.IItemHandler handler = (net.minecraftforge.items.IItemHandler) opt.get();
                    for (int i = 0; i < handler.getSlots(); i++) {
                        ItemStack stack = handler.getStackInSlot(i);
                        LightData data = getLightData(stack);
                        if (data != null && (strongestLight == null || data.radius > strongestLight.radius)) {
                            strongestLight = data;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

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
        if (stack.isEmpty()) {
            return null;
        }
        Item item = stack.getItem();
        if (ITEM_LIGHTS.containsKey(item)) {
            return ITEM_LIGHTS.get(item);
        }
        for (Map.Entry<TagKey<Item>, LightData> entry : TAG_LIGHTS.entrySet()) {
            if (stack.is(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}