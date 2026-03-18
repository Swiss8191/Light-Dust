package com.lightdust.event;

import com.lightdust.LightDust;
import com.lightdust.client.particle.DustParticle;
import com.lightdust.config.LightDustConfig;
import com.lightdust.init.ParticleInit;
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
            this.x = x; this.y = y; this.z = z; this.speed = speed;
        }
    }
    public static final java.util.List<MovingEntityData> ACTIVE_MOVING_ENTITIES = new java.util.ArrayList<>();

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();

        if (mc.isPaused() || mc.player == null || mc.level == null) return;

        if (!LightDustConfig.SPEC.isLoaded()) return;

        Player player = mc.player;
        Level level = mc.level;

        ACTIVE_MOVING_ENTITIES.clear();
        if (LightDustConfig.ENABLE_ENTITY_DISTURBANCE.get()) {
            double scanRadius = LightDustConfig.AMBIENT_RADIUS.get();
            net.minecraft.world.phys.AABB playerBounds = player.getBoundingBox().inflate(scanRadius);
            java.util.List<net.minecraft.world.entity.Entity> entities = level.getEntities((net.minecraft.world.entity.Entity)null, playerBounds, 
                e -> e instanceof net.minecraft.world.entity.LivingEntity || e instanceof net.minecraft.world.entity.projectile.Projectile);
            
            for (net.minecraft.world.entity.Entity e : entities) {
                double speed = e.getDeltaMovement().length();
                if (speed > 0.03) {
                    ACTIVE_MOVING_ENTITIES.add(new MovingEntityData(e.getX(), e.getY() + e.getBbHeight() / 2.0, e.getZ(), speed));
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

                    double radius = level.random.nextDouble() * maxRadius;
                    double angle = level.random.nextDouble() * Math.PI * 2;
                    double dX = Math.cos(angle) * radius;
                    double dZ = Math.sin(angle) * radius;
                    double px = player.getX() + dX;
                    double py = player.getY() + 0.1;
                    double pz = player.getZ() + dZ;
                    double forceMult = Math.max(0.2, (maxRadius - radius) / maxRadius);
                    double scale = Math.min(2.5, 1.0 + (lastFallDistance * 0.02));
                    double vx = (dX / (radius == 0 ? 1 : radius)) * outBase * forceMult * scale * (0.8 + level.random.nextDouble() * 0.4);
                    double vy = upBase * forceMult * scale * (0.8 + level.random.nextDouble() * 0.4);
                    double vz = (dZ / (radius == 0 ? 1 : radius)) * outBase * forceMult * scale * (0.8 + level.random.nextDouble() * 0.4);
                    level.addParticle(ParticleInit.ACTION_DUST_PARTICLE.get(), px, py, pz, vx, vy, vz);
                }

                DustParticle.LANDING_IMPACT_POS = player.position();
                DustParticle.LANDING_IMPACT_TICK = level.getGameTime();
                DustParticle.LANDING_IMPACT_FORCE = LightDustConfig.HEAVY_LANDING_AMBIENT_PUSH.get();
                DustParticle.LANDING_IMPACT_RADIUS = LightDustConfig.HEAVY_LANDING_AMBIENT_RADIUS.get();
            }
        }
        lastFallDistance = player.onGround() ? 0.0f : currentFallDistance;

        int radius = LightDustConfig.AMBIENT_RADIUS.get();
        int maxCap = LightDustConfig.AMBIENT_BLOCK_CAP.get();
        int diffThreshold = LightDustConfig.DAYTIME_LIGHT_DIFF.get();
        int radiusSqr = radius * radius;
        int falloffDist = LightDustConfig.FALLOFF_DISTANCE.get();
        int falloffDistSqr = falloffDist * falloffDist;
        double falloffMult = LightDustConfig.FALLOFF_MULTIPLIER.get();
        BlockPos playerPos = player.blockPosition();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        long tick = level.getGameTime();
        int tickMod = (int) (tick % 40);
        long time = level.getDayTime() % 24000;
        boolean isDay = time < 13000 || time > 23000;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.abs(x + y + z) % 40 != tickMod) continue;
                    int distSqr = x * x + y * y + z * z;
                    if (distSqr > radiusSqr) continue;
                    
                    mutablePos.set(playerPos.getX() + x, playerPos.getY() + y, playerPos.getZ() + z);
                    boolean canSeeSky = level.canSeeSky(mutablePos);
                    
                    if (canSeeSky) {
                        if (level.isThundering() && LightDustConfig.DISABLE_DURING_THUNDER.get()) continue;
                        else if (level.isRaining() && !level.isThundering() && LightDustConfig.DISABLE_DURING_RAIN.get()) continue;
                    }

                    if (level.dimension() == Level.OVERWORLD && isDay && canSeeSky && !level.isRaining()) continue; 

                    int localMaxCap = maxCap;
                    if (mutablePos.getY() < 0) {
                        double depthFactor = Math.min(1.0, (double) (-mutablePos.getY()) / 64.0);
                        localMaxCap = (int) (maxCap * (1.0 + depthFactor));
                    }

                    long posKey = BlockPos.asLong(mutablePos.getX(), mutablePos.getY(), mutablePos.getZ());
                    int currentCount = DustParticle.AMBIENT_COUNTS.getOrDefault(posKey, 0);

                    if (currentCount >= localMaxCap) continue;

                    int blockLight = level.getBrightness(LightLayer.BLOCK, mutablePos);
                    int minLight = LightDustConfig.MIN_BLOCK_LIGHT.get();
                    boolean isDarkCave = mutablePos.getY() < 60 && !canSeeSky && blockLight < minLight;
                    if (blockLight < minLight && !isDarkCave) continue;
                    
                    if (isDay && !isDarkCave && !level.isRaining()) {
                        int skyLight = level.getBrightness(LightLayer.SKY, mutablePos);
                        if ((blockLight - skyLight) <= diffThreshold) continue;
                    }

                    if (level.getFluidState(mutablePos).is(FluidTags.WATER)) continue;
                    BlockState state = level.getBlockState(mutablePos);
                    if (!state.getCollisionShape(level, mutablePos).isEmpty()) continue;

                    int targetCap;
                    if (isDarkCave) targetCap = Math.max(1, (int) (localMaxCap * 0.15f));
                    else if (blockLight >= 9) targetCap = localMaxCap;
                    else if (blockLight >= 7) targetCap = Math.max(1, (int) (localMaxCap * 0.6f));
                    else targetCap = Math.max(1, (int) (localMaxCap * 0.3f));

                    if (distSqr > falloffDistSqr) targetCap = Math.max(1, (int) (targetCap * falloffMult));

                    if (currentCount < targetCap) {
                        if (LightDustConfig.ENABLE_OCCLUSION_CULLING.get()) {
                            net.minecraft.world.phys.Vec3 eyePos = player.getEyePosition();
                            net.minecraft.world.phys.Vec3 targetPosVec = new net.minecraft.world.phys.Vec3(mutablePos.getX() + 0.5, mutablePos.getY() + 0.5, mutablePos.getZ() + 0.5);
                            net.minecraft.world.phys.BlockHitResult sightCheck = level.clip(new net.minecraft.world.level.ClipContext(
                                    eyePos, targetPosVec, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                                    net.minecraft.world.level.ClipContext.Fluid.NONE, player
                            ));

                            if (sightCheck.getType() != net.minecraft.world.phys.HitResult.Type.MISS && !sightCheck.getBlockPos().equals(mutablePos)) {
                                continue; 
                            }
                        }

                        DustParticle.PENDING_POS = mutablePos.immutable();
                        int spawnCount = 1;
                        if (currentCount == 0) spawnCount = isDarkCave ? 1 : 4;
                        else if (currentCount < targetCap / 2) spawnCount = isDarkCave ? 1 : 2;

                        spawnCount = Math.min(spawnCount, targetCap - currentCount);
                        for (int i = 0; i < spawnCount; i++) {
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
                if (level.getBlockState(lastTargetPos).isAir()) lastTargetPos = null;
            } else lastTargetPos = null;
        } else lastTargetPos = null;
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
        ACTIVE_MOVING_ENTITIES.clear(); // Clear entity cache
    }
}