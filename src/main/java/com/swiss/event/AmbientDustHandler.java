package com.lightdust.event;

import com.lightdust.LightDust;
import com.lightdust.client.particle.DustParticle;
import com.lightdust.config.LightDustConfig;
import com.lightdust.init.ParticleInit;
import com.lightdust.config.LightDustExperimentalConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LightDust.MODID, value = Dist.CLIENT)
public class AmbientDustHandler {

    private static float lastFallDistance = 0.0f;
    private static BlockPos lastTargetPos = null;

    public static class MovingEntityData {
        public double x, y, z, speed;
        public MovingEntityData(double x, double y, double z, double speed) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.speed = speed;
        }
    }
    public static final java.util.List<MovingEntityData> ACTIVE_MOVING_ENTITIES = new java.util.ArrayList<>();

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused() || mc.player == null || mc.level == null) {
            return;
        }

        if (!LightDustConfig.SPEC.isLoaded()) {
            return;
        }
        Player player = mc.player;
        Level level = mc.level;

        ACTIVE_MOVING_ENTITIES.clear();
        if (LightDustExperimentalConfig.ENABLE_ENTITY_DISTURBANCE.get()) {
            double scanRadius = LightDustConfig.AMBIENT_RADIUS.get(); 
            net.minecraft.world.phys.AABB playerBounds = player.getBoundingBox().inflate(scanRadius);
            java.util.List<net.minecraft.world.entity.Entity> entities = level.getEntities((net.minecraft.world.entity.Entity) null, playerBounds,
                    e -> e instanceof net.minecraft.world.entity.LivingEntity || e instanceof net.minecraft.world.entity.projectile.Projectile);
            
            int processedEntities = 0;
            int maxEntitiesToTrack = 6; 

            for (net.minecraft.world.entity.Entity e : entities) {
                if (processedEntities >= maxEntitiesToTrack) break;

                double speedSqr = e.getDeltaMovement().lengthSqr(); 
                if (speedSqr > 0.001) { 
                    double actualSpeed = Math.sqrt(speedSqr);
                    ACTIVE_MOVING_ENTITIES.add(new MovingEntityData(e.getX(), e.getY() + e.getBbHeight() / 2.0, e.getZ(), actualSpeed));
                    processedEntities++;
                }
            }
        }

        float currentFallDistance = player.fallDistance;
        if (player.onGround() && lastFallDistance > 3.0f) {
            BlockPos pos = player.blockPosition();
            if (level.getFluidState(pos).isEmpty()) {
                int maxParticles = LightDustConfig.HEAVY_LANDING_MAX_PARTICLES.get();
                int multiplier = LightDustConfig.HEAVY_LANDING_PARTICLE_MULTIPLIER.get();
                int count = Math.min(maxParticles, (int) (lastFallDistance * multiplier));
                double maxRadius = Math.min(5.0, 1.5 + (lastFallDistance * 0.1));
                double upBase = LightDustConfig.HEAVY_LANDING_UPWARD_SPEED.get();
                double outBase = LightDustConfig.HEAVY_LANDING_OUTWARD_SPEED.get();
                
                for (int i = 0; i < count; i++) {
                    double pRadius = level.random.nextDouble() * maxRadius;
                    double angle = level.random.nextDouble() * Math.PI * 2;
                    double dX = Math.cos(angle) * pRadius;
                    double dZ = Math.sin(angle) * pRadius;
                    double px = player.getX() + dX;
                    double py = player.getY() + 0.1;
                    double pz = player.getZ() + dZ;
                    double forceMult = Math.max(0.2, (maxRadius - pRadius) / maxRadius);
                    double scale = Math.min(2.5, 1.0 + (lastFallDistance * 0.02));
                    double vx = (dX / (pRadius == 0 ? 1 : pRadius)) * outBase * forceMult * scale * (0.8 + level.random.nextDouble() * 0.4);
                    double vy = upBase * forceMult * scale * (0.8 + level.random.nextDouble() * 0.4);
                    double vz = (dZ / (pRadius == 0 ? 1 : pRadius)) * outBase * forceMult * scale * (0.8 + level.random.nextDouble() * 0.4);
                    level.addParticle(ParticleInit.ACTION_DUST_PARTICLE.get(), px, py, pz, vx, vy, vz);
                }

                DustParticle.LANDING_IMPACT_POS = player.position();
                DustParticle.LANDING_IMPACT_TICK = level.getGameTime();
                DustParticle.LANDING_IMPACT_FORCE = LightDustConfig.HEAVY_LANDING_AMBIENT_PUSH.get();
                DustParticle.LANDING_IMPACT_RADIUS = LightDustConfig.HEAVY_LANDING_AMBIENT_RADIUS.get();
            }
        }
        lastFallDistance = player.onGround() ? 0.0f : currentFallDistance;

        double playerVelocitySqr = player.getDeltaMovement().lengthSqr();
        boolean isMovingFast = playerVelocitySqr > 0.005;

        int configRadius = LightDustConfig.AMBIENT_RADIUS.get();
        int radius = isMovingFast ? Math.min(5, configRadius) : configRadius; 
        
        int baseMaxCap = LightDustConfig.AMBIENT_BLOCK_CAP.get();
        int maxCap = isMovingFast ? Math.max(1, baseMaxCap / 2) : baseMaxCap;

        boolean doCulling = !isMovingFast && LightDustConfig.ENABLE_OCCLUSION_CULLING.get();

        int diffThreshold = LightDustConfig.DAYTIME_LIGHT_DIFF.get();
        int radiusSqr = radius * radius;
        int falloffDist = LightDustConfig.FALLOFF_DISTANCE.get();
        int falloffDistSqr = falloffDist * falloffDist;
        double falloffMult = LightDustConfig.FALLOFF_MULTIPLIER.get();
        BlockPos playerPos = player.blockPosition();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        com.lightdust.client.HandheldLightManager.LightData heldLight = com.lightdust.client.HandheldLightManager.getHeldLight(player);

        long tick = level.getGameTime();
        int tickMod = (int) (tick % 400); 
        long time = level.getDayTime() % 24000;
        boolean isDay = time < 13000 || time > 23000;

        int baseInterval = isMovingFast ? 2 : 10; 
        int maxRaycastsPerTick = 150; 
        int raycastsDoneThisTick = 0;
        boolean playerCanSeeSky = level.canSeeSky(playerPos);
        net.minecraft.world.phys.Vec3 lookVec = player.getLookAngle();

        int size = radius * 2 + 1;
        int offsetX = (tickMod * 7) % size;
        int offsetY = (tickMod * 11) % size;
        int offsetZ = (tickMod * 13) % size;

        for (int i = 0; i < size; i++) {
            int x = -radius + ((i + offsetX) % size);
            for (int j = 0; j < size; j++) {
                int y = -radius + ((j + offsetY) % size);
                for (int k = 0; k < size; k++) {
                    int z = -radius + ((k + offsetZ) % size);

                    int distSqr = x * x + y * y + z * z;
                    if (distSqr > radiusSqr) {
                        continue;
                    }

                    // If moving too fast then ignores behind
                    if (isMovingFast && distSqr > 4) { 
                        double dotProduct = (x * lookVec.x + y * lookVec.y + z * lookVec.z) / Math.sqrt(distSqr);
                        if (dotProduct < -0.15) {
                            continue;
                        }
                    }

                    int hash = Math.abs(x * 89 + y * 31 + z * 13);
                    int checkInterval = baseInterval;
                    if (heldLight != null && distSqr <= (heldLight.radius * heldLight.radius)) {
                        checkInterval = Math.max(2, baseInterval - 4); 
                    } else if (distSqr <= 36) {
                        checkInterval = Math.max(3, baseInterval - 2);
                    }

                    if (hash % checkInterval != (tickMod % checkInterval)) {
                        continue;
                    }

                    mutablePos.set(playerPos.getX() + x, playerPos.getY() + y, playerPos.getZ() + z);
                    
                    long posKey = BlockPos.asLong(mutablePos.getX(), mutablePos.getY(), mutablePos.getZ());
                    int localMaxCap = maxCap;
                    if (mutablePos.getY() < 0) {
                        double depthFactor = Math.min(1.0, (double) (-mutablePos.getY()) / 64.0);
                        localMaxCap = (int) (maxCap * (1.0 + depthFactor));
                    }
                    int currentCount = DustParticle.AMBIENT_COUNTS.getOrDefault(posKey, 0);
                    if (currentCount >= localMaxCap) {
                        continue;
                    }

                    boolean canSeeSky = level.canSeeSky(mutablePos);
                    if (canSeeSky) {
                        if (level.isThundering() && LightDustConfig.DISABLE_DURING_THUNDER.get()) {
                            continue;
                        } else if (level.isRaining() && !level.isThundering() && LightDustConfig.DISABLE_DURING_RAIN.get()) {
                            continue;
                        }
                    }

                    if (level.dimension() == Level.OVERWORLD && isDay && canSeeSky && !level.isRaining()) {
                        continue;
                    }

                    int blockLight = level.getBrightness(LightLayer.BLOCK, mutablePos);
                    if (heldLight != null) {
                        double distToPlayer = Math.sqrt(mutablePos.distToCenterSqr(player.getX(), player.getY(), player.getZ()));
                        int handLight = (int) (heldLight.radius - distToPlayer);
                        if (handLight > blockLight) {
                            blockLight = handLight;
                        }
                    }

                    int minLight = LightDustConfig.MIN_BLOCK_LIGHT.get();
                    boolean isDarkCave = mutablePos.getY() < 60 && !canSeeSky && blockLight < minLight;
                    if (blockLight < minLight && !isDarkCave) {
                        continue;
                    }
                    if (isDay && !isDarkCave && !level.isRaining()) {
                        int skyLight = level.getBrightness(LightLayer.SKY, mutablePos);
                        if ((blockLight - skyLight) <= diffThreshold) {
                            continue;
                        }
                    }

                    if (level.getFluidState(mutablePos).is(FluidTags.WATER)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(mutablePos);
                    if (!state.getCollisionShape(level, mutablePos).isEmpty()) {
                        continue;
                    }

                    int targetCap;
                    if (isDarkCave) {
                        targetCap = Math.max(1, (int) (localMaxCap * 0.15f));
                    } else if (blockLight >= 9) {
                        targetCap = localMaxCap;
                    } else if (blockLight >= 7) {
                        targetCap = Math.max(1, (int) (localMaxCap * 0.6f));
                    } else {
                        targetCap = Math.max(1, (int) (localMaxCap * 0.3f));
                    }

                    if (distSqr > falloffDistSqr) {
                        targetCap = Math.max(1, (int) (targetCap * falloffMult));
                    }
                    
                    if (currentCount < targetCap) {
                        boolean isVisible = true;
                        
                        if (doCulling && distSqr > 16) {
                            if (!(canSeeSky && playerCanSeeSky)) {
                                if (raycastsDoneThisTick >= maxRaycastsPerTick) {
                                    continue; 
                                }
                                raycastsDoneThisTick++;

                                net.minecraft.world.phys.Vec3 eyePos = player.getEyePosition();
                                net.minecraft.world.phys.Vec3 targetPosVec = new net.minecraft.world.phys.Vec3(mutablePos.getX() + 0.5, mutablePos.getY() + 0.5, mutablePos.getZ() + 0.5);
                                net.minecraft.world.phys.BlockHitResult sightCheck = level.clip(new net.minecraft.world.level.ClipContext(
                                        eyePos, targetPosVec, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                                        net.minecraft.world.level.ClipContext.Fluid.NONE, player
                                ));

                                if (sightCheck.getType() != net.minecraft.world.phys.HitResult.Type.MISS && !sightCheck.getBlockPos().equals(mutablePos)) {
                                    isVisible = false;
                                }
                            }
                        }

                        if (!isVisible) continue;

                        DustParticle.PENDING_POS = mutablePos.immutable();
                        int spawnCount = isMovingFast ? 4 : 1;
                        if (heldLight != null && distSqr <= (heldLight.radius * heldLight.radius)) {
                            spawnCount = Math.max(spawnCount, 2); 
                        }
                        spawnCount = Math.min(spawnCount, targetCap - currentCount);

                        for (int s = 0; s < spawnCount; s++) {
                            double px = mutablePos.getX() + level.random.nextDouble();
                            double py = mutablePos.getY() + 0.1 + (level.random.nextDouble() * 0.8);
                            double pz = mutablePos.getZ() + level.random.nextDouble();
                            level.addParticle(ParticleInit.DUST_PARTICLE.get(), px, py, pz, 0, 0, 0);
                        }
                        DustParticle.PENDING_POS = null;
                    }
                }
            }
        }

        if (tick % 60 == 0) {
            double hardCap = LightDustConfig.AMBIENT_HARD_CAP.get();
            double pruneDistSqr = (hardCap + 1) * (hardCap + 1);
            DustParticle.AMBIENT_COUNTS.keySet().removeIf(key -> BlockPos.of(key).distSqr(playerPos) > pruneDistSqr);
        }

        if (mc.options.keyAttack.isDown()) {
            if (lastTargetPos != null && level.getBlockState(lastTargetPos).isAir()) {
                int count = LightDustConfig.BREAK_PARTICLE_COUNT.get();
                double speed = LightDustConfig.BREAK_PARTICLE_SPEED.get();
                for (int i = 0; i < count; i++) {
                    double px = lastTargetPos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.5;
                    double py = lastTargetPos.getY() + 0.5 + (level.random.nextDouble() - 0.5) * 0.5;
                    double pz = lastTargetPos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.5;
                    double vx = (level.random.nextDouble() - 0.5) * speed;
                    double vy = (level.random.nextDouble() - 0.5) * speed;
                    double vz = (level.random.nextDouble() - 0.5) * speed;
                    level.addParticle(ParticleInit.ACTION_DUST_PARTICLE.get(), px, py, pz, vx, vy, vz);
                }
                lastTargetPos = null;
            } else if (mc.hitResult != null && mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                lastTargetPos = ((net.minecraft.world.phys.BlockHitResult) mc.hitResult).getBlockPos();
                if (level.getBlockState(lastTargetPos).isAir()) {
                    lastTargetPos = null;
                }
            } else {
                lastTargetPos = null;
            }
        } else {
            lastTargetPos = null;
        }
    }

    @SubscribeEvent
    public static void onPlaySound(net.minecraftforge.client.event.sound.PlaySoundEvent event) {
        if (event.getSound() != null && event.getSound().getLocation() != null) {
            String soundPath = event.getSound().getLocation().getPath();
            if (soundPath.contains("explode") || soundPath.contains("warden.roar") || soundPath.contains("sonic_boom")) {
                DustParticle.LOUD_NOISE_POS = new net.minecraft.world.phys.Vec3(event.getSound().getX(), event.getSound().getY(), event.getSound().getZ());
                if (Minecraft.getInstance().level != null) {
                    DustParticle.LOUD_NOISE_TICK = Minecraft.getInstance().level.getGameTime();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clearMaps();
    }

    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        clearMaps();
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity().level().isClientSide) {
            clearMaps();
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity().level().isClientSide) {
            clearMaps();
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            clearMaps();
        }
    }

    private static void clearMaps() {
        DustParticle.AMBIENT_COUNTS.clear();
        DustParticle.PENDING_POS = null;
        ACTIVE_MOVING_ENTITIES.clear();
    }
}