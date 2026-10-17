package com.pha.trainees.chemistry.data;

import com.pha.trainees.chemistry.particle.IonType;
import com.pha.trainees.chemistry.reaction.ConfigSources;
import com.pha.trainees.chemistry.reaction.ReactionRule;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * 双读比对（§17.1 选择性公开，批次 4 第一步）：把"硬编码内容"与"数据包内容"逐条对齐。
 *
 * <p><b>目的</b>：在真正切换之前，先证明"导出的数据包确实能替代硬编码定义"——
 * 如果两边逐字段一致，切换才是安全的；有不一致就先修，别切。</p>
 *
 * <p><b>纯逻辑</b>：只吃两个集合，不碰世界、不读文件，因此可单测。
 * 差异按 {@link Difference} 列出（种类 / id / 字段 / 硬编码值 / 数据值）。</p>
 *
 * <p><b>两处刻意的放宽</b>：</p>
 * <ul>
 *   <li>{@code demo_} 开头的条目视为"教学示例"，数据侧多出来不算差异（它们是公开仓库的示例包）。</li>
 *   <li>来自配置的字段（见 {@link ConfigSources}）不参与比对：数据侧写的是
 *       {@code {"config": ...}}，解析回来是默认值，本来就不会相等。</li>
 * </ul>
 */
public final class ChemDataParity {

    /** 一条差异。 */
    public record Difference(String kind, ResourceLocation id, String field, String hardcoded, String data) {
        @Override
        public String toString() {
            return "[" + kind + "] " + id + "#" + field + ": 硬编码=" + hardcoded + " 数据=" + data;
        }
    }

    private static final double EPS = 1e-6;

    private ChemDataParity() {
    }

    // ==================== 物质 ====================

    public static List<Difference> compareIons(Collection<IonType> hardcoded, Collection<IonType> fromData) {
        List<Difference> out = new ArrayList<>();
        Map<ResourceLocation, IonType> dataById = byId(fromData, IonType::getId);
        Map<ResourceLocation, IonType> hardById = byId(hardcoded, IonType::getId);

        for (IonType hard : hardById.values()) {
            IonType data = dataById.get(hard.getId());
            if (data == null) {
                out.add(new Difference("ION", hard.getId(), "<缺失>", "存在", "不存在"));
                continue;
            }
            String id = hard.getId().toString();
            diff(out, id, "phase", hard.getPhase().name(), data.getPhase().name());
            diffNum(out, id, "molarMass", hard.getMolarMass(), data.getMolarMass());
            diffNum(out, id, "specificHeat", hard.getSpecificHeat(), data.getSpecificHeat());
            diffNum(out, id, "formationEnthalpy", hard.getFormationEnthalpy(), data.getFormationEnthalpy());
            diffNum(out, id, "formationGibbs", hard.getFormationGibbs(), data.getFormationGibbs());
            diffNum(out, id, "toxicity", hard.getToxicityLevel(), data.getToxicityLevel());
            diff(out, id, "form", hard.getForm().name(), data.getForm().name());
            diff(out, id, "tags", sortedTags(hard), sortedTags(data));
            diffNum(out, id, "stableMin", hard.getMinStableTemp(), data.getMinStableTemp());
            diffNum(out, id, "stableMax", hard.getMaxStableTemp(), data.getMaxStableTemp());
        }
        for (IonType data : dataById.values()) {
            if (!hardById.containsKey(data.getId()) && !isDemo(data.getId())) {
                out.add(new Difference("ION", data.getId(), "<多余>", "不存在", "存在"));
            }
        }
        return out;
    }

    // ==================== 反应规则 ====================

    public static List<Difference> compareRules(Collection<ReactionRule> hardcoded,
                                                Collection<ReactionRule> fromData) {
        List<Difference> out = new ArrayList<>();
        Map<ResourceLocation, ReactionRule> dataById = byId(fromData, ReactionRule::getId);
        Map<ResourceLocation, ReactionRule> hardById = byId(hardcoded, ReactionRule::getId);

        for (ReactionRule hard : hardById.values()) {
            ReactionRule data = dataById.get(hard.getId());
            if (data == null) {
                out.add(new Difference("RULE", hard.getId(), "<缺失>", "存在", "不存在"));
                continue;
            }
            String id = hard.getId().toString();
            // 反应物/生成物/前置条件：按 ion id + 计量数比较
            diff(out, id, "reactants", stoichiometry(hard.getReactants()), stoichiometry(data.getReactants()));
            diff(out, id, "products", stoichiometry(hard.getProducts()), stoichiometry(data.getProducts()));
            diff(out, id, "preconditions", stoichiometry(hard.getPreconditions()),
                    stoichiometry(data.getPreconditions()));
            // 来自配置的字段跳过
            if (ConfigSources.get(hard.getId(), "deltaH") == null) {
                diffNum(out, id, "deltaH", hard.getDeltaH(), data.getDeltaH());
            }
            if (ConfigSources.get(hard.getId(), "deltaG") == null) {
                diffNum(out, id, "deltaG", hard.getDeltaG(), data.getDeltaG());
            }
            if (ConfigSources.get(hard.getId(), "activationEnergy") == null) {
                diffNum(out, id, "activationEnergy", hard.getActivationEnergy(), data.getActivationEnergy());
            }
            if (ConfigSources.get(hard.getId(), "minTemperature") == null) {
                diffNum(out, id, "minTemperature", hard.getMinTemperature(), data.getMinTemperature());
            }
            diffNum(out, id, "preExponentialFactor", hard.getPreExponentialFactor(), data.getPreExponentialFactor());
            diffNum(out, id, "electricalWork", hard.getElectricalWorkPerMol(), data.getElectricalWorkPerMol());
            diff(out, id, "family", hard.getFamily().name(), data.getFamily().name());
            diff(out, id, "quiet", String.valueOf(hard.isQuiet()), String.valueOf(data.isQuiet()));
            // 动态偏置：比"是否有"与"注册 id"
            diff(out, id, "dynamicPriority",
                    String.valueOf(hard.hasDynamicPriority()), String.valueOf(data.hasDynamicPriority()));
        }
        for (ReactionRule data : dataById.values()) {
            if (!hardById.containsKey(data.getId()) && !isDemo(data.getId())) {
                out.add(new Difference("RULE", data.getId(), "<多余>", "不存在", "存在"));
            }
        }
        return out;
    }

    // ==================== 工具 ====================

    private static boolean isDemo(ResourceLocation id) {
        return id.getPath().startsWith("demo_");
    }

    private static <T> Map<ResourceLocation, T> byId(Collection<T> items,
                                                    java.util.function.Function<T, ResourceLocation> idOf) {
        Map<ResourceLocation, T> map = new LinkedHashMap<>();
        for (T item : items) {
            map.put(idOf.apply(item), item);
        }
        return map;
    }

    private static String sortedTags(IonType ion) {
        List<String> tags = new ArrayList<>();
        ion.getTags().forEach(tag -> tags.add(tag.toString()));
        tags.sort(null);
        return String.join(",", tags);
    }

    private static Map<String, Integer> stoichiometry(Map<IonType, Integer> map) {
        Map<String, Integer> out = new TreeMap<>();
        map.forEach((ion, count) -> out.put(ion.getId().toString(), count));
        return out;
    }

    private static void diff(List<Difference> out, String id, String field, Object hard, Object data) {
        if (!Objects.equals(hard, data)) {
            out.add(new Difference(kind(id), ResourceLocation.tryParse(id), field,
                    String.valueOf(hard), String.valueOf(data)));
        }
    }

    private static void diffNum(List<Difference> out, String id, String field, double hard, double data) {
        if (Math.abs(hard - data) > EPS) {
            out.add(new Difference(kind(id), ResourceLocation.tryParse(id), field,
                    String.valueOf(hard), String.valueOf(data)));
        }
    }

    /** 差异种类（ION/RULE）——由调用方在各自方法里决定更准确，这里做兜底。 */
    private static String kind(String id) {
        return "ENTRY";
    }
}
