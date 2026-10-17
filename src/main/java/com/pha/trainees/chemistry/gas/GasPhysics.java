package com.pha.trainees.chemistry.gas;

import com.pha.trainees.config.ChemConfig;

/**
 * 气相基础物理（#10 / 蓝本 §19.27）：理想气体总压与"环境压下的最大容纳量"。
 *
 * <p><b>为什么压强不是新状态量</b>：容器与网格格都只存"各组分摩尔数 + 温度 + 体积"，
 * 总压由 {@code nRT/V} 导出——所以引入总压<b>不需要改任何存档格式</b>。
 * 真正新增的是"用它作驱动力"的项（超压喷出）与它的后果（危险层 / T_b(P)）。</p>
 *
 * <p><b>单位坑</b>：V 用升(L)、R = 8.314 J/(mol·K) 时 {@code nRT/V} 直接得到 kPa
 * （1 J/L = 1 kPa），再除以 101.325 才是标准大气压。历史事故：早期代码误除以 101325，
 * 结果小 1000 倍（见 {@code BeakerBlockEntity.getPressure()} 注释）。</p>
 *
 * <p><b>总压的作用域</b>：只有<b>密闭</b>空间里总压才是独立状态量。敞口容器与开放网格格的
 * 总压恒等于环境压，其输运完全由分压差（菲克扩散）决定——网格里不存在"总压对流"这个自由度。</p>
 *
 * <p><b>可测性</b>：带 ambient 参数的重载是纯函数，单元测试只用它们；
 * 不带参数的重载才去读 {@link ChemConfig}（Forge 配置在单测里未加载，读它会抛异常）。</p>
 */
public final class GasPhysics {

    private GasPhysics() {}

    /** 理想气体常数 J/(mol·K) */
    public static final double R = 8.314;

    /** 网格格固定体积（升）：1 格 = 1 m³ = 1000 L（§19.13） */
    public static final double CELL_VOLUME_L = 1000.0;

    /** 1 标准大气压 = 101.325 kPa */
    public static final double KPA_PER_ATM = 101.325;

    /** 低于此值视为零（与体系其他地方的 1e-9 口径一致） */
    public static final double EPS = 1e-9;

    /** 理想气体总压（atm）；气相摩尔数或体积不足时返回 0 */
    public static double pressureAtm(double gasMoles, double volumeLiters, double tempK) {
        if (gasMoles < EPS || volumeLiters < EPS) return 0;
        double kpa = (gasMoles * R * Math.max(0, tempK)) / volumeLiters;
        return kpa / KPA_PER_ATM;
    }

    /** 环境压强（atm，可配）。网格里未建模的"空气"就是靠它隐式表达的。 */
    public static double ambientAtm() {
        return ChemConfig.AMBIENT_PRESSURE_ATM.get();
    }

    /**
     * 环境压下、体积 V、温度 T 时容器能容纳的最大气体摩尔数（mol）。
     * 气相超过此量即为"相对环境超压"——开盖时应当喷出的正是这个超出部分。
     */
    public static double molesAtAmbient(double volumeLiters, double tempK) {
        return molesAtAmbient(volumeLiters, tempK, ambientAtm());
    }

    /** {@link #molesAtAmbient(double, double)} 的纯函数版本（ambient 由调用方给出，便于单测） */
    public static double molesAtAmbient(double volumeLiters, double tempK, double ambientAtm) {
        if (volumeLiters < EPS) return 0;
        return ambientAtm * KPA_PER_ATM * volumeLiters / (R * Math.max(1, tempK));
    }

    /** 相对环境压的比值（1.0 = 与环境压持平；>1 超压，<1 负压）；环境压非法时返回 0 */
    public static double overpressureRatio(double pressureAtm) {
        return overpressureRatio(pressureAtm, ambientAtm());
    }

    /** {@link #overpressureRatio(double)} 的纯函数版本 */
    public static double overpressureRatio(double pressureAtm, double ambientAtm) {
        return ambientAtm < EPS ? 0 : pressureAtm / ambientAtm;
    }
}
