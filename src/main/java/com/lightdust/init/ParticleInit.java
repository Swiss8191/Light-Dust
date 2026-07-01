package com.lightdust.init;

import com.lightdust.LightDust;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ParticleInit {

    public static final DeferredRegister<ParticleType<?>> PARTICLES
            = DeferredRegister.create(Registries.PARTICLE_TYPE, LightDust.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DUST_PARTICLE
            = PARTICLES.register("dust_particle", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ACTION_DUST_PARTICLE
            = PARTICLES.register("action_dust_particle", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> EXPLOSION_DUST_PARTICLE
            = PARTICLES.register("explosion_dust_particle", () -> new SimpleParticleType(false));

    // HD Variants
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DUST_PARTICLE_HD
            = PARTICLES.register("dust_particle_hd", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ACTION_DUST_PARTICLE_HD
            = PARTICLES.register("action_dust_particle_hd", () -> new SimpleParticleType(false));
            
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> EXPLOSION_DUST_PARTICLE_HD
            = PARTICLES.register("explosion_dust_particle_hd", () -> new SimpleParticleType(false));
}