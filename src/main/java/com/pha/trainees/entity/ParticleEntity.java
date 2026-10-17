package com.pha.trainees.entity;

import com.pha.trainees.util.game.ParticleHelper;
import com.pha.trainees.util.game.Tools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.ToLongBiFunction;

public class ParticleEntity extends Entity {

//    private Vec3 moveDirection;  // 使用Vec3代替Direction
//    private int lifespan = 20 * 10;
//    private final float speed = 0.75f;
    private Vec3 startPos;
    private Vec3 endPos;
    private Vec3 controlPos;
    private float progress;          // 0.0 ~ 1.0
    private int totalLifespan;        // 总 tick 数（例如 80 ticks = 4 秒）
    private SimpleParticleType type = ParticleTypes.FLAME;



    public ParticleEntity(EntityType<?> type, Level level) {
        super(type, level);
        // 初始化默认值（调用工厂方法时会被覆盖）
        this.startPos = Vec3.ZERO;
        this.endPos = Vec3.ZERO;
        this.controlPos = Vec3.ZERO;
        this.progress = 0;
        this.totalLifespan = 80; // 默认 4 秒
    }



    /**
     * 初始化弧线路径
     * @param start 起点
     * @param end   终点
     * @param durationTicks 行走所需 tick 数（例如 60 ~ 100）
     */
    public void initArc(Vec3 start, Vec3 end, int durationTicks) {
        this.startPos = start;
        this.endPos = end;
        this.totalLifespan = durationTicks;
        this.progress = 0;
        // 随机生成控制点（产生弧线）
        this.controlPos = generateRandomControlPoint(start, end);
        // 将实体直接放在起点
        this.setPos(start.x, start.y, start.z);
    }

    /**
     * 随机生成控制点：取中点，加上一个垂直于起点->终点向量的随机偏移
     */
    private Vec3 generateRandomControlPoint(Vec3 start, Vec3 end) {
        Vec3 mid = start.add(end).scale(0.5);
        Vec3 dir = end.subtract(start);          // 方向向量
        double length = dir.length();

        // 如果起点==终点，则随机向任意方向偏移
        if (length < 1e-6) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double radius = 2.0; // 固定小偏移
            return mid.add(new Vec3(Math.cos(angle) * radius, Math.sin(angle) * radius, 0));
        }

        // 获取一个垂直于 dir 的随机向量
        Vec3 perp = getRandomPerpendicular(dir.normalize());
        // 偏移量：长度 * 随机因子（0.3 ~ 0.8）
        double offsetFactor = 0.3 + random.nextDouble() * 0.5;
        double offsetDistance = length * offsetFactor;
        return mid.add(perp.scale(offsetDistance));
    }

    /**
     * 返回一个垂直于给定单位向量的随机单位向量
     */
    private Vec3 getRandomPerpendicular(Vec3 unit) {
        // 生成一个不共线的随机向量
        Vec3 randomVec = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5);
        // 减去平行分量得到垂直向量，再归一化
        Vec3 perp = randomVec.subtract(unit.scale(randomVec.dot(unit)));
        if (perp.lengthSqr() < 1e-6) {
            // 如果随机向量恰好平行（极小概率），换一个方向
            return getRandomPerpendicular(unit);
        }
        return perp.normalize();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        // 进度更新
        progress += 1.0f / totalLifespan;
        if (progress >= 1.0f) {
            this.discard();
            return;
        }

        // 二次贝塞尔曲线计算当前位置
        double t = progress;
        double oneMinusT = 1 - t;
        Vec3 pos = startPos.scale(oneMinusT * oneMinusT)
                .add(controlPos.scale(2 * oneMinusT * t))
                .add(endPos.scale(t * t));
        this.setPos(pos.x, pos.y, pos.z);



        // 基础轨迹粒子（服务器广播）
        ParticleHelper.send(
                level(), ParticleTypes.FALLING_LAVA,
                getX(), getY(), getZ(), 3, 0.0, 0.0, 0.0, 0.0
        );

        ParticleHelper.send(
                level(), type,
                getX(), getY(), getZ(), 3, 0.0, 0.0, 0.0, 0.01
        );

        if (type == ParticleTypes.FLAME && Tools.chance(level(), 0.05)) {
            ParticleHelper.send(
                    level(), ParticleTypes.LAVA,
                    getX(), getY(), getZ(), 1, 0.0, 0.0, 0.0, 0.1
            );
        }


        ParticleHelper.send(
                level(), ParticleTypes.ENCHANT,
                getX(), getY(), getZ(), 3, 0.3, 0.3, 0.3, 0.5
        );

//
//        // 2. 本地增强轨迹（客户端生成）
//        // 圆形轨迹
//        ParticleHelper.spawnCircle(
//                level(),
//                ParticleTypes.ENCHANT,
//                getX(), getY() + 0.5, getZ(),
//                8,      // 数量
//                0.3,    // 半径
//                0.02    // 速度
//        );

//        // 自定义颜色粒子
//        ParticleHelper.spawnColoredDust(
//                level(),
//                getX(), getY(), getZ(),
//                0.2f, 0.8f, 1.0f,  // 青色
//                1.5f                // 大小
//        );
    }


    @Override
    protected void defineSynchedData() {
        // 不需要同步数据，留空
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // 从 NBT 标签读取数据（若需持久化数据）
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // 将数据写入 NBT 标签（若需持久化数据）
    }

    public SimpleParticleType getParticleType() {
        return type;
    }

    public void setParticleType(SimpleParticleType type) {
        this.type = type;
    }
}