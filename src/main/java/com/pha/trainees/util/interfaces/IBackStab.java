package com.pha.trainees.util.interfaces;

import com.pha.trainees.util.game.EntityWay;
import com.pha.trainees.util.game.ParticleHelper;
import com.pha.trainees.util.game.Tools;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public interface IBackStab {

    default @NotNull InteractionResultHolder<ItemStack> useIt(@NotNull Level level, @NotNull Player player,
                                                            @NotNull InteractionHand interaction) {
        Entity nearestEntity = EntityWay.getNearestEntityInFront(player, 2.0, 15.0f);
        ParticleHelper.visualizeSector(level, player, 2., 30.0f, 30);
        if (nearestEntity != null) {
            if (nearestEntity instanceof LivingEntity living) {
                if (EntityWay.getYawDiffer(living, player) <= 30.0f){
                    living.hurt(player.damageSources().playerAttack(player), 20.0f);
                }
            }
        }
        ItemStack stack = player.getItemInHand(interaction);
        player.getCooldowns().addCooldown(stack.getItem(), 15);
        if (!player.isCreative()) {
            stack.hurtAndBreak(1, player, (e) -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
