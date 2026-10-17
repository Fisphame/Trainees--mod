package com.pha.trainees.util.types;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public class ItemPair4 {
    private final Item item1;
    private final Item item2;
    private final Item item3;
    private final Item item4;
    private final ItemStack result;

    public ItemPair4(Item item1, Item item2, Item item3, Item item4, ItemStack result) {
        this.item1 = item1;
        this.item2 = item2;
        this.item3 = item3;
        this.item4 = item4;
        this.result = result;
    }

    // 获取规范化列表（用于无序比较）
    private List<Item> normalized() {
        List<Item> list = Arrays.asList(item1, item2, item3, item4);
        list.sort(Comparator.comparing(Item::getDescriptionId)); // 按注册名排序
        return list;
    }

    // 判断四个输入物品是否匹配此配方（无序）
    public boolean matches(Item input1, Item input2, Item input3, Item input4) {
        if (input1 == null || input2 == null || input3 == null || input4 == null) return false;
        List<Item> inputList = Arrays.asList(input1, input2, input3, input4);
        inputList.sort(Comparator.comparing(Item::getDescriptionId));
        return normalized().equals(inputList);
    }

    // Getters
    public Item getItem1() {
        return item1;
    }
    public Item getItem2() {
        return item2;
    }
    public Item getItem3() {
        return item3;
    }
    public Item getItem4() {
        return item4;
    }
    public ItemStack getResultItem() {
        return result.copy();
    }

    @Override
    public String toString() {
        return String.format("%s & %s & %s & %s -> %s",
                item1, item2, item3, item4, result);
    }

    // 用于配方列表比较（如果需要）
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemPair4 that)) return false;
        return normalized().equals(that.normalized()) &&
                ItemStack.matches(result, that.result);
    }

    @Override
    public int hashCode() {
        return Objects.hash(normalized(), result);
    }
}