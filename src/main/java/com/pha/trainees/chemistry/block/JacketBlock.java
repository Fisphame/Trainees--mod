package com.pha.trainees.chemistry.block;

import com.pha.trainees.chemistry.blockentity.JacketBlockEntity;
import com.pha.trainees.chemistry.container.IContainerAttachment;
import com.pha.trainees.chemistry.util.AnalyzerAccess;
import com.pha.trainees.registry.ModChemistry;
import com.pha.trainees.registry.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
 * 电加热夹套（§19.29 第 2 步）：贴在容器邻面上的附着件，同时是<b>设定点型热源</b>。
 *
 * <p>作为热源它从<b>侧面</b>供热（前提是实现了 {@link IContainerAttachment}——
 * 所以把恒温浴挪到容器旁边不会突然开始加热，既有行为不变）；
 * 作为附着件它提供<b>传热面积加成</b>（多贴一面多一块面积，加性叠加）。</p>
 *
 * <p>交互（与恒温浴同一风格，V1 无 GUI）：空手右键 +5 K、潜行右键 −5 K；分析仪右键看诊断。</p>
 */
public class JacketBlock extends BaseEntityBlock implements IContainerAttachment {

    public JacketBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new JacketBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state,
                                                                  @NotNull BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModChemistry.ModChemistryBlockEntities.JACKET.get(),
                JacketBlockEntity::tick);
    }

    /** 传热面积加成：没电就等于没装 */
    @Override
    public double heatTransferAreaBonus(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof JacketBlockEntity jacket && jacket.isRunning()
                ? Math.max(0.0, ModConfig.COMMON.jacketAreaBonus.get())
                : 0.0;
    }

    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                          @NotNull Player player, @NotNull InteractionHand hand,
                                          @NotNull BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof JacketBlockEntity jacket)) {
            return InteractionResult.PASS;
        }

        // 分析仪优先（与其他化学方块一致）
        ItemStack held = player.getItemInHand(hand);
        InteractionResult analyzerResult = AnalyzerAccess.tryUse(level, pos, player, held);
        if (analyzerResult != InteractionResult.PASS) {
            return analyzerResult;
        }

        if (!held.isEmpty()) {
            return InteractionResult.CONSUME;   // 手持无关物品时不动设定点，避免误触
        }

        double setpoint = jacket.adjustSetpoint(player.isShiftKeyDown()
                ? -JacketBlockEntity.STEP_K
                : JacketBlockEntity.STEP_K);
        player.displayClientMessage(Component.translatable(
                jacket.isRunning() ? "message.trainees.jacket.set" : "message.trainees.jacket.set.idle",
                format(setpoint), format(setpoint - 273.15),
                jacket.getEnergyStored(), jacket.getMaxEnergyStored()), true);
        return InteractionResult.CONSUME;
    }

    private static String format(double value) {
        return String.format("%.1f", value);
    }
}
