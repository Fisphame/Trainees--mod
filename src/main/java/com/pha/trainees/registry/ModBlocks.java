package com.pha.trainees.registry;

import com.pha.trainees.Main;
import com.pha.trainees.block.*;
import com.pha.trainees.blockentity.AbsorbBlockEntity;
import com.pha.trainees.blockentity.AltarCoreBlockEntity;
import com.pha.trainees.blockentity.KunAltarBlockEntity;
import com.pha.trainees.blockentity.ManaExtractorBlockEntity;
import com.pha.trainees.blockentity.EnrichmentChamberBlockEntity;
import com.pha.trainees.blockentity.RefinedAggregatorBlockEntity;
import com.pha.trainees.blockentity.AbsorbBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


public class ModBlocks {

    public static class ModBlockEntities {
        public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
                DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Main.MODID);

        public static final RegistryObject<BlockEntityType<KunAltarBlockEntity>> KUN_ALTAR_ENTITY =
                BLOCK_ENTITIES.register("kun_altar_block",
                        () -> BlockEntityType.Builder.of(
                                KunAltarBlockEntity::new,
                                ModBlocks.KUN_ALTAR.get()
                        ).build(null));

        /** 祭坛主方块：魔力的载体（§17.1） */
        public static final RegistryObject<BlockEntityType<AltarCoreBlockEntity>> ALTAR_CORE_ENTITY =
                BLOCK_ENTITIES.register("altar_core",
                        () -> BlockEntityType.Builder.of(
                                AltarCoreBlockEntity::new,
                                ModBlocks.ALTAR_CORE_BLOCK.get()
                        ).build(null));

        /** 提取机：5 个档位方块共用一个方块实体类型（档位从 BlockState 读）。 */
        public static final RegistryObject<BlockEntityType<ManaExtractorBlockEntity>> MANA_EXTRACTOR =
                BLOCK_ENTITIES.register("mana_extractor",
                        () -> BlockEntityType.Builder.of(
                                ManaExtractorBlockEntity::new,
                                ModBlocks.EXTRACTOR_PRIMARY.get(),
                                ModBlocks.EXTRACTOR_ADVANCED.get(),
                                ModBlocks.EXTRACTOR_HIGH.get(),
                                ModBlocks.EXTRACTOR_ULTIMATE.get(),
                                ModBlocks.EXTRACTOR_CREATIVE.get()
                        ).build(null));

        /** 聚合机（§17.1） */
        public static final RegistryObject<BlockEntityType<RefinedAggregatorBlockEntity>> REFINED_AGGREGATOR =
                BLOCK_ENTITIES.register("refined_aggregator",
                        () -> BlockEntityType.Builder.of(
                                RefinedAggregatorBlockEntity::new,
                                ModBlocks.REFINED_AGGREGATOR.get()
                        ).build(null));

        /** 富集仓：5 个档位方块共用一个方块实体类型（档位从 BlockState 读） */
        public static final RegistryObject<BlockEntityType<EnrichmentChamberBlockEntity>> ENRICHMENT_CHAMBER =
                BLOCK_ENTITIES.register("enrichment_chamber",
                        () -> BlockEntityType.Builder.of(
                                EnrichmentChamberBlockEntity::new,
                                ModBlocks.ENRICHMENT_PRIMARY.get(),
                                ModBlocks.ENRICHMENT_ADVANCED.get(),
                                ModBlocks.ENRICHMENT_HIGH.get(),
                                ModBlocks.ENRICHMENT_ULTIMATE.get(),
                                ModBlocks.ENRICHMENT_CREATIVE.get()
                        ).build(null));

        public static final RegistryObject<BlockEntityType<AbsorbBlockEntity>> ABSORB_BLOCK_ENTITY =
                BLOCK_ENTITIES.register("absorb_block",
                        () -> BlockEntityType.Builder.of(
                                AbsorbBlockEntity::new,
                                ModBlocks.ABSORB_BLOCK.get()
                        ).build(null));
    }

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Main.MODID);

    //顺序：建筑方块 -> 染色方块 -> 自然方块 -> 功能方块 -> 红石方块

    public static final RegistryObject<Block> TWO_HALF_INGOT_BLOCK = BLOCKS.register("two_half_ingot_block",
            () -> new TwoHalfIngotCourseBlock.TwoHalfIngotBlock(
            BlockBehaviour.Properties.of()
                    .strength(2f,6f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
    ));

    public static final RegistryObject<Block> WAXED_TWO_HALF_INGOT_BLOCK = BLOCKS.register("waxed_two_half_ingot_block",
            () -> new TwoHalfIngotCourseBlock.WaxedTwoHalfIngotBlock(
                    BlockBehaviour.Properties.of()
                            .strength(2f,6f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()
            ));

    public static final RegistryObject<Block> ANTI_TWO_HALF_INGOT_BLOCK = BLOCKS.register("anti_two_half_ingot_block",
            () -> new TwoHalfIngotCourseBlock.AntiTwoHalfIngotBlock(
                    BlockBehaviour.Properties.of()
                            .strength(4f, 10f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()
            ));
// Pullium Inversine   Pullium inverside
    // 金矽块   /ɔːrɪvərsaɪt/
    public static final RegistryObject<Block> AURIVERSITE_BLOCK = BLOCKS.register("auriversite_block",
            () -> new AuriversiteBlock(
               BlockBehaviour.Properties.of()
                       .mapColor(MapColor.METAL)
                       .strength(3f, 8f)
                       .sound(SoundType.METAL)
                       .requiresCorrectToolForDrops()
            ));

    public static final RegistryObject<Block> FEATHER_BLOCK = BLOCKS.register("feather_block",
            () -> new Block(
            BlockBehaviour.Properties.of()
                    .strength(0.05f,0f)
                    .sound(SoundType.WOOL)
                    .instabreak()
                    .noCollission()
    ));

    // 鈅
    public static final RegistryObject<Block> SELENAURITE_ORE = BLOCKS.register("selenaurite_ore",
            () -> new Block(
            BlockBehaviour.Properties.of()
                    .strength(3f,3f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
            )
    );
    public static final RegistryObject<Block> DEEPSLATE_SELENAURITE_ORE = BLOCKS.register("deepslate_selenaurite_ore",
            () -> new Block(
                BlockBehaviour.Properties.of()
                        .strength(4.5f, 3f)
                        .sound(SoundType.DEEPSLATE)
                        .requiresCorrectToolForDrops()
            )
    );
    public static final RegistryObject<Block> SELENAURITE_BLOCK = BLOCKS.register("selenaurite_block",
            () -> new Block(
                    BlockBehaviour.Properties.of()
                            .strength(5f,6f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()
            )
    );
    // Nyctium  Ny
    // 钅辐
    public static final RegistryObject<Block> NYCTIUM_ORE = BLOCKS.register("nyctium_ore",
            () -> new Block(
                    BlockBehaviour.Properties.of()
                            .strength(3f,3f)
                            .sound(SoundType.STONE)
                            .requiresCorrectToolForDrops()
            )
    );
    public static final RegistryObject<Block> DEEPSLATE_NYCTIUM_ORE = BLOCKS.register("deepslate_nyctium_ore",
            () -> new Block(
                    BlockBehaviour.Properties.of()
                            .strength(4.5f, 3f)
                            .sound(SoundType.DEEPSLATE)
                            .requiresCorrectToolForDrops()
            )
    );
    public static final RegistryObject<Block> NYCTIUM_BLOCK = BLOCKS.register("nyctium_block",
            () -> new Block(
                    BlockBehaviour.Properties.of()
                            .strength(5f,6f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()
            )
    );
    // Terapium   Te
    // 钅寺
    public static final RegistryObject<Block> TERAPIUM_ORE = BLOCKS.register("terapium_ore",
            () -> new Block(
                    BlockBehaviour.Properties.of()
                            .strength(3f,3f)
                            .sound(SoundType.STONE)
                            .requiresCorrectToolForDrops()
            )
    );
    public static final RegistryObject<Block> DEEPSLATE_TERAPIUM_ORE = BLOCKS.register("deepslate_terapium_ore",
            () -> new Block(
                    BlockBehaviour.Properties.of()
                            .strength(4.5f, 3f)
                            .sound(SoundType.DEEPSLATE)
                            .requiresCorrectToolForDrops()
            )
    );
    public static final RegistryObject<Block> TERAPIUM_BLOCK = BLOCKS.register("terapium_block",
            () -> new Block(
                    BlockBehaviour.Properties.of()
                            .strength(5f,6f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()
            )
    );
    // Banalium  Bn
    // 钅朋
    public static final RegistryObject<Block> BANALIUM_ORE = BLOCKS.register("banalium_ore",
            () -> new Block(
                    BlockBehaviour.Properties.of()
                            .strength(3f,3f)
                            .sound(SoundType.STONE)
                            .requiresCorrectToolForDrops()
            )
    );
    public static final RegistryObject<Block> DEEPSLATE_BANALIUM_ORE = BLOCKS.register("deepslate_banalium_ore",
            () -> new Block(
                    BlockBehaviour.Properties.of()
                            .strength(4.5f, 3f)
                            .sound(SoundType.DEEPSLATE)
                            .requiresCorrectToolForDrops()
            )
    );
    public static final RegistryObject<Block> BANALIUM_BLOCK = BLOCKS.register("banalium_block",
            () -> new Block(
                    BlockBehaviour.Properties.of()
                            .strength(5f,6f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()
            )
    );
    // Nivtium  Nt

    // 钅宁
    public static final RegistryObject<Block> NIVTIUM_ORE = BLOCKS.register("nivtium_ore",
            () -> new Block(
                    BlockBehaviour.Properties.of()
                            .strength(3f,3f)
                            .sound(SoundType.STONE)
                            .requiresCorrectToolForDrops()
            )
    );
    public static final RegistryObject<Block> DEEPSLATE_NIVTIUM_ORE = BLOCKS.register("deepslate_nivtium_ore",
            () -> new Block(
                    BlockBehaviour.Properties.of()
                            .strength(4.5f, 3f)
                            .sound(SoundType.DEEPSLATE)
                            .requiresCorrectToolForDrops()
            )
    );
    public static final RegistryObject<Block> NIVTIUM_BLOCK = BLOCKS.register("nivtium_block",
            () -> new Block(
                    BlockBehaviour.Properties.of()
                            .strength(5f,6f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()
            )
    );
    // 钅克
    // Crucium  Ck
    public static final RegistryObject<Block> CRUCIUM_ORE = BLOCKS.register("crucium_ore",
            () -> new Block(
                    BlockBehaviour.Properties.of()
                            .strength(3f,3f)
                            .sound(SoundType.STONE)
                            .requiresCorrectToolForDrops()
            )
    );
    public static final RegistryObject<Block> DEEPSLATE_CRUCIUM_ORE = BLOCKS.register("deepslate_crucium_ore",
            () -> new Block(
                    BlockBehaviour.Properties.of()
                            .strength(4.5f, 3f)
                            .sound(SoundType.DEEPSLATE)
                            .requiresCorrectToolForDrops()
            )
    );
    public static final RegistryObject<Block> CRUCIUM_BLOCK = BLOCKS.register("crucium_block",
            () -> new Block(
                    BlockBehaviour.Properties.of()
                            .strength(5f,6f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()
            )
    );
    // 钅丝
    // Sertium  St
    public static final RegistryObject<Block> SERTIUM_ORE = BLOCKS.register("sertium_ore",
            () -> new Block(
                    BlockBehaviour.Properties.of()
                            .strength(3f,3f)
                            .sound(SoundType.STONE)
                            .requiresCorrectToolForDrops()
            )
    );
    public static final RegistryObject<Block> DEEPSLATE_SERTIUM_ORE = BLOCKS.register("deepslate_sertium_ore",
            () -> new Block(
                    BlockBehaviour.Properties.of()
                            .strength(4.5f, 3f)
                            .sound(SoundType.DEEPSLATE)
                            .requiresCorrectToolForDrops()
            )
    );
    public static final RegistryObject<Block> SERTIUM_BLOCK = BLOCKS.register("sertium_block",
            () -> new Block(
                    BlockBehaviour.Properties.of()
                            .strength(5f,6f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()
            )
    );
    // Placium  Pl
    // 钅比
    public static final RegistryObject<Block> PLACIUM_ORE = BLOCKS.register("placium_ore",
            () -> new Block(
                    BlockBehaviour.Properties.of()
                            .strength(3f,3f)
                            .sound(SoundType.STONE)
                            .requiresCorrectToolForDrops()
            )
    );
    public static final RegistryObject<Block> DEEPSLATE_PLACIUM_ORE = BLOCKS.register("deepslate_placium_ore",
            () -> new Block(
                    BlockBehaviour.Properties.of()
                            .strength(4.5f, 3f)
                            .sound(SoundType.DEEPSLATE)
                            .requiresCorrectToolForDrops()
            )
    );
    public static final RegistryObject<Block> PLACIUM_BLOCK = BLOCKS.register("placium_block",
            () -> new Block(
                    BlockBehaviour.Properties.of()
                            .strength(5f,6f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()
            )
    );




    public static final RegistryObject<Block> MYBLOCK = BLOCKS.register("myblock",
            () -> new Block(
            BlockBehaviour.Properties.of()
                    .strength(15.0f,1200f)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 15)
                    .emissiveRendering((state,world,pos) -> true)
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
    ));



    //反相篮球
    public static final RegistryObject<Block> BASKETBALL_ANTI_BLOCK = BLOCKS.register("basketball_anti_block",
            () -> new BasketballAntiBlock(
                    BlockBehaviour.Properties.of()
                            .strength(0.01f,3f)
                            .sound(SoundType.WOOL)
                            .mapColor(MapColor.COLOR_BLUE)
            )
    );

    public static final RegistryObject<Block> ALTAR_CORE_BLOCK = BLOCKS.register("altar_core_block",
            () -> new AltarCoreBlock(
                    BlockBehaviour.Properties.of()
                            .strength(7.5f, 15f)
                            .sound(SoundType.NETHER_BRICKS)
                            .mapColor(MapColor.STONE)
                            .requiresCorrectToolForDrops()
                            .lightLevel(state -> 0) // 确保不发光
                            .noOcclusion() // 允许相邻方块渲染其背面，对透明方块很重要
            )
    );

    /** 精炼精华缓存单元（§17.1）：相邻给祭坛主方块扩容，每个 +144,000 RE（配置可调）。 */    public static final RegistryObject<Block> MANA_CACHE_UNIT = BLOCKS.register("mana_cache_unit",
            () -> new ManaCacheUnitBlock(
                    BlockBehaviour.Properties.of()
                            .strength(3.0f, 6.0f)
                            .sound(SoundType.AMETHYST)
                            .mapColor(MapColor.COLOR_PURPLE)
                            .requiresCorrectToolForDrops()
                            .noOcclusion()
            )
    );

    // ===== 提取机（§17.1）：5 个注册方块 = 5 个档位（初级/中级/高级/终极/创造）=====
    // 档位是方块属性，不是升级件（作者 2026-10-02）；5 个方块共用一个方块实体类型。
    public static final RegistryObject<Block> EXTRACTOR_PRIMARY = BLOCKS.register("extractor_primary",
            () -> new ManaExtractorBlock(0, extractorProperties(MapColor.COLOR_ORANGE)));

    public static final RegistryObject<Block> EXTRACTOR_ADVANCED = BLOCKS.register("extractor_advanced",
            () -> new ManaExtractorBlock(1, extractorProperties(MapColor.METAL)));

    public static final RegistryObject<Block> EXTRACTOR_HIGH = BLOCKS.register("extractor_high",
            () -> new ManaExtractorBlock(2, extractorProperties(MapColor.GOLD)));

    public static final RegistryObject<Block> EXTRACTOR_ULTIMATE = BLOCKS.register("extractor_ultimate",
            () -> new ManaExtractorBlock(3, extractorProperties(MapColor.COLOR_BLACK)));

    /** 创造档：纯 mod 内不可获取（作者计划由整合包给） */
    public static final RegistryObject<Block> EXTRACTOR_CREATIVE = BLOCKS.register("extractor_creative",
            () -> new ManaExtractorBlock(4, extractorProperties(MapColor.COLOR_PURPLE)));

    /** 聚合机（§17.1）：RE → 物品，1 档、处理很快；默认配方 1600 RE → 1 精炼点。 */
    public static final RegistryObject<Block> REFINED_AGGREGATOR = BLOCKS.register("refined_aggregator",
            () -> new RefinedAggregatorBlock(BlockBehaviour.Properties.of()
                    .strength(3.5f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .mapColor(MapColor.DIAMOND)));

    // ===== 富集仓（§17.1）：5 个注册方块 = 5 个档位（与提取机同一套档位观）=====
    public static final RegistryObject<Block> ENRICHMENT_PRIMARY = BLOCKS.register("enrichment_primary",
            () -> new EnrichmentChamberBlock(0, extractorProperties(MapColor.COLOR_ORANGE)));

    public static final RegistryObject<Block> ENRICHMENT_ADVANCED = BLOCKS.register("enrichment_advanced",
            () -> new EnrichmentChamberBlock(1, extractorProperties(MapColor.METAL)));

    public static final RegistryObject<Block> ENRICHMENT_HIGH = BLOCKS.register("enrichment_high",
            () -> new EnrichmentChamberBlock(2, extractorProperties(MapColor.GOLD)));

    public static final RegistryObject<Block> ENRICHMENT_ULTIMATE = BLOCKS.register("enrichment_ultimate",
            () -> new EnrichmentChamberBlock(3, extractorProperties(MapColor.COLOR_BLACK)));

    public static final RegistryObject<Block> ENRICHMENT_CREATIVE = BLOCKS.register("enrichment_creative",
            () -> new EnrichmentChamberBlock(4, extractorProperties(MapColor.COLOR_PURPLE)));

    /** 提取机共用属性：机器手感统一（石头硬度、需要镐、金属音）。 */
    private static BlockBehaviour.Properties extractorProperties(MapColor color) {
        return BlockBehaviour.Properties.of()
                .strength(3.5f, 6.0f)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL)
                .mapColor(color);
    }

    public static final RegistryObject<Block> KUN_ALTAR = BLOCKS.register("kun_altar",            () -> new KunAltarBlock(
                    BlockBehaviour.Properties.of()
                            .strength(2.5f, 2.5f)
                            .sound(SoundType.STONE)
                            .mapColor(MapColor.STONE)
                            .requiresCorrectToolForDrops()
                            .lightLevel(state -> 0) // 确保不发光
                            .noOcclusion() // 允许相邻方块渲染其背面，对透明方块很重要
            )
    );

    public static final RegistryObject<Block> ABSORB_BLOCK = BLOCKS.register("absorb_block",
            () -> new AbsorbBlock(
                    BlockBehaviour.Properties.of()
                            .strength(1.5f, 6.0f)
                            .sound(SoundType.STONE)
                            .mapColor(MapColor.STONE)
                            .requiresCorrectToolForDrops()
            )
    );


}






