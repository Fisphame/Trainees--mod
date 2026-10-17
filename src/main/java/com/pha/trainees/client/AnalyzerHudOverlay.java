package com.pha.trainees.client;

import com.pha.trainees.Main;
import com.pha.trainees.chemistry.blockentity.BeakerBlockEntity;
import com.pha.trainees.chemistry.item.AnalyzerItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.List;

/**
 * 分析仪 HUD
 * 手持分析仪且准星对准烧杯时显示详细信息
 */
public class AnalyzerHudOverlay implements IGuiOverlay {

    @Override
    public void render(ForgeGui forgeGui, GuiGraphics guiGraphics, float partialTick,
                       int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // 检查玩家主手或副手是否持有分析仪
        ItemStack mainHand = mc.player.getMainHandItem();
        ItemStack offHand = mc.player.getOffhandItem();
        AnalyzerItem analyzer = getAnalyzerFromStack(mainHand);
        if (analyzer == null) {
            analyzer = getAnalyzerFromStack(offHand);
        }
        if (analyzer == null) return;

        // 检查准星是否对准化学容器（泛化：烧杯、电解槽等，只要 BE 是 BeakerBlockEntity 子类）
        if (!(mc.hitResult instanceof BlockHitResult blockHit)) return;
        BlockPos pos = blockHit.getBlockPos();

        // 获取容器数据
        BlockEntity entity = mc.level.getBlockEntity(pos);
        if (!(entity instanceof BeakerBlockEntity beaker)) return;

        // 获取分析结果
        List<Component> lines = analyzer.getAnalysisResult(beaker);

        // ====== 绘制信息面板 ======
        int padding = 10;
        int lineHeight = mc.font.lineHeight + 2;
        int maxWidth = lines.stream()
                .mapToInt(line -> mc.font.width(line))
                .max()
                .orElse(200) + padding * 2;

        // 面板位置：屏幕右上角（但靠左一点，避免遮挡）
        int panelX = screenWidth - maxWidth - 20;
        int panelY = 10;

        // 背景高度（根据行数动态计算）
        int totalHeight = lines.size() * lineHeight + padding * 2;

        // 绘制半透明背景
        guiGraphics.fill(panelX, panelY, panelX + maxWidth, panelY + totalHeight, 0xCC000000);
        guiGraphics.fill(panelX, panelY, panelX + maxWidth, panelY + 1, 0xFF555555); // 顶部边框
        guiGraphics.fill(panelX, panelY + totalHeight - 1, panelX + maxWidth, panelY + totalHeight, 0xFF555555); // 底部边框

        // 绘制文字
        int textX = panelX + padding;
        int textY = panelY + padding;
        for (Component line : lines) {
            guiGraphics.drawString(mc.font, line, textX, textY, 0xFFFFFF, false);
            textY += lineHeight;
        }

        // 绘制小指示器：显示当前使用的分析仪类型（本地化键）
        Component indicator = Component.translatable(analyzer.isCreativeMode()
                ? "gui.trainees.analyzer.item.creative_indicator"
                : "gui.trainees.analyzer.item.normal_indicator");
        guiGraphics.drawString(mc.font, indicator,
                panelX + padding, panelY + totalHeight + 4, 0xAAAAAA, false);
    }

    /**
     * 从物品堆中获取分析仪实例（如果是的话）
     */
    private AnalyzerItem getAnalyzerFromStack(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.getItem() instanceof AnalyzerItem analyzer) {
            return analyzer;
        }
        return null;
    }
}