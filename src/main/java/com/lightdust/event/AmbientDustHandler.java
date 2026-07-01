package com.lightdust.event;

import com.lightdust.LightDust;
import com.lightdust.client.particle.DustParticle;
import com.lightdust.config.LightDustConfig;
import com.lightdust.config.LightDustExperimentalConfig;
import com.lightdust.init.ParticleInit;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

@EventBusSubscriber(modid = LightDust.MODID, value = Dist.CLIENT)
public class AmbientDustHandler {

    private static float lastFallDistance = 0.0f;
    private static BlockPos lastTargetPos = null;
    private static net.minecraft.core.Direction lastTargetFace = null;
    private static int lastTargetColor = -1;
    private static net.minecraft.world.phys.Vec3 lastTargetHitVec = null;

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
    private static final java.util.List<BlockPos> VALID_SPAWN_SPACES = new java.util.ArrayList<>();
    private static long lastBfsTick = 0;

    private static void updateSpawnVolumeFAST(Player player, Level level, int radius) {
        VALID_SPAWN_SPACES.clear();
        int maxNodes = LightDustConfig.COMMON.fastSpawnChecks.get(); 
        BlockPos startPos = player.blockPosition().above();
        net.minecraft.util.RandomSource random = level.getRandom();

        for (int i = 0; i < maxNodes * 3 && VALID_SPAWN_SPACES.size() < maxNodes; i++) {
            int rx = startPos.getX() + random.nextInt(radius * 2 + 1) - radius;
            int ry = startPos.getY() + random.nextInt(radius + 3) - 2;
            int rz = startPos.getZ() + random.nextInt(radius * 2 + 1) - radius;
            
            BlockPos checkPos = new BlockPos(rx, ry, rz);
            if (level.hasChunkAt(checkPos)) {
                BlockState state = level.getBlockState(checkPos);
                if (state.isAir() || state.getCollisionShape(level, checkPos).isEmpty()) {
                    VALID_SPAWN_SPACES.add(checkPos);
                }
            }
        }
    }

    private static void updateSpawnVolumeFANCY(Player player, Level level, int radius) {
        VALID_SPAWN_SPACES.clear();
        int maxNodes = LightDustConfig.COMMON.fancySpawnChecks.get(); 
        int radiusSqr = radius * radius;
        
        BlockPos startPos = player.blockPosition().above();
        java.util.Queue<BlockPos> queue = new java.util.LinkedList<>();
        java.util.Set<BlockPos> visited = new java.util.HashSet<>();
        
        queue.add(startPos);
        visited.add(startPos);
        
        while (!queue.isEmpty() && VALID_SPAWN_SPACES.size() < maxNodes) {
            BlockPos curr = queue.poll();
            VALID_SPAWN_SPACES.add(curr);
            
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                BlockPos neighbor = curr.relative(dir);
                if (neighbor.distSqr(startPos) > radiusSqr) continue;
                
                if (visited.add(neighbor)) {
                    if (level.getBlockState(neighbor).getCollisionShape(level, neighbor).isEmpty()) {
                        queue.add(neighbor);
                    }
                }
            }
        }
    }

    private static ParticleOptions p(DeferredHolder<ParticleType<?>, SimpleParticleType> n, DeferredHolder<ParticleType<?>, SimpleParticleType> h) {
        return LightDustConfig.COMMON.useHdParticles.get() ? h.get() : n.get();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused() || mc.player == null || mc.level == null) {
            return;
        }

        if (!LightDustConfig.SPEC.isLoaded()) {
            return;
        }

        Player player = mc.player;
        Level level = mc.level;
        long currentTick = level.getGameTime();

        // Staggered Cache Clearing
        if (currentTick % 400 == 0) DustParticle.TINT_CACHE.clear();
        if (currentTick % 400 == 5) DustParticle.BIOME_CACHE.clear();
        if (currentTick % 400 == 10) DustParticle.BEHAVIOR_CACHE.clear();

        // Update Global Wind Grid
        com.lightdust.client.environment.GlobalWindGrid.tick(level, player.blockPosition());

        int scanRate = LightDustExperimentalConfig.EXPERIMENTAL.entityScanRate.get();
        if (currentTick % scanRate == 0) {
            ACTIVE_MOVING_ENTITIES.clear();
            if (LightDustExperimentalConfig.EXPERIMENTAL.enableEntityDisturbance.get()) {
                double scanRadius = LightDustConfig.COMMON.ambientRadius.get();
                net.minecraft.world.phys.AABB playerBounds = player.getBoundingBox().inflate(scanRadius);
                java.util.List<net.minecraft.world.entity.Entity> entities = level.getEntities((net.minecraft.world.entity.Entity) null, playerBounds,
                        e -> e instanceof net.minecraft.world.entity.LivingEntity || e instanceof net.minecraft.world.entity.projectile.Projectile || e instanceof net.minecraft.world.entity.vehicle.AbstractMinecart);

                int processedEntities = 0;
                int maxEntitiesToTrack = 6;

                for (net.minecraft.world.entity.Entity e : entities) {
                    if (processedEntities >= maxEntitiesToTrack) break;
                    double speedSqr = e.getDeltaMovement().lengthSqr();
                    if (speedSqr > 0.001) {
                        double actualSpeed = Math.max(0, Math.sqrt(speedSqr));
                        ACTIVE_MOVING_ENTITIES.add(new MovingEntityData(e.getX(), e.getY() + e.getBbHeight() / 2.0, e.getZ(), actualSpeed));
                        processedEntities++;
                    }
                }
            }
        }

        float currentFallDistance = player.fallDistance;
        if (LightDustConfig.COMMON.enableHeavyLandingDust.get() && player.onGround() && lastFallDistance > 3.0f) {
            BlockPos pos = player.blockPosition();
            BlockPos groundPos = pos.below();

            if (level.getFluidState(pos).isEmpty()) {
                int tempColor = -1;
                if (LightDustConfig.COMMON.enableDynamicBlockColors.get()) {
                    tempColor = level.getBlockState(groundPos).getMapColor(level, groundPos).col;
                }

                int maxParticles = LightDustConfig.COMMON.heavyLandingMaxParticles.get();
                int multiplier = LightDustConfig.COMMON.heavyLandingParticleMultiplier.get();
                int count = (int) Math.min(maxParticles, lastFallDistance * multiplier);
                
                double forceMult = Math.min(2.0, lastFallDistance / 5.0);

                try {
                    com.lightdust.client.particle.ActionDustParticle.CURRENT_BLOCK_COLOR = tempColor;
                    for (int i = 0; i < count; i++) {
                        double angle = level.random.nextDouble() * 2 * Math.PI;
                        double radius = level.random.nextDouble() * 1.5;
                        double px = pos.getX() + 0.5 + Math.cos(angle) * radius;
                        double py = pos.getY() + 0.1;
                        double pz = pos.getZ() + 0.5 + Math.sin(angle) * radius;

                        double vx = Math.cos(angle) * LightDustConfig.COMMON.heavyLandingOutwardSpeed.get() * forceMult * (0.8 + level.random.nextDouble() * 0.4);
                        double vz = Math.sin(angle) * LightDustConfig.COMMON.heavyLandingOutwardSpeed.get() * forceMult * (0.8 + level.random.nextDouble() * 0.4);
                        double scale = 1.0 - (radius / 1.5);
                        double vy = LightDustConfig.COMMON.heavyLandingUpwardSpeed.get() * forceMult * scale * (0.8 + level.random.nextDouble() * 0.4);

                        level.addParticle(p(ParticleInit.ACTION_DUST_PARTICLE, ParticleInit.ACTION_DUST_PARTICLE_HD), px, py, pz, vx, vy, vz);
                    }
                } finally {
                    com.lightdust.client.particle.ActionDustParticle.CURRENT_BLOCK_COLOR = -1;
                }
                
                DustParticle.LANDING_IMPACT_POS = player.position();
                DustParticle.LANDING_IMPACT_TICK = level.getGameTime();
                DustParticle.LANDING_IMPACT_FORCE = LightDustConfig.COMMON.heavyLandingAmbientPush.get() * 0.001;
                DustParticle.LANDING_IMPACT_RADIUS = LightDustConfig.COMMON.heavyLandingAmbientRadius.get();
            }
        }
        lastFallDistance = player.onGround() ? 0.0f : currentFallDistance;

        double playerVelocitySqr = player.getDeltaMovement().lengthSqr();
        boolean isMovingFast = playerVelocitySqr > 0.005;

        int configRadius = LightDustConfig.COMMON.ambientRadius.get();
        int radius = isMovingFast ? Math.min(5, configRadius) : configRadius;
        int baseMaxCap = LightDustConfig.COMMON.ambientBlockCap.get();
        int maxCap = isMovingFast ? Math.max(1, baseMaxCap / 2) : baseMaxCap;

        BlockPos playerPos = player.blockPosition();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        com.lightdust.client.HandheldLightManager.LightData heldLight = com.lightdust.client.HandheldLightManager.getHeldLight(player);

        if (currentTick - lastBfsTick >= 10) {
            lastBfsTick = currentTick;
            if (LightDustConfig.COMMON.spawningMode.get() == LightDustConfig.SpawningMode.FANCY) {
                updateSpawnVolumeFANCY(player, level, radius);
            } else {
                updateSpawnVolumeFAST(player, level, radius);
            }
        }

        if (!VALID_SPAWN_SPACES.isEmpty()) {
            int spawnAttempts = isMovingFast ? 80 : 30;
            for (int i = 0; i < spawnAttempts; i++) {
                mutablePos.set(VALID_SPAWN_SPACES.get(level.random.nextInt(VALID_SPAWN_SPACES.size())));

                if (LightDustConfig.COMMON.enableMaxYLevel.get() && mutablePos.getY() > LightDustConfig.COMMON.maxYLevel.get()) {
                    continue;
                }

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

                int blockLight = level.getBrightness(LightLayer.BLOCK, mutablePos);
                int minLight = LightDustConfig.COMMON.minBlockLight.get();

                if (heldLight != null && LightDustConfig.COMMON.enableHandheldLights.get()) {
                    double distToPlayer = Math.sqrt(player.distanceToSqr(mutablePos.getX() + 0.5, mutablePos.getY() + 0.5, mutablePos.getZ() + 0.5));
                    int handLight = (int) (heldLight.radius - distToPlayer);
                    if (handLight > blockLight) {
                        blockLight = handLight;
                    }
                }

                boolean isDarkCave = mutablePos.getY() < 60 && !level.canSeeSky(mutablePos);
                if (blockLight < minLight && !isDarkCave) {
                    continue;
                }

                int targetCap = isDarkCave ? Math.max(1, (int) (localMaxCap * 0.15f)) : localMaxCap;

                if (currentCount < targetCap) {
                    DustParticle.PENDING_POS = mutablePos.immutable();
                    int spawnCount = isMovingFast ? 4 : 1;

                    if (heldLight != null && mutablePos.distToCenterSqr(player.getX(), player.getY(), player.getZ()) <= (heldLight.radius * heldLight.radius)) {
                        spawnCount = Math.max(spawnCount, 2);
                    }

                    spawnCount = Math.min(spawnCount, targetCap - currentCount);
                    for (int s = 0; s < spawnCount; s++) {
                        double px = mutablePos.getX() + level.random.nextDouble();
                        double py = mutablePos.getY() + 0.1 + (level.random.nextDouble() * 0.8);
                        double pz = mutablePos.getZ() + level.random.nextDouble();
                        level.addParticle(p(ParticleInit.DUST_PARTICLE, ParticleInit.DUST_PARTICLE_HD), px, py, pz, 0, 0, 0);
                    }
                    DustParticle.PENDING_POS = null;
                }
            }
        }

        if (currentTick % 40 == 0) {
            int pruneRadius = LightDustConfig.COMMON.ambientHardCap.get() + 2;
            int pruneDistSqr = pruneRadius * pruneRadius;
            try {
                DustParticle.AMBIENT_COUNTS.keySet().removeIf(key -> BlockPos.of(key).distSqr(playerPos) > pruneDistSqr);
            } catch (Exception ignored) {
            }
        }

        if (LightDustConfig.COMMON.enableCaveTremors.get() && com.lightdust.client.particle.DustParticle.LOUD_NOISE_POS != null) {
            long ageInTicks = level.getGameTime() - com.lightdust.client.particle.DustParticle.LOUD_NOISE_TICK;
            if (ageInTicks < 80) {
                double intensity = Math.pow(Math.max(0.0, 1.0 - (ageInTicks / 80.0)), 2.5);
                double waveSpeed = 1.5;
                double waveDist = ageInTicks * waveSpeed;
                double maxRadius = 32.0;

                double distRatio = Math.min(1.0, waveDist / maxRadius);
                double expoFalloff = Math.pow(1.0 - distRatio, LightDustConfig.COMMON.tremorFalloffExponent.get());
                double epicenterDensityBoost = (distRatio < 0.1) ? LightDustConfig.COMMON.tremorEpicenterMultiplier.get() : 1.0;

                int baseCount = LightDustConfig.COMMON.caveTremorParticleCount.get();
                double circumference = 2 * Math.PI * Math.max(1.0, waveDist);
                int totalRingParticles = (int) ((circumference / 10.0) * baseCount * intensity * epicenterDensityBoost);

                int ambientRadius = LightDustConfig.COMMON.ambientRadius.get();
                int ambientRadiusSqr = ambientRadius * ambientRadius;

                net.minecraft.core.BlockPos.MutableBlockPos mut = new net.minecraft.core.BlockPos.MutableBlockPos();

                java.util.Map<Long, Integer> roofCache = new java.util.HashMap<>();
                java.util.Map<Long, Integer> floorCache = new java.util.HashMap<>();

                for (int i = 0; i < totalRingParticles; i++) {
                    float angle = level.random.nextFloat() * net.minecraft.util.Mth.TWO_PI;
                    double actualDist = waveDist - (level.random.nextDouble() * LightDustConfig.COMMON.tremorWavefrontThickness.get());
                    if (actualDist < 0) continue;

                    double targetX = com.lightdust.client.particle.DustParticle.LOUD_NOISE_POS.x + net.minecraft.util.Mth.cos(angle) * actualDist;
                    double targetZ = com.lightdust.client.particle.DustParticle.LOUD_NOISE_POS.z + net.minecraft.util.Mth.sin(angle) * actualDist;

                    double dx = targetX - player.getX();
                    double dz = targetZ - player.getZ();
                    if (dx * dx + dz * dz > ambientRadiusSqr) continue;

                    double randomYStart = player.getY() - 2.0 + (level.random.nextDouble() * 8.0);
                    mut.set(targetX, randomYStart, targetZ);

                    long colKey = mut.asLong();
                    int surfaceY = -1;
                    net.minecraft.core.Direction searchDir = net.minecraft.core.Direction.UP;

                    if (level.random.nextDouble() < LightDustConfig.COMMON.tremorCeilingBias.get()) {
                        if (roofCache.containsKey(colKey)) {
                            surfaceY = roofCache.get(colKey);
                        } else {
                            for (int y = 0; y < 10; y++) {
                                if (!level.getBlockState(mut).isAir()) {
                                    surfaceY = mut.getY() - 1;
                                    roofCache.put(colKey, surfaceY);
                                    break;
                                }
                                mut.move(net.minecraft.core.Direction.UP);
                            }
                            if (surfaceY == -1) roofCache.put(colKey, -1);
                        }
                    } else {
                        searchDir = net.minecraft.core.Direction.DOWN;
                        if (floorCache.containsKey(colKey)) {
                            surfaceY = floorCache.get(colKey);
                        } else {
                            for (int y = 0; y < 10; y++) {
                                if (!level.getBlockState(mut).isAir()) {
                                    surfaceY = mut.getY();
                                    floorCache.put(colKey, surfaceY);
                                    break;
                                }
                                mut.move(net.minecraft.core.Direction.DOWN);
                            }
                            if (surfaceY == -1) floorCache.put(colKey, -1);
                        }
                    }

                    if (surfaceY != -1) {
                        mut.setY(surfaceY);
                        int tempColor = -1;
                        if (LightDustConfig.COMMON.enableDynamicBlockColors.get()) {
                            tempColor = level.getBlockState(mut).getMapColor(level, mut).col;
                        }

                        double minGrav = LightDustConfig.COMMON.tremorMinGravity.get() * 0.001;
                        double maxGrav = LightDustConfig.COMMON.tremorMaxGravity.get() * 0.001;
                        double baseDynamicGravity = minGrav + ((maxGrav - minGrav) * expoFalloff);

                        float finalGravity;
                        if (searchDir == net.minecraft.core.Direction.UP) {
                            finalGravity = (float) (baseDynamicGravity * LightDustConfig.COMMON.tremorRoofMultiplier.get());
                        } else {
                            finalGravity = (float) (baseDynamicGravity * LightDustConfig.COMMON.tremorFloorMultiplier.get());
                        }

                        double px = mut.getX() + level.random.nextDouble();
                        double py = mut.getY() + (searchDir == net.minecraft.core.Direction.UP ? -0.1 : 1.1);
                        double pz = mut.getZ() + level.random.nextDouble();

                        double velMult = 0.1 + (4.9 * expoFalloff);
                        double chaos = 0.5 + (level.random.nextDouble() * 1.0);
                        
                        double pushDir = (searchDir == net.minecraft.core.Direction.UP) ? -1.05 : 1.4;
                        double baseVerticalForce = (searchDir == net.minecraft.core.Direction.UP) ? 0.06 : 0.08;

                        double vx = (level.random.nextDouble() - 0.5) * (0.05 * intensity * velMult * chaos);
                        double minKick = (searchDir == net.minecraft.core.Direction.DOWN) ? (0.02 * LightDustConfig.COMMON.tremorFloorKickForce.get()) : 0.0;
                        double vy = (pushDir * baseVerticalForce * velMult * intensity) + (level.random.nextDouble() - 0.5) * 0.05 + (pushDir * minKick);
                        double vz = (level.random.nextDouble() - 0.5) * (0.05 * intensity * velMult * chaos);

                        try {
                            com.lightdust.client.particle.ExplosionDustParticle.CURRENT_BLOCK_COLOR = tempColor;
                            com.lightdust.client.particle.ExplosionDustParticle.CURRENT_GRAVITY = finalGravity;
                            level.addParticle(p(ParticleInit.EXPLOSION_DUST_PARTICLE, ParticleInit.EXPLOSION_DUST_PARTICLE_HD), px, py, pz, vx, vy, vz);
                        } finally {
                            com.lightdust.client.particle.ExplosionDustParticle.CURRENT_BLOCK_COLOR = -1;
                            com.lightdust.client.particle.ExplosionDustParticle.CURRENT_GRAVITY = 0.003f;
                        }
                    }
                }
            }
        }

        if (mc.options.keyAttack.isDown()) {
            if (lastTargetPos != null && level.getBlockState(lastTargetPos).isAir()) {
                if (LightDustConfig.COMMON.enableBlockBreakingDust.get()) {
                    int count = LightDustConfig.COMMON.breakParticleCount.get();
                    double speed = LightDustConfig.COMMON.breakParticleSpeed.get();
                    int tempColor = -1;

                    if (LightDustConfig.COMMON.enableDynamicBlockColors.get()) {
                        tempColor = lastTargetColor;
                    }

                    double normX = lastTargetFace != null ? lastTargetFace.getStepX() : 0;
                    double normY = lastTargetFace != null ? lastTargetFace.getStepY() : 1;
                    double normZ = lastTargetFace != null ? lastTargetFace.getStepZ() : 0;

                    double originX = lastTargetHitVec != null ? lastTargetHitVec.x : lastTargetPos.getX() + 0.5;
                    double originY = lastTargetHitVec != null ? lastTargetHitVec.y : lastTargetPos.getY() + 0.5;
                    double originZ = lastTargetHitVec != null ? lastTargetHitVec.z : lastTargetPos.getZ() + 0.5;

                    try {
                        com.lightdust.client.particle.ActionDustParticle.CURRENT_BLOCK_COLOR = tempColor;
                        for (int i = 0; i < count; i++) {
                            double px = originX + (level.random.nextDouble() - 0.5) * 0.5;
                            double py = originY + (level.random.nextDouble() - 0.5) * 0.5;
                            double pz = originZ + (level.random.nextDouble() - 0.5) * 0.5;

                            double vx = (normX * 0.6 + (level.random.nextDouble() - 0.5)) * speed;
                            double vy = (normY * 0.6 + (level.random.nextDouble() - 0.5)) * speed;
                            double vz = (normZ * 0.6 + (level.random.nextDouble() - 0.5)) * speed;

                            level.addParticle(p(ParticleInit.ACTION_DUST_PARTICLE, ParticleInit.ACTION_DUST_PARTICLE_HD), px, py, pz, vx, vy, vz);
                        }
                    } finally {
                        com.lightdust.client.particle.ActionDustParticle.CURRENT_BLOCK_COLOR = -1;
                    }
                }
                lastTargetPos = null;
                lastTargetFace = null;
                lastTargetColor = -1;
                lastTargetHitVec = null;
            } else if (mc.hitResult != null && mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                net.minecraft.world.phys.BlockHitResult blockHit = (net.minecraft.world.phys.BlockHitResult) mc.hitResult;
                BlockPos hitPos = blockHit.getBlockPos();
                BlockState hitState = level.getBlockState(hitPos);

                if (!hitState.isAir()) {
                    lastTargetPos = hitPos;
                    lastTargetFace = blockHit.getDirection();
                    lastTargetHitVec = blockHit.getLocation();
                    lastTargetColor = hitState.getMapColor(level, hitPos).col;
                }
            }
        } else {
            lastTargetPos = null;
            lastTargetFace = null;
            lastTargetColor = -1;
            lastTargetHitVec = null;
        }
    }

    @SubscribeEvent
    public static void onPlaySound(net.neoforged.neoforge.client.event.sound.PlaySoundEvent event) {
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
