package com.pha.trainees.chemistry.phase;

import java.util.List;

/**
 * 一条有向相变边的规格（§19.17.1）。纯数据，不引用 Minecraft 类，可直接单测。
 *
 * <p>相变沿用反应图：{@code 反应物 → 生成物} 就是一个相物种变成另一个相物种，
 * ΔH/ΔG 由两个相物种的生成量推导（`deltaHComputed()`/`deltaGComputed()`），
 * 因此**物理平衡常数 K = exp(−ΔG(T)/RT) 天然在转变温度附近穿过 1**，
 * 方向由 Q vs K 自动决定，无需手写优先级。</p>
 *
 * <p>温度窗口（§19.17.1 引擎扩展之一）：
 * {@code minTemperatureK} 与 {@code maxTemperatureK} 构成规则的可运行区间——
 * 熔化只在 {@code T ≥ T_m} 运行，凝固只在 {@code T ≤ T_m − 迟滞} 运行，
 * 死区让液固稳定共存，杜绝"熔化吸热降温→立刻结冰→再熔化"的抖动。
 * 用 {@link Double#POSITIVE_INFINITY} 表示无上限、0 表示无下限。</p>
 *
 * @param ruleId            规则 id
 * @param fromId            起始相物种 id
 * @param toId              目标相物种 id
 * @param kind              相变种类（熔化/凝固/汽化/冷凝/升华/凝华）
 * @param minTemperatureK   可运行温度下限 (K)
 * @param maxTemperatureK   可运行温度上限 (K)
 * @param activationEnergyKj 活化能 (kJ/mol)
 * @param preExponentialFactor 指前因子 A
 * @param pressureShiftEnthalpyKj 温度门槛的压强修正用转变焓（kJ/mol；0 = 门槛不随压强移动）。
 *        只有汽化/冷凝带 L_v（§19.27 Stage 2）；固↔液不带 ΔV 数据，故意保持 0。
 */
public record PhaseTransition(String ruleId,
                              String fromId,
                              String toId,
                              Kind kind,
                              double minTemperatureK,
                              double maxTemperatureK,
                              double activationEnergyKj,
                              double preExponentialFactor,
                              double pressureShiftEnthalpyKj) {

    public enum Kind {
        /** 固 → 液（熔化） */
        FUSION,
        /** 液 → 固（凝固） */
        FREEZING,
        /** 液 → 气（汽化） */
        VAPORIZATION,
        /** 气 → 液（冷凝） */
        CONDENSATION,
        /** 固 → 气（升华） */
        SUBLIMATION,
        /** 气 → 固（凝华） */
        DEPOSITION
    }

    public PhaseTransition {
        if (ruleId == null || ruleId.isBlank()) throw new IllegalArgumentException("ruleId 不能为空");
        if (fromId == null || toId == null) throw new IllegalArgumentException("相物种 id 不能为空");
        if (fromId.equals(toId)) throw new IllegalArgumentException("相变两端不能是同一物种: " + fromId);
        if (!(preExponentialFactor > 0)) throw new IllegalArgumentException("指前因子必须为正: " + preExponentialFactor);
        if (activationEnergyKj < 0) throw new IllegalArgumentException("活化能不能为负: " + activationEnergyKj);
        if (maxTemperatureK < minTemperatureK) {
            throw new IllegalArgumentException("温度窗口倒置: " + minTemperatureK + " > " + maxTemperatureK);
        }
    }

    /** 该边的温度门槛是否随压强移动（目前只有汽化/冷凝带 L_v，§19.27 Stage 2）。 */
    public boolean isPressureShifted() {
        return pressureShiftEnthalpyKj > 0;
    }

    /** 是否有下界（便于测试与展示）。 */
    public boolean hasLowerBound() {
        return minTemperatureK > 0;
    }

    /** 是否有上界。 */
    public boolean hasUpperBound() {
        return Double.isFinite(maxTemperatureK);
    }

    /** 该边涉及的全部相物种 id（s1/e 校验用）。 */
    public List<String> speciesIds() {
        return List.of(fromId, toId);
    }
}
