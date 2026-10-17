package com.pha.trainees.compat.jei;

import com.mojang.blaze3d.platform.InputConstants;
import com.pha.trainees.Main;
import com.pha.trainees.mana.TierMath;
import com.pha.trainees.recipe.ExtractionRecipe;
import com.pha.trainees.registry.ModBlocks;
import com.pha.trainees.util.interfaces.IHoverText;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.ITooltipBuilder;
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
 * 提取配方页（§17.1）：1 输入 → 2 输出 + 能量/耗时，**右下角常驻"当前档位：x 级"**。
 *
 * <p>按作者的设计（参照格雷"电压等级"）：配方数据一律是初级基准值，**页内所有数值都按当前档位换算**，
 * 于是玩家不用自己算 `R·2^x` 和概率加成。档位是**客户端浏览状态**（{@link #displayedTier}），
 * 不改动任何配方数据。</p>
 *
 * <p>⚠ JEI 15.20 接点击的**正确钩子在分类本身**：{@code IRecipeCategory#handleInput(recipe, mouseX, mouseY, key)}
 * 配合 {@code isHandled(recipe)} 返回 true（不返回 true 就不会被调用）。
 * （早先我 grep API 时漏了这两个方法，误以为"没有点击钩子"——记录在蓝本里，别再犯。）
 * 这里只在**配方页右下角那条**（档位文本所在区域）响应：左键升档、右键降档。</p>
 */
public class ExtractionRecipeCategory implements IRecipeCategory<ExtractionRecipe>, IHoverText {

    public static final ResourceLocation UID = new ResourceLocation(Main.MODID, "extraction");
    public static final RecipeType<ExtractionRecipe> TYPE = new RecipeType<>(UID, ExtractionRecipe.class);

    private static final int BG_WIDTH = 160;
    private static final int BG_HEIGHT = 96;
    private static final int SLOT = 18;
    private static final int INPUT_X = 14;
    private static final int INPUT_Y = BG_HEIGHT / 2 - SLOT / 2;
    private static final int OUT_A_X = 60;
    private static final int OUT_A_Y = 14;
    private static final int OUT_B_X = 60;
    private static final int OUT_B_Y = BG_HEIGHT - 14 - SLOT;
    private static final int TEXT_X = 88;

    /** 客户端浏览用的档位（0~4）：所有页共用，切换后所有页面一起变。 */
    private static int displayedTier = TierMath.MIN_TIER;

    private final IDrawable background;
    private final IDrawable icon;

    public ExtractionRecipeCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(BG_WIDTH, BG_HEIGHT);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(ModBlocks.EXTRACTOR_PRIMARY.get()));
    }

    // ==================== 档位状态（给鼠标控制器用） ====================

    public static int displayedTier() {
        return displayedTier;
    }

    /** 升/降档（+1 升、"左键"；-1 降、"右键"），自动夹取到 [0,4]。 */
    public static void cycleTier(int delta) {
        displayedTier = TierMath.clampTier(displayedTier + delta);
    }

    public static boolean canGoUp() {
        return displayedTier < TierMath.MAX_TIER;
    }

    public static boolean canGoDown() {
        return displayedTier > TierMath.MIN_TIER;
    }

    // ==================== 分类信息 ====================

    @Override
    public @NotNull RecipeType<ExtractionRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return tr("jei.trainees.extraction");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    /** 声明"本分类要接鼠标输入"，否则 JEI 根本不会调 {@link #handleInput}。 */
    @Override
    public boolean isHandled(ExtractionRecipe recipe) {
        return true;
    }

    /**
     * 左键升档 / 右键降档（作者 §17.1：参照格雷"电压等级"）。
     *
     * <p>只在**右下角那条**（档位文本所在区域：y ∈ [BG_HEIGHT−14, BG_HEIGHT)，x 在右半边）响应——
     * 这样点物品槽不会误触升档，而"点那行字"又能精确命中。</p>
     *
     * @return true = 这次点击我吃掉了（不再传给 JEI 的其它处理）
     */
    @Override
    public boolean handleInput(ExtractionRecipe recipe, double mouseX, double mouseY, InputConstants.Key key) {
        boolean inTierStrip = mouseY >= BG_HEIGHT - 14 && mouseX >= BG_WIDTH / 2.0;
        if (!inTierStrip) return false;

        if (key.equals(InputConstants.Type.MOUSE.getOrCreate(0))) {          // 左键 = 升档
            if (!canGoUp()) return false;
            cycleTier(1);
            return true;
        }
        if (key.equals(InputConstants.Type.MOUSE.getOrCreate(1))) {          // 右键 = 降档
            if (!canGoDown()) return false;
            cycleTier(-1);
            return true;
        }
        return false;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ExtractionRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, INPUT_Y).addIngredients(recipe.getInput());

        builder.addSlot(RecipeIngredientRole.OUTPUT, OUT_A_X, OUT_A_Y)
                .addItemStack(recipe.getByproducts().isEmpty()
                        ? ItemStack.EMPTY
                        : recipe.getByproducts().get(0).stack());
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUT_B_X, OUT_B_Y)
                .addItemStack(recipe.getByproducts().size() > 1
                        ? recipe.getByproducts().get(1).stack()
                        : ItemStack.EMPTY);
    }

    /**
     * 画当前档位下的数值（右侧一列）+ 右下角档位文本。
     *
     * <p>坐标是**配方局部坐标**（JEI 已把画笔移到背景左上角）。</p>
     */
    @Override
    public void draw(ExtractionRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics gui,
                     double mouseX, double mouseY) {
        int tier = displayedTier;
        Font font = Minecraft.getInstance().font;

        // 边框（与祭坛页同一套"代码绘制"风格）
        gui.fill(0, 0, BG_WIDTH, BG_HEIGHT, 0xFF101010);
        gui.fill(0, 0, BG_WIDTH, 1, 0xFF555555);
        gui.fill(0, BG_HEIGHT - 1, BG_WIDTH, BG_HEIGHT, 0xFF555555);
        gui.fill(0, 0, 1, BG_HEIGHT, 0xFF555555);
        gui.fill(BG_WIDTH - 1, 0, BG_WIDTH, BG_HEIGHT, 0xFF555555);

        // 输入 → 输出 的两个箭头
        int midY = INPUT_Y + SLOT / 2;
        gui.drawString(font, "→", INPUT_X + SLOT + 2, midY - 4, 0xFFFFD080, false);
        gui.drawString(font, "↗", INPUT_X + SLOT + 10, midY - 12, 0xFFFFD080, false);
        gui.drawString(font, "↘", INPUT_X + SLOT + 10, midY + 4, 0xFFFFD080, false);

        // 右侧数值列：能收到多少 / 要多久 / 启动要多少（随档位换算）
        int y = 12;
        gui.drawString(font, Component.translatable("jei.trainees.extraction.energy", recipe.energyAt(tier)),
                TEXT_X, y, 0xFFD0A0FF, false);
        y += 14;
        gui.drawString(font, Component.translatable("jei.trainees.extraction.time", recipe.timeAt(tier)),
                TEXT_X, y, 0xFFFFFFFF, false);
        int cost = recipe.costAt(tier);
        if (cost > 0) {
            y += 14;
            gui.drawString(font, Component.translatable("jei.trainees.extraction.cost", cost),
                    TEXT_X, y, 0xFFFF8080, false);
        }

        // 两个输出槽**右下角**的概率（按当前档位换算；作者要求：0% 也要写出来，别藏）
        drawChance(gui, font, recipe, 0, OUT_A_X, OUT_A_Y, tier);
        drawChance(gui, font, recipe, 1, OUT_B_X, OUT_B_Y, tier);

        // 右下角：当前档位（常驻文本；"左键升 / 右键降"的说明放在这条的悬停 tooltip 里，见 getTooltip）
        Component tierText = Component.translatable("jei.trainees.extraction.tier",
                Component.translatable(TierMath.tierNameKey(tier)));
        int tierWidth = font.width(tierText);
        gui.drawString(font, tierText, BG_WIDTH - tierWidth - 4, BG_HEIGHT - 12, 0xFFFFE080, false);

        // 档位低于该配方要求时给一句明说（minTier 玩法）
        if (!recipe.allowedAt(tier)) {
            gui.drawString(font, Component.translatable("jei.trainees.extraction.tier_locked"),
                    4, BG_HEIGHT - 24, 0xFFFF5555, false);
        }
    }

    /**
     * 概率文本：画在**对应物品槽的右下角**（带深色底，压在物品图标上也看得清）。
     *
     * <p>0% 也写（作者 2026-10-02）——概率是"升档会变的东西"，藏掉 0% 反而让人以为这槽没东西。</p>
     */
    private static void drawChance(GuiGraphics gui, Font font, ExtractionRecipe recipe, int index,
                                   int slotX, int slotY, int tier) {
        if (index < 0 || index >= recipe.getByproducts().size()) return;
        int chance = recipe.chanceAt(index, tier);
        Component text = Component.literal(chance + "%");
        int width = font.width(text);
        int x = slotX + SLOT - width - 1;
        int y = slotY + SLOT - 8;
        gui.fill(x - 1, y - 1, x + width + 1, y + 8, 0xA0000000);
        gui.drawString(font, text, x, y, chance >= 100 ? 0xFF55FF55 : 0xFFFFE080, false);
    }

    /**
     * 悬停在**右下角档位那条**上时，说明怎么操作（作者：提示属于悬停文本，不要画在页面上占地方）。
     */
    @Override
    public void getTooltip(ITooltipBuilder tooltip, ExtractionRecipe recipe, IRecipeSlotsView slotsView,
                           double mouseX, double mouseY) {
        if (mouseY >= BG_HEIGHT - 14 && mouseX >= BG_WIDTH / 2.0) {
            tooltip.add(Component.translatable("jei.trainees.extraction.tier_hint"));
        }
    }
}
