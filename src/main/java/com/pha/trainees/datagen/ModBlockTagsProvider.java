package com.pha.trainees.datagen;

import com.pha.trainees.Main;
import com.pha.trainees.registry.ModChemistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * 方块公共标签（§19.21 已定）：把我们的矿/深层矿/金属块接入 Forge 通用约定标签，
 * 使 GT / Mekanism / Create / 各类矿机与加工机可以直接识别并处理我们的材料。
 *
 * <p>只生成 {@code forge:} 命名空间的标签；**不触碰**已手写的
 * {@code minecraft:mineable/pickaxe} 等文件，避免同一逻辑路径出现两份文件。</p>
 *
 * <p>§19.34 追加：化学功能标签走 {@code trainees:} 命名空间（同样是本生成器产出，
 * 不手写 JSON）。它们是**设备分类的单一真源**——JEI 催化剂列表、数据包作者写配方、
 * 以后扳手/清洁刷的目标判定都读这里，而不是各自硬编码方块清单。</p>
 */
public class ModBlockTagsProvider extends BlockTagsProvider {

    // ---- forge: 通用标签（用 TagKey 直接构造，避免依赖 Forge 常量命名差异） ----
    public static final TagKey<Block> ORES = forgeBlock("ores");
    public static final TagKey<Block> ORES_IN_GROUND_STONE = forgeBlock("ores_in_ground/stone");
    public static final TagKey<Block> ORES_IN_GROUND_DEEPSLATE = forgeBlock("ores_in_ground/deepslate");
    public static final TagKey<Block> STORAGE_BLOCKS = forgeBlock("storage_blocks");

    // ---- trainees: 化学功能标签（§19.34） ----
    /** 烧杯本体（最常用的反应容器） */
    public static final TagKey<Block> BEAKER = traineeBlock("beaker");
    /** 三种坩埚（金属/石英/陶瓷）：耐温不同的同类容器 */
    public static final TagKey<Block> CRUCIBLE = traineeBlock("crucible");
    /** 五种实验室反应釜（按材质分） */
    public static final TagKey<Block> REACTOR = traineeBlock("reactor");
    public static final TagKey<Block> ELECTROLYSIS_CELL = traineeBlock("electrolysis_cell");
    public static final TagKey<Block> PACKER = traineeBlock("packer");
    /** 所有"能盛化学品并跑反应"的容器：烧杯 + 坩埚 + 反应釜（JEI 化学反应催化剂读它） */
    public static final TagKey<Block> BEAKER_LIKE = traineeBlock("beaker_like");

    public ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Main.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (Material material : Material.ALL) {
            // 金属块 → 通用储存块 + 按材料细分
            tag(STORAGE_BLOCKS).add(material.storageBlock().get());
            tag(forgeBlock("storage_blocks/" + material.id())).add(material.storageBlock().get());

            // 矿（石质 + 深层）→ 通用矿 + 按材料细分 + 按赋存岩层
            if (material.hasOre()) {
                tag(ORES).add(material.ore().get(), material.deepslateOre().get());
                tag(forgeBlock("ores/" + material.id()))
                        .add(material.ore().get(), material.deepslateOre().get());
                tag(ORES_IN_GROUND_STONE).add(material.ore().get());
                tag(ORES_IN_GROUND_DEEPSLATE).add(material.deepslateOre().get());
            }
        }

        // ---- 化学设备（§19.34）：分类只在这里写一次 ----
        // 注意用 ModChemistryBlocks（方块）而不是 ModChemistryBlockItems（物品）——
        // 方块标签只吃 Block。
        tag(BEAKER).add(ModChemistry.ModChemistryBlocks.BEAKER.get());
        tag(CRUCIBLE).add(
                ModChemistry.ModChemistryBlocks.METAL_CRUCIBLE.get(),
                ModChemistry.ModChemistryBlocks.QUARTZ_CRUCIBLE.get(),
                ModChemistry.ModChemistryBlocks.CERAMIC_CRUCIBLE.get());
        tag(REACTOR).add(
                ModChemistry.ModChemistryBlocks.GLASS_REACTOR.get(),
                ModChemistry.ModChemistryBlocks.METAL_REACTOR.get(),
                ModChemistry.ModChemistryBlocks.QUARTZ_REACTOR.get(),
                ModChemistry.ModChemistryBlocks.CERAMIC_REACTOR.get(),
                ModChemistry.ModChemistryBlocks.GLASS_LINED_REACTOR.get());
        tag(ELECTROLYSIS_CELL).add(ModChemistry.ModChemistryBlocks.ELECTROLYSIS_CELL.get());
        tag(PACKER).add(ModChemistry.ModChemistryBlocks.PACKER.get());

        // 容器总集：用嵌套标签，将来加一种容器只需进对应子标签
        tag(BEAKER_LIKE).addTag(BEAKER).addTag(CRUCIBLE).addTag(REACTOR);
    }

    private static TagKey<Block> forgeBlock(String path) {
        return TagKey.create(Registries.BLOCK, new ResourceLocation("forge", path));
    }

    private static TagKey<Block> traineeBlock(String path) {
        return TagKey.create(Registries.BLOCK, new ResourceLocation(Main.MODID, path));
    }
}
