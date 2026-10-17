package com.pha.trainees.block;

import com.pha.trainees.machine.essence.EssenceNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 精练精华网导线（§17.1 ④）：网络连接的"物理载体"。
 *
 * <p>它自己**不存 RE**、也不做任何逻辑——只实现 {@link EssenceNetwork.Conductive}，
 * 于是端口洪泛时会沿着它一路找过去（见 {@code EssenceNetwork.scan}）。
 * 这样"线缆"是纯标记，将来要做线缆载流/损耗也只改端口那一处。</p>
 *
 * <p>形状取 6×6 的细杆（视觉上像线），碰撞箱比整块小，方便贴着机器铺。</p>
 */
public class EssenceCableBlock extends Block implements EssenceNetwork.Conductive {

    private static final VoxelShape SHAPE = Block.box(5.0, 5.0, 5.0, 11.0, 11.0, 11.0);

    public EssenceCableBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();   // 不挡路（玩家可以走过去）
    }
}
