package com.pha.trainees.block;

import com.pha.trainees.blockentity.EnrichmentChamberBlockEntity;
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
 * 富集仓（§17.1）：把多个原料聚成"富集态"，之后提取能拿更多 RE、更快、概率更高。
 *
 * <p>**5 个注册方块 = 5 个档位**（与提取机同一套档位观）。档位函数按作者口径：
 * 所需 RE 与处理时间都乘 `(5x²−45x+100)`——高级档更省电也更快，创造档免能量。</p>
 *
 * <p>交互（V1 无 GUI）：手持物品右键 = 依次放进**消耗槽 → 催化剂槽**（哪个空放哪个）；
 * 空手右键 = 取产物/取回原料；流体进出交给流体能力（管道/将来的线缆）。</p>
 */
public class EnrichmentChamberBlock extends BaseEntityBlock {

    private final int tier;

    public EnrichmentChamberBlock(int tier, Properties properties) {
        super(properties);
        this.tier = TierMath.clampTier(tier);
    }

    public int getTier() {
        return tier;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnrichmentChamberBlockEntity(pos, state);
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
            if (be instanceof EnrichmentChamberBlockEntity chamber) chamber.serverTick();
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof EnrichmentChamberBlockEntity)) {
            return InteractionResult.PASS;
        }
        // §17.1 魔法机器 GUI 化（作者定）：物品槽与流体容器槽都在 GUI 里操作
        if (player instanceof ServerPlayer serverPlayer) {
            net.minecraftforge.network.NetworkHooks.openScreen(serverPlayer,
                    new net.minecraft.world.SimpleMenuProvider(
                            (id, inventory, p) -> new com.pha.trainees.menu.MachineMenu(
                                    id, inventory, pos, com.pha.trainees.menu.MachineMenu.Kind.ENRICHMENT),
                            Component.translatable(state.getBlock().getDescriptionId())),
                    pos);
        }
        return InteractionResult.CONSUME;
    }
}
