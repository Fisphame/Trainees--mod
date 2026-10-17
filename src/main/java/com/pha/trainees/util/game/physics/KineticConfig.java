package com.pha.trainees.util.game.physics;

/**
 * 动能配置类，用于集中管理所有可配置参数
 */
public class KineticConfig {

    /**
     * 更新所有配置参数
     */
    public static void updateConfig(
            float decayRate,
            float linearConversionFactor,
            float gravityConversionFactor,
            float directionChangeFactor,
            float damageConversionFactor,
            int checkInterval
    ) {
        KineticData.KINETIC_DECAY_RATE = decayRate;
        KineticData.LINEAR_CONVERSION_FACTOR = linearConversionFactor;
        KineticData.GRAVITY_CONVERSION_FACTOR = gravityConversionFactor;
        KineticData.DIRECTION_CHANGE_FACTOR = directionChangeFactor;
        KineticData.DAMAGE_CONVERSION_FACTOR = damageConversionFactor;
        KineticData.CHECK_INTERVAL_TICKS = checkInterval;
    }

    /**
     * 获取当前配置的字符串表示
     */
    public static String getConfigString() {
        return String.format(
                "动能配置:\n" +
                        "  衰减速率: %.2f%%/检查\n" +
                        "  线性转换系数: %.5f (公式: k*|0.5*m*(v2²-v1²)|, m=50)\n" +
                        "  重力转换系数: %.3f (公式: m*g*k*h/100, m=50,g=10)\n" +
                        "  方向突变系数: %.2f%%/次\n" +
                        "  伤害转化系数: %.2f (满动能%.0f时伤害倍率: %.1f倍)\n" +
                        "  检查间隔: %d tick\n" +
                        "  最小下落计算高度: %.1f米\n" +
                        "  最大动能值: %.0f\n" +
                        "  \n" +
                        "物理公式参考:\n" +
                        "  线性动能%% = %.5f * | 0.5 * 50 * (v2² - v1²) |\n" +
                        "  垂直下落动能%% = (50*10*%.3f*h)/100 = %.3f * h\n" +
                        "  示例: 从0加速到5m/s = %.5f%%动能\n" +
                        "  示例: 下落10米 = %.1f%%动能",
                KineticData.KINETIC_DECAY_RATE,
                KineticData.LINEAR_CONVERSION_FACTOR,
                KineticData.GRAVITY_CONVERSION_FACTOR,
                KineticData.DIRECTION_CHANGE_FACTOR,
                KineticData.DAMAGE_CONVERSION_FACTOR,
                KineticData.MAX_KINETIC_ENERGY,
                1.0f + KineticData.DAMAGE_CONVERSION_FACTOR,
                KineticData.CHECK_INTERVAL_TICKS,
                KineticData.MIN_FALL_DISTANCE,
                KineticData.MAX_KINETIC_ENERGY,
                KineticData.LINEAR_CONVERSION_FACTOR,
                KineticData.GRAVITY_CONVERSION_FACTOR,
                50.0f * 10.0f * KineticData.GRAVITY_CONVERSION_FACTOR / 100.0f,
                KineticData.LINEAR_CONVERSION_FACTOR * 0.5 * 50 * 25, // 0.5*m*(v²), v=5m/s
                50.0f * 10.0f * KineticData.GRAVITY_CONVERSION_FACTOR / 100.0f * 10.0f
        );
    }
}