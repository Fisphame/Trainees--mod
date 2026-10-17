package com.pha.trainees.datagen;

import com.pha.trainees.registry.ModBlocks;
import com.pha.trainees.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 数据生成器用的材料清单（§19.21 标签首批覆盖对象）。
 *
 * <p>用一处单一真源描述"一种金属有哪些形态"，标签生成器据此产出
 * {@code forge:ores/…}、{@code forge:storage_blocks/…}、{@code forge:ingots/…}、{@code forge:nuggets/…}。
 * 将来内容增长（一级物质、新金属）只需往这里加一行。</p>
 */
public record Material(String id,
                       @Nullable RegistryObject<Block> ore,
                       @Nullable RegistryObject<Block> deepslateOre,
                       RegistryObject<Block> storageBlock,
                       RegistryObject<Item> ingot,
                       @Nullable RegistryObject<Item> nugget) {

    public boolean hasOre() {
        return ore != null && deepslateOre != null;
    }

    public boolean hasNugget() {
        return nugget != null;
    }

    public static final List<Material> ALL = List.of(
            new Material("selenaurite", ModBlocks.SELENAURITE_ORE, ModBlocks.DEEPSLATE_SELENAURITE_ORE,
                    ModBlocks.SELENAURITE_BLOCK, ModItems.SELENAURITE_INGOT, null),
            new Material("nyctium", ModBlocks.NYCTIUM_ORE, ModBlocks.DEEPSLATE_NYCTIUM_ORE,
                    ModBlocks.NYCTIUM_BLOCK, ModItems.NYCTIUM_INGOT, null),
            new Material("terapium", ModBlocks.TERAPIUM_ORE, ModBlocks.DEEPSLATE_TERAPIUM_ORE,
                    ModBlocks.TERAPIUM_BLOCK, ModItems.TERAPIUM_INGOT, null),
            new Material("banalium", ModBlocks.BANALIUM_ORE, ModBlocks.DEEPSLATE_BANALIUM_ORE,
                    ModBlocks.BANALIUM_BLOCK, ModItems.BANALIUM_INGOT, null),
            new Material("nivtium", ModBlocks.NIVTIUM_ORE, ModBlocks.DEEPSLATE_NIVTIUM_ORE,
                    ModBlocks.NIVTIUM_BLOCK, ModItems.NIVTIUM_INGOT, null),
            new Material("crucium", ModBlocks.CRUCIUM_ORE, ModBlocks.DEEPSLATE_CRUCIUM_ORE,
                    ModBlocks.CRUCIUM_BLOCK, ModItems.CRUCIUM_INGOT, null),
            new Material("sertium", ModBlocks.SERTIUM_ORE, ModBlocks.DEEPSLATE_SERTIUM_ORE,
                    ModBlocks.SERTIUM_BLOCK, ModItems.SERTIUM_INGOT, null),
            new Material("placium", ModBlocks.PLACIUM_ORE, ModBlocks.DEEPSLATE_PLACIUM_ORE,
                    ModBlocks.PLACIUM_BLOCK, ModItems.PLACIUM_INGOT, null),
            // 金矽：目前没有矿石（来源待定），但有块/锭/粒
            new Material("auriversite", null, null,
                    ModBlocks.AURIVERSITE_BLOCK, ModItems.AURIVERSITE_INGOT, ModItems.AURIVERSITE_NUGGET)
    );
}
