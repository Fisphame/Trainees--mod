package com.pha.trainees.enchantments;

public class AdventEnchantment extends KineticEnchantmentBase {
    // 每级增加的重力转换系数
    public static final float GRAVITY_FACTOR_BONUS_PER_LEVEL = 0.5f;

    public AdventEnchantment(Rarity rarity) {
        super(rarity);
    }

    public static float getGravityFactorBonus(int level) {
        return GRAVITY_FACTOR_BONUS_PER_LEVEL * level;
    }
}
