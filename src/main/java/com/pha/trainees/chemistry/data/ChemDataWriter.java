package com.pha.trainees.chemistry.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pha.trainees.chemistry.data.ChemDataRegistry.Problem;
import com.pha.trainees.chemistry.data.ChemDataRegistry.Severity;
import com.pha.trainees.chemistry.particle.IonType;
import com.pha.trainees.chemistry.reaction.ReactionRule;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 把引擎里的物质/规则**序列化**成契约 JSON（§17.1 选择性公开，批次 3）。
 *
 * <p>用途：把现有的硬编码定义导出成数据包，从而让"机制公开、实例私有"真正落地。
 * 导出必须<b>程序化</b>完成——手抄必然漏项、必然与代码漂移。</p>
 *
 * <p><b>两个关键技巧</b>（都是为了让导出结果和源数据一样"干净"）：</p>
 * <ol>
 *   <li><b>算出来的值写成 {@code "computed"}</b>：引擎里 ΔH/ΔG/电功可能是"由物质生成焓按能量守恒算出"
 *       的（{@code deltaHComputed()} 等）。构建后的对象只暴露数值、不暴露来源，所以这里**反推验证**：
 *       若数值恰好等于按生成焓算出的结果，就写 {@code "computed"}。
 *       好处是导出数据里少一批手填的热力学数值，也保住"禁止手填经验值"的纪律。</li>
 *   <li><b>默认值不写</b>：只写"与默认不同"的字段（{@code form} 与物态派生值相同就不写、
 *       {@code bias=1.0} 不写、{@code maxTemperature} 无穷不写……），让 JSON 只表达真实意图。</li>
 * </ol>
 *
 * <p><b>无法导出的信息</b>：动态优先级函数是 Java 代码，JSON 表达不了。遇到这种规则会在
 * {@link WriteResult#problems()} 里报一条 ERROR，提示必须先注册成命名偏置函数
 * （见 {@code docs/CHEMISTRY_DATA_SCHEMA.md} §3.2）——**不静默丢信息**。</p>
 */
public final class ChemDataWriter {

    private static final double EPSILON = 1e-6;

    /** 序列化结果：JSON + 诊断（可能出现"无法表达"的 ERROR）。 */
    public record WriteResult(JsonObject json, List<Problem> problems) {
    }

    private ChemDataWriter() {
    }

    // ==================== 物质 ====================

    public static WriteResult writeIon(IonType ion) {
        JsonObject json = new JsonObject();
        List<Problem> problems = new ArrayList<>();
        ResourceLocation id = ion.getId();

        json.addProperty("schemaVersion", 1);
        json.addProperty("phase", ion.getPhase().name());
        json.addProperty("molarMass", ion.getMolarMass());
        json.addProperty("specificHeat", ion.getSpecificHeat());
        json.addProperty("formationEnthalpy", ion.getFormationEnthalpy());
        json.addProperty("formationGibbs", ion.getFormationGibbs());
        if (ion.getToxicityLevel() != 0) {
            json.addProperty("toxicity", ion.getToxicityLevel());
        }
        json.addProperty("flameColor", hex(ion.getFlameColor()));
        if ((ion.getDisplayColor() & 0xFFFFFF) != 0xFFFFFF) {
            json.addProperty("displayColor", hex(ion.getDisplayColor() & 0xFFFFFF));
        }
        // 只有与物态派生的形态不同才写（否则等于写默认值）
        if (ion.getForm() != IonType.Form.defaultFor(ion.getPhase())) {
            json.addProperty("form", ion.getForm().name());
        }
        if (!ion.getTags().isEmpty()) {
            JsonArray tags = new JsonArray();
            ion.getTags().stream()
                    .map(ResourceLocation::toString)
                    .sorted()
                    .forEach(tags::add);
            json.add("tags", tags);
        }
        if (ion.getMinStableTemp() != 0 || ion.getMaxStableTemp() != 9999) {
            JsonObject range = new JsonObject();
            range.addProperty("min", ion.getMinStableTemp());
            range.addProperty("max", ion.getMaxStableTemp());
            json.add("stableRange", range);
        }
        return new WriteResult(json, problems);
    }

    // ==================== 反应规则 ====================

    public static WriteResult writeRule(ReactionRule rule) {
        JsonObject json = new JsonObject();
        List<Problem> problems = new ArrayList<>();
        ResourceLocation id = rule.getId();

        json.addProperty("schemaVersion", 1);
        json.add("reactants", stoichiometry(rule.getReactants()));
        json.add("products", stoichiometry(rule.getProducts()));
        if (!rule.getPreconditions().isEmpty()) {
            json.add("preconditions", stoichiometry(rule.getPreconditions()));
        }

        // ---- 热力学（能反推成 computed 就写 computed；来自配置就写 {"config": ...}）----
        JsonObject thermo = new JsonObject();
        double computedH = formationSum(rule.getProducts(), true) - formationSum(rule.getReactants(), true);
        double computedG = formationSum(rule.getProducts(), false) - formationSum(rule.getReactants(), false);
        String configH = com.pha.trainees.chemistry.reaction.ConfigSources.get(id, "deltaH");
        String configG = com.pha.trainees.chemistry.reaction.ConfigSources.get(id, "deltaG");
        if (configH != null) {
            thermo.add("deltaH", configRef(configH));
        } else if (close(rule.getDeltaH(), computedH)) {
            thermo.addProperty("deltaH", "computed");
        } else {
            thermo.addProperty("deltaH", rule.getDeltaH());
        }
        if (configG != null) {
            thermo.add("deltaG", configRef(configG));
        } else if (close(rule.getDeltaG(), computedG)) {
            thermo.addProperty("deltaG", "computed");
        } else {
            thermo.addProperty("deltaG", rule.getDeltaG());
        }
        if (rule.isEquilibriumOverridden()) {
            thermo.addProperty("equilibriumConstant", rule.getEquilibriumConstant());
            thermo.addProperty("equilibriumOverridden", true);
        }
        json.add("thermo", thermo);

        // ---- 动力学 ----
        JsonObject kinetics = new JsonObject();
        String configEa = com.pha.trainees.chemistry.reaction.ConfigSources.get(id, "activationEnergy");
        if (configEa != null) {
            kinetics.add("activationEnergy", configRef(configEa));
        } else {
            kinetics.addProperty("activationEnergy", rule.getActivationEnergy());
        }
        kinetics.addProperty("preExponentialFactor", rule.getPreExponentialFactor());
        json.add("kinetics", kinetics);

        // ---- 门槛 ----
        JsonObject gates = new JsonObject();
        String configMinT = com.pha.trainees.chemistry.reaction.ConfigSources.get(id, "minTemperature");
        if (configMinT != null) {
            gates.add("minTemperature", configRef(configMinT));
        } else {
            gates.addProperty("minTemperature", rule.getMinTemperature());
        }
        if (Double.isFinite(rule.getMaxTemperature())) {
            gates.addProperty("maxTemperature", rule.getMaxTemperature());
        }
        if (rule.isTemperaturePressureShifted()) {
            JsonObject shift = new JsonObject();
            shift.addProperty("deltaH", rule.getPressureShiftDeltaHKj());
            shift.addProperty("referenceAtm", rule.getPressureShiftReferenceAtm());
            gates.add("pressureShift", shift);
        }
        json.add("gates", gates);

        // ---- 电解（W = ΔG(产物) − ΔG(反应物) 反推为 computed）----
        double work = rule.getElectricalWorkPerMol();
        if (work > 0) {
            if (close(work, computedG)) {
                json.addProperty("electricalWork", "computed");
            } else {
                json.addProperty("electricalWork", work);
            }
        }

        // ---- 标志与家族 ----
        JsonObject flags = new JsonObject();
        if (rule.isQuiet()) flags.addProperty("quiet", true);
        if (rule.isSelfLoop()) flags.addProperty("selfLoop", true);
        if (flags.size() > 0) {
            json.add("flags", flags);
        }
        if (rule.getFamily() != ReactionRule.deriveFamily(rule.isQuiet(), null)) {
            json.addProperty("family", rule.getFamily().name());
        }

        // ---- 优先级 ----
        JsonObject priority = new JsonObject();
        if (rule.hasDynamicPriority()) {
            // 反查注册 id：拿到就写 {"function": id}（数据只引用 id，函数体留在引擎）
            net.minecraft.resources.ResourceLocation fnId =
                    com.pha.trainees.chemistry.reaction.BiasFunctions.idOf(rule.getDynamicPriorityBias());
            if (fnId != null) {
                priority.addProperty("function", fnId.toString());
            } else {
                problems.add(new Problem(Severity.ERROR, id, "priority.function",
                        "该规则使用了动态优先级函数，且它没有登记成命名偏置函数，无法用数据表达；"
                                + "请改用 BiasFunctions 里的具名函数（或注册到 BiasFunctionRegistry）"));
            }
        } else if (!close(rule.getGamePriorityBias(), 1.0)) {
            priority.addProperty("bias", rule.getGamePriorityBias());
        }
        if (priority.size() > 0) {
            json.add("priority", priority);
        }

        // 还有"来自配置但本期序列化器没覆盖"的字段时，明确报 WARN（不静默写死数字）
        java.util.Set<String> unsupported = new java.util.LinkedHashSet<>(
                com.pha.trainees.chemistry.reaction.ConfigSources.all(id).keySet());
        unsupported.removeAll(java.util.Set.of("deltaH", "deltaG", "activationEnergy", "minTemperature"));
        for (String field : unsupported) {
            problems.add(new Problem(Severity.WARN, id, field,
                    "该字段来自配置，但本期导出未覆盖它（会写成当前数值）"));
        }

        return new WriteResult(json, problems);
    }

    /** 构造 {@code {"config": "<path>"}} 引用对象。 */
    private static JsonObject configRef(String path) {
        JsonObject ref = new JsonObject();
        ref.addProperty("config", path);
        return ref;
    }

    // ==================== 工具 ====================

    private static JsonArray stoichiometry(Map<IonType, Integer> map) {
        JsonArray array = new JsonArray();
        map.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().getId().toString()))
                .forEach(e -> {
                    JsonObject entry = new JsonObject();
                    entry.addProperty("ion", e.getKey().getId().toString());
                    entry.addProperty("count", e.getValue());
                    array.add(entry);
                });
        return array;
    }

    private static double formationSum(Map<IonType, Integer> map, boolean enthalpy) {
        double sum = 0;
        for (Map.Entry<IonType, Integer> e : map.entrySet()) {
            sum += (enthalpy ? e.getKey().getFormationEnthalpy() : e.getKey().getFormationGibbs()) * e.getValue();
        }
        return sum;
    }

    private static boolean close(double a, double b) {
        return Math.abs(a - b) < EPSILON;
    }

    private static String hex(int color) {
        return String.format("0x%06X", color & 0xFFFFFF);
    }
}
