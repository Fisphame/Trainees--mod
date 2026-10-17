package com.pha.trainees.recipe;

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

/**
 * "聚合"配方（§17.1）：**消耗 RE → 产出物品**，聚合机只有 1 个档位（所以没有档位函数）。
 *
 * <pre>{@code
 * {
 *   "type": "trainees:aggregation",
 *   "energy": 1600,                                         // 消耗多少 RE
 *   "time":   20,                                           // 处理耗时（tick）；聚合机"处理很快"，默认给 20
 *   "result": { "item": "trainees:refined_point", "count": 1 }
 * }
 * }</pre>
 *
 * <p>数据驱动（作者选定），默认那条就是 1600 RE → 1 精炼点；以后要加更贵的聚合产物只写 JSON。</p>
 */
public class AggregationRecipe implements Recipe<Container> {

    private final ResourceLocation id;
    private final int energy;
    private final int time;
    private final ItemStack result;

    public AggregationRecipe(ResourceLocation id, int energy, int time, ItemStack result) {
        this.id = id;
        this.energy = Math.max(1, energy);
        this.time = Math.max(1, time);
        this.result = result;
    }

    public int getEnergy() {
        return energy;
    }

    public int getTime() {
        return time;
    }

    public ItemStack getResult() {
        return result;
    }

    @Override
    public boolean matches(Container container, Level level) {
        return false;
    }

    @Override
    public @NotNull ItemStack assemble(Container container, RegistryAccess access) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(RegistryAccess access) {
        return result;
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        return NonNullList.create();
    }

    @Override
    public @NotNull ResourceLocation getId() {
        return id;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipes.AGGREGATION_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return ModRecipes.AGGREGATION_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<AggregationRecipe> {

        @Override
        public AggregationRecipe fromJson(ResourceLocation recipeId, com.google.gson.JsonObject json) {
            int energy = json.has("energy") ? json.get("energy").getAsInt() : 1600;
            int time = json.has("time") ? json.get("time").getAsInt() : 20;
            ItemStack result = ShapedRecipe.itemStackFromJson(json.getAsJsonObject("result"));
            return new AggregationRecipe(recipeId, energy, time, result);
        }

        @Override
        public AggregationRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            return new AggregationRecipe(recipeId, buffer.readVarInt(), buffer.readVarInt(), buffer.readItem());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, AggregationRecipe recipe) {
            buffer.writeVarInt(recipe.energy);
            buffer.writeVarInt(recipe.time);
            buffer.writeItem(recipe.result);
        }
    }
}
