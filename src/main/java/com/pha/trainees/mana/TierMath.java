package com.pha.trainees.mana;

/**
 * 魔法机器的档位函数（§17.1，作者 2026-10-02 定稿）。
 *
 * <p>设计前提：**配方数据一律是初级(0 档)基准值**，运行时按档位换算——所以不需要为每个档位写一个配方类型
 * （参照格雷的"电压等级"）。三个式子（R = 初级输出能量、P = 初级副产物概率、T = 初级耗时 tick、x = 档位）：</p>
 * <ul>
 *   <li>输出能量 = <b>R · 2^x</b></li>
 *   <li>副产物概率 = <b>max(0, min(P + 5x² + 5x, 100))</b>（P 允许为负：初级"倒扣"、升档补回来）</li>
 *   <li>时间 = <b>max((5x² − 45x + 100) · T, 1)</b>；同一个多项式也是**富集仓所需 RE** 的倍率</li>
 * </ul>
 *
 * <table>
 *   <tr><th>x</th><th>档位</th><th>2^x</th><th>5x²+5x</th><th>5x²−45x+100</th></tr>
 *   <tr><td>0</td><td>初级</td><td>×1</td><td>+0%</td><td>×100</td></tr>
 *   <tr><td>1</td><td>中级</td><td>×2</td><td>+10%</td><td>×60</td></tr>
 *   <tr><td>2</td><td>高级</td><td>×4</td><td>+30%</td><td>×30</td></tr>
 *   <tr><td>3</td><td>终极</td><td>×8</td><td>+60%</td><td>×10</td></tr>
 *   <tr><td>4</td><td>创造</td><td>×16</td><td>+100%</td><td>×0 → 1 tick</td></tr>
 * </table>
 *
 * <p>纯函数、零 MC 依赖，由 {@code TierMathTest} 锁住（这些数字一旦漂移，所有配方的平衡都会跟着漂）。</p>
 */
public final class TierMath {

    /** 初级 = 0 档；创造 = 4 档（作者：创造档纯 mod 内不可获取，计划由整合包给）。 */
    public static final int MIN_TIER = 0;
    public static final int MAX_TIER = 4;

    private TierMath() {
    }

    /** 夹取到合法档位。 */
    public static int clampTier(int tier) {
        return Math.max(MIN_TIER, Math.min(MAX_TIER, tier));
    }

    /** 档位名（语言键）：JEI 右下角那行"当前档位：x 级"用。 */
    public static String tierNameKey(int tier) {
        return switch (clampTier(tier)) {
            case 0 -> "tier.trainees.primary";
            case 1 -> "tier.trainees.advanced";
            case 2 -> "tier.trainees.high";
            case 3 -> "tier.trainees.ultimate";
            default -> "tier.trainees.creative";
        };
    }

    /** 输出能量倍率 2^x。 */
    public static int energyMultiplier(int tier) {
        return 1 << clampTier(tier);
    }

    /** 副产物概率加成 5x² + 5x（百分点的加数）。 */
    public static int chanceBonus(int tier) {
        int x = clampTier(tier);
        return 5 * x * x + 5 * x;
    }

    /** 时间/能量倍率 5x² − 45x + 100（0 档 ×100、4 档 ×0）。 */
    public static int costAndTimeMultiplier(int tier) {
        int x = clampTier(tier);
        return 5 * x * x - 45 * x + 100;
    }

    /** 最终输出能量 = R · 2^x（用 long 中间量防溢出，再夹回 int 安全范围）。 */
    public static int energyOutput(int baseEnergy, int tier) {
        long value = (long) Math.max(0, baseEnergy) * energyMultiplier(tier);
        return (int) Math.min(Integer.MAX_VALUE, value);
    }

    /** 最终副产物概率 = max(0, min(P + 5x² + 5x, 100))，单位为百分点。 */
    public static int byproductChance(int baseChance, int tier) {
        int value = baseChance + chanceBonus(tier);
        return Math.max(0, Math.min(100, value));
    }

    /** 最终处理时间 = max((5x² − 45x + 100) · T, 1)，单位 tick（创造档就是 1 tick）。 */
    public static int processingTicks(int baseTicks, int tier) {
        long value = (long) Math.max(0, baseTicks) * costAndTimeMultiplier(tier);
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, value));
    }

    /**
     * 最终所需能量 = max((5x² − 45x + 100) · R, 0)（富集仓口径；提取机的"处理所需能量"同式）。
     *
     * <p>注意 4 档乘子是 0 → 创造档**不要钱**，这是作者明确要保留的效果（"保留0"）。</p>
     */
    public static int energyCost(int baseCost, int tier) {
        long value = (long) Math.max(0, baseCost) * costAndTimeMultiplier(tier);
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, value));
    }
}
