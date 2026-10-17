package com.pha.trainees.compat.jei;

import com.pha.trainees.Main;
import com.pha.trainees.recipe.TrainerAltarRecipe;
import com.pha.trainees.registry.ModBlocks;
import com.pha.trainees.util.interfaces.IHoverText;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
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

@SuppressWarnings({"deprecation", "removal"})
public class TrainerAltarRecipeCategory implements IRecipeCategory<TrainerAltarRecipe>, IHoverText {
    public static final ResourceLocation UID = new ResourceLocation(Main.MODID, "trainer_altar");
    public static final RecipeType<TrainerAltarRecipe> TYPE = new RecipeType<>(UID, TrainerAltarRecipe.class);

    // ===== 可配置参数 =====
    // §19.37（作者 2026-10-02）：原尺寸 136×68 + 6px 间隔太挤——底下的输入槽会顶出背景边界，
    // 五个槽挤在中间看着也糊。改为 160×96、间隔 10px、槽边长 18（与 JEI 槽位贴图一致），
    // 四角各留 11px 边距放箭头。
    private static final int BG_WIDTH = 160;          // 背景宽度（像素）
    private static final int BG_HEIGHT = 104;         // 背景高度：96 + 底部 8px 条，专门放"消耗 N RE"文本
    private static final int GAP = 10;                // 槽位边缘之间的绝对间隔（像素）
    private static final int SLOT_SIZE = 18;          // 物品槽边长（与 JEI 槽位贴图一致）
    private static final int CORNER_PADDING = 4;      // 右下角文本与边框的间距
    // =====================

    // 背景中心坐标（整数除法取整，中心允许偏移0.5像素，不影响槽位放置）
    private static final int CENTER_X = BG_WIDTH / 2;
    private static final int CENTER_Y = BG_HEIGHT / 2;

    // 输出槽左上角坐标（槽中心与背景中心重合）
    private static final int OUTPUT_X = CENTER_X - SLOT_SIZE / 2;
    private static final int OUTPUT_Y = CENTER_Y - SLOT_SIZE / 2;

    // 四个输入槽左上角坐标（顺时针：左、上、右、下）
    private static final int LEFT_X   = OUTPUT_X - SLOT_SIZE - GAP;
    private static final int LEFT_Y   = OUTPUT_Y;
    private static final int TOP_X    = OUTPUT_X;
    private static final int TOP_Y    = OUTPUT_Y - SLOT_SIZE - GAP;
    private static final int RIGHT_X  = OUTPUT_X + SLOT_SIZE + GAP;
    private static final int RIGHT_Y  = OUTPUT_Y;
    private static final int BOTTOM_X = OUTPUT_X;
    private static final int BOTTOM_Y = OUTPUT_Y + SLOT_SIZE + GAP;

    private final IDrawable background;
    private final IDrawable icon;

    public TrainerAltarRecipeCategory(IGuiHelper helper) {
        // §19.34：背景改为**代码绘制**（箭头指向中心产出）。
        // 原因：原来的 textures/gui/jei_trainer_altar.png 是 116×54，而分类声明的是 136×68，
        // JEI 会把贴图拉成非等比（横向 ×1.17、纵向 ×1.26）——所以看着"被拉伸"；
        // 而且那张图的布局（左侧 2×2 格子 + 右侧产出）与下面的槽位布局（十字 + 中心产出）根本不一致。
        // 代码绘制与槽位坐标共用同一组常量，永远不会对不上；另外两个分类也是这么做的。
        // 以后要换回美术图：把图做成 **正好 136×68**、四输入指向中心，再替换成
        // helper.createDrawable(texture, 0, 0, BG_WIDTH, BG_HEIGHT) 即可。
        this.background = new AltarBackground();
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.ALTAR_CORE_BLOCK.get()));
    }

    @Override
    public @NotNull RecipeType<TrainerAltarRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return tr("jei.trainees.trainer_altar");
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
    public void setRecipe(IRecipeLayoutBuilder builder, TrainerAltarRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, LEFT_X, LEFT_Y)
                .addIngredients(recipe.getInput1());
        builder.addSlot(RecipeIngredientRole.INPUT, TOP_X, TOP_Y)
                .addIngredients(recipe.getInput2());
        builder.addSlot(RecipeIngredientRole.INPUT, RIGHT_X, RIGHT_Y)
                .addIngredients(recipe.getInput3());
        builder.addSlot(RecipeIngredientRole.INPUT, BOTTOM_X, BOTTOM_Y)
                .addIngredients(recipe.getInput4());

        // 输出槽（中心）
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
                .addItemStack(recipe.getResultItem());
    }

    /**
     * 配方页**右下角文本**：精炼精华消耗（§17.1 增强层）。
     *
     * <p>作者 2026-10-02：不要悬停提示，要**直接看得见**的文本放右下角——玩家扫一眼就知道要攒多少 RE。
     * 坐标是"配方局部坐标"（JEI 已经把画笔移到背景左上角），所以能直接用槽位那套常量。
     * 基础配方（免费）**不写**"消耗 0"，免得每页都多一行噪音。</p>
     */
    @Override
    public void draw(TrainerAltarRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics, double mouseX, double mouseY) {
        int cost = recipe.getManaCost();
        if (cost <= 0) return;
        Font font = Minecraft.getInstance().font;
        Component text = Component.translatable("jei.trainees.altar.mana_cost", cost);
        int x = BG_WIDTH - font.width(text) - CORNER_PADDING;
        int y = BG_HEIGHT - font.lineHeight - CORNER_PADDING;
        guiGraphics.drawString(font, text, x, y, 0xFFFFFFFF, false);
    }

    /**
     * 代码绘制的祭坛示意背景（§19.34）：面板边框 + 四个指向中心产出槽的箭头。
     *
     * <p>尺寸常量与槽位坐标同源，所以"图"和"槽"不可能错位，也不存在贴图被拉伸的问题。</p>
     */
    private static class AltarBackground implements IDrawable {

        @Override
        public int getWidth() {
            return BG_WIDTH;
        }

        @Override
        public int getHeight() {
            return BG_HEIGHT;
        }

        @Override
        public void draw(@NotNull GuiGraphics guiGraphics, int xOffset, int yOffset) {
            guiGraphics.fill(xOffset, yOffset, xOffset + BG_WIDTH, yOffset + BG_HEIGHT, 0xFF101010);
            guiGraphics.fill(xOffset, yOffset, xOffset + BG_WIDTH, yOffset + 1, 0xFF555555);
            guiGraphics.fill(xOffset, yOffset + BG_HEIGHT - 1, xOffset + BG_WIDTH, yOffset + BG_HEIGHT, 0xFF555555);
            guiGraphics.fill(xOffset, yOffset, xOffset + 1, yOffset + BG_HEIGHT, 0xFF555555);
            guiGraphics.fill(xOffset + BG_WIDTH - 1, yOffset, xOffset + BG_WIDTH, yOffset + BG_HEIGHT, 0xFF555555);

            // 四个箭头都指向中心产出槽
            Font font = Minecraft.getInstance().font;
            int midX = OUTPUT_X + SLOT_SIZE / 2;
            int midY = OUTPUT_Y + SLOT_SIZE / 2;
            guiGraphics.drawString(font, "→", xOffset + LEFT_X + SLOT_SIZE + 2, yOffset + midY - 4, 0xFFFFD080, false);
            guiGraphics.drawString(font, "←", xOffset + RIGHT_X - 8, yOffset + midY - 4, 0xFFFFD080, false);
            guiGraphics.drawString(font, "↓", xOffset + midX - 4, yOffset + TOP_Y + SLOT_SIZE + 1, 0xFFFFD080, false);
            guiGraphics.drawString(font, "↑", xOffset + midX - 4, yOffset + BOTTOM_Y - 9, 0xFFFFD080, false);
        }
    }
}