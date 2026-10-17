package com.pha.trainees.chemistry.item;

import com.pha.trainees.chemistry.blockentity.BeakerBlockEntity;
import com.pha.trainees.chemistry.particle.IonType;
import com.pha.trainees.chemistry.util.IonDisplay;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 普通分析仪
 * 显示简化信息：温度、主要成分（前3种）
 */
public class AnalyzerItemNormal extends AnalyzerItem {

    private static final DecimalFormat DF = new DecimalFormat("#0.00");

    public AnalyzerItemNormal(Properties properties) {
        super(properties);
    }

    @Override
    public List<Component> getAnalysisResult(BeakerBlockEntity beaker) {
        List<Component> lines = new ArrayList<>();

        // 标题行
        lines.add(Component.translatable("gui.trainees.analyzer.title")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        // 温度（§19.34：颜色写在语言键里——标签黄、数值白，别整行染色）
        double tempK = beaker.getTemperature();
        double tempC = tempK - 273.15;
        lines.add(Component.translatable("gui.trainees.analyzer.temperature",
                DF.format(tempK), DF.format(tempC)));

        // 体积
        lines.add(Component.translatable("gui.trainees.analyzer.volume",
                DF.format(beaker.getVolume())));

        // 内容物（主要成分，取前3种）
        Map<IonType, Double> contents = beaker.getContents();
        if (contents.isEmpty()) {
            lines.add(Component.translatable("gui.trainees.analyzer.contents")
                    .append(Component.translatable("gui.trainees.analyzer.contents_empty")
                            .withStyle(ChatFormatting.GRAY)));
        } else {
            lines.add(Component.translatable("gui.trainees.analyzer.contents"));
            // 按物质的量从大到小排序，取前3
            List<Map.Entry<IonType, Double>> sorted = contents.entrySet().stream()
                    .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                    .limit(3)
                    .toList();
            for (Map.Entry<IonType, Double> entry : sorted) {
                IonType ion = entry.getKey();
                double moles = entry.getValue();
                String ionName = IonDisplay.format(ion.getId().getPath());
                lines.add(Component.literal("  §e" + ionName + ": §f" + DF.format(moles) + " mol"));
            }
            if (contents.size() > 3) {
                lines.add(Component.translatable("gui.trainees.analyzer.contents_more",
                        contents.size() - 3).withStyle(ChatFormatting.GRAY));
            }
        }

        // 压力（如果有气体）
        double pressure = beaker.getPressure();
        if (pressure > 0.01) {
            lines.add(Component.translatable("gui.trainees.analyzer.pressure",
                    DF.format(pressure)));
        }

        return lines;
    }

    @Override
    public String getDisplayTitle() {
        return "分析仪";
    }
}