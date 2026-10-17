package com.pha.trainees.chemistry.material;

import com.pha.trainees.chemistry.particle.IonType;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 一级物质注册表（§19.15 路线 C：少量"一级物质"做真物品/真流体）。
 *
 * <p><b>一级物质准入判据</b>（§19.15 定稿）：满足其一即一级——</p>
 * <ol>
 *   <li>元素单质（进入玩法的元素态物质）；</li>
 *   <li>跨系统接口：参与配方 / 需要桶与管道 / 需要经济与任务计价 / 玩家长期携带流通；</li>
 *   <li>教材级基础物（水、三酸两碱、常见溶剂燃料等）。</li>
 * </ol>
 *
 * <p><b>这里只提供脚手架</b>：内容层按批次把物质登记进来（登记一个 ItemStack 供给器即可）。
 * 用途：
 * <ul>
 *   <li>JEI 化学反应分类决定槽位呈现——有真物品就用 {@code ItemStack} 槽位（R/U 同时命中物品配方与化学方程式），
 *       没有才退回裸离子条目（§19.16 混用模型）；</li>
 *   <li>配方/市场/任务等系统按 {@link #itemFor} 取"该物质的代表物品"；</li>
 *   <li>规范表示：一级物品只是"带注册名的物质"，进出容器仍统一走同一 mapper（§19.15 风险提示）。</li>
 * </ul>
 *
 * <p>登记时机：模组初始化（静态注册）阶段。未登记的物质 = 二级（走 NBT 物质基类）。</p>
 */
public final class FirstClassSubstances {

    private FirstClassSubstances() {}

    private static final Map<IonType, Supplier<ItemStack>> REGISTRY = new LinkedHashMap<>();

    /**
     * 登记一个一级物质。
     *
     * @param ion   该物质对应的反应图节点（电中性物种；离子不应登记为一级）
     * @param stack 代表物品的供给器（延迟创建，避免注册阶段提前实例化 ItemStack）
     */
    public static void register(IonType ion, Supplier<ItemStack> stack) {
        if (ion == null || stack == null) return;
        REGISTRY.putIfAbsent(ion, stack);
    }

    /** 是否一级物质 */
    public static boolean isFirstClass(IonType ion) {
        return REGISTRY.containsKey(ion);
    }

    /** 该物质的代表物品（副本）；未登记返回 {@link Optional#empty()} */
    public static Optional<ItemStack> itemFor(IonType ion) {
        Supplier<ItemStack> supplier = REGISTRY.get(ion);
        if (supplier == null) return Optional.empty();
        ItemStack stack = supplier.get();
        return stack == null || stack.isEmpty() ? Optional.empty() : Optional.of(stack.copy());
    }

    /** 全部一级物质（只读视图，供 JEI / 配方 / 市场遍历） */
    public static Map<IonType, Supplier<ItemStack>> all() {
        return Collections.unmodifiableMap(REGISTRY);
    }
}
