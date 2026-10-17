package com.pha.trainees.recipe;

import com.pha.trainees.chemistry.product.ProductSpecs;
import com.pha.trainees.chemistry.spec.FractionRule;
import com.pha.trainees.chemistry.spec.ProductSpec;
import com.pha.trainees.registry.ModRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 打包机的**展示用配方**（§19.18.6；决策依据 §19.25「展示用配方 ≠ 机器驱动配方」）。
 *
 * <p><b>它不参与任何判定</b>：机器运行时仍然走 {@code PackerService} + {@code SpecMatcher} 做规格判定与扣料，
 * 本类只是把 {@link ProductSpec} **投影**成 JEI 能画的形状。因此：</p>
 * <ul>
 *   <li>{@link #matches} 恒 {@code false}（永不用于合成）；</li>
 *   <li>{@link #isSpecial()} 为 {@code true}（避免被塞进配方书/自动合成）；</li>
 *   <li>{@link #assemble} 只作占位，返回空。</li>
 * </ul>
 *
 * <p><b>为什么输入画成"规格"而不是 item 槽</b>：打包机的原料是**容器里的一份分布**
 * （`(物种→mol)` + 温度），不是物品，无法用 item 槽表达；能表达的是"这份溶液要满足什么标准"。</p>
 *
 * <p><b>单一真源</b>：本类由 {@link ProductSpecs#ALL} 转换而来（{@link #allFromSpecs()}），
 * 不另存一份规格数据。V1 **不提供 JSON 数据文件**、因此也**不经过 RecipeManager**
 * （没有 JSON 时 {@code getAllRecipesFor} 会是空表）——JEI 直接取 {@link #allFromSpecs()} 的结果。
 * 将来走 §19.19 的数据驱动时，再让数据表产出 JSON / 由 RecipeManager 供数据。</p>
 */
@SuppressWarnings({"deprecation", "removal"})
public class PackerRecipe implements Recipe<Container> {

    /** 一条规格条目（有效成分窗口 / 限值 / 优质档），是 {@link FractionRule} 的展示投影。 */
    public record SpecRule(String speciesId, double minFraction, double maxFraction) {
        public static SpecRule of(FractionRule rule) {
            return new SpecRule(rule.ionId(), rule.minFraction(), rule.maxFraction());
        }

        /** 是否属于"禁用"语义（上限 ≤ 0）：JEI 里显示为"禁用"而不是"≤ 0%"。 */
        public boolean isForbidden() {
            return maxFraction <= 0.0;
        }
    }

    private final ResourceLocation id;
    private final String productId;
    private final List<SpecRule> required;
    private final List<SpecRule> limits;
    private final List<SpecRule> premium;
    private final double bottleMoles;
    private final String goodItemId;
    private final String premiumItemId;
    private final String behaviorKey;

    public PackerRecipe(ResourceLocation id, String productId,
                        List<SpecRule> required, List<SpecRule> limits, List<SpecRule> premium,
                        double bottleMoles, String goodItemId, String premiumItemId, String behaviorKey) {
        this.id = id;
        this.productId = productId;
        this.required = List.copyOf(required);
        this.limits = List.copyOf(limits);
        this.premium = List.copyOf(premium);
        this.bottleMoles = bottleMoles;
        this.goodItemId = goodItemId;
        this.premiumItemId = premiumItemId;
        this.behaviorKey = behaviorKey;
    }

    // ==================== 规格 → 展示配方 ====================

    /** 把规格表全部产品转成展示配方（JEI 与将来的数据生成都从这里取）。 */
    public static List<PackerRecipe> allFromSpecs() {
        List<PackerRecipe> list = new ArrayList<>();
        for (ProductSpec spec : ProductSpecs.ALL) {
            list.add(fromSpec(spec));
        }
        return list;
    }

    /** 单个规格的投影。 */
    public static PackerRecipe fromSpec(ProductSpec spec) {
        List<SpecRule> required = new ArrayList<>();
        for (FractionRule rule : spec.required()) required.add(SpecRule.of(rule));
        List<SpecRule> limits = new ArrayList<>();
        for (FractionRule rule : spec.limits()) limits.add(SpecRule.of(rule));
        List<SpecRule> premium = new ArrayList<>();
        for (FractionRule rule : spec.premium()) premium.add(SpecRule.of(rule));

        return new PackerRecipe(new ResourceLocation(spec.id()),
                spec.id(),
                required, limits, premium,
                spec.bottleMoles(),
                spec.goodItem(), spec.premiumItem(), spec.behaviorKey());
    }

    // ==================== 展示字段 ====================

    public String getProductId() { return productId; }
    public List<SpecRule> getRequired() { return required; }
    public List<SpecRule> getLimits() { return limits; }
    public List<SpecRule> getPremium() { return premium; }
    public double getBottleMoles() { return bottleMoles; }
    public String getGoodItemId() { return goodItemId; }
    public String getPremiumItemId() { return premiumItemId; }
    public String getBehaviorKey() { return behaviorKey; }

    // ==================== Recipe 接口（全部为"展示用"占位） ====================

    @Override
    public boolean matches(@NotNull Container container, @NotNull Level level) {
        return false;   // 展示用：机器运行时不走配方系统
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull Container container, @NotNull RegistryAccess access) {
        return ItemStack.EMPTY;   // 占位：产出由打包机按规格判定后给出
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;   // 无法在工作台合成（它是机器）
    }

    @Override
    public @NotNull ItemStack getResultItem(@NotNull RegistryAccess access) {
        return ItemStack.EMPTY;   // 产出在 JEI 页面里由槽位自行添加
    }

    /** 特殊配方：不参与配方书/自动合成的自动匹配（§19.25）。 */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        return NonNullList.create();   // 原料是"容器里的分布"，没有 item 原料
    }

    @Override
    public @NotNull ResourceLocation getId() {
        return id;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipes.PACKER_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return ModRecipes.PACKER_TYPE.get();
    }

    // ==================== 序列化 ====================

    /**
     * V1 **没有 JSON 数据文件**，因此 {@code fromJson} 不会在正常流程里被调用；
     * 这里做一个"按 id 找规格"的保守实现，将来数据表产出 JSON 时可直接用。
     */
    public static class Serializer implements RecipeSerializer<PackerRecipe> {

        @Override
        public @NotNull PackerRecipe fromJson(@NotNull ResourceLocation recipeId,
                                              @NotNull com.google.gson.JsonObject json) {
            for (PackerRecipe candidate : allFromSpecs()) {
                if (candidate.getId().equals(recipeId)) return candidate;
            }
            // 找不到对应规格：给一个空壳（不静默伪造规格），JEI 上会显示为空规格页
            return new PackerRecipe(recipeId, recipeId.toString(),
                    List.of(), List.of(), List.of(), 1.0, "", "", "");
        }

        @Override
        public PackerRecipe fromNetwork(@NotNull ResourceLocation recipeId, @NotNull FriendlyByteBuf buffer) {
            String productId = buffer.readUtf();
            List<SpecRule> required = readRules(buffer);
            List<SpecRule> limits = readRules(buffer);
            List<SpecRule> premium = readRules(buffer);
            double bottleMoles = buffer.readDouble();
            String goodItemId = buffer.readUtf();
            String premiumItemId = buffer.readUtf();
            String behaviorKey = buffer.readUtf();
            return new PackerRecipe(recipeId, productId, required, limits, premium,
                    bottleMoles, goodItemId, premiumItemId, behaviorKey);
        }

        @Override
        public void toNetwork(@NotNull FriendlyByteBuf buffer, @NotNull PackerRecipe recipe) {
            buffer.writeUtf(recipe.productId);
            writeRules(buffer, recipe.required);
            writeRules(buffer, recipe.limits);
            writeRules(buffer, recipe.premium);
            buffer.writeDouble(recipe.bottleMoles);
            buffer.writeUtf(recipe.goodItemId);
            buffer.writeUtf(recipe.premiumItemId);
            buffer.writeUtf(recipe.behaviorKey);
        }

        private static List<SpecRule> readRules(FriendlyByteBuf buffer) {
            int size = buffer.readVarInt();
            List<SpecRule> rules = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                rules.add(new SpecRule(buffer.readUtf(), buffer.readDouble(), buffer.readDouble()));
            }
            return rules;
        }

        private static void writeRules(FriendlyByteBuf buffer, List<SpecRule> rules) {
            buffer.writeVarInt(rules.size());
            for (SpecRule rule : rules) {
                buffer.writeUtf(rule.speciesId());
                buffer.writeDouble(rule.minFraction());
                buffer.writeDouble(rule.maxFraction());
            }
        }
    }
}
