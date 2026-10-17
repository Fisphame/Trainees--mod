package com.pha.trainees.chemistry.heat;

import com.pha.trainees.chemistry.phase.PhaseChangeData;
import com.pha.trainees.chemistry.phase.PhaseChangeTable;
import org.jetbrains.annotations.Nullable;

/**
 * 浴槽热力学核心（§19.38 A1）：介质 + 加热器 + 环境散热 → 温度 / 相态演化。
 *
 * <p><b>为什么单独一个纯类</b>：相变平台是这批改动里唯一"物理容易写错"的部分
 * （潜热记账、平台夹持、迟滞、烧干）。拿 MC 环境测它又慢又难覆盖；这里零 MC 依赖、
 * 纯数值进出，由 {@code BathThermalModelTest} 锁住。</p>
 *
 * <p><b>模型</b>（作者 2026-10-02 定稿：保留设定点 + 自动调功 + 介质会耗尽）：</p>
 * <ol>
 *   <li>介质**按相分别记账**（固/液各多少摩尔），总热容 = Σ n_i·c_p,i + 容器项；</li>
 *   <li>净热流先升降温；一旦温度到达转变点且该相还在，**剩余热量全部转入潜热**，
 *       温度被"钉"在转变温度——冰水浴 0 ℃、沸水浴 100 ℃ 就是这么**涌现**的，
 *       不再有任何"水浴 ≤100 ℃"的硬编码区间；</li>
 *   <li>气相视为**逸出**（敞口浴，不留在浴里）：水烧干 = 液摩尔数归零 = 停机；</li>
 *   <li>升温与降温用**不同的平台**（凝固侧退 {@code hysteresisK}），避免平台附近抖动。</li>
 * </ol>
 *
 * <p><b>已知简化</b>（记进蓝本，不假装实现）：油/沙这类混合物只给"工作上限"（夹温度、
 * 不做介质损毁）；冰盐浴的共晶点 252 K 是**混合物常数**（依数性建模属于 §19.17.1 V2）。</p>
 */
public final class BathThermalModel {

    /** 允许的最低温度（K）：冷却侧的硬底，防止把介质算到 0 K 以下。 */
    public static final double MIN_TEMPERATURE_K = 1.0;

    /** 摩尔数低于此值视为"没介质了"（浮点残渣不算）。 */
    public static final double EMPTY_MOLES = 1e-6;

    /**
     * 介质的物理常数（纯数据）。
     *
     * @param meltingPointK        固⇄液平台温度（NaN = 该介质不建模固相）
     * @param boilingPointK        液⇄气平台温度（NaN = 不建模气相）
     * @param fusionKjPerMol       熔化焓 L_f（kJ/mol）
     * @param vaporizationKjPerMol 汽化焓 L_v（kJ/mol）
     * @param solidHeatCapacityJ   固相比热 J/(mol·K)
     * @param liquidHeatCapacityJ  液相比热 J/(mol·K)
     * @param maxTemperatureK      无相变介质的工作上限（NaN = 不设）
     * @param hysteresisK          凝固侧迟滞宽度（K）
     */
    public record Medium(double meltingPointK,
                         double boilingPointK,
                         double fusionKjPerMol,
                         double vaporizationKjPerMol,
                         double solidHeatCapacityJ,
                         double liquidHeatCapacityJ,
                         double maxTemperatureK,
                         double hysteresisK) {

        public boolean hasMelting() {
            return Double.isFinite(meltingPointK) && fusionKjPerMol > 0;
        }

        public boolean hasBoiling() {
            return Double.isFinite(boilingPointK) && vaporizationKjPerMol > 0;
        }

        /** 工作上限（无相变的混合物）；不设则 false。 */
        public boolean hasMaxTemperature() {
            return Double.isFinite(maxTemperatureK);
        }
    }

    /** 浴槽状态（不可变；方块实体持有并逐 tick 用返回值替换）。 */
    public record BathState(double temperatureK, double solidMoles, double liquidMoles) {

        public static BathState of(double temperatureK, double solidMoles, double liquidMoles) {
            return new BathState(temperatureK, Math.max(0, solidMoles), Math.max(0, liquidMoles));
        }

        public double totalMoles() {
            return solidMoles + liquidMoles;
        }

        public boolean isEmpty() {
            return totalMoles() <= EMPTY_MOLES;
        }
    }

    /**
     * 一步演化的结果。
     *
     * @param state           新状态（温度 + 固/液摩尔数）
     * @param evaporatedMoles 本步汽化逸出的摩尔数（供方块实体记账/提示）
     * @param latentJoules    本步真正用于潜热的热量（J，正 = 吸热熔化/汽化，负 = 放热凝固）
     */
    public record StepResult(BathState state, double evaporatedMoles, double latentJoules) {
    }

    /** 平台上一次潜热转换（内部用）：不在平台或方向不匹配时为 null。 */
    private record LatentStep(double remainingJoules, double solidMoles, double liquidMoles,
                              double evaporatedMoles, double latentJoules) {
    }

    private BathThermalModel() {
    }

    /** 当前总热容 J/K：Σ n_i·c_p,i + 容器项（无介质时只剩容器项）。可能为 0（空浴 + 容器项配成 0）。 */
    public static double heatCapacity(BathState state, Medium medium, double vesselHeatCapacityJ) {
        double cp = vesselHeatCapacityJ;
        cp += state.solidMoles() * medium.solidHeatCapacityJ();
        cp += state.liquidMoles() * medium.liquidHeatCapacityJ();
        return Math.max(0, cp);
    }

    /**
     * 推进一步。
     *
     * @param state               当前状态
     * @param medium              介质常数
     * @param netJoules           本步净热量（J，正 = 进热；含加热器给热与环境散热）
     * @param vesselHeatCapacityJ 浴盆本身热容 J/K
     * @return 新状态 + 本步逸出/潜热记账
     */
    public static StepResult step(BathState state, Medium medium, double netJoules, double vesselHeatCapacityJ) {
        BathState current = normalize(state, medium);
        double temperature = current.temperatureK();
        double solid = current.solidMoles();
        double liquid = current.liquidMoles();
        double remaining = netJoules;
        double latentTotal = 0;
        double evaporated = 0;

        // 介质没了：只剩容器跟环境换热（"烧干后慢慢凉下来"是自然结果，不需要特判）
        if (current.isEmpty()) {
            double cp = Math.max(0, vesselHeatCapacityJ);
            if (cp <= 0) return new StepResult(new BathState(temperature, 0, 0), 0, 0);
            temperature = clampTemperature(temperature + remaining / cp);
            return new StepResult(new BathState(temperature, 0, 0), 0, 0);
        }

        for (int guard = 0; guard < 6 && Math.abs(remaining) > 1e-9; guard++) {
            // ① 无潜热的混合物已到工作上限：夹住温度（已知简化：不损毁介质）
            if (remaining > 0 && medium.hasMaxTemperature()
                    && temperature >= medium.maxTemperatureK() - 1e-9) {
                break;
            }
            // ② 已经站在平台上：这一轮的热量全部转潜热，温度不动（相变平台本身）
            LatentStep plateau = onPlateau(temperature, remaining, medium, solid, liquid);
            if (plateau != null) {
                remaining = plateau.remainingJoules();
                solid = plateau.solidMoles();
                liquid = plateau.liquidMoles();
                evaporated += plateau.evaporatedMoles();
                latentTotal += plateau.latentJoules();
                continue;
            }
            // ③ 自由升降温；若本步会越过平台，只走到平台，余热交给下一轮
            double cp = heatCapacity(new BathState(temperature, solid, liquid), medium, vesselHeatCapacityJ);
            // 热容为 0（介质刚好烧干且容器项为 0）：没有东西可加热，余热直接丢弃，
            // 绝不能除下去——否则温度会瞬间爆炸到无意义的大数
            if (cp <= 0) {
                remaining = 0;
                break;
            }
            double stop = nextStop(temperature, remaining, solid, liquid, medium);
            double qToStop = Double.isNaN(stop) ? Double.NaN : (stop - temperature) * cp;
            if (Double.isNaN(stop) || Math.abs(remaining) <= Math.abs(qToStop) + 1e-9) {
                temperature = clampTemperature(temperature + remaining / cp);
                remaining = 0;
                break;
            }
            temperature = stop;
            remaining -= qToStop;
        }

        return new StepResult(BathState.of(temperature, solid, liquid), evaporated, latentTotal);
    }

    /**
     * 状态自洽化（不是玩法逻辑，是**容错**）：温度已在转变点之上却还留着固相
     * （改配置、旧存档迁移、外部直接写状态）→ **把温度夹回转变点**，让正常的平台逻辑
     * 按潜热逐步把冰化掉。
     *
     * <p><b>这里踩过一次坑（2026-10-02 作者上机发现）</b>：最初的写法是"温度高于熔点就把固相
     * 一次性倒进液相、且不做潜热记账"，结果表现为**冰瞬间化成水**（而且只有冰这条路径有，
     * 水→汽看起来正常）——因为那等于绕过了潜热墙。容错只能把状态拉回合法区域，
     * **绝不能替玩家把相变做完**。</p>
     */
    private static BathState normalize(BathState state, Medium medium) {
        if (state.solidMoles() > EMPTY_MOLES && medium.hasMelting()
                && state.temperatureK() > medium.meltingPointK() + 1e-9) {
            return BathState.of(medium.meltingPointK(), state.solidMoles(), state.liquidMoles());
        }
        return state;
    }

    /** 平台上的潜热转换；不在平台或方向与可用相不匹配时返回 null。 */
    @Nullable
    private static LatentStep onPlateau(double temperature, double remaining, Medium medium,
                                        double solid, double liquid) {
        if (remaining > 0 && isMeltingPlateau(temperature, medium) && solid > EMPTY_MOLES) {
            double latent = medium.fusionKjPerMol() * 1000.0;
            double moved = Math.min(solid, remaining / latent);
            return new LatentStep(remaining - moved * latent, solid - moved, liquid + moved, 0, moved * latent);
        }
        if (remaining > 0 && isBoilingPlateau(temperature, medium) && liquid > EMPTY_MOLES) {
            double latent = medium.vaporizationKjPerMol() * 1000.0;
            double moved = Math.min(liquid, remaining / latent);
            return new LatentStep(remaining - moved * latent, solid, liquid - moved, moved, moved * latent);
        }
        if (remaining < 0 && isFreezingPlateau(temperature, medium) && liquid > EMPTY_MOLES) {
            double latent = medium.fusionKjPerMol() * 1000.0;
            double moved = Math.min(liquid, -remaining / latent);
            return new LatentStep(remaining + moved * latent, solid + moved, liquid - moved, 0, -moved * latent);
        }
        return null;
    }

    /** 朝当前热流方向最近的"阻挡温度"：相变平台或工作上限；没有则 NaN（自由升降）。 */
    private static double nextStop(double temperature, double netJoules, double solid, double liquid, Medium medium) {
        double best = Double.NaN;
        if (netJoules > 0) {
            if (medium.hasMelting() && solid > EMPTY_MOLES && medium.meltingPointK() > temperature + 1e-9) {
                best = medium.meltingPointK();
            }
            if (medium.hasBoiling() && liquid > EMPTY_MOLES && medium.boilingPointK() > temperature + 1e-9) {
                best = Double.isNaN(best) ? medium.boilingPointK() : Math.min(best, medium.boilingPointK());
            }
            if (medium.hasMaxTemperature() && medium.maxTemperatureK() > temperature + 1e-9) {
                best = Double.isNaN(best) ? medium.maxTemperatureK() : Math.min(best, medium.maxTemperatureK());
            }
            return best;
        }
        // 冷却：凝固平台退迟滞宽度（与升温的熔点错开，杜绝平台抖动）
        if (medium.hasMelting() && liquid > EMPTY_MOLES) {
            double freezing = medium.meltingPointK() - medium.hysteresisK();
            if (freezing < temperature - 1e-9) best = freezing;
        }
        return best;
    }

    private static boolean isMeltingPlateau(double at, Medium medium) {
        return medium.hasMelting() && Math.abs(at - medium.meltingPointK()) < 1e-6;
    }

    private static boolean isFreezingPlateau(double at, Medium medium) {
        return medium.hasMelting() && Math.abs(at - (medium.meltingPointK() - medium.hysteresisK())) < 1e-6;
    }

    private static boolean isBoilingPlateau(double at, Medium medium) {
        return medium.hasBoiling() && Math.abs(at - medium.boilingPointK()) < 1e-6;
    }

    private static double clampTemperature(double kelvin) {
        return Math.max(MIN_TEMPERATURE_K, kelvin);
    }

    /**
     * 水介质的默认常数（数值直接取 {@link PhaseChangeTable} 的水三相数据，
     * 比热用文献值：冰 38.0、水 75.3 J/(mol·K)）——供方块实体与测试共用。
     */
    public static Medium water() {
        PhaseChangeData water = PhaseChangeTable.WATER_SYSTEM;
        return new Medium(water.meltingPointK(), water.boilingPointK(),
                water.fusionEnthalpyKj(), water.vaporizationEnthalpyKj(),
                38.0, 75.3, Double.NaN, water.hysteresisK());
    }
}
