package com.pha.trainees.chemistry.gas;

import com.pha.trainees.Main;
import com.pha.trainees.chemistry.particle.IonType;
import net.minecraft.resources.ResourceLocation;

/**
 * 大气边界条件（§19.27.1）：<b>空气不建模成网格内容，只作为查询期的兜底</b>。
 *
 * <p><b>为什么不能真初始化空气</b>：一个区块 16×16×384 = 98,304 格，玩家视野 21×21 区块 ≈ 4300 万格。
 * 就算只给"有空气的格"建一个 {@link GasMixture}（两个 map 条目，保守约 150 B），也是 <b>GB 量级</b>；
 * 而且 {@code tickDiffusion} 每 10 tick 要遍历它们，区块卸载的 LOD 坍缩、待水合队列、
 * 湮灭阈值清扫全都要为"什么都没发生的空气"付账。现在的网格之所以撑得住，正是因为它是<b>稀疏</b>的
 * （见 {@code GAS_ACTIVE_RADIUS}）。所以：<b>大气不是物质，是边界条件。</b></p>
 *
 * <p><b>位移规则</b>（本类的核心）：</p>
 * <pre>P_物种 = 该物种在干空气中的体积分数 × max(0, P_环境 − P_该格已建模)</pre>
 * <p>{@code max(0, …)} 是必须的：一格被氯气灌满时 {@code P_已建模 → 环境压}，留给空气的余量 → 0，
 * 于是"这个格子里没有氧气"自然成立——"灌 CO₂ 灭火""密闭空间窒息""抽真空"全都不需要特判，
 * 是算出来的。</p>
 *
 * <p><b>⚠ 氮气陷阱（务必先读）</b>：N₂ 占 79%，<b>绝不能给它"标准浓度"</b>。一旦敞口容器按浓度吸空气，
 * 每个敞口烧杯都会凭空多出约 0.033 mol/L 的 N₂ 与 0.0087 mol/L 的 O₂，导致
 * ① {@code contents.isEmpty()} 失效（空烧杯不再显示"空"）；② 分析仪内容物列表被空气刷屏；
 * ③ 取样取出"空气样品"；④ 哈伯的氮白送。因此 <b>§19.27.1 决策 A：容器不自动吸空气</b>——
 * 本类只是"查询接口"，不驱动任何自动取气。</p>
 *
 * <p><b>可测性</b>：核心方法全部只依赖 {@link ResourceLocation} 与基本类型，是纯函数；
 * {@link IonType} 重载只是薄适配层（单测里构造不出 IonType，因为 Builder 会读 Forge 配置）。</p>
 */
public final class AmbientAir {

    private AmbientAir() {}

    /** 干空气中 O₂ 的体积分数（= 摩尔分数） */
    public static final double O2_FRACTION = 0.21;
    /** 干空气中 N₂ 的体积分数（= 摩尔分数） */
    public static final double N2_FRACTION = 0.79;

    /** 该物种在干空气中的体积分数；<b>不建模的组分返回 0</b>（氩、CO₂ 等暂不建模）。 */
    public static double fractionOf(ResourceLocation speciesId) {
        if (speciesId == null) return 0;
        // 命名空间不符 → 失败关闭（与 SpecMatcher 的 UNKNOWN_MASS 同一纪律），避免别的模组的 o2 被当成我们的
        if (!Main.MODID.equals(speciesId.getNamespace())) return 0;
        return switch (speciesId.getPath()) {
            case "o2" -> O2_FRACTION;
            case "n2" -> N2_FRACTION;
            default -> 0;
        };
    }

    /** 该物种在干空气中的体积分数（{@link IonType} 适配层）。 */
    public static double fractionOf(IonType species) {
        return species == null ? 0 : fractionOf(species.getId());
    }

    /**
     * 该物种由大气提供的分压（atm）：{@code 分数 × max(0, P_环境 − P_已建模)}。
     *
     * @param modeledPressureAtm 该空间已建模气体（网格格或容器内容物）的总压；负值按 0 处理
     * @param ambientAtm         环境压强（atm）；通常传 {@link GasPhysics#ambientAtm()}
     */
    public static double partialPressureAtm(ResourceLocation speciesId, double modeledPressureAtm, double ambientAtm) {
        double fraction = fractionOf(speciesId);
        if (fraction <= 0 || ambientAtm <= 0) return 0;
        double free = ambientAtm - Math.max(0, modeledPressureAtm);
        return free <= 0 ? 0 : fraction * free;
    }

    /** {@link #partialPressureAtm(ResourceLocation, double, double)} 的 IonType 适配层。 */
    public static double partialPressureAtm(IonType species, double modeledPressureAtm, double ambientAtm) {
        return species == null ? 0 : partialPressureAtm(species.getId(), modeledPressureAtm, ambientAtm);
    }

    /** 使用当前配置的环境压。 */
    public static double partialPressureAtm(IonType species, double modeledPressureAtm) {
        return partialPressureAtm(species, modeledPressureAtm, GasPhysics.ambientAtm());
    }

    /**
     * 大气在该体积/温度下能为该物种提供的摩尔数（mol）——"这里够不够烧"就用它回答。
     * 例：1 m³（1000 L）的纯空气在 293 K、1 atm 下总摩尔 ≈ 41.6 mol，其中 O₂ ≈ 8.7 mol。
     */
    public static double availableMoles(ResourceLocation speciesId, double volumeLiters, double tempK,
                                        double modeledPressureAtm, double ambientAtm) {
        double pressure = partialPressureAtm(speciesId, modeledPressureAtm, ambientAtm);
        if (pressure <= 0 || volumeLiters <= 0) return 0;
        return pressure * GasPhysics.KPA_PER_ATM * volumeLiters / (GasPhysics.R * Math.max(1, tempK));
    }

    /** {@link #availableMoles(ResourceLocation, double, double, double, double)} 的 IonType 适配层。 */
    public static double availableMoles(IonType species, double volumeLiters, double tempK,
                                       double modeledPressureAtm, double ambientAtm) {
        return species == null ? 0
                : availableMoles(species.getId(), volumeLiters, tempK, modeledPressureAtm, ambientAtm);
    }
}
