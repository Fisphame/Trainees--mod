package com.pha.trainees.block;

import com.pha.trainees.blockentity.EssenceNetworkPortBlockEntity;
import com.pha.trainees.menu.MachineMenu;
import com.pha.trainees.multiblock.EssencePortPattern;
import com.pha.trainees.util.game.structure.MultiblockPattern;
import com.pha.trainees.util.game.structure.MultiblockStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 精练精华网端口（§17.1 ④）：多方块结构的**主方块**，也是整个网络的唯一聚合点。
 *
 * <p>结构规格（作者 2026-10-03 定）：3×3 底 × 高 4；**正面的第 2 层中间必须是端口主方块**；
 * **3×3 的中间那格（纵向全部 4 层）必须是端口墙壁方块**；其余格子必须是
 * "端口多方块结构方块"tag 里的任意一种；功能方块上限见 {@link EssencePortPattern}。</p>
 *
 * <p>激活状态**不持久化**：方块实体每 20 tick 重算结构（唯一真相），并用"成形沿"做一次性动作；
 * 端口自己的邻居一变就 {@link EssenceNetworkPortBlockEntity#invalidateStructure()} 提前重查。</p>
 */
public class EssenceNetworkPortBlock extends BaseEntityBlock {

    public EssenceNetworkPortBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EssenceNetworkPortBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof EssenceNetworkPortBlockEntity port) port.serverTick();
        };
    }

    /** 端口自己的邻居变动 → 让方块实体下一次 tick 立刻重查结构。 */
    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof EssenceNetworkPortBlockEntity port) {
            port.invalidateStructure();
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof EssenceNetworkPortBlockEntity port)) {
            return InteractionResult.PASS;
        }

        // 1) 结构检查并回话
        @Nullable MultiblockPattern.MatchResult match =
                MultiblockStructure.checkStructure(level, pos, EssencePortPattern.STRUCTURE_ID);
        if (match == null) {
            player.displayClientMessage(
                    Component.translatable("message.trainees.essence_port.incomplete"), false);
        } else {
            @Nullable String reason = EssencePortPattern.validateFunctionBlocks(level, match);
            player.displayClientMessage(Component.translatable(reason != null
                    ? reason : "message.trainees.essence_port.formed"), false);
            port.refresh();
        }

        // 2) 打开 GUI（能量与网络信息；走 NetworkHooks 才能带坐标，见 docs/GUI.md）
        if (player instanceof ServerPlayer serverPlayer) {
            net.minecraftforge.network.NetworkHooks.openScreen(serverPlayer,
                    new net.minecraft.world.SimpleMenuProvider(
                            (id, inventory, p) -> new MachineMenu(id, inventory, pos, MachineMenu.Kind.PORT),
                            Component.translatable(state.getBlock().getDescriptionId())),
                    pos);
        }
        return InteractionResult.CONSUME;
    }
}
