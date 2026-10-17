package com.pha.trainees.chemistry.spec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;

import static com.pha.trainees.chemistry.spec.SpecResult.RuleResult.Kind;
import static com.pha.trainees.chemistry.spec.SpecResult.RuleResult.Status;

/**
 * 规格判定纯逻辑层（§19.18 日化品 / 打包机）。不引用任何 Minecraft 类，可在 JUnit 下直接测。
 *
 * <p><b>键空间契约</b>：{@code molesById} 的 key 与 {@link FractionRule#ionId()} 是<b>同一套物种 id
 * 字符串</b>（统一使用完整形式 {@code namespace:path} 最安全）。判定不做"匹配"，每条规则只有一次
 * {@code Map.get}：map 是"容器里此刻有什么"的事实，规则是"规格关心什么"的提问，join key 就是那个
 * 相同的 id 字符串。不要把规则拿去遍历 map（既慢，又会漏掉"规则要的物种根本不存在"这种情况）。</p>
 *
 * <p><b>质量分数</b>：{@code fraction(i) = moles(i) × M(i) / totalMass}，分母是<b>全容器</b>总质量，
 * 含未被任何规则提及的水与杂质。所以必须先扫一遍 map 求出总量，再逐条评规则；
 * 未申报物种只影响 {@link SpecResult#othersFraction()}，不影响 verdict。</p>
 *
 * <p><b>判定优先级</b>（所有规则都会被评估并填进 {@code details}，不提前退出）：
 * <ol>
 *   <li>任一规则 {@link Status#UNKNOWN_MASS}（规则引用的 id 解析不出摩尔质量，或与 map 的命名空间
 *       写法不一致）→ REJECTED（失败关闭，避免"限值被静默跳过"）</li>
 *   <li>required / limits 任一不 OK → REJECTED（质量优先于数量：成分不合格比量不够更该被玩家看到）</li>
 *   <li>{@code usableMoles < bottleMoles} → NOT_ENOUGH</li>
 *   <li>premium 非空且全部 OK → PREMIUM，否则 PASS</li>
 * </ol></p>
 *
 * <p><b>异常与状态的边界</b>：异常只用于"事实侧/调用侧"的编程错误（map 里的摩尔数非法、map 里真实
 * 存在的物种取不到摩尔质量、参数为 null）；"数据侧"的错误（规格里写了不存在的 id）以状态码暴露，
 * 让打包机可以照常拒绝出货而不是抛异常崩在玩家右键上。</p>
 */
public final class SpecMatcher {

    /** 分数比较容差：实际分数在 1e-2 量级，1e-9 远低于任何真实批次噪声。 */
    public static final double EPS = 1e-9;

    private SpecMatcher() {
    }

    /**
     * 判定一份溶液是否符合规格。
     *
     * @param spec        规格（可先用 {@link #validate(ProductSpec)} 做结构校验）
     * @param molesById   容器内容物：物种 id → 摩尔数（空容器传空 Map）
     * @param molarMassOf 摩尔质量查询（g/mol）；查不到请返回 ≤ 0 或 NaN，不要抛异常
     * @param usableMoles 容器中可灌装的溶液总量（mol），用于瓶量门槛
     * @return 判定结果；任何情况下都不返回 null
     */
    public static SpecResult evaluate(ProductSpec spec,
                                      Map<String, Double> molesById,
                                      ToDoubleFunction<String> molarMassOf,
                                      double usableMoles) {
        if (spec == null) throw new IllegalArgumentException("spec 不能为 null");
        if (molesById == null) throw new IllegalArgumentException("molesById 不能为 null（空容器请传空 Map）");
        if (molarMassOf == null) throw new IllegalArgumentException("molarMassOf 不能为 null");
        if (!Double.isFinite(usableMoles) || usableMoles < 0.0) {
            throw new IllegalArgumentException("usableMoles 必须为非负有限值，实际 " + usableMoles);
        }

        List<FractionRule> required = requireRules(spec.required(), "required");
        List<FractionRule> limits = requireRules(spec.limits(), "limits");
        List<FractionRule> premium = requireRules(spec.premium(), "premium");

        // 规格声明的物种 = required ∪ limits ∪ premium；其余计入 othersFraction
        Set<String> declared = new LinkedHashSet<>();
        addIds(declared, required);
        addIds(declared, limits);
        addIds(declared, premium);

        // ① 扫 map 一次：总摩尔数、总质量、未申报质量（顺带做事实侧卫生检查）
        double totalMoles = 0.0;
        double totalMass = 0.0;
        double othersMass = 0.0;
        for (Map.Entry<String, Double> entry : molesById.entrySet()) {
            String id = entry.getKey();
            Double value = entry.getValue();
            if (id == null || id.isBlank()) throw new IllegalArgumentException("molesById 含空物种 id");
            if (value == null) throw new IllegalArgumentException("物种 " + id + " 的摩尔数为 null");
            double moles = value;
            if (!Double.isFinite(moles) || moles < 0.0) {
                throw new IllegalArgumentException("物种 " + id + " 的摩尔数非法: " + moles);
            }
            if (moles == 0.0) continue;
            double molarMass = resolveMolarMass(molarMassOf, id);
            totalMoles += moles;
            double mass = moles * molarMass;
            totalMass += mass;
            if (!declared.contains(id)) othersMass += mass;
        }

        // 空容器 / 零质量：没有可判定的内容（也不做 0/0）
        if (totalMass <= 0.0) {
            return new SpecResult(Verdict.REJECTED, List.of(), totalMoles, 0.0, 0.0);
        }

        // ② id 约定审计表：key 的"去命名空间"形式 → 真实 key，用于发现命名空间写法不一致
        Map<String, String> keyByPath = new HashMap<>();
        for (Map.Entry<String, Double> entry : molesById.entrySet()) {
            Double value = entry.getValue();
            if (value != null && value > 0.0) {
                keyByPath.putIfAbsent(pathOf(entry.getKey()), entry.getKey());
            }
        }

        // ③ 逐条评规则：按规格顺序遍历规则（不遍历 map），顺序 = required → limits → premium
        List<SpecResult.RuleResult> details =
                new ArrayList<>(required.size() + limits.size() + premium.size());
        for (FractionRule rule : required) {
            details.add(evaluateRule(rule, Kind.REQUIRED, molesById, keyByPath, totalMass, molarMassOf));
        }
        for (FractionRule rule : limits) {
            details.add(evaluateRule(rule, Kind.LIMIT, molesById, keyByPath, totalMass, molarMassOf));
        }
        for (FractionRule rule : premium) {
            details.add(evaluateRule(rule, Kind.PREMIUM, molesById, keyByPath, totalMass, molarMassOf));
        }

        // ④ 汇总
        Verdict verdict = aggregate(details, usableMoles, spec.bottleMoles());
        return new SpecResult(verdict, List.copyOf(details), totalMoles, totalMass, othersMass / totalMass);
    }

    /**
     * 规格自身的结构合法性校验（返回错误清单，空 = 合法）。
     * 只校验规格内部一致性：<b>id 在注册表里是否存在属于数据包加载期的另一层校验</b>（那里才拿得到注册表）。
     */
    public static List<String> validate(ProductSpec spec) {
        List<String> errors = new ArrayList<>();
        if (spec == null) {
            errors.add("spec 为 null");
            return List.copyOf(errors);
        }
        if (isBlank(spec.id())) errors.add("spec.id 为空");
        if (isBlank(spec.goodItem())) errors.add("spec.goodItem 为空");
        if (!Double.isFinite(spec.bottleMoles()) || spec.bottleMoles() <= 0.0) {
            errors.add("spec.bottleMoles 必须为正的有限值，实际 " + spec.bottleMoles());
        }

        List<FractionRule> required = validateRuleList(spec.required(), "required", errors);
        List<FractionRule> limits = validateRuleList(spec.limits(), "limits", errors);
        List<FractionRule> premium = validateRuleList(spec.premium(), "premium", errors);

        if (required.isEmpty() && limits.isEmpty()) {
            errors.add("required 与 limits 同时为空：规格必须有牙齿（至少一项门槛）");
        }
        if (!premium.isEmpty() && isBlank(spec.premiumItem())) {
            errors.add("声明了 premium 窗口但 premiumItem 为空");
        }
        // 优质档必须是有效成分窗口的"更窄子区间"（§19.18）
        for (FractionRule p : premium) {
            for (FractionRule r : required) {
                if (!r.ionId().equals(p.ionId())) continue;
                if (p.minFraction() < r.minFraction() - EPS) {
                    errors.add("premium 窗口不得宽于 required: " + p.ionId()
                            + " premium.min=" + p.minFraction() + " < required.min=" + r.minFraction());
                }
                if (p.maxFraction() > r.maxFraction() + EPS) {
                    errors.add("premium 窗口不得宽于 required: " + p.ionId()
                            + " premium.max=" + p.maxFraction() + " > required.max=" + r.maxFraction());
                }
            }
        }
        return List.copyOf(errors);
    }

    /** 测试/数据生成器辅助：按质量分数与摩尔质量反算摩尔数（g / (g/mol)）。 */
    public static double molesFor(double fraction, double totalMass, double molarMass) {
        if (!Double.isFinite(molarMass) || molarMass <= 0.0) {
            throw new IllegalArgumentException("molarMass 必须为正的有限值，实际 " + molarMass);
        }
        return totalMass * fraction / molarMass;
    }

    // ==================== 内部实现 ====================

    /** 事实侧（map 里真实存在的物种）取不到合法摩尔质量 → 注册表/调用侧的编程错误，直接抛。 */
    private static double resolveMolarMass(ToDoubleFunction<String> molarMassOf, String id) {
        double molarMass = molarMassOf.applyAsDouble(id);
        if (!Double.isFinite(molarMass) || molarMass <= 0.0) {
            throw new IllegalArgumentException("物种 " + id + " 的摩尔质量非法: " + molarMass);
        }
        return molarMass;
    }

    private static SpecResult.RuleResult evaluateRule(FractionRule rule,
                                                      Kind kind,
                                                      Map<String, Double> molesById,
                                                      Map<String, String> keyByPath,
                                                      double totalMass,
                                                      ToDoubleFunction<String> molarMassOf) {
        final String id = rule.ionId();
        if (id == null || id.isBlank()) throw new IllegalArgumentException(kind + " 规则含空 ionId");
        final double min = rule.minFraction();
        final double max = rule.maxFraction();

        // (a) id 写法不一致：规则问的物种"换了种写法"就在容器里 → 数据/集成错误，失败关闭
        //     （否则命名空间写错的上限规则会因为查不到 → 0 → OK，把禁用物静默放过）
        if (!molesById.containsKey(id)) {
            String otherKey = keyByPath.get(pathOf(id));
            if (otherKey != null && !otherKey.equals(id)) {
                return new SpecResult.RuleResult(id, 0.0, min, max, Status.UNKNOWN_MASS, kind);
            }
        }

        // (b) 规则引用的物种解析不出摩尔质量（典型：数据包把 id 打错）→ 失败关闭
        double molarMass = molarMassOf.applyAsDouble(id);
        if (!Double.isFinite(molarMass) || molarMass <= 0.0) {
            return new SpecResult.RuleResult(id, 0.0, min, max, Status.UNKNOWN_MASS, kind);
        }

        double actual = molesById.getOrDefault(id, 0.0) * molarMass / totalMass;
        Status status;
        if (max <= 0.0 && actual > EPS) {
            status = Status.PRESENT_FORBIDDEN;   // 上限为 0 = 禁用物质，单独给状态便于 GUI 说明
        } else if (actual < min - EPS) {
            status = Status.BELOW_MIN;
        } else if (actual > max + EPS) {
            status = Status.ABOVE_MAX;
        } else {
            status = Status.OK;
        }
        return new SpecResult.RuleResult(id, actual, min, max, status, kind);
    }

    /** verdict 汇总：只读 details，不看输入，便于单独推理。 */
    private static Verdict aggregate(List<SpecResult.RuleResult> details, double usableMoles, double bottleMoles) {
        boolean unknown = false;
        boolean basicFailed = false;
        boolean hasPremium = false;
        boolean premiumFailed = false;
        for (SpecResult.RuleResult detail : details) {
            if (detail.status() == Status.UNKNOWN_MASS) {
                unknown = true;
            } else if (detail.kind() == Kind.PREMIUM) {
                hasPremium = true;
                if (detail.status() != Status.OK) premiumFailed = true;
            } else if (detail.status() != Status.OK) {
                basicFailed = true;
            }
        }
        if (unknown) return Verdict.REJECTED;          // 数据错误：失败关闭
        if (basicFailed) return Verdict.REJECTED;      // 质量优先于数量
        if (usableMoles < bottleMoles - EPS) return Verdict.NOT_ENOUGH;
        return (hasPremium && !premiumFailed) ? Verdict.PREMIUM : Verdict.PASS;
    }

    private static List<FractionRule> requireRules(List<FractionRule> rules, String field) {
        if (rules == null) {
            throw new IllegalArgumentException("ProductSpec." + field + " 为 null（无内容请传空 List）");
        }
        for (FractionRule rule : rules) {
            if (rule == null) throw new IllegalArgumentException("ProductSpec." + field + " 含 null 规则");
        }
        return rules;
    }

    private static List<FractionRule> validateRuleList(List<FractionRule> rules, String field, List<String> errors) {
        if (rules == null) {
            errors.add(field + " 为 null（无内容请传空 List）");
            return List.of();
        }
        Set<String> seen = new LinkedHashSet<>();
        for (int i = 0; i < rules.size(); i++) {
            FractionRule rule = rules.get(i);
            String where = field + "[" + i + "]";
            if (rule == null) {
                errors.add(where + " 为 null");
                continue;
            }
            if (isBlank(rule.ionId())) {
                errors.add(where + ".ionId 为空");
            } else if (!seen.add(rule.ionId())) {
                errors.add(where + ".ionId 重复: " + rule.ionId());
            }
            double min = rule.minFraction();
            double max = rule.maxFraction();
            if (!Double.isFinite(min) || min < 0.0) {
                errors.add(where + ".minFraction 必须为非负有限值，实际 " + min);
            }
            if (Double.isNaN(max) || max == Double.NEGATIVE_INFINITY) {
                errors.add(where + ".maxFraction 非法，实际 " + max);
            }
            if (Double.isFinite(min) && Double.isFinite(max) && max < min) {
                errors.add(where + " 窗口倒置: min=" + min + " > max=" + max);
            }
        }
        return rules;
    }

    private static void addIds(Set<String> target, List<FractionRule> rules) {
        for (FractionRule rule : rules) target.add(rule.ionId());
    }

    /** 去掉命名空间前缀（"trainees:cl2" → "cl2"），用于 id 写法一致性审计。 */
    private static String pathOf(String id) {
        int colon = id.indexOf(':');
        return colon >= 0 ? id.substring(colon + 1) : id;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
