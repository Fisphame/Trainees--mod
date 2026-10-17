package com.pha.trainees.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import javax.annotation.Nullable;

public class AuriversiteBlock extends Block {
    // 定义方向属性，这里使用水平方向（北东南西）
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public AuriversiteBlock(Properties properties) {
        super(properties);
        // 设置默认状态，面向北方
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 获取玩家的水平朝向（忽略垂直方向）
        Direction facing = context.getHorizontalDirection();
        // 获取玩家的对面方向（使方块面对玩家）
        return this.defaultBlockState()
                .setValue(FACING, facing.getOpposite());
    }
}
