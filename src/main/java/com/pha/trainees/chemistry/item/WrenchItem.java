package com.pha.trainees.chemistry.item;

import com.pha.trainees.chemistry.blockentity.LabReactorBlockEntity;
import com.pha.trainees.chemistry.container.IContainerAttachment;
import com.pha.trainees.chemistry.container.IContainerPort;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * 扳手（§19.33）：科技模组里"扳手"这个角色——<b>把机械件完整拆下来，而不是打碎</b>。
 *
 * <p>右键目标：</p>
 * <ul>
 *   <li>反应釜上的<b>密封釜头</b> → 取下并还给你；</li>
 *   <li><b>附着件 / 端口</b>（搅拌器、电加热夹套、阀门）→ 立即完整取下（按掉落表掉落）。</li>
 * </ul>
 *
 * <p>安装仍走各自的方式：方块直接放置、密封釜头手持右键装上。
 * 之所以单独做一个物品，是因为"拆卸"是一个明确的意图，不该和"潜行 + 空手"这种手势混在一起。</p>
 *
 * <p><b>已知简化</b>：扳手拆下的附着件<b>不保留其内部状态</b>（搅拌器余电、夹套设定点会丢），
 * 按掉落表给回方块本体。要保留就得给这些方块做"带方块实体的物品"，留到需要时再做。</p>
 */
public class WrenchItem extends Item {

    public WrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player == null) return InteractionResult.PASS;

        // ① 密封釜头（反应釜）：取下并还给玩家
        if (level.getBlockEntity(pos) instanceof LabReactorBlockEntity reactor && reactor.hasSealedHead()) {
            ItemStack head = reactor.removeHead();
            if (!head.isEmpty()) {
                if (!player.getInventory().add(head)) {
                    player.drop(head, false);
                }
                player.displayClientMessage(Component.translatable(
                        "message.trainees.reactor.head_off"), true);
            }
            return InteractionResult.CONSUME;
        }

        // ② 附着件 / 端口：完整拆下（按掉落表掉落本体，再移除方块）
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof IContainerAttachment || block instanceof IContainerPort) {
            Block.dropResources(state, level, pos);
            level.destroyBlock(pos, false);
            player.displayClientMessage(Component.translatable(
                    "message.trainees.wrench.removed"), true);
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }
}
