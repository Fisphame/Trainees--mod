package com.pha.trainees.recipe;

import com.pha.trainees.mana.TierMath;
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
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * "提取"配方（§17.1）：**数据驱动**（datapack JSON），配方数据一律是**初级(0 档)基准值**。
 *
 * <p>JSON 形状：</p>
 * <pre>{@code
 * {
 *   "type": "trainees:extraction",
 *   "input":       { "item": "minecraft:gold_ingot" },   // 支持 Ingredient（item / tag / 数组）
 *   "energy":      120,        // 初级输出能量（RE）
 *   "time":        40,         // 初级耗时（tick）
 *   "cost":        0,          // 初级"处理所需能量"（RE，一次性付）
 *   "byproducts": [            // 最多 2 个；chance 允许为负（初级倒扣，靠升档补回来）
 *     { "item": "minecraft:iron_nugget", "chance": -20 },
 *     { "item": "minecraft:gold_nugget", "chance": 0 }
 *   ],
 *   "minTier": 0, "maxTier": 4  // 可选：某些配方低于某档做不了 / 到某档封顶
 * }
 * }</pre>
 *
 * <p>档位换算全部走 {@link TierMath}（能量 ×2^x、概率 +5x²+5x、时间与能耗 ×(5x²−45x+100)）。
 * 机器不匹配 {@link #matches}（与祭坛同一套做法：由机器自己去 RecipeManager 查表），
 * 这样档位、缓存、副产物概率这些"机器侧状态"不会被硬塞进配方匹配。</p>
 */
public class ExtractionRecipe implements Recipe<Container> {

    /** 副产物：一件物品 + 初始概率（百分点，可负）。 */
    public record Byproduct(ItemStack stack, int chance) {
    }

    /** 提取机只有 2 个输出槽（§17.1 作者规格）：配方里多写的会被忽略。 */
    public static final int MAX_BYPRODUCTS = 2;

    private final ResourceLocation id;
    private final Ingredient input;
    private final int baseEnergy;
    private final int baseTime;
    private final int baseCost;
    private final List<Byproduct> byproducts;
    private final int minTier;
    private final int maxTier;

    public ExtractionRecipe(ResourceLocation id, Ingredient input, int baseEnergy, int baseTime, int baseCost,
                            List<Byproduct> byproducts, int minTier, int maxTier) {
        this.id = id;
        this.input = input;
        this.baseEnergy = Math.max(0, baseEnergy);
        this.baseTime = Math.max(0, baseTime);
        this.baseCost = Math.max(0, baseCost);
        // 规格是"2 个输出槽"（§17.1），所以在配方层就把副产物砍到前 2 个——
        // 免得数据写多了以后出现"机器只掷 2 个、JEI 却显示 3 个"的错位
        this.byproducts = List.copyOf(byproducts.size() > MAX_BYPRODUCTS
                ? byproducts.subList(0, MAX_BYPRODUCTS)
                : byproducts);
        this.minTier = TierMath.clampTier(minTier);
        this.maxTier = TierMath.clampTier(maxTier);
    }

    // ==================== 档位换算后的视图（机器与 JEI 都读这些） ====================

    /** 某档位的输出能量（RE）。 */
    public int energyAt(int tier) {
        return TierMath.energyOutput(baseEnergy, tier);
    }

    /** 某档位的处理时间（tick）。 */
    public int timeAt(int tier) {
        return TierMath.processingTicks(baseTime, tier);
    }

    /**
     * 某档位的"处理所需能量"（RE，一次性付）。
     *
     * <p><b>提取机这条与档位无关</b>（作者 2026-10-02 定）：初级写多少，各档就是多少——
     * 高档次只提升产出/概率/速度，不改变启动成本。富集仓那条才用
     * {@link TierMath#energyCost(int, int)}（`(5x²−45x+100)·R`，创造档免费）。</p>
     */
    public int costAt(int tier) {
        return baseCost;
    }

    /** 第 index 个副产物在某档位的概率（百分点，0~100）。 */
    public int chanceAt(int index, int tier) {
        if (index < 0 || index >= byproducts.size()) return 0;
        return TierMath.byproductChance(byproducts.get(index).chance(), tier);
    }

    /** 该档位能不能跑这条配方（作者：配方可声明最低/最高档位）。 */
    public boolean allowedAt(int tier) {
        int x = TierMath.clampTier(tier);
        return x >= minTier && x <= maxTier;
    }

    // ==================== 原始数据（JEI 展示"初级基准"时也要用） ====================

    public Ingredient getInput() {
        return input;
    }

    public int getBaseEnergy() {
        return baseEnergy;
    }

    public int getBaseTime() {
        return baseTime;
    }

    public int getBaseCost() {
        return baseCost;
    }

    public List<Byproduct> getByproducts() {
        return byproducts;
    }

    public int getMinTier() {
        return minTier;
    }

    public int getMaxTier() {
        return maxTier;
    }

    // ==================== Recipe 接口（与祭坛同一套：机器自己查表，不靠 matches） ====================

    @Override
    public boolean matches(Container container, Level level) {
        return false;
    }

    @Override
    public @NotNull ItemStack assemble(Container container, RegistryAccess access) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(RegistryAccess access) {
        // 提取的"产物"是能量；副产物只是概率掉落，所以这里给空（JEI 由我们的分类自己画）
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(input);
        return list;
    }

    @Override
    public @NotNull ResourceLocation getId() {
        return id;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipes.EXTRACTION_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return ModRecipes.EXTRACTION_TYPE.get();
    }

    // ==================== 序列化 ====================

    public static class Serializer implements RecipeSerializer<ExtractionRecipe> {

        @Override
        public ExtractionRecipe fromJson(ResourceLocation recipeId, com.google.gson.JsonObject json) {
            Ingredient input = Ingredient.fromJson(json.get("input"));
            int energy = json.has("energy") ? json.get("energy").getAsInt() : 0;
            int time = json.has("time") ? json.get("time").getAsInt() : 0;
            int cost = json.has("cost") ? json.get("cost").getAsInt() : 0;
            int minTier = json.has("minTier") ? json.get("minTier").getAsInt() : TierMath.MIN_TIER;
            int maxTier = json.has("maxTier") ? json.get("maxTier").getAsInt() : TierMath.MAX_TIER;

            List<Byproduct> byproducts = new ArrayList<>();
            if (json.has("byproducts")) {
                for (com.google.gson.JsonElement element : json.getAsJsonArray("byproducts")) {
                    com.google.gson.JsonObject obj = element.getAsJsonObject();
                    ItemStack stack = ShapedRecipe.itemStackFromJson(obj);
                    int chance = obj.has("chance") ? obj.get("chance").getAsInt() : 0;
                    byproducts.add(new Byproduct(stack, chance));
                }
            }
            return new ExtractionRecipe(recipeId, input, energy, time, cost, byproducts, minTier, maxTier);
        }

        @Override
        public ExtractionRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            Ingredient input = Ingredient.fromNetwork(buffer);
            int energy = buffer.readVarInt();
            int time = buffer.readVarInt();
            int cost = buffer.readVarInt();
            int minTier = buffer.readVarInt();
            int maxTier = buffer.readVarInt();
            int count = buffer.readVarInt();
            List<Byproduct> byproducts = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                byproducts.add(new Byproduct(buffer.readItem(), unzigzag(buffer.readVarInt())));
            }
            return new ExtractionRecipe(recipeId, input, energy, time, cost, byproducts, minTier, maxTier);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ExtractionRecipe recipe) {
            recipe.input.toNetwork(buffer);
            buffer.writeVarInt(recipe.baseEnergy);
            buffer.writeVarInt(recipe.baseTime);
            buffer.writeVarInt(recipe.baseCost);
            buffer.writeVarInt(recipe.minTier);
            buffer.writeVarInt(recipe.maxTier);
            buffer.writeVarInt(recipe.byproducts.size());
            for (Byproduct byproduct : recipe.byproducts) {
                buffer.writeItem(byproduct.stack());
                // chance 允许为负 → 用 zigzag 塞进 VarInt
                buffer.writeVarInt(zigzag(byproduct.chance()));
            }
        }

        private static int zigzag(int value) {
            return (value << 1) ^ (value >> 31);
        }

        private static int unzigzag(int value) {
            return (value >>> 1) ^ -(value & 1);
        }
    }
}
