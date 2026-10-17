package com.pha.trainees.util.game;

import com.pha.trainees.enchantments.*;
import com.pha.trainees.registry.ModEnchantments;
import com.pha.trainees.util.game.physics.KineticData;
import com.pha.trainees.util.interfaces.IKineticWeapon;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public class EnchantmentCalculator {

    /**
 * 根据物品堆栈计算实际应用的线性转换系数
 * @param stack 物品堆栈
 * @param baseFactor 基础系数（来自KineticData.LINEAR_CONVERSION_FACTOR）
 * @return 应用附魔加成后的系数
 */
public static float getModifiedLinearFactor(ItemStack stack, float baseFactor) {
    if (!(stack.getItem() instanceof IKineticWeapon)) {
        return baseFactor;
    }

    // 获取Sprint附魔的等级
    int sprintLevel = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.SPRINT.get(), stack);
    if (sprintLevel > 0) {
        float bonus = SprintEnchantment.getLinearFactorBonus(sprintLevel);
        return baseFactor + bonus;
    }

    return baseFactor;
}

    /**
     * 根据物品堆栈计算实际应用的重力转换系数
     * @param stack 物品堆栈
     * @param baseFactor 基础系数（来自KineticData.GRAVITY_CONVERSION_FACTOR）
     * @return 应用附魔加成后的系数
     */
    public static float getModifiedGravityFactor(ItemStack stack, float baseFactor) {
        if (!(stack.getItem() instanceof IKineticWeapon)) {
            return baseFactor;
        }

        // 获取Advent附魔的等级
        int adventLevel = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.ADVENT.get(), stack);
        if (adventLevel > 0) {
            float bonus = AdventEnchantment.getGravityFactorBonus(adventLevel);
            return baseFactor + bonus;
        }

        return baseFactor;
    }

    /**
     * 根据物品堆栈计算实际应用的动能衰减速率
     * @param stack 物品堆栈
     * @param baseDecayRate 基础衰减速率（来自KineticData.KINETIC_DECAY_RATE）
     * @return 应用附魔加成后的衰减速率
     */
    public static float getModifiedDecayRate(ItemStack stack, float baseDecayRate) {
        if (!(stack.getItem() instanceof IKineticWeapon)) {
            return baseDecayRate;
        }

        // 获取Balance附魔的等级
        int balanceLevel = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.BALANCE.get(), stack);
        if (balanceLevel > 0) {
            float reduction = BalanceEnchantment.getKineticDecayRate(balanceLevel);
            return Math.max(0, baseDecayRate - reduction);
        }

        return baseDecayRate;
    }

    /**
     * 根据物品堆栈计算实际应用的伤害转化系数
     * @param stack 物品堆栈
     * @param baseFactor 基础系数（来自KineticData.DAMAGE_CONVERSION_FACTOR）
     * @return 应用附魔加成后的系数
     */
    public static float getModifiedDamageFactor(ItemStack stack, float baseFactor) {
        if (!(stack.getItem() instanceof IKineticWeapon)) {
            return baseFactor;
        }

        // 获取Conversion附魔的等级
        int conversionLevel = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.CONVERSION.get(), stack);
        if (conversionLevel > 0) {
            float bonus = ConversionEnchantment.getDamageFactorBonus(conversionLevel);
            return baseFactor + bonus;
        }

        return baseFactor;
    }

    /**
     * 根据物品堆栈计算实际应用的最大动能值
     * @param stack 物品堆栈
     * @param baseMaxEnergy 基础最大动能值（来自KineticData.MAX_KINETIC_ENERGY）
     * @return 应用附魔加成后的最大动能值
     */
    public static float getModifiedMaxKineticEnergy(ItemStack stack, float baseMaxEnergy) {
        if (!(stack.getItem() instanceof IKineticWeapon)) {
            return baseMaxEnergy;
        }

        // 获取Exceeded附魔的等级
        int exceededLevel = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.EXCEEDED.get(), stack);
        if (exceededLevel > 0) {
            float bonus = ExceededEnchantment.getKineticEnergyBonus(exceededLevel);
            return baseMaxEnergy + bonus;
        }

        return baseMaxEnergy;
    }

    /**
     * 获取物品上所有动能相关的附魔信息
     */
    public static String getKineticEnchantmentInfo(ItemStack stack) {
        if (!(stack.getItem() instanceof IKineticWeapon)) {
            return "无动能附魔";
        }

        StringBuilder info = new StringBuilder();

        // 检查Sprint附魔
        int sprintLevel = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.SPRINT.get(), stack);
        if (sprintLevel > 0) {
            info.append("§b疾跑 §7(").append(sprintLevel).append("级)§r\n");
            info.append("  线性动能效率 +").append(String.format("%.5f",
                    SprintEnchantment.getLinearFactorBonus(sprintLevel))).append("\n");
        }

        // 检查Advent附魔
        int adventLevel = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.ADVENT.get(), stack);
        if (adventLevel > 0) {
            info.append("§c冒险 §7(").append(adventLevel).append("级)§r\n");
            info.append("  重力动能效率 +").append(String.format("%.2f",
                    AdventEnchantment.getGravityFactorBonus(adventLevel))).append("\n");
        }

        // 检查Balance附魔
        int balanceLevel = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.BALANCE.get(), stack);
        if (balanceLevel > 0) {
            info.append("§a平衡 §7(").append(balanceLevel).append("级)§r\n");
            info.append("  动能衰减 -").append(String.format("%.2f%%",
                    BalanceEnchantment.getKineticDecayRate(balanceLevel))).append("\n");
        }

        // 检查Conversion附魔
        int conversionLevel = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.CONVERSION.get(), stack);
        if (conversionLevel > 0) {
            info.append("§d转化 §7(").append(conversionLevel).append("级)§r\n");
            info.append("  伤害转化 +").append(String.format("%.1f",
                    ConversionEnchantment.getDamageFactorBonus(conversionLevel))).append("\n");
        }

        // 检查Exceeded附魔
        int exceededLevel = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.EXCEEDED.get(), stack);
        if (exceededLevel > 0) {
            info.append("§6超越 §7(").append(exceededLevel).append("级)§r\n");
            info.append("  最大动能 +").append(String.format("%.0f",
                    ExceededEnchantment.getKineticEnergyBonus(exceededLevel))).append("\n");
        }

        if (info.isEmpty()) {
            return "§7无动能附魔§r";
        }

        return info.toString();
    }

    /**
     * 获取附魔加成后的线性转换系数（简写方法）
     */
    public static float getEffectiveLinearFactor(ItemStack stack) {
        return getModifiedLinearFactor(stack, KineticData.LINEAR_CONVERSION_FACTOR);
    }

    /**
     * 获取附魔加成后的重力转换系数（简写方法）
     */
    public static float getEffectiveGravityFactor(ItemStack stack) {
        return getModifiedGravityFactor(stack, KineticData.GRAVITY_CONVERSION_FACTOR);
    }

    /**
     * 获取附魔加成后的动能衰减速率（简写方法）
     */
    public static float getEffectiveDecayRate(ItemStack stack) {
        return getModifiedDecayRate(stack, KineticData.KINETIC_DECAY_RATE);
    }

    /**
     * 获取附魔加成后的伤害转化系数（简写方法）
     */
    public static float getEffectiveDamageFactor(ItemStack stack) {
        return getModifiedDamageFactor(stack, KineticData.DAMAGE_CONVERSION_FACTOR);
    }

    /**
     * 获取附魔加成后的最大动能值（简写方法）
     */
    public static float getEffectiveMaxKineticEnergy(ItemStack stack) {
        return getModifiedMaxKineticEnergy(stack, KineticData.MAX_KINETIC_ENERGY);
    }
}
