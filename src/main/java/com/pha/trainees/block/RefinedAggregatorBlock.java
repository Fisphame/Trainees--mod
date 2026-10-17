package com.pha.trainees.block;

import com.pha.trainees.blockentity.RefinedAggregatorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 聚合机（§17.1）：把 **RE 聚合成物品**，只有 1 个档位、处理很快。
 * 默认配方：1600 RE → 1 个**精炼点**（加点系统的货币）。
 *
 * <p>能量进出走玩家按键（R 注能 / 潜行+R 抽取）——它本身是 {@code IManaStorage} 方块，Jade 直接显示缓存；
 * 交互只有一条：**空手右键取产物**（它不需要输入物品）。</p>
 */
public class RefinedAggregatorBlock extends BaseEntityBlock {

    public RefinedAggregatorBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RefinedAggregatorBlockEntity(pos, state);
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
            if (be instanceof RefinedAggregatorBlockEntity aggregator) aggregator.serverTick();
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof RefinedAggregatorBlockEntity)) {
            return InteractionResult.PASS;
        }
        // §17.1 魔法机器 GUI 化（作者定）：模板槽/产物槽都在 GUI 里操作，右键不再塞/取
        if (player instanceof ServerPlayer serverPlayer) {
            net.minecraftforge.network.NetworkHooks.openScreen(serverPlayer,
                    new net.minecraft.world.SimpleMenuProvider(
                            (id, inventory, p) -> new com.pha.trainees.menu.MachineMenu(
                                    id, inventory, pos, com.pha.trainees.menu.MachineMenu.Kind.AGGREGATOR),
                            Component.translatable(state.getBlock().getDescriptionId())),
                    pos);
        }
        return InteractionResult.CONSUME;
    }
}
