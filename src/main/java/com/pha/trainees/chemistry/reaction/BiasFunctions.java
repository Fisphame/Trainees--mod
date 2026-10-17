package com.pha.trainees.chemistry.reaction;

import com.pha.trainees.Main;
import com.pha.trainees.chemistry.data.BiasFunctionRegistry;
import com.pha.trainees.chemistry.particle.IonType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 引擎自带的**命名偏置函数**（§17.1 选择性公开：这类参数无法用数字表达，只能留在引擎里）。
 *
 * <p>为什么单独一个类：同一个函数要供**两处**使用——</p>
 * <ol>
 *   <li>规则定义（{@code ModChemistry} 里 {@code .dynamicPriorityBias(id, fn)}）</li>
 *   <li>命名函数注册表（{@code BiasFunctionRegistry}，供数据包按 id 引用）</li>
 * </ol>
 *
 * <p>集中放这里，能保证"规则用的函数"与"注册的函数"是**同一个实例**，不会出现
 * "规则里改了表达式但注册表里还是旧的"这种漂移。</p>
 */
public final class BiasFunctions {

    /** 低酸路径：{@code 4/(4+[H⁺])}，随酸度升高而减弱（碳酸盐 → 碳酸氢盐）。 */
    public static final ResourceLocation CARBONATE_LOW_ACID =
            new ResourceLocation(Main.MODID, "carbonate_low_acid_bias");

    /** 高酸路径：{@code [H⁺]/(4+[H⁺])}，随酸度升高而占优（碳酸盐 → CO₂）。 */
    public static final ResourceLocation CARBONATE_HIGH_ACID =
            new ResourceLocation(Main.MODID, "carbonate_high_acid_bias");

    private static final double HALF_SATURATION = 4.0;

    /** 函数实例 → 注册 id 的反查表（用身份比较，避免 lambda 的 equals 语义问题）。 */
    private static final java.util.Map<Function<com.pha.trainees.chemistry.container.IChemicalContainer, Double>,
            ResourceLocation> BY_INSTANCE = java.util.Collections.synchronizedMap(new java.util.IdentityHashMap<>());

    private BiasFunctions() {
    }

    /**
     * 反查某个偏置函数注册时用的 id（导出工具用）。
     *
     * <p>为什么要反查而不是在规则上存一个 id 字段：这样**注册表本身就是唯一真相**——
     * 规则只要用 {@link #lowAcid}/{@link #highAcid} 这两个函数，导出就一定能拿到 id；
     * 忘了登记的函数自然拿不到 id，导出器会报错（契约要求不静默丢信息）。</p>
     *
     * @return 已登记的 id；未登记返回 null
     */
    @org.jetbrains.annotations.Nullable
    public static ResourceLocation idOf(
            @org.jetbrains.annotations.Nullable Function<com.pha.trainees.chemistry.container.IChemicalContainer, Double> fn) {
        return fn == null ? null : BY_INSTANCE.get(fn);
    }

    /** 登记并记录反查（供 {@link #registerAll} 内部使用）。 */
    private static void register(ResourceLocation id,
                                 Function<com.pha.trainees.chemistry.container.IChemicalContainer, Double> fn) {
        BiasFunctionRegistry.register(id, fn);
        BY_INSTANCE.put(fn, id);
    }

    /** 低酸优先（[H⁺] 越高越弱）。 */
    public static double lowAcidBias(double hPlusConcentration) {
        return HALF_SATURATION / (HALF_SATURATION + hPlusConcentration);
    }

    /** 高酸优先（[H⁺] 越高越强）。 */
    public static double highAcidBias(double hPlusConcentration) {
        return hPlusConcentration / (HALF_SATURATION + hPlusConcentration);
    }

    /**
     * 供规则定义使用的闭包（**单实例**：第一次调用时创建并缓存，之后返回同一个对象）。
     *
     * <p>为什么必须缓存：导出器用"函数实例 → id"的反查（{@link #idOf}），
     * 若每次调用都新建 lambda，规则持有的实例与注册的实例就不是同一个，反查必然落空
     * （这一版就是这么错的，导出仍报"无法表达"）。缓存后两者是同一实例。</p>
     */
    private static java.util.function.Function<com.pha.trainees.chemistry.container.IChemicalContainer, Double> lowAcidFn;
    private static java.util.function.Function<com.pha.trainees.chemistry.container.IChemicalContainer, Double> highAcidFn;
    private static Supplier<IonType> hPlusSupplier;

    public static synchronized Function<com.pha.trainees.chemistry.container.IChemicalContainer, Double> lowAcid(
            Supplier<IonType> hPlus) {
        if (lowAcidFn == null) {
            hPlusSupplier = hPlus;
            lowAcidFn = container -> lowAcidBias(container.getEffectiveConcentration(hPlusSupplier.get()));
        }
        return lowAcidFn;
    }

    public static synchronized Function<com.pha.trainees.chemistry.container.IChemicalContainer, Double> highAcid(
            Supplier<IonType> hPlus) {
        if (highAcidFn == null) {
            hPlusSupplier = hPlus;
            highAcidFn = container -> highAcidBias(container.getEffectiveConcentration(hPlusSupplier.get()));
        }
        return highAcidFn;
    }

    /**
     * 把本轮所有具名函数登记到 {@link BiasFunctionRegistry}（引擎启动时调用一次）。
     *
     * @param hPlus 氢离子（由注册表提供，避免这个类反向依赖 ModChemistry）
     */
    public static void registerAll(Supplier<IonType> hPlus) {
        register(CARBONATE_LOW_ACID, lowAcid(hPlus));
        register(CARBONATE_HIGH_ACID, highAcid(hPlus));
    }
}
