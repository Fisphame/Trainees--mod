package com.pha.trainees.util.interfaces;

import com.pha.trainees.registry.ModEnchantments;
import com.pha.trainees.util.game.SoundCourse;
import com.pha.trainees.util.game.Tools;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public interface IChargeable {
    default InteractionResultHolder<ItemStack> charge(Level level, Player player, InteractionHand hand){
        ItemStack stack = player.getItemInHand(hand);
        Vec3 look = player.getLookAngle();

        // 水平方向速度
        look = new Vec3(look.x, 0, look.z).normalize();
        player.setDeltaMovement(look);
        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
        player.getCooldowns().addCooldown(stack.getItem(), getChargeCooldown());

        // 重置摔落高度
        if (!level.isClientSide()) { // 仅在服务端执行
            int litheLevel = stack.getEnchantmentLevel(ModEnchantments.LITHE.get());
            if (litheLevel > 0) {
                player.fallDistance = 0.0F;
            }
        }

        if (consumesDurabilityOnCharge(player)) {
            stack.hurtAndBreak(1, player, (e) -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        }
        SoundEvent sound = SoundCourse.getIndexSound(SoundCourse.HIT_SOUNDS.get(), level);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                sound, SoundSource.PLAYERS, 1.5F, 1.0F);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    default int getChargeCooldown() {
        return 20;
    }

    default boolean consumesDurabilityOnCharge(Player player) {
        return !player.isCreative();
    }

}
