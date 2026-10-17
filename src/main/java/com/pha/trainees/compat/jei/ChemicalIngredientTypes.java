package com.pha.trainees.compat.jei;

import com.pha.trainees.chemistry.particle.IonType;
import mezz.jei.api.ingredients.IIngredientType;

/**
 * 本模组注册到 JEI 的自定义条目类型（§19.16）。
 *
 * <p>用途：**裸离子**（H⁺/OH⁻/SO₄²⁻ 等没有物品对应物的物种）在 JEI 中作为可搜索、可 R/U 聚焦的条目存在；
 * 有物品的物质（一级真物品 / 二级 NBT 物质）仍走 `ItemStack` 槽位，从而 R/U 能同时命中物品配方与化学方程式。</p>
 */
public final class ChemicalIngredientTypes {

    private ChemicalIngredientTypes() {}

    /** 裸离子条目类型（唯一 id 固定，避免 JEI 用类名做序列化键） */
    public static final IIngredientType<IonType> ION = new IIngredientType<>() {
        @Override
        public Class<? extends IonType> getIngredientClass() {
            return IonType.class;
        }

        @Override
        public String getUid() {
            return "trainees:ion";
        }
    };
}
