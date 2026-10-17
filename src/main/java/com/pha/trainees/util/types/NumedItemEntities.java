package com.pha.trainees.util.types;

import net.minecraft.world.entity.item.ItemEntity;
import java.util.Arrays;

public record NumedItemEntities(ItemEntity[] itemEntities, int num) {
    public NumedItemEntities(ItemEntity[] itemEntities, int num) {
        // 修复：确保数组不为null且长度正确
        this.itemEntities = itemEntities != null ? itemEntities : new ItemEntity[0];
        this.num = Math.max(0, num);
    }

    @Override
    public ItemEntity[] itemEntities() {
        return Arrays.copyOf(itemEntities, itemEntities.length);
    }
}