package com.pha.trainees.machine.essence;

import com.pha.trainees.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 精练精华网（§17.1 ④，"电网的对应物"）的**连通性扫描**。
 *
 * <p><b>为什么按需洪泛而不是维护全局图</b>：作者对定位的原话是"端口起到一个整合作用"——
 * 端口是唯一的聚合点。所以由端口发起扫描：沿**导线**（{@link Conductive}）走，沿途收集
 * **缓存单元**与**总线**；结果由端口自己缓存（默认 20 tick 重扫，或邻居变动时置脏）。
 * 这样不存在"拆一根线导致图不一致"的经典 bug，也不需要全局状态。</p>
 *
 * <p><b>缓存单元只贡献容量</b>：本 mod 的缓存单元是**没有方块实体的纯方块**
 * （见 {@code ManaCacheUnitBlock}），它给祭坛扩容的方式就是"贴着数数量"。
 * 所以这里同样只**数数量**：端口容量 = 自身基础 + 连到的缓存单元数 × {@code cacheUnitCapacity}，
 * 而 **RE 存在端口自己身上**（"这些缓存单元视为端口的容量"）。</p>
 *
 * <p>安全阀：单次扫描最多访问 {@link #MAX_VISITED} 个方块（防卡服装置），超限即停止并标记
 * {@code truncated}，由调用方决定怎么提示。</p>
 */
public final class EssenceNetwork {

    /** 单次扫描访问上限（导线 + 端点）。 */
    public static final int MAX_VISITED = 4096;

    /** 导线方块：网络沿它传播（导线、网络扩展接口都实现它）。 */
    public interface Conductive {
    }

    /** 总线：输入/输出/无线 都实现它（网络只负责"找得到"，语义由总线自己实现）。 */
    public interface Bus {
    }

    /** 扫描结果：连到的缓存单元位置 + 总线位置 + 是否因为超限被截断。 */
    public record Result(List<BlockPos> cacheUnits, List<BlockPos> buses, boolean truncated) {
        public static Result empty() {
            return new Result(List.of(), List.of(), false);
        }

        public int cacheUnitCount() {
            return cacheUnits.size();
        }
    }

    private EssenceNetwork() {
    }

    /**
     * 从 {@code origin} 洪泛，收集连通的缓存单元与总线。
     *
     * <p>起点自身不计入（起点是端口/扩展接口）；六个方向都走（网络不分面）。
     * 缓存单元与总线是**端点**：收集到就不再从它们继续传播（它们不是导线）。</p>
     */
    public static Result scan(Level level, BlockPos origin) {
        List<BlockPos> cacheUnits = new ArrayList<>();
        List<BlockPos> buses = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();

        for (Direction side : Direction.values()) {
            queue.add(origin.relative(side));
        }
        boolean truncated = false;

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            if (!visited.add(pos)) continue;
            if (visited.size() > MAX_VISITED) {
                truncated = true;
                break;
            }
            if (!level.isLoaded(pos)) continue;

            if (level.getBlockState(pos).is(ModBlocks.MANA_CACHE_UNIT.get())) {
                cacheUnits.add(pos);
                continue;   // 端点：不再往外传
            }

            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof Bus) {
                buses.add(pos);
                continue;   // 端点
            }

            if (level.getBlockState(pos).getBlock() instanceof Conductive) {
                for (Direction side : Direction.values()) {
                    BlockPos next = pos.relative(side);
                    if (!visited.contains(next)) {
                        queue.add(next);
                    }
                }
            }
        }
        return new Result(cacheUnits, buses, truncated);
    }

    /** 饱和加法：两个容量相加不会溢出成负数（巨型网络会碰到）。 */
    public static int saturatingAdd(int a, int b) {
        long sum = (long) a + (long) b;
        return sum > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, sum);
    }

    /** 饱和乘法（缓存单元数 × 单个体容量）。 */
    public static int saturatingMultiply(int count, int perUnit) {
        long product = (long) count * (long) perUnit;
        return product > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, product);
    }
}
