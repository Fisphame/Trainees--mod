package com.pha.trainees.util.interfaces;

import com.pha.trainees.util.game.ParticleHelper;
import com.pha.trainees.util.game.Tools;
import com.pha.trainees.util.math.MathT;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;

public interface IScythe {
    float[] SWEEP_RADIUS = {2f, 4f, MathT.MATH99};
    float[] SWEEP_DAMAGE_MULTIPLIER = {2f, 6f, MathT.MATH99};
    int[] SWEEP_PARTICLE_NUM = {30, 60, 200};
    int[] SWEEP_PARTICLE_AREA = {2, 4, 6};

    default void attack(ItemStack stack, LivingEntity target, LivingEntity attacker, int index) {
        if (attacker.level().isClientSide || !(attacker instanceof Player player)) return;
        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        // 检测是否满足横扫条件（原版逻辑）
        float attackStrength = player.getAttackStrengthScale(0.5F);
        if (attackStrength <= 0.9F) return;

        // 获取范围内的所有生物
        AABB area = target.getBoundingBox().inflate(getSweepRadius(), getSweepHigh(), getSweepRadius());
        List<LivingEntity> entities = player.level().getEntitiesOfClass(
                LivingEntity.class, area,
                e -> e != player && e != target && !e.isAlliedTo(player)
        );

        // 对每个生物应用伤害
        for (LivingEntity entity : entities) {
            float baseDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            float sweepDamage = baseDamage * getSweepDamageMultiplier();
            entity.hurt(player.damageSources().playerAttack(player), sweepDamage);
        }

        ParticleHelper.send(serverLevel, ParticleTypes.SWEEP_ATTACK,
                target.getX(), target.getY(), target.getZ(),
                SWEEP_PARTICLE_NUM[index], SWEEP_PARTICLE_AREA[index], SWEEP_PARTICLE_AREA[index], SWEEP_PARTICLE_AREA[index], 0
        );
    }

    default boolean onHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker, int index) {
        attack(stack, target, attacker, index);
        return true;
    }

    int getTierIndex();

    default float getSweepRadius() {
        int index = getTierIndex();
        return index < SWEEP_RADIUS.length ? SWEEP_RADIUS[index] : 2f;
    }

    default float getSweepDamageMultiplier() {
        int index = getTierIndex();
        return index < SWEEP_DAMAGE_MULTIPLIER.length ? SWEEP_DAMAGE_MULTIPLIER[index] : 2f;
    }

    default float getSweepHigh(){
        return 0.25f;
    }
}
