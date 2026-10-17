package com.pha.trainees.chemistry.product;

import com.pha.trainees.chemistry.spec.SpecResult;

/**
 * 一次灌装判定的结果（④ 打包机）。
 *
 * @param result        规格判定结果（含逐条明细，供诊断面板复用）
 * @param productItemId 产出的物品 id；空串 = 不产出（量不足，或自动模式下不合格）
 * @param defect        次品类别（合格品为 {@link DefectKind#NONE}）
 * @param reasonKey     展示用语言键（次品/拒收原因），合格品为空串
 */
public record PackOutcome(SpecResult result, String productItemId, DefectKind defect, String reasonKey) {

    /**
     * 次品类别：**按失败类别而非按产品**共用少量 item（§19.18）——
     * 因为反噬取决于违规种类（游离氯放毒、碱烧伤、酸自毁放气），与"它是哪种产品"无关。
     */
    public enum DefectKind {
        /** 无缺陷（合格/优质，或量不足而根本没判定） */
        NONE,
        /** 游离氯超标 → 使用时放毒 */
        CHLORINE,
        /** 碱超标 → 烧伤 */
        ALKALI,
        /** 酸超标 → 自毁放气 */
        ACID,
        /** 氯酸盐超标 → 氧化性杂质，误服中毒（高温歧化的副产物） */
        CHLORATE,
        /** 有效成分不在窗口内（浓度不合格，**非危险品** → 任何模式都不灌装） */
        OFF_SPEC
    }

    /** 是否真的产出物品。 */
    public boolean producesItem() {
        return productItemId != null && !productItemId.isEmpty();
    }

    public boolean isPremium() {
        return result != null && result.verdict() == com.pha.trainees.chemistry.spec.Verdict.PREMIUM;
    }

    /** 次品（产出了东西但判定不合格）。 */
    public boolean isDefective() {
        return producesItem() && defect != DefectKind.NONE;
    }

    /** 次品 item id（按失败类别共用）；无缺陷时返回空串。 */
    public String defectiveItemId() {
        return defect == DefectKind.NONE ? "" : "trainees:defective_" + defect.name().toLowerCase(java.util.Locale.ROOT);
    }

    /** 展示用语言键（次品原因）。 */
    public String defectLangKey() {
        return defect == DefectKind.NONE ? "" : "gui.trainees.packer.defect." + defect.name().toLowerCase(java.util.Locale.ROOT);
    }
}
