package com.pha.trainees.chemistry.phase;

/**
 * 相物种的注册数据（§19.17.1）。
 *
 * <p><b>相变表是唯一真源</b>：新相节点（冰等）在注册时从这里读出生成量，
 * 而不是在注册处再手填一遍，避免"表里一套、注册里一套"的漂移。
 * 尚未迁移的既有物种（水/蒸汽/食盐/熔盐）目前仍在注册处写常量，
 * 待 §19.19 S1 数据驱动正式化后统一由表生成注册。</p>
 *
 * @param id                物种 id（path 形式，如 {@code h2o_solid}）
 * @param molarMass         摩尔质量 (g/mol)
 * @param formationEnthalpy 标准生成焓 ΔHf° (kJ/mol, 298 K)
 * @param formationGibbs    标准生成吉布斯自由能 ΔGf° (kJ/mol, 298 K)
 */
public record PhaseSpecies(String id, double molarMass, double formationEnthalpy, double formationGibbs) {
}
