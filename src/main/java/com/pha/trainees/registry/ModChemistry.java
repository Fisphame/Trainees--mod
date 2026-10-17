package com.pha.trainees.registry;

import com.pha.trainees.Main;
import com.pha.trainees.block.CheJibpBlock;
import com.pha.trainees.chemistry.block.BeakerBlock;
import com.pha.trainees.chemistry.block.CreativeEnergySourceBlock;
import com.pha.trainees.chemistry.block.CreativeHeatSourceBlock;
import com.pha.trainees.chemistry.block.ElectrolysisCellBlock;
import com.pha.trainees.chemistry.block.LabReactorBlock;
import com.pha.trainees.chemistry.block.PackerBlock;
import com.pha.trainees.chemistry.block.JacketBlock;
import com.pha.trainees.chemistry.block.StirrerBlock;
import com.pha.trainees.chemistry.block.ThermostaticBathBlock;
import com.pha.trainees.chemistry.block.ValveBlock;
import com.pha.trainees.chemistry.blockentity.BeakerBlockEntity;
import com.pha.trainees.chemistry.blockentity.CreativeEnergySourceBlockEntity;
import com.pha.trainees.chemistry.blockentity.CreativeHeatSourceBlockEntity;
import com.pha.trainees.chemistry.blockentity.ElectrolysisCellBlockEntity;
import com.pha.trainees.chemistry.blockentity.JacketBlockEntity;
import com.pha.trainees.chemistry.blockentity.LabReactorBlockEntity;
import com.pha.trainees.chemistry.blockentity.PackerBlockEntity;
import com.pha.trainees.chemistry.blockentity.StirrerBlockEntity;
import com.pha.trainees.chemistry.blockentity.ValveBlockEntity;
import com.pha.trainees.chemistry.blockentity.ThermostaticBathBlockEntity;
//import com.pha.trainees.chemistry.fluid.HydrochloricAcidFluid;
import com.pha.trainees.chemistry.container.ContainerMaterial;
import com.pha.trainees.chemistry.material.FirstClassSubstances;
import com.pha.trainees.chemistry.material.FixedCompositionTable;
import com.pha.trainees.chemistry.item.AnalyzerItemCreative;
import com.pha.trainees.chemistry.item.BrushItem;
import com.pha.trainees.chemistry.item.DefectiveProductItem;
import com.pha.trainees.chemistry.product.PackOutcome;
import com.pha.trainees.chemistry.item.AnalyzerItemNormal;
import com.pha.trainees.chemistry.item.SubstanceItem;
import com.pha.trainees.chemistry.item.WrenchItem;
import com.pha.trainees.chemistry.particle.IonTags;
import com.pha.trainees.chemistry.particle.IonType;
import com.pha.trainees.chemistry.particle.Phase;
import com.pha.trainees.chemistry.phase.PhaseChangeData;
import com.pha.trainees.chemistry.phase.PhaseChangeTable;
import com.pha.trainees.chemistry.phase.PhaseTransition;
import com.pha.trainees.chemistry.reaction.ReactionEdge;
import com.pha.trainees.chemistry.reaction.ReactionGraph;
import com.pha.trainees.chemistry.reaction.ReactionRule;
import com.pha.trainees.chemistry.reaction.RuleFamily;
import com.pha.trainees.util.game.chemistry.ChemicalEquation;
import com.pha.trainees.util.game.chemistry.ReactionConditions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.pha.trainees.item.ChemicalItem.*;
import static com.pha.trainees.block.ChemistryBlock.*;

@SuppressWarnings({"deprecation", "removal"})
public class ModChemistry {

    public static class ModChemistryBlocks {
        public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Main.MODID);

        //氢氧化鸡方块
        public static final RegistryObject<Block> CHE_JIOH_BLOCK = BLOCKS.register("che_jioh_block",
                () -> new JiOHBlock(
                        BlockBehaviour.Properties.of()
                                .strength(2f,6f)
                                .sound(SoundType.BONE_BLOCK)
                                .requiresCorrectToolForDrops()
                ));

        // 氧化鸡方块
        public static final RegistryObject<Block> CHE_JI2O_BLOCK = BLOCKS.register("che_ji2o_block",
                () -> new Ji2OBlock(
                        BlockBehaviour.Properties.of()
                                .strength(2f, 6f)
                                .sound(SoundType.BONE_BLOCK)
                                .requiresCorrectToolForDrops()
                ));

        // 涂蜡的氧化鸡方块
        public static final RegistryObject<Block> CHE_WAXED_JI2O_BLOCK = BLOCKS.register("che_waxed_ji2o_block",
                () -> new WaxedJi2OBlock(
                        BlockBehaviour.Properties.of()
                                .strength(2f, 6f)
                                .sound(SoundType.BONE_BLOCK)
                                .requiresCorrectToolForDrops()
                ));

        // 欲焰鸡方块
        public static final RegistryObject<Block> CHE_JI2O2_BLOCK = BLOCKS.register("che_ji2o2_block",
                () -> new Ji2O2Block(
                        BlockBehaviour.Properties.of()
                                .strength(2f, 6f)
                                .sound(SoundType.BONE_BLOCK)
                                .requiresCorrectToolForDrops()
                ));

        //反相素方块
        public static final RegistryObject<Block> CHE_JIBP_BLOCK = BLOCKS.register("che_jibp_block",
                () -> new CheJibpBlock(
                        BlockBehaviour.Properties.of()
                                .strength(0.5f,3f)
                                .sound(SoundType.WET_GRASS)
                )
        );

        // RGT反相素方块
        public static final RegistryObject<Block> CHE_JIBP_BLOCK_RGT = BLOCKS.register("che_jibp_block_rgt",
                () -> new CheJibpBlock(
                        BlockBehaviour.Properties.of()
                                .strength(0.5f,1200f)
                                .sound(SoundType.WET_GRASS)
                )
        );

        public static final RegistryObject<LiquidBlock> CHE_HBP_BLOCK = BLOCKS.register("che_hbp_block",
                () -> new LiquidBlock(ModFluids.SOURCE_CHE_HBP,
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.WATER)
                                .replaceable()
                                .noCollission()
                                .strength(100.0F)
                                .pushReaction(PushReaction.DESTROY)
                                .noLootTable()
                )
        );

//        public static final RegistryObject<LiquidBlock> JI_LIQUID_BLOCK =BLOCKS.register("ji_liquid_block",
//                () -> new LiquidBlock(ModFluid.SOURCE_JI,
//                        BlockBehaviour.Properties.of()
//                                .mapColor(MapColor.GOLD)
//                                .strength(100.0F)
//                                .replaceable()
//                                .noCollission()
//                                .pushReaction(PushReaction.DESTROY)
//                                .noLootTable()
//                        )
//        );

        public static final RegistryObject<BeakerBlock> BEAKER = BLOCKS.register("beaker",
                () -> new BeakerBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.TERRACOTTA_WHITE)
                        .strength(0.5f)
                        .noOcclusion()
                        .isViewBlocking((state, level, pos) -> false)
                        .isSuffocating((state, level, pos) -> false)
                        .sound(SoundType.GLASS)
                ));

        // 电解槽（§19.10）：双电极接入面、双面供电、产物分拣
        public static final RegistryObject<ElectrolysisCellBlock> ELECTROLYSIS_CELL = BLOCKS.register(
                "electrolysis_cell",
                () -> new ElectrolysisCellBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(3.0f, 6.0f)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.METAL), ContainerMaterial.CERAMIC));

        // 创造热源（§19.24）：设定点型热源，目标温度可任意指定（含 0 ℃ 以下），用于测试温度依赖的化学
        public static final RegistryObject<CreativeHeatSourceBlock> CREATIVE_HEAT_SOURCE = BLOCKS.register(
                "creative_heat_source",
                () -> new CreativeHeatSourceBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_RED)
                        .strength(2.0f, 6.0f)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.METAL)
                ));

        // 创造能源方块（§19.38 附）：无限 FE 源，供上机测试电设备（恒温浴/夹套/打包机）
        public static final RegistryObject<CreativeEnergySourceBlock> CREATIVE_ENERGY_SOURCE = BLOCKS.register(
                "creative_energy_source",
                () -> new CreativeEnergySourceBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_RED)
                        .strength(2.0f, 6.0f)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.METAL)
                ));

        // ====== 材料分级坩埚（§6.3 / ③-3）：与烧杯共用引擎，只有材料不同 ======
        // 耐温上限：金属 1673 K < 石英 1873 K < 陶瓷 2273 K；玻璃烧杯仅 773 K。
        // 这构成科技树第一道硬墙：要炼钢（1600 K+）就必须先有坩埚。
        public static final RegistryObject<BeakerBlock> METAL_CRUCIBLE = BLOCKS.register(
                "metal_crucible",
                () -> new BeakerBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(3.0f, 6.0f)
                        .noOcclusion()
                        .isViewBlocking((state, level, pos) -> false)
                        .isSuffocating((state, level, pos) -> false)
                        .sound(SoundType.METAL), ContainerMaterial.METAL));

        public static final RegistryObject<BeakerBlock> QUARTZ_CRUCIBLE = BLOCKS.register(
                "quartz_crucible",
                () -> new BeakerBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.QUARTZ)
                        .strength(2.5f, 6.0f)
                        .noOcclusion()
                        .isViewBlocking((state, level, pos) -> false)
                        .isSuffocating((state, level, pos) -> false)
                        .sound(SoundType.GLASS), ContainerMaterial.QUARTZ));

        public static final RegistryObject<BeakerBlock> CERAMIC_CRUCIBLE = BLOCKS.register(
                "ceramic_crucible",
                () -> new BeakerBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.TERRACOTTA_WHITE)
                        .strength(2.5f, 6.0f)
                        .noOcclusion()
                        .isViewBlocking((state, level, pos) -> false)
                        .isSuffocating((state, level, pos) -> false)
                        .sound(SoundType.STONE), ContainerMaterial.CERAMIC));

        // 恒温浴（§19.24 ③-4）：设定点型热源，介质决定可达区间（水浴 ≤100 ℃ / 油浴 ≤250 ℃ / 沙浴 ≤600 ℃ / 冰盐浴）
        public static final RegistryObject<ThermostaticBathBlock> THERMOSTATIC_BATH = BLOCKS.register(
                "thermostatic_bath",
                () -> new ThermostaticBathBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_LIGHT_BLUE)
                        .strength(2.0f, 6.0f)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.METAL)
                ));

        // 打包机（§19.18 / Step ④）：读取正上方容器，按规格质检后灌装；无方块实体、无 GUI（V1）
        public static final RegistryObject<PackerBlock> PACKER = BLOCKS.register(
                "packer",
                () -> new PackerBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_GRAY)
                        .strength(2.5f, 6.0f)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.METAL)
                ));

        // 阀门（§19.28）：装在容器"口"所在那一格，右键切换通/闭——让"密封"成为玩家可控动作
        public static final RegistryObject<ValveBlock> VALVE = BLOCKS.register(
                "valve",
                () -> new ValveBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(1.5f, 6.0f)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.METAL)
                ));

        // 搅拌器（§19.29）：贴在容器邻面的附着件，有电就转，提升传热与气液传质（不改反应速率）
        public static final RegistryObject<StirrerBlock> STIRRER = BLOCKS.register(
                "stirrer",
                () -> new StirrerBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(2.0f, 6.0f)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.METAL)
                ));

        // 电加热夹套（§19.29 第 2 步）：附着件形态的设定点热源 + 传热面积加成
        public static final RegistryObject<JacketBlock> JACKET = BLOCKS.register(
                "jacket",
                () -> new JacketBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_ORANGE)
                        .strength(2.0f, 6.0f)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.METAL)
                ));

        // ====== 实验室反应釜（§19.30 方案 1）：10 L 单方块，四档材质 ======
        public static final RegistryObject<LabReactorBlock> GLASS_REACTOR = BLOCKS.register(
                "glass_reactor",
                () -> new LabReactorBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_LIGHT_BLUE)
                        .strength(2.0f, 6.0f)
                        .noOcclusion()
                        .isViewBlocking((state, level, pos) -> false)
                        .isSuffocating((state, level, pos) -> false)
                        .sound(SoundType.GLASS), ContainerMaterial.GLASS));

        public static final RegistryObject<LabReactorBlock> METAL_REACTOR = BLOCKS.register(
                "metal_reactor",
                () -> new LabReactorBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(3.0f, 8.0f)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.METAL), ContainerMaterial.METAL));

        public static final RegistryObject<LabReactorBlock> QUARTZ_REACTOR = BLOCKS.register(
                "quartz_reactor",
                () -> new LabReactorBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.QUARTZ)
                        .strength(2.5f, 7.0f)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.STONE), ContainerMaterial.QUARTZ));

        public static final RegistryObject<LabReactorBlock> CERAMIC_REACTOR = BLOCKS.register(
                "ceramic_reactor",
                () -> new LabReactorBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.TERRACOTTA_WHITE)
                        .strength(3.0f, 9.0f)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.STONE), ContainerMaterial.CERAMIC));

        // 搪玻璃反应釜（§19.32）：玻璃的耐蚀 + 钢的耐压；衬层被腐蚀透则降级为钢基体
        public static final RegistryObject<LabReactorBlock> GLASS_LINED_REACTOR = BLOCKS.register(
                "glass_lined_reactor",
                () -> new LabReactorBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_LIGHT_BLUE)
                        .strength(3.0f, 8.0f)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.METAL), ContainerMaterial.GLASS_LINED));
    }

    public static class ModChemistryBlockItems {
        public static final DeferredRegister<Item> ITEMS =
                DeferredRegister.create(ForgeRegistries.ITEMS, Main.MODID);

        //氢氧化鸡方块
        public static final RegistryObject<Item> CHE_JIOH_BLOCK_ITEM = ITEMS.register("che_jioh_block",
                () -> new JiOH_B(ModChemistryBlocks.CHE_JIOH_BLOCK.get(),
                        new Item.Properties()
                ));
        // 氧化鸡方块
        public static final RegistryObject<Item> CHE_JI2O_BLOCK_ITEM = ITEMS.register("che_ji2o_block",
                () -> new Ji2O_B(ModChemistryBlocks.CHE_JI2O_BLOCK.get(),
                        new Item.Properties()
                ));
        // 涂蜡的氧化鸡方块
        public static final RegistryObject<Item> CHE_WAXED_JI2O_BLOCK_ITEM = ITEMS.register("che_waxed_ji2o_block",
                () -> new Waxed_Ji2O_B(ModChemistryBlocks.CHE_WAXED_JI2O_BLOCK.get(),
                        new Item.Properties()
                ));
        // 欲焰鸡方块
        public static final RegistryObject<Item> CHE_JI2O2_BLOCK_ITEM = ITEMS.register("che_ji2o2_block",
                () -> new Ji2O2_B(ModChemistryBlocks.CHE_JI2O2_BLOCK.get(),
                        new Item.Properties()
                ));
        //反相素方块
        public static final RegistryObject<Item> CHE_JIBP_BLOCK_ITEM = ITEMS.register("che_jibp_block",
                () -> new JiBp_B(ModChemistryBlocks.CHE_JIBP_BLOCK.get(),
                        new Item.Properties()
                                .rarity(Rarity.UNCOMMON)
                ));
        // 烧杯
        public static final RegistryObject<Item> BEAKER = ITEMS.register("beaker",
                () -> new BlockItem(ModChemistryBlocks.BEAKER.get(),
                        new Item.Properties()
                ));

        // 电解槽
        public static final RegistryObject<Item> ELECTROLYSIS_CELL = ITEMS.register("electrolysis_cell",
                () -> new BlockItem(ModChemistryBlocks.ELECTROLYSIS_CELL.get(),
                        new Item.Properties()
                ));

        // 创造热源（§19.24）
        public static final RegistryObject<Item> CREATIVE_HEAT_SOURCE = ITEMS.register("creative_heat_source",
                () -> new BlockItem(ModChemistryBlocks.CREATIVE_HEAT_SOURCE.get(),
                        new Item.Properties()
                ));

        // 创造能源方块（§19.38 附）
        public static final RegistryObject<Item> CREATIVE_ENERGY_SOURCE = ITEMS.register("creative_energy_source",
                () -> new BlockItem(ModChemistryBlocks.CREATIVE_ENERGY_SOURCE.get(),
                        new Item.Properties()
                ));

        // 材料分级坩埚（§6.3 / ③-3）
        public static final RegistryObject<Item> METAL_CRUCIBLE = ITEMS.register("metal_crucible",
                () -> new BlockItem(ModChemistryBlocks.METAL_CRUCIBLE.get(), new Item.Properties()));
        public static final RegistryObject<Item> QUARTZ_CRUCIBLE = ITEMS.register("quartz_crucible",
                () -> new BlockItem(ModChemistryBlocks.QUARTZ_CRUCIBLE.get(), new Item.Properties()));
        public static final RegistryObject<Item> CERAMIC_CRUCIBLE = ITEMS.register("ceramic_crucible",
                () -> new BlockItem(ModChemistryBlocks.CERAMIC_CRUCIBLE.get(), new Item.Properties()));

        // 恒温浴（§19.24 ③-4）
        public static final RegistryObject<Item> THERMOSTATIC_BATH = ITEMS.register("thermostatic_bath",
                () -> new BlockItem(ModChemistryBlocks.THERMOSTATIC_BATH.get(), new Item.Properties()));

        // 阀门（§19.28）
        public static final RegistryObject<Item> VALVE = ITEMS.register("valve",
                () -> new BlockItem(ModChemistryBlocks.VALVE.get(), new Item.Properties()));

        // 搅拌器（§19.29）
        public static final RegistryObject<Item> STIRRER = ITEMS.register("stirrer",
                () -> new BlockItem(ModChemistryBlocks.STIRRER.get(), new Item.Properties()));

        // 电加热夹套（§19.29 第 2 步）
        public static final RegistryObject<Item> JACKET = ITEMS.register("jacket",
                () -> new BlockItem(ModChemistryBlocks.JACKET.get(), new Item.Properties()));

        // 实验室反应釜（§19.30）：四档材质
        public static final RegistryObject<Item> GLASS_REACTOR = ITEMS.register("glass_reactor",
                () -> new BlockItem(ModChemistryBlocks.GLASS_REACTOR.get(), new Item.Properties()));
        public static final RegistryObject<Item> METAL_REACTOR = ITEMS.register("metal_reactor",
                () -> new BlockItem(ModChemistryBlocks.METAL_REACTOR.get(), new Item.Properties()));
        public static final RegistryObject<Item> QUARTZ_REACTOR = ITEMS.register("quartz_reactor",
                () -> new BlockItem(ModChemistryBlocks.QUARTZ_REACTOR.get(), new Item.Properties()));
        public static final RegistryObject<Item> CERAMIC_REACTOR = ITEMS.register("ceramic_reactor",
                () -> new BlockItem(ModChemistryBlocks.CERAMIC_REACTOR.get(), new Item.Properties()));
        public static final RegistryObject<Item> GLASS_LINED_REACTOR = ITEMS.register("glass_lined_reactor",
                () -> new BlockItem(ModChemistryBlocks.GLASS_LINED_REACTOR.get(), new Item.Properties()));

        // 打包机（§19.18 / Step ④）
        public static final RegistryObject<Item> PACKER = ITEMS.register("packer",
                () -> new BlockItem(ModChemistryBlocks.PACKER.get(), new Item.Properties()));

    }

    public static class ModChemistryBlockEntities {
        public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
                DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Main.MODID);

        public static final RegistryObject<BlockEntityType<BeakerBlockEntity>> BEAKER =
                BLOCK_ENTITIES.register("beaker",
                        () -> BlockEntityType.Builder.of(BeakerBlockEntity::new,
                                ModChemistryBlocks.BEAKER.get(),
                                ModChemistryBlocks.METAL_CRUCIBLE.get(),
                                ModChemistryBlocks.QUARTZ_CRUCIBLE.get(),
                                ModChemistryBlocks.CERAMIC_CRUCIBLE.get()).build(null)
                );

        public static final RegistryObject<BlockEntityType<ElectrolysisCellBlockEntity>> ELECTROLYSIS_CELL =
                BLOCK_ENTITIES.register("electrolysis_cell",
                        () -> BlockEntityType.Builder.of(ElectrolysisCellBlockEntity::new,
                                ModChemistryBlocks.ELECTROLYSIS_CELL.get()).build(null)
                );

        public static final RegistryObject<BlockEntityType<CreativeHeatSourceBlockEntity>> CREATIVE_HEAT_SOURCE =
                BLOCK_ENTITIES.register("creative_heat_source",
                        () -> BlockEntityType.Builder.of(CreativeHeatSourceBlockEntity::new,
                                ModChemistryBlocks.CREATIVE_HEAT_SOURCE.get()).build(null)
                );

        public static final RegistryObject<BlockEntityType<CreativeEnergySourceBlockEntity>> CREATIVE_ENERGY_SOURCE =
                BLOCK_ENTITIES.register("creative_energy_source",
                        () -> BlockEntityType.Builder.of(CreativeEnergySourceBlockEntity::new,
                                ModChemistryBlocks.CREATIVE_ENERGY_SOURCE.get()).build(null)
                );

        public static final RegistryObject<BlockEntityType<PackerBlockEntity>> PACKER =
                BLOCK_ENTITIES.register("packer",
                        () -> BlockEntityType.Builder.of(PackerBlockEntity::new,
                                ModChemistryBlocks.PACKER.get()).build(null)
                );

        public static final RegistryObject<BlockEntityType<ThermostaticBathBlockEntity>> THERMOSTATIC_BATH =
                BLOCK_ENTITIES.register("thermostatic_bath",
                        () -> BlockEntityType.Builder.of(ThermostaticBathBlockEntity::new,
                                ModChemistryBlocks.THERMOSTATIC_BATH.get()).build(null)
                );

        // 搅拌器（§19.29）：内部 FE 缓冲 + 每 tick 扣费
        public static final RegistryObject<BlockEntityType<StirrerBlockEntity>> STIRRER =
                BLOCK_ENTITIES.register("stirrer",
                        () -> BlockEntityType.Builder.of(StirrerBlockEntity::new,
                                ModChemistryBlocks.STIRRER.get()).build(null)
                );

        // 电加热夹套（§19.29）：设定点 + 按交付热量计费
        public static final RegistryObject<BlockEntityType<JacketBlockEntity>> JACKET =
                BLOCK_ENTITIES.register("jacket",
                        () -> BlockEntityType.Builder.of(JacketBlockEntity::new,
                                ModChemistryBlocks.JACKET.get()).build(null)
                );

        // 阀门（§19.32 收尾）：开/关仍是方块状态（那份设计理由不变），
        // 方块实体只承载"腐蚀度"这一件跟着方块走的状态量
        public static final RegistryObject<BlockEntityType<ValveBlockEntity>> VALVE =
                BLOCK_ENTITIES.register("valve",
                        () -> BlockEntityType.Builder.of(ValveBlockEntity::new,
                                ModChemistryBlocks.VALVE.get()).build(null)
                );

        // 实验室反应釜（§19.30）：四档材质共用一个 BE 类型（与坩埚共用烧杯 BE 同一做法）
        public static final RegistryObject<BlockEntityType<LabReactorBlockEntity>> LAB_REACTOR =
                BLOCK_ENTITIES.register("lab_reactor",
                        () -> BlockEntityType.Builder.of(LabReactorBlockEntity::new,
                                ModChemistryBlocks.GLASS_REACTOR.get(),
                                ModChemistryBlocks.METAL_REACTOR.get(),
                                ModChemistryBlocks.QUARTZ_REACTOR.get(),
                                ModChemistryBlocks.CERAMIC_REACTOR.get(),
                                ModChemistryBlocks.GLASS_LINED_REACTOR.get()).build(null)
                );
    }

    public static class ModChemistryItems {
        public static final DeferredRegister<Item> ITEMS =
                DeferredRegister.create(ForgeRegistries.ITEMS, Main.MODID);


        //化学书
        public static final RegistryObject<Item> CHEMISTRY_BOOK = ITEMS.register("chemistry_book",
                () -> new ChemistryBookItem(
                        new Item.Properties()
                                .stacksTo(1)
                                .rarity(Rarity.UNCOMMON)
                )
        );

        // 分析仪
        public static final RegistryObject<AnalyzerItemNormal> ANALYZER = ITEMS.register("analyzer",
                () -> new AnalyzerItemNormal(
                        new Item.Properties()
                                .stacksTo(1)
                )
        );
        public static final RegistryObject<AnalyzerItemCreative> ANALYZER_CREATIVE = ITEMS.register("analyzer_creative",
                () -> new AnalyzerItemCreative(
                        new Item.Properties()
                                .stacksTo(1)
                                .rarity(Rarity.EPIC)
                )
        );

        // ====== 物质基类物品 ======
        public static final RegistryObject<SubstanceItem> SUBSTANCE = ITEMS.register("substance",
                () -> new SubstanceItem(new Item.Properties().stacksTo(64)));

        // ====== 一级物质（§19.15：元素单质 / 教材级基础物 / 跨系统接口）======
        // 组成固定 ⇒ 不需要 trainees:form 谓词与 overrides；每个物品一个固定模型，
        // 直接复用试剂瓶那套两层贴图（layer0 内容物吃 tint / layer1 瓶身）。
        public static final RegistryObject<SubstanceItem> HYDROCHLORIC_ACID = bottleItem("hydrochloric_acid");
        public static final RegistryObject<SubstanceItem> SULFURIC_ACID = bottleItem("sulfuric_acid");
        public static final RegistryObject<SubstanceItem> SODIUM_HYDROXIDE = bottleItem("sodium_hydroxide");
        public static final RegistryObject<SubstanceItem> SODIUM_CARBONATE = bottleItem("sodium_carbonate");
        public static final RegistryObject<SubstanceItem> HYDROGEN_PEROXIDE = bottleItem("hydrogen_peroxide");
        public static final RegistryObject<SubstanceItem> BENZENE = bottleItem("benzene");
        public static final RegistryObject<SubstanceItem> SULFUR = bottleItem("sulfur");
        public static final RegistryObject<SubstanceItem> SODIUM_CHLORIDE = bottleItem("sodium_chloride");

        /** 一级物质：1 个物品 = 1 mol（与 {@code ofPure(ion, 1.0)} 同一量级，便于手算与测试对齐） */
        private static final double MOLES_PER_ITEM = 1.0;

        private static RegistryObject<SubstanceItem> bottleItem(String id) {
            return ITEMS.register(id, () -> new SubstanceItem(new Item.Properties().stacksTo(64)));
        }

        /**
         * 一级物质登记（由 {@code Main.commonSetup} 调用：此时注册表已就绪，{@code .get()} 安全）。
         * 一次登记喂两张表：① 定组成表（物品 → 成分，供容器输入 / 形态 / 染色 / 提示）；
         * ② {@link FirstClassSubstances} 代表物品（物种 → 物品，供 JEI / 配方 / 市场取用）。
         */
        public static void registerFirstClassSubstances() {
            for (FirstClass binding : FIRST_CLASS) {
                FixedCompositionTable.register(binding.item().get(), Map.of(binding.species(), MOLES_PER_ITEM));
                FirstClassSubstances.register(binding.species(), () -> new ItemStack(binding.item().get()));
            }
        }

        /**
         * 一级物质登记表（**单一真源**，§19.34）：物种 ↔ 代表物品。
         *
         * <p>注册（{@link #registerFirstClassSubstances()}）、JEI 槽位、配方/市场，以及
         * **数据生成的物品分类标签**（酸碱盐/氧化剂/还原剂：{@code data/trainees/tags/items/}）
         * 全部读这张表——加一行就全都跟上了，不会出现"注册了但标签没跟上"的漂移。</p>
         */
        public static final List<FirstClass> FIRST_CLASS = List.of(
                new FirstClass(ModIons.HCL_AQ, HYDROCHLORIC_ACID),
                new FirstClass(ModIons.H2SO4, SULFURIC_ACID),
                new FirstClass(ModIons.NaOH, SODIUM_HYDROXIDE),
                new FirstClass(ModIons.Na2CO3, SODIUM_CARBONATE),
                new FirstClass(ModIons.H2O2, HYDROGEN_PEROXIDE),
                new FirstClass(ModIons.C6H6, BENZENE),
                new FirstClass(ModIons.SULFUR, SULFUR),
                new FirstClass(ModIons.NaCl, SODIUM_CHLORIDE)
        );

        /** 一级物质绑定：物种 + 代表物品（record：数据生成要遍历，不方便用裸 Map） */
        public record FirstClass(IonType species, RegistryObject<SubstanceItem> item) {
        }

        // 离子交换膜（§19.10 电解槽：放入膜槽使产物分侧纯化；后期材料升级版后续再换材质/配方）
        public static final RegistryObject<Item> ION_MEMBRANE = ITEMS.register("ion_membrane",
                () -> new Item(
                        new Item.Properties()
                                .stacksTo(16)
                ));

        // 密封釜头（§19.30.2）：装到实验室反应釜上 = 全焊态（全部面封死 + 残留更大 + 清洗到不了底）
        public static final RegistryObject<Item> SEALED_HEAD = ITEMS.register("sealed_head",
                () -> new Item(new Item.Properties().stacksTo(4)));

        // 扳手 / 清洁刷（§19.33）：拆卸与清洗各交给一个明确的工具，别用"潜行 + 空手"这种易混手势
        public static final RegistryObject<Item> WRENCH = ITEMS.register("wrench",
                () -> new WrenchItem(new Item.Properties().stacksTo(1)));
        public static final RegistryObject<Item> BRUSH = ITEMS.register("brush",
                () -> new BrushItem(new Item.Properties().stacksTo(1)));

        // ====== 日化品与次品（§19.18 / Step ④）======
        // 合格/优质两档；次品**按失败类别**共用 item（反噬取决于违规种类，与产品无关）。
        public static final RegistryObject<Item> BLEACH_GOOD = ITEMS.register("bleach_good",
                () -> new Item(new Item.Properties().stacksTo(16)));
        public static final RegistryObject<Item> BLEACH_PREMIUM = ITEMS.register("bleach_premium",
                () -> new Item(new Item.Properties().stacksTo(16)));
        public static final RegistryObject<Item> DEFECTIVE_CHLORINE = ITEMS.register("defective_chlorine",
                () -> new DefectiveProductItem(PackOutcome.DefectKind.CHLORINE,
                        new Item.Properties().stacksTo(16)));
        public static final RegistryObject<Item> DEFECTIVE_ALKALI = ITEMS.register("defective_alkali",
                () -> new DefectiveProductItem(PackOutcome.DefectKind.ALKALI,
                        new Item.Properties().stacksTo(16)));
        public static final RegistryObject<Item> DEFECTIVE_ACID = ITEMS.register("defective_acid",
                () -> new DefectiveProductItem(PackOutcome.DefectKind.ACID,
                        new Item.Properties().stacksTo(16)));
        public static final RegistryObject<Item> DEFECTIVE_CHLORATE = ITEMS.register("defective_chlorate",
                () -> new DefectiveProductItem(PackOutcome.DefectKind.CHLORATE,
                        new Item.Properties().stacksTo(16)));
        // 注：**没有**"浓度不符"次品（§19.18 决策）——非危险失败一律不灌装，次品只能是危险品
        //相酸桶
//        public static final RegistryObject<Item> CHE_HBP_BUCKET = ITEMS.register("che_hbp_bucket",
//                () -> new BucketItem(
//                        ModFluid.SOURCE_CHE_HBP,
//                        new Item.Properties()
//                                .craftRemainder(BUCKET)
//                                .stacksTo(1)
//                ));
        //鸡桶
//        public static final RegistryObject<Item> JI_BUCKET = ITEMS.register("ji_bucket",
//                () -> new BucketItem(
//                        ModFluid.,
//                        new Item.Properties()
//                                .craftRemainder(Items.BUCKET)
//                                .stacksTo(1)
//                ));

//        public static final Item JI_BUCKET = registerItem("ji_bucket",
//                new BucketItem(ModFluid.JI, (new Item.Properties()).craftRemainder(BUCKET).stacksTo(1)));

        // 杂质
        public static final RegistryObject<Item> IMPERFECTION = ITEMS.register("imperfection",
                () -> new Item(
                        new Item.Properties()
                                .fireResistant()
                                .rarity(Rarity.UNCOMMON)
                                .setNoRepair()
                ));
        // 氧化鸡锭
        public static final RegistryObject<Item> CHE_JI2O_INGOT = ITEMS.register("che_ji2o_ingot",
                () -> new Ji2O(new Item.Properties()
                ));
        // 氧化鸡粒
        public static final RegistryObject<Item> CHE_JI2O_NUGGET = ITEMS.register("che_ji2o_nugget",
                () -> new Ji2O(new Item.Properties()
                ));
        // 过氧化鸡锭
        public static final RegistryObject<Item> CHE_JI2O2_INGOT = ITEMS.register("che_ji2o2_ingot",
                () -> new Ji2O2(new Item.Properties()
                ));
        // 过氧化鸡粒
        public static final RegistryObject<Item> CHE_JI2O2_NUGGET = ITEMS.register("che_ji2o2_nugget",
                () -> new Ji2O2(new Item.Properties()
                ));
        // 二氧化黑固体
        public static final RegistryObject<Item> CHE_BPO2_SOLID = ITEMS.register("che_bpo2_solid",
                () -> new BpO2(new Item.Properties()
                ));
        // 三氧化黑固体
        public static final RegistryObject<Item> CHE_BPO3_SOLID = ITEMS.register("che_bpo3_solid",
                () -> new BpO3(new Item.Properties()
                ));
        // 黑化氢粉末
        public static final RegistryObject<Item> CHE_HBP_POWDER = ITEMS.register("che_hbp_powder",
                () -> new HBp(new Item.Properties()
                ));
        // 次黑酸粉末
        public static final RegistryObject<Item> CHE_HBPO_POWDER = ITEMS.register("che_hbpo_powder",
                () -> new HBpO(new Item.Properties()
                ));
        // 黑酸粉末
        public static final RegistryObject<Item> CHE_HBPO_3_POWDER = ITEMS.register("che_hbpo3_powder",
                () -> new HBpO3(new Item.Properties()
                ));
        // 高黑酸粉末
        public static final RegistryObject<Item> CHE_HBPO_4_POWDER = ITEMS.register("che_hbpo4_powder",
                () -> new HBpO4(new Item.Properties()
                ));
        // 氢氧化鸡锭
        public static final RegistryObject<Item> CHE_JIOH_INGOT = ITEMS.register("che_jioh_ingot",
                () -> new JiOH(new Item.Properties()
                ));
        // 氢氧化鸡粒
        public static final RegistryObject<Item> CHE_JIOH_NUGGET = ITEMS.register("che_jioh_nugget",
                () -> new JiOH(new Item.Properties()
                ));
        // 反相素
        public static final RegistryObject<Item> CHE_JIBP_PIECE = ITEMS.register("che_jibp_piece",
                () -> new JiBp(new Item.Properties()
                        .rarity(Rarity.UNCOMMON)
                ));
        // 次黑酸鸡
        public static final RegistryObject<Item> CHE_JIBPO_CRYSTALLIZATION = ITEMS.register("che_jibpo_crystallization",
                () -> new JiBpO(new Item.Properties()
                ));
        // 黑酸鸡
        public static final RegistryObject<Item> CHE_JIBPO_3_CRYSTALLIZATION = ITEMS.register("che_jibpo3_crystallization",
                () -> new JiBpO3(new Item.Properties()
                ));
        // 黑酸鸡
        public static final RegistryObject<Item> CHE_JIBPO_4_CRYSTALLIZATION = ITEMS.register("che_jibpo4_crystallization",
                () -> new JiBpO4(new Item.Properties()
                ));

        // 氢氧化钠（§19.34 删除：新化学线用一级物质 sodium_hydroxide，本项从未被引用）


//        public static final RegistryObject<Item> HYDROCHLORIC_ACID_BUCKET = ITEMS.register(
//                "hydrochloric_acid_bucket",
//                () -> new BucketItem(
//                        ModChemistryFluids.HYDROCHLORIC_ACID,
//                        new Item.Properties().stacksTo(1)
//                )
//        );


    }

//    public static class ModChemistryFluidBlocks {
//        public static final DeferredRegister<Block> BLOCKS =
//                DeferredRegister.create(ForgeRegistries.BLOCKS, Main.MODID);
//
//        public static final RegistryObject<LiquidBlock> HYDROCHLORIC_ACID = BLOCKS.register(
//                "hydrochloric_acid",
//                () -> new LiquidBlock(
//                        ModChemistryFluids.HYDROCHLORIC_ACID,
//                        BlockBehaviour.Properties.of()
//                                .mapColor(MapColor.COLOR_LIGHT_GREEN)
//                                .strength(100f)
//                                .noCollission()
//                                .noLootTable()
//                                .replaceable()
//                                .liquid()
//                )
//        );
//    }

    // ==================== 流体注册 ====================
//    public static class ModChemistryFluids {
//        public static final DeferredRegister<FluidType> FLUID_TYPES =
//                DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, Main.MODID);
//        public static final DeferredRegister<Fluid> FLUIDS =
//                DeferredRegister.create(ForgeRegistries.FLUIDS, Main.MODID);
//
//        // ----- 盐酸流体 -----
//        public static final RegistryObject<FluidType> HYDROCHLORIC_ACID_TYPE = FLUID_TYPES.register(
//                "hydrochloric_acid",
//                () -> new FluidType(
//                        FluidType.Properties.create()
//                                .descriptionId("fluid.trainees.hydrochloric_acid")
//                                .temperature(293) // 常温
//                                .density(1100)    // 比水略重
//                                .viscosity(1000)
//                                .canSwim(false)
//                                .canDrown(true)
//                                .supportsBoating(false)
//                ) {
//                    @Override
//                    public net.minecraft.network.chat.Component getDescription() {
//                        return net.minecraft.network.chat.Component.translatable("fluid.trainees.hydrochloric_acid");
//                    }
//                }
//        );
//
//        public static final RegistryObject<FlowingFluid> HYDROCHLORIC_ACID = FLUIDS.register(
//                "hydrochloric_acid",
//                () -> new ForgeFlowingFluid.Source(
//                        new ForgeFlowingFluid.Properties(
//                                HYDROCHLORIC_ACID_TYPE,
//                                HYDROCHLORIC_ACID,
//                                HYDROCHLORIC_ACID_FLOWING
//                        ).block(ModChemistryFluidBlocks.HYDROCHLORIC_ACID)
//                )
//        );
//
//        public static final RegistryObject<FlowingFluid> HYDROCHLORIC_ACID_FLOWING = FLUIDS.register(
//                "hydrochloric_acid_flowing",
//                () -> new ForgeFlowingFluid.Flowing(
//                        new ForgeFlowingFluid.Properties(
//                                HYDROCHLORIC_ACID_TYPE,
//                                HYDROCHLORIC_ACID,
//                                HYDROCHLORIC_ACID_FLOWING
//                        ).block(ModChemistryFluidBlocks.HYDROCHLORIC_ACID)
//                )
//        );
//
//
//    }



    public static class ModChemistryEquations {
        // HBpO分解反应: 2HBpO =光照= 2HBp + O2↑
        public static final ChemicalEquation HBPO_DECOMPOSITION =
                new ChemicalEquation.Builder("hbpo_decomposition")
                        .withName("HBpO光分解")
                        .withDescription("次黑酸在光照下分解为纽黑粉末和氧气")
                        .addReactant(ModChemistry.ModChemistryItems.CHE_HBPO_POWDER.get(), 2, "次黑酸")
                        .addProduct(ModChemistry.ModChemistryItems.CHE_HBP_POWDER.get(), 2, "纽黑粉末")
                        // .addProduct(ModChemistry.ModChemistryItems.CHE_O2.get(), 1, "氧气")
                        .withTimedConditions(
                                ReactionConditions.hbpoDecomposeCondition.get(),
                                ReactionConditions.hbpoDurationProvider.get()
                        )
//                        .addTag("decomposition")
//                        .addTag("photochemical")
//                        .addTag("redox")
                        .build();

        // Bp2与水反应: Bp2 + H2O == HBp + HBpO
        public static final ChemicalEquation BP2_WATER_REACTION =
                new ChemicalEquation.Builder("bp2_water_reaction")
                        .withName("黑单质水解")
                        .addReactant(ModItems.POWDER_ANTI.get(), 1, "黑单质")
                        .addProduct(ModChemistry.ModChemistryItems.CHE_HBP_POWDER.get(), 1, "纽黑粉末")
                        .addProduct(ModChemistry.ModChemistryItems.CHE_HBPO_POWDER.get(), 1, "次黑酸")
                        .withTimedConditions(
                                ReactionConditions.bp2AndWaterCondition.get(),
                                ReactionConditions.random5to10
                        )
//                        .addTag("hydrolysis")
//                        .addTag("redox")
                        .build();

        // Ji与水反应
        public static final ChemicalEquation JI_WATER_REACTION =
                new ChemicalEquation.Builder("ji_water_reaction")
                        .withName("鸡单质水解")
                        .addReactant(ModItems.TWO_HALF_INGOT.get(), 2)
                        .addProduct(ModChemistryItems.CHE_JIOH_INGOT.get(), 2)
                        .withTimedConditions(
                                ReactionConditions.jiAndWaterCondition.get(),
                                ReactionConditions.dur0
                        )
//                        .addTag("hydrolysis")
//                        .addTag("redox")
                        .build();

//        MachineChemicalEquation example = new MachineChemicalEquation.Builder("ji_water_reaction_machine")
//                .addReactant(2, ModItems.TWO_HALF_INGOT.get())
//                .addProduct(2, ModChemistryItems.CHE_JIOH_INGOT.get())
//                .addCondition(new CatalystCondition(List.of(new ItemStack(SomeItem.ITEM)), 0.1)) // 消耗概率10%
//                .withSequence(1)
//                .withDuration(100) // 100 ticks = 5秒
//                .addTag("machine")
//                .build();
    }

    // ==================== 内容：demo 版（公开快照用） ====================
    //
    // 本文件由 scripts/gen-demo-modchemistry.sh 生成：
    //   · 字段名与真实版一一对应（保证引擎里几百处 ModIons.X 引用仍能编译）
    //   · 所有物质都是**虚构的 demo 物质**（id 前缀 demo_），不含任何真实化学数据
    //   · 反应规则为空（demo 内容包只用于"能编译、能启动、能演示"）
    public static class ModIons {
        /** 与真实版一致：**public**（ElectrolysisCellBlockEntity / GasCodec / JEI 插件都直接引用它）。 */
        public static final java.util.List<IonType> ALL_IONS = new java.util.ArrayList<>();

        private static IonType register(IonType type) {
            ALL_IONS.add(type);
            return type;
        }

        /** demo 物质：虚构参数，仅用于让引擎可运行。 */
        private static IonType demo(String path) {
            return register(new IonType.Builder(new ResourceLocation(Main.MODID, "demo_" + path), Phase.SOLID)
                    .molarMass(100.0)
                    .specificHeat(50.0)
                    .formationEnthalpy(0.0)
                    .formationGibbs(0.0)
                    .build());
        }

        public static final IonType H_plus = demo("h_plus");
        public static final IonType Na_1 = demo("na_1");
        public static final IonType K_1 = demo("k_1");
        public static final IonType NH4_plus = demo("nh4_plus");
        public static final IonType Fe_2 = demo("fe_2");
        public static final IonType Fe_3 = demo("fe_3");
        public static final IonType Cu_1 = demo("cu_1");
        public static final IonType Cu_2 = demo("cu_2");
        public static final IonType Cl_minus = demo("cl_minus");
        public static final IonType ClO_minus = demo("clo_minus");
        public static final IonType ClO3_minus = demo("clo3_minus");
        public static final IonType OH_minus = demo("oh_minus");
        public static final IonType NO3_minus = demo("no3_minus");
        public static final IonType SO4_2minus = demo("so4_2minus");
        public static final IonType CO3_2minus = demo("co3_2minus");
        public static final IonType HCl = demo("hcl");
        public static final IonType HCL_AQ = demo("hcl_aq");
        public static final IonType Cl2 = demo("cl2");
        public static final IonType O2 = demo("o2");
        public static final IonType N2 = demo("n2");
        public static final IonType CO = demo("co");
        public static final IonType C = demo("c");
        public static final IonType H2 = demo("h2");
        public static final IonType CO2 = demo("co2");
        public static final IonType SO2 = demo("so2");
        public static final IonType SO3 = demo("so3");
        public static final IonType NH3 = demo("nh3");
        public static final IonType H2O_GAS = demo("h2o_gas");
        public static final IonType H2O = demo("h2o");
        public static final IonType H2O2 = demo("h2o2");
        public static final IonType H2O_SOLID = demo("h2o_solid");
        public static final IonType NaCl = demo("nacl");
        public static final IonType NaCl_MOLTEN = demo("nacl_molten");
        public static final IonType NaOH = demo("naoh");
        public static final IonType NaOH_MOLTEN = demo("naoh_molten");
        public static final IonType Na2CO3 = demo("na2co3");
        public static final IonType MnO2 = demo("mno2");
        public static final IonType CaCO3 = demo("caco3");
        public static final IonType CaO = demo("cao");
        public static final IonType Ca_OH_2 = demo("ca_oh_2");
        public static final IonType NaHSO4 = demo("nahso4");
        public static final IonType Fe = demo("fe");
        public static final IonType Cu = demo("cu");
        public static final IonType H2SO4 = demo("h2so4");
        public static final IonType Fe2O3 = demo("fe2o3");
        public static final IonType SiO2 = demo("sio2");
        public static final IonType Cu2O = demo("cu2o");
        public static final IonType Mg_2 = demo("mg_2");
        public static final IonType Ca_2 = demo("ca_2");
        public static final IonType C8H18 = demo("c8h18");
        public static final IonType C10H22 = demo("c10h22");
        public static final IonType C12H26 = demo("c12h26");
        public static final IonType C6H6 = demo("c6h6");
        public static final IonType SULFUR = demo("sulfur");
        public static final IonType N2O4 = demo("n2o4");
        public static final IonType NO2 = demo("no2");
        public static final IonType HCO3_minus = demo("hco3_minus");
        public static final IonType Na = demo("na");

        public static IonType getById(ResourceLocation id) {
            return ALL_IONS.stream().filter(ion -> ion.getId().equals(id)).findFirst().orElse(null);
        }

        public static java.util.List<IonType> all() {
            return java.util.List.copyOf(ALL_IONS);
        }
    }

    // ==================== 反应注册：demo 版 ====================
    public static class Reactions {
        /**
         * demo 内容包**没有任何反应规则**。
         *
         * <p>真实内容包的规则由数据包提供（chemistry/reactions/*.json），本方法保持空实现，
         * 这样公开版不含任何真实反应数据，但引擎与 UI 仍然可运行。</p>
         */
        public static void registerAll() {
            // 无规则
        }

        /**
         * 真实版里这是私有的相变注册（由 registerAll 内部调用）；这里保留一个**同名公有空实现**，
         * 因为测试会直接调它（demo 版没有相变规则，空实现即可）。
         */
        public static void registerPhaseTransitions() {
            // 无相变规则
        }
    }
}
