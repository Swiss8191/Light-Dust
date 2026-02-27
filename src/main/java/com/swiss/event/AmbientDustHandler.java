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

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused() || mc.player == null || mc.level == null) return;
        if (!LightDustConfig.SPEC.isLoaded()) return;
        Player player = mc.player;
        Level level = mc.level;
        
        int radius = LightDustConfig.AMBIENT_RADIUS.get();
        int maxCap = LightDustConfig.AMBIENT_BLOCK_CAP.get();
        int diffThreshold = LightDustConfig.DAYTIME_LIGHT_DIFF.get();
        int radiusSqr = radius * radius;

        BlockPos playerPos = player.blockPosition();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        long tick = level.getGameTime();
        int tickMod = (int)(tick % 40);
        long time = level.getDayTime() % 24000;
        boolean isDay = time < 13000 || time > 23000; 

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    
                    if (Math.abs(x + y + z) % 40 != tickMod) continue;
                    if (x*x + y*y + z*z > radiusSqr) continue;
                    mutablePos.set(playerPos.getX() + x, playerPos.getY() + y, playerPos.getZ() + z);

                    if (level.dimension() == Level.OVERWORLD && isDay && level.canSeeSky(mutablePos)) {
                        continue;
                    }

                    long posKey = BlockPos.asLong(mutablePos.getX(), mutablePos.getY(), mutablePos.getZ());
                    int currentCount = DustParticle.AMBIENT_COUNTS.get(posKey);
                    
                    if (currentCount >= maxCap) continue;

                    int blockLight = level.getBrightness(LightLayer.BLOCK, mutablePos);
                    if (blockLight < LightDustConfig.MIN_BLOCK_LIGHT.get()) continue;

                    if (isDay) {
                        int skyLight = level.getBrightness(LightLayer.SKY, mutablePos);
                        if ((blockLight - skyLight) <= diffThreshold) continue;
                    }

                    if (level.getFluidState(mutablePos).is(FluidTags.WATER)) continue;
                    BlockState state = level.getBlockState(mutablePos);
                    if (!state.getCollisionShape(level, mutablePos).isEmpty()) continue;

                    int targetCap;
                    if (blockLight >= 9) targetCap = maxCap;
                    else if (blockLight >= 7) targetCap = Math.max(1, (int)(maxCap * 0.6f));
                    else targetCap = Math.max(1, (int)(maxCap * 0.3f));
                    if (currentCount < targetCap) {
                        DustParticle.PENDING_POS = mutablePos.immutable();
                        int spawnCount = 1;
                        if (currentCount == 0) {
                            spawnCount = 4;
                        } else if (currentCount < targetCap / 2) {
                            spawnCount = 2;
                        }

                        spawnCount = Math.min(spawnCount, targetCap - currentCount);
                        for (int i = 0; i < spawnCount; i++) {
                            double px = mutablePos.getX() + level.random.nextDouble();
                            double py = mutablePos.getY() + level.random.nextDouble();
                            double pz = mutablePos.getZ() + level.random.nextDouble();
                            level.addParticle(
                                ParticleInit.DUST_PARTICLE.get(), 
                                px, py, pz, 
                                0, 0, 0
                            );
                        }
                        
                        DustParticle.PENDING_POS = null;
                    }
                }
            }
        }

        if (tick % 60 == 0) {
            double hardCap = LightDustConfig.AMBIENT_HARD_CAP.get();
            double pruneDistSqr = (hardCap + 1) * (hardCap + 1);
            
            var iterator = DustParticle.AMBIENT_COUNTS.long2IntEntrySet().iterator();
            while (iterator.hasNext()) {
                long key = iterator.next().getLongKey();
                if (BlockPos.of(key).distSqr(playerPos) > pruneDistSqr) {
                    iterator.remove();
                }
            }
        }

        // block go poof
        if (player.swingTime == 1 && mc.hitResult != null && mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            BlockPos breakPos = ((net.minecraft.world.phys.BlockHitResult) mc.hitResult).getBlockPos();
            if (level.getBlockState(breakPos).isAir()) {
                int count = LightDustConfig.BREAK_PARTICLE_COUNT.get();
                double speed = LightDustConfig.BREAK_PARTICLE_SPEED.get();
                for (int i = 0; i < count; i++) {
                    double px = breakPos.getX() + level.random.nextDouble();
                    double py = breakPos.getY() + level.random.nextDouble();
                    double pz = breakPos.getZ() + level.random.nextDouble();
                    
                    // yeet
                    double vx = (level.random.nextDouble() - 0.5) * speed;
                    double vy = (level.random.nextDouble() - 0.5) * speed;
                    double vz = (level.random.nextDouble() - 0.5) * speed;
                    
                    level.addParticle(ParticleInit.DUST_PARTICLE.get(), px, py, pz, vx, vy, vz);
                }
            }
        }
    }

    @SubscribeEvent public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) { clearMaps(); }
    @SubscribeEvent public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) { clearMaps(); }
    @SubscribeEvent public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) { 
        if (event.getEntity().level().isClientSide) clearMaps();
    }
    @SubscribeEvent public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) { 
        if (event.getEntity().level().isClientSide) clearMaps();
    }
    @SubscribeEvent public static void onWorldUnload(LevelEvent.Unload event) { 
        if (event.getLevel().isClientSide()) clearMaps();
    }

    private static void clearMaps() {
        DustParticle.AMBIENT_COUNTS.clear();
        DustParticle.PENDING_POS = null;
    }
}
