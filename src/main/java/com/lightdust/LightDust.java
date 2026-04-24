package com.lightdust;

import com.lightdust.client.particle.DustParticle;
import com.lightdust.client.particle.ActionDustParticle;
import com.lightdust.config.LightDustConfig;
import com.lightdust.config.LightDustColorConfig;
import com.lightdust.init.ParticleInit;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(LightDust.MODID)
public class LightDust {
    public static final String MODID = "lightdust";

    public LightDust() {
        IEventBus modEventBus = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();

        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
            net.minecraftforge.fml.config.ModConfig.Type.COMMON, 
            com.lightdust.config.LightDustConfig.SPEC,
            "lightdust/main.toml"
        );

        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
            net.minecraftforge.fml.config.ModConfig.Type.COMMON, 
            com.lightdust.config.LightDustColorConfig.SPEC,
            "lightdust/colors.toml"
        );

        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
            net.minecraftforge.fml.config.ModConfig.Type.COMMON, 
            com.lightdust.config.LightDustExperimentalConfig.SPEC,
            "lightdust/experimental.toml"
        );

        com.lightdust.init.ParticleInit.PARTICLES.register(modEventBus);
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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