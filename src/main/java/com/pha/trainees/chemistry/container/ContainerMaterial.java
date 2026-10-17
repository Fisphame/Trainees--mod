package com.pha.trainees.chemistry.container;

/**
 * 容器材料档位（§6.3 材料分级，③-3）。
 *
 * <p><b>为什么需要它</b>：在此之前烧杯的耐温上限只有一个全局配置值（默认 1800 K），
 * 于是"玻璃烧杯能不能炼钢"这种问题不存在——所有物质都能用同一个容器做到任意温度。
 * 材料分级把**耐温上限**变成科技树的第一道硬墙（§19.23）：炼钢要 1600 K+，玻璃撑不住，
 * 必须先有石英/陶瓷坩埚。</p>
 *
 * <p><b>现实依据</b>（长期安全工作温度，非熔点）：硼硅玻璃 ≈500 ℃；耐热钢 ≈1400 ℃；
 * 石英玻璃 ≈1600 ℃ 且热震稳定性极好；氧化铝陶瓷 ≈2000 ℃。三个坩埚方块共用烧杯引擎，
 * 仅材料不同——所以只有这一张表是真源，方块只是给它挂一个名字。</p>
 *
 * @param id                档位 id（调试/展示用）
 * @param maxSafeTemperatureK 容器的安全温度上限 (K)：超过后温度被钳制（材质撑不住）
 * @param capacityScale     热容倍率（乘在全局基准热容上；越厚重升降温越慢）
 * @param transferScale     换热倍率（乘在全局换热系数上；金属导热最快）
 * @param maxSafePressureAtm 安全压力上限（atm，§19.31）：密闭时超过即泄压/破裂；
 *        玻璃远低于钢——现实里硼硅玻璃反应釜就是常压~微正压设备（≈2 atm），
 *        搪玻璃钢釜 6~10 atm，钢制高压釜更高。**敞口容器永远用不到它**（敞口恒为环境压）。
 */
public enum ContainerMaterial {

    /** 硼硅玻璃烧杯 / 玻璃反应釜：耐温最低、耐压最低，但透明可视 */
    GLASS("glass", 773.0, 1.0, 1.0, 2.0),
    /** 耐热钢坩埚 / 钢反应釜：导热快、热容小，能到炼钢区间，耐压最高 */
    METAL("metal", 1673.0, 0.8, 1.5, 10.0),
    /** 石英坩埚 / 石英反应釜：耐温高、热震稳定性好；石英玻璃耐压与玻璃同档略好 */
    QUARTZ("quartz", 1873.0, 1.3, 1.1, 3.0),
    /** 陶瓷（氧化铝）坩埚 / 陶瓷反应釜：耐温最高，厚重、升降温慢，耐压中等 */
    CERAMIC("ceramic", 2273.0, 1.6, 1.2, 5.0),
    /**
     * 搪玻璃（玻璃内衬钢釜，§19.32）：<b>玻璃的耐蚀 + 钢的耐压</b>，
     * 但耐温受玻璃衬层限制（现实 −20~260 ℃）、且衬层一旦被腐蚀透就<b>降级为钢基体</b>
     * （见 {@link CorrosionTable#degradedMaterial}）。
     */
    GLASS_LINED("glass_lined", 533.15, 1.2, 1.2, 8.0);

    private final String id;
    private final double maxSafeTemperatureK;
    private final double capacityScale;
    private final double transferScale;
    private final double maxSafePressureAtm;

    ContainerMaterial(String id, double maxSafeTemperatureK, double capacityScale,
                      double transferScale, double maxSafePressureAtm) {
        this.id = id;
        this.maxSafeTemperatureK = maxSafeTemperatureK;
        this.capacityScale = capacityScale;
        this.transferScale = transferScale;
        this.maxSafePressureAtm = maxSafePressureAtm;
    }

    public String id() {
        return id;
    }

    public double maxSafeTemperatureK() {
        return maxSafeTemperatureK;
    }

    public double capacityScale() {
        return capacityScale;
    }

    public double transferScale() {
        return transferScale;
    }

    /** 安全压力上限（atm，§19.31）：只在密闭时有意义 */
    public double maxSafePressureAtm() {
        return maxSafePressureAtm;
    }

    /** 该材料在给定温度下是否已经超出安全范围（诊断/警告用）。 */
    public boolean isOverheated(double temperatureK) {
        return temperatureK > maxSafeTemperatureK;
    }
}
