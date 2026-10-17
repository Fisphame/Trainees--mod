package com.pha.trainees.util.game.physics;

import com.pha.trainees.util.game.EnchantmentCalculator;
import com.pha.trainees.util.game.Tools;
import com.pha.trainees.util.math.MathT;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import static com.pha.trainees.util.game.physics.KineticData.*;

/**
 * 动能计算系统
 */
public class KineticEnergySystem {

    /**
     * 更新物品的动能值
     * 只在服务器端调用
     */
    public static void updateKineticEnergy(Player player, ItemStack stack) {
        if (player.level().isClientSide) {
            return; // 只在服务器端计算
        }

        CompoundTag tag = stack.getOrCreateTag();
        long currentTick = player.level().getGameTime();

        // 检查是否到了检查时间
        if (!shouldCheck(tag, currentTick)) {
            return;
        }

        // 获取附魔后的最大动能值
        float maxEnergy = EnchantmentCalculator.getEffectiveMaxKineticEnergy(stack);

        // 获取当前动能
        float currentEnergy = getKineticEnergy(tag);

        // 获取上次检查的数据
        Vec3 lastPos = getLastPosition(tag);
        Vec3 lastMotion = getLastMotion(tag);
        double lastY = getLastY(tag);

        // 计算动能增量
        float energyGain = calculateEnergyGain(player, lastPos, lastMotion, lastY, stack);

        // 获取附魔后的衰减速率
        float decayRate = EnchantmentCalculator.getEffectiveDecayRate(stack);

        // 应用衰减（如果静止）
        if (isPlayerStationary(player)) {
            decayRate += KINETIC_DECAY_RATE;
        }

        float nowEnergy = currentEnergy + energyGain - decayRate;
        currentEnergy = MathT.inInterval(nowEnergy, 0, maxEnergy);

        // 保存到NBT - 这里使用实际值，不是百分比
        setKineticEnergy(tag, currentEnergy);

        // 保存当前数据供下次检查使用
        saveCurrentData(player, tag);
        saveLastCheckTick(tag, currentTick);

        // 设置回物品
        stack.setTag(tag);

    }

    /**
     * 计算动能增量
     */
    private static float calculateEnergyGain(Player player, Vec3 lastPos, Vec3 lastMotion, double lastY, ItemStack stack) {
        float gain = 0;

        // 1. 线性移动动能（公式：|△Ek| = | 1/2 * m * (v2² - v1²) | * k）
        gain += calculateLinearKineticGain(player, lastMotion, stack);

        // 2. 垂直下落动能（公式：△动能% = m * g * k * h / 100）
        gain += calculateVerticalGain(player, lastY, stack);

        // 3. 方向突变动能（90度以上急转弯每次增加5%动能）
        gain += calculateDirectionChangeGain(player, lastMotion);

        return gain;
    }

    /**
     * 计算线性移动动能增益
     * 公式：△动能% = k * | 1/2 * m * (v2² - v1²) |
     * 其中：m = 50, v1和v2是检查时间开始和结束时的速度大小
     */
    private static float calculateLinearKineticGain(Player player, Vec3 lastMotion, ItemStack stack) {
        if (lastMotion == null) {
            return 0.0f;
        }

        // 获取当前水平速度大小
        Vec3 currentMotion = player.getDeltaMovement();
        Vec3 currentHorizontal = new Vec3(currentMotion.x, 0, currentMotion.z);
        double v2 = currentHorizontal.length() * 20; // 转换为m/s

        // 获取上次检查的水平速度大小
        Vec3 lastHorizontal = new Vec3(lastMotion.x, 0, lastMotion.z);
        double v1 = lastHorizontal.length() * 20; // 转换为m/s

        // 计算动能变化量（焦耳）
        double deltaEk = 0.5 * MASS * (v2 * v2 - v1 * v1);

        // 获取应用附魔后的线性转换系数
        float effectiveFactor = EnchantmentCalculator.getEffectiveLinearFactor(stack);

        // 取绝对值并应用转换系数
        float gain = (float)(Math.abs(deltaEk) * effectiveFactor);

        return gain;
    }

    /**
     * 计算垂直下落动能增益
     * 公式：△动能% = (500 * k * h) / 100 = 5 * k * h
     * 其中：k为重力转换系数，h为检查时间内的下落高度（米）
     */
    public static float calculateVerticalGain(Player player, double lastY, ItemStack stack) {
        double fallDistance = Math.abs(lastY - player.getY()); // 单位：米
        if (fallDistance < MIN_FALL_DISTANCE) {
            return 0.0f;
        }

        // 获取应用附魔后的重力转换系数
        float effectiveGravityFactor = EnchantmentCalculator.getEffectiveGravityFactor(stack);

        // 使用公式：△动能% = (m * g * k * h) / 100
        float gain = MASS * GRAVITY * effectiveGravityFactor / 100.0f * (float)fallDistance;

        return gain;
    }


    /**
     * 计算方向突变动能增益
     */
    private static float calculateDirectionChangeGain(Player player, Vec3 lastMotion) {
        if (lastMotion == null || lastMotion.length() <= 0.01) {
            return 0.0f;
        }

        Vec3 currentMotion = player.getDeltaMovement();
        Vec3 currentHorizontal = new Vec3(currentMotion.x, 0, currentMotion.z);
        Vec3 lastHorizontal = new Vec3(lastMotion.x, 0, lastMotion.z);

        if (currentHorizontal.length() <= 0.01) {
            return 0.0f;
        }

        // 计算方向夹角（点积）
        double dot = currentHorizontal.normalize().dot(lastHorizontal.normalize());
        // 如果夹角大于90度（点积小于0）
        if (dot < 0) {
            return DIRECTION_CHANGE_FACTOR;
        }

        return 0.0f;
    }

    /**
     * 判断玩家是否静止
     */
    private static boolean isPlayerStationary(Player player) {
        Vec3 motion = player.getDeltaMovement();
        return motion.lengthSqr() < 0.01; // 速度很小视为静止
    }

    /**
     * 保存当前数据供下次检查使用
     */
    private static void saveCurrentData(Player player, CompoundTag tag) {
        savePosition(tag, player.position());
        saveMotion(tag, player.getDeltaMovement());
        saveLastY(tag, player.getY());
    }

    /**
     * 消耗所有动能，返回额外伤害倍数
     */
    public static float consumeKineticEnergy(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        float kineticEnergy = getKineticEnergy(tag);

        // 重置动能
        setKineticEnergy(tag, 0);
        stack.setTag(tag);

        // 获取附魔后的最大动能值和伤害转化系数
        float maxEnergy = EnchantmentCalculator.getEffectiveMaxKineticEnergy(stack);
        float damageFactor = EnchantmentCalculator.getEffectiveDamageFactor(stack);

        // 返回伤害倍率
        if (kineticEnergy > 0 && maxEnergy > 0) {
            // 可调整参数
            final float RATIO_WEIGHT = 1f;
            final float NONLINEAR_WEIGHT = 1.5f;
            final float TRANSITION_POINT = 0.65f;
            final float FAST_GROWTH_FACTOR = 2.37f;
            final float SLOW_GROWTH_SLOPE = 0.77f;
            final float SLOW_GROWTH_INTERCEPT = 0.5f;

            // 第一部分：线性部分
            float ratioPart = (kineticEnergy / maxEnergy) * damageFactor * RATIO_WEIGHT;

            // 第二部分：非线性部分
            float x = kineticEnergy / BASE_MAX_KINETIC_ENERGY;
            float nonlinearValue;

            if (x <= TRANSITION_POINT) {
                // 快速增长阶段：二次函数
                nonlinearValue = FAST_GROWTH_FACTOR * x * x;
            } else {
                // 缓慢增长阶段：线性函数
                nonlinearValue = SLOW_GROWTH_SLOPE * x + SLOW_GROWTH_INTERCEPT;
            }

            // 应用最大动能加成和权重
            float maxRatio = Math.max(maxEnergy / BASE_MAX_KINETIC_ENERGY - 1.0f, 0.0f);
            float absolutePart = nonlinearValue * maxRatio * NONLINEAR_WEIGHT;

            // 总伤害倍率
            return 1.0f + ratioPart + absolutePart;
        }
        return 1.0f;
    }

    /**
     * 获取当前动能值（0-附魔后的最大值）
     */
    public static float getKineticEnergyValue(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return 0.0f;
        }
        return getKineticEnergy(stack.getTag());
    }

    /**
     * 获取当前动能百分比（0-100）
     */
    public static float getKineticEnergyPercentage(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return 0.0f;
        }

        float energy = getKineticEnergy(stack.getTag());
        float maxEnergy = EnchantmentCalculator.getEffectiveMaxKineticEnergy(stack);

        if (maxEnergy <= 0) {
            return 0.0f;
        }

        return (energy / maxEnergy) * 100.0f;
    }

    /**
     * 获取附魔后的最大动能值（用于显示）
     */
    public static float getEffectiveMaxKineticEnergy(ItemStack stack) {
        return EnchantmentCalculator.getEffectiveMaxKineticEnergy(stack);
    }

    /*
     * 重力转换系数k的调整建议：
     *
     * 根据公式：△动能% = 5 * k * h
     *
     * 当前默认值k=0.2的效果：
     * - 下落1米：1%动能
     * - 下落10米：10%动能
     * - 下落35米：35%动能（受MAX_VERTICAL_GAIN=70限制）
     * - 下落70米：70%动能（达到最大）
     *
     * 可调整范围：
     * - k=0.01：下落100米仅5%动能（非常保守）
     * - k=0.1：下落10米获得5%动能（平衡偏弱）
     * - k=0.2：下落10米获得10%动能（默认，与原1%每格相近）
     * - k=0.5：下落10米获得25%动能（强调垂直机动）
     * - k=1.0：下落10米获得50%动能（高风险高回报）
     * - k=2.0：下落10米获得100%动能（极限玩法）
     */
    /*
     * 线性转换系数k的调整建议：
     *
     * 根据公式：△动能% = k * | 1/2 * m * (v2² - v1²) |
     * 其中：m = 50
     *
     * 1. 物理意义：
     *    - 在现实中，50kg物体从0加速到5m/s需要625J动能
     *    - 动能百分比是抽象值，需要平衡游戏性
     *
     * 2. 取值范围建议：
     *    - 最小值：0.00001（非常保守，625J对应0.00625%动能）
     *    - 默认值：0.0001（平衡，625J对应0.0625%动能）
     *    - 中等值：0.0005（625J对应0.3125%动能）
     *    - 高值：0.001（625J对应0.625%动能）
     *    - 最大值：0.01（625J对应6.25%动能）
     *
     * 3. 与重力系数的平衡：
     *    - 默认重力系数k=0.2，下落1米获得1%动能
     *    - 默认线性系数k=0.0001，从0加速到5m/s获得0.0625%动能
     *    - 这意味着垂直下落获取动能的效率大约是水平加速的16倍
     *    - 可以根据游戏风格调整这个比例
     */

}