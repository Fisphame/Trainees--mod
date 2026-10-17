package com.pha.trainees.chemistry.product;

import com.pha.trainees.chemistry.spec.ProductSpec;
import com.pha.trainees.chemistry.spec.SpecMatcher;
import com.pha.trainees.chemistry.spec.SpecResult;
import com.pha.trainees.chemistry.spec.Verdict;

import java.util.Map;
import java.util.function.ToDoubleFunction;

/**
 * 打包机判定服务（④ 纯逻辑层，零 MC 依赖、可单测）。
 *
 * <p>把「容器里的溶液 + 目标产品的规格」变成一个明确的出货决策：</p>
 * <ul>
 *   <li><b>自动模式</b>（{@link #evaluateAuto}）：不合格**不产出**（也不扣料），只把诊断交给调用方；</li>
 *   <li><b>强制灌装</b>（{@link #evaluateForced}）：不合格也产**次品**，按失败类别分类
 *       （游离氯/碱/酸/浓度不符），使用时反噬——质检因此从"门槛"升级为"安全机制"（§19.18）。</li>
 * </ul>
 *
 * <p>量不足（{@code NOT_ENOUGH}）在任何模式下都**不产出**：灌不满瓶，谈不上合格品或次品。</p>
 */
public final class PackerService {

    private PackerService() {
    }

    /** 自动模式：合格/优质出货，不合格不产出。 */
    public static PackOutcome evaluateAuto(ProductSpec spec,
                                           SpecBasisAdapter adapter,
                                           Map<String, Double> containerContents,
                                           ToDoubleFunction<String> ionMolarMass,
                                           double usableMoles) {
        return evaluate(spec, adapter, containerContents, ionMolarMass, usableMoles, false);
    }

    /** 强制灌装：不合格也产次品（按失败类别），调用方负责照常扣料。 */
    public static PackOutcome evaluateForced(ProductSpec spec,
                                             SpecBasisAdapter adapter,
                                             Map<String, Double> containerContents,
                                             ToDoubleFunction<String> ionMolarMass,
                                             double usableMoles) {
        return evaluate(spec, adapter, containerContents, ionMolarMass, usableMoles, true);
    }

    /** 便捷入口：按产品 id 自动选取口径适配器（含氯消毒液走有效氯折算）。 */
    public static PackOutcome evaluateAuto(String productId,
                                           Map<String, Double> containerContents,
                                           ToDoubleFunction<String> ionMolarMass,
                                           double usableMoles) {
        ProductSpec spec = ProductSpecs.byId(productId);
        if (spec == null) throw new IllegalArgumentException("未找到产品规格: " + productId);
        return evaluateAuto(spec, ProductSpecs.adapterFor(productId), containerContents, ionMolarMass, usableMoles);
    }

    private static PackOutcome evaluate(ProductSpec spec,
                                        SpecBasisAdapter adapter,
                                        Map<String, Double> containerContents,
                                        ToDoubleFunction<String> ionMolarMass,
                                        double usableMoles,
                                        boolean forced) {
        if (spec == null) throw new IllegalArgumentException("spec 不能为 null");
        if (adapter == null) throw new IllegalArgumentException("adapter 不能为 null");
        if (containerContents == null) throw new IllegalArgumentException("containerContents 不能为 null");

        Map<String, Double> basis = adapter.toSpecBasis(containerContents);
        SpecResult result = SpecMatcher.evaluate(spec, basis, adapter.molarMassOf(ionMolarMass), usableMoles);

        if (result.verdict() == Verdict.PREMIUM) {
            return new PackOutcome(result, spec.premiumItem(), PackOutcome.DefectKind.NONE, "");
        }
        if (result.verdict() == Verdict.PASS) {
            return new PackOutcome(result, spec.goodItem(), PackOutcome.DefectKind.NONE, "");
        }
        if (result.verdict() == Verdict.NOT_ENOUGH) {
            // 灌不满瓶：任何模式都不产出
            return new PackOutcome(result, "", PackOutcome.DefectKind.NONE,
                    "gui.trainees.packer.not_enough");
        }

        // REJECTED（含 UNKNOWN_MASS 的失败关闭）：按失败类别归因
        PackOutcome.DefectKind defect = classify(result);
        // 强制灌装**只为危险失败**产出次品（§19.18 决策）：
        // 游离氯/碱/酸会反噬（放毒/烧伤/自毁），而"浓度不符"既不危险也没有教育价值——
        // 产出来只是垃圾，还会把"次品"这个语义从"危险品"污染成"不达标商品"。因此任何模式都不灌装。
        if (!forced || !isHazardous(defect)) {
            return new PackOutcome(result, "", defect,
                    isHazardous(defect) ? defectLangKey(defect) : "message.trainees.packer.off_spec_no_fill");
        }
        String itemId = "trainees:defective_" + defect.name().toLowerCase(java.util.Locale.ROOT);
        return new PackOutcome(result, itemId, defect, defectLangKey(defect));
    }

    /** 危险失败类别：会产出次品并在使用时反噬；其余（浓度不符）一律不灌装。 */
    private static boolean isHazardous(PackOutcome.DefectKind defect) {
        return defect == PackOutcome.DefectKind.CHLORINE
                || defect == PackOutcome.DefectKind.ALKALI
                || defect == PackOutcome.DefectKind.ACID
                || defect == PackOutcome.DefectKind.CHLORATE;
    }

    /**
     * 失败归因：**危险项优先**（游离氯 &gt; 碱/酸 &gt; 浓度不符），与扫描顺序无关。
     * 因为玩家最需要先知道"这瓶会放毒/烧伤"，而不是"浓度低了 0.3%"。
     */
    private static PackOutcome.DefectKind classify(SpecResult result) {
        PackOutcome.DefectKind fallback = PackOutcome.DefectKind.NONE;
        for (SpecResult.RuleResult detail : result.details()) {
            if (detail.kind() == SpecResult.RuleResult.Kind.PREMIUM) continue;
            if (detail.status() == SpecResult.RuleResult.Status.OK) continue;
            PackOutcome.DefectKind hazard = hazardFor(detail.ionId());
            if (hazard != PackOutcome.DefectKind.OFF_SPEC) return hazard;
            fallback = PackOutcome.DefectKind.OFF_SPEC;
        }
        return fallback;
    }

    /** 危险杂质映射：**从数据包读**（chemistry/hazard_kinds.json），缺失则优雅降级为"不合格"。 */
    private static PackOutcome.DefectKind hazardFor(String ionId) {
        if (ionId == null) return PackOutcome.DefectKind.OFF_SPEC;
        String kind = com.pha.trainees.chemistry.data.ChemDataRegistry.hazardKind(ionId);
        if (kind == null) return PackOutcome.DefectKind.OFF_SPEC;
        try {
            return PackOutcome.DefectKind.valueOf(kind);
        } catch (IllegalArgumentException e) {
            return PackOutcome.DefectKind.OFF_SPEC;   // 未知类型名 → 保守降级
        }
    }

    private static String defectLangKey(PackOutcome.DefectKind defect) {
        return "gui.trainees.packer.defect." + defect.name().toLowerCase(java.util.Locale.ROOT);
    }
}
