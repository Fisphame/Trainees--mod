package com.pha.trainees.enchantments;

import com.pha.trainees.util.interfaces.IChargeable;
import com.pha.trainees.util.interfaces.IKineticWeapon;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class EnchantmentCategories {
    public static final EnchantmentCategory KINETIC_WEAPON = EnchantmentCategory.create(
            "kinetic_weapon",
            (item) -> item instanceof IKineticWeapon
    );

    public static final EnchantmentCategory CHARGEABLE = EnchantmentCategory.create(
            "chargeable",
            (item) -> item instanceof IChargeable
    );
}
