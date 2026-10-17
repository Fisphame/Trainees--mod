package com.pha.trainees.util.interfaces;

import com.pha.trainees.util.game.ParticleHelper;
import com.pha.trainees.util.game.Tools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public interface IHoneycomb {

    default InteractionResult useHoneycomb(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit, Block targetBlock) {
        ItemStack itemInHand = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResult.FAIL;
        }

        if (itemInHand.getItem() == Items.HONEYCOMB) {

            level.setBlock(pos, targetBlock.defaultBlockState(), 3);
            ParticleHelper.sendSurfaces(level, ParticleTypes.WAX_ON, pos, 5, 0.25, 0.25, 0.25, 0.03);
            level.playSound(null, pos,
                    SoundEvents.HONEYCOMB_WAX_ON, SoundSource.BLOCKS,
                    1.0F, 1.0F
            );
            if (!player.isCreative()) {
                itemInHand.shrink(1);
            }

            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    default InteractionResult useAxe(BlockState state, Level level, BlockPos pos,
                                           Player player, InteractionHand hand, BlockHitResult hit, Block targetBlock) {
        ItemStack itemInHand = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResult.FAIL;
        }
        if (itemInHand.getItem() instanceof AxeItem) {
            level.setBlock(pos, targetBlock.defaultBlockState(), 3);
            ParticleHelper.sendSurfaces(level, ParticleTypes.WAX_OFF, pos, 5, 0.25, 0.25, 0.25, 0.03);
            level.playSound(null, pos,
                    SoundEvents.AXE_WAX_OFF, SoundSource.BLOCKS,
                    1.0F, 1.0F
            );
            if (!player.isCreative()) {
                itemInHand.hurtAndBreak(1, player, (e) -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
