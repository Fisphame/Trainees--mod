package com.pha.trainees.chemistry.spec;

import java.util.List;

/**
 * 规格判定结果。
 *
 * @param verdict        最终结论（只有 PASS / PREMIUM 会出货，见 {@link #acceptable()}）
 * @param details        逐条规则结果，顺序固定为 required → limits → premium（GUI 可按序直接渲染）；
 *                       注意同一物种可能同时出现在 required 与 premium 两条里，统计时勿重复计入
 * @param totalMoles     容器内<b>全部</b>物种的摩尔数合计（不只是被规格声明的部分）
 * @param totalMass      容器内<b>全部</b>物种的质量合计（g），即质量分数的分母
 * @param othersFraction 未被任何规则声明的物种质量占比（水/杂质），不影响 verdict
 */
public record SpecResult(Verdict verdict, List<RuleResult> details,
                         double totalMoles, double totalMass, double othersFraction) {

    /** 出货判定：只有 PASS / PREMIUM 会产出成品。 */
    public boolean acceptable() {
        return verdict == Verdict.PASS || verdict == Verdict.PREMIUM;
    }

    public record RuleResult(String ionId, double actualFraction,
                             double minFraction, double maxFraction, Status status, Kind kind) {
        public enum Status { OK, BELOW_MIN, ABOVE_MAX, PRESENT_FORBIDDEN, UNKNOWN_MASS }

        /** 规则所属清单：required（有效成分窗口）/ limits（限值上限）/ premium（优质档窗口）。 */
        public enum Kind { REQUIRED, LIMIT, PREMIUM }
    }
}
