package com.pha.trainees.util.game.chemistry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 机械方块中化学反应所需的条件接口。
 */
public interface MachineCondition {

    /**
     * 检查条件是否满足（在反应匹配阶段调用）
     * @param machine 机械方块实体
     * @param level 世界
     * @param pos 机械位置
     * @return true 表示满足，可以进行反应
     */
    boolean test(BlockEntity machine, Level level, BlockPos pos);

    /**
     * 在反应开始时调用（可选，可用于开始计时、粒子效果等）
     */
    default void onReactionStart(BlockEntity machine, Level level, BlockPos pos) {}

    /**
     * 在反应完成时调用（可选，可用于消耗催化剂、触发后续效果等）
     */
    default void onReactionComplete(BlockEntity machine, Level level, BlockPos pos) {}
}
