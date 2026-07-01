package com.lightdust;

import com.lightdust.client.particle.DustParticle;
import com.lightdust.client.particle.ActionDustParticle;
import com.lightdust.init.ParticleInit;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.fml.ModContainer;

@Mod(LightDust.MODID)
public class LightDust {
    public static final String MODID = "lightdust";

    public LightDust(IEventBus modEventBus, ModContainer modContainer) {

        modContainer.registerConfig(ModConfig.Type.COMMON, com.lightdust.config.LightDustConfig.SPEC, "lightdust/main.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, com.lightdust.config.LightDustColorConfig.SPEC, "lightdust/colors.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, com.lightdust.config.LightDustExperimentalConfig.SPEC, "lightdust/experimental.toml");

        modContainer.registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class, (mc, parent) -> com.lightdust.client.gui.LightDustConfigMenu.createScreen(parent));

        ParticleInit.PARTICLES.register(modEventBus);

        modEventBus.addListener(this::onConfigReload);
    }

    public void onConfigReload(net.neoforged.fml.event.config.ModConfigEvent.Reloading event) {
        if (event.getConfig().getModId().equals(MODID)) {
            if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
                if (com.lightdust.config.LightDustConfig.SPEC.isLoaded() && com.lightdust.config.LightDustColorConfig.SPEC.isLoaded()) {
                    com.lightdust.client.particle.DustParticle.reloadColors();
                    com.lightdust.client.HandheldLightManager.reload();
                }
            }
        }
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(ParticleInit.DUST_PARTICLE.get(), DustParticle.Provider::new);
            event.registerSpriteSet(ParticleInit.ACTION_DUST_PARTICLE.get(), ActionDustParticle.Provider::new);
            event.registerSpriteSet(ParticleInit.EXPLOSION_DUST_PARTICLE.get(), com.lightdust.client.particle.ExplosionDustParticle.Provider::new);

            event.registerSpriteSet(ParticleInit.DUST_PARTICLE_HD.get(), DustParticle.Provider::new);
            event.registerSpriteSet(ParticleInit.ACTION_DUST_PARTICLE_HD.get(), ActionDustParticle.Provider::new);
            event.registerSpriteSet(ParticleInit.EXPLOSION_DUST_PARTICLE_HD.get(), com.lightdust.client.particle.ExplosionDustParticle.Provider::new);
        }
    }
}