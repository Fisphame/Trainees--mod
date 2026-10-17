package com.pha.trainees.chemistry.data;

import com.pha.trainees.Main;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

/**
 * 化学内容的加载时机（§17.1 选择性公开，批次 2）。
 *
 * <p>服务器启动完成时读一次数据包里的化学内容。放在 {@code ServerStartedEvent} 而不是静态初始化里，
 * 是因为：① 数据包此时一定可用；② 注册对象此时已解析（静态初始化阶段碰注册对象会崩，见 A1 的事故记录）。</p>
 */
@Mod.EventBusSubscriber(modid = Main.MODID)
public final class ChemDataEvents {

    private ChemDataEvents() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        registerEngineBiasFunctions();
        ChemDataLoader.load(event.getServer().getResourceManager());
        // 注意：导出（见 onServerTick）**不能**放在这里——反应图是在 Done 之后才构建的
        // （冒烟日志里 "Reaction graph ready" 出现在 Done 之后），此时导出会得到 0 条规则。
    }

    /**
     * 开发用的一次性导出：若存在标记文件 {@code run/.dump-chemdata}，则在启动 5 秒后
     * 把硬编码内容导出成数据包 JSON（批次 3a），并删除标记。
     *
     * <p><b>为什么用标记文件而不是命令</b>：导出要能被脚本无人值守地触发（专服起完就导完），
     * 命令行注入 stdin 在 Gradle run 任务里不可靠。标记文件方案零配置、且**只在标记存在时**
     * 才动作（正常游戏绝不会触发）。人肉触发也可以：在 {@code run/} 下建一个同名空文件再重启。</p>
     *
     * <p>为什么要等到第 100 tick：反应图在服务器启动完成后才注册（见上），早于此会导出 0 条规则。</p>
     */
    @SubscribeEvent
    public static void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        if (event.getServer().getTickCount() != DUMP_TRIGGER_TICK) return;

        java.nio.file.Path marker = findDumpMarker();
        if (marker == null) return;

        // 输出到"标记文件所在目录"旁边的 chemdata-dump/：
        // 专服的 CWD 是 run/，而开发脚本可能从项目根建标记，所以两种位置都找、并就地输出。
        java.nio.file.Path target = marker.toAbsolutePath().getParent().resolve("chemdata-dump");
        ChemDataExporter.Report report = ChemDataExporter.export(target,
                id -> com.pha.trainees.registry.ModChemistry.ModIons.getById(id));
        ChemDataExporter.logReport(report);
        Main.LOGGER.info("[ChemData] 导出目录：{}", target);
        try {
            java.nio.file.Files.deleteIfExists(marker);
        } catch (java.io.IOException e) {
            Main.LOGGER.warn("[ChemData] 删除导出标记失败：{}", e.getMessage());
        }
    }

    /**
     * 找导出标记：同时接受"服务器工作目录下"与"项目根的 run/ 下"两种位置。
     *
     * <p>踩过的坑：专服的 CWD 是 {@code run/}，所以在项目根建 {@code run/.dump-chemdata}
     * 并不会被 {@code Path.of("run", ...)} 找到（那会解析成 {@code run/run/...}）。</p>
     */
    @Nullable
    private static java.nio.file.Path findDumpMarker() {
        for (java.nio.file.Path candidate : java.util.List.of(
                java.nio.file.Path.of(".dump-chemdata"),
                java.nio.file.Path.of("run", ".dump-chemdata"))) {
            if (java.nio.file.Files.exists(candidate)) return candidate;
        }
        return null;
    }

    /** 导出触发的服务器 tick（5 秒，足够反应图注册完成）。 */
    private static final int DUMP_TRIGGER_TICK = 100;

    /**
     * 双读比对（批次 4 第一步）：把**硬编码内容**与**数据包内容**逐条对齐并报告差异。
     *
     * <p>为什么放在第 100 tick：反应图（硬编码规则）是在服务器启动完成后才注册的，
     * 早于此硬编码侧还是空的（与导出同一个原因）。</p>
     *
     * <p>这一步**纯增量、零风险**：只读不写，不改变任何运行行为；它的价值是回答
     * "导出的数据包到底能不能替代硬编码定义"——差异为 0 才谈得上切换。</p>
     */
    @SubscribeEvent
    public static void onParityTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        if (event.getServer().getTickCount() != DUMP_TRIGGER_TICK) return;
        if (!PARITY_ENABLED) return;

        reportParity();
    }

    /** 双读比对开关：默认开（只读，不影响运行）；想安静时改成 false。 */
    private static final boolean PARITY_ENABLED = true;

    /** 执行一次双读比对并打日志。 */
    public static void reportParity() {
        // 硬编码规则：图中的边按 id 去重（一条规则可能对应多条边）
        java.util.Map<net.minecraft.resources.ResourceLocation,
                com.pha.trainees.chemistry.reaction.ReactionRule> uniqueRules = new java.util.LinkedHashMap<>();
        for (com.pha.trainees.chemistry.reaction.ReactionEdge edge
                : com.pha.trainees.chemistry.reaction.ReactionGraph.getInstance().getAllEdges()) {
            uniqueRules.putIfAbsent(edge.getRule().getId(), edge.getRule());
        }

        java.util.List<ChemDataParity.Difference> diffs = new java.util.ArrayList<>();
        diffs.addAll(ChemDataParity.compareIons(
                com.pha.trainees.registry.ModChemistry.ModIons.all(), ChemDataRegistry.ions()));
        diffs.addAll(ChemDataParity.compareRules(uniqueRules.values(), ChemDataRegistry.rules()));

        Main.LOGGER.info("[ChemData] 双读比对：硬编码 {} 物质 / {} 规则；数据包 {} 物质 / {} 规则；差异 {} 条",
                com.pha.trainees.registry.ModChemistry.ModIons.all().size(), uniqueRules.size(),
                ChemDataRegistry.ionCount(), ChemDataRegistry.ruleCount(), diffs.size());
        if (diffs.isEmpty()) {
            Main.LOGGER.info("[ChemData] 双读比对通过：数据包与硬编码逐字段一致（可以安全切换）");
            return;
        }
        int shown = 0;
        for (ChemDataParity.Difference diff : diffs) {
            if (shown++ >= 40) {
                Main.LOGGER.error("[ChemData] 差异过多，仅显示前 40 条（共 {} 条）", diffs.size());
                break;
            }
            Main.LOGGER.error("[ChemData] 差异 {}", diff);
        }
    }

    /**
     * 注册**引擎侧**的命名偏置函数（契约 §3.2：函数体在引擎、公开；数据只引用 id）。
     *
     * <p>这里注册一个教学用的示例函数，用来把"命名函数"这条契约跑通（示例数据包里的
     * {@code demo_alpha_to_beta.json} 就引用它）。真实内容包可以引用同一个函数，或由<b>引擎</b>
     * 注册更多函数（内容包自己不能注册函数——那是机制层）。</p>
     */
    private static void registerEngineBiasFunctions() {
        BiasFunctionRegistry.register(
                new net.minecraft.resources.ResourceLocation(Main.MODID, "demo_bias"),
                container -> container.getTemperature() > 350.0 ? 2.0 : 1.0);
    }
}
