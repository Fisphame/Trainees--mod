package com.pha.trainees.chemistry.util;

import com.pha.trainees.chemistry.particle.IonType;
import com.pha.trainees.chemistry.reaction.ReactionGraph;
import com.pha.trainees.chemistry.reaction.ReactionRule;

import java.util.Map;

/**
 * 反应方程式渲染器：把 {@link ReactionRule} 表示为可读的化学方程式。
 *
 * <p>例：{@code 2H₂ + O₂ → 2H₂O}（系数省略 1；化学式走 {@link IonDisplay}，含价态上标与物态后缀）。</p>
 *
 * <p>显示规范（作者确认 2026-09-06）：**只显示纯方程式 + 粗体**，ΔH / 最低温度等条件不进方程式，
 * 避免干扰阅读；id 仅在诊断细节里作为次要信息保留。</p>
 *
 * <p>本方法为 mod 内显示（分析仪诊断/预测/失败行、后续任何 UI）与将来的 JEI 分类共用，
 * 不依赖任何客户端类。</p>
 */
public final class ReactionFormula {

    private ReactionFormula() {}

    /** 方程式纯文本：{@code 2H₂ + O₂ → 2H₂O} */
    public static String format(ReactionRule rule) {
        if (rule == null) return "?";
        StringBuilder sb = new StringBuilder();
        appendSide(sb, rule.getReactants());
        sb.append(" → ");
        appendSide(sb, rule.getProducts());
        return sb.toString();
    }

    /** 粗体包装（聊天栏 / GUI 文本行内嵌；§r 收尾避免污染后续文本） */
    public static String bold(String text) {
        return "§l" + text + "§r";
    }

    /** 方程式 + 粗体 */
    public static String formatBold(ReactionRule rule) {
        return bold(format(rule));
    }

    /**
     * 按规则 id 查表后格式化（{@link ReactionGraph#getRuleById}）。
     * 找不到时回退返回 id 本身，保证诊断行不出现空白。
     */
    public static String formatById(String ruleId) {
        if (ruleId == null) return "?";
        ReactionRule rule = ReactionGraph.getInstance().getRuleById(ruleId);
        return rule == null ? ruleId : format(rule);
    }

    private static void appendSide(StringBuilder sb, Map<IonType, Integer> side) {
        if (side == null || side.isEmpty()) {
            sb.append("∅");
            return;
        }
        boolean first = true;
        for (Map.Entry<IonType, Integer> e : side.entrySet()) {
            if (!first) sb.append(" + ");
            first = false;
            int coefficient = e.getValue() == null ? 1 : e.getValue();
            if (coefficient > 1) sb.append(coefficient);
            sb.append(IonDisplay.format(e.getKey().getId().getPath()));
        }
    }
}
