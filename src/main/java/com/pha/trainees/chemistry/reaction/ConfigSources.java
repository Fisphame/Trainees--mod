package com.pha.trainees.chemistry.reaction;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 反应规则的**配置来源登记表**（§17.1 选择性公开：把"这个数值来自配置"记录下来）。
 *
 * <p><b>为什么需要它</b>：{@code ReactionRule.Builder} 里的 {@code *FromConfig(config)} 只把
 * {@code config::get} 存成 {@code Supplier<Double>}，规则对外只暴露**数值**。
 * 于是导出成数据时只能写死一个数字 ✗ —— 之后改配置不再生效（隐蔽的坏）。
 * 这个表把"字段 → 配置路径"记下来，导出器据此写 {@code {"config": "chemistry.xxx"}}。</p>
 *
 * <p><b>为什么用静态表而不是给规则加字段</b>：这样不必动 {@code ReactionRule} 的字段与构造函数
 * （改动面最小）；这张表只在"配置驱动的规则"上写入少量条目，纯开发/导出用途。</p>
 */
public final class ConfigSources {

    /** 规则 id → (字段名 → 配置路径)。 */
    private static final Map<ResourceLocation, Map<String, String>> BY_RULE = new ConcurrentHashMap<>();

    private ConfigSources() {
    }

    /** 登记一条规则的配置来源（空 map 视为清除）。由 {@code ReactionRule.Builder#build()} 调用。 */
    public static void putAll(ResourceLocation ruleId, @Nullable Map<String, String> sources) {
        if (sources == null || sources.isEmpty()) {
            BY_RULE.remove(ruleId);
            return;
        }
        BY_RULE.put(ruleId, Map.copyOf(sources));
    }

    /** 某个字段是否来自配置；是则返回配置路径（点分），否则 null。 */
    @Nullable
    public static String get(ResourceLocation ruleId, String field) {
        Map<String, String> sources = BY_RULE.get(ruleId);
        return sources == null ? null : sources.get(field);
    }

    /** 该规则全部"来自配置"的字段（字段名 → 路径）；没有则空 map。 */
    public static Map<String, String> all(ResourceLocation ruleId) {
        Map<String, String> sources = BY_RULE.get(ruleId);
        return sources == null ? Map.of() : sources;
    }
}
