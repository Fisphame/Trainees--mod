package com.pha.trainees.item;

import com.pha.trainees.util.interfaces.IHoverText;
import com.pha.trainees.registry.ModBlocks;
import com.pha.trainees.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;


public class Duihuanquan extends Item implements IHoverText {
    public Duihuanquan(Properties p_41383_){
        super(p_41383_);
    }

    public @NotNull InteractionResult useOn(UseOnContext context){
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack itemStack = context.getItemInHand();
        BlockPos blockpos = context.getClickedPos();
        BlockState blockstate = level.getBlockState(blockpos);
        Block block = blockstate.getBlock();
        // 仅允许在只因方块（myblock）上兑换（与 tooltip 描述一致）
        if (player != null && block == ModBlocks.MYBLOCK.get()) {
            itemStack.shrink(1);
            ItemStack spawnStack = new ItemStack(ModItems.UPGRADE_THEME.get(), 1);
            ItemEntity itemEntity = new ItemEntity(level, blockpos.getX(), blockpos.getY() + 1, blockpos.getZ(), spawnStack);
            itemEntity.level().addFreshEntity(itemEntity);
            level.playSound(player, blockpos, SoundEvents.CHICKEN_DEATH, SoundSource.BLOCKS, 1.0F, 1.0F);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useOn(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltipComponents, flag);
        addHoverText(stack, level, tooltipComponents, flag, "duihuanquan");
    }
}

