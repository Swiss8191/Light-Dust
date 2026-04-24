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
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = LightDust.MODID, value = Dist.CLIENT)
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

    private static void updateSpawnVolumeBFS(Player player, Level level, int radius) {
        VALID_SPAWN_SPACES.clear();
        int maxNodes = 1200; 
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

    private static ParticleOptions p(RegistryObject<SimpleParticleType> n, RegistryObject<SimpleParticleType> h) {
        return LightDustConfig.USE_HD_PARTICLES.get() ? h.get() : n.get();
    }

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

        long currentTick = level.getGameTime();
        if (currentTick % 20 == 0) {
            DustParticle.TINT_CACHE.clear();
            DustParticle.BIOME_CACHE.clear();
            DustParticle.BEHAVIOR_CACHE.clear();
            DustParticle.DRAFT_CACHE.clear();
        }

        int scanRate = com.lightdust.config.LightDustExperimentalConfig.ENTITY_SCAN_RATE.get();
        if (currentTick % scanRate == 0) {
            ACTIVE_MOVING_ENTITIES.clear();
            if (com.lightdust.config.LightDustExperimentalConfig.ENABLE_ENTITY_DISTURBANCE.get()) {
                double scanRadius = LightDustConfig.AMBIENT_RADIUS.get();
                net.minecraft.world.phys.AABB playerBounds = player.getBoundingBox().inflate(scanRadius);

                java.util.List<net.minecraft.world.entity.Entity> entities = level.getEntities((net.minecraft.world.entity.Entity) null, playerBounds,
                        e -> e instanceof net.minecraft.world.entity.LivingEntity || e instanceof net.minecraft.world.entity.projectile.Projectile || e instanceof net.minecraft.world.entity.vehicle.AbstractMinecart);
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
        }

        float currentFallDistance = player.fallDistance;
        if (player.onGround() && lastFallDistance > 3.0f) {
            BlockPos pos = player.blockPosition();
            BlockPos groundPos = pos.below();
            
            if (level.getFluidState(pos).isEmpty()) {
                
                int tempColor = -1;
                if (com.lightdust.config.LightDustConfig.ENABLE_DYNAMIC_BLOCK_COLORS.get()) {
                    tempColor = level.getBlockState(groundPos).getMapColor(level, groundPos).col;
                }

                int maxParticles = LightDustConfig.HEAVY_LANDING_MAX_PARTICLES.get();
                int multiplier = LightDustConfig.HEAVY_LANDING_PARTICLE_MULTIPLIER.get();
                int count = Math.min(maxParticles, (int) (lastFallDistance * multiplier));
                double maxRadius = Math.min(5.0, 1.5 + (lastFallDistance * 0.1));
                double upBase = LightDustConfig.HEAVY_LANDING_UPWARD_SPEED.get();
                double outBase = LightDustConfig.HEAVY_LANDING_OUTWARD_SPEED.get();
                try {
                    com.lightdust.client.particle.ActionDustParticle.CURRENT_BLOCK_COLOR = tempColor;
                    for (int i = 0; i < count; i++) {
                        double pRadius = level.random.nextDouble() * maxRadius;
                        float angle = level.random.nextFloat() * net.minecraft.util.Mth.TWO_PI;

                        float cosAngle = net.minecraft.util.Mth.cos(angle);
                        float sinAngle = net.minecraft.util.Mth.sin(angle);
                        
                        double dX = cosAngle * pRadius;
                        double dZ = sinAngle * pRadius;
                        
                        double px = player.getX() + dX;
                        double py = player.getY() + 0.1;
                        double pz = player.getZ() + dZ;
                        
                        double forceMult = Math.max(0.2, (maxRadius - pRadius) / maxRadius);
                        double scale = Math.min(2.5, 1.0 + (lastFallDistance * 0.02));

                        double vx = cosAngle * outBase * forceMult * scale * (0.8 + level.random.nextDouble() * 0.4);
                        double vy = upBase * forceMult * scale * (0.8 + level.random.nextDouble() * 0.4);
                        double vz = sinAngle * outBase * forceMult * scale * (0.8 + level.random.nextDouble() * 0.4);
                        level.addParticle(p(ParticleInit.ACTION_DUST_PARTICLE, ParticleInit.ACTION_DUST_PARTICLE_HD), px, py, pz, vx, vy, vz);
                    }
                } finally {
                    com.lightdust.client.particle.ActionDustParticle.CURRENT_BLOCK_COLOR = -1;
                }
                
                DustParticle.LANDING_IMPACT_POS = player.position();
                DustParticle.LANDING_IMPACT_TICK = level.getGameTime();
                DustParticle.LANDING_IMPACT_FORCE = LightDustConfig.HEAVY_LANDING_AMBIENT_PUSH.get() * 0.001;
                DustParticle.LANDING_IMPACT_RADIUS = LightDustConfig.HEAVY_LANDING_AMBIENT_RADIUS.get();
            }
        }

        lastFallDistance = player.onGround() ?
        0.0f : currentFallDistance;

        double playerVelocitySqr = player.getDeltaMovement().lengthSqr();
        boolean isMovingFast = playerVelocitySqr > 0.005;

        int configRadius = LightDustConfig.AMBIENT_RADIUS.get();
        int radius = isMovingFast ? Math.min(5, configRadius) : configRadius; 
        
        int baseMaxCap = LightDustConfig.AMBIENT_BLOCK_CAP.get();
        int maxCap = isMovingFast ?
        Math.max(1, baseMaxCap / 2) : baseMaxCap;

        BlockPos playerPos = player.blockPosition();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        com.lightdust.client.HandheldLightManager.LightData heldLight = com.lightdust.client.HandheldLightManager.getHeldLight(player);
        long tick = level.getGameTime();

        if (currentTick - lastBfsTick >= 10) {
            lastBfsTick = currentTick;
            updateSpawnVolumeBFS(player, level, radius);
        }

        if (!VALID_SPAWN_SPACES.isEmpty()) {
            int spawnAttempts = isMovingFast ?
            80 : 30; 
            
            for (int i = 0; i < spawnAttempts; i++) {
                mutablePos.set(VALID_SPAWN_SPACES.get(level.random.nextInt(VALID_SPAWN_SPACES.size())));
                if (LightDustConfig.ENABLE_MAX_Y_LEVEL.get() && mutablePos.getY() > LightDustConfig.MAX_Y_LEVEL.get()) {
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
                int minLight = LightDustConfig.MIN_BLOCK_LIGHT.get();
                boolean isDarkCave = mutablePos.getY() < 60 && !level.canSeeSky(mutablePos) && blockLight < minLight;
                if (blockLight < minLight && !isDarkCave && heldLight == null) {
                    continue;
                }

                int targetCap = isDarkCave ?
                Math.max(1, (int) (localMaxCap * 0.15f)) : localMaxCap;
                
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

        if (tick % 60 == 0) {
            try {
                double hardCap = LightDustConfig.AMBIENT_HARD_CAP.get();
                double pruneDistSqr = (hardCap + 1) * (hardCap + 1);
                DustParticle.AMBIENT_COUNTS.keySet().removeIf(key -> BlockPos.of(key).distSqr(playerPos) > pruneDistSqr);
            } catch (Exception e) {
            }
        }

        if (com.lightdust.client.particle.DustParticle.LOUD_NOISE_POS != null) {
            long ageInTicks = level.getGameTime() - com.lightdust.client.particle.DustParticle.LOUD_NOISE_TICK;
            if (ageInTicks < 80) { 
                double intensity = Math.pow(Math.max(0.0, 1.0 - (ageInTicks / 80.0)), 2.5);
                double waveSpeed = 1.5; 
                double waveDist = ageInTicks * waveSpeed; 
                double maxRadius = 32.0;
                double distRatio = Math.min(1.0, waveDist / maxRadius);

                double expoFalloff = Math.pow(1.0 - distRatio, com.lightdust.config.LightDustConfig.TREMOR_FALLOFF_EXPONENT.get());
                double epicenterDensityBoost = (distRatio < 0.1) ? com.lightdust.config.LightDustConfig.TREMOR_EPICENTER_MULTIPLIER.get() : 1.0;

                int baseCount = com.lightdust.config.LightDustConfig.CAVE_TREMOR_PARTICLE_COUNT.get();
                double circumference = 2 * Math.PI * Math.max(1.0, waveDist);

                int totalRingParticles = (int) ((circumference / 10.0) * baseCount * intensity * epicenterDensityBoost);
                int ambientRadius = com.lightdust.config.LightDustConfig.AMBIENT_RADIUS.get();
                int ambientRadiusSqr = ambientRadius * ambientRadius;
                net.minecraft.core.BlockPos.MutableBlockPos mut = new net.minecraft.core.BlockPos.MutableBlockPos();
                java.util.Map<Long, Integer> roofCache = new java.util.HashMap<>();
                java.util.Map<Long, Integer> floorCache = new java.util.HashMap<>();
                for (int i = 0; i < totalRingParticles; i++) {
                    float angle = level.random.nextFloat() * net.minecraft.util.Mth.TWO_PI;
                    double actualDist = waveDist - (level.random.nextDouble() * com.lightdust.config.LightDustConfig.TREMOR_WAVEFRONT_THICKNESS.get()); 
                    if (actualDist < 0) continue;
                    double targetX = com.lightdust.client.particle.DustParticle.LOUD_NOISE_POS.x + net.minecraft.util.Mth.cos(angle) * actualDist;
                    double targetZ = com.lightdust.client.particle.DustParticle.LOUD_NOISE_POS.z + net.minecraft.util.Mth.sin(angle) * actualDist;
                    double dx = targetX - player.getX();
                    double dz = targetZ - player.getZ();
                    if (dx * dx + dz * dz > ambientRadiusSqr) continue;
                    double randomYStart = player.getY() - 2.0 + (level.random.nextDouble() * 8.0);
                    mut.set(targetX, randomYStart, targetZ);

                    long colKey = net.minecraft.core.BlockPos.asLong((int)targetX, 0, (int)targetZ);
                    boolean forceCeiling = level.random.nextDouble() < com.lightdust.config.LightDustConfig.TREMOR_CEILING_BIAS.get();
                    net.minecraft.core.Direction searchDir = forceCeiling ? net.minecraft.core.Direction.UP : (level.random.nextBoolean() ? net.minecraft.core.Direction.UP : net.minecraft.core.Direction.DOWN);
                    boolean foundSurface = false;
                    java.util.Map<Long, Integer> activeCache = (searchDir == net.minecraft.core.Direction.UP) ? roofCache : floorCache;
                    if (activeCache.containsKey(colKey)) {
                        mut.set(targetX, activeCache.get(colKey), targetZ);
                        foundSurface = true;
                    } else {
                        double startX = mut.getX();
                        double startY = mut.getY();
                        double startZ = mut.getZ();

                        for (int step = 0; step < 12; step++) {
                            mut.move(searchDir);
                            if (!level.getBlockState(mut).isAir() && !level.getBlockState(mut).getCollisionShape(level, mut).isEmpty()) {
                                foundSurface = true;
                                activeCache.put(colKey, mut.getY()); 
                                break;
                            }
                        }
                        
                        if (!foundSurface) {
                       

                             searchDir = searchDir.getOpposite();
                            activeCache = (searchDir == net.minecraft.core.Direction.UP) ? roofCache : floorCache;
                            if (activeCache.containsKey(colKey)) {
                                mut.set(targetX, activeCache.get(colKey), targetZ);
                                foundSurface = true;
                            } else {
                                mut.set(startX, startY, startZ);
                                for (int step = 0; step < 12; step++) {
                                    mut.move(searchDir);
                                    if (!level.getBlockState(mut).isAir() && !level.getBlockState(mut).getCollisionShape(level, mut).isEmpty()) {
                                        foundSurface = true;
                                        activeCache.put(colKey, mut.getY()); 
                                        break;
                                    }
                                }
                            }
                        }
             

                    }
                    
                    if (foundSurface) {
                        int tempColor = -1;
                        if (com.lightdust.config.LightDustConfig.ENABLE_DYNAMIC_BLOCK_COLORS.get()) {
                            tempColor = level.getBlockState(mut).getMapColor(level, mut).col;
                        }
                        
                        double minGrav = com.lightdust.config.LightDustConfig.TREMOR_MIN_GRAVITY.get() * 0.001;
                        double maxGrav = com.lightdust.config.LightDustConfig.TREMOR_MAX_GRAVITY.get() * 0.001;
                        double baseDynamicGravity = minGrav + ((maxGrav - minGrav) * expoFalloff);
                        
                        float finalGravity;
                        if (searchDir == net.minecraft.core.Direction.UP) {
                            finalGravity = (float) (baseDynamicGravity * com.lightdust.config.LightDustConfig.TREMOR_ROOF_MULTIPLIER.get());
                        } else {
                            finalGravity = (float) (baseDynamicGravity * com.lightdust.config.LightDustConfig.TREMOR_FLOOR_MULTIPLIER.get());
                        }

                        double px = mut.getX() + level.random.nextDouble();
                        double py = mut.getY() + (searchDir == net.minecraft.core.Direction.UP ? -0.1 : 1.1);
                        double pz = mut.getZ() + level.random.nextDouble();
                        double velMult = 0.1 + (4.9 * expoFalloff);
                        double chaos = 0.5 + (level.random.nextDouble() * 1.0);
                        double pushDir = (searchDir == net.minecraft.core.Direction.UP) ? -1.05 : 1.4;
                        double baseVerticalForce = (searchDir == net.minecraft.core.Direction.UP) ? 0.06 : 0.08;
                        double vx = (level.random.nextDouble() - 0.5) * (0.05 * intensity * velMult * chaos);
                        double minKick = (searchDir == net.minecraft.core.Direction.DOWN) ? (0.02 * com.lightdust.config.LightDustConfig.TREMOR_FLOOR_KICK_FORCE.get()) : 0.0;
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
             

                int count = LightDustConfig.BREAK_PARTICLE_COUNT.get();
                double speed = LightDustConfig.BREAK_PARTICLE_SPEED.get();

                int tempColor = -1;
                if (LightDustConfig.ENABLE_DYNAMIC_BLOCK_COLORS.get()) {
                    tempColor = lastTargetColor;
                }

                double normX = lastTargetFace != null ?
                lastTargetFace.getStepX() : 0;
                double normY = lastTargetFace != null ? lastTargetFace.getStepY() : 1; 
                double normZ = lastTargetFace != null ?
                lastTargetFace.getStepZ() : 0;
                double originX = lastTargetHitVec != null ? lastTargetHitVec.x : lastTargetPos.getX() + 0.5;
                double originY = lastTargetHitVec != null ? lastTargetHitVec.y : lastTargetPos.getY() + 0.5;
                double originZ = lastTargetHitVec != null ?
                lastTargetHitVec.z : lastTargetPos.getZ() + 0.5;
                
                try {
                    com.lightdust.client.particle.ActionDustParticle.CURRENT_BLOCK_COLOR = tempColor;
                    for (int i = 0; i < count; i++) {
                        double px = originX + (level.random.nextDouble() - 0.5) * 0.35;
                        double py = originY + (level.random.nextDouble() - 0.5) * 0.35;
                        double pz = originZ + (level.random.nextDouble() - 0.5) * 0.35;
                        double vx = (normX * 0.6 + (level.random.nextDouble() - 0.5)) * speed;
                        double vy = (normY * 0.6 + (level.random.nextDouble() - 0.5)) * speed;
                        double vz = (normZ * 0.6 + (level.random.nextDouble() - 0.5)) * speed;
                        
                        level.addParticle(p(ParticleInit.ACTION_DUST_PARTICLE, ParticleInit.ACTION_DUST_PARTICLE_HD), px, py, pz, vx, vy, vz);
                    }
                } finally {
                    com.lightdust.client.particle.ActionDustParticle.CURRENT_BLOCK_COLOR = -1;
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