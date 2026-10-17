package com.pha.trainees.util.game.physics;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

/**
 * 动能数据存储和计算类
 */
public class KineticData {
    // NBT键名
    public static final String KEY_KINETIC_ENERGY = "KineticEnergy";
    public static final String KEY_LAST_POS_X = "LastPosX";
    public static final String KEY_LAST_POS_Y = "LastPosY";
    public static final String KEY_LAST_POS_Z = "LastPosZ";
    public static final String KEY_LAST_MOTION_X = "LastMotionX";
    public static final String KEY_LAST_MOTION_Y = "LastMotionY";
    public static final String KEY_LAST_MOTION_Z = "LastMotionZ";
    public static final String KEY_LAST_Y = "LastY";
    public static final String KEY_LAST_CHECK_TICK = "LastCheckTick";

    // 配置参数
    public static float KINETIC_DECAY_RATE = 1.5f; // 每检查时间衰减%
    public static float LINEAR_CONVERSION_FACTOR = 0.0025f; // 线性动能转换系数
    public static float GRAVITY_CONVERSION_FACTOR = 1.5f; // 重力转换系数k，当0.2，每米下落提供1%动能
    public static float DIRECTION_CHANGE_FACTOR = 5.0f; // 90度急转 = 5%动能
    public static float DAMAGE_CONVERSION_FACTOR = 15.0f; // 伤害转化系数
    public static int CHECK_INTERVAL_TICKS = 1; // 检查时间间隔（tick）
    public static float MAX_KINETIC_ENERGY = 250.0f; // 最大动能值
    public static final float BASE_MAX_KINETIC_ENERGY = 250.0f; // 基础最大动能值


    // 物理常数
    public static final float MASS = 50.0f; // 质量m = 50（单位：kg）
    public static final float GRAVITY = 10.0f; // g = 10 (单位：N/kg)
    public static final float MIN_FALL_DISTANCE = 1.0f; // 最小计算下落高度，低于this米忽略

    /*
      重力转换系数k的取值范围和建议值：

      根据公式：△动能% = (500 * k * h) / 100 = 5 * k * h

      1. 物理意义参考：
         - 在现实物理中，1kg物体下落1米获得约9.8J动能
         - 游戏中的动能百分比是抽象值，但需要保持平衡性

      2. 取值范围建议：
         - 最小值：0.0001（非常保守，每米下落仅0.05%动能）
         - 默认值：0.002（每米下落获得1%动能，与原设定接近）
         - 最大值：0.02（每米下落获得10%动能，适合高机动玩法）

      3. 与其他系数的关系：
         - 当k=0.2时，垂直下落与线性移动的比值合理
         - 当k>1.0时，垂直下落会成为主要动能来源
         - 需要配合MAX_VERTICAL_GAIN=70.0f限制
     */

    /*
     线性动能转换系数k的取值范围说明：
     公式：△动能% = k * | 1/2 * m * (v2² - v1²) |
     其中：m=50，v1和v2是检查时间开始和结束时的瞬时速度大小（m/s）

     取值范围建议：
     - 最小值：0.00001（非常保守）
     - 默认值：0.0001（平衡值）
     - 最大值：0.001（高增益）

     计算示例（v1=0, v2=5m/s）：
     △Ek = 0.5 * 50 * (25 - 0) = 625 J
     k=0.0001时：△动能% = 0.0625%
     k=0.001时：△动能% = 0.625%
     k=0.01时：△动能% = 6.25%
    */

    /**
     * 从NBT获取动能值
     */
    public static float getKineticEnergy(CompoundTag tag) {
        if (tag == null || !tag.contains(KEY_KINETIC_ENERGY)) {
            return 0.0f;
        }
        return tag.getFloat(KEY_KINETIC_ENERGY);
    }

    /**
     * 设置动能值到NBT
     */
    public static void setKineticEnergy(CompoundTag tag, float energy) {
        tag.putFloat(KEY_KINETIC_ENERGY, energy);
    }

    /**
     * 获取上次检查的位置
     */
    public static Vec3 getLastPosition(CompoundTag tag) {
        if (tag.contains(KEY_LAST_POS_X) && tag.contains(KEY_LAST_POS_Y) && tag.contains(KEY_LAST_POS_Z)) {
            return new Vec3(
                    tag.getDouble(KEY_LAST_POS_X),
                    tag.getDouble(KEY_LAST_POS_Y),
                    tag.getDouble(KEY_LAST_POS_Z)
            );
        }
        return null;
    }

    /**
     * 保存当前位置
     */
    public static void savePosition(CompoundTag tag, Vec3 position) {
        tag.putDouble(KEY_LAST_POS_X, position.x);
        tag.putDouble(KEY_LAST_POS_Y, position.y);
        tag.putDouble(KEY_LAST_POS_Z, position.z);
    }

    /**
     * 获取上次检查的速度
     */
    public static Vec3 getLastMotion(CompoundTag tag) {
        if (tag.contains(KEY_LAST_MOTION_X) && tag.contains(KEY_LAST_MOTION_Y) && tag.contains(KEY_LAST_MOTION_Z)) {
            return new Vec3(
                    tag.getDouble(KEY_LAST_MOTION_X),
                    tag.getDouble(KEY_LAST_MOTION_Y),
                    tag.getDouble(KEY_LAST_MOTION_Z)
            );
        }
        return null;
    }

    /**
     * 保存当前速度
     */
    public static void saveMotion(CompoundTag tag, Vec3 motion) {
        tag.putDouble(KEY_LAST_MOTION_X, motion.x);
        tag.putDouble(KEY_LAST_MOTION_Y, motion.y);
        tag.putDouble(KEY_LAST_MOTION_Z, motion.z);
    }

    /**
     * 保存上次检查的高度
     */
    public static void saveLastY(CompoundTag tag, double y) {
        tag.putDouble(KEY_LAST_Y, y);
    }

    /**
     * 获取上次检查的高度
     */
    public static double getLastY(CompoundTag tag) {
        return tag.getDouble(KEY_LAST_Y);
    }

    /**
     * 保存上次检查的tick
     */
    public static void saveLastCheckTick(CompoundTag tag, long tick) {
        tag.putLong(KEY_LAST_CHECK_TICK, tick);
    }

    /**
     * 获取上次检查的tick
     */
    public static long getLastCheckTick(CompoundTag tag) {
        return tag.getLong(KEY_LAST_CHECK_TICK);
    }

    /**
     * 判断是否需要检查（是否过了检查间隔）
     */
    public static boolean shouldCheck(CompoundTag tag, long currentTick) {
        long lastCheck = getLastCheckTick(tag);
        return currentTick - lastCheck >= CHECK_INTERVAL_TICKS;
    }



}