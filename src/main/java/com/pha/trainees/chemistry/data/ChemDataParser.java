package com.pha.trainees.chemistry.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pha.trainees.chemistry.data.ChemDataRegistry.Problem;
import com.pha.trainees.chemistry.data.ChemDataRegistry.Severity;
import com.pha.trainees.chemistry.particle.IonType;
import com.pha.trainees.chemistry.particle.Phase;
import com.pha.trainees.chemistry.reaction.ReactionRule;
import com.pha.trainees.chemistry.reaction.RuleFamily;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

/**
 * 化学内容的数据解析与校验（契约：{@code docs/CHEMISTRY_DATA_SCHEMA.md}）。
 *
 * <p><b>纯逻辑</b>：只吃 {@link JsonObject} 与一个"按 id 找物质"的查找函数，不碰世界、不读文件，
 * 所以可以单元测试（{@code ChemDataParserTest}）。加载器 {@link ChemDataLoader} 负责读文件并调用这里。</p>
 *
 * <p><b>校验纪律</b>（契约 §5）：</p>
 * <ul>
 *   <li>未知字段 → WARN（便于版本演进），不影响加载；</li>
 *   <li>缺必填 / 引用不存在的物质 / count 非正 / 温度区间倒置 / 引用未注册的命名函数 → **ERROR**；</li>
 *   <li>只要出现一条 ERROR，该条目**整体不加载**（返回 null），绝不"部分生效"——半生效的内容最难排查。</li>
 * </ul>
 */
public final class ChemDataParser {

    /** 解析结果：值（失败为 null）+ 诊断。 */
    public record ParseResult<T>(@Nullable T value, List<Problem> problems) {
    }

    private static final Set<String> ION_FIELDS = Set.of(
            "schemaVersion", "phase", "molarMass", "specificHeat", "formationEnthalpy", "formationGibbs",
            "toxicity", "flameColor", "displayColor", "form", "tags", "stableRange");

    private static final Set<String> RULE_FIELDS = Set.of(
            "schemaVersion", "reactants", "products", "preconditions", "thermo", "kinetics", "gates",
            "electricalWork", "flags", "family", "priority");

    private ChemDataParser() {
    }

    // ==================== 物质 ====================

    public static ParseResult<IonType> parseIon(ResourceLocation id, JsonObject json) {
        List<Problem> problems = new ArrayList<>();
        warnUnknown(json, ION_FIELDS, id, problems);

        Phase phase = readEnum(json, "phase", Phase.class, id, problems);
        if (phase == null) {
            if (!hasError(problems)) {
                problems.add(new Problem(Severity.ERROR, id, "phase", "缺少必填字段 phase"));
            }
            return new ParseResult<>(null, problems);
        }

        IonType.Builder builder = new IonType.Builder(id, phase);
        Double molarMass = readNumber(json, "molarMass", id, problems);
        if (molarMass != null) builder.molarMass(molarMass);
        Double specificHeat = readNumber(json, "specificHeat", id, problems);
        if (specificHeat != null) builder.specificHeat(specificHeat);
        Double enthalpy = readNumber(json, "formationEnthalpy", id, problems);
        if (enthalpy != null) builder.formationEnthalpy(enthalpy);
        Double gibbs = readNumber(json, "formationGibbs", id, problems);
        if (gibbs != null) builder.formationGibbs(gibbs);
        Double toxicity = readNumber(json, "toxicity", id, problems);
        if (toxicity != null) builder.toxicityLevel((int) Math.round(toxicity));
        Double flame = readColor(json, "flameColor", id, problems);
        if (flame != null) builder.flameColor(flame.intValue());
        Double display = readColor(json, "displayColor", id, problems);
        if (display != null) builder.displayColor(display.intValue());

        IonType.Form form = readEnum(json, "form", IonType.Form.class, id, problems);
        if (form != null) builder.form(form);

        if (json.has("tags")) {
            for (String tag : readStringList(json.get("tags"), "tags", id, problems)) {
                ResourceLocation tagId = ResourceLocation.tryParse(tag);
                if (tagId == null) {
                    problems.add(new Problem(Severity.ERROR, id, "tags", "不是合法的标签 id: " + tag));
                } else {
                    builder.tag(tagId);
                }
            }
        }

        if (json.has("stableRange")) {
            JsonElement element = json.get("stableRange");
            if (element.isJsonObject()) {
                JsonObject range = element.getAsJsonObject();
                Double min = readNumber(range, "min", id, problems, "stableRange.min");
                Double max = readNumber(range, "max", id, problems, "stableRange.max");
                if (min != null && max != null && min > max) {
                    problems.add(new Problem(Severity.ERROR, id, "stableRange", "min 大于 max"));
                } else if (min != null && max != null) {
                    builder.stableRange(min, max);
                }
            } else {
                problems.add(new Problem(Severity.ERROR, id, "stableRange", "应为对象 {min,max}"));
            }
        }

        if (hasError(problems)) return new ParseResult<>(null, problems);
        return new ParseResult<>(builder.build(), problems);
    }

    // ==================== 反应规则 ====================

    public static ParseResult<ReactionRule> parseRule(ResourceLocation id, JsonObject json,
                                                      Function<ResourceLocation, IonType> ionLookup) {
        List<Problem> problems = new ArrayList<>();
        warnUnknown(json, RULE_FIELDS, id, problems);

        ReactionRule.Builder builder = new ReactionRule.Builder(id);

        int reactants = readStoichiometry(json.get("reactants"), "reactants", id, ionLookup, problems, builder,
                Side.REACTANT);
        int products = readStoichiometry(json.get("products"), "products", id, ionLookup, problems, builder,
                Side.PRODUCT);
        if (reactants <= 0) {
            problems.add(new Problem(Severity.ERROR, id, "reactants", "至少需要一个反应物"));
        }
        if (products <= 0) {
            problems.add(new Problem(Severity.ERROR, id, "products", "至少需要一个生成物"));
        }
        // 前置条件（催化剂等）：要求存在但不消耗——**必须与反应物分开**，
        // 否则既会污染"反应物"集合，也会让 deltaHComputed() 按多出来的反应物算命（回环自检抓到过）
        readStoichiometry(json.get("preconditions"), "preconditions", id, ionLookup, problems, builder,
                Side.PRECONDITION);

        // ---- 热力学 ----
        JsonObject thermo = objectOrNull(json, "thermo");
        if (thermo != null) {
            readThermoValue(thermo, "deltaH", id, problems, v -> builder.deltaH(v), () -> builder.deltaHComputed());
            readThermoValue(thermo, "deltaG", id, problems, v -> builder.deltaG(v), () -> builder.deltaGComputed());
            Double k = readNumber(thermo, "equilibriumConstant", id, problems, "thermo.equilibriumConstant");
            if (k != null) builder.equilibriumConstant(k);
            if (thermo.has("equilibriumOverridden") && thermo.get("equilibriumOverridden").getAsBoolean()) {
                builder.equilibriumOverridden();
            }
        }

        // ---- 动力学 ----
        JsonObject kinetics = objectOrNull(json, "kinetics");
        if (kinetics != null) {
            Double ea = readNumber(kinetics, "activationEnergy", id, problems, "kinetics.activationEnergy");
            if (ea != null) builder.activationEnergy(ea);
            Double a = readNumber(kinetics, "preExponentialFactor", id, problems, "kinetics.preExponentialFactor");
            if (a != null) builder.preExponentialFactor(a);
        }

        // ---- 门槛 ----
        JsonObject gates = objectOrNull(json, "gates");
        if (gates != null) {
            Double minT = readNumber(gates, "minTemperature", id, problems, "gates.minTemperature");
            if (minT != null) builder.minTemperature(minT);
            if (gates.has("maxTemperature") && !gates.get("maxTemperature").isJsonNull()) {
                Double maxT = readNumber(gates, "maxTemperature", id, problems, "gates.maxTemperature");
                if (maxT != null) builder.maxTemperature(maxT);
            }
            if (gates.has("pressureShift")) {
                JsonObject shift = objectOrNull(gates, "pressureShift");
                if (shift != null) {
                    Double latent = readNumber(shift, "deltaH", id, problems, "gates.pressureShift.deltaH");
                    if (latent != null && latent > 0) builder.pressureShiftedTemperature(latent);
                    Double refAtm = readNumber(shift, "referenceAtm", id, problems, "gates.pressureShift.referenceAtm");
                    if (refAtm != null && Math.abs(refAtm - 1.0) > 1e-9) {
                        problems.add(new Problem(Severity.WARN, id, "gates.pressureShift.referenceAtm",
                                "引擎固定参考压强为 1 atm，此值被忽略"));
                    }
                }
            }
        }

        // ---- 电解 ----
        if (json.has("electricalWork")) {
            JsonElement element = json.get("electricalWork");
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()
                    && "computed".equalsIgnoreCase(element.getAsString())) {
                builder.electricalWorkComputed();
            } else {
                Double work = readNumber(json, "electricalWork", id, problems);
                if (work != null) builder.electricalWorkPerMol(work);
            }
        }

        // ---- 标志与家族 ----
        JsonObject flags = objectOrNull(json, "flags");
        if (flags != null) {
            if (flags.has("quiet") && flags.get("quiet").getAsBoolean()) builder.quiet();
            if (flags.has("selfLoop") && flags.get("selfLoop").getAsBoolean()) builder.selfLoop(true);
        }
        RuleFamily family = readEnum(json, "family", RuleFamily.class, id, problems);
        if (family != null) builder.family(family);

        // ---- 优先级（含"命名函数"契约）----
        JsonObject priority = objectOrNull(json, "priority");
        if (priority != null) {
            Double bias = readNumber(priority, "bias", id, problems, "priority.bias");
            if (bias != null) builder.gamePriorityBias(bias);
            if (priority.has("function")) {
                ResourceLocation fnId = ResourceLocation.tryParse(priority.get("function").getAsString());
                if (fnId == null) {
                    problems.add(new Problem(Severity.ERROR, id, "priority.function", "不是合法的函数 id"));
                } else {
                    Function<com.pha.trainees.chemistry.container.IChemicalContainer, Double> fn =
                            BiasFunctionRegistry.get(fnId);
                    if (fn == null) {
                        problems.add(new Problem(Severity.ERROR, id, "priority.function",
                                "未注册的命名偏置函数（拒绝加载，不静默降级）: " + fnId));
                    } else {
                        builder.dynamicPriorityBias(fn);
                    }
                }
            }
        }

        if (hasError(problems)) return new ParseResult<>(null, problems);
        return new ParseResult<>(builder.build(), problems);
    }

    // ==================== 内部工具 ====================

    /** 计量条目属于哪一侧（前置条件是"要求存在但不消耗"，必须与反应物分开）。 */
    private enum Side {
        REACTANT,
        PRODUCT,
        PRECONDITION
    }

    private static int readStoichiometry(@Nullable JsonElement element, String field, ResourceLocation file,
                                         Function<ResourceLocation, IonType> ionLookup, List<Problem> problems,
                                         ReactionRule.Builder builder, Side side) {
        if (element == null || !element.isJsonArray()) return 0;
        int accepted = 0;
        for (JsonElement item : element.getAsJsonArray()) {
            if (!item.isJsonObject()) {
                problems.add(new Problem(Severity.ERROR, file, field, "条目应为对象 {ion,count}"));
                continue;
            }
            JsonObject entry = item.getAsJsonObject();
            String ionId = entry.has("ion") ? entry.get("ion").getAsString() : null;
            int count = entry.has("count") ? entry.get("count").getAsInt() : 1;
            if (ionId == null) {
                problems.add(new Problem(Severity.ERROR, file, field, "缺少 ion 字段"));
                continue;
            }
            if (count <= 0) {
                problems.add(new Problem(Severity.ERROR, file, field, "count 必须为正整数: " + count));
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(ionId);
            IonType ion = id == null ? null : ionLookup.apply(id);
            if (ion == null) {
                problems.add(new Problem(Severity.ERROR, file, field, "引用了不存在的物质: " + ionId));
                continue;
            }
            switch (side) {
                case REACTANT -> builder.reactant(ion, count);
                case PRODUCT -> builder.product(ion, count);
                case PRECONDITION -> builder.precondition(ion, count);
            }
            accepted++;
        }
        return accepted;
    }

    /** 支持三种写法：数字 / "computed" / {"config": ...}（本批次 config 仅警告）。 */
    private static void readThermoValue(JsonObject owner, String field, ResourceLocation file,
                                        List<Problem> problems,
                                        java.util.function.DoubleConsumer constant,
                                        Runnable computed) {
        if (!owner.has(field)) return;
        JsonElement element = owner.get(field);
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String text = element.getAsString();
            if ("computed".equalsIgnoreCase(text)) {
                computed.run();
            } else {
                problems.add(new Problem(Severity.ERROR, file, field, "字符串只接受 \"computed\"，收到: " + text));
            }
            return;
        }
        if (element.isJsonObject()) {
            JsonObject ref = element.getAsJsonObject();
            if (ref.has("config")) {
                problems.add(new Problem(Severity.WARN, file, field,
                        "配置引用尚未接入（本批次只支持数字与 computed），已忽略: " + ref.get("config").getAsString()));
            } else {
                problems.add(new Problem(Severity.ERROR, file, field, "对象只接受 {\"config\": ...}"));
            }
            return;
        }
        Double value = readNumber(owner, field, file, problems);
        if (value != null) constant.accept(value);
    }

    @Nullable
    private static JsonObject objectOrNull(JsonObject owner, String field) {
        JsonElement element = owner.get(field);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    /** 读数字（可带字段名前缀用于诊断）：不存在返回 null；存在但不是数字则记 ERROR 并返回 null。 */
    @Nullable
    private static Double readNumber(JsonObject owner, String field, ResourceLocation file, List<Problem> problems) {
        return readNumber(owner, field, file, problems, field);
    }

    @Nullable
    private static Double readNumber(JsonObject owner, String field, ResourceLocation file, List<Problem> problems,
                                     String label) {
        JsonElement element = owner.get(field);
        if (element == null || element.isJsonNull()) return null;
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            problems.add(new Problem(Severity.ERROR, file, label, "应为数字"));
            return null;
        }
        return element.getAsDouble();
    }

    /** 颜色：接受 0xRRGGBB 字符串或整数。 */
    @Nullable
    private static Double readColor(JsonObject owner, String field, ResourceLocation file, List<Problem> problems) {
        JsonElement element = owner.get(field);
        if (element == null || element.isJsonNull()) return null;
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return element.getAsDouble();
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String text = element.getAsString().trim().toLowerCase(Locale.ROOT);
            try {
                return (double) Long.decode(text).intValue();
            } catch (NumberFormatException e) {
                problems.add(new Problem(Severity.ERROR, file, field, "颜色格式应为 0xRRGGBB: " + text));
                return null;
            }
        }
        problems.add(new Problem(Severity.ERROR, file, field, "颜色应为数字或 0xRRGGBB"));
        return null;
    }

    @Nullable
    private static <E extends Enum<E>> E readEnum(JsonObject owner, String field, Class<E> type,
                                                  ResourceLocation file, List<Problem> problems) {
        JsonElement element = owner.get(field);
        if (element == null || element.isJsonNull()) return null;
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            problems.add(new Problem(Severity.ERROR, file, field, "应为枚举名（字符串）"));
            return null;
        }
        String text = element.getAsString();
        for (E constant : type.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(text)) return constant;
        }
        problems.add(new Problem(Severity.ERROR, file, field, "未知枚举值: " + text));
        return null;
    }

    private static List<String> readStringList(@Nullable JsonElement element, String field, ResourceLocation file,
                                               List<Problem> problems) {
        List<String> out = new ArrayList<>();
        if (element == null || element.isJsonNull()) return out;
        if (!element.isJsonArray()) {
            problems.add(new Problem(Severity.ERROR, file, field, "应为字符串数组"));
            return out;
        }
        for (JsonElement item : element.getAsJsonArray()) {
            if (item.isJsonPrimitive()) {
                out.add(item.getAsString());
            } else {
                problems.add(new Problem(Severity.ERROR, file, field, "数组元素应为字符串"));
            }
        }
        return out;
    }

    private static void warnUnknown(JsonObject json, Set<String> known, ResourceLocation file, List<Problem> out) {
        Set<String> extra = new LinkedHashSet<>(json.keySet());
        extra.removeAll(known);
        for (String field : extra) {
            out.add(new Problem(Severity.WARN, file, field, "未知字段（已忽略；可能是未来版本新增）"));
        }
    }

    private static boolean hasError(List<Problem> problems) {
        for (Problem problem : problems) {
            if (problem.severity() == Severity.ERROR) return true;
        }
        return false;
    }
}
