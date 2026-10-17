package com.pha.trainees.registry;

import com.pha.trainees.Main;
import com.pha.trainees.item.*;
import com.pha.trainees.item.materials.TrainMaterial;
import com.pha.trainees.util.game.enums.ModTiers;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Main.MODID);

    //顺序：方块（block item） -> 工具与实用物品 -> 战斗用品 -> 食物与饮品 -> 原材料 -> 刷怪蛋

    private static RegistryObject<Item> registerBlockItem(String name, Block block) {
        return registerBlockItem(name, block, new Item.Properties());
    }
    private static RegistryObject<Item> registerBlockItem(String name, Block block, Item.Properties properties) {
        return ITEMS.register(name, () -> new BlockItem(block, properties));
    }

    //两锭半块
    public static final RegistryObject<Item> TWO_HALF_INGOT_BLOCK_ITEM = ITEMS.register("two_half_ingot_block",
            () -> new KunCourseItem.TwoHalfIngotBlockItem(ModBlocks.TWO_HALF_INGOT_BLOCK.get(),
                    new Item.Properties()
            ));
    // 涂蜡的两锭半块
    public static final RegistryObject<Item> WAXED_TWO_HALF_INGOT_BLOCK_ITEM = ITEMS.register("waxed_two_half_ingot_block",
            () -> new BlockItem(ModBlocks.WAXED_TWO_HALF_INGOT_BLOCK.get(),
                    new Item.Properties()
            ));
    // 反两锭半块
    public static final RegistryObject<Item> ANTI_TWO_HALF_INGOT_BLOCK_ITEM = ITEMS.register("anti_two_half_ingot_block",
            () -> new BlockItem(ModBlocks.ANTI_TWO_HALF_INGOT_BLOCK.get(),
                    new Item.Properties()
            ));
    // 金矽块
    public static final RegistryObject<Item> AURIVERSITE_BLOCK_ITEM = ITEMS.register("auriversite_block",
            () -> new BlockItem(ModBlocks.AURIVERSITE_BLOCK.get(),
                    new Item.Properties()
            ));
    //羽毛方块
    public static final RegistryObject<Item> FEATHER_BLOCK_ITEM = ITEMS.register("feather_block",
            () -> new BlockItem(ModBlocks.FEATHER_BLOCK.get(),
            new Item.Properties()
    ));
    //鈅矿
    public static final RegistryObject<Item> SELENAURITE_ORE_ITEM = ITEMS.register("selenaurite_ore",
            () -> new BlockItem(ModBlocks.SELENAURITE_ORE.get(),
            new Item.Properties()
    ));
    //深层鈅矿
    public static final RegistryObject<Item> DEEPSLATE_SELENAURITE_ORE_ITEM = ITEMS.register("deepslate_selenaurite_ore",
            () -> new BlockItem(ModBlocks.DEEPSLATE_SELENAURITE_ORE.get(),
                    new Item.Properties()
            ));
    //鈅块
    public static final RegistryObject<Item> SELENAURITE_BLOCK_ITEM = ITEMS.register("selenaurite_block",
            () -> new BlockItem(ModBlocks.SELENAURITE_BLOCK.get(),
                    new Item.Properties()
            ));
    // 一堆矿石
    public static final RegistryObject<Item> NYCTIUM_ORE_ITEM = ITEMS.register("nyctium_ore",
            () -> new BlockItem(ModBlocks.NYCTIUM_ORE.get(),
                    new Item.Properties()
            ));
    public static final RegistryObject<Item> DEEPSLATE_NYCTIUM_ORE_ITEM = ITEMS.register("deepslate_nyctium_ore",
            () -> new BlockItem(ModBlocks.DEEPSLATE_NYCTIUM_ORE.get(),
                    new Item.Properties()
            ));
    public static final RegistryObject<Item> NYCTIUM_BLOCK_ITEM = ITEMS.register("nyctium_block",
            () -> new BlockItem(ModBlocks.NYCTIUM_BLOCK.get(),
                    new Item.Properties()
            ));
    public static final RegistryObject<Item> TERAPIUM_ORE_ITEM = ITEMS.register("terapium_ore",
            () -> new BlockItem(ModBlocks.TERAPIUM_ORE.get(),
                    new Item.Properties()
            ));
    public static final RegistryObject<Item> DEEPSLATE_TERAPIUM_ORE_ITEM = ITEMS.register("deepslate_terapium_ore",
            () -> new BlockItem(ModBlocks.DEEPSLATE_TERAPIUM_ORE.get(),
                    new Item.Properties()
            ));
    public static final RegistryObject<Item> TERAPIUM_BLOCK_ITEM = ITEMS.register("terapium_block",
            () -> new BlockItem(ModBlocks.TERAPIUM_BLOCK.get(),
                    new Item.Properties()
            ));
    public static final RegistryObject<Item> BANALIUM_ORE_ITEM = ITEMS.register("banalium_ore",
            () -> new BlockItem(ModBlocks.BANALIUM_ORE.get(),
                    new Item.Properties()
            ));
    public static final RegistryObject<Item> DEEPSLATE_BANALIUM_ORE_ITEM = ITEMS.register("deepslate_banalium_ore",
            () -> new BlockItem(ModBlocks.DEEPSLATE_BANALIUM_ORE.get(),
                    new Item.Properties()
            ));
    public static final RegistryObject<Item> BANALIUM_BLOCK_ITEM = ITEMS.register("banalium_block",
            () -> new BlockItem(ModBlocks.BANALIUM_BLOCK.get(),
                    new Item.Properties()
            ));
    public static final RegistryObject<Item> NIVTIUM_ORE_ITEM = ITEMS.register("nivtium_ore",
            () -> new BlockItem(ModBlocks.NIVTIUM_ORE.get(),
                    new Item.Properties()
            ));
    public static final RegistryObject<Item> DEEPSLATE_NIVTIUM_ORE_ITEM = ITEMS.register("deepslate_nivtium_ore",
            () -> new BlockItem(ModBlocks.DEEPSLATE_NIVTIUM_ORE.get(),
                    new Item.Properties()
            ));
    public static final RegistryObject<Item> NIVTIUM_BLOCK_ITEM = ITEMS.register("nivtium_block",
            () -> new BlockItem(ModBlocks.NIVTIUM_BLOCK.get(),
                    new Item.Properties()
            ));
    public static final RegistryObject<Item> CRUCIUM_ORE_ITEM = ITEMS.register("crucium_ore",
            () -> new BlockItem(ModBlocks.CRUCIUM_ORE.get(),
                    new Item.Properties()
            ));
    public static final RegistryObject<Item> DEEPSLATE_CRUCIUM_ORE_ITEM = ITEMS.register("deepslate_crucium_ore",
            () -> new BlockItem(ModBlocks.DEEPSLATE_CRUCIUM_ORE.get(),
                    new Item.Properties()
            ));
    public static final RegistryObject<Item> CRUCIUM_BLOCK_ITEM = ITEMS.register("crucium_block",
            () -> new BlockItem(ModBlocks.CRUCIUM_BLOCK.get(),
                    new Item.Properties()
            ));
    public static final RegistryObject<Item> SERTIUM_ORE_ITEM = ITEMS.register("sertium_ore",
            () -> new BlockItem(ModBlocks.SERTIUM_ORE.get(),
                    new Item.Properties()
            ));
    public static final RegistryObject<Item> DEEPSLATE_SERTIUM_ORE_ITEM = ITEMS.register("deepslate_sertium_ore",
            () -> new BlockItem(ModBlocks.DEEPSLATE_SERTIUM_ORE.get(),
                    new Item.Properties()
            ));
    public static final RegistryObject<Item> SERTIUM_BLOCK_ITEM = ITEMS.register("sertium_block",
            () -> new BlockItem(ModBlocks.SERTIUM_BLOCK.get(),
                    new Item.Properties()
            ));
    public static final RegistryObject<Item> PLACIUM_ORE_ITEM = ITEMS.register("placium_ore",
            () -> new BlockItem(ModBlocks.PLACIUM_ORE.get(),
                    new Item.Properties()
            ));
    public static final RegistryObject<Item> DEEPSLATE_PLACIUM_ORE_ITEM = ITEMS.register("deepslate_placium_ore",
            () -> new BlockItem(ModBlocks.DEEPSLATE_PLACIUM_ORE.get(),
                    new Item.Properties()
            ));
    public static final RegistryObject<Item> PLACIUM_BLOCK_ITEM = ITEMS.register("placium_block",
            () -> new BlockItem(ModBlocks.PLACIUM_BLOCK.get(),
                    new Item.Properties()
            ));

    //只因方块
    public static final RegistryObject<Item> myblockitem = ITEMS.register("myblock",
            () -> new BlockItem(ModBlocks.MYBLOCK.get(),
            new Item.Properties()
                    .rarity(Rarity.RARE)
    ));

    //祭坛核心
    public static final RegistryObject<Item> ALTAR_CORE_BLOCK_ITEM = ITEMS.register("altar_core_block",
            () -> new BlockItem(ModBlocks.ALTAR_CORE_BLOCK.get(),
                    new Item.Properties()
            )
    );

    //精炼精华缓存单元（§17.1）：贴祭坛主方块放，扩容
    public static final RegistryObject<Item> MANA_CACHE_UNIT_ITEM = ITEMS.register("mana_cache_unit",
            () -> new BlockItem(ModBlocks.MANA_CACHE_UNIT.get(),
                    new Item.Properties()
            )
    );

    //提取机（§17.1）：5 个档位方块
    public static final RegistryObject<Item> EXTRACTOR_PRIMARY_ITEM = ITEMS.register("extractor_primary",
            () -> new BlockItem(ModBlocks.EXTRACTOR_PRIMARY.get(), new Item.Properties()));
    public static final RegistryObject<Item> EXTRACTOR_ADVANCED_ITEM = ITEMS.register("extractor_advanced",
            () -> new BlockItem(ModBlocks.EXTRACTOR_ADVANCED.get(), new Item.Properties()));
    public static final RegistryObject<Item> EXTRACTOR_HIGH_ITEM = ITEMS.register("extractor_high",
            () -> new BlockItem(ModBlocks.EXTRACTOR_HIGH.get(), new Item.Properties()));
    public static final RegistryObject<Item> EXTRACTOR_ULTIMATE_ITEM = ITEMS.register("extractor_ultimate",
            () -> new BlockItem(ModBlocks.EXTRACTOR_ULTIMATE.get(), new Item.Properties()));
    public static final RegistryObject<Item> EXTRACTOR_CREATIVE_ITEM = ITEMS.register("extractor_creative",
            () -> new BlockItem(ModBlocks.EXTRACTOR_CREATIVE.get(), new Item.Properties()));

    //聚合机（§17.1）：RE → 物品
    public static final RegistryObject<Item> REFINED_AGGREGATOR_ITEM = ITEMS.register("refined_aggregator",
            () -> new BlockItem(ModBlocks.REFINED_AGGREGATOR.get(), new Item.Properties()));

    //精炼点（§17.1）：加点系统的货币（聚合机产出；从背包里消耗）
    public static final RegistryObject<Item> REFINED_POINT = ITEMS.register("refined_point",
            () -> new Item(new Item.Properties().stacksTo(64)));

    //富集仓（§17.1）：5 个档位方块
    public static final RegistryObject<Item> ENRICHMENT_PRIMARY_ITEM = ITEMS.register("enrichment_primary",
            () -> new BlockItem(ModBlocks.ENRICHMENT_PRIMARY.get(), new Item.Properties()));
    public static final RegistryObject<Item> ENRICHMENT_ADVANCED_ITEM = ITEMS.register("enrichment_advanced",
            () -> new BlockItem(ModBlocks.ENRICHMENT_ADVANCED.get(), new Item.Properties()));
    public static final RegistryObject<Item> ENRICHMENT_HIGH_ITEM = ITEMS.register("enrichment_high",
            () -> new BlockItem(ModBlocks.ENRICHMENT_HIGH.get(), new Item.Properties()));
    public static final RegistryObject<Item> ENRICHMENT_ULTIMATE_ITEM = ITEMS.register("enrichment_ultimate",
            () -> new BlockItem(ModBlocks.ENRICHMENT_ULTIMATE.get(), new Item.Properties()));
    public static final RegistryObject<Item> ENRICHMENT_CREATIVE_ITEM = ITEMS.register("enrichment_creative",
            () -> new BlockItem(ModBlocks.ENRICHMENT_CREATIVE.get(), new Item.Properties()));
    //只因祭坛
    public static final RegistryObject<Item> KUN_ALTAR_BLOCK_ITEM = ITEMS.register("kun_altar",
            () -> new BlockItem(ModBlocks.KUN_ALTAR.get(),
                    new Item.Properties()
            )
    );
    // 汲取方块
    public static final RegistryObject<Item> ABSORB_BLOCK_ITEM = ITEMS.register("absorb_block",
            () -> new BlockItem(ModBlocks.ABSORB_BLOCK.get(),
                    new Item.Properties()
            )
    );

    //反相篮球
    public static final RegistryObject<Item> BASKETBALL_ANTI_BLOCK_ITEM = ITEMS.register("basketball_anti_block",
            () -> new BlockItem(ModBlocks.BASKETBALL_ANTI_BLOCK.get(),
                    new Item.Properties()
            )
    );
    //只因锹
    public static final RegistryObject<Item> KUN_SHOVEL= ITEMS.register("kun_shovel" ,
            ()-> new ShovelItem(
                    Tiers.IRON,
                    1,
                    -3F,
                    new Item.Properties()
                            .durability(913)
            )
    );
    //只因镐
    public static final RegistryObject<Item> KUN_PICKAXE= ITEMS.register("kun_pickaxe" ,
            ()-> new PickaxeItem(
                    Tiers.IRON,
                    1,
                    -2.8F,
                    new Item.Properties()
                            .durability(913)
            )
    );
    //终极只因镐
    public static final RegistryObject<Item> KUN_PICKAXE_FINAL= ITEMS.register("kun_pickaxe_final" ,
            ()-> new KunCourseItem.KunPickaxeFinal(
                    Tiers.DIAMOND,
                    3,
                    -2.8F,
                    new Item.Properties()
                            .durability(913 * 2)
                            .rarity(Rarity.UNCOMMON)
            )
    );
    //四倍挖掘 -> 提升4倍

    //只因斧
    public static final RegistryObject<Item> KUN_AXE= ITEMS.register("kun_axe" ,
            ()-> new AxeItem(
                    Tiers.IRON,
                    6,
                    -3.1F,
                    new Item.Properties()
                            .durability(913)
            )
    );
    //只因锄
    public static final RegistryObject<Item> KUN_HOE= ITEMS.register("kun_hoe" ,
            ()-> new HoeItem(
                    Tiers.IRON,
                    -2,
                    -1.0F,
                    new Item.Properties()
                            .durability(913)
            )
    );



    //篮球
    public static final RegistryObject<Item> KUN_BASKETBALL = ITEMS.register("kun_basketball",
            () -> new Item(
                    new Item.Properties()
                            .stacksTo(1)
                            .durability(250)
                            .rarity(Rarity.UNCOMMON)
            )
    );

    //反相篮球
    public static final RegistryObject<Item> BASKETBALL_ANTI = ITEMS.register("basketball_anti",
            () -> new BasketballAntiItem(
                    new Item.Properties()
                            .stacksTo(1)
                            .durability(250)
                            .rarity(Rarity.UNCOMMON)
            ));





    //只因剑
    public static final RegistryObject<Item> KUN_SWORD = ITEMS.register("kun_sword" ,
            ()-> new KunCourseItem.KunSwordItem(
                    Tiers.IRON,
                    3,
                    -2.4F,
                    new Item.Properties()
                            .durability(913)
            )
    );

    //只因短匕
    public static final RegistryObject<Item> KUN_DAGGER = ITEMS.register("kun_dagger" ,
            ()-> new KunCourseItem.KunDaggerItem(
                    Tiers.IRON,
                    0,
                    0F,
                    new Item.Properties()
                            .durability(913)
            )
    );
    //只因镰刀
    public static final RegistryObject<Item> KUN_SCYTHE = ITEMS.register("kun_scythe",
            () -> new ScytheCourseItem.ScytheItem(
                    ModTiers.SCYTHE, // 自定义工具等级（见步骤2）
                    2,               // 基础攻击伤害（实际伤害 = 5 + 等级加成）
                    -2.8F,           // 攻击速度（原版钻石剑为-2.4）
                    new Item.Properties().durability(913)
                            .rarity(Rarity.COMMON)
            )
    );
    //0+1+3=5
    //横扫伤害为4*3=12

    //只因锤
    public static final RegistryObject<Item> KUN_MACE = ITEMS.register("kun_mace" ,
            ()-> new AxeMaceItem(
                    Tiers.IRON,
                    11,
                    -3.4F,
                    new Item.Properties()
                            .durability(913)
            )
    );

    //只因复合镰刀
    public static final RegistryObject<Item> KUN_COMPOUND_SCYTHE = ITEMS.register("kun_compound_scythe",
            () -> new ScytheCourseItem.CompoundScytheItem(
                    ModTiers.SCYTHE,
                    4,
                    -2.4F,
                    new Item.Properties().durability(913 * 2)
                            .rarity(Rarity.UNCOMMON)
            )
    );
    //4+1+3=8
    //横扫伤害为8*6=48
    //提升4倍

    //只因阔斧锤
    public static final RegistryObject<Item> KUN_AXEMACE = ITEMS.register("kun_axemace" ,
            ()-> new AxeMaceItem(
                    Tiers.IRON,
                    53,
                    -3F,
                    new Item.Properties()
                            .durability(913 * 2)
                            .rarity(Rarity.UNCOMMON)
            )
    );
    //15*4=60 提升4倍

    // 号角
    public static final RegistryObject<Item> TENEBRIS_PLUME_CLARION = ITEMS.register("tenebris_plume_clarion",
            () -> new TenebrisPlumeClarionItem(
                    new Item.Properties()
                            .rarity(Rarity.UNCOMMON)
                            .stacksTo(1)
            ));

    // 金矽刺剑
    public static final RegistryObject<Item> AURIVERSITE_RAPIER = ITEMS.register("auriversite_rapier",
            () -> new AuriversiteRapierItem(
                    ModTiers.STAB,
                    1,
                    -1.4F,
                    new Item.Properties().durability(913)
            )
    );


    //盔甲lv1
    public static final RegistryObject<Item> TRAIN_I_HELMET = ITEMS.register("train_i_helmet",
            () -> new ArmorItem(
                    new TrainMaterial.LevelI(),
                    ArmorItem.Type.HELMET,
                    new Item.Properties()
            )
    );
    public static final RegistryObject<Item> TRAIN_I_CHESTPLATE = ITEMS.register("train_i_chestplate",
            () -> new ArmorItem(
                    new TrainMaterial.LevelI(),
                    ArmorItem.Type.CHESTPLATE,
                    new Item.Properties()
            )
    );

    public static final RegistryObject<Item> TRAIN_I_LEGGINGS = ITEMS.register("train_i_leggings",
            () -> new ArmorItem(
                    new TrainMaterial.LevelI(),
                    ArmorItem.Type.LEGGINGS,
                    new Item.Properties()
            )
    );

    public static final RegistryObject<Item> TRAIN_I_BOOTS = ITEMS.register("train_i_boots",
            () -> new ArmorItem(
                    new TrainMaterial.LevelI(),
                    ArmorItem.Type.BOOTS,
                    new Item.Properties()
            )
    );

    //盔甲lv2
    public static final RegistryObject<Item> TRAIN_II_HELMET = ITEMS.register("train_ii_helmet",
            () -> new ArmorItem(
                    new TrainMaterial.LevelII(),
                    ArmorItem.Type.HELMET,
                    new Item.Properties()
                            .rarity(Rarity.UNCOMMON)
            )
    );
    public static final RegistryObject<Item> TRAIN_II_CHESTPLATE = ITEMS.register("train_ii_chestplate",
            () -> new ArmorItem(
                    new TrainMaterial.LevelII(),
                    ArmorItem.Type.CHESTPLATE,
                    new Item.Properties()
                            .rarity(Rarity.UNCOMMON)
            )
    );

    public static final RegistryObject<Item> TRAIN_II_LEGGINGS = ITEMS.register("train_ii_leggings",
            () -> new ArmorItem(
                    new TrainMaterial.LevelII(),
                    ArmorItem.Type.LEGGINGS,
                    new Item.Properties()
                            .rarity(Rarity.UNCOMMON)
            )
    );

    public static final RegistryObject<Item> TRAIN_II_BOOTS = ITEMS.register("train_ii_boots",
            () -> new ArmorItem(
                    new TrainMaterial.LevelII(),
                    ArmorItem.Type.BOOTS,
                    new Item.Properties()
                            .rarity(Rarity.UNCOMMON)
            )
    );

    //盔甲lv3
    public static final RegistryObject<Item> TRAIN_III_HELMET = ITEMS.register("train_iii_helmet",
            () -> new ArmorItem(
                    new TrainMaterial.LevelIII(),
                    ArmorItem.Type.HELMET,
                    new Item.Properties()
                            .rarity(Rarity.RARE)
            )
    );
    public static final RegistryObject<Item> TRAIN_III_CHESTPLATE = ITEMS.register("train_iii_chestplate",
            () -> new ArmorItem(
                    new TrainMaterial.LevelIII(),
                    ArmorItem.Type.CHESTPLATE,
                    new Item.Properties()
                            .rarity(Rarity.RARE)
            )
    );

    public static final RegistryObject<Item> TRAIN_III_LEGGINGS = ITEMS.register("train_iii_leggings",
            () -> new ArmorItem(
                    new TrainMaterial.LevelIII(),
                    ArmorItem.Type.LEGGINGS,
                    new Item.Properties()
                            .rarity(Rarity.RARE)
            )
    );

    public static final RegistryObject<Item> TRAIN_III_BOOTS = ITEMS.register("train_iii_boots",
            () -> new ArmorItem(
                    new TrainMaterial.LevelIII(),
                    ArmorItem.Type.BOOTS,
                    new Item.Properties()
                            .rarity(Rarity.RARE)
            )
    );


    //“苹果”
    public static final RegistryObject<Item> REAL_APPLE = ITEMS.register("real_apple",
            () -> new Item(new Item.Properties()
                    .food(new FoodProperties.Builder()
                            .nutrition(4)
                            .saturationMod(0.3f)
                            .alwaysEat()
                            .build()
                    )
                    .rarity(Rarity.COMMON)
            ));

    //阿只因苹果
    public static final RegistryObject<Item> AHKUN_APPLE = ITEMS.register("ahkun_apple",
            () -> new EnchantedFoodItem(new Item.Properties()
                    .food(new FoodProperties.Builder()
                            .nutrition(4)                    // 回复饥饿值4
                            .saturationMod(1.2f)            // 饱和度 = 4 * 1.2 * 2 = 9.6
                            .alwaysEat()                    // 允许满饱食度时食用
                            .build()
                    )
                    .rarity(Rarity.EPIC)
            ));

    //大阿只因苹果
    public static final RegistryObject<Item> BIG_AHKUN_APPLE = ITEMS.register("big_ahkun_apple",
            () -> new EnchantedFoodItem(new Item.Properties()
                    .food(new FoodProperties.Builder()
                            .nutrition(5)                    // 回复饥饿值5
                            .saturationMod(1.9f)            // 饱和度 = 4 * 1.9 * 2 = 15.2
                            .alwaysEat()                    // 允许满饱食度时食用
                            .build()
                    )
                    .rarity(Rarity.EPIC)
            ));




    //石棍
    public static final RegistryObject<Item> STONE_STICK = ITEMS.register("stone_stick",
            () -> new StoneStickItem(new Item.Properties())
    );
    //石棍T
    public static final RegistryObject<Item> STONE_STICK_T = ITEMS.register("stone_stick_t",
            () -> new StoneStickTItem(new Item.Properties()));
    //只因粒
    public static final RegistryObject<Item> KUN_NUGGET = ITEMS.register("kun_nugget",
            () -> new KunCourseItem.KunNuggetItem(new Item.Properties())
    );
    //两锭半
    public static final RegistryObject<Item> TWO_HALF_INGOT = ITEMS.register("two_half_ingot",
            () -> new KunCourseItem.TwoHalfIngotItem(new Item.Properties()
            ));
    //黑粉
    public static final RegistryObject<Item> POWDER_ANTI = ITEMS.register("powder_anti",
            () -> new PowderAntiCourseItem.PowderAntiItem(new Item.Properties()
            ));
    //黑粉 4x
    public static final RegistryObject<Item> POWDER_ANTI_4 = ITEMS.register("powder_anti_4",
            () -> new PowderAntiCourseItem.PowderAnti4Item(new Item.Properties()
            ));
    //一小堆黑粉
    public static final RegistryObject<Item> POWDER_ANTI_9 = ITEMS.register("powder_anti_9",
            () -> new PowderAntiCourseItem.PowderAnti9Item(new Item.Properties()
            ));
    // 金矽锭
    public static final RegistryObject<Item> AURIVERSITE_INGOT = ITEMS.register("auriversite_ingot",
            () -> new Item(new Item.Properties())
    );
    // 金矽粒
    public static final RegistryObject<Item> AURIVERSITE_NUGGET = ITEMS.register("auriversite_nugget",
            () -> new Item(new Item.Properties())
    );
    // 鈅锭
    public static final RegistryObject<Item> SELENAURITE_INGOT = ITEMS.register("selenaurite_ingot",
            () -> new Item(new Item.Properties())
    );
    public static final RegistryObject<Item> NYCTIUM_INGOT = ITEMS.register("nyctium_ingot",
            () -> new Item( new Item.Properties())
    );
    public static final RegistryObject<Item> TERAPIUM_INGOT = ITEMS.register("terapium_ingot",
            () -> new Item( new Item.Properties())
    );
    public static final RegistryObject<Item> BANALIUM_INGOT = ITEMS.register("banalium_ingot",
            () -> new Item( new Item.Properties())
    );
    public static final RegistryObject<Item> NIVTIUM_INGOT = ITEMS.register("nivtium_ingot",
            () -> new Item( new Item.Properties())
    );
    public static final RegistryObject<Item> CRUCIUM_INGOT = ITEMS.register("crucium_ingot",
            () -> new Item( new Item.Properties())
    );
    public static final RegistryObject<Item> SERTIUM_INGOT = ITEMS.register("sertium_ingot",
            () -> new Item( new Item.Properties())
    );
    public static final RegistryObject<Item> PLACIUM_INGOT = ITEMS.register("placium_ingot",
            () -> new Item( new Item.Properties())
    );

    //弹性材料
    public static final RegistryObject<Item> ELASTOMERIC_MATERIAL = ITEMS.register("elastomeric_material",
            ()-> new Item(new Item.Properties())
    );
    //弹性核心
    public static final RegistryObject<Item> ELASTOMERIC_CORE = ITEMS.register("elastomeric_core",
            ()-> new Item(new Item.Properties())
    );
    //祭坛核心
    public static final RegistryObject<Item> ALTAR_CORE = ITEMS.register("altar_core",
            () -> new Item(new Item.Properties())
    );
    //金羽毛
    public static final RegistryObject<Item> GOLD_FEATHER = ITEMS.register("gold_feather",
            ()-> new Item(new Item.Properties())
    );
    //心
    public static final RegistryObject<Item> TENEBRIS_PLUME_HEART = ITEMS.register("tenebris_plume_heart",
            () -> new Item(new Item.Properties()
                    .rarity(Rarity.RARE)
            )
    );
    //兑换券
    public static final RegistryObject<Item> duihuanquan = ITEMS.register("duihuanquan",
            () -> new Duihuanquan(new Item.Properties()
                    .rarity(Rarity.RARE)
            ));
    //升级模版
    public static final RegistryObject<Item> UPGRADE_THEME = ITEMS.register("upgrade_theme",
            ()-> new Item(new Item.Properties()
            ));
    //盔甲升级模版
    public static final RegistryObject<Item> UPGRADE_THEME_ARMOR = ITEMS.register("upgrade_theme_armor",
            ()-> new Item(new Item.Properties()
            ));
    //只因蛋
    public static final RegistryObject<Item> KUN_EGG = ITEMS.register("kun_egg",
            () -> new EggCourseItem.KunEggItem(new Item.Properties()
                    .stacksTo(16)
            ));
    //黑蛋
    public static final RegistryObject<Item> BLACK_EGG = ITEMS.register("black_egg",
            () -> new EggCourseItem.BlackEggItem(new Item.Properties()
                    .stacksTo(16)
            ));
    //金蛋
    public static final RegistryObject<Item> GOLD_EGG = ITEMS.register("gold_egg",
            () -> new EggCourseItem.GoldEggItem(new Item.Properties()
                    .stacksTo(16)
            ));


    //练习鸡刷怪蛋
    public static final RegistryObject<Item> KUN_TRAINEES_SPAWN_EGG = ITEMS.register("kun_trainees_spawn_egg",
            () -> new ForgeSpawnEggItem(
                    ModEntities.KUN_TRAINEES,
                    0xFFFFFF,// 主颜色（蛋底色）
                    0xff7f27,// 副颜色（斑点色）
                    new Item.Properties()
            )
    );
    //黑粉刷怪蛋
    public static final RegistryObject<Item> KUN_ANTI_SPAWN_EGG = ITEMS.register("kun_anti_spawn_egg",
            () -> new ForgeSpawnEggItem(
                    ModEntities.KUN_ANTI,
                    0x9f9f9f,
                    0x272727,
                    new Item.Properties()
            )
    );
    //金鸡刷怪蛋
    public static final RegistryObject<Item> GOLD_CHICKEN_SPAWN_EGG = ITEMS.register("gold_chicken_spawn_egg",
            () -> new ForgeSpawnEggItem(
                    ModEntities.GOLD_CHICKEN,
                    0xFFD700,
                    0xFFDF00,
                    new Item.Properties()
                            .rarity(Rarity.UNCOMMON)
            )
    );
    //幕后黑粉刷怪蛋
    public static final RegistryObject<Item> KUN_ANTI_BOSS_SPAWN_EGG = ITEMS.register("kun_anti_boss_spawn_egg",
            () -> new ForgeSpawnEggItem(
                    ModEntities.KUN_ANTI_BOSS,
                    0x9f9f9f,
                    0xff7f27,
                    new Item.Properties()
            )
    );

}
