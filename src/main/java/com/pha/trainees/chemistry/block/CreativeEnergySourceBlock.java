package com.pha.trainees.chemistry.block;

import com.pha.trainees.chemistry.blockentity.CreativeEnergySourceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 创造能源方块（§19.38 附）：放在那里就是一个**无限 FE 源**，供上机测试电设备（恒温浴/夹套/打包机）。
 *
 * <p>无交互、无 GUI：接线缆或用任何会抽电的机器贴着它即可。</p>
 */
public class CreativeEnergySourceBlock extends BaseEntityBlock {

    public CreativeEnergySourceBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeEnergySourceBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
