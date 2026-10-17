package com.pha.trainees.item.materials;

import com.pha.trainees.registry.ModItems;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;


//  TRAIN 练习生盔甲材料体系  包含三个等级：I, II, III

public class TrainMaterial {
    private static final int[][] PROTECTION_PER_SLOT ={
            new int[]{3, 6, 4, 1},
            new int[]{5, 8, 5, 2},
            new int[]{5, 8, 5, 2}
    }; //盔甲值
    private static final int[] DURABILITY_PER_SLOT = new int[]{
            13, 15, 16, 11
    }; //基础耐久
    private static final int[] DURABILITY_PER_SLOT_COEFFICIENT = new int[]{
            15, 25, 35
    }; //耐久乘数
    private static final int[] ENCHANTMENT_VALUE = new int[]{
            10, 15, 20
    }; //附魔能力
    private static final float[] TOUGHNESS_VALUE = new float[]{
            0.0f, 2.5f, 4.0f
    }; //韧性
    private static final float[] KNOCKBACK_RESISTANCE_VALUE = new float[]{
            0.0f, 0.0f, 0.15f
    }; //击退抗性
    private static final SoundEvent[] SOUND_EVENTS = new SoundEvent[]{
            SoundEvents.ARMOR_EQUIP_IRON, SoundEvents.ARMOR_EQUIP_DIAMOND, SoundEvents.ARMOR_EQUIP_NETHERITE
    };//声音
    private static final String[] NAME = new String[]{
            "trainees:train_i","trainees:train_ii","trainees:train_iii"
            // 纹理路径: train_i(i)(i)_layer_1.png
    }; //材质名字
    private static final Supplier<Ingredient> REPAIR_MATERIAL = () ->
            Ingredient.of(ModItems.TWO_HALF_INGOT.get());


    // ==========  I ==========
    public static class LevelI implements ArmorMaterial {
        private static final int level = 0;
        @Override
        public int getDurabilityForType(ArmorItem.Type type) {return DURABILITY_PER_SLOT[type.getSlot().getIndex()] * DURABILITY_PER_SLOT_COEFFICIENT[level];}
        @Override
        public int getDefenseForType(ArmorItem.Type type) {return PROTECTION_PER_SLOT[level][type.getSlot().getIndex()];}
        @Override
        public int getEnchantmentValue() {return ENCHANTMENT_VALUE[level];}
        @Override
        public @NotNull SoundEvent getEquipSound() {
            return SOUND_EVENTS[level];
        }
        @Override
        public @NotNull Ingredient getRepairIngredient() {
            return REPAIR_MATERIAL.get();
        }
        @Override
        public @NotNull String getName() {return NAME[level];}
        @Override
        public float getToughness() {return TOUGHNESS_VALUE[level];}
        @Override
        public float getKnockbackResistance() {return KNOCKBACK_RESISTANCE_VALUE[level];}
    }

    // ==========  II ==========
    public static class LevelII implements ArmorMaterial {
        private static final int level = 1;
        @Override
        public int getDurabilityForType(ArmorItem.Type type) {return DURABILITY_PER_SLOT[type.getSlot().getIndex()] * DURABILITY_PER_SLOT_COEFFICIENT[level];}
        @Override
        public int getDefenseForType(ArmorItem.Type type) {return PROTECTION_PER_SLOT[level][type.getSlot().getIndex()];}
        @Override
        public int getEnchantmentValue() {return ENCHANTMENT_VALUE[level];}
        @Override
        public @NotNull SoundEvent getEquipSound() {
            return SOUND_EVENTS[level];
        }
        @Override
        public @NotNull Ingredient getRepairIngredient() {
            return REPAIR_MATERIAL.get();
        }
        @Override
        public @NotNull String getName() {return NAME[level];}
        @Override
        public float getToughness() {return TOUGHNESS_VALUE[level];}
        @Override
        public float getKnockbackResistance() {return KNOCKBACK_RESISTANCE_VALUE[level];}
    }

    // ==========  III ==========
    public static class LevelIII implements ArmorMaterial {
        private static final int level = 2;
        @Override
        public int getDurabilityForType(ArmorItem.Type type) {return DURABILITY_PER_SLOT[type.getSlot().getIndex()] * DURABILITY_PER_SLOT_COEFFICIENT[level];}
        @Override
        public int getDefenseForType(ArmorItem.Type type) {return PROTECTION_PER_SLOT[level][type.getSlot().getIndex()];}
        @Override
        public int getEnchantmentValue() {return ENCHANTMENT_VALUE[level];}
        @Override
        public @NotNull SoundEvent getEquipSound() {
            return SOUND_EVENTS[level];
        }
        @Override
        public @NotNull Ingredient getRepairIngredient() {
            return REPAIR_MATERIAL.get();
        }
        @Override
        public @NotNull String getName() {return NAME[level];}
        @Override
        public float getToughness() {return TOUGHNESS_VALUE[level];}
        @Override
        public float getKnockbackResistance() {return KNOCKBACK_RESISTANCE_VALUE[level];}
    }
}