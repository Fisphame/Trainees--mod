package com.pha.trainees.compat.jei;

import com.mojang.blaze3d.platform.InputConstants;
import com.pha.trainees.Main;
import com.pha.trainees.mana.TierMath;
import com.pha.trainees.recipe.EnrichmentRecipe;
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
 * 富集配方页（§17.1）：**多原料 → 富集态**，所需 RE 与耗时都随档位变。
 *
 * <p>与提取页同风格：右侧写当前档位下的数值，**右下角常驻档位文本**且**左键升 / 右键降**
 * （富集是"高级档更省电也更快"，所以档位切换对它尤其有意义）。
 * 流体的需求写成文字行（3 个流体槽画出来太挤，而且数值本来就要随档位变）。</p>
 */
public class EnrichmentRecipeCategory implements IRecipeCategory<EnrichmentRecipe> {

    public static final ResourceLocation UID = new ResourceLocation(Main.MODID, "enrichment");
    public static final RecipeType<EnrichmentRecipe> TYPE = new RecipeType<>(UID, EnrichmentRecipe.class);

    private static final int BG_WIDTH = 168;
    private static final int BG_HEIGHT = 104;
    private static final int SLOT = 18;

    /** 与提取页共用一个浏览档位（作者：页内切档，页内数值一致）。 */
    private static int displayedTier = TierMath.MIN_TIER;

    private final IDrawable background;
    private final IDrawable icon;

    public EnrichmentRecipeCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(BG_WIDTH, BG_HEIGHT);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(ModBlocks.ENRICHMENT_PRIMARY.get()));
    }

    public static void cycleTier(int delta) {
        displayedTier = TierMath.clampTier(displayedTier + delta);
    }

    @Override
    public @NotNull RecipeType<EnrichmentRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("jei.trainees.enrichment");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    /** 要接鼠标输入（切档），必须返回 true，否则 JEI 不调 handleInput。 */
    @Override
    public boolean isHandled(EnrichmentRecipe recipe) {
        return true;
    }

    @Override
    public boolean handleInput(EnrichmentRecipe recipe, double mouseX, double mouseY, InputConstants.Key key) {
        boolean inTierStrip = mouseY >= BG_HEIGHT - 14 && mouseX >= BG_WIDTH / 2.0;
        if (!inTierStrip) return false;
        if (key.equals(InputConstants.Type.MOUSE.getOrCreate(0))) {
            if (displayedTier >= TierMath.MAX_TIER) return false;
            cycleTier(1);
            return true;
        }
        if (key.equals(InputConstants.Type.MOUSE.getOrCreate(1))) {
            if (displayedTier <= TierMath.MIN_TIER) return false;
            cycleTier(-1);
            return true;
        }
        return false;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, EnrichmentRecipe recipe, IFocusGroup focuses) {
        // A4：流体也画成**槽**（以前只写"250 mB 水"的文字行，看不清也点不了）
        // 注意：JEI 15.20 的 VanillaTypes 只有 ITEM_STACK，**流体在 ForgeTypes.FLUID_STACK**（javap 查过）
        if (recipe.getInputItem() != null) {
            builder.addSlot(RecipeIngredientRole.INPUT, 8, 18).addIngredients(recipe.getInputItem());
        }
        if (recipe.getInputFluid() != null) {
            builder.addSlot(RecipeIngredientRole.INPUT, 8, 38)
                    .addIngredient(mezz.jei.api.forge.ForgeTypes.FLUID_STACK, recipe.getInputFluid());
        }
        if (recipe.getCatalystItem() != null) {
            builder.addSlot(RecipeIngredientRole.INPUT, 8, 60).addIngredients(recipe.getCatalystItem());
        }
        if (recipe.getCatalystFluid() != null) {
            builder.addSlot(RecipeIngredientRole.INPUT, 8, 80)
                    .addIngredient(mezz.jei.api.forge.ForgeTypes.FLUID_STACK, recipe.getCatalystFluid());
        }
        for (int i = 0; i < recipe.getItemResults().size(); i++) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 132, 20 + i * 34)
                    .addItemStack(recipe.getItemResults().get(i));
        }
        // 流体产物也画槽（与"输入 2 流体 + 输出 2 流体"的规格对齐）
        for (int i = 0; i < recipe.getFluidResults().size(); i++) {
            EnrichmentRecipe.FluidOutput output = recipe.getFluidResults().get(i);
            builder.addSlot(RecipeIngredientRole.OUTPUT, 132, 74 + i * 18)
                    .addIngredient(mezz.jei.api.forge.ForgeTypes.FLUID_STACK,
                            new net.minecraftforge.fluids.FluidStack(output.fluid(), output.amount()));
        }
    }

    @Override
    public void draw(EnrichmentRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics gui,
                     double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        int tier = displayedTier;

        gui.fill(0, 0, BG_WIDTH, BG_HEIGHT, 0xFF101010);
        gui.fill(0, 0, BG_WIDTH, 1, 0xFF555555);
        gui.fill(0, BG_HEIGHT - 1, BG_WIDTH, BG_HEIGHT, 0xFF555555);
        gui.fill(0, 0, 1, BG_HEIGHT, 0xFF555555);
        gui.fill(BG_WIDTH - 1, 0, BG_WIDTH, BG_HEIGHT, 0xFF555555);

        gui.drawString(font, "→", 104, 26, 0xFFFFD080, false);

        // 右侧数值列（随档位换算）
        gui.drawString(font, Component.translatable("jei.trainees.enrichment.energy", recipe.energyAt(tier)),
                36, 12, 0xFFD0A0FF, false);
        gui.drawString(font, Component.translatable("jei.trainees.enrichment.time", recipe.timeAt(tier)),
                36, 26, 0xFFFFFFFF, false);

        // 流体需求现在画成槽（见 setRecipe），这里不再写文字行——否则会和槽位重叠
        if (recipe.getCatalystItem() != null) {
            gui.drawString(font, Component.translatable("jei.trainees.enrichment.catalyst"),
                    30, 65, 0xFF808080, false);
        }

        // 右下角档位（常驻）——操作提示在悬停里，见 getTooltip
        Component tierText = Component.translatable("jei.trainees.extraction.tier",
                Component.translatable(TierMath.tierNameKey(tier)));
        gui.drawString(font, tierText, BG_WIDTH - font.width(tierText) - 4, BG_HEIGHT - 12, 0xFFFFE080, false);
    }

    @Override
    public void getTooltip(mezz.jei.api.gui.builder.ITooltipBuilder tooltip, EnrichmentRecipe recipe,
                           IRecipeSlotsView slotsView, double mouseX, double mouseY) {
        if (mouseY >= BG_HEIGHT - 14 && mouseX >= BG_WIDTH / 2.0) {
            tooltip.add(Component.translatable("jei.trainees.extraction.tier_hint"));
        }
    }
}
