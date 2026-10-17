package com.pha.trainees.enchantments;

public class ExceededEnchantment extends KineticEnchantmentBase {
    // 每级增加的最大动能值
    public static final float KINETIC_ENERGY_BONUS_PER_LEVEL = 150f;

    public ExceededEnchantment(Rarity rarity) {
        super(rarity);
    }

    public static float getKineticEnergyBonus(int level) {
        return KINETIC_ENERGY_BONUS_PER_LEVEL * level;
    }
}
