package com.pha.trainees.chemistry.heat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 活跃热源登记表（§19.38 A2）：让"本模组的热源"能被**气体网格**看见。
 *
 * <p><b>为什么必须是登记表，而不是去扫方块实体</b>：{@code GasGridManager.heatOffset} 可能在
 * {@code ChunkEvent.Load} 内被调用，那里**绝不能**碰方块实体或强制加载区块（历史自死锁事故）。
 * 登记表是一张纯内存表：方块实体在加载/卸载/状态变化时自己登记，扫描方只读表——
 * 于是"热源驱动气体"与"不碰区块"两件事不再冲突。</p>
 *
 * <p>键按 {@link Level} 实例（客户端/服务端各自的维度单例），用弱引用表避免世界重载泄漏。
 * 值用 {@link Collections#synchronizedMap} 与并发安全的内部表：区块反序列化可能在工作线程上
 * 触发 {@code onLoad}。</p>
 */
public final class HeatSourceTracker {

    /** 一条登记：温度（K）与是否在工作（关机/没介质 → 不参与）。 */
    public record Entry(double temperatureK, boolean active) {
    }

    /** 纯数学部分用的样本：位置 + 温度 + 是否工作。 */
    public record Sample(BlockPos pos, double temperatureK, boolean active) {
    }

    /** 每个 Level 一张 pos → 登记的表（弱键：世界重载后自动释放）。 */
    private static final Map<Level, Map<BlockPos, Entry>> BY_LEVEL =
            Collections.synchronizedMap(new WeakHashMap<>());

    private HeatSourceTracker() {
    }

    /** 登记或更新一个热源（方块实体的 onLoad / 每 tick / 玩家改设定点时调用）。 */
    public static void update(Level level, BlockPos pos, double temperatureK, boolean active) {
        if (level == null) return;
        Map<BlockPos, Entry> table = BY_LEVEL.computeIfAbsent(level, ignored -> new java.util.concurrent.ConcurrentHashMap<>());
        table.put(pos.immutable(), new Entry(temperatureK, active));
    }

    /** 注销（方块实体被移除 / 区块卸载时调用）。 */
    public static void unregister(Level level, BlockPos pos) {
        if (level == null) return;
        Map<BlockPos, Entry> table = BY_LEVEL.get(level);
        if (table != null) table.remove(pos);
    }

    /**
     * 以 {@code query} 为中心，取"绝对值最大"的一条热源贡献（J/K 量纲的温差），
     * 没有热源时返回 0。**带符号**：冷源给出负值——这正是 §19.38 A2 要修的那件事
     * （旧实现 {@code if (contrib > best)} 只保留正贡献，冰浴对气体完全无效）。
     *
     * @param level   查询所在维度
     * @param query   查询格
     * @param envTemp 环境温度（K），贡献 = (T_源 − T_环境) × exp(−d/λ)
     * @param radius  扫描半径（格）
     * @param lambda  距离衰减常数（格）
     */
    public static double contribution(Level level, BlockPos query, double envTemp, int radius, double lambda) {
        Map<BlockPos, Entry> table = BY_LEVEL.get(level);
        if (table == null || table.isEmpty()) return 0;
        List<Sample> samples = new ArrayList<>();
        for (Map.Entry<BlockPos, Entry> e : table.entrySet()) {
            BlockPos pos = e.getKey();
            if (Math.abs(pos.getX() - query.getX()) > radius
                    || Math.abs(pos.getY() - query.getY()) > radius
                    || Math.abs(pos.getZ() - query.getZ()) > radius) {
                continue;
            }
            samples.add(new Sample(pos, e.getValue().temperatureK(), e.getValue().active()));
        }
        return bestOffset(samples, query, envTemp, lambda);
    }

    /**
     * 纯数学：从样本里取"绝对值最大的温差贡献"（带符号）。
     *
     * @param samples 附近热源样本（可含不工作的）
     * @param query   查询格
     * @param envTemp 环境温度（K）
     * @param lambda  距离衰减常数（格）
     * @return 最大 |贡献| 的那个贡献（带符号）；没有可用热源时 0
     */
    public static double bestOffset(Collection<Sample> samples, BlockPos query, double envTemp, double lambda) {
        double best = 0;
        for (Sample sample : samples) {
            if (sample == null || !sample.active()) continue;
            double dx = sample.pos().getX() - query.getX();
            double dy = sample.pos().getY() - query.getY();
            double dz = sample.pos().getZ() - query.getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance <= 1e-9) continue;   // 自己那格由容器自身逻辑处理
            double contrib = (sample.temperatureK() - envTemp) * Math.exp(-distance / lambda);
            if (Math.abs(contrib) > Math.abs(best)) best = contrib;
        }
        return best;
    }

    /** 仅供调试/测试：某维度当前登记数量。 */
    public static int size(Level level) {
        Map<BlockPos, Entry> table = BY_LEVEL.get(level);
        return table == null ? 0 : table.size();
    }
}
