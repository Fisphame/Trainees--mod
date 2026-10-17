package com.pha.trainees.chemistry.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pha.trainees.Main;
import com.pha.trainees.chemistry.particle.IonType;
import com.pha.trainees.chemistry.reaction.ReactionEdge;
import com.pha.trainees.chemistry.reaction.ReactionGraph;
import com.pha.trainees.chemistry.reaction.ReactionRule;
import com.pha.trainees.registry.ModChemistry;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 把引擎里的硬编码内容**导出**成数据包 JSON（§17.1 选择性公开，批次 3a）。
 *
 * <p><b>为什么必须程序化导出</b>：手抄必然漏项、必然与代码漂移。所以遍历注册表、
 * 逐条序列化、并**立刻回读校验**——"导出没写错"的唯一硬证据就是"读回来和源对象一致"。</p>
 *
 * <p><b>回环自检</b>（{@link #export} 的第三步）：写完每个文件后，用 {@link ChemDataParser}
 * 把它读回来解析一次，再与源对象逐字段比对。任何不一致都记进 {@link Report#mismatches()}。
 * 这样"导出器 + 契约 + 解析器"三者的一致性当场就能验，不用等进游戏。</p>
 */
public final class ChemDataExporter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** 导出报告：写了多少、跳过多少、回读比对结果。 */
    public record Report(int ionsWritten, int rulesWritten, List<String> problems, List<String> mismatches) {
        public boolean clean() {
            return problems.isEmpty() && mismatches.isEmpty();
        }
    }

    private ChemDataExporter() {
    }

    /**
     * 导出全部内容到 {@code targetDir}（会创建 ions/ 与 reactions/ 子目录）。
     *
     * @param ionLookup 回读校验时用来把 JSON 里的 ion id 解析回对象（用硬编码注册表即可）
     */
    public static Report export(Path targetDir, java.util.function.Function<ResourceLocation, IonType> ionLookup) {
        List<String> problems = new ArrayList<>();
        List<String> mismatches = new ArrayList<>();
        int ionsWritten = 0;
        int rulesWritten = 0;

        try {
            Path ionDir = targetDir.resolve("ions");
            Path ruleDir = targetDir.resolve("reactions");
            Files.createDirectories(ionDir);
            Files.createDirectories(ruleDir);

            // ---- 1) 物质 ----
            for (IonType ion : ModChemistry.ModIons.all()) {
                ChemDataWriter.WriteResult result = ChemDataWriter.writeIon(ion);
                for (ChemDataRegistry.Problem problem : result.problems()) {
                    problems.add(problem.toString());
                }
                Path file = ionDir.resolve(ion.getId().getPath() + ".json");
                if (writeJson(file, result.json(), problems)) {
                    ionsWritten++;
                    verifyIon(file, ion, mismatches, problems);
                }
            }

            // ---- 2) 反应规则 ----
            // 注意：ReactionGraph 存的是**边**，一条规则可能对应多条边（按节点展开），
            // 所以这里必须**按规则 id 去重**再写文件；否则同一条规则会被反复写同一个文件，
            // 看起来"导出成功"，实际上数目对不上、且掩盖真正的 id 冲突。
            // 若出现"同一个 id 指向两个不同规则对象"，那是源数据的问题，必须报出来而不是静默覆盖。
            java.util.Map<ResourceLocation, ReactionRule> uniqueRules = new java.util.LinkedHashMap<>();
            for (ReactionEdge edge : ReactionGraph.getInstance().getAllEdges()) {
                ReactionRule rule = edge.getRule();
                ReactionRule previous = uniqueRules.putIfAbsent(rule.getId(), rule);
                if (previous != null && previous != rule) {
                    problems.add("同一 id 指向多条不同规则（导出会覆盖，需要改成唯一 id）: " + rule.getId());
                }
            }

            for (ReactionRule rule : uniqueRules.values()) {
                ChemDataWriter.WriteResult result = ChemDataWriter.writeRule(rule);
                for (ChemDataRegistry.Problem problem : result.problems()) {
                    problems.add(problem.toString());
                }
                Path file = ruleDir.resolve(rule.getId().getPath() + ".json");
                if (writeJson(file, result.json(), problems)) {
                    rulesWritten++;
                    verifyRule(file, rule, ionLookup, mismatches, problems);
                }
            }
        } catch (IOException e) {
            problems.add("导出失败: " + e.getMessage());
        }

        return new Report(ionsWritten, rulesWritten, problems, mismatches);
    }

    private static boolean writeJson(Path file, JsonObject json, List<String> problems) {
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(json, writer);
            return true;
        } catch (IOException e) {
            problems.add("写入 " + file + " 失败: " + e.getMessage());
            return false;
        }
    }

    // ==================== 回环校验（导出后立刻读回来比对） ====================

    private static void verifyIon(Path file, IonType source, List<String> mismatches, List<String> problems) {
        JsonObject json = readBack(file, problems);
        if (json == null) return;
        ChemDataParser.ParseResult<IonType> parsed = ChemDataParser.parseIon(source.getId(), json);
        for (ChemDataRegistry.Problem problem : parsed.problems()) {
            if (problem.severity() == ChemDataRegistry.Severity.ERROR) {
                problems.add("回读失败 " + file.getFileName() + ": " + problem);
            }
        }
        if (parsed.value() == null) return;

        IonType back = parsed.value();
        compare(mismatches, file, "phase", source.getPhase(), back.getPhase());
        compare(mismatches, file, "molarMass", source.getMolarMass(), back.getMolarMass());
        compare(mismatches, file, "specificHeat", source.getSpecificHeat(), back.getSpecificHeat());
        compare(mismatches, file, "formationEnthalpy", source.getFormationEnthalpy(), back.getFormationEnthalpy());
        compare(mismatches, file, "formationGibbs", source.getFormationGibbs(), back.getFormationGibbs());
        compare(mismatches, file, "toxicity", source.getToxicityLevel(), back.getToxicityLevel());
        compare(mismatches, file, "form", source.getForm(), back.getForm());
        compare(mismatches, file, "tags", source.getTags(), back.getTags());
        compare(mismatches, file, "stableMin", source.getMinStableTemp(), back.getMinStableTemp());
        compare(mismatches, file, "stableMax", source.getMaxStableTemp(), back.getMaxStableTemp());
    }

    private static void verifyRule(Path file, ReactionRule source,
                                   java.util.function.Function<ResourceLocation, IonType> ionLookup,
                                   List<String> mismatches, List<String> problems) {
        JsonObject json = readBack(file, problems);
        if (json == null) return;
        ChemDataParser.ParseResult<ReactionRule> parsed = ChemDataParser.parseRule(source.getId(), json, ionLookup);
        for (ChemDataRegistry.Problem problem : parsed.problems()) {
            if (problem.severity() == ChemDataRegistry.Severity.ERROR) {
                problems.add("回读失败 " + file.getFileName() + ": " + problem);
            }
        }
        if (parsed.value() == null) return;

        ReactionRule back = parsed.value();
        compare(mismatches, file, "reactants", stoichiometryOf(source), stoichiometryOf(back));
        compare(mismatches, file, "products", stoichiometryOf(back), stoichiometryOf(source));
        // 来自配置的字段写成了 {"config": ...}：解析器不落地配置引用（回读只能得到默认值），
        // 所以这类字段**不参与**回读比对，否则会误报"不一致"。
        if (!isConfigRef(json, "thermo", "deltaH")) {
            compare(mismatches, file, "deltaH", source.getDeltaH(), back.getDeltaH());
        }
        if (!isConfigRef(json, "thermo", "deltaG")) {
            compare(mismatches, file, "deltaG", source.getDeltaG(), back.getDeltaG());
        }
        if (!isConfigRef(json, "kinetics", "activationEnergy")) {
            compare(mismatches, file, "activationEnergy", source.getActivationEnergy(), back.getActivationEnergy());
        }
        compare(mismatches, file, "preExponentialFactor", source.getPreExponentialFactor(),
                back.getPreExponentialFactor());
        if (!isConfigRef(json, "gates", "minTemperature")) {
            compare(mismatches, file, "minTemperature", source.getMinTemperature(), back.getMinTemperature());
        }
        compare(mismatches, file, "electricalWork", source.getElectricalWorkPerMol(),
                back.getElectricalWorkPerMol());
        compare(mismatches, file, "family", source.getFamily(), back.getFamily());
        compare(mismatches, file, "quiet", source.isQuiet(), back.isQuiet());
        compare(mismatches, file, "preconditions", source.getPreconditions(), back.getPreconditions());
    }

    /** JSON 里某字段是否写成了 {@code {"config": ...}} 引用。 */
    private static boolean isConfigRef(JsonObject root, String section, String field) {
        var sectionElement = root.get(section);
        if (sectionElement == null || !sectionElement.isJsonObject()) return false;
        var value = sectionElement.getAsJsonObject().get(field);
        return value != null && value.isJsonObject() && value.getAsJsonObject().has("config");
    }

    @Nullable
    private static JsonObject readBack(Path file, List<String> problems) {
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            return JsonParser.parseString(text).getAsJsonObject();
        } catch (Exception e) {
            problems.add("回读 " + file.getFileName() + " 失败: " + e.getMessage());
            return null;
        }
    }

    /** 把 {IonType → 计量数} 换成 {ion id 字符串 → 计量数}，便于比对与打印。 */
    private static java.util.Map<String, Integer> stoichiometryOf(ReactionRule rule) {
        java.util.Map<String, Integer> out = new java.util.TreeMap<>();
        rule.getReactants().forEach((ion, count) -> out.put("R:" + ion.getId(), count));
        rule.getProducts().forEach((ion, count) -> out.put("P:" + ion.getId(), count));
        return out;
    }

    private static void compare(List<String> mismatches, Path file, String field, Object expected, Object actual) {
        if (expected instanceof Double a && actual instanceof Double b) {
            if (Math.abs(a - b) > 1e-6) {
                mismatches.add(file.getFileName() + "#" + field + ": 源=" + a + " 回读=" + b);
            }
            return;
        }
        if (!java.util.Objects.equals(expected, actual)) {
            mismatches.add(file.getFileName() + "#" + field + ": 源=" + expected + " 回读=" + actual);
        }
    }

    /** 输出一行摘要（供日志）。 */
    public static void logReport(Report report) {
        Main.LOGGER.info("[ChemData] 导出完成：物质 {} 个、反应 {} 条；问题 {}、回读不一致 {}",
                report.ionsWritten(), report.rulesWritten(), report.problems().size(), report.mismatches().size());
        for (String problem : report.problems()) {
            Main.LOGGER.error("[ChemData] 导出问题：{}", problem);
        }
        for (String mismatch : report.mismatches()) {
            Main.LOGGER.error("[ChemData] 回读不一致：{}", mismatch);
        }
        if (report.clean()) {
            Main.LOGGER.info("[ChemData] 回环自检通过：导出结果与源对象逐字段一致");
        }
    }
}
