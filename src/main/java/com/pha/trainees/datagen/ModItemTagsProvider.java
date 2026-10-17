package com.pha.trainees.datagen;

import com.pha.trainees.Main;
import com.pha.trainees.chemistry.particle.IonTags;
import com.pha.trainees.registry.ModChemistry;
import com.pha.trainees.registry.ModItemTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.BlockTagsProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 物品公共标签（§19.21）：矿/储存块用 {@code copy} 从方块标签同步，
 * 锭与粒直接登记，使外部 mod 的加工链能识别我们的材料（锭→粉→板…）。
 */
public class ModItemTagsProvider extends ItemTagsProvider {

    private static final TagKey<Item> ORES = forgeItem("ores");
    private static final TagKey<Item> ORES_IN_GROUND_STONE = forgeItem("ores_in_ground/stone");
    private static final TagKey<Item> ORES_IN_GROUND_DEEPSLATE = forgeItem("ores_in_ground/deepslate");
    private static final TagKey<Item> STORAGE_BLOCKS = forgeItem("storage_blocks");
    private static final TagKey<Item> INGOTS = forgeItem("ingots");
    private static final TagKey<Item> NUGGETS = forgeItem("nuggets");

    // ---- trainees: 化学功能物品标签（§19.34） ----
    private static final TagKey<Item> BEAKER = traineeItem("beaker");
    private static final TagKey<Item> CRUCIBLE = traineeItem("crucible");
    private static final TagKey<Item> REACTOR = traineeItem("reactor");
    private static final TagKey<Item> ELECTROLYSIS_CELL = traineeItem("electrolysis_cell");
    private static final TagKey<Item> PACKER = traineeItem("packer");
    private static final TagKey<Item> BEAKER_LIKE = traineeItem("beaker_like");

    /** 会从物种标签派生物品标签的化学分类（酸碱盐 / 氧化剂 / 还原剂） */
    private static final List<ResourceLocation> CHEM_CLASS_TAGS = List.of(
            IonTags.ACID, IonTags.BASE, IonTags.SALT, IonTags.OXIDIZER, IonTags.REDUCER);

    public ModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                               BlockTagsProvider blockTagsProvider) {
        super(output, lookupProvider, blockTagsProvider.contentsGetter());
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // 矿与储存块：直接复制方块标签（BlockItem 映射由 copy 处理）
        copy(ModBlockTagsProvider.ORES, ORES);
        copy(ModBlockTagsProvider.ORES_IN_GROUND_STONE, ORES_IN_GROUND_STONE);
        copy(ModBlockTagsProvider.ORES_IN_GROUND_DEEPSLATE, ORES_IN_GROUND_DEEPSLATE);
        copy(ModBlockTagsProvider.STORAGE_BLOCKS, STORAGE_BLOCKS);

        for (Material material : Material.ALL) {
            // 按材料细分的矿（仅当该材料确实有矿时才存在对应方块标签）
            if (material.hasOre()) {
                copy(TagKey.create(Registries.BLOCK, new ResourceLocation("forge", "ores/" + material.id())),
                        forgeItem("ores/" + material.id()));
            }
            // 按材料细分的储存块
            copy(TagKey.create(Registries.BLOCK, new ResourceLocation("forge", "storage_blocks/" + material.id())),
                    forgeItem("storage_blocks/" + material.id()));

            // 锭（通用 + 细分）
            tag(INGOTS).add(material.ingot().get());
            tag(forgeItem("ingots/" + material.id())).add(material.ingot().get());

            // 粒（通用 + 细分）
            if (material.hasNugget()) {
                tag(NUGGETS).add(material.nugget().get());
                tag(forgeItem("nuggets/" + material.id())).add(material.nugget().get());
            }
        }

        // ---- 化学设备：方块标签 → 物品标签（JEI 催化剂按物品标签取，cross-mod 也能按标签找机器） ----
        copy(ModBlockTagsProvider.BEAKER, BEAKER);
        copy(ModBlockTagsProvider.CRUCIBLE, CRUCIBLE);
        copy(ModBlockTagsProvider.REACTOR, REACTOR);
        copy(ModBlockTagsProvider.ELECTROLYSIS_CELL, ELECTROLYSIS_CELL);
        copy(ModBlockTagsProvider.PACKER, PACKER);
        copy(ModBlockTagsProvider.BEAKER_LIKE, BEAKER_LIKE);

        // ---- 化学分类标签（§19.34）：物品 ← 物种的 IonTags ----
        // 分类**不在这里复制**：一级物质表（FIRST_CLASS）给出"物品 ↔ 物种"，物种身上的
        // IonTags 才是分类真源——给某个物种加一个 tag，对应物品标签自动多一条。
        for (ModChemistry.ModChemistryItems.FirstClass binding : ModChemistry.ModChemistryItems.FIRST_CLASS) {
            for (ResourceLocation chemTag : CHEM_CLASS_TAGS) {
                if (binding.species().getTags().contains(chemTag)) {
                    tag(TagKey.create(Registries.ITEM, chemTag)).add(binding.item().get());
                }
            }
        }

        // ---- 日化品（§19.37）：消毒液家族共用一个标签 ----
        // JEI 图鉴按它挂页、打包机规格页按它放产出槽；以后加新规格只要进这里就全跟上了。
        // 键定义在 ModItemTags（运行时可见），本表只负责填值。
        tag(ModItemTags.BLEACH).add(
                ModChemistry.ModChemistryItems.BLEACH_GOOD.get(),
                ModChemistry.ModChemistryItems.BLEACH_PREMIUM.get(),
                ModChemistry.ModChemistryItems.DEFECTIVE_CHLORINE.get(),
                ModChemistry.ModChemistryItems.DEFECTIVE_ALKALI.get(),
                ModChemistry.ModChemistryItems.DEFECTIVE_ACID.get(),
                ModChemistry.ModChemistryItems.DEFECTIVE_CHLORATE.get());
    }

    private static TagKey<Item> forgeItem(String path) {
        return TagKey.create(Registries.ITEM, new ResourceLocation("forge", path));
    }

    private static TagKey<Item> traineeItem(String path) {
        return TagKey.create(Registries.ITEM, new ResourceLocation(Main.MODID, path));
    }
}
