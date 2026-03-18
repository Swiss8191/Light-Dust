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
        
        try {
            String biomeName = level.getBiome(pos).unwrapKey().map(key -> key.location().toString()).orElse("minecraft:plains");
            for (String entry : com.lightdust.config.LightDustColorConfig.CUSTOM_BIOME_TINTS.get()) {
                String[] parts = entry.split("=");
                if (parts.length == 2 && parts[0].trim().equals(biomeName) && parts[1].contains("#")) {
                    String hex = parts[1].substring(parts[1].indexOf("#") + 1).trim();
                    if (hex.length() == 6) {
                        r = Integer.parseInt(hex.substring(0, 2), 16) / 255f;
                        g = Integer.parseInt(hex.substring(2, 4), 16) / 255f;
                        b = Integer.parseInt(hex.substring(4, 6), 16) / 255f;
                    }
                    break;
                }
            }
        } catch (Exception e) {
            // Fallback
        }

        int blockLight = level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos);
        int skyLight = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos);
        int maxLight = Math.max(blockLight, skyLight);
        
        float intensity = Math.max(0f, (maxLight - 4) / 11.0f);
        float baseBrightness = 0.35F + (0.65F * intensity);

        float baseR = r * baseBrightness;
        float baseG = g * baseBrightness;
        float baseB = b * baseBrightness;
        float[] blockTint = null;
 
        searchLoop:
        for (net.minecraft.core.BlockPos p : net.minecraft.core.BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2))) {
            net.minecraft.world.level.block.state.BlockState state = level.getBlockState(p);
            if (state.getLightEmission(level, p) > 0) {
                String blockName = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
                for (String entry : com.lightdust.config.LightDustColorConfig.CUSTOM_TINTS.get()) {
                    String[] parts = entry.split("=");
                    if (parts.length == 2 && parts[0].trim().equals(blockName) && parts[1].contains("#")) {
                        String hex = parts[1].substring(parts[1].indexOf("#") + 1).trim();
                        if (hex.length() == 6) {
                            blockTint = new float[] {
                                Integer.parseInt(hex.substring(0, 2), 16) / 255f,
                                Integer.parseInt(hex.substring(2, 4), 16) / 255f,
                                Integer.parseInt(hex.substring(4, 6), 16) / 255f
                            };
                        }
                        break searchLoop;
                    }
                }
            }
        }

        float strength = com.lightdust.config.LightDustColorConfig.TINT_STRENGTH.get().floatValue();
        float rVar = (level.random.nextFloat() - 0.5F) * 0.1F;
        float gVar = (level.random.nextFloat() - 0.5F) * 0.1F;
        float bVar = (level.random.nextFloat() - 0.5F) * 0.1F;

        if (blockTint != null && strength > 0) {
            this.rCol = net.minecraft.util.Mth.clamp((baseR * (1 - strength) + blockTint[0] * strength) + rVar, 0.0F, 1.0F);
            this.gCol = net.minecraft.util.Mth.clamp((baseG * (1 - strength) + blockTint[1] * strength) + gVar, 0.0F, 1.0F);
            this.bCol = net.minecraft.util.Mth.clamp((baseB * (1 - strength) + blockTint[2] * strength) + bVar, 0.0F, 1.0F);
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

        if (this.onGround && LightDustConfig.ENABLE_DUST_SETTLING.get()) {
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

            double driftDown = LightDustConfig.ACTION_DUST_GRAVITY.get() + (this.level.random.nextDouble() * 0.001);
            
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
                this.yd = -Math.abs(prevYd) * (bounceForce * 0.75) - 0.02; 
            }
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