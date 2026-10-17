package com.pha.trainees.enchantments;

public class SprintEnchantment extends KineticEnchantmentBase {
    // 每级增加的线性转换系数
    public static final float LINEAR_FACTOR_BONUS_PER_LEVEL = 0.0005f;

    public SprintEnchantment(Rarity rarity) {
        super(rarity);
    }

    /**
     * 获取附魔提供的线性转换系数加成
     */
    public static float getLinearFactorBonus(int level) {
        return LINEAR_FACTOR_BONUS_PER_LEVEL * level;
    }
}
