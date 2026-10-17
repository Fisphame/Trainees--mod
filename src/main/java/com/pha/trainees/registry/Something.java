package com.pha.trainees.registry;

import com.pha.trainees.Main;
import com.pha.trainees.block.PowderAnti99Block;
import com.pha.trainees.item.*;
import com.pha.trainees.util.math.MathT;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings({"deprecation", "removal"})
public class Something {
    public static class PrankBlocks {
        public static final DeferredRegister<Block> BLOCKS =
                DeferredRegister.create(ForgeRegistries.BLOCKS, Main.MODID);
        //神人（真的是人）方块

        //顺序：建筑方块 -> 染色方块 -> 自然方块 -> 功能方块 -> 红石方块

        public static final RegistryObject<Block> PEI_FANG_BLOCK = BLOCKS.register("pei_fang_block", () -> new Block(
                BlockBehaviour.Properties.of()
                        .strength(1.0f,1000.0f)
                        .sound(SoundType.SAND)
                        .requiresCorrectToolForDrops()
                        .lightLevel(state -> 15)
                        .emissiveRendering((state,world,pos) -> true)
                        .jumpFactor(5.0f)
        ));

        public static final RegistryObject<Block> POWDER_ANTI_99_BLOCK = BLOCKS.register("powder_anti_99",() ->
                new PowderAnti99Block(
                        -16777216,
                        BlockBehaviour.Properties.of()
                                .strength(0.5f, 250f)
                                .sound(SoundType.SAND)
                ));

    }

    public static class PrankItems {
        //模组中专门整蛊的部分……
        public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Main.MODID);
        //顺序：方块（block item） -> 工具与实用物品 -> 战斗用品 -> 食物与饮品 -> 原材料 -> 刷怪蛋
        //"钻石镐" 24*24
        public static final RegistryObject<Item> REAL_DIAMOND_PICKAXE = ITEMS.register("real_diamond_pickaxe",
                () -> new LongCourseItem.RealPickaxeItem(
                        Tiers.DIAMOND,
                        1,
                        -2.8F,
                        new Item.Properties()
                                .durability(1561)
                                .rarity(Rarity.UNCOMMON)
                ));

        //"钻 石 镐" 32*32
        public static final RegistryObject<Item> LONG_REAL_DIAMOND_PICKAXE = ITEMS.register("long_real_diamond_pickaxe",
                () -> new LongCourseItem.RealPickaxeItem(
                        Tiers.DIAMOND,
                        2,
                        -2.9F,
                        new Item.Properties()
                                .durability(1561)
                                .rarity(Rarity.UNCOMMON)
                ));

        //"钻  石  镐" 40*40
        public static final RegistryObject<Item> LONGER_REAL_DIAMOND_PICKAXE = ITEMS.register("longer_real_diamond_pickaxe",
                () -> new LongCourseItem.RealPickaxeItem(
                        Tiers.DIAMOND,
                        2,
                        -2.9F,
                        new Item.Properties()
                                .durability(1561)
                                .rarity(Rarity.UNCOMMON)
                ));

        //"钻   石   镐" 48*48
        public static final RegistryObject<Item> LONGEST_REAL_DIAMOND_PICKAXE = ITEMS.register("longest_real_diamond_pickaxe",
                () -> new LongCourseItem.RealPickaxeItem(
                        Tiers.DIAMOND,
                        3,
                        -3.0F,
                        new Item.Properties()
                                .durability(1561)
                                .rarity(Rarity.UNCOMMON)
                ));

        //"钻    石    镐" 56*56
        public static final RegistryObject<Item> THE_LONGEST_REAL_DIAMOND_PICKAXE = ITEMS.register(
                "the_longest_real_diamond_pickaxe",
                () -> new LongCourseItem.RealPickaxeItem(
                        Tiers.DIAMOND,
                        3,
                        -3.0F,
                        new Item.Properties()
                                .durability(1561)
                                .rarity(Rarity.UNCOMMON)
                ));

        //"钻     石     镐" 64*64
        public static final RegistryObject<Item> THE_THE_LONGEST_REAL_DIAMOND_PICKAXE = ITEMS.register(
                "the_the_longest_real_diamond_pickaxe",
                () -> new LongCourseItem.RealPickaxeItem(
                        Tiers.DIAMOND,
                        4,
                        -3.0F,
                        new Item.Properties()
                                .durability(1561)
                                .rarity(Rarity.UNCOMMON)
                ));

        //"钻石镐".zip
        public static final RegistryObject<Item> REAL_DIAMOND_PICKAXE_ZIP = ITEMS.register("real_diamond_pickaxe_zip",
                () -> new PickaxeItem(
                        Tiers.DIAMOND,
                        20,
                        -1.5F,
                        new Item.Properties()
                                .durability(114514)
                                .rarity(Rarity.EPIC)
                ));

        //黑粉镰刀
        public static final RegistryObject<Item> POWDER_ANTI_99_SCYTHE = ITEMS.register("powder_anti_99_scythe",
                () -> new ScytheCourseItem.BlackPowderScytheItem(
                        Tiers.GOLD,
                        499,
                        -2.4F,
                        new Item.Properties()
                                .durability((int) MathT.MATH99)
                                .rarity(Rarity.RARE)
                ));

        //长木棍
        public static final RegistryObject<Item> LONG_STICK = ITEMS.register("long_stick",
                ()-> new LongCourseItem.LongItem(new Item.Properties())
        );
        //长长木棍
        public static final RegistryObject<Item> LONGER_STICK = ITEMS.register("longer_stick",
                ()-> new LongCourseItem.LongItem(new Item.Properties())
        );
        //长长长木棍
        public static final RegistryObject<Item> LONGEST_STICK = ITEMS.register("longest_stick",
                ()-> new LongCourseItem.LongItem(new Item.Properties())
        );
        //左半镐
        public static final RegistryObject<Item> LEFT_HALF_DIAMOND_PICKAXE = ITEMS.register("left_half_diamond_pickaxe",
                ()-> new PickaxeItem(
                        Tiers.DIAMOND,
                        0,
                        -3.0F,
                        new Item.Properties()
                                .durability(2)
                )
        );
        //右半镐
        public static final RegistryObject<Item> RIGHT_HALF_DIAMOND_PICKAXE = ITEMS.register("right_half_diamond_pickaxe",
                ()-> new PickaxeItem(
                        Tiers.DIAMOND,
                        0,
                        -3.0F,
                        new Item.Properties()
                                .durability(2)
                )
        );

        //一小堆黑粉 powder_anti_9
        //一堆黑粉
        public static final RegistryObject<Item> POWDER_ANTI_92 = ITEMS.register("powder_anti_92",
                () -> new PowderAntiCourseItem.PowderAnti92Item(new Item.Properties()));
        //一大堆黑粉
        public static final RegistryObject<Item> POWDER_ANTI_93 = ITEMS.register("powder_anti_93",
                () -> new PowderAntiCourseItem.PowderAnti93Item(new Item.Properties()));
        //一小坨黑粉
        public static final RegistryObject<Item> POWDER_ANTI_94 = ITEMS.register("powder_anti_94",
                () -> new PowderAntiCourseItem.PowderAnti94Item(new Item.Properties()));
        //一坨黑粉
        public static final RegistryObject<Item> POWDER_ANTI_95 = ITEMS.register("powder_anti_95",
                () -> new PowderAntiCourseItem.PowderAnti95Item(new Item.Properties()));
        //一大坨黑粉
        public static final RegistryObject<Item> POWDER_ANTI_96 = ITEMS.register("powder_anti_96",
                () -> new PowderAntiCourseItem.PowderAnti96Item(new Item.Properties()));
        //一小块黑粉
        public static final RegistryObject<Item> POWDER_ANTI_97 = ITEMS.register("powder_anti_97",
                () -> new PowderAntiCourseItem.PowderAnti97Item(new Item.Properties()));
        //一块黑粉
        public static final RegistryObject<Item> POWDER_ANTI_98 = ITEMS.register("powder_anti_98",
                () -> new PowderAntiCourseItem.PowderAnti98Item(new Item.Properties()));
        //黑粉块
        public static final RegistryObject<Item> POWDER_ANTI_99_BLOCK_ITEM = ITEMS.register("powder_anti_99",
                () -> new BlockItem(PrankBlocks.POWDER_ANTI_99_BLOCK.get(),
                        new Item.Properties()));


        //配方块
        public static final RegistryObject<Item> PEI_FANG_BLOCK_ITEM = ITEMS.register("pei_fang_block",
                () -> new BlockItem(PrankBlocks.PEI_FANG_BLOCK.get(),
                        new Item.Properties()
                                .rarity(Rarity.UNCOMMON)
                ));

        //配方球
        public static final RegistryObject<Item> PEI_FANG_BALL = ITEMS.register("pei_fang_ball",
                () -> new SnowballItem(
                        new Item.Properties()
                                .rarity(Rarity.UNCOMMON)
                ));
        //配方
        public static final RegistryObject<Item> PEI_FANG = ITEMS.register("pei_fang",
                () -> new SwordItem(
                        Tiers.NETHERITE,
                        0,
                        1003,
                        new Item.Properties()
                                .food(new FoodProperties.Builder()
                                        .nutrition(1)
                                        .saturationMod(1f)
                                        .alwaysEat()
                                        .build()
                                )
                                .durability(25000)
                                .rarity(Rarity.UNCOMMON)
                                .fireResistant()
                                .setNoRepair()
                )
        );

        //注册了一个什么物品啊？怎么只有空格啊？？？而且怎么还不报错？
        public static final RegistryObject<Item> GunMu = ITEMS.register("gun_mu",
                ()-> new Item(new Item.Properties()));
    }



    public static class Paintings {
        public static final DeferredRegister<PaintingVariant> PAINTING_VARIANTS =
                DeferredRegister.create(ForgeRegistries.PAINTING_VARIANTS, Main.MODID);
        private static RegistryObject<PaintingVariant> rpv(String name, int width, int height) {
            return PAINTING_VARIANTS.register(name, () -> new PaintingVariant(width, height));
        }

        //画作 - 参数：(宽度, 高度) 单位：像素

        // 慢门配方系列 (Spf, Slow shutter speed PeiFang series)
        // 1 - 月牙天冲  -  塑料瓶匿迹于残影，也乱他脸于不可见
        public static final RegistryObject<PaintingVariant> SPF1 = rpv("spf1", 32, 32);

        // 2 - 旋耳炫光  -  俯仰绕耳，光冲脸过，独留他耳旋
        public static final RegistryObject<PaintingVariant> SPF2 = rpv("spf2", 32, 32);

        // 3 - 光抚视慈  -  白光抚摸着他视线慈和的脸，勾出笑来
        public static final RegistryObject<PaintingVariant> SPF3 = rpv("spf3", 32, 32);

        // 4 - 平观光洒  -  他捧着惨白与狂烈，任光洒在前胸与脸颊
        public static final RegistryObject<PaintingVariant> SPF4 = rpv("spf4", 32, 16);

        // 图片系列(p, Painting series)
        // 1 - 好臭啊（悲）
        public static final RegistryObject<PaintingVariant> P1 = rpv("p1", 32, 32);
    }
}