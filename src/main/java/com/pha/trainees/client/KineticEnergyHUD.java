package com.pha.trainees.client;

import com.mojang.blaze3d.platform.Window;
import com.pha.trainees.item.AuriversiteRapierItem;
import com.pha.trainees.util.game.EnchantmentCalculator;
import com.pha.trainees.util.game.Tools;
import com.pha.trainees.util.game.physics.KineticEnergySystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class KineticEnergyHUD {

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.EXPERIENCE_BAR.id())) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null || mc.options.hideGui) {
            return;
        }

        // 检查主手和副手
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        ItemStack kineticSword = null;
        if (mainHand.getItem() instanceof AuriversiteRapierItem) {
            kineticSword = mainHand;
        } else if (offHand.getItem() instanceof AuriversiteRapierItem) {
            kineticSword = offHand;
        }

        if (kineticSword != null) {
            // 获取动能百分比（0-100）
            float energyPercentage = KineticEnergySystem.getKineticEnergyPercentage(kineticSword);
            // 获取实际动能值
            float energyValue = KineticEnergySystem.getKineticEnergyValue(kineticSword);

            // 渲染动能条，传入ItemStack用于获取实际最大值
            renderKineticBar(event.getGuiGraphics(), energyValue, energyPercentage, kineticSword, mc.getWindow());
        }
    }

    private static void renderKineticBar(GuiGraphics guiGraphics, float energyValue,
                                         float energyPercentage, ItemStack kineticSword, Window window) {
        int screenWidth = window.getGuiScaledWidth();
        int screenHeight = window.getGuiScaledHeight();

        // 经验条位置：屏幕底部中间
        int x = screenWidth / 2 - 91; // 经验条背景的X坐标
        int y = screenHeight - 56; // 经验条上方

        // 动能条尺寸
        int barWidth = 182;
        int barHeight = 5;

        // 绘制背景条
        guiGraphics.fill(x, y, x + barWidth, y + barHeight, 0xFF000000); // 黑色背景边框
        guiGraphics.fill(x + 1, y + 1, x + barWidth - 1, y + barHeight - 1, 0xFF555555); // 灰色背景

        // 计算填充宽度（基于实际动能值与最大值的比例）
        // 注意：这里使用百分比来计算填充，但限制最大为100%
        float fillRatio = Math.min(energyPercentage, 100f) / 100f;
        int fillWidth = Math.max(0, Math.min(barWidth - 2, (int) ((barWidth - 2) * fillRatio)));

        if (fillWidth > 0) {
            // 根据动能百分比选择颜色
            int color = getEnergyColor(energyPercentage);
            guiGraphics.fill(x + 1, y + 1, x + 1 + fillWidth, y + barHeight - 1, color);
        }

        // 绘制文字（显示百分比）
        String text = String.format("%.1f%%", energyPercentage);
        int textWidth = Minecraft.getInstance().font.width(text);
        int textX = screenWidth / 2 - textWidth / 2 + 30;
        int textY = y - 10;

        // 绘制文字背景
        guiGraphics.fill(textX - 2, textY - 2, textX + textWidth + 2, textY + 9, 0x80000000);
        // 绘制文字
        guiGraphics.drawString(Minecraft.getInstance().font, text, textX, textY, 0xFFFFFF);

        // 获取实际的最大动能值
        float actualMaxEnergy = EnchantmentCalculator.getEffectiveMaxKineticEnergy(kineticSword);

        String valueText = String.format("%.0f/%.0f", energyValue, actualMaxEnergy);
        int valueTextWidth = Minecraft.getInstance().font.width(valueText);
        int valueTextX = screenWidth / 2 - valueTextWidth / 2 - 30;
        int valueTextY = y - 10;

        guiGraphics.fill(valueTextX - 2, valueTextY - 2,
                valueTextX + valueTextWidth + 2, valueTextY + 9, 0x80000000);
        guiGraphics.drawString(Minecraft.getInstance().font, valueText,
                valueTextX, valueTextY, 0xFFFFFF);

    }

    private static int getEnergyColor(float energyPercentage) {
        // 根据动能值渐变颜色：低（红）->中（黄）->高（绿）->超高（紫）
        if (energyPercentage < 30) {
            // 红色到黄色渐变
            float ratio = energyPercentage / 30.0f;
            int r = 255;
            int g = (int) (255 * ratio);
            int b = 0;
            return (0xFF << 24) | (r << 16) | (g << 8) | b;
        } else if (energyPercentage < 70) {
            // 黄色到绿色渐变
            float ratio = (energyPercentage - 30) / 40.0f;
            int r = (int) (255 * (1 - ratio));
            int g = 255;
            int b = 0;
            return (0xFF << 24) | (r << 16) | (g << 8) | b;
        } else if (energyPercentage < 100) {
            // 绿色到青色渐变
            float ratio = (energyPercentage - 70) / 30.0f;
            int r = 0;
            int g = 255;
            int b = (int) (255 * ratio);
            return (0xFF << 24) | (r << 16) | (g << 8) | b;
        } else {
            // 超过100%时显示紫色
            float ratio = Math.min(1.0f, (energyPercentage - 100) / 100.0f);
            int r = 150 + (int)(105 * ratio);
            int g = 0;
            int b = 150 + (int)(105 * ratio);
            return (0xFF << 24) | (r << 16) | (g << 8) | b;
        }
    }
}