package com.pha.trainees.registry;

import com.pha.trainees.Main;
import com.pha.trainees.enchantments.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, Main.MODID);

    public static final RegistryObject<Enchantment> ten_thousand_sword =
            ENCHANTMENTS.register("ten_thousand_sword",
                    () -> new TenThousandSwordEnchantment(Enchantment.Rarity.VERY_RARE)
            );

    public static final RegistryObject<Enchantment> SPRINT =
            ENCHANTMENTS.register("sprint",
                    () -> new SprintEnchantment(Enchantment.Rarity.UNCOMMON)
            );
    public static final RegistryObject<Enchantment> ADVENT =
            ENCHANTMENTS.register("advent",
                    () -> new AdventEnchantment(Enchantment.Rarity.UNCOMMON)
            );
    public static final RegistryObject<Enchantment> BALANCE =
            ENCHANTMENTS.register("balance",
                    () -> new BalanceEnchantment(Enchantment.Rarity.UNCOMMON)
            );
    public static final RegistryObject<Enchantment> EXCEEDED =
            ENCHANTMENTS.register("exceeded",
                    () -> new ExceededEnchantment(Enchantment.Rarity.UNCOMMON)
            );
    public static final RegistryObject<Enchantment> CONVERSION =
            ENCHANTMENTS.register("conversion",
                    () -> new ConversionEnchantment(Enchantment.Rarity.UNCOMMON)
            );

    public static final RegistryObject<Enchantment> LITHE =
            ENCHANTMENTS.register("lithe",
                    () -> new LitheEnchantment(Enchantment.Rarity.UNCOMMON)
            );
}