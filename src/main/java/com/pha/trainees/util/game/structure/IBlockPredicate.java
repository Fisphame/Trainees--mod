package com.pha.trainees.util.game.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@FunctionalInterface
public interface IBlockPredicate {
    boolean test(Level level, BlockPos pos, BlockState state);

    // 常用条件工厂方法
    static IBlockPredicate block(Block block) {
        return (level, pos, state) -> state.is(block);
    }

    static IBlockPredicate blockEntity(Class<? extends BlockEntity> type) {
        return (level, pos, state) -> level.getBlockEntity(pos) != null &&
                type.isAssignableFrom(level.getBlockEntity(pos).getClass());
    }

    static IBlockPredicate alwaysTrue() {
        return (level, pos, state) -> true;
    }

    static IBlockPredicate alwaysFalse() {
        return (level, pos, state) -> false;
    }
}
