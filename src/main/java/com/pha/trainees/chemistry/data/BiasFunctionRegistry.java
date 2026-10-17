package com.pha.trainees.chemistry.data;

import com.pha.trainees.chemistry.container.IChemicalContainer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * 命名偏置函数注册表（契约见 {@code docs/CHEMISTRY_DATA_SCHEMA.md} §3.2）。
 *
 * <p><b>为什么需要它</b>：现行引擎里有一种参数无法用数字表达——{@code dynamicPriorityBias} 是
 * <b>以容器状态为自变量的函数</b>（例如"酸度越高越优先"）。JSON 表达不了函数，于是契约规定：</p>
 * <ul>
 *   <li><b>函数体属于机制</b>（在本仓库、公开）：按 id 注册在这里；</li>
 *   <li><b>数据只引用 id</b>：{@code "priority": { "function": "trainees:aqueous_acid_bias" } }；</li>
 *   <li><b>未注册的 id 直接报错</b>，不静默降级（静默降级会让内容作者以为生效了）。</li>
 * </ul>
 *
 * <p>这条划分正是"机制公开 / 实例私有"能成立的关键：<b>能表达为数字的进数据，不能表达的进引擎。</b></p>
 */
public final class BiasFunctionRegistry {

    private static final Map<ResourceLocation, Function<IChemicalContainer, Double>> FUNCTIONS =
            new LinkedHashMap<>();

    private BiasFunctionRegistry() {
    }

    /** 注册一个命名偏置函数（引擎侧调用；幂等覆盖并返回旧值）。 */
    @Nullable
    public static Function<IChemicalContainer, Double> register(
            ResourceLocation id, Function<IChemicalContainer, Double> function) {
        return FUNCTIONS.put(id, function);
    }

    @Nullable
    public static Function<IChemicalContainer, Double> get(ResourceLocation id) {
        return FUNCTIONS.get(id);
    }

    public static boolean isRegistered(ResourceLocation id) {
        return FUNCTIONS.containsKey(id);
    }

    public static Set<ResourceLocation> ids() {
        return Collections.unmodifiableSet(FUNCTIONS.keySet());
    }
}
