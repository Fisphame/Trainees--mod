package com.pha.trainees.chemistry.block;

import com.pha.trainees.chemistry.blockentity.CreativeHeatSourceBlockEntity;
import com.pha.trainees.chemistry.util.AnalyzerAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 创造热源（§19.24）：把<b>任意目标温度</b>施加给其正上方的容器，含 0 ℃ 以下的低温，
 * 用于在游戏内精确验证温度依赖的化学（例如含氯消毒液的 60~80 ℃ 窗口、冷冻混合物、液氮）。
 *
 * <p>交互：右键升档、潜行右键降档（档位表见 {@link CreativeHeatSourceBlockEntity#PRESETS}）；
 * 需要任意值（非档位）时用 {@code /chemtester heat <K>} 直接指定。空手右键即可，无需 GUI。</p>
 */
public class CreativeHeatSourceBlock extends BaseEntityBlock {

    public CreativeHeatSourceBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeHeatSourceBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof CreativeHeatSourceBlockEntity source)) {
            return InteractionResult.PASS;
        }

        // 分析仪优先（与烧杯/电解槽一致）：右键看诊断
        InteractionResult analyzerResult = AnalyzerAccess.tryUse(level, pos, player, player.getItemInHand(hand));
        if (analyzerResult != InteractionResult.PASS) {
            return analyzerResult;
        }

        double kelvin = source.cyclePreset(player.isShiftKeyDown() ? -1 : 1);
        player.displayClientMessage(Component.translatable(
                "message.trainees.heat_source.set",
                format(kelvin), format(kelvin - 273.15)), true);
        return InteractionResult.CONSUME;
    }

    private static String format(double value) {
        return String.format("%.1f", value);
    }
}
