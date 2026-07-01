package com.lightdust.client.environment;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.Arrays;

public class GlobalWindGrid {
    private static final int RADIUS = 24; // 48x48x48 block area
    private static final int DIAMETER = RADIUS * 2;
    private static final int MAX_CELLS = DIAMETER * DIAMETER * DIAMETER;
 
    private static final float[] gridX = new float[MAX_CELLS];
    private static final float[] gridY = new float[MAX_CELLS];
    private static final float[] gridZ = new float[MAX_CELLS];

    private static BlockPos center = BlockPos.ZERO;
    private static int updateIndex = 0;

    public static void tick(Level level, BlockPos playerPos) {
        if (center.distSqr(playerPos) > 144) { 
            center = playerPos;
            Arrays.fill(gridX, 0);
            Arrays.fill(gridY, 0);
            Arrays.fill(gridZ, 0);
            updateIndex = 0;
        }

        int cellsPerTick = com.lightdust.config.LightDustExperimentalConfig.EXPERIMENTAL.gridCellsPerTick.get();
        int maxProbe = com.lightdust.config.LightDustExperimentalConfig.EXPERIMENTAL.gridProbeDepth.get();

        BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();

        for (int i = 0; i < cellsPerTick; i++) {
            if (updateIndex >= MAX_CELLS) updateIndex = 0;

            int x = updateIndex % DIAMETER;
            int y = (updateIndex / DIAMETER) % DIAMETER;
            int z = updateIndex / (DIAMETER * DIAMETER);

            mut.set(center.getX() + x - RADIUS, center.getY() + y - RADIUS, center.getZ() + z - RADIUS);
            
            if (level.getBlockState(mut).isAir()) {
                int spacePX = probeSpace(level, mut, Direction.EAST, maxProbe);
                int spaceNX = probeSpace(level, mut, Direction.WEST, maxProbe);
                int spacePY = probeSpace(level, mut, Direction.UP, maxProbe);
                int spaceNY = probeSpace(level, mut, Direction.DOWN, maxProbe);
                int spacePZ = probeSpace(level, mut, Direction.SOUTH, maxProbe);
                int spaceNZ = probeSpace(level, mut, Direction.NORTH, maxProbe);

                float pullX = (spacePX - spaceNX) / (float) maxProbe;
                float pullY = (spacePY - spaceNY) / (float) maxProbe;
                float pullZ = (spacePZ - spaceNZ) / (float) maxProbe;

                float repelX = (1.0f / Math.max(1, spaceNX)) - (1.0f / Math.max(1, spacePX));
                float repelY = (1.0f / Math.max(1, spaceNY)) - (1.0f / Math.max(1, spacePY));
                float repelZ = (1.0f / Math.max(1, spaceNZ)) - (1.0f / Math.max(1, spacePZ));

                gridX[updateIndex] = (pullX * 1.5f) + repelX;
                gridY[updateIndex] = (pullY * 0.5f) + repelY;
                gridZ[updateIndex] = (pullZ * 1.5f) + repelZ;
            }

            updateIndex++;
        }
    }

    private static int probeSpace(Level level, BlockPos pos, Direction dir, int max) {
        BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos().set(pos);
        for (int i = 0; i < max; i++) {
            mut.move(dir);
            if (!level.getBlockState(mut).getCollisionShape(level, mut).isEmpty()) {
                return i;
            }
        }
        return max;
    }

    // Array Lookup for Particles
    public static float[] getDraft(BlockPos pos) {
        int lx = pos.getX() - center.getX() + RADIUS;
        int ly = pos.getY() - center.getY() + RADIUS;
        int lz = pos.getZ() - center.getZ() + RADIUS;

        // Check if particle is inside the grid bounds
        if (lx >= 0 && lx < DIAMETER && ly >= 0 && ly < DIAMETER && lz >= 0 && lz < DIAMETER) {
            int idx = lx + ly * DIAMETER + lz * DIAMETER * DIAMETER;
            return new float[]{gridX[idx], gridY[idx], gridZ[idx]};
        }
        return new float[]{0, 0, 0};
    }
}