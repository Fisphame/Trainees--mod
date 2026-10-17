package com.pha.trainees.chemistry.product;

import com.pha.trainees.chemistry.spec.FractionRule;
import com.pha.trainees.chemistry.spec.ProductSpec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 日化品规格表（§19.18，④ 打包机的内容层）。
 *
 * <p>V1 只做**含氯消毒液**一个产品作为样板（把 ② 的工艺竞争真正变成规格约束）：
 * 有效氯窗口由"温度 + 碱量 + 通氯终点"决定，游离氯/游离酸/氯酸盐分别对应
 * "通氯终点 / 碱的摩尔比 / 高温歧化"三个可操纵旋钮——这正是 §19.18「规格要有牙齿」。</p>
 *
 * <p>数值为**参考国标指标（简化）**，不使用真实品牌名。质量分数口径 w/w；
 * 有效氯通过 {@link SpecBasisAdapter#availableChlorine()} 折算（1 mol ClO⁻ ≡ 1 mol Cl₂）。</p>
 *
 * <p>TODO（S1 数据驱动）：本表将来由数据包提供，与相变表/一级物质表共用 §19.19.9 的基础设施。</p>
 */
public final class ProductSpecs {

    /**
     * 含氯消毒液（漂白液）：合格 有效氯 5.0~8.0%，优质 6.0~7.5%（更窄子区间）。
     *
     * <p><b>为什么这四条杂质上限留在代码里</b>（而不是数据包）：它们是<b>安全上限</b>
     * （游离氯 0、游离酸 ≤0.05%、游离碱 ≤1%、氯酸盐 ≤1%）——**限制**产品、不是制备配方，
     * 公开无风险且具教育意义；而把它挪进数据会牵动一条已标定的判定链（测试直接用本常量）。
     * 数据驱动的分析见 {@code scripts/make-public-snapshot.sh} 的白名单注释。</p>
     */
    public static final ProductSpec BLEACH = new ProductSpec(
            "trainees:bleach",
            List.of(new FractionRule(SpecBasisAdapter.AVAILABLE_CHLORINE_ID, 0.050, 0.080)),
            List.of(
                    new FractionRule("trainees:cl2", 0.0, 0.0),          // 禁用游离氯（放毒/升压）
                    new FractionRule("trainees:h_plus", 0.0, 0.0005),    // 游离酸上限 0.05%（放 Cl₂）
                    new FractionRule("trainees:oh_minus", 0.0, 0.010),   // 游离碱上限 1%（腐蚀、且放 Cl₂ 慢）
                    new FractionRule("trainees:clo3_minus", 0.0, 0.010)  // 氯酸盐杂质上限 1%（高温歧化）
            ),
            1.0,                                        // 每瓶消耗 1 mol 溶液（§19.18 的 mol 份定义）
            "trainees:bleach_good",
            "trainees:bleach_premium",
            List.of(new FractionRule(SpecBasisAdapter.AVAILABLE_CHLORINE_ID, 0.060, 0.075)),
            "bleach",                                   // behavior key（V1 只记录，机制后续挂接）
            ""                                          // requiredNode（科技树节点，⑤ 再定义）
    );

    public static final List<ProductSpec> ALL = List.of(BLEACH);

    private static final Map<String, ProductSpec> BY_ID = buildIndex();
    private static final Map<String, SpecBasisAdapter> ADAPTERS = buildAdapters();

    private ProductSpecs() {
    }

    /** 按产品 id 查规格（支持 {@code path} 与 {@code namespace:path} 两种写法），找不到返回 null。 */
    public static ProductSpec byId(String productId) {
        if (productId == null) return null;
        ProductSpec spec = BY_ID.get(productId);
        if (spec != null) return spec;
        int colon = productId.indexOf(':');
        return colon >= 0 ? BY_ID.get(productId.substring(colon + 1)) : null;
    }

    /** 该产品判定时使用的口径适配器（默认按真实物种判定；含氯消毒液走有效氯折算）。 */
    public static SpecBasisAdapter adapterFor(String productId) {
        ProductSpec spec = byId(productId);
        if (spec == null) return SpecBasisAdapter.identity();
        return ADAPTERS.getOrDefault(spec.id(), SpecBasisAdapter.identity());
    }

    /** 产品 id 的短名（用于展示/调试）。 */
    public static String shortId(ProductSpec spec) {
        String id = spec.id();
        int colon = id.indexOf(':');
        return colon >= 0 ? id.substring(colon + 1) : id;
    }

    private static Map<String, ProductSpec> buildIndex() {
        Map<String, ProductSpec> index = new LinkedHashMap<>();
        for (ProductSpec spec : ALL) {
            index.put(spec.id(), spec);
            index.put(shortId(spec), spec);
        }
        return Map.copyOf(index);
    }

    private static Map<String, SpecBasisAdapter> buildAdapters() {
        Map<String, SpecBasisAdapter> adapters = new LinkedHashMap<>();
        adapters.put(BLEACH.id(), SpecBasisAdapter.availableChlorine());
        return Map.copyOf(adapters);
    }
}
