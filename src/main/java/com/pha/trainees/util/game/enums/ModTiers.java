package com.pha.trainees.util.game.enums;

import com.pha.trainees.registry.ModChemistry;
import com.pha.trainees.registry.ModItems;
import net.minecraft.util.LazyLoadedValue;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;

public enum ModTiers implements Tier {

    SCYTHE(3, 913, 8.0F, 3.0F, 15, () -> {
        return Ingredient.of(ModItems.TWO_HALF_INGOT.get());
    }
    ),
    STAB(2, 913, 8.0F, 3.0F, 15, () -> {
        return Ingredient.of(ModChemistry.ModChemistryItems.CHE_JIBP_PIECE.get());
        }
    );




    private final int level;
    private final int uses;
    private final float speed;
    private final float damage;
    private final int enchantmentValue;
    private final LazyLoadedValue<Ingredient> repairIngredient;

    ModTiers(int level, int uses, float speed, float damage, int enchantmentValue, Supplier<Ingredient> repairIngredient) {
        this.level = level;
        this.uses = uses;
        this.speed = speed;
        this.damage = damage;
        this.enchantmentValue = enchantmentValue;
        this.repairIngredient = new LazyLoadedValue<>(repairIngredient);
    }

    public int getUses() {
        return this.uses;
    }

    public float getSpeed() {
        return this.speed;
    }

    public float getAttackDamageBonus() {
        return this.damage;
    }

    public int getLevel() {
        return this.level;
    }

    public int getEnchantmentValue() {
        return this.enchantmentValue;
    }

    public Ingredient getRepairIngredient() {
        return this.repairIngredient.get();
    }

}