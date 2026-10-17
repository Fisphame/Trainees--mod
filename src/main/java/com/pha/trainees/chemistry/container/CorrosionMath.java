package com.pha.trainees.chemistry.container;

/**
 * 腐蚀的纯数学层（§19.32）：<b>为什么走动力学而不是 ΔG</b>——
 * 热力学上大多数金属在水里都"应该"腐蚀（Fe + 2H⁺ → Fe²⁺ + H₂ 的 ΔG 是负的），
 * 现实中它们不烂是因为<b>钝化膜</b>。所以腐蚀的主导因素是动力学，
 * 用 ΔG 判"该不该腐蚀"会得出"水溶解所有金属"的荒谬结论。
 *
 * <p>因此本层与反应动力学同一语义：一个可标定的速率 + 温度加速，
 * <b>不引入任何新的 ΔH/ΔG</b>。</p>
 *
 * <p>纯函数、无 Minecraft 依赖，由 {@code CorrosionMathTest} 锁定。</p>
 */
public final class CorrosionMath {

    private CorrosionMath() {}

    /** 参考温度（K）：室温 */
    public static final double REFERENCE_TEMP_K = 293.15;
    /** 温度加速：每升高这么多开尔文速率翻倍（阿伦尼乌斯的通俗版） */
    public static final double TEMP_DOUBLING_K = 20.0;

    /** 温度加速因子：293 K → 1.0；373 K → 2^4 = 16；473 K → 2^9 = 512。 */
    public static double temperatureFactor(double tempK) {
        if (tempK <= 0) return 0;
        return Math.pow(2.0, (tempK - REFERENCE_TEMP_K) / TEMP_DOUBLING_K);
    }

    /**
     * 单个物种对某材质的侵蚀载荷：浓度(mol/L) × 该材质对该侵蚀类型的敏感度。
     * 调用方把容器内所有物种的载荷相加得到总载荷。
     */
    public static double load(ContainerMaterial material, ErosionType type, double concentrationMolPerL) {
        if (concentrationMolPerL <= 0) return 0;
        return concentrationMolPerL * CorrosionTable.susceptibility(material, type);
    }

    /**
     * 每秒腐蚀度增量（0~1 的无量纲分数）。
     *
     * @param totalLoad Σ(浓度 × 敏感度)，由 {@link #load} 逐物种逐类型累加
     * @param baseRatePerSecond 基础速率（配置；即在"1 mol/L、293 K、敏感度 1"下的每秒腐蚀比例）
     * @param tempK 容器当前温度
     */
    public static double ratePerSecond(double totalLoad, double baseRatePerSecond, double tempK) {
        if (totalLoad <= 0 || baseRatePerSecond <= 0) return 0;
        return totalLoad * baseRatePerSecond * temperatureFactor(tempK);
    }
}
