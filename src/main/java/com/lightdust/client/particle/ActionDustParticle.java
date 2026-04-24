package com.lightdust.client.particle;

import com.lightdust.config.LightDustConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ActionDustParticle extends TextureSheetParticle {
    public static int CURRENT_BLOCK_COLOR = -1;
    private float rotSpeed;
    private final float baseAlpha;

    protected ActionDustParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z);
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
        
        this.quadSize = com.lightdust.config.LightDustConfig.PARTICLE_SIZE.get().floatValue() * 1.5F;
        this.baseAlpha = com.lightdust.config.LightDustConfig.AMBIENT_DUST_OPACITY.get().floatValue() * 1.5F;
        this.lifetime = 40 + level.random.nextInt(30);
        this.gravity = 0.00F; 
        this.hasPhysics = true;

        net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.containing(x, y, z);
        float r = 0.8F; 
        float g = 0.8F; 
        float b = 0.8F;

        float[] biomeTint = DustParticle.getBiomeBaseTint(level, pos);
        if (biomeTint != null) {
            r = biomeTint[0];
            g = biomeTint[1]; 
            b = biomeTint[2];
        }

        if (CURRENT_BLOCK_COLOR != -1) { 
            float blockR = ((CURRENT_BLOCK_COLOR >> 16) & 0xFF) / 255.0F;
            float blockG = ((CURRENT_BLOCK_COLOR >> 8) & 0xFF) / 255.0F;
            float blockB = (CURRENT_BLOCK_COLOR & 0xFF) / 255.0F;
            
            r = (r * 0.2F) + (blockR * 0.8F);
            g = (g * 0.2F) + (blockG * 0.8F);
            b = (b * 0.2F) + (blockB * 0.8F);
        }

        int blockLight = level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos);
        int skyLight = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos);
        int maxLight = Math.max(blockLight, skyLight);
        float intensity = Math.max(0f, (maxLight - 4) / 11.0f);
        float baseBrightness = 0.55F + (0.45F * intensity);
        
        float baseR = r * baseBrightness;
        float baseG = g * baseBrightness;
        float baseB = b * baseBrightness;

        float[] lightTint = DustParticle.getNearbyTint(level, pos);

        float strength = com.lightdust.config.LightDustColorConfig.TINT_STRENGTH.get().floatValue() * 0.3F;
        
        float rVar = (level.random.nextFloat() - 0.5F) * 0.1F;
        float gVar = (level.random.nextFloat() - 0.5F) * 0.1F;
        float bVar = (level.random.nextFloat() - 0.5F) * 0.1F;

        if (lightTint != null && strength > 0) {
            float illuminatedR = baseR * lightTint[0] * 1.5F; 
            float illuminatedG = baseG * lightTint[1] * 1.5F;
            float illuminatedB = baseB * lightTint[2] * 1.5F;
            
            this.rCol = net.minecraft.util.Mth.clamp((baseR * (1 - strength) + illuminatedR * strength) + rVar, 0.0F, 1.0F);
            this.gCol = net.minecraft.util.Mth.clamp((baseG * (1 - strength) + illuminatedG * strength) + gVar, 0.0F, 1.0F);
            this.bCol = net.minecraft.util.Mth.clamp((baseB * (1 - strength) + illuminatedB * strength) + bVar, 0.0F, 1.0F);
        } else {
            this.rCol = net.minecraft.util.Mth.clamp(baseR + rVar, 0.0F, 1.0F);
            this.gCol = net.minecraft.util.Mth.clamp(baseG + gVar, 0.0F, 1.0F);
            this.bCol = net.minecraft.util.Mth.clamp(baseB + bVar, 0.0F, 1.0F);
        }

        this.roll = level.random.nextFloat() * net.minecraft.util.Mth.TWO_PI;
        this.oRoll = this.roll;
        this.rotSpeed = (level.random.nextFloat() - 0.5F) * 0.2F;

        this.pickSprite(sprites);
        this.alpha = this.baseAlpha;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        if (this.onGround && com.lightdust.config.LightDustExperimentalConfig.ENABLE_DUST_SETTLING.get()) {
            this.xd = 0.0;
            this.zd = 0.0;
            if (this.yd < 0.0) {
                this.yd = 0.0;
            }
            this.rotSpeed = 0.0F;
        } else {
            this.oRoll = this.roll;
            this.roll += this.rotSpeed;
        }

        if (this.age > this.lifetime - 15) {
            this.alpha = this.baseAlpha * ((this.lifetime - this.age) / 15.0F);
        }

        if (!this.onGround) {
            float seed = (float)(this.x * 10.0 + this.y * 10.0 + this.z * 10.0);
            float time = (float)(this.age * 0.05F);
            
            double sinX = net.minecraft.util.Mth.sin(time * 0.8f + seed) * 0.0002;
            double cosZ = net.minecraft.util.Mth.cos(time * 1.1f + seed) * 0.0002;
            double microTurbulence = (this.level.random.nextDouble() - 0.5) * 0.0002;
            double driftDown = (LightDustConfig.ACTION_DUST_GRAVITY.get() * 0.001) + (this.level.random.nextDouble() * 0.001);
            
            this.xd += sinX + microTurbulence;
            this.zd += cosZ + (microTurbulence * 0.5);
            this.yd -= driftDown;
        }

        double prevXd = this.xd;
        double prevYd = this.yd;
        double prevZd = this.zd;

        this.move(this.xd, this.yd, this.zd);

        if (!this.onGround) {
            double bounceForce = LightDustConfig.ACTION_DUST_BOUNCE.get();
            if (this.xd != prevXd) {
                this.xd = -prevXd * bounceForce;
            }
            if (this.zd != prevZd) {
                this.zd = -prevZd * bounceForce;
            }
            if (this.yd != prevYd) {
                this.yd = -prevYd * (bounceForce * 0.75);
            }
        }

        if (this.onGround) {
            this.xd *= 0.5D;
            this.zd *= 0.5D;

            // If the particle is barely moving anymore turn off collisions
            if (this.hasPhysics && Math.abs(this.xd) < 0.005 && Math.abs(this.zd) < 0.005 && Math.abs(this.yd) < 0.005) {
                this.hasPhysics = false;
                this.xd = 0.0;
                this.yd = 0.0;
                this.zd = 0.0;
                this.rotSpeed = 0.0F; 
            }
        } else {
            this.xd *= 0.92D;
            this.yd *= 0.92D;
            this.zd *= 0.92D;
        }

        if (this.onGround) {
            this.xd *= 0.5D;
            this.zd *= 0.5D;
        } else {
            this.xd *= 0.92D;
            this.yd *= 0.92D;
            this.zd *= 0.92D;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new ActionDustParticle(level, x, y, z, dx, dy, dz, sprites);
        }
    }
}