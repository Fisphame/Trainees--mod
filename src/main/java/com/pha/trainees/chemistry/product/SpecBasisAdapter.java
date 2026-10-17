package com.pha.trainees.chemistry.product;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/**
 * 判定口径适配器（§19.18 / §19.18.2）：把**容器内容物**折算成**该产品规格所要求的测量口径**。
 *
 * <p>为什么需要它：国标测的是**有效氯**，而不是"次氯酸根质量分数"。1 mol ClO⁻ 与 1 mol Cl₂
 * 都是 2 电子氧化剂，所以 1 mol ClO⁻ ≡ 1 mol Cl₂（70.90 g）——折算就是在适配层把一个
 * "折算物种"挂上去，判定层（{@code SpecMatcher}）保持纯粹、键空间契约不变。</p>
 *
 * <p>多数产品不需要折算（{@link #identity()}：直接按真实物种判定）。</p>
 */
public interface SpecBasisAdapter {

    /** 把容器成分折算成判定口径的成分表（物种 id → 摩尔数）。 */
    Map<String, Double> toSpecBasis(Map<String, Double> containerContents);

    /** 判定口径下的摩尔质量查询：折算物种用当量摩尔质量，其余交给原查询。 */
    ToDoubleFunction<String> molarMassOf(ToDoubleFunction<String> ionMolarMass);

    // ==================== 直接按物种判定 ====================

    static SpecBasisAdapter identity() {
        return new SpecBasisAdapter() {
            @Override
            public Map<String, Double> toSpecBasis(Map<String, Double> containerContents) {
                return new LinkedHashMap<>(containerContents);
            }

            @Override
            public ToDoubleFunction<String> molarMassOf(ToDoubleFunction<String> ionMolarMass) {
                return ionMolarMass;
            }
        };
    }

    // ==================== 有效氯折算 ====================

    /** 折算物种 id（不是真实注册的离子，只存在于判定口径里） */
    String AVAILABLE_CHLORINE_ID = "trainees:available_chlorine";
    /** Cl₂ 摩尔质量 (g/mol)：有效氯的当量基准 */
    double EQUIVALENT_MOLAR_MASS = 70.90;
    /** 次氯酸根真实物种 id */
    String HYPOCHLORITE_ID = "trainees:clo_minus";
    /** 次氯酸根摩尔质量 (g/mol)，用于文档/图鉴里的换算说明 */
    double HYPOCHLORITE_MOLAR_MASS = 51.45;

    /**
     * 有效氯口径：把 {@code clo_minus} 的摩尔数原样挂到 {@code available_chlorine} 上，
     * 摩尔质量取 70.90 → 于是"有效氯质量分数"= n(ClO⁻)×70.90 / 总质量，与国标口径一致。
     * 其余物种（游离氯、游离酸、氯酸盐、水…）原样保留，限值规则照常生效。
     */
    static SpecBasisAdapter availableChlorine() {
        return new SpecBasisAdapter() {
            @Override
            public Map<String, Double> toSpecBasis(Map<String, Double> containerContents) {
                Map<String, Double> basis = new LinkedHashMap<>(containerContents);
                Double hypochlorite = basis.remove(HYPOCHLORITE_ID);
                if (hypochlorite != null && hypochlorite > 0) {
                    basis.merge(AVAILABLE_CHLORINE_ID, hypochlorite, Double::sum);
                }
                return basis;
            }

            @Override
            public ToDoubleFunction<String> molarMassOf(ToDoubleFunction<String> ionMolarMass) {
                return id -> AVAILABLE_CHLORINE_ID.equals(id)
                        ? EQUIVALENT_MOLAR_MASS
                        : ionMolarMass.applyAsDouble(id);
            }
        };
    }

    /** 由 ClO⁻ 的质量分数换算有效氯质量分数（图鉴/文档/测试用）。 */
    static double availableChlorineFraction(double hypochloriteFraction) {
        return hypochloriteFraction * EQUIVALENT_MOLAR_MASS / HYPOCHLORITE_MOLAR_MASS;
    }
}
