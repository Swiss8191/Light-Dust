package com.lightdust.client.particle;

import com.lightdust.config.LightDustConfig;
import com.lightdust.config.LightDustColorConfig;
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

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
@OnlyIn(Dist.CLIENT)
public class DustParticle extends TextureSheetParticle {

    public static final ConcurrentHashMap<Long, Integer> AMBIENT_COUNTS = new ConcurrentHashMap<>();
    public static int TOTAL_AMBIENT_COUNT = 0;
    public static net.minecraft.world.phys.Vec3 LOUD_NOISE_POS = null;
    public static long LOUD_NOISE_TICK = 0;
    public static net.minecraft.world.phys.Vec3 LANDING_IMPACT_POS = null;
    public static long LANDING_IMPACT_TICK = 0;
    public static double LANDING_IMPACT_FORCE = 0.0;
    public static double LANDING_IMPACT_RADIUS = 4.0;
    public static BlockPos PENDING_POS = null;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final java.util.Map<net.minecraft.world.level.block.Block, float[]> LIGHT_COLORS = new java.util.HashMap<>();
    private static final java.util.Map<String, float[]> BIOME_COLORS = new java.util.HashMap<>();
    public static final java.util.Map<net.minecraft.world.level.block.Block, String> TRIGGER_BLOCKS = new java.util.HashMap<>();
    public static final java.util.Map<String, float[]> TRIGGER_COLORS = new java.util.HashMap<>();
    public static final java.util.Map<net.minecraft.world.level.block.Block, double[]> HEAT_SOURCE_BLOCKS = new java.util.HashMap<>();
    public static final ConcurrentHashMap<Long, float[]> TINT_CACHE = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<Long, float[]> BIOME_CACHE = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<Long, DustBehavior> BEHAVIOR_CACHE = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<Long, double[]> DRAFT_CACHE = new ConcurrentHashMap<>();

    private final float[] cachedBiomeTint;
    public enum DustBehavior {
        DEFAULT, SPORE, SCULK, SNOWY, ASH, HEAVY
    }
    
    private DustBehavior behavior = DustBehavior.DEFAULT;
    private final float rVar;
    private final float gVar;
    private final float bVar;

    public static boolean colorsLoaded = false;
    private final BlockPos ownerPos;
    private boolean isDetached = false;
    private int bounceCount = 0;
    
    private float rotSpeed; 
    
    private final int tickOffset;
    private float baseAlpha;
    private double caveDraftX = 0;
    private double caveDraftY = 0;
    private double caveDraftZ = 0;

    private double flowVectorX = 0;
    private double flowVectorY = 0;
    private double flowVectorZ = 0;
    private boolean flowInitialized = false;
    private boolean isStuckInWeb = false;

    protected DustParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z);
        this.ownerPos = PENDING_POS;
        this.tickOffset = level.random.nextInt(20);
        float ambientOpacity = LightDustConfig.AMBIENT_DUST_OPACITY.get().floatValue();

        this.rVar = (level.random.nextFloat() - 0.5F) * 0.1F;
        this.gVar = (level.random.nextFloat() - 0.5F) * 0.1F;
        this.bVar = (level.random.nextFloat() - 0.5F) * 0.1F;
        
        BlockPos pos = BlockPos.containing(x, y, z);
        this.cachedBiomeTint = determineEnvironmentalTint(level, pos, this);

        if (this.ownerPos != null) {
            AMBIENT_COUNTS.merge(this.ownerPos.asLong(), 1, Integer::sum);
            TOTAL_AMBIENT_COUNT++;
            
            int blockLight = level.getBrightness(LightLayer.BLOCK, this.ownerPos);
            float[] blockTint = getNearbyTint(level, this.ownerPos);

            Player playerForLight = Minecraft.getInstance().player;
            if (playerForLight != null && LightDustConfig.ENABLE_HANDHELD_LIGHTS.get()) {
                com.lightdust.client.HandheldLightManager.LightData lightData = com.lightdust.client.HandheldLightManager.getHeldLight(playerForLight);
                if (lightData != null) {
                    double distToPlayer = Math.sqrt(playerForLight.distanceToSqr(this.x, this.y, this.z));
                    int handLight = (int) (lightData.radius - distToPlayer);

                    if (handLight > blockLight) {
                        blockLight = handLight;
                        blockTint = lightData.color; 
                    }
                }
            }

            float intensity = Math.max(0f, (blockLight - 6) / 9.0f);
            float baseBrightness = 0.15F + (0.85F * intensity);
            float strength = LightDustColorConfig.TINT_STRENGTH.get().floatValue();
            
            float baseR = (this.cachedBiomeTint != null && this.cachedBiomeTint.length >= 3) ? 
                this.cachedBiomeTint[0] * baseBrightness : baseBrightness;
            float baseG = (this.cachedBiomeTint != null && this.cachedBiomeTint.length >= 3) ? 
                this.cachedBiomeTint[1] * baseBrightness : baseBrightness;
            float baseB = (this.cachedBiomeTint != null && this.cachedBiomeTint.length >= 3) ? 
                this.cachedBiomeTint[2] * baseBrightness : baseBrightness;
            
            if (blockTint != null && strength > 0) {
                this.rCol = Mth.clamp((baseR * (1 - strength) + blockTint[0] * strength) + this.rVar, 0.0F, 1.0F);
                this.gCol = Mth.clamp((baseG * (1 - strength) + blockTint[1] * strength) + this.gVar, 0.0F, 1.0F);
                this.bCol = Mth.clamp((baseB * (1 - strength) + blockTint[2] * strength) + this.bVar, 0.0F, 1.0F);
            } else {
                this.rCol = Mth.clamp(baseR + this.rVar, 0.0F, 1.0F);
                this.gCol = Mth.clamp(baseG + this.gVar, 0.0F, 1.0F);
                this.bCol = Mth.clamp(baseB + this.bVar, 0.0F, 1.0F);
            }
            
            this.baseAlpha = ambientOpacity + (0.28F * intensity);
            this.alpha = 0.0F; 
            this.lifetime = LightDustConfig.PARTICLE_LIFETIME.get() + level.random.nextInt(100);
        } else {
            this.lifetime = LightDustConfig.PARTICLE_LIFETIME.get() / 2;
            float defaultBrightness = 0.8F;
            float baseR = this.cachedBiomeTint != null ? this.cachedBiomeTint[0] * defaultBrightness : defaultBrightness;
            float baseG = this.cachedBiomeTint != null ? this.cachedBiomeTint[1] * defaultBrightness : defaultBrightness;
            float baseB = this.cachedBiomeTint != null ? this.cachedBiomeTint[2] * defaultBrightness : defaultBrightness;
            
            this.rCol = Mth.clamp(baseR + this.rVar, 0.0F, 1.0F);
            this.gCol = Mth.clamp(baseG + this.gVar, 0.0F, 1.0F);
            this.bCol = Mth.clamp(baseB + this.bVar, 0.0F, 1.0F);
            
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
        
        this.hasPhysics = true;
        this.roll = level.random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;
        this.rotSpeed = (level.random.nextFloat() - 0.5F) * 0.1F; 

        this.pickSprite(sprites);
        this.setSize(0.01F, 0.01F);
    }

    private int probeSpace(ClientLevel level, BlockPos start, net.minecraft.core.Direction dir, int maxDepth) {
        BlockPos.MutableBlockPos mut = start.mutable();
        for (int i = 1; i <= maxDepth; i++) {
            mut.move(dir);
            net.minecraft.world.level.block.state.BlockState state = level.getBlockState(mut);
            if (!state.isAir() && !state.getCollisionShape(level, mut).isEmpty()) {
                return i;
            }
        }
        return maxDepth;
    }

    @Override
    public void remove() {
        if (!this.removed && this.ownerPos != null && !this.isDetached) {
            long key = this.ownerPos.asLong();
            AMBIENT_COUNTS.computeIfPresent(key, (k, v) -> v <= 1 ? null : v - 1);
            if (TOTAL_AMBIENT_COUNT > 0) {
                TOTAL_AMBIENT_COUNT--;
            }
        }
        super.remove();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        BlockPos blockpos = BlockPos.containing(this.x, this.y, this.z);
        int baseLight = this.level.hasChunkAt(blockpos) ? LevelRenderer.getLightColor(this.level, blockpos) : 0;

        if (blockpos.getY() < 60 && !this.level.canSeeSky(blockpos)) {
            int blockLight = this.level.getBrightness(LightLayer.BLOCK, blockpos);
            if (blockLight > 0) {
                Player player = Minecraft.getInstance().player;
                if (player != null) {
                    double dist = Math.sqrt(player.distanceToSqr(this.x, this.y, this.z));
                    double glowRadius = 6.0; 
                    
                    if (dist <= glowRadius) {
                        int currentBlockLight = baseLight & 255;
                        int currentSkyLight = baseLight >> 16 & 255;
                        double glowFactor = 1.0 - (dist / glowRadius);
                        int slightGlow = (int) (10 * glowFactor);

                        int finalBlockLight = Math.max(currentBlockLight, slightGlow);

                        return (baseLight & 0xFF000000) |
                        (currentSkyLight << 16) | finalBlockLight;
                    }
                }
            }
        }
        return baseLight;
    }

    @Override
    public void tick() {
        if (this.isStuckInWeb) {
            this.xo = this.x;
            this.yo = this.y;
            this.zo = this.z;

            BlockPos currentPos = BlockPos.containing(this.x, this.y, this.z);
            if (!level.getBlockState(currentPos).is(net.minecraft.world.level.block.Blocks.COBWEB)) {
                this.isStuckInWeb = false;
                this.hasPhysics = true;
            } else {
                if (this.age++ >= this.lifetime) {
                    this.remove();
                }
                return; // Skip all wind and movement math if still stuck
            }
        }

        super.tick();
        if (!this.isDetached && this.ownerPos != null && (this.age + this.tickOffset) % 20 == 0) {
            if (this.ownerPos.distToCenterSqr(this.x, this.y, this.z) > 25.0) { 
                long key = this.ownerPos.asLong();
                AMBIENT_COUNTS.computeIfPresent(key, (k, v) -> v <= 1 ? null : v - 1);
                if (TOTAL_AMBIENT_COUNT > 0) TOTAL_AMBIENT_COUNT--;
                this.isDetached = true;
                this.lifetime = Math.min(this.lifetime, this.age + 60); 
            }
        }

        Player player = Minecraft.getInstance().player;
        float seed = (float)(this.x * 10.0 + this.y * 10.0 + this.z * 10.0) + (this.tickOffset * 100.0F) + (this.rVar * 5000.0F);
        if (this.onGround && com.lightdust.config.LightDustExperimentalConfig.ENABLE_DUST_SETTLING.get()) {
            this.xd = 0.0;
            this.zd = 0.0;
            if (this.yd < 0.0) {
                this.yd = 0.0;
            }
            this.rotSpeed = 0.0F;
        }

        if (this.age < 20) {
            this.alpha = this.baseAlpha * (this.age / 20.0F);
        } else if (this.age > this.lifetime - 20) {
            this.alpha = this.baseAlpha * ((this.lifetime - this.age) / 20.0F);
        } else {
            this.alpha = this.baseAlpha;
        }

        float targetAlpha = this.alpha;
        if (this.behavior == DustBehavior.SPORE) {
            if (this.tickOffset % 3 == 0) {
                float pulse = Mth.sin((this.age * 0.05F) + seed);
                if (pulse > 0) targetAlpha *= (1.0F + pulse * 0.6F);
            }
        } else if (this.behavior == DustBehavior.SNOWY) {
            if (this.tickOffset % 4 == 0) {
                int cycle = (this.age + this.tickOffset * 11) % 80;
                if (cycle < 15) {
                    float flash = Mth.sin((cycle / 15.0F) * (float)Math.PI);
                    targetAlpha *= (1.0F + flash * 1.5F); 
                }
            }
        } else if (this.behavior == DustBehavior.SCULK) {
            if (this.tickOffset % 2 == 0) {
                targetAlpha *= (1.0F + Mth.sin((this.age * 0.04F) + seed) * 0.4F);
            }
        }

        if (this.tickOffset % 4 == 0) {
            int glintCycle = (this.age + this.tickOffset * 17) % 160;
            if (glintCycle < 24) {
                float glintPhase = Mth.sin((glintCycle / 24.0F) * (float)Math.PI);
                targetAlpha *= (1.0F + glintPhase * 0.8F);
            }
        }
        
        this.alpha = Mth.clamp(targetAlpha, 0.0F, 1.0F);
        BlockPos currentPos = BlockPos.containing(this.x, this.y, this.z);

        if ((this.age + tickOffset) % 20 == 0) {

            if (level.getBlockState(currentPos).is(net.minecraft.world.level.block.Blocks.COBWEB)) {
                this.isStuckInWeb = true;
                this.xd = 0.0; 
                this.yd = 0.0; 
                this.zd = 0.0;
                this.hasPhysics = false;
                this.lifetime += 100; 
                return;
            }

            if (level.getFluidState(currentPos).is(FluidTags.WATER)) {
                this.remove();
                return;
            }
            
            boolean canSeeSky = level.canSeeSky(currentPos);
            if (canSeeSky) {
                if (level.isThundering() && LightDustConfig.DISABLE_DURING_THUNDER.get()) {
                    this.remove();
                    return;
                } else if (level.isRaining() && !level.isThundering() && LightDustConfig.DISABLE_DURING_RAIN.get()) {
                    this.remove();
                    return;
                }
            }

            int blockLight = level.getBrightness(LightLayer.BLOCK, currentPos);
            float[] blockTint = null;
            if (this.ownerPos != null) {
                blockTint = getNearbyTint(level, this.ownerPos);
            }

            Player playerForLight = Minecraft.getInstance().player;
            if (playerForLight != null && LightDustConfig.ENABLE_HANDHELD_LIGHTS.get()) {
                com.lightdust.client.HandheldLightManager.LightData lightData = com.lightdust.client.HandheldLightManager.getHeldLight(playerForLight);
                if (lightData != null) {
                    double distToPlayer = Math.sqrt(playerForLight.distanceToSqr(this.x, this.y, this.z));
                    int handLight = (int) (lightData.radius - distToPlayer);

                    if (handLight > blockLight) {
                        blockLight = handLight;
                        if (this.ownerPos != null) {
                            blockTint = lightData.color;
                        }
                    }
                }
            }

            boolean isDarkCave = currentPos.getY() < 60 && !canSeeSky;
            if (blockLight < LightDustConfig.MIN_BLOCK_LIGHT.get() && !isDarkCave) {
                this.remove();
                return;
            }

            if (this.ownerPos != null) {
                float intensity = Math.max(0f, (blockLight - 6) / 9.0f);
                float baseBrightness = 0.15F + (0.85F * intensity);

                float strength = LightDustColorConfig.TINT_STRENGTH.get().floatValue();
                float baseR = this.cachedBiomeTint != null ?
                this.cachedBiomeTint[0] * baseBrightness : baseBrightness;
                float baseG = this.cachedBiomeTint != null ? this.cachedBiomeTint[1] * baseBrightness : baseBrightness;
                float baseB = this.cachedBiomeTint != null ? this.cachedBiomeTint[2] * baseBrightness : baseBrightness;
                if (blockTint != null && strength > 0) {
                    this.rCol = Mth.clamp((baseR * (1 - strength) + blockTint[0] * strength) + this.rVar, 0.0F, 1.0F);
                    this.gCol = Mth.clamp((baseG * (1 - strength) + blockTint[1] * strength) + this.gVar, 0.0F, 1.0F);
                    this.bCol = Mth.clamp((baseB * (1 - strength) + blockTint[2] * strength) + this.bVar, 0.0F, 1.0F);
                } else {
                    this.rCol = Mth.clamp(baseR + this.rVar, 0.0F, 1.0F);
                    this.gCol = Mth.clamp(baseG + this.gVar, 0.0F, 1.0F);
                    this.bCol = Mth.clamp(baseB + this.bVar, 0.0F, 1.0F);
                }
                
                float ambientOpacity = LightDustConfig.AMBIENT_DUST_OPACITY.get().floatValue();
                this.baseAlpha = ambientOpacity + (0.28F * intensity);
            }

            if (!this.flowInitialized) {
                this.flowVectorX = (level.random.nextDouble() - 0.5);
                this.flowVectorY = (level.random.nextDouble() - 0.5) * 0.2; 
                this.flowVectorZ = (level.random.nextDouble() - 0.5);
                double len = Math.sqrt(this.flowVectorX * this.flowVectorX + this.flowVectorY * this.flowVectorY + this.flowVectorZ * this.flowVectorZ);
                if (len > 0) { this.flowVectorX /= len; this.flowVectorY /= len; this.flowVectorZ /= len;
                }
                this.flowInitialized = true;
            }

            if (isDarkCave && com.lightdust.config.LightDustExperimentalConfig.ENABLE_CAVE_DRAFTS.get()) {
                long posKey = currentPos.asLong();
                double[] envData;

                if (DRAFT_CACHE.containsKey(posKey)) {
                    envData = DRAFT_CACHE.get(posKey);
                } else {
                    int maxProbe = 6;
                    int spacePX = probeSpace(level, currentPos, net.minecraft.core.Direction.EAST, maxProbe);
                    int spaceNX = probeSpace(level, currentPos, net.minecraft.core.Direction.WEST, maxProbe);
                    int spacePY = probeSpace(level, currentPos, net.minecraft.core.Direction.UP, maxProbe);
                    int spaceNY = probeSpace(level, currentPos, net.minecraft.core.Direction.DOWN, maxProbe);
                    int spacePZ = probeSpace(level, currentPos, net.minecraft.core.Direction.SOUTH, maxProbe);
                    int spaceNZ = probeSpace(level, currentPos, net.minecraft.core.Direction.NORTH, maxProbe);
                    double pullX = (spacePX - spaceNX) / (double) maxProbe;
                    double pullY = (spacePY - spaceNY) / (double) maxProbe;
                    double pullZ = (spacePZ - spaceNZ) / (double) maxProbe;

                    double repelX = (1.0 / Math.max(1, spaceNX)) - (1.0 / Math.max(1, spacePX));
                    double repelY = (1.0 / Math.max(1, spaceNY)) - (1.0 / Math.max(1, spacePY));
                    double repelZ = (1.0 / Math.max(1, spaceNZ)) - (1.0 / Math.max(1, spacePZ));
                    envData = new double[]{pullX, pullY, pullZ, repelX, repelY, repelZ};
                    DRAFT_CACHE.put(posKey, envData);
                }

                float time = level.getGameTime() * 0.002f;
                double macroX = Mth.sin(currentPos.getX() * 0.03f + time);
                double macroZ = Mth.cos(currentPos.getZ() * 0.03f + time);
                this.flowVectorX = (this.flowVectorX * 0.7) + (macroX * 0.2) + (envData[0] * 1.5) + envData[3];
                this.flowVectorY = (this.flowVectorY * 0.7) + (envData[1] * 0.5) + envData[4];
                this.flowVectorZ = (this.flowVectorZ * 0.7) + (macroZ * 0.2) + (envData[2] * 1.5) + envData[5];
                double len = Math.sqrt(this.flowVectorX * this.flowVectorX + this.flowVectorY * this.flowVectorY + this.flowVectorZ * this.flowVectorZ);
                if (len > 0) {
                    this.flowVectorX /= len;
                    this.flowVectorY /= len;
                    this.flowVectorZ /= len;
                }

                double baseStrength = com.lightdust.config.LightDustExperimentalConfig.CAVE_DRAFT_STRENGTH.get();
                double draftStrength = baseStrength * 0.0001;

                this.caveDraftX = this.flowVectorX * draftStrength;
                this.caveDraftY = this.flowVectorY * draftStrength;
                this.caveDraftZ = this.flowVectorZ * draftStrength;
            } else {
                this.caveDraftX = 0;
                this.caveDraftY = 0;
                this.caveDraftZ = 0;
            }

            long time = level.getDayTime() % 24000;
            boolean isDay = time < 13000 || time > 23000;
            if (isDay && !isDarkCave && !level.isRaining()) {
                int skyLight = level.getBrightness(LightLayer.SKY, currentPos);
                int diffThreshold = LightDustConfig.DAYTIME_LIGHT_DIFF.get();

                if ((blockLight - skyLight) <= diffThreshold) {
                    this.remove();
                    return;
                }
            }

            if (player != null) {
                double maxDist = LightDustConfig.AMBIENT_HARD_CAP.get();
                if (player.distanceToSqr(this.x, this.y, this.z) > maxDist * maxDist) {
                    this.remove();
                    return;
                }
            }
        }

        if ((this.age + tickOffset) % 10 == 0) {
            double appliedUpdraftSpeed = 0;
            BlockPos.MutableBlockPos mutableBelow = new BlockPos.MutableBlockPos();
            for (int i = 1; i <= 7; i++) {
                mutableBelow.set(this.x, this.y - i, this.z);
                net.minecraft.world.level.block.state.BlockState stateBelow = level.getBlockState(mutableBelow);
                double[] heatData = HEAT_SOURCE_BLOCKS.get(stateBelow.getBlock());
                
                if (stateBelow.is(net.minecraft.world.level.block.Blocks.FIRE) || stateBelow.is(net.minecraft.world.level.block.Blocks.SOUL_FIRE)) {
                    boolean isSideFire = false;
                    if (stateBelow.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.NORTH)) {
                        isSideFire = stateBelow.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.NORTH) ||
                                     stateBelow.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.SOUTH) ||
                                     stateBelow.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.EAST) ||
                                     stateBelow.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WEST) ||
                                     stateBelow.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.UP);
                    }

                    if (heatData == null) {
                        heatData = new double[]{0.035, 4.0, 0.6};
                    }

                    if (isSideFire) {
                        heatData = new double[]{heatData[0] * 0.4, Math.max(1, heatData[1] * 0.5), heatData[2] * 0.6};
                    }
                }

                if (heatData == null && stateBelow.getFluidState().is(FluidTags.LAVA)) {
                    heatData = HEAT_SOURCE_BLOCKS.get(net.minecraft.world.level.block.Blocks.LAVA);
                }
                if (heatData != null) {
                    double reach = heatData[1];
                    double radius = heatData[2];
                    if (i <= reach) {
                        double dX = this.x - (mutableBelow.getX() + 0.5);
                        double dZ = this.z - (mutableBelow.getZ() + 0.5);
                        if ((dX * dX + dZ * dZ) <= (radius * radius)) {
                            appliedUpdraftSpeed = heatData[0];
                            break;
                        }
                    }
                }
                if (stateBelow.isSolidRender(level, mutableBelow)) break;
            }
            
            if (appliedUpdraftSpeed > 0) {
                this.yd += appliedUpdraftSpeed + (level.random.nextDouble() * 0.01);
                this.xd += (level.random.nextDouble() - 0.5) * 0.02;
                this.zd += (level.random.nextDouble() - 0.5) * 0.02;
                this.onGround = false;
            }
        }

        if (com.lightdust.config.LightDustExperimentalConfig.ENABLE_ENTITY_DISTURBANCE.get() && (this.age + tickOffset) % 6 == 0) {
            double pushConfig = com.lightdust.config.LightDustExperimentalConfig.ENTITY_PUSH_STRENGTH.get();
            for (com.lightdust.event.AmbientDustHandler.MovingEntityData data : com.lightdust.event.AmbientDustHandler.ACTIVE_MOVING_ENTITIES) {
                double dx = this.x - data.x;
                double dy = this.y - data.y;
                double dz = this.z - data.z;
                double distSqr = dx*dx + dy*dy + dz*dz;
                if (distSqr < 2.25) { 
                    double dist = Math.sqrt(distSqr);
                    if (dist < 0.1) dist = 0.1;
                    
                    this.xd += (dx / dist) * data.speed * pushConfig;
                    this.yd += (dy / dist) * data.speed * pushConfig;
                    this.zd += (dz / dist) * data.speed * pushConfig;
                    this.onGround = false; 
                }
            }
        }

        if (LOUD_NOISE_POS != null) {
            long ageInTicks = level.getGameTime() - LOUD_NOISE_TICK;
            if (ageInTicks < 30) {
                double dx = this.x - LOUD_NOISE_POS.x();
                double dy = this.y - LOUD_NOISE_POS.y();
                double dz = this.z - LOUD_NOISE_POS.z();
                double distSqr = dx * dx + dy * dy + dz * dz;
                if (distSqr > 0.1 && distSqr < 144) {
                    double dist = Math.sqrt(distSqr);
                    double wavefrontDist = ageInTicks; 

                    if (Math.abs(dist - wavefrontDist) < 2.0) {

                        double forceFalloff = Math.max(0.0, 1.0 - (dist / 12.0));
                        double pushForce = 0.1* forceFalloff; 
                        
                        this.xd += (dx / dist) * pushForce;
                        this.yd += (dy / dist) * pushForce;
                        this.zd += (dz / dist) * pushForce;
                        
                        this.onGround = false;
                        this.hasPhysics = true;
                    }
                }
            }
        }

        if (LANDING_IMPACT_POS != null && level.getGameTime() - LANDING_IMPACT_TICK < 15) {
            double maxRadius = LANDING_IMPACT_RADIUS;
            double distSqr = LANDING_IMPACT_POS.distanceToSqr(this.x, this.y, this.z);
            if (distSqr < (maxRadius * maxRadius)) { 
                double dist = Math.sqrt(distSqr);
                if (dist < 0.1) dist = 0.1;
                double push = (maxRadius - dist) * LANDING_IMPACT_FORCE;
                if (push > 0) {
                    this.xd += ((this.x - LANDING_IMPACT_POS.x) / dist) * push + (level.random.nextDouble() - 0.5) * 0.01;
                    this.yd += ((this.y - LANDING_IMPACT_POS.y) / dist) * push * 0.5 + (level.random.nextDouble() - 0.5) * 0.01;
                    this.zd += ((this.z - LANDING_IMPACT_POS.z) / dist) * push + (level.random.nextDouble() - 0.5) * 0.01;
                    this.onGround = false;
                }
            }
        }

        int skyLightLevel = level.getBrightness(LightLayer.SKY, currentPos);
        double windExposure = Math.max(0.0, (skyLightLevel - 8.0) / 7.0); 

        int blockedDirs = 0;
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            for (int i = 1; i <= 6; i++) {
                BlockPos checkPos = currentPos.relative(dir, i);
                if (!level.getBlockState(checkPos).getCollisionShape(level, checkPos).isEmpty()) {
                    blockedDirs++;
                    break;
                }
            }
        }

        if (blockedDirs >= 3) {
            windExposure = 0.0;
        } else if (skyLightLevel < 8 && currentPos.getY() >= 50) {
            windExposure = 0.5;
        }

        if (!this.onGround) {
            this.oRoll = this.roll;
            this.roll += this.rotSpeed;
            
            float gameTimeF = (float) level.getGameTime();
            float time = (float)(this.age * 0.05F);
            double sinX = Mth.sin(time * 0.8f + seed);
            double cosZ = Mth.cos(time * 1.1f + seed);
            double driftDown = 0.000015 + (level.random.nextDouble() * 0.00003);
            double microTurbulence = (level.random.nextDouble() - 0.5) * 0.0001;
            if (this.behavior == DustBehavior.SPORE) {
                driftDown = 0.000005 + (level.random.nextDouble() * 0.00001);
                microTurbulence *= 1.5; 
                sinX *= 1.2; cosZ *= 1.2;
            } else if (this.behavior == DustBehavior.ASH || this.behavior == DustBehavior.HEAVY) {
                driftDown = 0.00006 + (level.random.nextDouble() * 0.00005);
                microTurbulence *= 0.6; 
                sinX *= 0.4; cosZ *= 0.4;
            } else if (this.behavior == DustBehavior.SCULK) {
                driftDown = 0.00001 + (level.random.nextDouble() * 0.00001);
                microTurbulence *= 0.2; 
                if (level.random.nextInt(50) == 0) {
                    this.xd += (level.random.nextDouble() - 0.5) * 0.005;
                    this.zd += (level.random.nextDouble() - 0.5) * 0.005;
                }
            }

            double altitudeMultiplier = Math.max(1.0, 1.0 + ((this.y - 64.0) / 128.0));
            double windX = 0;
            double windZ = 0;
            
            if (windExposure > 0) {
                 double windSpeedModifier = LightDustConfig.WIND_SPEED_CLEAR.get();
                 if (level.isThundering()) windSpeedModifier = LightDustConfig.WIND_SPEED_THUNDER.get();
                 else if (level.isRaining()) windSpeedModifier = LightDustConfig.WIND_SPEED_RAIN.get();
                 if (com.lightdust.config.LightDustExperimentalConfig.ENABLE_ADVANCED_WIND_MATH.get()) {
                     float timeDilation = gameTimeF * 0.0105f + Mth.sin(gameTimeF * 0.004f) * 1.2f;
                     double macroX = Mth.sin(timeDilation);
                     double macroZ = Mth.cos(timeDilation);

                     double gridX = this.x * 0.05;
                     double gridZ = this.z * 0.05;
                     double gustTime = gameTimeF * 0.015;

                     double spatialX = Mth.sin((float)(gridX + gustTime)) * Mth.cos((float)(gridZ - gustTime * 0.8f));
                     double spatialZ = Mth.cos((float)(gridZ + gustTime)) * Mth.sin((float)(gridX + gustTime * 0.9f));
                     double finalWindX = (macroX * 0.6 + spatialX * 0.4) * 0.04;
                     double finalWindZ = (macroZ * 0.6 + spatialZ * 0.4) * 0.04;
                     windX = finalWindX * altitudeMultiplier * windSpeedModifier * windExposure;
                     windZ = finalWindZ * altitudeMultiplier * windSpeedModifier * windExposure;
                 } else {
                     double simpleWindX = Mth.sin(gameTimeF * 0.002f) * 0.02;
                     double simpleWindZ = Mth.cos(gameTimeF * 0.002f) * 0.02;
                     windX = simpleWindX * altitudeMultiplier * windSpeedModifier * windExposure;
                     windZ = simpleWindZ * altitudeMultiplier * windSpeedModifier * windExposure;
                 }
            }

            double uniqueDriftStrength = 0.00008 + (this.gVar * 0.0004);
            double swayX = net.minecraft.util.Mth.sin((float)(this.age * 0.02 + seed)) * 0.000015;
            double swayY = net.minecraft.util.Mth.cos((float)(this.age * 0.015 + seed * 1.5)) * 0.00001;
            double swayZ = net.minecraft.util.Mth.sin((float)(this.age * 0.025 + seed * 0.8)) * 0.000015;
            this.xd += sinX * uniqueDriftStrength + swayX + windX + this.caveDraftX;
            this.yd += swayY - driftDown + this.caveDraftY;
            this.zd += cosZ * uniqueDriftStrength + swayZ + windZ + this.caveDraftZ;
        }

        double jitterX = (level.random.nextDouble() - 0.5) * 0.015;
        double jitterY = (level.random.nextDouble() - 0.5) * 0.015;
        double jitterZ = (level.random.nextDouble() - 0.5) * 0.015;
        if (player != null && player.distanceToSqr(this.x, this.y, this.z) < LightDustConfig.PLAYER_INTERACT_RADIUS.get()) {
            double range = 2.0;
            double dx = this.x - player.getX();
            double dy = this.y - (player.getY() + 1.0); 
            double dz = this.z - player.getZ();
            double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
            if (dist < 0.01) dist = 0.01;
            double nx = dx / dist; double ny = dy / dist; double nz = dz / dist;
            Vec3 pVel = player.getDeltaMovement();
            double hSpeedSqr = pVel.x * pVel.x + pVel.z * pVel.z;
            if (player.swingTime > 0) {
                Vec3 look = player.getLookAngle();
                if ((nx * look.x) + (ny * look.y) + (nz * look.z) > 0.5) { 
                    double slashForce = 0.002;
                    this.xd += look.x * slashForce + (nx * 0.005) + jitterX;
                    this.yd += look.y * slashForce + (ny * 0.005) + jitterY;
                    this.zd += look.z * slashForce + (nz * 0.005) + jitterZ;
                    this.onGround = false;
                }
            }

            if (player.isUsingItem() && player.getUseItem().getItem() instanceof ShieldItem) {
                Vec3 look = player.getLookAngle();
                if ((nx * look.x) + (ny * look.y) + (nz * look.z) > 0.3) {
                    double shieldPush = 0.04 / dist;
                    this.xd += (nx * shieldPush) + jitterX;
                    this.yd += (ny * shieldPush) + jitterY;
                    this.zd += (nz * shieldPush) + jitterZ;
                    this.onGround = false;
                }
            }

            if (hSpeedSqr > 0.0001) {
                double horizontalSpeed = Math.sqrt(hSpeedSqr);
                double proximityFactor = (range - dist) / range;
                double pushStrength = horizontalSpeed * proximityFactor * 0.05;
                this.xd += (nx * pushStrength) + jitterX;
                this.yd += (ny * pushStrength) + jitterY;
                this.zd += (nz * pushStrength) + jitterZ;
                this.onGround = false;
            }
        }

        HitResult hit = Minecraft.getInstance().hitResult;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos breakPos = ((BlockHitResult)hit).getBlockPos();
            if (player != null && player.swingTime > 0 && breakPos.distToCenterSqr(this.x, this.y, this.z) < 4.0) {
                
                double pullX = (breakPos.getX() + 0.5) - this.x;
                double pullY = (breakPos.getY() + 0.5) - this.y;
                double pullZ = (breakPos.getZ() + 0.5) - this.z;
                double distSqrBreak = pullX * pullX + pullY * pullY + pullZ * pullZ;
                double vacuumRadius = LightDustConfig.BREAK_VACUUM_RADIUS.get();
                if (level.getBlockState(breakPos).isAir() && distSqrBreak < (vacuumRadius * vacuumRadius)) {
                    double distBreak = Math.sqrt(distSqrBreak);
                    if (distBreak < 0.1) distBreak = 0.1;
                    double force = LightDustConfig.BREAK_VACUUM_FORCE.get() * 0.001;
                    this.xd += (pullX / distBreak) * force + jitterX;
                    this.yd += (pullY / distBreak) * force + jitterY;
                    this.zd += (pullZ / distBreak) * force + jitterZ;
                    this.onGround = false;
                }
            }
        }

        if (this.onGround) {
            this.xd *= 0.5;
            this.zd *= 0.5;
            if (windExposure > 0) {
                this.age += 4;
            }
        } else {
            this.xd *= 0.94;
            this.yd *= 0.94;
            this.zd *= 0.94;
        }

        double oldXd = this.xd;
        double oldZd = this.zd;
        
        this.move(this.xd, this.yd, this.zd);

        boolean hitX = Math.abs(oldXd) > 0.001 && Math.abs(this.xd) < 0.0001;
        boolean hitZ = Math.abs(oldZd) > 0.001 && Math.abs(this.zd) < 0.0001;
        if (hitX || hitZ) {
            if (com.lightdust.config.LightDustExperimentalConfig.ENABLE_WIND_DEFLECTION.get() && this.bounceCount < 2) {
                this.bounceCount++;
                double kineticEnergy = Math.sqrt(oldXd * oldXd + oldZd * oldZd) * 0.7;
                if (hitX) {
                    double directionZ = oldZd != 0 ?
                    Math.signum(oldZd) : (level.random.nextBoolean() ? 1.0 : -1.0);
                    this.zd += directionZ * kineticEnergy;
                    this.xd += -oldXd * 0.3;
                }
                if (hitZ) {
                    double directionX = oldXd != 0 ?
                    Math.signum(oldXd) : (level.random.nextBoolean() ? 1.0 : -1.0);
                    this.xd += directionX * kineticEnergy;
                    this.zd += -oldZd * 0.3;
                }
                
                this.yd += (level.random.nextDouble() - 0.2) * kineticEnergy * 0.8;
                this.rotSpeed += (level.random.nextFloat() - 0.5f) * 0.15f;
                if (windExposure > 0) this.age += 1;
            } else {
                this.yd += 0.015 * windExposure;
                this.age += 2; 
            }
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
            return new DustParticle(level, x, y, z, dx, dy, dz, sprites);
        }
    }

    public static float[] determineEnvironmentalTint(ClientLevel level, BlockPos pos, DustParticle particleInstance) {
        if (!colorsLoaded) reloadColors();
        long posKey = pos.asLong();

        // Check if we already did the math for this block recently
        if (BIOME_CACHE.containsKey(posKey) && BEHAVIOR_CACHE.containsKey(posKey)) {
            if (particleInstance != null) particleInstance.behavior = BEHAVIOR_CACHE.get(posKey);
            float[] cached = BIOME_CACHE.get(posKey);
            return cached.length == 0 ? null : cached;
        }

        String dominantSource = level.getBiome(pos).unwrapKey().map(key -> key.location().toString()).orElse("minecraft:plains");
        float[] actualBiomeTint = BIOME_COLORS.get(dominantSource);
        DustBehavior behavior = DustBehavior.DEFAULT;

        if (pos.getY() < 60) {
            java.util.Map<String, Integer> colorCounts = new java.util.HashMap<>();
            for (BlockPos p : BlockPos.betweenClosed(pos.offset(-3, -3, -3), pos.offset(3, 3, 3))) {
                net.minecraft.world.level.block.state.BlockState state = level.getBlockState(p);
                if (state.isAir()) continue;
                
                String hexGroup = TRIGGER_BLOCKS.get(state.getBlock());
                if (hexGroup != null) {
                    colorCounts.put(hexGroup, colorCounts.getOrDefault(hexGroup, 0) + 1);
                }
            }
            
            String dominantHex = null;
            int maxCount = 0;
            for (java.util.Map.Entry<String, Integer> entry : colorCounts.entrySet()) {
                if (entry.getValue() > maxCount) {
                    maxCount = entry.getValue();
                    dominantHex = entry.getKey();
                }
            }
            
            if (maxCount > 3 && dominantHex != null) {
                dominantSource = dominantHex;
                actualBiomeTint = TRIGGER_COLORS.get(dominantHex);
            }
        }
        
        if (dominantSource.contains("lush") || dominantSource.contains("mushroom") || dominantSource.contains("swamp") || dominantSource.equals("#8FCE00")) {
            behavior = DustBehavior.SPORE;
        } else if (dominantSource.contains("sculk") || dominantSource.contains("deep_dark") || dominantSource.equals("#006666")) {
            behavior = DustBehavior.SCULK;
        } else if (dominantSource.contains("snow") || dominantSource.contains("ice") || dominantSource.contains("frozen")) {
            behavior = DustBehavior.SNOWY;
        } else if (dominantSource.contains("basalt") || dominantSource.contains("nether") || dominantSource.equals("#4A4A52")) {
            behavior = DustBehavior.ASH;
        } else if (dominantSource.contains("desert") || dominantSource.contains("badlands") || dominantSource.contains("dripstone") || dominantSource.equals("#8B6B4A")) {
            behavior = DustBehavior.HEAVY;
        }
        
        if (particleInstance != null) particleInstance.behavior = behavior;
        // Save to cache so other particles spawning here don't have to calculate this
        BIOME_CACHE.put(posKey, actualBiomeTint != null ? actualBiomeTint : new float[0]);
        BEHAVIOR_CACHE.put(posKey, behavior);
        
        return actualBiomeTint;
    }

    public static float[] getBiomeBaseTint(ClientLevel level, BlockPos pos) {
        if (!colorsLoaded) reloadColors();
        return level.getBiome(pos).unwrapKey()
                .map(key -> key.location().toString())
                .map(BIOME_COLORS::get)
                .orElse(null);
    }

    public static void reloadColors() {
        LIGHT_COLORS.clear();
        for (String entry : LightDustColorConfig.CUSTOM_TINTS.get()) {
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
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("[Light Dust] Failed to parse custom tint config entry '{}': {}", entry, e.getMessage());
            }
        }
        
        BIOME_COLORS.clear();
        for (String entry : LightDustColorConfig.CUSTOM_BIOME_TINTS.get()) {
            try {
                String[] parts = entry.split("=");
                if (parts.length == 2 && parts[1].contains("#")) {
                    String biomeId = parts[0].trim();
                    String hex = parts[1].substring(parts[1].indexOf("#") + 1).trim();
                    if (hex.length() == 6) {
                        int r = Integer.parseInt(hex.substring(0, 2), 16);
                        int g = Integer.parseInt(hex.substring(2, 4), 16);
                        int b = Integer.parseInt(hex.substring(4, 6), 16);
                        BIOME_COLORS.put(biomeId, new float[]{r / 255f, g / 255f, b / 255f});
                    }
                }
            } catch (Exception e) {
                LOGGER.error("[Light Dust] Failed to parse biome tint '{}': {}", entry, e.getMessage());
            }
        }

        TRIGGER_BLOCKS.clear();
        TRIGGER_COLORS.clear();
        for (String entry : LightDustColorConfig.CAVE_BIOME_TRIGGERS.get()) {
            try {
                String[] parts = entry.split("=");
                if (parts.length == 2 && parts[1].contains("#")) {
                    net.minecraft.resources.ResourceLocation rl = new net.minecraft.resources.ResourceLocation(parts[0].trim());
                    net.minecraft.world.level.block.Block block = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(rl);
                    
                    if (block != null && block != net.minecraft.world.level.block.Blocks.AIR) {
                        String hex = parts[1].trim();
                        TRIGGER_BLOCKS.put(block, hex);
                        
                        if (!TRIGGER_COLORS.containsKey(hex)) {
                            String cleanHex = hex.substring(hex.indexOf("#") + 1);
                            if (cleanHex.length() == 6) {
                                int r = Integer.parseInt(cleanHex.substring(0, 2), 16);
                                int g = Integer.parseInt(cleanHex.substring(2, 4), 16);
                                int b = Integer.parseInt(cleanHex.substring(4, 6), 16);
                                TRIGGER_COLORS.put(hex, new float[]{r / 255f, g / 255f, b / 255f});
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("[Light Dust] Failed to parse cave trigger '{}': {}", entry, e.getMessage());
            }
        }

        HEAT_SOURCE_BLOCKS.clear();
        if (LightDustConfig.SPEC.isLoaded()) {
            for (String entry : LightDustConfig.HEAT_BLOCKS.get()) {
                try {
                    String[] parts = entry.split("=");
                    if (parts.length == 2 && parts[1].contains(",")) {
                        String[] data = parts[1].split(",");
                        if (data.length == 3) {
                            double speed = Double.parseDouble(data[0].trim());
                            double reach = Double.parseDouble(data[1].trim());
                            double radius = Double.parseDouble(data[2].trim());

                            net.minecraft.resources.ResourceLocation rl = new net.minecraft.resources.ResourceLocation(parts[0].trim());
                            net.minecraft.world.level.block.Block block = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(rl);
                            if (block != null && block != net.minecraft.world.level.block.Blocks.AIR) {
                                HEAT_SOURCE_BLOCKS.put(block, new double[]{speed, reach, radius});
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("[Light Dust] Failed to parse heat block '{}': {}", entry, e.getMessage());
                }
            }
        }
        
        colorsLoaded = true;
    }

    public static float[] getNearbyTint(ClientLevel level, BlockPos pos) {
        if (!colorsLoaded) reloadColors();
        long posKey = pos.asLong();

        if (TINT_CACHE.containsKey(posKey)) {
            float[] cached = TINT_CACHE.get(posKey);
            return cached.length == 0 ? null : cached;
        }

        float[] foundColor = null;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2))) {
            net.minecraft.world.level.block.state.BlockState state = level.getBlockState(p);
            if (state.getLightEmission(level, p) > 0) {
                float[] color = LIGHT_COLORS.get(state.getBlock());
                if (color != null) {
                    foundColor = color;
                    break;
                }
            }
        }
        
        TINT_CACHE.put(posKey, foundColor != null ? foundColor : new float[0]);
        return foundColor;
    }
}