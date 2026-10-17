package com.pha.trainees.chemistry.container;

import java.util.EnumMap;
import java.util.Map;

/**
 * 腐蚀敏感度表（§19.32）：<b>材质 × 侵蚀类型 → 0~1</b>，以及"衬层被吃光后降级到哪"。
 *
 * <p><b>这些数字是"定性的真实材料学知识"落到动力学系数上，不是编造的热力学数值</b>——
 * 与反应规则里的 {@code activationEnergy}/{@code preExponentialFactor} 同一层级（可调、可标定）：</p>
 * <ul>
 *   <li><b>玻璃/石英</b>：耐几乎所有酸（0.01~0.02），<b>唯独怕 HF</b>（本表把 HF 归到 HALIDE，将是最高档）；
 *       怕<b>热碱</b>（0.15，SiO₂ + 2NaOH → Na₂SiO₃ + H₂O）；怕熔盐。</li>
 *   <li><b>金属（钢）</b>：怕酸（1.00，Fe + 2H⁺ → Fe²⁺ + H₂↑）；碱里反而钝化（0.05）；
 *       <b>怕 Cl⁻ 点蚀</b>（0.30）。</li>
 *   <li><b>陶瓷（Al₂O₃）</b>：两头都中等，没有明显短板也没有明显强项。</li>
 *   <li><b>搪玻璃</b>：衬层按玻璃算；<b>一旦吃光就暴露钢基体</b>（见 {@link #degradedMaterial}）。</li>
 * </ul>
 *
 * <p><b>标定目标</b>（写在这里，便于以后调系数时不跑偏）：打开腐蚀后，
 * 消毒液工况（稀碱 ~0.5 mol/L、≈335 K）跑一批只应吃掉<b>几个百分点</b>；
 * 而浓碱沸腾、Cl⁻ + 不锈钢、HF + 玻璃应当在合理时间内真的毁掉容器。</p>
 */
public final class CorrosionTable {

    private CorrosionTable() {}

    /** 顺序与 {@link ErosionType} 的声明序一致：ACID, BASE, HALIDE, OXIDIZER, MOLTEN_SALT */
    private static final Map<ContainerMaterial, double[]> TABLE = new EnumMap<>(ContainerMaterial.class);

    static {
        TABLE.put(ContainerMaterial.GLASS, new double[]{0.02, 0.15, 0.00, 0.05, 0.30});
        TABLE.put(ContainerMaterial.QUARTZ, new double[]{0.01, 0.12, 0.00, 0.04, 0.25});
        TABLE.put(ContainerMaterial.METAL, new double[]{1.00, 0.05, 0.30, 0.40, 0.50});
        TABLE.put(ContainerMaterial.CERAMIC, new double[]{0.20, 0.20, 0.05, 0.10, 0.40});
        TABLE.put(ContainerMaterial.GLASS_LINED, new double[]{0.02, 0.15, 0.00, 0.05, 0.30});
    }

    /** 某材质对某侵蚀类型的敏感度（0 = 基本不侵蚀，1 = 强侵蚀）；未知组合返回 0。 */
    public static double susceptibility(ContainerMaterial material, ErosionType type) {
        if (material == null || type == null) return 0.0;
        double[] row = TABLE.get(material);
        return row == null ? 0.0 : row[type.ordinal()];
    }

    /**
     * 衬层被腐蚀透之后降级到哪种材质：<b>搪玻璃 → 钢基体</b>（真实失效模式），
     * 其余材质返回 null（没有"内层"可降级，直接破裂）。
     */
    public static ContainerMaterial degradedMaterial(ContainerMaterial material) {
        return material == ContainerMaterial.GLASS_LINED ? ContainerMaterial.METAL : null;
    }
}
