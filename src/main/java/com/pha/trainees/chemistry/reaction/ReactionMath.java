package com.pha.trainees.chemistry.reaction;

import java.util.Map;

/**
 * 化学引擎的纯数学层（§19.8 单元测试策略）。
 * 只含标准数学计算，**不依赖任何 Minecraft/Forge/配置类**，可在纯 JUnit 中测试。
 * 引擎（ReactionRule/ReactionEngine）内部的计算委托到这里，保证"核心数学有测试保护"。
 */
public final class ReactionMath {

    /** 气体常数 R（J/(mol·K)） */
    public static final double R = 8.314;
    /** 标准参考温度（K） */
    public static final double T0 = 298.0;

    private ReactionMath() {}

    /**
     * 阿伦尼乌斯速率常数：k = A × exp(-Ea / (R × T))
     * @param activationEnergyKj 活化能（kJ/mol，与代码中激活能单位一致）
     * @param preExponentialFactor 指前因子 A
     * @param temperatureKelvin 温度（K）
     * @return 速率常数 k；T ≤ 0 返回 0
     */
    public static double rateConstant(double activationEnergyKj, double preExponentialFactor, double temperatureKelvin) {
        if (temperatureKelvin <= 0) return 0;
        return preExponentialFactor * Math.exp(-activationEnergyKj * 1000 / (R * temperatureKelvin));
    }

    /**
     * 物理平衡常数：K = exp(-ΔG(T)/(R·T))，ΔG(T) = ΔH - T·ΔS，
     * ΔS 由 298K 数据反推：ΔS = (ΔH - ΔG°) / 298（假设 ΔH、ΔS 近似恒定）。
     * 强放热反应高温 K 依然巨大（燃烧 1300K 仍 CO₂ 占优），避免手设 K° 高温塌缩。
     * @param deltaHkJ ΔH（kJ/mol）
     * @param deltaGkJ ΔG°（kJ/mol）
     * @param temperatureKelvin 温度（K）
     * @return 平衡常数 K；T ≤ 0 返回 0
     */
    public static double equilibriumConstantPhysical(double deltaHkJ, double deltaGkJ, double temperatureKelvin) {
        if (temperatureKelvin <= 0) return 0;
        double deltaS = (deltaHkJ - deltaGkJ) * 1000.0 / T0;           // J/(mol·K)
        double deltaGT = deltaHkJ * 1000.0 - temperatureKelvin * deltaS; // J/mol
        return Math.exp(-deltaGT / (R * temperatureKelvin));
    }

    /**
     * 范特霍夫平衡常数（显式覆盖）：K = K° × exp(-ΔH/R × (1/T - 1/298))
     * 供 N₂O₄ / SO₂-SO₃ 接触法 / 哈伯等可逆玩法保持手调平衡点。
     * @param equilibriumConstantAt298 K°（298K 时的平衡常数）
     * @param deltaHkJ ΔH（kJ/mol）
     * @param temperatureKelvin 温度（K）
     * @return 平衡常数 K；T ≤ 0 返回 0
     */
    public static double equilibriumConstantVanHoff(double equilibriumConstantAt298, double deltaHkJ, double temperatureKelvin) {
        if (temperatureKelvin <= 0) return 0;
        double exponent = -deltaHkJ * 1000 / R * (1.0 / temperatureKelvin - 1.0 / T0);
        return equilibriumConstantAt298 * Math.exp(exponent);
    }

    /**
     * 电解有效平衡常数：ΔG_eff = ΔG - W（电功等效于把平衡推向产物）
     * @param deltaGkJ ΔG（kJ/mol）
     * @param electricalWorkKj 电功 W（kJ/mol）
     * @param temperatureKelvin 温度（K）
     * @return K = exp(-(ΔG - W)/(R·T))；T ≤ 0 返回 0
     */
    public static double equilibriumConstantElectrolysis(double deltaGkJ, double electricalWorkKj, double temperatureKelvin) {
        if (temperatureKelvin <= 0) return 0;
        return Math.exp(-(deltaGkJ - electricalWorkKj) * 1000 / (R * temperatureKelvin));
    }

    /**
     * 反应商 Q = ∏[产物]^ν / ∏[反应物]^ν
     * @param productConc 产物 → (浓度, 计量数)
     * @param reactantConc 反应物 → (浓度, 计量数)
     * @return Q；反应物浓度乘积为 0 时返回 Double.MAX_VALUE（表示不可能正向）
     */
    public static double reactionQuotient(Map<Double, Integer> productConc, Map<Double, Integer> reactantConc) {
        double numerator = 1.0;
        for (Map.Entry<Double, Integer> e : productConc.entrySet()) {
            numerator *= Math.pow(e.getKey(), e.getValue());
        }
        double denominator = 1.0;
        for (Map.Entry<Double, Integer> e : reactantConc.entrySet()) {
            denominator *= Math.pow(e.getKey(), e.getValue());
        }
        if (denominator < 1e-12) return Double.MAX_VALUE;
        return numerator / denominator;
    }

    /**
     * 净速率：v_net = v_f × (1 - Q/K)
     * @return 净速率；Q ≥ K 时返回 ≤ 0（已达平衡或逆向）
     */
    public static double netRate(double forwardRate, double quotient, double equilibriumConstant) {
        if (equilibriumConstant <= 0) return 0;
        return forwardRate * (1.0 - quotient / equilibriumConstant);
    }

    /**
     * 本 Tick 反应进度：Δξ = v_net × Δt（默认 Δt = 1 tick = 0.05 s）
     */
    public static double deltaXi(double netRate, double deltaTimeSeconds) {
        return netRate * deltaTimeSeconds;
    }

    /**
     * 克劳修斯–克拉珀龙：把"参考压强下的转变温度"移动到目标压强（§19.27 Stage 2）。
     *
     * <p>{@code ln(P/P_ref) = −ΔH/R · (1/T − 1/T_ref)}，解出
     * {@code 1/T(P) = 1/T_ref − R·ln(P/P_ref)/ΔH}。升压 → 转变温度升高（沸点上升）；
     * 降压 → 转变温度降低（真空蒸馏）。ΔH 传 kJ/mol、R 用 J/(mol·K)，故内部 ×1000。</p>
     *
     * <p><b>只适用于凝聚相 ↔ 气相</b>：那里 ΔV ≈ 气相摩尔体积，理想气体近似很准。
     * 固 ↔ 液需要摩尔体积差 ΔV（本表没有该数据），而且水的熔化线斜率是<b>负</b>的（ΔV&lt;0），
     * 硬套本式会得到相反的符号——所以熔点/凝固点不做压强修正。</p>
     *
     * @param referenceTemperatureK 参考压强下的转变温度 T_ref（K）
     * @param referencePressureAtm  参考压强 P_ref（atm）
     * @param latentHeatKjPerMol    转变焓 ΔH（kJ/mol，>0 才有物理意义）
     * @param pressureAtm           目标压强 P（atm）
     * @return 该压强下的转变温度；参数非法返回 T_ref；该压强下"无论多热都不转变"时返回 +∞
     */
    public static double clausiusClapeyronTemperature(double referenceTemperatureK,
                                                      double referencePressureAtm,
                                                      double latentHeatKjPerMol,
                                                      double pressureAtm) {
        if (referenceTemperatureK <= 0 || referencePressureAtm <= 0
                || latentHeatKjPerMol <= 1e-9 || pressureAtm <= 0) {
            return referenceTemperatureK;
        }
        double inverseT = 1.0 / referenceTemperatureK
                - R * Math.log(pressureAtm / referencePressureAtm) / (latentHeatKjPerMol * 1000.0);
        if (inverseT <= 1e-12) return Double.POSITIVE_INFINITY;
        return 1.0 / inverseT;
    }
}
