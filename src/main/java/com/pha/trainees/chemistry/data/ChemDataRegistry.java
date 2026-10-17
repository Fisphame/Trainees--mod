package com.pha.trainees.chemistry.data;

import com.pha.trainees.chemistry.particle.IonType;
import com.pha.trainees.chemistry.reaction.ReactionRule;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据驱动加载出来的化学内容（§17.1/§19：选择性公开的"实例"层）。
 *
 * <p><b>定位</b>：引擎（机制）公开，内容（实例）私有。本类只保存"从数据包读进来的东西"，
 * 不含任何硬编码物质——所以公开仓库里可以存在本类，却没有任何具体内容。</p>
 *
 * <p><b>与既有硬编码内容的关系</b>（迁移期）：现有定义仍在 {@code ModChemistry.ModIons} 里，
 * 本注册表是**并存的第二来源**。迁移批次 3 会把硬编码定义导出为数据包，
 * 之后本注册表成为唯一来源（届时再做切换，切换点集中在一处）。</p>
 */
public final class ChemDataRegistry {

    /** 诊断级别：WARN 不影响加载，ERROR 会跳过该条目。 */
    public enum Severity {
        WARN,
        ERROR
    }

    /** 一条加载诊断（文件 + 字段 + 说明），供内容作者自检。 */
    public record Problem(Severity severity, ResourceLocation file, String field, String message) {
        @Override
        public String toString() {
            return "[" + severity + "] " + file + (field.isEmpty() ? "" : "#" + field) + ": " + message;
        }
    }

    private static final Map<ResourceLocation, IonType> IONS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, ReactionRule> RULES = new LinkedHashMap<>();
    /** 危险杂质映射：物质 id 字符串 → 缺陷类型名（打包机判定"不合格"时用；来自 chemistry/hazard_kinds.json）。 */
    private static final Map<String, String> HAZARD_KINDS = new LinkedHashMap<>();
    private static final List<Problem> PROBLEMS = new ArrayList<>();

    private ChemDataRegistry() {
    }

    /** 登记一条"物质 → 缺陷类型"。 */
    public static void putHazardKind(String ionId, String kind) {
        HAZARD_KINDS.put(ionId, kind);
    }

    /** 查某物质的缺陷类型名；没有则 null（调用方优雅降级为"不合格"，不崩）。 */
    @Nullable
    public static String hazardKind(String ionId) {
        return HAZARD_KINDS.get(ionId);
    }

    public static int hazardKindCount() {
        return HAZARD_KINDS.size();
    }

    /** 产品杂质上限覆盖：产品 id → 上限规则（来自 chemistry/product_limits.json，属于内容）。 */
    private static final Map<String, java.util.List<com.pha.trainees.chemistry.spec.FractionRule>> PRODUCT_LIMITS =
            new LinkedHashMap<>();

    public static void putProductLimits(String productId,
                                        java.util.List<com.pha.trainees.chemistry.spec.FractionRule> limits) {
        PRODUCT_LIMITS.put(productId, java.util.List.copyOf(limits));
    }

    /** 查某产品的杂质上限覆盖；没有则 null（调用方用代码里的默认值）。 */
    @Nullable
    public static java.util.List<com.pha.trainees.chemistry.spec.FractionRule> productLimits(String productId) {
        return PRODUCT_LIMITS.get(productId);
    }

    /** 开始一次加载（清空内容与诊断；由加载器调用）。 */
    public static void beginReload() {
        IONS.clear();
        RULES.clear();
        HAZARD_KINDS.clear();
        PROBLEMS.clear();
    }

    public static void putIon(IonType ion) {
        IONS.put(ion.getId(), ion);
    }

    public static void putRule(ReactionRule rule) {
        RULES.put(rule.getId(), rule);
    }

    public static void addProblem(Problem problem) {
        PROBLEMS.add(problem);
    }

    public static void addProblems(Collection<Problem> problems) {
        PROBLEMS.addAll(problems);
    }

    @Nullable
    public static IonType ion(ResourceLocation id) {
        return IONS.get(id);
    }

    public static Collection<IonType> ions() {
        return Collections.unmodifiableCollection(IONS.values());
    }

    public static Collection<ReactionRule> rules() {
        return Collections.unmodifiableCollection(RULES.values());
    }

    public static List<Problem> problems() {
        return Collections.unmodifiableList(PROBLEMS);
    }

    public static int ionCount() {
        return IONS.size();
    }

    public static int ruleCount() {
        return RULES.size();
    }
}
