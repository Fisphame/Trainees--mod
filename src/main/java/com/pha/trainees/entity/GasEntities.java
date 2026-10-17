package com.pha.trainees.entity;

import com.pha.trainees.util.game.BlockCourse;
import com.pha.trainees.util.game.Tools;
import com.pha.trainees.util.math.MathT;
import com.pha.trainees.util.math.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class GasEntities {
    public static class HydrogenEntity extends GasEntity {
        // 氢气的特定参数
        // 初始大小
        // 初始浓度
        // 清除浓度
        // 扩张速度
        public static final float INITIAL_SIZE = 1.0f;
        public static final float INITIAL_CONCENTRATION = 1.0f;
        public static final float MIN_CONCENTRATION = 0.05f;
        public static final float EXPANSION_RATE = 0.05f;


        private static final Pair EXPLOSION_CONCENTRATION_THRESHOLD = new Pair(0.04, 0.756);
        private static final int FIRE_CHECK_INTERVAL = 10;
        public int fireCheckTimer = 0;

        public HydrogenEntity(EntityType<?> entityType, Level level,
                              float initialSize,
                              float initialConcentration,
                              float minConcentration,
                              float expansionRate) {
            super(entityType, level, initialSize, initialConcentration, minConcentration, expansionRate);
        }

        public HydrogenEntity(EntityType<?> entityType, Level level,
                              float initialSize,
                              float initialConcentration,
                              float expansionRate) {
            super(entityType, level, initialSize, initialConcentration, MIN_CONCENTRATION, expansionRate);
        }

        public HydrogenEntity(EntityType<?> entityType, Level level) {
            super(entityType, level,
                    INITIAL_SIZE, INITIAL_CONCENTRATION,
                    MIN_CONCENTRATION, EXPANSION_RATE);
        }

        @Override
        public void tick() {
            super.tick();

            // 只在服务器端执行逻辑
            if (!this.level().isClientSide()) {
                // 获取当前浓度
                float currentConcentration = getConcentration();

                // 更新检查计时器
                fireCheckTimer++;

                // 如果浓度处于阈值且达到检查间隔，检查火源
                if (fireCheckTimer >= FIRE_CHECK_INTERVAL) {
                    // 重置计时器
                    fireCheckTimer = 0;


                    BlockPos fireSourcePos = checkForFireSource();
                    // 随机采样检查（更高效）
                    if (fireSourcePos != null) {
                        if(MathT.isInInterval(currentConcentration, EXPLOSION_CONCENTRATION_THRESHOLD)){
                            Tools.DoTnt_center(level(), fireSourcePos, currentConcentration * 5f + 4.0f);
                        }
                        else {
                            level().setBlock(fireSourcePos, Blocks.WATER.defaultBlockState(), 1);
                        }

                        this.discard();
                    }
                }
            }
        }

        private BlockPos checkForFireSource() {
            net.minecraft.world.phys.AABB boundingBox = getBoundingBox();

            // 计算碰撞箱的尺寸
            double width = boundingBox.maxX - boundingBox.minX;
            double height = boundingBox.maxY - boundingBox.minY;
            double depth = boundingBox.maxZ - boundingBox.minZ;

            // 采样数量基于碰撞箱体积，但限制最大采样数
            int sampleCount = (int)((width * height * depth) / 8.0);
            sampleCount = Math.min(Math.max(sampleCount, 8), 32); // 限制在8-32个采样点

            // 随机采样检查
            for (int i = 0; i < sampleCount; i++) {
                // 在碰撞箱内随机选取一个位置
                double randomX = boundingBox.minX + (level().random.nextDouble() * width);
                double randomY = boundingBox.minY + (level().random.nextDouble() * height);
                double randomZ = boundingBox.minZ + (level().random.nextDouble() * depth);

                BlockPos blockPos = BlockPos.containing(randomX, randomY, randomZ);

                // 检查当前方块和周围的方块
                BlockPos fireSourcePos = checkBlockAndNeighbors(blockPos);
                if (fireSourcePos != null) {
                    return fireSourcePos;
                }
            }

            return null;
        }

        // 检查方块及其周围方块是否是火源
        // 检查方块及其周围方块是否是火源
        private BlockPos checkBlockAndNeighbors(BlockPos centerPos) {
            // 检查中心方块
            if (isFireSource(centerPos)) {
                return centerPos;
            }

            // 检查周围6个方向的相邻方块
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        // 跳过中心方块（已检查）和对角线（可选）
                        if ((dx == 0 && dy == 0 && dz == 0) ||
                                (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 1)) {
                            continue;
                        }

                        BlockPos neighborPos = centerPos.offset(dx, dy, dz);
                        if (isFireSource(neighborPos)) {
                            return neighborPos;
                        }
                    }
                }
            }

            return null;
        }

        private boolean isFireSource(BlockPos pos) {
            // 确保位置在有效范围内
            if (pos.getY() < level().getMinBuildHeight() || pos.getY() > level().getMaxBuildHeight()) {
                return false;
            }

            return BlockCourse.isInFire(level(), pos);
        }
    }

    public static class OxygenEntity extends GasEntity {
        // 氧气的特定参数
        public static final float INITIAL_SIZE = 1.0f;
        public static final float INITIAL_CONCENTRATION = 1.0f;
        public static final float MIN_CONCENTRATION = 0.105f;
        public static final float EXPANSION_RATE = 0.03f;

        public OxygenEntity(EntityType<?> entityType, Level level,
                              float initialSize,
                              float initialConcentration,
                              float minConcentration,
                              float expansionRate) {
            super(entityType, level, initialSize, initialConcentration, minConcentration, expansionRate);
        }

        public OxygenEntity(EntityType<?> entityType, Level level,
                              float initialSize,
                              float initialConcentration,
                              float expansionRate) {
            super(entityType, level, initialSize, initialConcentration, MIN_CONCENTRATION, expansionRate);
        }

        public OxygenEntity(EntityType<?> entityType, Level level) {
            super(entityType, level,
                    INITIAL_SIZE, INITIAL_CONCENTRATION,
                    MIN_CONCENTRATION, EXPANSION_RATE);
        }

        @Override
        public void tick(){
            super.tick();
        }
    }
}
