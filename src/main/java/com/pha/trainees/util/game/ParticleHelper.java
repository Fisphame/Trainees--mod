package com.pha.trainees.util.game;

import com.pha.trainees.entity.ParticleEntity;
import com.pha.trainees.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.awt.*;

public class ParticleHelper {
    /**
     * 生成一个基础粒子
     */
    public static void spawn(Level level, ParticleOptions particleType,
                                          double x, double y, double z,
                                          double vx, double vy, double vz) {
        if (level.isClientSide()) {
            level.addParticle(particleType, x, y, z, vx, vy, vz);
        }
    }
    /**
     * 生成自定义颜色的灰尘粒子
     * @param size 粒子大小
     *             <p>
     *             r, g, b 颜色分量 (0.0-1.0)
     */
    public static void spawnColoredDust(Level level, double x, double y, double z,
                                        float r, float g, float b, float size) {
        if (level.isClientSide()) {
            Vector3f color = new Vector3f(r, g, b);
            DustParticleOptions options = new DustParticleOptions(color, size);
            level.addParticle(options, x, y, z, 0, 0, 0);
        }
    }
    public static void spawnColoredDust(Level level, double x, double y, double z, double vx, double vy, double vz,
                                        float r, float g, float b, float size) {
        if (level.isClientSide()) {
            Vector3f color = new Vector3f(r, g, b);
            DustParticleOptions options = new DustParticleOptions(color, size);
            level.addParticle(options, x, y, z, vx, vy, vz);
        }
    }
    /**
     * 生成颜色渐变的灰尘粒子
     * @param level 世界实例
     * fromR, fromG, fromB 起始颜色
     * toR, toG, toB 结束颜色
     * @param size 粒子大小
     */
    public static void spawnGradientDust(Level level, double x, double y, double z,
                                         float fromR, float fromG, float fromB,
                                         float toR, float toG, float toB,
                                         float size) {
        if (level.isClientSide()) {
            Vector3f fromColor = new Vector3f(fromR, fromG, fromB);
            Vector3f toColor = new Vector3f(toR, toG, toB);
            DustColorTransitionOptions options =
                    new DustColorTransitionOptions(fromColor, toColor, size);
            level.addParticle(options, x, y, z, 0, 0, 0);
        }
    }
    public static void spawnGradientDust(Level level, double x, double y, double z, double vx, double vy, double vz,
                                         float fromR, float fromG, float fromB,
                                         float toR, float toG, float toB,
                                         float size) {
        if (level.isClientSide()) {
            Vector3f fromColor = new Vector3f(fromR, fromG, fromB);
            Vector3f toColor = new Vector3f(toR, toG, toB);
            DustColorTransitionOptions options =
                    new DustColorTransitionOptions(fromColor, toColor, size);
            level.addParticle(options, x, y, z, vx, vy, vz);
        }
    }
    /**
     * 生成圆形分布的粒子
     * @param level 世界实例
     * @param particleType 粒子类型
     * <p>
     * centerX, centerY, centerZ 圆心坐标
     * @param count 粒子数量
     * @param radius 圆半径
     * @param speed 粒子向外扩散的速度
     */
    public static void spawnCircle(Level level, ParticleOptions particleType,
                                            double centerX, double centerY, double centerZ,
                                            int count, double radius, double speed) {
        if (level.isClientSide()) {
            for (int i = 0; i < count; i++) {
                double angle = (2 * Math.PI * i) / count;
                double x = centerX + Math.cos(angle) * radius;
                double z = centerZ + Math.sin(angle) * radius;

                double vx = Math.cos(angle) * speed;
                double vz = Math.sin(angle) * speed;

                send(level, particleType, x, centerY, z, 1, 0.0, 0.0, 0.0, vx);

                level.addParticle(particleType, x, centerY, z, vx, 0, vz);
            }
        }
    }
    /**
     * 生成球形分布的粒子
     * @param level 世界实例
     * @param particleType 粒子类型
     * <P></P>
     * centerX, centerY, centerZ 球心坐标
     * @param count 粒子数量
     * @param radius 球半径
     * @param speed 粒子向外扩散的速度
     */
    public static void spawnSphere(Level level, ParticleOptions particleType,
                                            double centerX, double centerY, double centerZ,
                                            int count, double radius, double speed) {
        if (level.isClientSide()) {
            for (int i = 0; i < count; i++) {
                // 球面坐标
                double theta = 2 * Math.PI * Math.random();
                double phi = Math.acos(2 * Math.random() - 1);

                double x = centerX + radius * Math.sin(phi) * Math.cos(theta);
                double y = centerY + radius * Math.cos(phi);
                double z = centerZ + radius * Math.sin(phi) * Math.sin(theta);

                // 从中心向外扩散的速度
                double vx = Math.sin(phi) * Math.cos(theta) * speed;
                double vy = Math.cos(phi) * speed;
                double vz = Math.sin(phi) * Math.sin(theta) * speed;

                level.addParticle(particleType, x, y, z, vx, vy, vz);
            }
        }
    }
    /**
     * 生成连接两点的线条状粒子
     * @param level 世界实例
     * @param particleType 粒子类型
     * <p></p>
     * startX, startY, startZ 起点坐标
     * endX, endY, endZ 终点坐标
     * @param count 粒子数量（粒子数量 = 线条段数 + 1）
     * @param speed 粒子沿线条移动的速度
     */
    public static void spawnLine(Level level, ParticleOptions particleType,
                                          double startX, double startY, double startZ,
                                          double endX, double endY, double endZ,
                                          int count, double speed) {
        if (level.isClientSide()) {
            int segments = count - 1;
            double dx = (endX - startX) / segments;
            double dy = (endY - startY) / segments;
            double dz = (endZ - startZ) / segments;

            for (int i = 0; i < segments; i++) {
                double x = startX + dx * i;
                double y = startY + dy * i;
                double z = startZ + dz * i;

                level.addParticle(particleType, x, y, z, dx * speed, dy * speed, dz * speed);
            }
        }
    }
    /**
     * 生成方块破坏粒子
     * @param state 方块状态
     */
    public static void spawnBlockBreak(Level level, double x, double y, double z,
                                                BlockState state) {
        if (level.isClientSide()) {
            level.addParticle(
                    new BlockParticleOption(ParticleTypes.BLOCK, state),
                    x, y, z,
                    0, 0, 0
            );
        }
    }

    /**
     * 生成物品掉落粒子
     * @param stack 物品堆
     */
    public static void spawnItemDrop(Level level, double x, double y, double z,
                                          ItemStack stack) {
        if (level.isClientSide()) {
            level.addParticle(
                    new ItemParticleOption(ParticleTypes.ITEM, stack),
                    x, y, z,
                    0, 0, 0
            );
        }
    }
    /**
     * 可视化扇形区域（生成粒子效果）
     */
    public static void visualizeSector(Level level, Player player, double maxDistance, float angleDegrees,
                                       int particleCount) {
        if (level.isClientSide()) {
            // 在扇形区域边缘生成粒子
            float halfAngle = angleDegrees / 2.0f;

            for (int i = 0; i < particleCount; i++) {
                // 计算当前粒子的角度
                float currentAngle = -halfAngle + (halfAngle * 2 * i / (float) particleCount);

                // 将角度转换为弧度
                double angleRad = Math.toRadians(- currentAngle + player.getYRot());

                // 计算粒子位置
                double x = player.getX() + Math.sin(angleRad) * maxDistance;
                double z = player.getZ() + Math.cos(angleRad) * maxDistance;
                double y = player.getY() + 0.5; // 玩家高度

                // 生成粒子
                level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,
                        x, y, z, 0, 0, 0);
            }
        }
    }

    /**
     * 快捷生成一些粒子
     */
    public static void spawnParticles(Level level, BlockPos pos) {
        if (level == null || !level.isClientSide()) return;

        double centerX = pos.getX() + 0.5;
        double centerY = pos.getY() + 1.0;
        double centerZ = pos.getZ() + 0.5;

        for (int i = 0; i < 10; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 1.5;
            double offsetY = level.random.nextDouble() * 1.5;
            double offsetZ = (level.random.nextDouble() - 0.5) * 1.5;

            spawn(level, ParticleTypes.ENCHANT,
                    centerX + offsetX, centerY + offsetY, centerZ + offsetZ,
                    0, 0, 0
            );
        }
    }
    /**
     * 服务器发送粒子给所有玩家
     * @param level 世界实例
     * @param particleType 粒子类型
     * <p>
     * x, y, z 中心位置
     * @param count 粒子数量
     * <p>
     * dx, dy, dz 随机偏移范围
     * @param speed 基础速度
     */
    public static void send(Level level, ParticleOptions particleType,
                                      double x, double y, double z,
                                      int count, double dx, double dy, double dz,
                                      double speed) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    particleType,
                    x, y, z,
                    count,
                    dx, dy, dz,
                    speed
            );
        }
    }
    public static void sendSurfaces(Level level, ParticleOptions particleType, BlockPos pos, int count,
                                   double dx, double dy, double dz, double speed){
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            double x = pos.getX(), y = pos.getY(), z = pos.getZ();
            double spread = 0.2D;
            send(serverLevel, particleType, x + 0.5, y + 0.5, z - spread, count, dx, dy, 0, speed);
            send(serverLevel, particleType, x + 0.5, y - speed, z + 0.5, count, dx, 0, dz, speed);
            send(serverLevel, particleType, x - speed, y + 0.5, z + 0.5, count, 0, dy, dz, speed);
            send(serverLevel, particleType, x + 0.5, y + 0.5, z + 1.0 + speed, count, dx, dy, 0, speed);
            send(serverLevel, particleType, x + 0.5, y + 1.0 + speed, z + 0.5, count, dx, 0, dz, speed);
            send(serverLevel, particleType, x + 1.0 + speed, y + 0.5, z + 0.5, count, 0, dy, dz, speed);
        }
    }
    public static void sendGradientDust(Level level, double x, double y, double z, double dx, double dy, double dz,
                                                            int count,
                                                            float fromR, float fromG, float fromB,
                                                            float toR, float toG, float toB,
                                                            float size, double speed) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            Vector3f fromColor = new Vector3f(fromR, fromG, fromB);
            Vector3f toColor = new Vector3f(toR, toG, toB);
            DustColorTransitionOptions options =
                    new DustColorTransitionOptions(fromColor, toColor, size);
            serverLevel.sendParticles(
                    options,
                    x, y, z,
                    count,
                    dx, dy, dz,
                    speed
            );
        }
    }
    /**
     * 服务器发送粒子给特定玩家
     */
    public static void sendToPlayer(
                                     ServerPlayer player,
                                     ParticleOptions particleType,
                                     double x, double y, double z,
                                     int count, double dx, double dy, double dz,
                                     double speed, boolean force) {
        player.connection.send(
                new ClientboundLevelParticlesPacket(
                        particleType,
                        force,  // 是否强制显示
                        x, y, z,
                        (float)dx, (float)dy, (float)dz,
                        (float)speed,
                        count
                )
        );
    }
    /**
     * 爆炸效果
     */
    public static void spawnExplosionEffect(Level level, double x, double y, double z,
                                            float size) {
        // 爆炸核心
        send(level, ParticleTypes.EXPLOSION, x, y, z, 1, 0, 0, 0, 0);

        // 爆炸烟雾
        send(level, ParticleTypes.EXPLOSION_EMITTER, x, y, z,
                (int)(size * 20), size, size, size, 0.5);

        // 烟雾扩散
        spawnSphere(level, ParticleTypes.SMOKE, x, y, z,
                (int)(size * 50), size * 2, 0.1);
    }
    /**
     * 火焰效果
     */
    public static void spawnFireEffect(Level level, double x, double y, double z,
                                       float intensity) {
        int count = (int)(intensity * 20);

        // 火焰粒子
        for (int i = 0; i < count; i++) {
            double offsetX = (Math.random() - 0.5) * intensity;
            double offsetY = Math.random() * intensity * 2;
            double offsetZ = (Math.random() - 0.5) * intensity;

            double vx = (Math.random() - 0.5) * 0.02;
            double vy = Math.random() * 0.05 + 0.02;
            double vz = (Math.random() - 0.5) * 0.02;

            level.addParticle(ParticleTypes.FLAME,
                    x + offsetX, y + offsetY, z + offsetZ,
                    vx, vy, vz);
        }

        // 烟雾
        for (int i = 0; i < count / 2; i++) {
            double offsetX = (Math.random() - 0.5) * intensity * 1.5;
            double offsetY = Math.random() * intensity * 2.5;
            double offsetZ = (Math.random() - 0.5) * intensity * 1.5;

            level.addParticle(ParticleTypes.SMOKE,
                    x + offsetX, y + offsetY, z + offsetZ,
                    0, 0.01, 0);
        }
    }
    /**
     * 魔法效果（适合附魔、技能等）
     */
    public static void spawnMagicEffect(Level level, double x, double y, double z,
                                        float r, float g, float b) {
        if (level.isClientSide()) {
            // 中心闪烁
            spawnColoredDust(level, x, y, z, r, g, b, 2.0f);

            // 圆形扩散
            spawnCircle(level, ParticleTypes.ENCHANT,
                    x, y, z, 16, 1.0, 0.05);

            // 上升粒子
            for (int i = 0; i < 10; i++) {
                double offsetX = (Math.random() - 0.5) * 0.5;
                double offsetZ = (Math.random() - 0.5) * 0.5;

                Vector3f color = new Vector3f(r, g, b);
                level.addParticle(
                        new DustParticleOptions(color, 1.0f),
                        x + offsetX, y, z + offsetZ,
                        0, 0.1 + Math.random() * 0.1, 0
                );
            }
        }
    }
    /**
     * 轨迹效果（如飞行物轨迹）
     */
    public static void spawnTrailEffect(Level level, Vec3 start, Vec3 end,
                                        ParticleOptions particleType,
                                        int density, double spacing) {
        if (level.isClientSide()) {
            Vec3 direction = end.subtract(start).normalize();
            double distance = start.distanceTo(end);
            int steps = (int)(distance / spacing);

            for (int i = 0; i <= steps; i++) {
                double t = (double)i / steps;
                Vec3 pos = start.add(direction.scale(t * distance));

                // 在轨迹上生成粒子
                for (int j = 0; j < density; j++) {
                    double offsetX = (Math.random() - 0.5) * 0.2;
                    double offsetY = (Math.random() - 0.5) * 0.2;
                    double offsetZ = (Math.random() - 0.5) * 0.2;

                    double vx = (Math.random() - 0.5) * 0.01;
                    double vy = (Math.random() - 0.5) * 0.01;
                    double vz = (Math.random() - 0.5) * 0.01;

                    level.addParticle(particleType,
                            pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                            vx, vy, vz);
                }
            }
        }
    }
    /**
     * 获取随机颜色
     */
    public static Vector3f getRandomColor() {
        return new Vector3f(
                (float)Math.random(),
                (float)Math.random(),
                (float)Math.random()
        );
    }
    /**
     * 获取彩虹颜色（根据时间变化）
     */
    public static Vector3f getRainbowColor(long time) {
        float hue = (System.currentTimeMillis() % 6000) / 6000.0f;
        Color color = Color.getHSBColor(hue, 1.0f, 1.0f);
        return new Vector3f(
                color.getRed() / 255.0f,
                color.getGreen() / 255.0f,
                color.getBlue() / 255.0f
        );
    }

    public static void spawnArcParticle(Level level, SimpleParticleType particleType, Vec3 start, Vec3 end) {
        if (level.isClientSide) return; // 仅在服务端生成实体

        // 获取实体类型（你需要提前注册 ParticleEntity 的类型）
        EntityType<ParticleEntity> type = ModEntities.PARTICLE_ENTITY.get(); // 根据你的注册方式调整

        ParticleEntity entity = new ParticleEntity(type, level);
        entity.setParticleType(particleType);
        // 随机决定行走时间（1~3 秒，20 ticks/秒）
        int duration = 20 + level.random.nextInt(20); // 40~80 ticks
        entity.initArc(start, end, duration);

        level.addFreshEntity(entity);
    }

    public static void spawnArcParticle(Level level, SimpleParticleType particleType, Vec3 start, Vec3 end, int baseTime, int randomTime) {
        if (level.isClientSide) return; // 仅在服务端生成实体

        // 获取实体类型（你需要提前注册 ParticleEntity 的类型）
        EntityType<ParticleEntity> type = ModEntities.PARTICLE_ENTITY.get(); // 根据你的注册方式调整

        ParticleEntity entity = new ParticleEntity(type, level);
        entity.setParticleType(particleType);
        // 随机决定行走时间（1~3 秒，20 ticks/秒）
        int duration = baseTime + level.random.nextInt(randomTime);
        entity.initArc(start, end, duration);

        level.addFreshEntity(entity);
    }
}
