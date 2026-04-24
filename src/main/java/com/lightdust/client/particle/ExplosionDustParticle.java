package com.lightdust.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ExplosionDustParticle extends TextureSheetParticle {
    public static int CURRENT_BLOCK_COLOR = -1;
    public static float CURRENT_GRAVITY = 0.003f;
    private final float individualGravity;
    private final float individualFriction;
    private final float baseAlpha;
    private float rotSpeed;

    protected ExplosionDustParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z);
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;

        this.quadSize = com.lightdust.config.LightDustConfig.PARTICLE_SIZE.get().floatValue() * 2.0F;
        this.baseAlpha = com.lightdust.config.LightDustConfig.AMBIENT_DUST_OPACITY.get().floatValue() * 2.0F;
        this.individualGravity = 0.001f + level.random.nextFloat() * 0.001f;
        this.individualFriction = 0.83f + level.random.nextFloat() * 0.1f;
        this.lifetime = 50 + level.random.nextInt(70);
        this.gravity = CURRENT_GRAVITY;
        this.hasPhysics = false;

        float r = 0.8F; float g = 0.8F; float b = 0.8F;
        if (CURRENT_BLOCK_COLOR != -1) {
            float blockR = ((CURRENT_BLOCK_COLOR >> 16) & 0xFF) / 255.0F;
            float blockG = ((CURRENT_BLOCK_COLOR >> 8) & 0xFF) / 255.0F;
            float blockB = (CURRENT_BLOCK_COLOR & 0xFF) / 255.0F;
            r = (r * 0.2F) + (blockR * 0.8F);
            g = (g * 0.2F) + (blockG * 0.8F);
            b = (b * 0.2F) + (blockB * 0.8F);
        }

        // Apply basic shading based on world light
        net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.containing(x, y, z);
        int blockLight = level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos);
        int skyLight = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos);
        int maxLight = Math.max(blockLight, skyLight);
        float intensity = Math.max(0f, (maxLight - 4) / 11.0f);
        float baseBrightness = 0.55F + (0.45F * intensity);

        this.rCol = net.minecraft.util.Mth.clamp(r * baseBrightness, 0.0F, 1.0F);
        this.gCol = net.minecraft.util.Mth.clamp(g * baseBrightness, 0.0F, 1.0F);
        this.bCol = net.minecraft.util.Mth.clamp(b * baseBrightness, 0.0F, 1.0F);

        this.roll = level.random.nextFloat() * net.minecraft.util.Mth.TWO_PI;
        this.oRoll = this.roll;

        this.rotSpeed = (level.random.nextFloat() - 0.5F) * 0.5F; 

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

        this.oRoll = this.roll;
        this.roll += this.rotSpeed;

        this.rotSpeed *= 0.92F;

        if (this.age > this.lifetime - 20) {
            this.alpha = this.baseAlpha * ((this.lifetime - this.age) / 20.0F);
        }

        float chaoticGravity = this.gravity * (0.8F + (this.level.random.nextFloat() * 0.4F));
        this.yd -= chaoticGravity;

        this.move(this.xd, this.yd, this.zd);

        this.xd *= this.individualFriction; 
        this.yd *= this.individualFriction;
        this.zd *= this.individualFriction;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new ExplosionDustParticle(level, x, y, z, dx, dy, dz, sprites);
        }
    }
}