package com.pha.trainees.block;

import com.pha.trainees.blockentity.ManaExtractorBlockEntity;
import com.pha.trainees.mana.TierMath;
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
 * 提取机（§17.1）：按"提取"配方把物品榨成**精炼精华（RE）**，可能出副产物。
 *
 * <p>5 个注册方块 = 5 个档位（初级/中级/高级/终极/创造，{@link TierMath} 的 0~4 档）——
 * 档位是**方块属性**，不是升级件：作者 2026-10-02 定的"5 个注册方块"。</p>
 *
 * <p>交互（V1 不用 GUI）：手持物品右键 = 放进输入槽；空手右键 = 取出产物。
 * 缓存里的 RE 用玩家按键（R 注能 / 潜行+R 抽取）进出——它本身就是一个 {@code IManaStorage} 方块，
 * 所以 Jade 上会直接显示它的存量。</p>
 */
public class ManaExtractorBlock extends BaseEntityBlock {

    private final int tier;

    public ManaExtractorBlock(int tier, Properties properties) {
        super(properties);
        this.tier = TierMath.clampTier(tier);
    }

    public int getTier() {
        return tier;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // 档位由方块自己带（BE 从 BlockState 读），所以 5 个方块共用一个方块实体类型
        return new ManaExtractorBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** 服务端 tick：提取按 tick 推进。 */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof ManaExtractorBlockEntity extractor) extractor.serverTick();
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof ManaExtractorBlockEntity)) {
            return InteractionResult.PASS;
        }
        // §17.1 魔法机器 GUI 化（作者定）：操作一律进 GUI，右键不再塞/取物品
        if (player instanceof ServerPlayer serverPlayer) {
            // 必须用 NetworkHooks.openScreen：原版 openMenu 不携带额外数据，客户端会因为 buf=null 崩
            net.minecraftforge.network.NetworkHooks.openScreen(serverPlayer,
                    new net.minecraft.world.SimpleMenuProvider(
                            (id, inventory, p) -> new com.pha.trainees.menu.MachineMenu(
                                    id, inventory, pos, com.pha.trainees.menu.MachineMenu.Kind.EXTRACTOR),
                            Component.translatable(state.getBlock().getDescriptionId())),
                    pos);
        }
        return InteractionResult.CONSUME;
    }
}
