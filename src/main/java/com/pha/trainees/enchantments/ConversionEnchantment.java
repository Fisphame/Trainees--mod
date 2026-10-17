package com.pha.trainees.enchantments;

public class ConversionEnchantment extends KineticEnchantmentBase {
    // 每级增加的伤害转化系数
    public static final float DAMAGE_FACTOR_BONUS_PER_LEVEL = 4.0f;

    public ConversionEnchantment(Rarity rarity) {
        super(rarity);
    }

    public static float getDamageFactorBonus(int level) {
        return DAMAGE_FACTOR_BONUS_PER_LEVEL * level;
    }
}
