package com.pha.trainees.util.game.chemistry;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 化学组分，表示反应物或生成物中的一个物质。
 * 注意：构造参数顺序为 (int coefficient, Item item)，无描述字段。
 */
public class ChemicalComponent {
    private final Item item;
    private final int coefficient; // 系数（摩尔数）

    public ChemicalComponent(int coefficient, Item item) {
        this.coefficient = coefficient;
        this.item = item;
    }

    public Item getItem() {
        return item;
    }

    public int getCoefficient() {
        return coefficient;
    }

    /**
     * 根据反应次数生成对应数量的物品堆
     * @param reactionCount 反应次数
     * @return 物品堆（数量 = 系数 * reactionCount）
     */
    public ItemStack createStack(int reactionCount) {
        return new ItemStack(item, coefficient * reactionCount);
    }
}
