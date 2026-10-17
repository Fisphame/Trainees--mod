package com.pha.trainees.chemistry.material;

import com.pha.trainees.chemistry.particle.IonType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 定组成物质表（§19.15 一级物质的"真物品"路线）。
 *
 * <p><b>为什么需要它</b>：{@link SubstanceItem} 原有的两条 NBT 路径（{@code blueprint} / {@code composition}）
 * 服务的是"成分运行时才知道"的物质——矿石、海水、动态混合物。而三酸两碱、常见溶液、单质这类
 * <b>组成固定</b>的物质应当是<b>真物品</b>：FTB 任务判据（{@code itemfilters:tag} 匹配不了 NBT）、
 * kubejs、市场计价、JEI 分组都按物品 id 走。</p>
 *
 * <p><b>两条路并存</b>：本表登记的物品在其自身 NBT 之外提供成分；{@code SubstanceItem.getComposition}
 * 先查本表，未命中才回落 NBT。因此一级物质的物品进出容器仍走同一 mapper
 * （§19.15："一级物品只是带注册名的物质"）。</p>
 *
 * <p><b>不受难度乘数影响</b>：与 {@link SubstanceBlueprint} 不同，本表成分是绝对值，
 * 不会被 {@code valuableMultiplier / gangueMultiplier} 缩放——溶液浓度不该随难度档位漂移。</p>
 *
 * <p><b>约定</b>：1 个物品 = 表里登记的摩尔数（当前登记均为 1.0 mol），便于手算与测试对齐。</p>
 *
 * <p><b>线程</b>：只在 commonSetup 登记（服务端逻辑初始化），运行期只读。</p>
 */
public final class FixedCompositionTable {

    private FixedCompositionTable() {}

    /** 摩尔数比较容差（相对），与规格判定同源口径 */
    private static final double EPS = 1e-9;

    /** 一条登记：物品 → 固定成分 */
    public record Entry(Item item, Map<IonType, Double> composition) {}

    private static final Map<Item, Entry> BY_ITEM = new LinkedHashMap<>();
    private static final List<Entry> ENTRIES = new ArrayList<>();

    /** 登记一个定组成物质；同一物品重复登记时忽略后者。 */
    public static void register(Item item, Map<IonType, Double> composition) {
        if (item == null || composition == null || composition.isEmpty()) return;
        if (BY_ITEM.containsKey(item)) return;
        Entry entry = new Entry(item, Map.copyOf(composition));
        BY_ITEM.put(item, entry);
        ENTRIES.add(entry);
    }

    /** 该物品是否为已登记的定组成物质 */
    public static boolean contains(Item item) {
        return item != null && BY_ITEM.containsKey(item);
    }

    /** 物品 → 成分（未登记返回 empty） */
    public static Optional<Map<IonType, Double>> compositionOf(Item item) {
        Entry entry = item == null ? null : BY_ITEM.get(item);
        return entry == null ? Optional.empty() : Optional.of(entry.composition());
    }

    /**
     * 成分 → 物品（精确匹配，容差 {@link #EPS} 相对）。
     * 用途：从容器取出的纯样本若能对应某个一级物质，就还原成那个真物品，而不是退化成 NBT 混合物。
     */
    public static Optional<ItemStack> itemFor(Map<IonType, Double> composition) {
        if (composition == null || composition.isEmpty()) return Optional.empty();
        for (Entry entry : ENTRIES) {
            if (sameComposition(entry.composition(), composition)) {
                return Optional.of(new ItemStack(entry.item()));
            }
        }
        return Optional.empty();
    }

    /** 全部登记（只读视图，供 JEI / 调试遍历） */
    public static List<Entry> all() {
        return Collections.unmodifiableList(ENTRIES);
    }

    private static boolean sameComposition(Map<IonType, Double> a, Map<IonType, Double> b) {
        if (a.size() != b.size()) return false;
        for (Map.Entry<IonType, Double> e : a.entrySet()) {
            Double other = b.get(e.getKey());
            if (other == null) return false;
            double x = e.getValue();
            double y = other;
            if (Math.abs(x - y) > EPS * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)))) return false;
        }
        return true;
    }
}
