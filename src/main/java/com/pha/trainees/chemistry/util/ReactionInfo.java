package com.pha.trainees.chemistry.util;

import com.pha.trainees.chemistry.particle.IonType;
import com.pha.trainees.chemistry.reaction.ReactionMath;
import com.pha.trainees.chemistry.reaction.ReactionRule;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 反应的条件 / 参考文本（§19.16 条件区定稿 + §19.36 按家族分版），供 JEI 分类与分析仪共用。
 *
 * <p><b>三个家族各有自己的条件区</b>（同样的数据，说人话）：</p>
 * <ul>
 *   <li>{@code CHEMICAL}：最低可进行条件（温度/前置/通电）+ 参考区（ΔH、Ea、K）；</li>
 *   <li>{@code IONIZATION}：门槛只有"必须有水"，所以写一行**介质**，不写"最低温度 ≥ 0 K"这种废话；</li>
 *   <li>{@code PHASE}：换成物理量——**转变窗口**、**潜热**、沸点随压强移动；Ea/K 对相变没有意义，不显示。</li>
 * </ul>
 *
 * <p>只读、纯展示：不读取玩家容器状态（JEI 是静态百科；"你现在够不够"由分析仪回答）。</p>
 */
public final class ReactionInfo {

    private ReactionInfo() {}

    /** 条件区全部文本行（已本地化、含颜色）：按家族分派 */
    public static List<Component> conditionLines(ReactionRule rule) {
        return switch (rule.getFamily()) {
            case PHASE -> phaseLines(rule);
            case IONIZATION -> ionizationLines(rule);
            case CHEMICAL -> chemicalLines(rule);
        };
    }

    /** 化学反应（含电解）：门槛区 + 空行 + 参考区 */
    private static List<Component> chemicalLines(ReactionRule rule) {
        List<Component> lines = new ArrayList<>();

        // ---- 最低可进行条件 ----
        lines.add(Component.translatable("jei.trainees.reaction.cond.header")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        lines.add(Component.translatable("jei.trainees.reaction.cond.min_temp",
                fmt(rule.getMinTemperature(), 0)));
        if (rule.isTemperaturePressureShifted()) {
            // §19.27 Stage 2：JEI 是静态百科，只给 1 atm 参考值；实际门槛随压强移动
            lines.add(Component.translatable("jei.trainees.reaction.cond.min_temp_pressure")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        for (Map.Entry<IonType, Integer> e : rule.getPreconditions().entrySet()) {
            lines.add(Component.translatable("jei.trainees.reaction.cond.precondition",
                    IonDisplay.format(e.getKey().getId().getPath()), String.valueOf(e.getValue())));
        }
        if (rule.getElectricalWorkPerMol() > 0) {
            lines.add(Component.translatable("jei.trainees.reaction.cond.power",
                    fmt(rule.getElectricalWorkPerMol(), 1)));
        }

        // ---- 规划参考（非门槛）：§19.34 起不再打小标题，只用空行隔开 ----
        lines.add(Component.literal(""));
        lines.add(deltaHLine(rule));
        lines.add(Component.translatable("jei.trainees.reaction.cond.ea",
                fmt(rule.getActivationEnergy(), 1)));
        lines.add(Component.translatable("jei.trainees.reaction.cond.k",
                sci(equilibriumAt298(rule))));
        return lines;
    }

    /**
     * 电离（§19.36）：这一族的门槛只有一件事——**必须有水**（作为介质，不消耗）。
     * 所以不写"最低温度 ≥ 0 K"，改写成一行"介质：…"；ΔH/Ea/K 照旧（强电解质的 K 很有信息量）。
     */
    private static List<Component> ionizationLines(ReactionRule rule) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("jei.trainees.ionization.header")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        for (Map.Entry<IonType, Integer> e : rule.getPreconditions().entrySet()) {
            lines.add(Component.translatable("jei.trainees.ionization.medium",
                    IonDisplay.format(e.getKey().getId().getPath()), String.valueOf(e.getValue())));
        }
        lines.add(Component.translatable("jei.trainees.ionization.note")
                .withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.literal(""));
        lines.add(deltaHLine(rule));
        lines.add(Component.translatable("jei.trainees.reaction.cond.ea",
                fmt(rule.getActivationEnergy(), 1)));
        lines.add(Component.translatable("jei.trainees.reaction.cond.k",
                sci(equilibriumAt298(rule))));
        return lines;
    }

    /**
     * 相变（§19.36）：条件区换成物理量。min/max 温度就是相变表给的**转变窗口**，
     * ΔH 就是**潜热**（L_f / L_v），汽化/冷凝还会随压强移动；Ea/K 对相变没有意义，故不显示。
     */
    private static List<Component> phaseLines(ReactionRule rule) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("jei.trainees.phase.header")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        double max = rule.getMaxTemperature();
        lines.add(Component.translatable("jei.trainees.phase.window",
                fmt(rule.getMinTemperature(), 0), Double.isFinite(max) ? fmt(max, 0) : "∞"));
        lines.add(Component.translatable("jei.trainees.phase.latent", fmt(Math.abs(rule.getDeltaH()), 1)));
        if (rule.isTemperaturePressureShifted()) {
            lines.add(Component.translatable("jei.trainees.phase.pressure")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        // §19.38：作者 2026-10-02 要求删掉"物理过程：不是化学反应…"那行尾注——
        // 页面本身就是"相变"，不需要再解释它不是什么。
        return lines;
    }

    /** ΔH 行（放热/吸热）：三个家族共用同一句措辞 */
    private static Component deltaHLine(ReactionRule rule) {
        double deltaH = rule.getDeltaH();
        String thermal = Component.translatable(deltaH < 0
                ? "jei.trainees.reaction.cond.exothermic"
                : "jei.trainees.reaction.cond.endothermic").getString();
        return Component.translatable("jei.trainees.reaction.cond.delta_h",
                fmt(deltaH, 1), thermal);
    }

    /**
     * 298 K 平衡常数（纯数学层计算，不依赖容器）：
     * 电解规则按"理论电功"折算；显式覆盖走范特霍夫；否则物理公式。
     */
    public static double equilibriumAt298(ReactionRule rule) {
        double t = ReactionMath.T0;
        if (rule.getElectricalWorkPerMol() > 0) {
            return ReactionMath.equilibriumConstantElectrolysis(
                    rule.getDeltaG(), rule.getElectricalWorkPerMol(), t);
        }
        if (rule.isEquilibriumOverridden()) {
            return ReactionMath.equilibriumConstantVanHoff(
                    rule.getEquilibriumConstant(), rule.getDeltaH(), t);
        }
        return ReactionMath.equilibriumConstantPhysical(rule.getDeltaH(), rule.getDeltaG(), t);
    }

    private static String fmt(double v, int decimals) {
        return String.format("%." + decimals + "f", v);
    }

    private static String sci(double v) {
        if (!Double.isFinite(v)) return "∞";
        return String.format("%.2e", v);
    }
}
