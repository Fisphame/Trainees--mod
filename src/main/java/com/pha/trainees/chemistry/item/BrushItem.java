package com.pha.trainees.chemistry.item;

import com.pha.trainees.chemistry.blockentity.BeakerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * 清洁刷（§19.33）：把"清洗"从"潜行 + 空手右键"这种容易误触的手势里拿出来，
 * 交给一个明确的工具物品。
 *
 * <p>右键容器 → {@link BeakerBlockEntity#cleanVessel()}：
 * 可拆盖能擦到一点不剩；装了密封釜头（全焊）只能冲掉浮的、死角留在地板上。
 * 提示语只说成功与否，**不解释后果**——效果写在分析仪读数与 JEI/任务里。</p>
 */
public class BrushItem extends Item {

    public BrushItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player == null) return InteractionResult.PASS;

        if (level.getBlockEntity(pos) instanceof BeakerBlockEntity beaker) {
            double cleaned = beaker.cleanVessel();
            player.displayClientMessage(Component.translatable(
                    cleaned > 1e-9 ? "message.trainees.brush.cleaned" : "message.trainees.brush.clean_nothing"),
                    true);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
