package com.lightdust.client.particle;

import com.lightdust.config.LightDustConfig;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class DustParticle extends TextureSheetParticle {
    
    public static final Long2IntOpenHashMap AMBIENT_COUNTS = new Long2IntOpenHashMap();
    public static int TOTAL_AMBIENT_COUNT = 0;
    public static BlockPos PENDING_POS = null;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final java.util.Map<net.minecraft.world.level.block.Block, float[]> LIGHT_COLORS = new java.util.HashMap<>();
    public static boolean colorsLoaded = false;

    private final BlockPos ownerPos;
    private final float rotSpeed;
    private final int tickOffset;
    private float baseAlpha;
    private final float variation;

    protected DustParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z);
        this.ownerPos = PENDING_POS;
        this.tickOffset = level.random.nextInt(20);
        float ambientOpacity = LightDustConfig.AMBIENT_DUST_OPACITY.get().floatValue();

        this.variation = level.random.nextFloat() * 0.05F;
        if (this.ownerPos != null) {
            AMBIENT_COUNTS.addTo(this.ownerPos.asLong(), 1);
            TOTAL_AMBIENT_COUNT++;
            int light = level.getBrightness(LightLayer.BLOCK, this.ownerPos);
            float intensity = Math.max(0f, (light - 6) / 9.0f);
            float baseBrightness = 0.15F + (0.85F * intensity);
            
            // grab the tint
            float[] tint = getNearbyTint(level, this.ownerPos);
            float strength = LightDustConfig.TINT_STRENGTH.get().floatValue();
            
            if (tint != null && strength > 0) {
                this.rCol = (baseBrightness * (1 - strength) + tint[0] * strength) + this.variation;
                this.gCol = (baseBrightness * (1 - strength) + tint[1] * strength) + this.variation;
                this.bCol = (baseBrightness * (1 - strength) + tint[2] * strength) + this.variation;
            } else {
                this.rCol = baseBrightness + this.variation;
                this.gCol = baseBrightness + this.variation;
                this.bCol = baseBrightness + this.variation;
            }
            
            this.baseAlpha = ambientOpacity + (0.28F * intensity);
            this.alpha = 0.0F; 
            this.lifetime = LightDustConfig.PARTICLE_LIFETIME.get() + level.random.nextInt(100);
        } else {
            this.lifetime = LightDustConfig.PARTICLE_LIFETIME.get() / 2;
            this.rCol = 0.8F;
            this.gCol = 0.8F; 
            this.bCol = 0.8F;
            this.baseAlpha = ambientOpacity;
            this.alpha = baseAlpha;
        }

        this.quadSize = LightDustConfig.PARTICLE_SIZE.get().floatValue(); 
        this.gravity = 0.000F;

        if (dx != 0 || dy != 0 || dz != 0) {
            this.xd = dx;
            this.yd = dy;
            this.zd = dz;
        } else {
            this.xd = (level.random.nextFloat() - 0.5F) * 0.005F;
            this.yd = (level.random.nextFloat() - 0.5F) * 0.005F;
            this.zd = (level.random.nextFloat() - 0.5F) * 0.005F;
        }
        
        this.hasPhysics = false; 
        this.roll = level.random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;
        this.rotSpeed = (level.random.nextFloat() - 0.5F) * 0.1F; 

        this.pickSprite(sprites);
        this.setSize(0.01F, 0.01F);
    }

    @Override
    public void remove() {
        if (!this.removed && ownerPos != null) {
            long key = ownerPos.asLong();
            int oldVal = AMBIENT_COUNTS.addTo(key, -1);
            if (oldVal <= 1) AMBIENT_COUNTS.remove(key);
            if (TOTAL_AMBIENT_COUNT > 0) TOTAL_AMBIENT_COUNT--;
        }
        super.remove();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        if ((this.age + tickOffset) % 10 == 0) {
            BlockPos blockpos = BlockPos.containing(this.x, this.y, this.z);
            return this.level.hasChunkAt(blockpos) ? LevelRenderer.getLightColor(this.level, blockpos) : 0;
        }
        return super.getLightColor(partialTick);
    }

    @Override
    public void tick() {
        super.tick();
        // Fade Logic
        if (this.age < 20) {
            this.alpha = this.baseAlpha * (this.age / 20.0F);
        } else if (this.age > this.lifetime - 20) {
            this.alpha = this.baseAlpha * ((this.lifetime - this.age) / 20.0F);
        } else {
            this.alpha = this.baseAlpha;
        }

        if ((this.age + tickOffset) % 20 == 0) {
            BlockPos currentPos = BlockPos.containing(this.x, this.y, this.z);
            if (level.getFluidState(currentPos).is(FluidTags.WATER)) {
                this.remove(); return;
            }
            
            int blockLight = level.getBrightness(LightLayer.BLOCK, currentPos);
            if (blockLight < 4) {
                this.remove();
                return;
            }

            if (this.ownerPos != null) {
                float intensity = Math.max(0f, (blockLight - 6) / 9.0f);
                float baseBrightness = 0.15F + (0.85F * intensity);

                float[] tint = getNearbyTint(level, this.ownerPos);
                float strength = LightDustConfig.TINT_STRENGTH.get().floatValue();
                
                if (tint != null && strength > 0) {
                    this.rCol = (baseBrightness * (1 - strength) + tint[0] * strength) + this.variation;
                    this.gCol = (baseBrightness * (1 - strength) + tint[1] * strength) + this.variation;
                    this.bCol = (baseBrightness * (1 - strength) + tint[2] * strength) + this.variation;
                } else {
                    this.rCol = baseBrightness + this.variation;
                    this.gCol = baseBrightness + this.variation;
                    this.bCol = baseBrightness + this.variation;
                }
                
                float ambientOpacity = LightDustConfig.AMBIENT_DUST_OPACITY.get().floatValue();
                this.baseAlpha = ambientOpacity + (0.28F * intensity);
            }

            long time = level.getDayTime() % 24000;
            boolean isDay = time < 13000 || time > 23000;
            if (isDay) {
                int skyLight = level.getBrightness(LightLayer.SKY, currentPos);
                int diffThreshold = LightDustConfig.DAYTIME_LIGHT_DIFF.get();

                if ((blockLight - skyLight) <= diffThreshold) {
                    this.remove();
                    return;
                }
            }

            Player player = Minecraft.getInstance().player;
            if (player != null) {
                double maxDist = LightDustConfig.AMBIENT_HARD_CAP.get();
                if (player.distanceToSqr(this.x, this.y, this.z) > maxDist * maxDist) {
                    this.remove();
                    return;
                }
            }
        }

        this.oRoll = this.roll;
        this.roll += this.rotSpeed;
        
        // Physics
        float seed = (float)(this.x + this.y + this.z);
        float time = (float)((this.age + seed) * 0.15);
        double sinX = Mth.sin(time * 0.8f + seed);
        double cosZ = Mth.cos(time * 1.1f + seed);
        
        double driftDown = 0.00002 + (level.random.nextDouble() * 0.00005);
        double microTurbulence = (level.random.nextDouble() - 0.5) * 0.00012;

        this.xd += sinX * 0.0001 + microTurbulence;
        this.zd += cosZ * 0.0001 + (microTurbulence * 0.5);
        this.yd -= (driftDown + (sinX * 0.00005));
        double jitterX = (level.random.nextDouble() - 0.5) * 0.02;
        double jitterY = (level.random.nextDouble() - 0.5) * 0.02;
        double jitterZ = (level.random.nextDouble() - 0.5) * 0.02;

        Player player = Minecraft.getInstance().player;
        if (player != null && player.distanceToSqr(this.x, this.y, this.z) < 4.0) {
            double range = 2.0;
            double dx = this.x - player.getX();
            double dy = this.y - (player.getY() + 1.0); 
            double dz = this.z - player.getZ();
            double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
            if (dist < 0.01) dist = 0.01;
            double nx = dx / dist; double ny = dy / dist; double nz = dz / dist;
            Vec3 pVel = player.getDeltaMovement();
            double horizontalSpeed = Math.sqrt(pVel.x * pVel.x + pVel.z * pVel.z);
            if (player.swingTime > 0) {
                Vec3 look = player.getLookAngle();
                if ((nx * look.x) + (ny * look.y) + (nz * look.z) > 0.5) { 
                    double slashForce = 0.002;
                    this.xd += look.x * slashForce + (nx * 0.005) + jitterX;
                    this.yd += look.y * slashForce + (ny * 0.005) + jitterY;
                    this.zd += look.z * slashForce + (nz * 0.005) + jitterZ;
                }
            }

            if (player.isUsingItem() && player.getUseItem().getItem() instanceof ShieldItem) {
                Vec3 look = player.getLookAngle();
                if ((nx * look.x) + (ny * look.y) + (nz * look.z) > 0.3) {
                    double shieldPush = 0.04 / dist;
                    this.xd += (nx * shieldPush) + jitterX;
                    this.yd += (ny * shieldPush) + jitterY;
                    this.zd += (nz * shieldPush) + jitterZ;
                }
            }

            if (horizontalSpeed > 0.01) {
                double proximityFactor = (range - dist) / range;
                double pushStrength = horizontalSpeed * proximityFactor * 0.05; 
                this.xd += (nx * pushStrength) + jitterX;
                this.yd += (ny * pushStrength) + jitterY;
                this.zd += (nz * pushStrength) + jitterZ;
            }
        }

        if (level.isClientSide) {
             HitResult hit = Minecraft.getInstance().hitResult;
             if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                 BlockPos breakPos = ((BlockHitResult)hit).getBlockPos();
                 if (player != null && player.swingTime > 0 && breakPos.distToCenterSqr(this.x, this.y, this.z) < 4.0) {
                     double dX = this.x - (breakPos.getX() + 0.5);
                     double dY = this.y - (breakPos.getY() + 0.5);
                     double dZ = this.z - (breakPos.getZ() + 0.5);
                     double distSqrBreak = dX * dX + dY * dY + dZ * dZ;
                     if (level.getBlockState(breakPos).isAir() && distSqrBreak < 3) {
                         double distBreak = Math.sqrt(distSqrBreak);
                         if (distBreak < 0.1) distBreak = 0.1;
                         double force = 0.01; 
                         this.xd += (dX / distBreak) * force + jitterX;
                         this.yd += (dY / distBreak) * force + jitterY;
                         this.zd += (dZ / distBreak) * force + jitterZ;
                     }
                 }
             }
         }

        this.xd *= 0.94;
        this.yd *= 0.94;
        this.zd *= 0.94;
        
        this.move(this.xd, this.yd, this.zd);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites = sprites; }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new DustParticle(level, x, y, z, dx, dy, dz, sprites);
        }
    }

    public static void reloadColors() {
        LIGHT_COLORS.clear();
        for (String entry : LightDustConfig.CUSTOM_TINTS.get()) {
            try {
                String[] parts = entry.split("=");
                if (parts.length == 2 && parts[1].contains("#")) {
                    net.minecraft.resources.ResourceLocation rl = new net.minecraft.resources.ResourceLocation(parts[0].trim());
                    net.minecraft.world.level.block.Block block = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(rl);
                    
                    if (block != null && block != net.minecraft.world.level.block.Blocks.AIR) {
                        String hex = parts[1].substring(parts[1].indexOf("#") + 1).trim();
                        if (hex.length() == 6) {
                            int r = Integer.parseInt(hex.substring(0, 2), 16);
                            int g = Integer.parseInt(hex.substring(2, 4), 16);
                            int b = Integer.parseInt(hex.substring(4, 6), 16);
                            LIGHT_COLORS.put(block, new float[]{r / 255f, g / 255f, b / 255f});
                        } else {
                            LOGGER.error("[Light Dust] Invalid hex code length in config for entry '{}'. Must be 6 characters after '#'.", entry);
                        }
                    } else {
                        LOGGER.warn("[Light Dust] Block not found in registry for config entry '{}'. It may be from an uninstalled mod.", entry);
                    }
                } else {
                    LOGGER.error("[Light Dust] Malformed custom tint entry: '{}'. Format must be 'modid:block_name=#RRGGBB'.", entry);
                }
            } catch (NumberFormatException e) {
                LOGGER.error("[Light Dust] Invalid hex characters in config for entry '{}'.", entry);
            } catch (Exception e) {
                LOGGER.error("[Light Dust] Failed to parse custom tint config entry '{}': {}", entry, e.getMessage());
            }
        }
        colorsLoaded = true;
    }

    private float[] getNearbyTint(ClientLevel level, BlockPos pos) {
        if (!colorsLoaded) {
            reloadColors();
        }
        
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2))) {
            net.minecraft.world.level.block.state.BlockState state = level.getBlockState(p);
            
            if (state.getLightEmission(level, p) > 0) {
                float[] color = LIGHT_COLORS.get(state.getBlock());
                if (color != null) {
                    return color; 
                }
            }
        }
        return null; 
    }
}