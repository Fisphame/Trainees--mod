package com.pha.trainees.enchantments;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class TenThousandSwordEnchantment extends Enchantment {
    public TenThousandSwordEnchantment(Rarity rarity) {
        // 限定附魔目标为剑（WEAPON），且只能附魔在主手
        super(rarity, EnchantmentCategory.WEAPON, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    @Override
    public int getMaxLevel() {
        return 5; // 附魔最大等级
    }

    // 设置附魔为宝藏（只能通过宝箱或交易获得）
    @Override
    public boolean isTreasureOnly() {
        return true;
    }
    // 禁止在附魔台中生成
    @Override
    public boolean isDiscoverable() {
        return false;
    }
    @Override
    public boolean isTradeable() {
        return true;
    }
}