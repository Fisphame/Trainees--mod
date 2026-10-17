package com.pha.trainees.chemistry.util;

import com.pha.trainees.chemistry.particle.IonType;
import com.pha.trainees.registry.ModChemistry.ModIons;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

/**
 * 固体物品 <-> 离子 映射工具
 * 用于将原版物品映射到化学离子
 */
public class SolidIonMapper {

    private static final Map<Item, IonType> ITEM_TO_ION = new HashMap<>();

    static {
        // 原版矿物/金属 -> 单质
        register(Items.IRON_INGOT, ModIons.Fe);
        register(Items.IRON_NUGGET, ModIons.Fe); // 实际添加时乘以 1/9
        register(Items.RAW_IRON, ModIons.Fe);
        register(Items.COPPER_INGOT, ModIons.Cu);
        register(Items.RAW_COPPER, ModIons.Cu);
        register(Items.GOLD_INGOT, null); // 暂不处理
        register(Items.EMERALD, null);    // 暂不处理
        register(Items.DIAMOND, null);    // 暂不处理

        // 盐类
        register(Items.SUGAR, null);      // 暂不处理（有机物）
        register(Items.BONE_MEAL, null);  // 暂不处理（磷酸钙，后续可加）

        // 本模组物品（需后续注册后添加）
        // 例如：NaCl 锭 -> ModIons.NaCl
        // 但 NaCl 是固体，溶解后应拆分为 Na_1 + Cl_minus
        // 这里需要根据实际情况决定是直接添加分子还是拆分
    }

    private static void register(Item item, IonType ion) {
        ITEM_TO_ION.put(item, ion);
    }

    public static IonType getIonForItem(Item item) {
        return ITEM_TO_ION.getOrDefault(item, null);
    }

    public static boolean isMappableItem(Item item) {
        return ITEM_TO_ION.containsKey(item);
    }

    /**
     * 批量注册（用于模组自己的物品）
     */
    public static void registerCustomItem(Item item, IonType ion) {
        register(item, ion);
    }
}