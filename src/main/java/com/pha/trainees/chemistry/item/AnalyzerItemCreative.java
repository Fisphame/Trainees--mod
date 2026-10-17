package com.pha.trainees.chemistry.item;

import com.pha.trainees.chemistry.blockentity.BeakerBlockEntity;
import com.pha.trainees.chemistry.particle.IonType;
import com.pha.trainees.config.ChemConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 创造模式分析仪
 * 显示完整调试信息：所有离子列表、温度、体积、压力、热容等
 */
public class AnalyzerItemCreative extends AnalyzerItem {

    private static final DecimalFormat DF = new DecimalFormat("#0.0000");
    private static final DecimalFormat DF_SIMPLE = new DecimalFormat("#0.00");

    public AnalyzerItemCreative(Properties properties) {
        super(properties);
    }

    @Override
    public List<Component> getAnalysisResult(BeakerBlockEntity beaker) {
        List<Component> lines = new ArrayList<>();

        // ====== 标题 ======
        lines.add(Component.translatable("gui.trainees.analyzer.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        // ====== 物理状态 ======
        double tempK = beaker.getTemperature();
        double tempC = tempK - 273.15;
        lines.add(Component.translatable("gui.trainees.analyzer.temperature", DF.format(tempK), DF.format(tempC)));
        lines.add(Component.translatable("gui.trainees.analyzer.volume" , DF_SIMPLE.format(beaker.getVolume())));
        lines.add(Component.translatable("gui.trainees.analyzer.heat_capacity" , DF_SIMPLE.format(beaker.getTotalHeatCapacity())));

        double pressure = beaker.getPressure();
        lines.add(Component.translatable("gui.trainees.analyzer.pressure",DF_SIMPLE.format(pressure)));

        // ====== 内容物（全部列出） ======
        Map<IonType, Double> contents = beaker.getContents();
        if (contents.isEmpty()) {
            lines.add(Component.translatable("gui.trainees.analyzer.contents_empty").withStyle(ChatFormatting.GRAY));
        } else {
            lines.add(Component.translatable("gui.trainees.analyzer.contents").withStyle(ChatFormatting.GRAY));
            // 按物质的量从大到小排序
            contents.entrySet().stream()
                    .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                    .forEach(entry -> {
                        IonType ion = entry.getKey();
                        double moles = entry.getValue();
                        String ionName = ion.getId().getPath();
                        String phase = ion.getPhase().name();
                        String tags = ion.getTags().toString();
                        lines.add(Component.literal(
                                "  §7" + ionName + "§f: " + DF.format(moles) + " mol  §8[" + phase + "]"
                        ));
                        // 显示标签（如果有）
                        if (!ion.getTags().isEmpty()) {
                            lines.add(Component.literal("    §8tags: " + tags));
                        }
                    });
        }

        // ====== 安全状态 ======
        double maxTemp = ChemConfig.MAX_SAFE_TEMPERATURE.get();
        double ratio = tempK / maxTemp;
        if (ratio > 0.9) {
            lines.add(Component.translatable("gui.trainees.analyzer.warning.critical", DF_SIMPLE.format(ratio * 100)));
        } else if (ratio > 0.7) {
            lines.add(Component.translatable("gui.trainees.analyzer.warning.high",  DF_SIMPLE.format(ratio * 100)));
        }

        // ====== 展示列表（§19.34：说明文字归 JEI，工具提示只留数据） ======
        return lines;
    }

    @Override
    public boolean isCreativeMode() {
        return true;
    }

    @Override
    public String getDisplayTitle() {
        return null;
    }
}