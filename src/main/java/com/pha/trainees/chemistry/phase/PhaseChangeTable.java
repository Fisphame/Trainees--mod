package com.pha.trainees.chemistry.phase;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 相变数据表（**公开快照用的 demo 版**）。
 *
 * <p>本文件是 {@code scripts/public-overrides/PhaseChangeTable.java}，由公开快照脚本覆盖到快照里。
 * 它与真实版的**类名、字段名、方法签名完全一致**（引擎里对 {@code PhaseChangeTable.ICE} 之类的引用
 * 照常编译），但数据是**虚构的 demo 值**，不含任何真实物性或热力学数据。</p>
 *
 * <p>真实内容（水的三相、食盐熔盐等）属于私有内容包。</p>
 */
public final class PhaseChangeTable {

    /** 关联系数 R (kJ/(mol·K)) */
    public static final double R_KJ = 0.008314;
    /** 生成量参考温度 (K) */
    public static final double REFERENCE_TEMPERATURE_K = 298.15;

    // ==================== demo 物质三态 ====================
    public static final PhaseSpecies ICE = new PhaseSpecies("demo_solid", 100.0, -10.0, -9.0);
    public static final PhaseSpecies WATER = new PhaseSpecies("demo_liquid", 200.0, -20.0, -18.0);
    public static final PhaseSpecies STEAM = new PhaseSpecies("demo_gas", 200.0, -30.0, -28.0);

    // ==================== demo 两相（用于演示"固⇄液"）====================
    public static final PhaseSpecies SALT = new PhaseSpecies("demo_salt", 300.0, -40.0, -38.0);
    public static final PhaseSpecies MOLTEN_SALT = new PhaseSpecies("demo_salt_molten", 300.0, -35.0, -34.0);

    public static final PhaseChangeData WATER_SYSTEM =
            new PhaseChangeData("demo_liquid", ICE, WATER, STEAM, 273.15, 373.15, 6.01, 40.65, 1.0);

    public static final PhaseChangeData SALT_SYSTEM =
            new PhaseChangeData("demo_salt", SALT, MOLTEN_SALT, null, 1074.0, 1738.0, 28.0, 170.0, 5.0);

    public static final List<PhaseChangeData> ALL = List.of(WATER_SYSTEM, SALT_SYSTEM);

    private PhaseChangeTable() {
    }

    // ==================== 边生成 ====================

    /** 生成某物质的全部有向相变边（三相 → 6 条；两相 → 2 条）。 */
    public static List<PhaseTransition> transitions(PhaseChangeData data) {
        List<PhaseTransition> edges = new ArrayList<>(6);
        PhaseSpecies solid = data.solid();
        PhaseSpecies liquid = data.liquid();
        PhaseSpecies gas = data.gas();
        double hysteresis = data.hysteresisK();

        if (solid != null && liquid != null) {
            double melting = data.meltingPointK();
            edges.add(edge(data, PhaseTransition.Kind.FUSION, solid, liquid,
                    melting, Double.POSITIVE_INFINITY, 20.0, 1e9, 0.0));
            edges.add(edge(data, PhaseTransition.Kind.FREEZING, liquid, solid,
                    0.0, melting - hysteresis, 25.0, 1e8, 0.0));
        }
        if (liquid != null && gas != null) {
            double boiling = data.boilingPointK();
            double vaporization = data.vaporizationEnthalpyKj();
            edges.add(edge(data, PhaseTransition.Kind.VAPORIZATION, liquid, gas,
                    boiling, Double.POSITIVE_INFINITY, 45.0, 1e9, vaporization));
            edges.add(edge(data, PhaseTransition.Kind.CONDENSATION, gas, liquid,
                    0.0, boiling - hysteresis, 10.0, 1e10, vaporization));
        }
        if (solid != null && gas != null) {
            edges.add(edge(data, PhaseTransition.Kind.SUBLIMATION, solid, gas,
                    0.0, Double.POSITIVE_INFINITY, 65.0, 1e3, 0.0));
            edges.add(edge(data, PhaseTransition.Kind.DEPOSITION, gas, solid,
                    0.0, Double.POSITIVE_INFINITY, 60.0, 1e3, 0.0));
        }
        return List.copyOf(edges);
    }

    /** 全表的有向边（注册用）。 */
    public static List<PhaseTransition> allTransitions() {
        List<PhaseTransition> all = new ArrayList<>(8);
        for (PhaseChangeData data : ALL) {
            all.addAll(transitions(data));
        }
        return List.copyOf(all);
    }

    private static PhaseTransition edge(PhaseChangeData data, PhaseTransition.Kind kind,
                                        PhaseSpecies from, PhaseSpecies to,
                                        double minKelvin, double maxKelvin,
                                        double activationEnergyKj, double preExponential,
                                        double pressureShiftEnthalpyKj) {
        String ruleId = "phase_" + data.key() + "_" + kind.name().toLowerCase(Locale.ROOT);
        return new PhaseTransition(ruleId, from.id(), to.id(), kind,
                minKelvin, maxKelvin, activationEnergyKj, preExponential, pressureShiftEnthalpyKj);
    }

    // ==================== 热力学自洽性 ====================

    /** 有向边的转变焓（kJ/mol）：ΔH = ΔHf°(to) − ΔHf°(from)。 */
    public static double transitionEnthalpy(PhaseSpecies from, PhaseSpecies to) {
        return to.formationEnthalpy() - from.formationEnthalpy();
    }

    /** 由 298 K 生成量推出的转变温度：ΔS = (ΔH − ΔG)/298.15，T = ΔH/ΔS。 */
    public static double predictedTransitionTemperature(PhaseSpecies from, PhaseSpecies to) {
        double deltaH = transitionEnthalpy(from, to);
        double deltaG = to.formationGibbs() - from.formationGibbs();
        double deltaS = (deltaH - deltaG) / REFERENCE_TEMPERATURE_K;
        if (Math.abs(deltaS) < 1e-9) return Double.NaN;
        return deltaH / deltaS;
    }
}
