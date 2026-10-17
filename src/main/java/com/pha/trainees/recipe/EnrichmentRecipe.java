package com.pha.trainees.recipe;

import com.pha.trainees.mana.TierMath;
import com.pha.trainees.registry.ModRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * "富集"配方（§17.1）：把**多个原料聚成"富集态"**，之后提取能拿更多 RE、更快、概率更高。
 *
 * <p>配方结构完全按作者的口述：</p>
 * <ul>
 *   <li>输入：**2 个 item 槽**（1 消耗 + 1 催化剂）+ **2 个流体槽**（1 消耗 + 1 催化剂）+ **所需 RE**</li>
 *   <li>输出：**2 个 item 槽** + **2 个流体槽**</li>
 *   <li>另有**处理时间**</li>
 * </ul>
 *
 * <p>档位函数（与提取机共用同一条多项式 `5x²−45x+100`，但**富集仓把它用在能量上**）：</p>
 * <ul>
 *   <li>所需 RE = max((5x²−45x+100)·R, 0) —— 高级档更省电，创造档免费（作者要保留 0）</li>
 *   <li>处理时间 = max((5x²−45x+100)·T, 1)</li>
 * </ul>
 *
 * <p>催化剂槽按作者意图是"可选的额外要求"（例如化学线来的催化剂），所以 JSON 里可省略。</p>
 */
public class EnrichmentRecipe implements Recipe<Container> {

    /** 输出的物品/流体条目（流体用注册名 + mB）。 */
    public record FluidOutput(Fluid fluid, int amount) {
    }

    private final ResourceLocation id;
    @Nullable
    private final Ingredient inputItem;
    @Nullable
    private final Ingredient catalystItem;
    @Nullable
    private final FluidStack inputFluid;
    @Nullable
    private final FluidStack catalystFluid;
    private final int baseEnergy;
    private final int baseTime;
    private final List<ItemStack> itemResults;
    private final List<FluidOutput> fluidResults;

    public EnrichmentRecipe(ResourceLocation id, @Nullable Ingredient inputItem, @Nullable Ingredient catalystItem,
                            @Nullable FluidStack inputFluid, @Nullable FluidStack catalystFluid,
                            int baseEnergy, int baseTime,
                            List<ItemStack> itemResults, List<FluidOutput> fluidResults) {
        this.id = id;
        this.inputItem = inputItem;
        this.catalystItem = catalystItem;
        this.inputFluid = inputFluid;
        this.catalystFluid = catalystFluid;
        this.baseEnergy = Math.max(0, baseEnergy);
        this.baseTime = Math.max(0, baseTime);
        this.itemResults = List.copyOf(itemResults.size() > 2 ? itemResults.subList(0, 2) : itemResults);
        this.fluidResults = List.copyOf(fluidResults.size() > 2 ? fluidResults.subList(0, 2) : fluidResults);
    }

    // ==================== 档位换算后的视图 ====================

    /** 某档位所需的 RE（富集仓：能量走多项式，高级档更省）。 */
    public int energyAt(int tier) {
        return TierMath.energyCost(baseEnergy, tier);
    }

    /** 某档位的处理时间。 */
    public int timeAt(int tier) {
        return TierMath.processingTicks(baseTime, tier);
    }

    // ==================== 原始数据（JEI 展示"初级基准"用） ====================

    @Nullable
    public Ingredient getInputItem() {
        return inputItem;
    }

    @Nullable
    public Ingredient getCatalystItem() {
        return catalystItem;
    }

    @Nullable
    public FluidStack getInputFluid() {
        return inputFluid;
    }

    @Nullable
    public FluidStack getCatalystFluid() {
        return catalystFluid;
    }

    public int getBaseEnergy() {
        return baseEnergy;
    }

    public int getBaseTime() {
        return baseTime;
    }

    public List<ItemStack> getItemResults() {
        return itemResults;
    }

    public List<FluidOutput> getFluidResults() {
        return fluidResults;
    }

    // ==================== Recipe 接口（与提取/聚合同一套：机器自己查表） ====================

    @Override
    public boolean matches(Container container, Level level) {
        return false;
    }

    @Override
    public @NotNull ItemStack assemble(Container container, RegistryAccess access) {
        return itemResults.isEmpty() ? ItemStack.EMPTY : itemResults.get(0).copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(RegistryAccess access) {
        return itemResults.isEmpty() ? ItemStack.EMPTY : itemResults.get(0);
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        if (inputItem != null) list.add(inputItem);
        if (catalystItem != null) list.add(catalystItem);
        return list;
    }

    @Override
    public @NotNull ResourceLocation getId() {
        return id;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipes.ENRICHMENT_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return ModRecipes.ENRICHMENT_TYPE.get();
    }

    // ==================== 序列化 ====================

    public static class Serializer implements RecipeSerializer<EnrichmentRecipe> {

        @Override
        public EnrichmentRecipe fromJson(ResourceLocation recipeId, com.google.gson.JsonObject json) {
            Ingredient input = json.has("item") ? Ingredient.fromJson(json.get("item")) : null;
            Ingredient catalyst = json.has("catalyst") ? Ingredient.fromJson(json.get("catalyst")) : null;
            FluidStack inputFluid = json.has("fluid") ? fluidFromJson(json.getAsJsonObject("fluid")) : null;
            FluidStack catalystFluid = json.has("fluidCatalyst")
                    ? fluidFromJson(json.getAsJsonObject("fluidCatalyst"))
                    : null;
            int energy = json.has("energy") ? json.get("energy").getAsInt() : 0;
            int time = json.has("time") ? json.get("time").getAsInt() : 0;

            List<ItemStack> itemResults = new ArrayList<>();
            if (json.has("results")) {
                for (com.google.gson.JsonElement element : json.getAsJsonArray("results")) {
                    itemResults.add(ShapedRecipe.itemStackFromJson(element.getAsJsonObject()));
                }
            }
            List<FluidOutput> fluidResults = new ArrayList<>();
            if (json.has("fluidResults")) {
                for (com.google.gson.JsonElement element : json.getAsJsonArray("fluidResults")) {
                    com.google.gson.JsonObject obj = element.getAsJsonObject();
                    Fluid fluid = BuiltInRegistries.FLUID.get(new ResourceLocation(obj.get("fluid").getAsString()));
                    int amount = obj.has("amount") ? obj.get("amount").getAsInt() : 1000;
                    if (fluid != null) fluidResults.add(new FluidOutput(fluid, amount));
                }
            }
            return new EnrichmentRecipe(recipeId, input, catalyst, inputFluid, catalystFluid,
                    energy, time, itemResults, fluidResults);
        }

        private static FluidStack fluidFromJson(com.google.gson.JsonObject obj) {
            Fluid fluid = BuiltInRegistries.FLUID.get(new ResourceLocation(obj.get("fluid").getAsString()));
            int amount = obj.has("amount") ? obj.get("amount").getAsInt() : 1000;
            return fluid == null ? null : new FluidStack(fluid, amount);
        }

        @Override
        public EnrichmentRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            Ingredient input = buffer.readBoolean() ? Ingredient.fromNetwork(buffer) : null;
            Ingredient catalyst = buffer.readBoolean() ? Ingredient.fromNetwork(buffer) : null;
            FluidStack inputFluid = buffer.readBoolean() ? FluidStack.readFromPacket(buffer) : null;
            FluidStack catalystFluid = buffer.readBoolean() ? FluidStack.readFromPacket(buffer) : null;
            int energy = buffer.readVarInt();
            int time = buffer.readVarInt();

            int itemCount = buffer.readVarInt();
            List<ItemStack> itemResults = new ArrayList<>(itemCount);
            for (int i = 0; i < itemCount; i++) {
                itemResults.add(buffer.readItem());
            }
            int fluidCount = buffer.readVarInt();
            List<FluidOutput> fluidResults = new ArrayList<>(fluidCount);
            for (int i = 0; i < fluidCount; i++) {
                FluidStack stack = FluidStack.readFromPacket(buffer);
                fluidResults.add(new FluidOutput(stack.getFluid(), stack.getAmount()));
            }
            return new EnrichmentRecipe(recipeId, input, catalyst, inputFluid, catalystFluid,
                    energy, time, itemResults, fluidResults);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, EnrichmentRecipe recipe) {
            writeIngredient(buffer, recipe.inputItem);
            writeIngredient(buffer, recipe.catalystItem);
            writeFluid(buffer, recipe.inputFluid);
            writeFluid(buffer, recipe.catalystFluid);
            buffer.writeVarInt(recipe.baseEnergy);
            buffer.writeVarInt(recipe.baseTime);
            buffer.writeVarInt(recipe.itemResults.size());
            for (ItemStack stack : recipe.itemResults) {
                buffer.writeItem(stack);
            }
            buffer.writeVarInt(recipe.fluidResults.size());
            for (FluidOutput output : recipe.fluidResults) {
                new FluidStack(output.fluid(), output.amount()).writeToPacket(buffer);
            }
        }

        private static void writeIngredient(FriendlyByteBuf buffer, @Nullable Ingredient ingredient) {
            buffer.writeBoolean(ingredient != null);
            if (ingredient != null) ingredient.toNetwork(buffer);
        }

        private static void writeFluid(FriendlyByteBuf buffer, @Nullable FluidStack stack) {
            buffer.writeBoolean(stack != null);
            if (stack != null) stack.writeToPacket(buffer);
        }
    }
}
