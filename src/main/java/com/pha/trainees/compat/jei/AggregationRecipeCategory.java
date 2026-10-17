package com.pha.trainees.compat.jei;

import com.pha.trainees.Main;
import com.pha.trainees.recipe.AggregationRecipe;
import com.pha.trainees.registry.ModBlocks;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 聚合配方页（§17.1）：**RE → 物品**，聚合机只有 1 档，所以没有档位 UI。
 *
 * <p>与提取页同风格：左侧写消耗与耗时，右侧是产物槽。**模板槽**的用法（作者选的 B 方案）
 * 写在页面提示里：把产物放进模板槽，机器就跑这条配方。</p>
 */
public class AggregationRecipeCategory implements IRecipeCategory<AggregationRecipe> {

    public static final ResourceLocation UID = new ResourceLocation(Main.MODID, "aggregation");
    public static final RecipeType<AggregationRecipe> TYPE = new RecipeType<>(UID, AggregationRecipe.class);

    private static final int BG_WIDTH = 132;
    private static final int BG_HEIGHT = 58;
    private static final int SLOT = 18;
    private static final int OUT_X = 96;
    private static final int OUT_Y = 14;

    private final IDrawable background;
    private final IDrawable icon;

    public AggregationRecipeCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(BG_WIDTH, BG_HEIGHT);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(ModBlocks.REFINED_AGGREGATOR.get()));
    }

    @Override
    public @NotNull RecipeType<AggregationRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("jei.trainees.aggregation");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AggregationRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUT_X, OUT_Y).addItemStack(recipe.getResult());
    }

    @Override
    public void draw(AggregationRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics gui,
                     double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        gui.fill(0, 0, BG_WIDTH, BG_HEIGHT, 0xFF101010);
        gui.fill(0, 0, BG_WIDTH, 1, 0xFF555555);
        gui.fill(0, BG_HEIGHT - 1, BG_WIDTH, BG_HEIGHT, 0xFF555555);
        gui.fill(0, 0, 1, BG_HEIGHT, 0xFF555555);
        gui.fill(BG_WIDTH - 1, 0, BG_WIDTH, BG_HEIGHT, 0xFF555555);

        gui.drawString(font, Component.translatable("jei.trainees.aggregation.energy", recipe.getEnergy()),
                6, 10, 0xFFD0A0FF, false);
        gui.drawString(font, Component.translatable("jei.trainees.aggregation.time", recipe.getTime()),
                6, 24, 0xFFFFFFFF, false);
        gui.drawString(font, "→", OUT_X - 16, OUT_Y + 5, 0xFFFFD080, false);

        // 模板槽用法（作者选的 B 方案）：写在页脚，玩家不用去翻文档
        gui.drawString(font, Component.translatable("jei.trainees.aggregation.template"),
                4, BG_HEIGHT - 12, 0xFF808080, false);
    }
}
