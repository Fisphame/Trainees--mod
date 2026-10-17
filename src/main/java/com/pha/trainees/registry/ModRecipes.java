package com.pha.trainees.registry;

import com.pha.trainees.Main;
import com.pha.trainees.recipe.AggregationRecipe;
import com.pha.trainees.recipe.EnrichmentRecipe;
import com.pha.trainees.recipe.ExtractionRecipe;
import com.pha.trainees.recipe.PackerRecipe;
import com.pha.trainees.recipe.TrainerAltarRecipe;
import com.pha.trainees.util.game.chemistry.CatalystCondition;
import com.pha.trainees.util.game.chemistry.MachineChemicalEquation;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"deprecation", "removal"})
public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Main.MODID);
    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, Main.MODID);

    public static final RegistryObject<RecipeSerializer<TrainerAltarRecipe>> TRAINER_ALTAR_SERIALIZER =
            SERIALIZERS.register("trainer_altar", TrainerAltarRecipe.Serializer::new);

    public static final RegistryObject<RecipeType<TrainerAltarRecipe>> TRAINER_ALTAR_TYPE =
            TYPES.register("trainer_altar", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return new ResourceLocation(Main.MODID, "trainer_altar").toString();
                }
            });

    // ====== 提取机（§17.1）：**数据驱动**（datapack JSON），配方数据是初级(0档)基准值 ======
    public static final RegistryObject<RecipeSerializer<ExtractionRecipe>> EXTRACTION_SERIALIZER =
            SERIALIZERS.register("extraction", ExtractionRecipe.Serializer::new);

    public static final RegistryObject<RecipeType<ExtractionRecipe>> EXTRACTION_TYPE =
            TYPES.register("extraction", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return new ResourceLocation(Main.MODID, "extraction").toString();
                }
            });

    // ====== 聚合机（§17.1）：数据驱动，RE → 物品（默认 1600 RE → 1 精炼点）======
    public static final RegistryObject<RecipeSerializer<AggregationRecipe>> AGGREGATION_SERIALIZER =
            SERIALIZERS.register("aggregation", AggregationRecipe.Serializer::new);

    public static final RegistryObject<RecipeType<AggregationRecipe>> AGGREGATION_TYPE =
            TYPES.register("aggregation", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return new ResourceLocation(Main.MODID, "aggregation").toString();
                }
            });

    // ====== 富集仓（§17.1）：数据驱动，2 item + 2 流体输入（含催化剂）+ 档位函数（能量/时间都吃多项式）======
    public static final RegistryObject<RecipeSerializer<EnrichmentRecipe>> ENRICHMENT_SERIALIZER =
            SERIALIZERS.register("enrichment", EnrichmentRecipe.Serializer::new);

    public static final RegistryObject<RecipeType<EnrichmentRecipe>> ENRICHMENT_TYPE =
            TYPES.register("enrichment", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return new ResourceLocation(Main.MODID, "enrichment").toString();
                }
            });

    // ====== 打包机（§19.18.6）：**展示用配方**，不参与判定/扣料（运行时仍走 PackerService 规格判定）======
    // V1 没有 JSON 数据文件，因此不经过 RecipeManager；JEI 直接取 PackerRecipe.allFromSpecs()。
    public static final RegistryObject<RecipeSerializer<PackerRecipe>> PACKER_SERIALIZER =
            SERIALIZERS.register("packer", PackerRecipe.Serializer::new);

    public static final RegistryObject<RecipeType<PackerRecipe>> PACKER_TYPE =
            TYPES.register("packer", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return new ResourceLocation(Main.MODID, "packer").toString();
                }
            });

//    public static final Lazy<List<TrainerAltarRecipe>> TRAINER_ALTAR_RECIPES = Lazy.of(() -> {
//        List<TrainerAltarRecipe> list = new ArrayList<>();
//        // 从旧的 ItemPair4 转换，或直接添加新配方
//        // 示例：四个石头 -> 钻石
//        list.add(createRecipe("stone_to_diamond",
//                Ingredient.of(Items.STONE), Ingredient.of(Items.STONE),
//                Ingredient.of(Items.STONE), Ingredient.of(Items.STONE),
//                new ItemStack(Items.DIAMOND)));
//
//        // 四个原木 -> 绿宝石
//        list.add(createRecipe("log_to_emerald",
//                Ingredient.of(Items.OAK_LOG), Ingredient.of(Items.OAK_LOG),
//                Ingredient.of(Items.OAK_LOG), Ingredient.of(Items.OAK_LOG),
//                new ItemStack(Items.EMERALD)));
//
//        // 四个二分之一锭 -> 四根石棒
//        list.add(createRecipe("ingot_to_stick",
//                Ingredient.of(ModItems.TWO_HALF_INGOT.get()), Ingredient.of(ModItems.TWO_HALF_INGOT.get()),
//                Ingredient.of(ModItems.TWO_HALF_INGOT.get()), Ingredient.of(ModItems.TWO_HALF_INGOT.get()),
//                new ItemStack(ModItems.STONE_STICK.get(), 4)));
//
//        return list;
//    });
//
//    private static TrainerAltarRecipe createRecipe(String name, Ingredient i1, Ingredient i2,
//                                                   Ingredient i3, Ingredient i4, ItemStack result) {
//        return new TrainerAltarRecipe(
//                new ResourceLocation(Main.MODID, name),
//                i1, i2, i3, i4, result
//        );
//    }


//    public static final Lazy<List<ItemPair4>> TRAINER_ALTAR_RECIPES = Lazy.of(() -> {
//        List<ItemPair4> list = new ArrayList<>();
//        // 仅祭坛合成
//        addRecipe(list, UPGRADE_THEME.get(), Items.DIAMOND, Items.DIAMOND, TWO_HALF_INGOT.get(), UPGRADE_THEME.get(), 2);
//        addRecipe(list, KUN_MACE.get(), KUN_AXE.get(), UPGRADE_THEME.get(), TWO_HALF_INGOT.get(), KUN_AXEMACE.get(), 1);
//        addRecipe(list, KUN_SCYTHE.get(), KUN_SWORD.get(), KUN_DAGGER.get(), UPGRADE_THEME.get(), KUN_COMPOUND_SCYTHE.get(), 1);
//        addRecipe(list, KUN_PICKAXE.get(), UPGRADE_THEME.get(), TWO_HALF_INGOT.get(), TWO_HALF_INGOT.get(), KUN_PICKAXE_FINAL.get(), 1);
//        addRecipe(list, TWO_HALF_INGOT.get(), Items.EGG, Items.EGG, ELASTOMERIC_CORE.get(), KUN_BASKETBALL.get(), 1);
//        addRecipe(list,TRAIN_II_HELMET.get(), UPGRADE_THEME_ARMOR.get(), TWO_HALF_INGOT.get(), TWO_HALF_INGOT.get(),
//                TRAIN_III_HELMET.get(), 1);
//        addRecipe(list,TRAIN_II_CHESTPLATE.get(), UPGRADE_THEME_ARMOR.get(), TWO_HALF_INGOT.get(), TWO_HALF_INGOT.get(),
//                TRAIN_III_CHESTPLATE.get(), 1);
//        addRecipe(list,TRAIN_II_LEGGINGS.get(), UPGRADE_THEME_ARMOR.get(), TWO_HALF_INGOT.get(), TWO_HALF_INGOT.get(),
//                TRAIN_III_LEGGINGS.get(), 1);
//        addRecipe(list, TRAIN_II_BOOTS.get(), UPGRADE_THEME_ARMOR.get(), TWO_HALF_INGOT.get(), TWO_HALF_INGOT.get(),
//                TRAIN_III_BOOTS.get(), 1);
//        addRecipe(list, Items.EGG, Items.GOLD_BLOCK, Items.GOLD_BLOCK, Items.GOLD_BLOCK, GOLD_EGG.get(), 1);
//
//        // 比直接合成便宜
//        addRecipe(list, KUN_BASKETBALL.get(), CHE_JIBP_PIECE.get(), CHE_JIBP_PIECE.get(), CHE_JIBP_PIECE.get(), BASKETBALL_ANTI.get(), 1);
//        addRecipe(list, Items.GOLDEN_APPLE, Items.GOLD_INGOT, Items.GOLD_INGOT, Items.GOLD_INGOT, Items.ENCHANTED_GOLDEN_APPLE, 1);
//        addRecipe(list, Items.ENCHANTED_GOLDEN_APPLE, Items.GOLD_INGOT, Items.FEATHER, TWO_HALF_INGOT.get(), AHKUN_APPLE.get(), 1);
//        addRecipe(list, AHKUN_APPLE.get(), Items.GOLD_INGOT, Items.FEATHER, TWO_HALF_INGOT.get(), BIG_AHKUN_APPLE.get(), 1);
//        addRecipe(list, UPGRADE_THEME.get(), CHE_JIBP_PIECE.get(), CHE_JIBP_PIECE.get(), CHE_JIBP_PIECE.get(),
//                UPGRADE_THEME_ARMOR.get(), 3);
//
//        // 与直接合成无异
//        addRecipe(list, POWDER_ANTI.get(), POWDER_ANTI.get(), POWDER_ANTI.get(), POWDER_ANTI.get(), POWDER_ANTI_4.get(), 1);
//
//        return list;
//    });
//    private static void addRecipe(List<ItemPair4> list, Item a, Item b, Item c, Item d, ItemStack re) {
//        list.add(new ItemPair4(a, b, c, d, re));
//    }
//    private static void addRecipe(List<ItemPair4> list, Item a, Item b, Item c, Item d, Item re, int count) {
//        addRecipe(list, a, b, c, d, new ItemStack(re, count));
//    }


    public static class ModMachineRecipes {
        private static final List<MachineChemicalEquation> RECIPES = new ArrayList<>();

        static {
            // 示例配方：Ji + Bp -> JiBp
            MachineChemicalEquation jiBp = new MachineChemicalEquation.Builder("ji_bp_reaction")
                    .withName("黑化鸡合成")
                    .addReactant(1, ModItems.TWO_HALF_INGOT.get())
                    .addReactant(1, ModItems.POWDER_ANTI.get())
                    .addProduct(1, ModChemistry.ModChemistryItems.CHE_JIBP_PIECE.get())
                    .withDuration(40) // 2秒
                    .addTag("machine")
                    .build();
            RECIPES.add(jiBp);

            // 示例带催化剂的配方
            MachineChemicalEquation jiWater = new MachineChemicalEquation.Builder("ji_water_reaction")
                    .withName("鸡与水反应")
                    .addReactant(2, ModItems.TWO_HALF_INGOT.get())
                    .addProduct(2, ModChemistry.ModChemistryItems.CHE_JIOH_INGOT.get())
//                    .addCondition(new CatalystCondition(List.of(new ItemStack(ModItems.CATALYST.get())), 0.5)) // 50%概率消耗
                    .withDuration(100) // 5秒
                    .addTag("machine")
                    .build();
            RECIPES.add(jiWater);
        }

        public static List<MachineChemicalEquation> getAllRecipes() {
            return RECIPES;
        }

    }
}
