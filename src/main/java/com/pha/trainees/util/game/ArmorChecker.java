package com.pha.trainees.util.game;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ArmorChecker {

    /**
     * 检查玩家是否穿着符合条件的完整套装
     */
    public static boolean isWearingFullArmorSet(Player player) {
        return checkHelmet(player) &&
                checkChestplate(player) &&
                checkLeggings(player) &&
                checkBoots(player);
    }


    /**
     * 检查头盔：皮革头盔，哨兵纹饰，下界合金质升级，淡灰色(#9D9D97)
     */
    private static boolean checkHelmet(Player player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);

        // 检查是否为皮革头盔
        if (!helmet.is(Items.LEATHER_HELMET)) return false;

        CompoundTag tag = helmet.getTag();
        if (tag == null) return false;

        // 检查颜色（淡灰色 #9D9D97 = 10329495）
        if (tag.contains("display")) {
            CompoundTag display = tag.getCompound("display");
            if (display.contains("color")) {
                int color = display.getInt("color");
                if (color != 10329495) return false; // #9D9D97
            } else {
                return false;
            }
        } else {
            return false;
        }

        // 检查纹饰
        if (tag.contains("Trim")) {
            CompoundTag trim = tag.getCompound("Trim");
            String pattern = trim.getString("pattern");
            String material = trim.getString("material");

            // 哨兵纹饰，下界合金质
            return "minecraft:sentry".equals(pattern) &&
                    "minecraft:netherite".equals(material);
        }

        return false;
    }

    /**
     * 检查胸甲：皮革胸甲，潮汐纹饰，石英质升级，黑色(#1D1D21)
     */
    private static boolean checkChestplate(Player player) {
        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);

        // 检查是否为皮革胸甲
        if (!chestplate.is(Items.LEATHER_CHESTPLATE)) return false;

        CompoundTag tag = chestplate.getTag();
        if (tag == null) return false;

        // 检查颜色（黑色 #1D1D21 = 1908001）
        if (tag.contains("display")) {
            CompoundTag display = tag.getCompound("display");
            if (display.contains("color")) {
                int color = display.getInt("color");
                if (color != 1908001) return false; // #1D1D21
            } else {
                return false;
            }
        } else {
            return false;
        }

        // 检查纹饰
        if (tag.contains("Trim")) {
            CompoundTag trim = tag.getCompound("Trim");
            String pattern = trim.getString("pattern");
            String material = trim.getString("material");

            // 潮汐纹饰，石英质
            return "minecraft:tide".equals(pattern) &&
                    "minecraft:quartz".equals(material);
        }

        return false;
    }

    /**
     * 检查护腿：铁护腿，恼鬼纹饰，铁质升级
     */
    private static boolean checkLeggings(Player player) {
        ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);

        // 检查是否为铁护腿
        if (!leggings.is(Items.IRON_LEGGINGS)) return false;

        CompoundTag tag = leggings.getTag();
        if (tag == null) return false;

        // 检查纹饰
        if (tag.contains("Trim")) {
            CompoundTag trim = tag.getCompound("Trim");
            String pattern = trim.getString("pattern");
            String material = trim.getString("material");

            // 恼鬼纹饰，铁质
            return "minecraft:vex".equals(pattern) &&
                    "minecraft:iron".equals(material);
        }

        return false;
    }

    /**
     * 检查靴子：下界合金靴子，猪鼻纹饰，石英质升级
     */
    private static boolean checkBoots(Player player) {
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);

        // 检查是否为下界合金靴子
        if (!boots.is(Items.NETHERITE_BOOTS)) return false;

        CompoundTag tag = boots.getTag();
        if (tag == null) return false;

        // 检查纹饰
        if (tag.contains("Trim")) {
            CompoundTag trim = tag.getCompound("Trim");
            String pattern = trim.getString("pattern");
            String material = trim.getString("material");

            // 猪鼻纹饰，石英质
            return "minecraft:snout".equals(pattern) &&
                    "minecraft:quartz".equals(material);
        }

        return false;
    }

    /**
     * 调试方法：打印盔甲详细信息
     */
    public static void debugArmor(Player player) {
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) {
            ItemStack stack = player.getItemBySlot(slot);
            System.out.println(slot.getName() + ": " + stack);
            if (stack.hasTag()) {
                System.out.println("  NBT: " + stack.getTag());
            }
        }
    }
}
