package com.pha.trainees.enchantments;

import com.pha.trainees.util.interfaces.IChargeable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.NotNull;

import static com.pha.trainees.enchantments.EnchantmentCategories.CHARGEABLE;

public class LitheEnchantment extends Enchantment {
    public LitheEnchantment(Rarity rarity) {
        super(rarity, CHARGEABLE, new EquipmentSlot[]{
                EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
        });
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return stack.getItem() instanceof IChargeable;
    }

    @Override
    public boolean canApplyAtEnchantingTable(@NotNull ItemStack stack) {
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
