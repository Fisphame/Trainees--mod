package com.pha.trainees.util.game.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface IActivationHandler {
    /**
     * 结构激活时调用
     */
    default void onActivate(Level level, BlockPos matchPos) {}

    /**
     * 预激活检查
     */
    default boolean preActivateCheck(Level level, BlockPos matchPos) {
        return true;
    }

    /**
     * 激活失败时调用
     */
    default void onActivationFailed(Level level, BlockPos matchPos, String reason) {}

    /**
     * 结构被破坏时调用
     */
    default void onStructureBroken(Level level, BlockPos matchPos) {}

    /**
     * 逻辑 tick，用于高频处理（如合成检测）
     * @param activeTime 已激活时间（游戏刻）
     */
    default void onLogicTick(Level level, BlockPos matchPos, long activeTime) {}

    /**
     * 效果 tick，用于低频粒子效果
     * @param activeTime 已激活时间（游戏刻）
     */
    default void onEffectTick(Level level, BlockPos matchPos, long activeTime) {}

    /**
     * 逻辑 tick 间隔（游戏刻）
     */
    int getLogicTickInterval();

    /**
     * 效果 tick 间隔（游戏刻）
     */
    int getEffectTickInterval();

    /**
     * 是否允许重新激活已激活的结构
     */
    default boolean allowReactivate() {
        return false;
    }
}