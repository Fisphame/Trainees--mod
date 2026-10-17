package com.pha.trainees.chemistry.reaction;

import com.pha.trainees.Main;
import com.pha.trainees.chemistry.container.IChemicalContainer;
import com.pha.trainees.chemistry.particle.IonType;
import com.pha.trainees.chemistry.particle.Phase;
import com.pha.trainees.config.ChemConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 反应规则对象
 * 包含完整化学方程式、热力学参数、动力学参数和前置条件
 */
public class ReactionRule {

    private final ResourceLocation id;
    private final Map<IonType, Integer> reactants;    // 反应物及计量数（精确匹配）
    private final Map<IonType, Integer> products;     // 生成物及计量数
    private final Map<IonType, Integer> preconditions; // 前置条件（催化剂等，不消耗）
    // 使用 Supplier 支持懒求值与热加载（配置变更即时生效）
    private final Supplier<Double> deltaH;              // 反应焓变 (kJ/mol)
    private final Supplier<Double> deltaG;              // 标准吉布斯自由能变 (kJ/mol)
    private final Supplier<Double> equilibriumConstant; // 298K 平衡常数
    private final Supplier<Double> activationEnergy;    // 活化能 (kJ/mol)
    private final Supplier<Double> preExponentialFactor;// 指前因子 A
    private final Supplier<Double> gamePriorityBias;    // 游戏性优先级偏置（默认1.0）
    private final Supplier<Double> minTemperature;      // 最低触发温度 (K)
    private final Supplier<Double> maxTemperature;      // 最高可运行温度 (K，默认 +∞)
    // 电解所需电功（kJ/mol，蓝本 §17）：>0 表示该反应为电解反应，通电时 ΔG_eff = ΔG - W
    private final Supplier<Double> electricalWorkPerMol;
    // 动态优先级函数（蓝本 §5.2）：以容器状态为自变量的优先级偏置，为空则用 gamePriorityBias
    private final Function<IChemicalContainer, Double> dynamicPriorityBias;
    private final boolean isSelfLoop;                   // 是否为自环（分解反应）
    // 是否显式覆盖平衡常数（手设 K° × 范特霍夫）。默认 false → 物理公式 K = exp(-ΔG(T)/(R·T))。
    private final boolean equilibriumOverridden;
    // 相变等"安静规则"（§19.17.1）：不写失败诊断，避免门槛附近刷满分析仪与失败缓冲。
    private final boolean quiet;
    // 家族（§19.36）：决定这条规则进 JEI 的哪一页（化学反应 / 电离 / 相变）。
    // 不显式指定时由 build() 派生：quiet → PHASE，其余 → CHEMICAL（电离必须显式声明）。
    private final RuleFamily family;
    // 温度门槛的压强修正（§19.27 Stage 2）：>0 表示门槛随压强按克劳修斯–克拉珀龙移动，
    // 参考温度 = 本规则自己的 min/max 门槛值（即 1 atm 下的转变温度）。
    private final double pressureShiftDeltaHKj;
    private final double pressureShiftRefAtm;

    private ReactionRule(Builder builder) {
        this.id = builder.id;
        this.reactants = Map.copyOf(builder.reactants);
        this.products = Map.copyOf(builder.products);
        this.preconditions = Map.copyOf(builder.preconditions);

        this.deltaH = builder.deltaH;
        this.deltaG = builder.deltaG;
        this.equilibriumConstant = builder.equilibriumConstant;
        this.activationEnergy = builder.activationEnergy;
        this.preExponentialFactor = builder.preExponentialFactor;
        this.gamePriorityBias = builder.gamePriorityBias;
        this.minTemperature = builder.minTemperature;
        this.maxTemperature = builder.maxTemperature;
        this.electricalWorkPerMol = builder.electricalWorkPerMol;
        this.dynamicPriorityBias = builder.dynamicPriorityBias;
        this.isSelfLoop = builder.isSelfLoop;
        this.equilibriumOverridden = builder.equilibriumOverridden;
        this.quiet = builder.quiet;
        this.family = deriveFamily(builder.quiet, builder.family);
        this.pressureShiftDeltaHKj = builder.pressureShiftDeltaHKj;
        this.pressureShiftRefAtm = builder.pressureShiftRefAtm;
    }

    // ==================== Getters ====================
    public ResourceLocation getId() { return id; }

    /**
     * 家族派生（§19.36，纯函数便于单测）：显式优先；否则 {@code quiet → 相变}、其余 {@code → 化学反应}。
     *
     * <p>{@link RuleFamily#IONIZATION} **不派生**——判据（selfLoop + 水前置）会要求模型层知道
     * "哪个物种是水"，且以后一条水的自环分解会被误分类，所以电离规则必须显式写
     * {@code .family(RuleFamily.IONIZATION)}。</p>
     */
    public static RuleFamily deriveFamily(boolean quiet, RuleFamily explicit) {
        if (explicit != null) return explicit;
        return quiet ? RuleFamily.PHASE : RuleFamily.CHEMICAL;
    }
    public Map<IonType, Integer> getReactants() { return reactants; }
    public Map<IonType, Integer> getProducts() { return products; }
    public Map<IonType, Integer> getPreconditions() { return preconditions; }
    public double getDeltaH() { return deltaH.get(); }
    public double getDeltaG() { return deltaG.get(); }
    public double getEquilibriumConstant() { return equilibriumConstant.get(); }
    public double getActivationEnergy() { return activationEnergy.get(); }
    public double getPreExponentialFactor() { return preExponentialFactor.get(); }
    public double getGamePriorityBias() { return gamePriorityBias.get(); }
    public double getMinTemperature() { return minTemperature.get(); }
    public double getMaxTemperature() { return maxTemperature.get(); }

    // ==================== 温度门槛的压强修正（§19.27 Stage 2） ====================

    /** 该规则的温度门槛是否随压强移动（目前只有汽化/冷凝传了 L_v）。 */
    public boolean isTemperaturePressureShifted() { return pressureShiftDeltaHKj > 0; }

    /** 参考压强（atm）：相变表固定 1 atm。 */
    public double getPressureShiftReferenceAtm() { return pressureShiftRefAtm; }

    /** 温度门槛压移用的潜热（kJ/mol）——导出工具需要它才能把相变规则写全。 */
    public double getPressureShiftDeltaHKj() { return pressureShiftDeltaHKj; }

    /**
     * <b>当前压强下</b>的最低触发温度（K）。未标注压移的规则、以及 +∞/≤0 这类无意义门槛
     * （汽化的上限、冷凝的下限）原样返回——所以只有"汽化下限 = T_b(T)"与"冷凝上限 = T_b − 迟滞"
     * 会真正随压强移动。
     */
    public double getMinTemperatureAt(double pressureAtm) {
        return shiftGateForPressure(getMinTemperature(), pressureAtm);
    }

    /** <b>当前压强下</b>的最高可运行温度（K）。 */
    public double getMaxTemperatureAt(double pressureAtm) {
        return shiftGateForPressure(getMaxTemperature(), pressureAtm);
    }

    private double shiftGateForPressure(double gateK, double pressureAtm) {
        if (pressureShiftDeltaHKj <= 0 || !Double.isFinite(gateK) || gateK <= 0) return gateK;
        return ReactionMath.clausiusClapeyronTemperature(gateK, pressureShiftRefAtm, pressureShiftDeltaHKj, pressureAtm);
    }
    public boolean isSelfLoop() { return isSelfLoop; }

    /** 相变等"安静规则"：不写失败诊断（§19.17.1）。 */
    public boolean isQuiet() { return quiet; }

    /** 家族（§19.36）：JEI 分类与条件区文案按它切换。 */
    public RuleFamily getFamily() { return family; }

    /** 是否显式覆盖了平衡常数（可逆玩法；JEI 等只读展示需要区分算法，§19.16） */
    public boolean isEquilibriumOverridden() { return equilibriumOverridden; }

    /**
     * 是否使用了<b>动态优先级函数</b>（§17.1 选择性公开：这类参数无法用数字表达）。
     *
     * <p>导出工具用它在"序列化成数据"时报出**会丢失的信息**：动态函数本体是 Java 代码，
     * 无法反推成 JSON；正确做法是把它注册成<b>命名偏置函数</b>并在数据里引用 id
     * （见 {@code BiasFunctionRegistry} 与 {@code docs/CHEMISTRY_DATA_SCHEMA.md} §3.2）。</p>
     */
    public boolean hasDynamicPriority() { return dynamicPriorityBias != null; }

    /**
     * 动态优先级函数本体（可能为 null）。
     *
     * <p>导出工具拿它去 {@code BiasFunctions.idOf(...)} 反查注册 id：反查得到就写
     * {@code {"priority": {"function": id}}}，反查不到才报"无法表达"的错。
     * 这样**注册表是唯一真相**，规则上不需要再存一个 id 字段。</p>
     */
    public Function<IChemicalContainer, Double> getDynamicPriorityBias() { return dynamicPriorityBias; }

    /**
     * 检查容器是否满足前置条件（催化剂等）
     */
    public boolean checkPreconditions(IChemicalContainer container) {
        for (Map.Entry<IonType, Integer> entry : preconditions.entrySet()) {
            if (container.getAmount(entry.getKey()) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 计算当前温度下的实际速率常数 k
     * k = A * exp(-Ea / (R * T))（纯数学，见 ReactionMath.rateConstant）
     */
    public double calculateRateConstant(double temperatureKelvin) {
        return ReactionMath.rateConstant(activationEnergy.get(), preExponentialFactor.get(), temperatureKelvin);
    }

    /**
     * 计算当前温度下的实际平衡常数 K。
     * 电解反应（W>0）通电时：K 由 ΔG_eff 决定（电功等效于把平衡推向产物）。
     * 显式覆盖（equilibriumOverridden）：K = K° × exp(-ΔH/R * (1/T - 1/298))（范特霍夫）。
     * 默认（物理公式）：K = exp(-ΔG(T)/(R·T))，ΔG(T) = ΔH - T·ΔS，ΔS 由 298K 数据反推。
     * 计算细节委托纯数学层 ReactionMath（可单元测试）。
     */
    public double calculateEquilibriumConstant(double temperatureKelvin, IChemicalContainer container) {
        double work = electricalWorkPerMol.get();
        if (work > 0 && container.isElectricallyPowered()) {
            return ReactionMath.equilibriumConstantElectrolysis(deltaG.get(), work, temperatureKelvin);
        }

        if (temperatureKelvin <= 0) {
            Main.LOGGER.warn("[Engine] calculateEquilibriumConstant: temperature {} <= 0, returning 0",
                    temperatureKelvin);
            return 0;
        }

        if (equilibriumOverridden) {
            // 显式覆盖（游戏性可逆）：K° × 范特霍夫
            return ReactionMath.equilibriumConstantVanHoff(equilibriumConstant.get(), deltaH.get(), temperatureKelvin);
        }

        // 物理平衡常数：K = exp(-ΔG(T)/(R·T))，ΔG(T) = ΔH - T·ΔS（由生成数据推出）
        return ReactionMath.equilibriumConstantPhysical(deltaH.get(), deltaG.get(), temperatureKelvin);
    }

    /**
     * 电解所需电功（kJ/mol），>0 表示该反应为电解反应
     */
    public double getElectricalWorkPerMol() { return electricalWorkPerMol.get(); }

    /**
     * 有效自由能变（蓝本 §17 电解）：ΔG_eff = ΔG - W。
     * 电解反应（W>0）且容器通电时返回抵消后的值；否则返回原 ΔG。
     */
    public double getEffectiveDeltaG(IChemicalContainer container) {
        double work = electricalWorkPerMol.get();
        if (work <= 0 || !container.isElectricallyPowered()) return deltaG.get();
        return deltaG.get() - work;
    }

    /**
     * 计算该反应在当前容器状态下的“优先级评分”
     * Score = (-ΔG_eff / Ea) * 温度修正 * 偏置
     * 电解反应（W>0）未通电时不可运行（返回 0）
     */
    public double calculatePriority(IChemicalContainer container) {
        double temp = container.getTemperature();
        if (temp < minTemperature.get() || temp > maxTemperature.get()) return 0;
        // 电解反应未通电时不可运行
        if (electricalWorkPerMol.get() > 0 && !container.isElectricallyPowered()) return 0;
        // 基础评分：热力学驱动力（有效自由能）/ 动力学壁垒
        double baseScore = (-getEffectiveDeltaG(container)) / (activationEnergy.get() + ChemConfig.PRIORITY_EPSILON.get());
        // 温度修正：温度越高，动力学因素权重越大
        double tempModifier = 1.0 +
                ChemConfig.PRIORITY_TEMPERATURE_MODIFIER.get() * (temp - 298.0) / ChemConfig.PRIORITY_TEMPERATURE_REFERENCE.get();
        double bias = dynamicPriorityBias != null ? dynamicPriorityBias.apply(container) : gamePriorityBias.get();
        return baseScore * tempModifier * bias;
    }

    @Override
    public String toString() {
        return "ReactionRule{" + id + "}";
    }

    // ==================== Builder ====================
    public static class Builder {
        private final ResourceLocation id;
        private final Map<IonType, Integer> reactants = new HashMap<>();
        private final Map<IonType, Integer> products = new HashMap<>();
        private final Map<IonType, Integer> preconditions = new HashMap<>();
        private Supplier<Double> deltaH = () -> 0.0;
        private Supplier<Double> deltaG = () -> 0.0;
        private Supplier<Double> equilibriumConstant = ChemConfig.DEFAULT_EQUILIBRIUM_CONSTANT;
        private Supplier<Double> activationEnergy = ChemConfig.DEFAULT_ACTIVATION_ENERGY;
        private Supplier<Double> preExponentialFactor = ChemConfig.DEFAULT_PRE_EXPONENTIAL_FACTOR;
        private Supplier<Double> gamePriorityBias = () -> 1.0;
        private Supplier<Double> minTemperature = ChemConfig.DEFAULT_MIN_TEMPERATURE;
        private Supplier<Double> maxTemperature = () -> Double.POSITIVE_INFINITY;
        private boolean quiet = false;
        private RuleFamily family = null;
        private Supplier<Double> electricalWorkPerMol = () -> 0.0;
        private Function<IChemicalContainer, Double> dynamicPriorityBias = null;
        private boolean isSelfLoop = false;
        private boolean equilibriumOverridden = false;
        private double pressureShiftDeltaHKj = 0.0;
        private double pressureShiftRefAtm = 1.0;

        public Builder(ResourceLocation id) {
            this.id = id;
        }

        public Builder reactant(IonType ion, int stoichiometry) {
            reactants.put(ion, stoichiometry);
            return this;
        }

        public Builder product(IonType ion, int stoichiometry) {
            products.put(ion, stoichiometry);
            return this;
        }

        public Builder precondition(IonType ion, int amount) {
            preconditions.put(ion, amount);
            return this;
        }

        public Builder deltaH(double val) { this.deltaH = () -> val; return this; }
        public Builder deltaG(double val) { this.deltaG = () -> val; return this; }
        public Builder equilibriumConstant(double val) { this.equilibriumConstant = () -> val; return this; }

        /**
         * 显式覆盖平衡常数（可逆玩法）：以手设 K° × 范特霍夫计算 K。
         * 不调用则走物理公式 K = exp(-ΔG(T)/(R·T))，ΔG(T) = ΔH - T·ΔS（由生成数据推出）。
         * 供 N₂O₄、SO₂/SO₃ 接触法、哈伯等作者手调平衡点的可逆反应使用。
         */
        public Builder equilibriumOverridden() {
            this.equilibriumOverridden = true;
            return this;
        }
        public Builder activationEnergy(double val) { this.activationEnergy = () -> val; return this; }
        public Builder preExponentialFactor(double val) { this.preExponentialFactor = () -> val; return this; }
        public Builder gamePriorityBias(double val) { this.gamePriorityBias = () -> val; return this; }
        public Builder minTemperature(double val) { this.minTemperature = () -> val; return this; }

        /**
         * 设置可运行温度上限（§19.17.1 温度窗口）：高于该值时优先级为 0 且执行被拦截。
         * 相变用它做对偶门槛——熔化要求 T ≥ T_m，凝固要求 T ≤ T_m − 迟滞，死区杜绝熔点附近抖动。
         */
        public Builder maxTemperature(double val) { this.maxTemperature = () -> val; return this; }

        /**
         * 让温度门槛随压强移动（§19.27 Stage 2，克劳修斯–克拉珀龙）。
         * 参考温度取本规则自己的 min/max 门槛值（= 1 atm 下的转变温度），参考压强 1 atm。
         *
         * <p>只有汽化/冷凝该传 L_v；固↔液<b>不要</b>传（缺 ΔV 数据，且水的熔化线斜率为负，
         * 硬套会得到相反符号——见 {@link ReactionMath#clausiusClapeyronTemperature}）。</p>
         */
        public Builder pressureShiftedTemperature(double latentHeatKjPerMol) {
            this.pressureShiftDeltaHKj = latentHeatKjPerMol;
            return this;
        }

        /** 相变等"安静规则"：不写失败诊断（§19.17.1）。 */
        public Builder quiet() { this.quiet = true; return this; }

        /**
         * 显式指定家族（§19.36）。不调用则派生：{@code quiet()} → {@link RuleFamily#PHASE}，
         * 其余 → {@link RuleFamily#CHEMICAL}；{@link RuleFamily#IONIZATION} 只能显式指定
         * （理由见 {@link RuleFamily} 的类注释）。
         */
        public Builder family(RuleFamily val) { this.family = val; return this; }

        /**
         * 设置动态优先级函数（蓝本 §5.2 浓度依赖反应）。
         * 以容器当前状态为自变量返回偏置系数，用于竞争反应按浓度选择主导路径。
         */
        public Builder dynamicPriorityBias(Function<IChemicalContainer, Double> fn) {
            this.dynamicPriorityBias = fn;
            return this;
        }

        /**
         * 设置电解所需电功（kJ/mol，蓝本 §17）。>0 表示该反应为电解反应，
         * 通电时引擎以 ΔG_eff = ΔG - W 参与优先级与平衡计算，并按 Δξ 消耗电功。
         */
        public Builder electricalWorkPerMol(double val) {
            this.electricalWorkPerMol = () -> val;
            return this;
        }

        /**
         * 自动以反应自由能作为理论最小电功（W = ΔG）。
         * 适用于电解等非自发反应：通电后 ΔG_eff = 0（临界可驱动）。
         */
        public Builder electricalWorkComputed() {
            this.electricalWorkPerMol = () -> deltaGOf(products) - deltaGOf(reactants);
            return this;
        }

        /**
         * 按能量守恒自动计算反应焓变：ΔH = ΣΔHf°(产物) - ΣΔHf°(反应物)
         * 数据来源为 IonType 的生成焓，符合蓝本 §8.3 硬约束（禁止手填经验值）
         */
        public Builder deltaHComputed() {
            this.deltaH = () -> deltaHOf(products) - deltaHOf(reactants);
            return this;
        }

        /**
         * 按能量守恒自动计算标准自由能变：ΔG = ΣΔGf°(产物) - ΣΔGf°(反应物)
         */
        public Builder deltaGComputed() {
            this.deltaG = () -> deltaGOf(products) - deltaGOf(reactants);
            return this;
        }

        private static double deltaHOf(Map<IonType, Integer> map) {
            double sum = 0;
            for (Map.Entry<IonType, Integer> e : map.entrySet()) {
                sum += e.getKey().getFormationEnthalpy() * e.getValue();
            }
            return sum;
        }

        private static double deltaGOf(Map<IonType, Integer> map) {
            double sum = 0;
            for (Map.Entry<IonType, Integer> e : map.entrySet()) {
                sum += e.getKey().getFormationGibbs() * e.getValue();
            }
            return sum;
        }

        public Builder selfLoop(boolean val) { this.isSelfLoop = val; return this; }

        // ====== 从配置读取（懒求值） ======
        /**
         * 记录"该字段来自配置"的路径，并返回懒求值 supplier。
         *
         * <p>{@code ConfigValue#getPath()} 返回 {@code List<String>}（已 javap 确认），拼成点分路径后
         * 交给 {@link ConfigSources}；导出工具据此写 {@code {"config": "chemistry.xxx"}} 而不是写死数字。</p>
         */
        private final Map<String, String> configSources = new HashMap<>();

        private Supplier<Double> fromConfig(String field, ForgeConfigSpec.DoubleValue config) {
            configSources.put(field, String.join(".", config.getPath()));
            return config::get;
        }

        public Builder deltaHFromConfig(ForgeConfigSpec.DoubleValue config) {
            this.deltaH = fromConfig("deltaH", config);
            return this;
        }
        public Builder deltaGFromConfig(ForgeConfigSpec.DoubleValue config) {
            this.deltaG = fromConfig("deltaG", config);
            return this;
        }
        public Builder equilibriumConstantFromConfig(ForgeConfigSpec.DoubleValue config) {
            this.equilibriumConstant = fromConfig("equilibriumConstant", config);
            return this;
        }
        public Builder activationEnergyFromConfig(ForgeConfigSpec.DoubleValue config) {
            this.activationEnergy = fromConfig("activationEnergy", config);
            return this;
        }
        public Builder preExponentialFactorFromConfig(ForgeConfigSpec.DoubleValue config) {
            this.preExponentialFactor = fromConfig("preExponentialFactor", config);
            return this;
        }
        public Builder gamePriorityBiasFromConfig(ForgeConfigSpec.DoubleValue config) {
            this.gamePriorityBias = fromConfig("gamePriorityBias", config);
            return this;
        }
        public Builder minTemperatureFromConfig(ForgeConfigSpec.DoubleValue config) {
            this.minTemperature = fromConfig("minTemperature", config);
            return this;
        }

        public ReactionRule build() {
            if (reactants.isEmpty() || products.isEmpty()) {
                throw new IllegalStateException("Reaction rule must have at least one reactant and one product");
            }
            // 自动检测是否为自环
            if (reactants.size() == 1 && products.size() == 1) {
                IonType reactant = reactants.keySet().iterator().next();
                IonType product = products.keySet().iterator().next();
                if (reactant.equals(product)) {
                    this.isSelfLoop = true;
                }
            }
            // 记录"哪些字段来自配置"（导出时写 {"config": ...} 而不是写死数字）
            ConfigSources.putAll(id, configSources);
            return new ReactionRule(this);
        }
    }
}