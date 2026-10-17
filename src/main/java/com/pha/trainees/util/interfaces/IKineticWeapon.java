package com.pha.trainees.util.interfaces;

import com.pha.trainees.util.game.ParticleHelper;
import com.pha.trainees.util.game.Tools;
import com.pha.trainees.util.math.MathT;
import com.pha.trainees.util.game.physics.KineticEnergySystem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;

public interface IKineticWeapon {
    /**
     * 更新动能（在inventoryTick中调用）
     */
    default void updateKineticEnergy(Level level, Entity entity, ItemStack stack, boolean isSelected) {
        if (!level.isClientSide && entity instanceof Player player && isSelected) {
            KineticEnergySystem.updateKineticEnergy(player, stack);
        }
    }

    /**
     * 应用动能伤害（在hurtEnemy中调用）
     */
    default float applyKineticDamage(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.level().isClientSide() || !(attacker instanceof Player player)) return 0.0f;
        float damageMultiplier = KineticEnergySystem.consumeKineticEnergy(stack);
        float baseDamage = ((SwordItem) stack.getItem()).getDamage();
        float bonusDamage = baseDamage * damageMultiplier;
        if (bonusDamage > 0) {
            target.hurt(player.damageSources().playerAttack(player), bonusDamage);

        }
        return bonusDamage;
    }

    default void applyParticle(ItemStack stack, LivingEntity target, LivingEntity attacker, float damage){
        if (IKineticWeapon.isKineticUpdateEnabled(stack) && attacker instanceof Player player) {
            ParticleHelper.send(player.level(), ParticleTypes.LAVA, target.getX(), target.getY(), target.getZ(),
                    MathT.inInterval((int) (damage * 3f), 10, 500), 0.1, 0.1, 0.1, 0.1);
        }
    }

    default boolean changeEnabled(ItemStack stack, Player player){
        boolean currentState = IKineticWeapon.isKineticUpdateEnabled(stack);
        boolean newState = !currentState;
        IKineticWeapon.setKineticUpdateEnabled(stack, newState);

        Component message = Component.literal(newState ? "--[ - ]--" : "--[ x ]--")
                .withStyle(newState ? ChatFormatting.GREEN : ChatFormatting.RED);
        player.displayClientMessage(message, true);
        return newState;
    }


    /**
     * 默认的动能更新状态键名
     */
    static String getKineticUpdateEnabledKey() {
        return "KineticUpdateEnabled";
    }

    /**
     * 检查动能更新是否启用（默认实现）
     */
    static boolean isKineticUpdateEnabled(ItemStack stack) {
        if (!stack.hasTag()) {
            return true;
        }
        CompoundTag tag = stack.getTag();
        String key = getKineticUpdateEnabledKey();
        return !tag.contains(key) || tag.getBoolean(key);
    }

    /**
     * 设置动能更新状态（默认实现）
     */
    static void setKineticUpdateEnabled(ItemStack stack, boolean enabled) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean(getKineticUpdateEnabledKey(), enabled);
    }
}
