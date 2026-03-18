package com.lightdust.init;

import com.lightdust.LightDust;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ParticleInit {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = 
        DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, LightDust.MODID);

    public static final RegistryObject<SimpleParticleType> DUST_PARTICLE = 
        PARTICLES.register("dust_particle", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> ACTION_DUST_PARTICLE = 
        PARTICLES.register("action_dust_particle", () -> new SimpleParticleType(false));
}