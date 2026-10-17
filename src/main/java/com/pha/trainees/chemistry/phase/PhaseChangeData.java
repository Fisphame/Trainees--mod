package com.pha.trainees.chemistry.phase;

/**
 * 一个物质的相变数据行（§19.17.1「每物质一行」）。
 *
 * <p>{@code T_m}/{@code T_b} 是**转变温度门槛**，{@code fusionEnthalpyKj}/{@code vaporizationEnthalpyKj}
 * 是文献转变焓（用于校验与后续蒸馏/吸收的饱和蒸气压），真正的 ΔH/ΔG 仍由两个相物种的**生成量**推导
 * （§8.3 禁止手填经验值），两者应当互相自洽——由 {@code PhaseChangeTableTest} 锁定。</p>
 *
 * @param key                    物质键（用于生成规则 id，如 {@code h2o}）
 * @param solid                  固相物种（可为 null：该物质无此相）
 * @param liquid                 液相物种（可为 null）
 * @param gas                    气相物种（可为 null）
 * @param meltingPointK          熔点 T_m (K)
 * @param boilingPointK          沸点 T_b (K)
 * @param fusionEnthalpyKj       熔化焓 L_f (kJ/mol)
 * @param vaporizationEnthalpyKj 汽化焓 L_v (kJ/mol)
 * @param hysteresisK            凝固/冷凝侧的死区宽度 ΔT（避免熔点附近抖动）
 */
public record PhaseChangeData(String key,
                              PhaseSpecies solid,
                              PhaseSpecies liquid,
                              PhaseSpecies gas,
                              double meltingPointK,
                              double boilingPointK,
                              double fusionEnthalpyKj,
                              double vaporizationEnthalpyKj,
                              double hysteresisK) {

    public PhaseChangeData {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("相变数据 key 不能为空");
        int phases = (solid == null ? 0 : 1) + (liquid == null ? 0 : 1) + (gas == null ? 0 : 1);
        if (phases < 2) throw new IllegalArgumentException(key + " 至少需要两个相才能生成相变边");
        if (hysteresisK < 0) throw new IllegalArgumentException(key + " 的迟滞不能为负: " + hysteresisK);
        if (solid != null && liquid != null && !(meltingPointK > 0)) {
            throw new IllegalArgumentException(key + " 有固液两相，熔点必须为正: " + meltingPointK);
        }
        if (liquid != null && gas != null && !(boilingPointK > 0)) {
            throw new IllegalArgumentException(key + " 有液气两相，沸点必须为正: " + boilingPointK);
        }
        if (solid != null && liquid != null && gas != null && !(meltingPointK < boilingPointK)) {
            throw new IllegalArgumentException(key + " 熔点必须低于沸点: " + meltingPointK + " / " + boilingPointK);
        }
        if (fusionEnthalpyKj < 0 || vaporizationEnthalpyKj < 0) {
            throw new IllegalArgumentException(key + " 的转变焓必须为非负（方向由边的两端决定）");
        }
    }
}
