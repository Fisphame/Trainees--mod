package com.pha.trainees.chemistry.container;

import com.pha.trainees.chemistry.engine.ReactionEngine;
import com.pha.trainees.chemistry.engine.ReactionFailure;
import com.pha.trainees.chemistry.particle.IonType;
import com.pha.trainees.chemistry.particle.Phase;
import com.pha.trainees.chemistry.reaction.ReactionRule;
import com.pha.trainees.config.ChemConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Set;

/**
 * 化学容器接口
 * 所有可进行化学反应的方块实体（烧杯、反应釜、电解槽等）需实现此接口
 */
public interface IChemicalContainer {

    // ==================== 粒子管理 ====================

    /** 获取容器内所有粒子及其物质的量（mol） */
    Map<IonType, Double> getContents();

    /** 获取指定粒子的物质的量（mol），不存在则返回 0 */
    double getAmount(IonType ion);

    /** 添加粒子（增加物质的量），返回实际增加量 */
    double addIon(IonType ion, double moles);

    /**
     * 添加离子，可指定是否触发引擎
     * 默认行为：调用 addIon(ion, moles) 并触发引擎
     */
    default double addIon(IonType ion, double moles, boolean triggerEngine) {
        // 默认实现：直接调用原方法，由子类重写更精确的控制
        return addIon(ion, moles);
    }

    /** 移除粒子（减少物质的量），返回实际移除量（可能小于请求量） */
    double removeIon(IonType ion, double moles);

    default double removeIon(IonType ion, double moles, boolean triggerEngine) {
        return removeIon(ion, moles);
    }

    /** 检查容器是否包含指定粒子且数量足够 */
    boolean contains(IonType ion, double minMoles);

    /** 获取容器中所有粒子的种类集合 */
    Set<IonType> getPresentIons();

    // ==================== 物理状态 ====================

    /** 获取当前温度（开尔文 K） */
    double getTemperature();

    /** 设置温度（开尔文 K），通常由热力学引擎调用 */
    void setTemperature(double temperatureKelvin);

    /** 获取容器有效容积（升 L），用于浓度计算 */
    double getVolume();

    /** 设置容器容积（通常由玩家操作或机器状态改变） */
    void setVolume(double volumeLiters);

    /** 获取容器当前总压（仅对气相有效，返回 Pa 或 atm） */
    double getPressure();

    // ==================== 热力学 ====================

    /** 获取容器总热容（J/K） */
    double getTotalHeatCapacity();

    /** 向容器添加热能（焦耳 J），温度相应上升 */
    void addThermalEnergy(double joules);

    // ==================== 标识 ====================

    /** 获取容器所在的 Level */
    Level getLevel();

    /** 获取容器所在的 BlockPos */
    BlockPos getBlockPos();

    /** 检查容器是否已被破坏或移除 */
    boolean isRemoved();

    /** 标记容器需要同步更新（发给客户端） */
    void setChanged();

    // ==================== 引擎查询接口 ====================

    /**
     * 获取指定离子在当前容器中的有效浓度（mol/L），物态感知（蓝本 §4.2）：
     * - GAS / AQUEOUS：浓度 = n / volume（参与速率方程与反应商的浓度乘积）
     * - LIQUID / SOLID：活度恒为 1.0（不参与速率方程与反应商的浓度乘积）
     * 消耗/生成的数量判断仍基于 getAmount()（摩尔数），不受此方法影响。
     */
    default double getEffectiveConcentration(IonType ion) {
        Phase phase = ion.getPhase();
        if (phase == Phase.LIQUID || phase == Phase.SOLID) {
            return 1.0;
        }
        double volume = getVolume();
        if (volume <= 0) return 0;
        return getAmount(ion) / volume;
    }

    /**
     * 检查容器中是否所有反应物种类都存在（非零量）。
     * 注意：只检查"存在"，不要求达到完整计量数——具体每次消耗量由引擎按 Δξ 缩放，
     * 计量数 >1 的反应（如 2NaCl → 2Na + Cl₂）在存量低于完整计量数时仍应能继续部分反应。
     */
    default boolean containsAll(Map<IonType, Integer> requirements) {
        for (Map.Entry<IonType, Integer> entry : requirements.entrySet()) {
            if (getAmount(entry.getKey()) <= 1e-9) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取容器内所有粒子的总摩尔数
     */
    default double getTotalMoles() {
        return getContents().values().stream().mapToDouble(Double::doubleValue).sum();
    }

    // ==================== 开口与总压（#10 / §19.27） ====================

    /**
     * 容器自身的"端口面"（可通气 / 将来可接管道与贴片的方向）。
     * <b>默认空集 = 密闭</b>；烧杯与电解槽覆写为"上口"（玻璃壁不是口）。
     * 将来的盖子、阀门、管道贴片只需改这个集合或加 override，不必改交换逻辑。
     */
    default Set<net.minecraft.core.Direction> getPortFaces() {
        return Set.of();
    }

    /**
     * 该端口面是否<b>真的</b>与外界相通：是端口 <b>且</b> 外侧没有被阻挡。
     * 默认"非空气即阻挡"——与历史行为（只看上方是否空气）逐字一致；
     * 多孔/格栅等透气方块留给管道系统扩展。
     */
    default boolean isFaceOpen(net.minecraft.core.Direction face) {
        if (!getPortFaces().contains(face)) return false;
        Level level = getLevel();
        if (level == null) return false;
        return level.getBlockState(getBlockPos().relative(face)).isAir();
    }

    /** 当前所有与外界相通的面（有序：按 Direction 枚举序） */
    default Set<net.minecraft.core.Direction> getOpenFaces() {
        Set<net.minecraft.core.Direction> open = new java.util.LinkedHashSet<>();
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
            if (isFaceOpen(d)) open.add(d);
        }
        return open;
    }

    /** 是否密闭（没有任何相通的面）。总压<b>只在密闭时</b>才是独立状态量（§19.27）。 */
    default boolean isSealed() {
        return getOpenFaces().isEmpty();
    }

    /** 气相总摩尔数（只有气相贡献压强；液相/固相不算） */
    default double getGasMoles() {
        double sum = 0;
        for (Map.Entry<IonType, Double> e : getContents().entrySet()) {
            if (e.getKey().getPhase() == Phase.GAS) sum += e.getValue();
        }
        return sum;
    }

    /**
     * 理想气体总压（atm，由 {@code nRT/V} 导出，§19.27）。
     * <ul>
     *   <li><b>密闭容器</b>：这就是真实总压（超压/负压判定与开盖喷出的依据）；</li>
     *   <li><b>敞口容器</b>：这是"若密闭时的分压之和"，也是分压逸散的判据来源——
     *       注意敞口容器的总压物理上恒等于环境压，故本值可远超 1 atm 而不产生后果
     *       （有意保留的妥协，见 §19.27 决策 A）。</li>
     * </ul>
     */
    default double getIdealPressureAtm() {
        return com.pha.trainees.chemistry.gas.GasPhysics.pressureAtm(getGasMoles(), getVolume(), getTemperature());
    }

    /**
     * 相变/沸点等"温度门槛"使用的有效压强（atm，§19.27 Stage 2）：
     * <b>密闭容器</b>用真实总压 {@link #getIdealPressureAtm()}；<b>敞口容器</b>恒为环境压
     * （气体自由逸散，总压物理上就是环境压）。所以"真空蒸馏"的前提正是"能把容器密封"。
     */
    default double getEffectivePressureAtm() {
        return isSealed()
                ? getIdealPressureAtm()
                : com.pha.trainees.chemistry.gas.GasPhysics.ambientAtm();
    }

    // ==================== 附着件倍率（§19.29） ====================

    /**
     * 传热系数倍率（附着件汇总：搅拌器等）。默认 1.0 = 不影响；
     * 没有任何附着件时容器行为与历史逐字一致。
     */
    default double getHeatTransferFactor() {
        return 1.0;
    }

    /** 气液传质（容器 ↔ 气体网格交换速率）倍率（附着件汇总）。默认 1.0。 */
    default double getGasTransferFactor() {
        return 1.0;
    }

    // ==================== 容量上限（§19.30：随容积缩放） ====================

    /**
     * 本容器允许的总摩尔数上限。默认取全局配置；大容器（反应釜）按容积放大，
     * 这样"批次更大"不需要另开一套容量逻辑。
     */
    default double getMaxTotalMoles() {
        return com.pha.trainees.config.ChemConfig.MAX_TOTAL_MOLES.get();
    }

    /** 本容器允许的单成分摩尔数上限。默认取全局配置，大容器按容积放大。 */
    default double getMaxMolesPerComponent() {
        return com.pha.trainees.config.ChemConfig.MAX_MOLES_PER_COMPONENT.get();
    }

    // ==================== 耐压上限（§19.31 危险层） ====================

    /**
     * 本容器的安全压力上限（atm）：超过即泄压（`rupture` 打开时还会破裂）。
     *
     * <p>默认 {@code +∞}——**未知容器不炸**，只有明确声明了材料的容器才有耐压概念
     * （{@code BeakerBlockEntity} 按材料给，反应釜再乘上釜盖系数）。</p>
     */
    default double getMaxSafePressureAtm() {
        return Double.POSITIVE_INFINITY;
    }

    // ==================== 腐蚀（§19.32 危险层之二） ====================

    /** 容器腐蚀度 0~1（0 = 全新）；不支持腐蚀的容器返回 0。 */
    default double getCorrosionLevel() {
        return 0.0;
    }

    /** 容器当前材质档 id（诊断显示用）；未知返回 "unknown"。 */
    default String getMaterialId() {
        return "unknown";
    }

    // ==================== 电解/通电（蓝本 §17） ====================

    /**
     * 容器当前是否通电。
     * 理论阶段由配置 ELECTROLYZER_FREE_POWER 决定（无限电调试）；
     * 未来接入机器能量系统后，由容器的 IEnergyStorage 决定（覆盖此方法）。
     */
    default boolean isElectricallyPowered() {
        return ChemConfig.ELECTROLYZER_FREE_POWER.get();
    }

    /**
     * 消耗电能（kJ），返回是否成功扣除。
     * 理论阶段无限电时恒成功；未来接入能量系统后从 IEnergyStorage 扣减并返回结果。
     */
    default boolean consumeElectricalEnergy(double kilojoules) {
        return isElectricallyPowered();
    }

    // ==================== 平衡状态管理（用于引擎） ====================

    /**
     * 标记某个反应规则在容器中已达到平衡（暂不触发）
     */
    void markRuleBalanced(ReactionRule rule);

    /**
     * 检查某个反应规则是否已被标记为平衡状态
     */
    boolean isRuleBalanced(ReactionRule rule);

    /**
     * 重置该容器中所有规则的平衡状态
     * 当容器成分发生变化时（添加/移除粒子），应调用此方法
     */
    void resetBalancedRules();

    // ==================== 失败诊断记录（§19.6/19.7） ====================

    /**
     * 记录一次规则执行失败（供失败反馈/反应预测器使用）。
     * 默认实现不记录；烧杯等容器可覆写为保留最近 N 条（仅运行时，不存 NBT）。
     */
    default void recordFailure(ReactionFailure failure) {
    }

    /**
     * 获取本容器最近的失败记录（最旧在前）。
     * 默认实现返回空列表。
     */
    default java.util.List<ReactionFailure> getRecentFailures() {
        return java.util.List.of();
    }

    // ==================== 实际执行记录与竞争信息（§19.7 决策 2c） ====================

    /**
     * 记录一次实际执行成功的反应（含被它挤掉的竞争规则说明）。
     * 默认实现不记录；烧杯等容器可覆写为保留最近 N 条（仅运行时，不存 NBT）。
     */
    default void recordExecutedRule(com.pha.trainees.chemistry.engine.ExecutedRule record) {
    }

    /**
     * 获取本容器最近实际执行成功的反应记录（最旧在前）。
     * 与 {@link #getRecentFailures()} 配对：一个答"为什么没反应"，一个答"原料被谁消耗了"。
     */
    default java.util.List<com.pha.trainees.chemistry.engine.ExecutedRule> getRecentExecutedRules() {
        return java.util.List.of();
    }

    // ==================== 诊断补充信息（§19.14） ====================

    /**
     * 容器专属诊断补充行（分析仪诊断页显示）。
     *
     * <p>通用信息（温度/体积/总量/供电/内容物）由分析仪统一生成；机器类容器在这里补充
     * 自己的状态（如电解槽的电极方向、隔膜、双面储能），这样分析仪无需特判任何具体容器类型，
     * 也不会把机器信息刷进聊天栏（§19.14 点 1：信息查询统一走分析仪）。</p>
     *
     * <p>追加的是**结构化行**（翻译键 + 参数），由客户端本地化渲染（§19.16 语言键一并处理）。</p>
     *
     * @param lines 追加 {@link com.pha.trainees.chemistry.report.ReportLine}
     */
    default void appendDiagnostics(java.util.List<com.pha.trainees.chemistry.report.ReportLine> lines) {
    }

    // ==================== 产物处理钩子（§19.10 电解槽） ====================

    /**
     * 引擎在应用反应产物时调用：容器可自行处理该产物（如电解槽按电极/膜分拣导出）。
     * @return true = 容器已处理该产物（引擎不再 addIon 入 contents、不触发连锁入队）；
     *         false = 默认行为（引擎正常 addIon 入 contents 并视出边入队）。
     * 烧杯不覆写此方法，行为零变化。
     */
    default boolean onProductGenerated(ReactionRule rule, IonType ion, double moles) {
        return false;
    }

    // ==================== 电解工作电压档（§19.10 / §19.11） ====================

    /**
     * 电解工作电压倍率（相对理论分解电压；玩家侧表现为 1~5 档旋钮）。
     * 影响：① 实际电功 W_实际 = 理论电功 × factor（每 mol 电耗上升、净热账 W_实际 − ΔH 转热）；
     *      ② 电解进度倍率（档越高单位时间产物越多）。
     * 非电解容器（烧杯等）默认 1.0（= 理论最低工作点，无额外发热）。
     */
    default double getVoltageFactor() {
        return 1.0;
    }
}