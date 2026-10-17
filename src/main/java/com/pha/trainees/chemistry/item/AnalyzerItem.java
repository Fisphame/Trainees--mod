package com.pha.trainees.chemistry.item;

import com.pha.trainees.chemistry.blockentity.BeakerBlockEntity;
import com.pha.trainees.chemistry.util.AnalyzerAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 分析仪基类
 * 手持时右键化学容器（烧杯/电解槽/未来机器）打开诊断面板（§19.6/19.7），潜行右键发聊天简报；
 * 准星对准容器时 HUD 常驻显示。
 */
public class AnalyzerItem extends Item {

    public AnalyzerItem(Properties properties) {
        super(properties);
    }

    /**
     * 右键点击方块：统一交给 {@link AnalyzerAccess}（泛化到所有 {@code IChemicalContainer}）。
     */
    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        return AnalyzerAccess.tryUse(context.getLevel(), context.getClickedPos(),
                player, context.getItemInHand());
    }

    /**
     * 获取要显示的信息（由子类重写）
     * @param beaker 目标烧杯
     * @return 信息组件列表
     */
    public List<Component> getAnalysisResult(BeakerBlockEntity beaker) {
        // 默认显示容器基础状态（本地化键，子类可覆写为更详细面板）
        return List.of(
                Component.translatable("gui.trainees.analyzer.title"),
                Component.translatable("gui.trainees.analyzer.temperature",
                        String.format("%.1f", beaker.getTemperature()),
                        String.format("%.1f", beaker.getTemperature() - 273.15))
        );
    }

    /**
     * 判断是否为创造模式分析仪（子类重写）
     */
    public boolean isCreativeMode() {
        return false;
    }

    /**
     * 获取显示标题
     */
    public String getDisplayTitle() {
        return "分析仪";
    }
}