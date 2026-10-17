package com.pha.trainees.chemistry.block;

import com.pha.trainees.chemistry.blockentity.StirrerBlockEntity;
import com.pha.trainees.chemistry.container.IContainerAttachment;
import com.pha.trainees.registry.ModChemistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 搅拌器（§19.29）：贴在容器邻面上的附着件，<b>有电就转</b>（耗 FE），
 * 作用是提升<b>传热系数</b>与<b>气液传质速率</b>——<b>不动反应速率</b>（理由见 {@link IContainerAttachment}）。
 *
 * <p>不设开关：<b>电就是开关</b>。断电即停，因子立刻回到 1.0（等于没装）。</p>
 */
public class StirrerBlock extends BaseEntityBlock implements IContainerAttachment {

    public StirrerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        // BaseEntityBlock 默认 INVISIBLE；本方块用普通模型渲染（无 BER）
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new StirrerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state,
                                                                  @NotNull BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModChemistry.ModChemistryBlockEntities.STIRRER.get(),
                StirrerBlockEntity::tick);
    }

    @Override
    public double heatTransferFactor(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof StirrerBlockEntity stirrer
                ? stirrer.heatTransferFactor() : 1.0;
    }

    @Override
    public double gasTransferFactor(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof StirrerBlockEntity stirrer
                ? stirrer.gasTransferFactor() : 1.0;
    }

    /** 右键查看是否在转（诊断用；真正的读数在分析仪里） */
    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                          @NotNull Player player, @NotNull InteractionHand hand,
                                          @NotNull BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof StirrerBlockEntity stirrer) {
            player.displayClientMessage(Component.translatable(
                    stirrer.isRunning() ? "message.trainees.stirrer.running" : "message.trainees.stirrer.idle",
                    stirrer.getEnergyStored(), stirrer.getMaxEnergyStored()), true);
        }
        return InteractionResult.SUCCESS;
    }
}
