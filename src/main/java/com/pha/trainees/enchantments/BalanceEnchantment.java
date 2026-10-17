package com.pha.trainees.enchantments;

public class BalanceEnchantment extends KineticEnchantmentBase {
    // 每级减少的“每检查时间衰减动能”
    public static final float KINETIC_DECAY_RATE_PER_LEVEL = 0.05f;

    public BalanceEnchantment(Rarity rarity) {
        super(rarity);
    }

    public static float getKineticDecayRate(int level) {
        return KINETIC_DECAY_RATE_PER_LEVEL * level;
    }
}
