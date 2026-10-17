package com.pha.trainees.enchantments;

import com.pha.trainees.util.interfaces.IKineticWeapon;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.NotNull;

import static com.pha.trainees.enchantments.EnchantmentCategories.KINETIC_WEAPON;

/**
 * 动能系附魔基类：疾跑/冒险/平衡/超越/转化五件套共用模板。
 * 各子类只提供每级系数常量与加成 getter。
 */
public abstract class KineticEnchantmentBase extends Enchantment {
    public KineticEnchantmentBase(Rarity rarity) {
        super(rarity, KINETIC_WEAPON, new EquipmentSlot[]{
                EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
        });
    }

    @Override
    public int getMaxLevel() {
        return 5;
    }

    @Override
    public int getMinCost(int level) {
        return 10 + (level - 1) * 10;
    }

    @Override
    public int getMaxCost(int level) {
        // 附魔的最大经验成本
        return getMinCost(level) + 20;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        // 只允许附魔在动能武器上
        return stack.getItem() instanceof IKineticWeapon;
    }

    @Override
    public boolean canApplyAtEnchantingTable(@NotNull ItemStack stack) {
        // 可以在附魔台上附魔
        return canEnchant(stack);
    }

    @Override
    public boolean isAllowedOnBooks() {
        return true;
    }

    @Override
    public boolean isTreasureOnly() {
        return false;
    }

    @Override
    public boolean isTradeable() {
        return true;
    }
}
