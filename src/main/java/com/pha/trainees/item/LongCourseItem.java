package com.pha.trainees.item;

import com.pha.trainees.util.interfaces.IHoverText;
import com.pha.trainees.util.interfaces.IMineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class LongCourseItem {
    public static class LongItem extends Item {
        public LongItem(Properties p_41383_) {
            super(p_41383_);
        }
    }

    public static class RealPickaxeItem extends PickaxeItem implements IMineBlock, IHoverText {
        public RealPickaxeItem(Tier p_42961_, int p_42962_, float p_42963_, Properties p_42964_) {
            super(p_42961_, p_42962_, p_42963_, p_42964_);
        }

        @Override
        public boolean mineBlock(ItemStack stack, @NotNull Level level, BlockState state,
                                 BlockPos pos, LivingEntity miningEntity) {
            boolean result = super.mineBlock(stack, level, state, pos, miningEntity);
            whatDoesItMeanMining(level, pos);
            return result;
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag flag) {
            super.appendHoverText(stack, level, tooltipComponents, flag);
            addHoverText(stack, level, tooltipComponents, flag, "real_pickaxe_item");
        }
    }
}
