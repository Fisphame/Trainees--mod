package com.pha.trainees.compat.jei;

import com.pha.trainees.Main;
import com.pha.trainees.chemistry.reaction.ReactionEdge;
import com.pha.trainees.chemistry.reaction.ReactionGraph;
import com.pha.trainees.chemistry.reaction.ReactionRule;
import com.pha.trainees.chemistry.reaction.RuleFamily;
import com.pha.trainees.recipe.PackerRecipe;
import com.pha.trainees.recipe.ExtractionRecipe;
import com.pha.trainees.recipe.TrainerAltarRecipe;
import com.pha.trainees.registry.ModBlocks;
import com.pha.trainees.registry.ModChemistry;
import com.pha.trainees.registry.ModItemTags;
import com.pha.trainees.registry.ModRecipes;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IModIngredientRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"deprecation", "removal"})
@JeiPlugin
public class TraineesJeiPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_UID = new ResourceLocation(Main.MODID, "jei_plugin");

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        // 注册你的自定义配方类别
        registration.addRecipeCategories(new TrainerAltarRecipeCategory(guiHelper));
        registration.addRecipeCategories(new ExtractionRecipeCategory(guiHelper));
        registration.addRecipeCategories(new AggregationRecipeCategory(guiHelper));
        registration.addRecipeCategories(new EnrichmentRecipeCategory(guiHelper));
        // 规则三页（§19.16 百科式 + §19.36 家族分页）：同一个类 + 不同 type/标题/图标
        registration.addRecipeCategories(new ReactionRecipeCategory(guiHelper,
                ReactionRecipeCategory.TYPE, "jei.trainees.reaction", "Rx", 0xFF2E7D4F));
        registration.addRecipeCategories(new ReactionRecipeCategory(guiHelper,
                ReactionRecipeCategory.IONIZATION_TYPE, "jei.trainees.ionization", "Io", 0xFF2E5F8D));
        registration.addRecipeCategories(new ReactionRecipeCategory(guiHelper,
                ReactionRecipeCategory.PHASE_TYPE, "jei.trainees.phase", "Ph", 0xFF5B3A7A));
        // 打包机规格（§19.18.6）：展示用配方（规格 → 规格页 + 产出）
        registration.addRecipeCategories(new PackerRecipeCategory(guiHelper));
    }

    /**
     * 注册自定义条目类型（§19.16）：裸离子在 JEI 中作为可搜索、可 R/U 聚焦的条目。
     * 物质（一级真物品 / 二级 NBT 物质）仍走 ItemStack 槽位，从而 R/U 同时命中物品配方与化学方程式。
     */
    @Override
    public void registerIngredients(@NotNull IModIngredientRegistration registration) {
        registration.register(ChemicalIngredientTypes.ION,
                ModChemistry.ModIons.ALL_IONS,
                new IonIngredientHelper(),
                new IonIngredientRenderer());
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        // 需要在客户端有 level 时才能访问 RecipeManager
        if (Minecraft.getInstance().level != null) {
            RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();
            List<TrainerAltarRecipe> recipes = recipeManager.getAllRecipesFor(ModRecipes.TRAINER_ALTAR_TYPE.get())
                    .stream()
                    .toList();
            registration.addRecipes(TrainerAltarRecipeCategory.TYPE, recipes);

            // 提取机配方（§17.1，数据驱动）：页内数值按"当前档位"换算（ExtractionRecipeCategory 自带档位状态）
            List<ExtractionRecipe> extractions =
                    recipeManager.getAllRecipesFor(ModRecipes.EXTRACTION_TYPE.get()).stream().toList();
            registration.addRecipes(ExtractionRecipeCategory.TYPE, extractions);

            // 聚合机配方（§17.1，数据驱动）：RE → 物品（默认 1600 RE → 1 精炼点）
            List<com.pha.trainees.recipe.AggregationRecipe> aggregations =
                    recipeManager.getAllRecipesFor(ModRecipes.AGGREGATION_TYPE.get()).stream().toList();
            registration.addRecipes(AggregationRecipeCategory.TYPE, aggregations);

            // 富集仓配方（§17.1，数据驱动）：多原料 → 富集态；页内数值随档位换算
            List<com.pha.trainees.recipe.EnrichmentRecipe> enrichments =
                    recipeManager.getAllRecipesFor(ModRecipes.ENRICHMENT_TYPE.get()).stream().toList();
            registration.addRecipes(EnrichmentRecipeCategory.TYPE, enrichments);
        }

        // 规则三页（§19.36）：一次遍历反应图，按**家族**分桶、按 id 去重（多条边共享同一规则）。
        // 相变（quiet）不再被丢弃，而是进自己的页——quiet 只表示"不写失败诊断"，不是"不给玩家看"。
        Map<RuleFamily, Map<String, ReactionRule>> byFamily = new LinkedHashMap<>();
        for (ReactionEdge edge : ReactionGraph.getInstance().getAllEdges()) {
            ReactionRule rule = edge.getRule();
            if (rule == null || rule.getId() == null) continue;
            byFamily.computeIfAbsent(rule.getFamily(), family -> new LinkedHashMap<>())
                    .putIfAbsent(rule.getId().toString(), rule);
        }
        registration.addRecipes(ReactionRecipeCategory.TYPE,
                new ArrayList<>(rulesOf(byFamily, RuleFamily.CHEMICAL)));
        registration.addRecipes(ReactionRecipeCategory.IONIZATION_TYPE,
                new ArrayList<>(rulesOf(byFamily, RuleFamily.IONIZATION)));
        registration.addRecipes(ReactionRecipeCategory.PHASE_TYPE,
                new ArrayList<>(rulesOf(byFamily, RuleFamily.PHASE)));

        // 打包机规格（§19.18.6）：**展示用配方**直接由规格表投影而来（PackerRecipe.allFromSpecs()），
        // **不经过 RecipeManager** —— V1 没有 JSON 数据文件，getAllRecipesFor 会是空表。
        // 机器运行时仍走 PackerService 的规格判定，本页只负责"看得懂"（§19.25 展示用配方 ≠ 机器驱动配方）。
        registration.addRecipes(PackerRecipeCategory.TYPE, PackerRecipe.allFromSpecs());

        // 日化品图鉴（§19.37 定稿：**合并成一页**）：按 trainees:bleach 标签挂页——
        // 合格品、优质品与四类危险次品共用同一份信息页（含"次品为何危险"一段），
        // 以后加新规格只要进标签就自动带上页面。
        // 不再有"优质品专用页 / 次品专用页"（原 jei.trainees.bleach.premium_info 与
        // jei.trainees.defective.info 两个键已删）。
        // （具体文案由作者统一重写，见 §19.37 待办。）
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(ModItemTags.BLEACH)) {
            registration.addIngredientInfo(new ItemStack(holder.value()),
                    VanillaTypes.ITEM_STACK, Component.translatable("jei.trainees.bleach.info"));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ALTAR_CORE_BLOCK.get()), TrainerAltarRecipeCategory.TYPE);
        // 提取机：5 个档位方块都是催化剂（看配方时点任意一档都能进来，页内再切档）
        for (var extractor : List.of(ModBlocks.EXTRACTOR_PRIMARY.get(), ModBlocks.EXTRACTOR_ADVANCED.get(),
                ModBlocks.EXTRACTOR_HIGH.get(), ModBlocks.EXTRACTOR_ULTIMATE.get(), ModBlocks.EXTRACTOR_CREATIVE.get())) {
            registration.addRecipeCatalyst(new ItemStack(extractor), ExtractionRecipeCategory.TYPE);
        }
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.REFINED_AGGREGATOR.get()),
                AggregationRecipeCategory.TYPE);
        for (var chamber : List.of(ModBlocks.ENRICHMENT_PRIMARY.get(), ModBlocks.ENRICHMENT_ADVANCED.get(),
                ModBlocks.ENRICHMENT_HIGH.get(), ModBlocks.ENRICHMENT_ULTIMATE.get(),
                ModBlocks.ENRICHMENT_CREATIVE.get())) {
            registration.addRecipeCatalyst(new ItemStack(chamber), EnrichmentRecipeCategory.TYPE);
        }

        // 催化剂口径（§19.36 作者定）：列**设计用途**上的入口，而不是穷举"谁跑得动"。
        // 背景：电解槽继承 BeakerBlockEntity、调的是同一个 ReactionEngine.tick，所以它事实上
        // 相变/电离/普通反应都能跑；但 JEI 催化剂是"推荐你从这儿做"的指引，
        // "我这台机器现在能不能跑"由分析仪回答（§19.14）。
        // 顺序即推荐度：烧杯永远第一（新手第一个容器）。

        // ① 化学反应（含电解）
        registration.addRecipeCatalyst(new ItemStack(ModChemistry.ModChemistryBlockItems.BEAKER.get()),
                ReactionRecipeCategory.TYPE);
        addCatalystsFromTag(registration, ReactionRecipeCategory.TYPE,
                ModChemistry.ModChemistryBlockItems.BEAKER.get(), "beaker_like");
        registration.addRecipeCatalyst(new ItemStack(ModChemistry.ModChemistryBlockItems.ELECTROLYSIS_CELL.get()),
                ReactionRecipeCategory.TYPE);

        // ② 电离：水系反应，烧杯 + 反应釜（坩埚是高温熔体用的，不列）
        registration.addRecipeCatalyst(new ItemStack(ModChemistry.ModChemistryBlockItems.BEAKER.get()),
                ReactionRecipeCategory.IONIZATION_TYPE);
        addCatalystsFromTag(registration, ReactionRecipeCategory.IONIZATION_TYPE,
                ModChemistry.ModChemistryBlockItems.BEAKER.get(), "reactor");

        // ③ 相变：任何"能装料 + 能控温"的容器（烧杯 / 坩埚×3 / 反应釜×5），烧杯排第一
        addCatalystsFromTag(registration, ReactionRecipeCategory.PHASE_TYPE, null, "beaker_like");

        // 打包机（§19.18.6）：按 `trainees:packer` 标签取——以后加机型/加等级不用回来改这里
        addCatalystsFromTag(registration, PackerRecipeCategory.TYPE, null, "packer");
    }

    /** 取某家族的规则集合（缺桶时返回空表，避免 JEI 收到 null） */
    private static Collection<ReactionRule> rulesOf(Map<RuleFamily, Map<String, ReactionRule>> byFamily,
                                                    RuleFamily family) {
        Map<String, ReactionRule> bucket = byFamily.get(family);
        return bucket == null ? List.of() : bucket.values();
    }

    /**
     * 按 {@code trainees:<path>} 物品标签批量注册催化剂（§19.34）。
     *
     * <p>分类真源在数据生成器里（{@code ModBlockTagsProvider/ModItemTagsProvider}），
     * JEI 只读标签——所以"哪些方块能做这个反应"永远只有一处定义。</p>
     *
     * @param already 已经单独登记过的物品（跳过，避免同一个催化剂出现两次）
     */
    private static void addCatalystsFromTag(IRecipeCatalystRegistration registration, RecipeType<?> type,
                                            @Nullable Item already, String path) {
        TagKey<Item> tag = TagKey.create(Registries.ITEM, new ResourceLocation(Main.MODID, path));
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
            Item item = holder.value();
            if (item == already) continue;
            registration.addRecipeCatalyst(new ItemStack(item), type);
        }
    }
}
