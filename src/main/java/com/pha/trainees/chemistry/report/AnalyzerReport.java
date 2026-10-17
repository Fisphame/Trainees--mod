package com.pha.trainees.chemistry.report;

import com.pha.trainees.chemistry.container.IChemicalContainer;
import com.pha.trainees.chemistry.engine.ExecutedRule;
import com.pha.trainees.chemistry.engine.PredictedRule;
import com.pha.trainees.chemistry.engine.ReactionEngine;
import com.pha.trainees.chemistry.engine.ReactionFailure;
import com.pha.trainees.chemistry.gas.GasPhysics;
import com.pha.trainees.chemistry.particle.IonType;
import com.pha.trainees.chemistry.particle.Phase;
import com.pha.trainees.chemistry.util.IonDisplay;
import com.pha.trainees.chemistry.util.ReactionFormula;
import com.pha.trainees.config.ChemConfig;
import com.pha.trainees.network.AnalyzerReportPacket;
import com.pha.trainees.network.ModNetwork;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 分析仪诊断报告生成器（§19.6/19.7 合流设计的服务端侧）。
 *
 * <p>同一份诊断数据供两个出口使用：</p>
 * <ul>
 *   <li><b>GUI（方案 a）</b>：{@link #sendToClient} 打包推给客户端 {@code AnalyzerScreen}（诊断页 + 预测页）；</li>
 *   <li><b>聊天简报（方案 b）</b>：{@link #sendChatBrief} 潜行右键时发到聊天框（快速查看）。</li>
 * </ul>
 *
 * <p><b>本地化（§19.16）</b>：服务端只发"翻译键 + 参数"（{@link ReportLine}），
 * 玩家可见文本全部由客户端渲染；聊天简报直接用 {@link Component#translatable}（客户端同样会本地化）。</p>
 *
 * <p>数据来源全部是引擎既有查询：{@link ReactionEngine#predictReactions}（正向预测）、
 * 容器的 {@code getRecentFailures}（失败原因）与 {@code getRecentExecutedRules}（§19.7 决策 2c 竞争信息）。</p>
 */
public final class AnalyzerReport {

    private AnalyzerReport() {}

    private static final int MAX_FAILURE_LINES = 5;
    private static final int MAX_EXECUTED_LINES = 20;

    // ==================== 对外出口 ====================

    /** 打开诊断 GUI：服务端组装结构化报告并推给该玩家客户端。 */
    public static void sendToClient(ServerPlayer player, IChemicalContainer container) {
        ModNetwork.get().send(PacketDistributor.PLAYER.with(() -> player), build(container));
    }

    // ==================== 报告组装 ====================

    public static AnalyzerReportPacket build(IChemicalContainer container) {
        return new AnalyzerReportPacket(
                containerIdOf(container),
                summaryLines(container),
                contentLines(container),
                failureLines(container),
                executedLines(container),
                predictedLines(container, true),
                predictedLines(container, false));
    }

    private static List<ReportLine> summaryLines(IChemicalContainer container) {
        List<ReportLine> lines = new ArrayList<>();
        double temp = container.getTemperature();
        lines.add(ReportLine.of("gui.trainees.analyzer.line.temperature", fmt(temp, 1), fmt(temp - 273.15, 1)));
        lines.add(ReportLine.of("gui.trainees.analyzer.line.volume", fmt(container.getVolume(), 2)));
        lines.add(ReportLine.of("gui.trainees.analyzer.line.total",
                fmt(container.getTotalMoles(), 3), fmt(container.getMaxTotalMoles(), 0)));
        if (container.isElectricallyPowered()) {
            lines.add(ReportLine.of("gui.trainees.analyzer.line.powered", fmt(container.getVoltageFactor(), 2)));
        } else {
            lines.add(ReportLine.of("gui.trainees.analyzer.line.unpowered"));
        }
        // #10 / §19.27：开口（密闭与否）+ 总压。压强是导出量，不新增存档字段。
        Set<Direction> openFaces = container.getOpenFaces();
        if (openFaces.isEmpty()) {
            lines.add(ReportLine.of("gui.trainees.analyzer.line.seal.closed"));
        } else {
            StringBuilder sb = new StringBuilder();
            for (Direction d : openFaces) {
                if (sb.length() > 0) sb.append('/');
                sb.append(d.getName());
            }
            lines.add(ReportLine.of("gui.trainees.analyzer.line.seal.open",
                    String.valueOf(openFaces.size()), sb.toString()));
        }
        double idealPressure = container.getIdealPressureAtm();
        double ambient = GasPhysics.ambientAtm();
        double ratio = GasPhysics.overpressureRatio(idealPressure, ambient);
        lines.add(ReportLine.of("gui.trainees.analyzer.line.pressure",
                fmt(idealPressure, 3), fmt(ambient, 2), fmt(ratio, 2)));
        if (ratio >= ChemConfig.PRESSURE_OVER_RATIO.get()) {
            lines.add(ReportLine.raw("gui.trainees.analyzer.line.pressure.over", List.of(
                    ReportLine.ReportArg.of(fmt(ratio, 2)),
                    ReportLine.ReportArg.key(hazardStateKey()))));
        } else if (ratio > 0 && ratio <= ChemConfig.PRESSURE_UNDER_RATIO.get()) {
            lines.add(ReportLine.raw("gui.trainees.analyzer.line.pressure.under", List.of(
                    ReportLine.ReportArg.of(fmt(ratio, 2)),
                    ReportLine.ReportArg.key(hazardStateKey()))));
        }
        // §19.31：密闭时显示"耐压预算"（敞口无意义——敞口恒为环境压，永远不会超压）
        if (container.isSealed()) {
            double rating = container.getMaxSafePressureAtm();
            if (Double.isFinite(rating) && rating > 0) {
                double used = idealPressure / rating;
                lines.add(ReportLine.of(used >= 1.0
                                ? "gui.trainees.analyzer.line.pressure.rating.over"
                                : "gui.trainees.analyzer.line.pressure.rating",
                        fmt(rating, 2), fmt(idealPressure, 2), fmt(used * 100, 0)));
            }
        }
        // §19.32：腐蚀度（只在非零时显示；默认关掉腐蚀时自然不出现）
        double corrosion = container.getCorrosionLevel();
        if (corrosion > 1e-6) {
            lines.add(ReportLine.raw("gui.trainees.analyzer.line.corrosion", List.of(
                    ReportLine.ReportArg.of(fmt(corrosion * 100, 1)),
                    ReportLine.ReportArg.key("trainees.material." + container.getMaterialId()))));
        }
        // §19.29：附着件倍率（只有非 1.0 时才显示，避免烧杯刷屏）
        double heatFactor = container.getHeatTransferFactor();
        double gasFactor = container.getGasTransferFactor();
        if (Math.abs(heatFactor - 1.0) > 1e-6 || Math.abs(gasFactor - 1.0) > 1e-6) {
            lines.add(ReportLine.of("gui.trainees.analyzer.line.transfer",
                    fmt(heatFactor, 2), fmt(gasFactor, 2)));
        }
        // 机器类容器的专属状态（§19.14：电解槽的电极/隔膜/储能等走这个钩子）
        container.appendDiagnostics(lines);
        lines.add(ReportLine.of("gui.trainees.analyzer.line.position", container.getBlockPos().toShortString()));
        return lines;
    }

    /** 压强危险开关对应的提示键（默认关：只说明"未启用"，不施加任何后果，§19.27） */
    private static String hazardStateKey() {
        return ChemConfig.PRESSURE_HAZARD_ENABLED.get()
                ? "gui.trainees.analyzer.pressure.hazard.on"
                : "gui.trainees.analyzer.pressure.hazard.off";
    }

    private static List<ReportLine> contentLines(IChemicalContainer container) {
        List<ReportLine> lines = new ArrayList<>();
        Map<IonType, Double> contents = container.getContents();
        if (contents.isEmpty()) {
            lines.add(ReportLine.of("gui.trainees.analyzer.line.empty"));
            return lines;
        }
        double total = Math.max(container.getTotalMoles(), 1e-9);
        contents.entrySet().stream()
                .sorted(Comparator.comparingDouble((Map.Entry<IonType, Double> e) -> e.getValue()).reversed())
                .forEach(e -> {
                    IonType ion = e.getKey();
                    double pct = e.getValue() / total * 100.0;
                    lines.add(ReportLine.raw("gui.trainees.analyzer.line.content", List.of(
                            ReportLine.ReportArg.of(IonDisplay.format(ion.getId().getPath())),
                            ReportLine.ReportArg.of(fmt(e.getValue(), 4)),
                            ReportLine.ReportArg.of(fmt(pct, 1)),
                            ReportLine.ReportArg.key(ion.getForm().getTranslationKey()))));
                });
        return lines;
    }

    private static List<ReportLine> failureLines(IChemicalContainer container) {
        List<ReportLine> lines = new ArrayList<>();
        List<ReactionFailure> failures = container.getRecentFailures();
        if (failures.isEmpty()) {
            lines.add(ReportLine.of("gui.trainees.analyzer.line.empty"));
            return lines;
        }
        int shown = 0;
        for (int i = failures.size() - 1; i >= 0 && shown < MAX_FAILURE_LINES; i--, shown++) {
            ReactionFailure f = failures.get(i);
            lines.add(ReportLine.raw("gui.trainees.analyzer.line.failure", List.of(
                    ReportLine.ReportArg.key(failureNameKey(f.type())),
                    ReportLine.ReportArg.of(ReactionFormula.bold(ReactionFormula.formatById(f.ruleId()))))));
            lines.add(failureDetail(f));
        }
        return lines;
    }

    private static List<ReportLine> executedLines(IChemicalContainer container) {
        List<ReportLine> lines = new ArrayList<>();
        List<ExecutedRule> executed = container.getRecentExecutedRules();
        if (executed.isEmpty()) {
            lines.add(ReportLine.of("gui.trainees.analyzer.line.no_executed"));
            return lines;
        }
        int shown = 0;
        for (int i = executed.size() - 1; i >= 0 && shown < MAX_EXECUTED_LINES; i--, shown++) {
            ExecutedRule r = executed.get(i);
            lines.add(ReportLine.of("gui.trainees.analyzer.line.executed",
                    ReactionFormula.bold(ReactionFormula.formatById(r.ruleId())), fmt(r.deltaXi(), 4)));
            for (ExecutedRule.CompetitionNote note : r.competition()) {
                lines.add(ReportLine.of("gui.trainees.analyzer.line.competition",
                        note.blockedRuleId(), IonDisplay.format(pathOf(note.reactantId()))));
            }
        }
        return lines;
    }

    /** 预测页：executable=true 取可执行，false 取未达条件（带可操作建议）。 */
    private static List<ReportLine> predictedLines(IChemicalContainer container, boolean executable) {
        List<ReportLine> lines = new ArrayList<>();
        for (PredictedRule p : ReactionEngine.predictReactions(container)) {
            boolean isExec = p.status() == PredictedRule.Status.EXECUTABLE;
            if (isExec != executable) continue;
            String equation = ReactionFormula.bold(ReactionFormula.format(p.rule()));
            if (isExec) {
                lines.add(ReportLine.of("gui.trainees.analyzer.line.executable",
                        equation, argOr(p, 0, "-"), argOr(p, 1, "-")));
            } else {
                lines.add(ReportLine.raw("gui.trainees.analyzer.line.blocked", List.of(
                        ReportLine.ReportArg.key(predictedNameKey(p.status())),
                        ReportLine.ReportArg.of(equation))));
                lines.add(predictedDetail(p));
                lines.add(suggestion(container, p));
            }
        }
        if (lines.isEmpty()) {
            lines.add(ReportLine.of(executable
                    ? "gui.trainees.analyzer.line.no_executable"
                    : "gui.trainees.analyzer.line.no_blocked"));
        }
        return lines;
    }

    // ==================== 失败 / 预测 的明细与建议（结构化） ====================

    /** 失败明细行：按类型给键 + 参数 */
    private static ReportLine failureDetail(ReactionFailure f) {
        String key = failureDetailKey(f.type());
        List<ReportLine.ReportArg> args = new ArrayList<>();
        for (String a : f.args()) args.add(ReportLine.ReportArg.of(a));
        return ReportLine.raw(key, args);
    }

    /** 预测明细行：按状态（含 ALREADY_BALANCED 的两种变体）给键 + 参数 */
    private static ReportLine predictedDetail(PredictedRule p) {
        String key = predictedDetailKey(p);
        List<ReportLine.ReportArg> args = new ArrayList<>();
        for (String a : p.args()) args.add(ReportLine.ReportArg.of(a));
        return ReportLine.raw(key, args);
    }

    /** 可操作建议（§19.7：不只说"缺什么"，还要说"怎么补"） */
    private static ReportLine suggestion(IChemicalContainer container, PredictedRule p) {
        return switch (p.status()) {
            case TEMPERATURE_TOO_LOW -> ReportLine.of("gui.trainees.analyzer.suggest.temperature",
                    fmt(p.rule().getMinTemperatureAt(container.getEffectivePressureAtm()), 0),
                    fmt(container.getTemperature(), 0));
            case MISSING_PRECONDITION -> ReportLine.of("gui.trainees.analyzer.suggest.precondition",
                    names(p.rule().getPreconditions(), container));
            case MISSING_REACTANT -> ReportLine.of("gui.trainees.analyzer.suggest.reactant",
                    names(p.rule().getReactants(), container));
            case NOT_POWERED -> ReportLine.of("gui.trainees.analyzer.suggest.power");
            case ALREADY_BALANCED -> ReportLine.of("gui.trainees.analyzer.suggest.balanced");
            case EXECUTABLE -> ReportLine.of("gui.trainees.analyzer.suggest.executable");
        };
    }

    // ==================== 键名映射 ====================

    /** 失败原因名（玩家可读，客户端本地化） */
    public static String failureNameKey(ReactionFailure.Type type) {
        return "gui.trainees.analyzer.failure." + type.name().toLowerCase();
    }

    private static String failureDetailKey(ReactionFailure.Type type) {
        return "gui.trainees.analyzer.failure_detail." + type.name().toLowerCase();
    }

    private static String predictedNameKey(PredictedRule.Status status) {
        return "gui.trainees.analyzer.predicted." + status.name().toLowerCase();
    }

    /** ALREADY_BALANCED 有两种来源（引擎已标记 / Q≥K），用参数个数区分键 */
    private static String predictedDetailKey(PredictedRule p) {
        String base = "gui.trainees.analyzer.predicted_detail." + p.status().name().toLowerCase();
        if (p.status() == PredictedRule.Status.ALREADY_BALANCED) {
            return p.args().size() >= 2 ? base + ".q" : base + ".marked";
        }
        return base;
    }

    // ==================== 辅助 ====================

    private static String argOr(PredictedRule p, int index, String fallback) {
        return index < p.args().size() ? p.args().get(index) : fallback;
    }

    /** "namespace:path" → "path"（IonDisplay 需要 path） */
    private static String pathOf(String ionId) {
        int sep = ionId.indexOf(':');
        return sep >= 0 ? ionId.substring(sep + 1) : ionId;
    }

    /** 列出容器中缺失（≈0）的指定离子显示名；全齐时返回"（无缺失）" */
    private static String names(Map<IonType, Integer> required, IChemicalContainer container) {
        StringBuilder sb = new StringBuilder();
        for (IonType ion : required.keySet()) {
            if (container.getAmount(ion) <= 1e-9) {
                if (sb.length() > 0) sb.append("、");
                sb.append(IonDisplay.format(ion.getId().getPath()));
            }
        }
        return sb.length() == 0 ? "?" : sb.toString();
    }

    /**
     * 容器显示名的**翻译键**（方块 id 的 descriptionId，如 {@code block.trainees.beaker}）：
     * 由客户端本地化（§19.14 点 2 的同一规则：方块显示统一取 id 翻译键）。
     */
    private static String containerIdOf(IChemicalContainer container) {
        try {
            if (container.getLevel() != null) {
                return container.getLevel().getBlockState(container.getBlockPos()).getBlock()
                        .getDescriptionId();
            }
        } catch (Exception ignored) {
        }
        return "?";
    }

    private static String fmt(double v, int decimals) {
        return String.format("%." + decimals + "f", v);
    }
}
