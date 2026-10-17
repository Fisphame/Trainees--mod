package com.pha.trainees.compat.jei;

import com.pha.trainees.chemistry.particle.IonType;
import com.pha.trainees.chemistry.util.IonDisplay;
import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;

/**
 * 裸离子条目的 JEI 渲染器（§19.16）：在 16×16 空间里画"色块 + 化学式缩写"，
 * tooltip 给完整化学式与物理化学数据。
 *
 * <p>正式形态贴图（粉末/晶体/溶液…）落地后，可把色块替换为形态底图 + tint，接口不变。</p>
 */
public class IonIngredientRenderer implements IIngredientRenderer<IonType> {

    private static final int SIZE = 16;
    /** 色块内边（与下面 fill(1,1,15,15) 对应） */
    private static final int PAD = 1;
    /** 最大字号缩放：0.5 时 4~5 个字符正好铺满 16 px */
    private static final float MAX_SCALE = 0.5f;

    @Override
    public void render(GuiGraphics guiGraphics, IonType ion) {
        int color = ion.getDisplayColor();
        // 底 + 色块（后续可换成形态贴图 tint）
        guiGraphics.fill(0, 0, SIZE, SIZE, 0xFF101010);
        guiGraphics.fill(1, 1, SIZE - 1, SIZE - 1, color);

        // 化学式**整串**绘制，按可用宽度自适应缩放（§19.34）：
        // 原来把式子截成前 3 个字符（SO₄²⁻ → SO₄），信息直接丢了；16 px 里要放下
        // HCO₃⁻ / HSO₄⁻ 这种 4~5 字符的式子只能缩，缩到能塞进 14 px 为止。
        // 亮色底用黑字、暗色底用白字。
        Font font = Minecraft.getInstance().font;
        String label = IonDisplay.format(ion.getId().getPath());
        int textWidth = font.width(label);
        int usable = SIZE - 2 * PAD;
        float scale = textWidth <= 0 ? MAX_SCALE : Math.min(MAX_SCALE, usable / (float) textWidth);
        float x = (SIZE - textWidth * scale) / 2f;
        float y = (SIZE - 8 * scale) / 2f + 0.5f;        // 8 ≈ 字体行高，垂直居中

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(scale, scale, 1.0f);
        guiGraphics.drawString(font, label, 0, 0, isBright(color) ? 0xFF202020 : 0xFFFFFFFF, false);
        pose.popPose();
    }

    /** 感知亮度判据（用于选择前景文字色） */
    private boolean isBright(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return (r * 299 + g * 587 + b * 114) / 1000 > 150;
    }

    /**
     * tooltip（JEI 也用它做搜索）。注意：JEI 15 里 {@code getTooltip(V, TooltipFlag)} 仍是抽象方法，
     * 新签名 {@code getTooltip(V, Player, TooltipFlag)} 默认委托到它。
     */
    @SuppressWarnings("deprecation")
    @Override
    public List<Component> getTooltip(IonType ion, TooltipFlag tooltipFlag) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(IonDisplay.format(ion.getId().getPath())).withStyle(ChatFormatting.BOLD));
        lines.add(Component.translatable("jei.trainees.ion.id", ion.getId().toString())
                .withStyle(ChatFormatting.DARK_GRAY));
        // §19.34：不再显示「形态: 粉末/气体…」——那是**代表物品**的形态，不是这个物种在容器里的形态
        // （同一个 NaCl 可以是固体、溶液或熔融态）。图例统一写在分类页上，逐条重复只会误导。
        lines.add(Component.translatable("jei.trainees.ion.molar_mass", fmt(ion.getMolarMass(), 2)));
        lines.add(Component.translatable("jei.trainees.ion.formation",
                fmt(ion.getFormationEnthalpy(), 1), fmt(ion.getFormationGibbs(), 1)));
        if (ion.getToxicityLevel() > 0) {
            lines.add(Component.translatable("jei.trainees.ion.toxicity", ion.getToxicityLevel()));
        }
        if (!ion.getTags().isEmpty()) {
            lines.add(Component.translatable("jei.trainees.ion.tags", ion.getTags().toString()));
        }
        return lines;
    }

    private String fmt(double v, int decimals) {
        return String.format("%." + decimals + "f", v);
    }
}
