package com.pha.trainees.chemistry.engine;

import com.pha.trainees.Main;
import com.pha.trainees.chemistry.container.IChemicalContainer;
import com.pha.trainees.chemistry.particle.IonType;
import com.pha.trainees.chemistry.particle.Phase;
import com.pha.trainees.chemistry.reaction.ReactionEdge;
import com.pha.trainees.chemistry.reaction.ReactionGraph;
import com.pha.trainees.chemistry.reaction.ReactionRule;
import com.pha.trainees.chemistry.util.IonDisplay;
import com.pha.trainees.config.ChemConfig;
import com.pha.trainees.registry.ModChemistry;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 化学反应引擎
 * 采用双模式驱动：事件驱动（快速反应）+ Tick轮询（慢速反应）
 */
public class ReactionEngine {

    // ============================================================
    // 一、常量配置（可配置化，见 ChemConfig.engine 组）
    // ============================================================

    /** 每 Tick 最多处理的连锁反应数量，防止栈溢出（ChemConfig.ENGINE_MAX_CHAIN_REACTIONS_PER_TICK） */
    private static int getMaxChainReactionsPerTick() {
        return ChemConfig.ENGINE_MAX_CHAIN_REACTIONS_PER_TICK.get();
    }

    /** 反应进度截断阈值，防止 Zeno 悖论（ChemConfig.ENGINE_EPSILON） */
    private static double getEpsilon() {
        return ChemConfig.ENGINE_EPSILON.get();
    }

    /** Tick 轮询间隔（每 N Tick 执行一次完整轮询）（ChemConfig.ENGINE_POLLING_INTERVAL） */
    private static int getPollingInterval() {
        return ChemConfig.ENGINE_POLLING_INTERVAL.get();
    }

    /** 单次轮询最多处理的规则数量（ChemConfig.ENGINE_MAX_RULES_PER_POLL） */
    private static int getMaxRulesPerPoll() {
        return ChemConfig.ENGINE_MAX_RULES_PER_POLL.get();
    }

    // ============================================================
    // 二、状态管理（线程安全）
    // ============================================================

    /**
     * 待处理事件队列
     * 存储新加入的离子及其所属容器，等待对应容器在下一个 Tick 处理。
     * 必须关联容器：多个容器同时存在时，避免离子被错误地应用于其他容器。
     */
    private record QueuedEntry(IChemicalContainer container, IonType ion) {}

    private static final ConcurrentLinkedQueue<QueuedEntry> PENDING_QUEUE = new ConcurrentLinkedQueue<>();

    // ============================================================
    // 三、最近反应记录（环形缓冲区，蓝本 §14.2）
    // ============================================================

    private static final int MAX_RECENT_REACTIONS = 20;
    private static final ArrayDeque<ReactionRecord> RECENT_REACTIONS = new ArrayDeque<>(MAX_RECENT_REACTIONS);

    // 全局失败诊断记录（§19.6/19.7）：所有容器共享一份最近失败，供 /chemtester 等调试查看
    private static final int MAX_RECENT_FAILURES = 20;
    private static final ArrayDeque<ReactionFailure> RECENT_FAILURES = new ArrayDeque<>(MAX_RECENT_FAILURES);

    /** 一次成功执行的反应记录 */
    public record ReactionRecord(String ruleId, double deltaXi, double heatKj, long gameTime) {}

    private static void recordReaction(ReactionRule rule, double deltaXi, double heatKj, long gameTime) {
        synchronized (RECENT_REACTIONS) {
            if (RECENT_REACTIONS.size() >= MAX_RECENT_REACTIONS) {
                RECENT_REACTIONS.removeFirst();
            }
            RECENT_REACTIONS.addLast(new ReactionRecord(rule.getId().toString(), deltaXi, heatKj, gameTime));
        }
    }

    /** 获取最近反应记录（最旧在前），用于调试命令 /chemtester last */
    public static List<ReactionRecord> getRecentReactions() {
        synchronized (RECENT_REACTIONS) {
            return new ArrayList<>(RECENT_REACTIONS);
        }
    }

    /**
     * 记录一次规则执行失败：写入所属容器（供分析仪/预测器按容器读取）+ 全局缓冲（供调试命令）。
     * @param args 结构化参数（供展示层本地化渲染；可为空）
     */
    private static void recordFailure(IChemicalContainer container, ReactionFailure.Type type,
                                      ReactionRule rule, String detail, String... args) {
        // 相变等"安静规则"（§19.17.1）：门槛之外驱动力极小，会走 ε 截断 / Q≥K 分支，
        // 照常记录会刷满分析仪诊断页与失败环形缓冲，因此直接跳过。
        if (rule.isQuiet()) return;
        long gameTime = gameTimeOf(container);
        ReactionFailure failure = new ReactionFailure(type, rule.getId().toString(), detail, gameTime, List.of(args));
        container.recordFailure(failure);
        synchronized (RECENT_FAILURES) {
            if (RECENT_FAILURES.size() >= MAX_RECENT_FAILURES) {
                RECENT_FAILURES.removeFirst();
            }
            RECENT_FAILURES.addLast(failure);
        }
    }

    /** 获取全局最近失败记录（最旧在前），用于调试命令 */
    public static List<ReactionFailure> getRecentFailures() {
        synchronized (RECENT_FAILURES) {
            return new ArrayList<>(RECENT_FAILURES);
        }
    }

    /**
     * 反应预测查询（§19.6 反应预测器 / 失败反馈的正向版数据源）：
     * 遍历容器内所有可达规则，返回每条规则当前状态（可执行 / 缺什么条件）。
     * 纯查询，不修改容器状态，与执行路径解耦。
     */
    public static List<PredictedRule> predictReactions(IChemicalContainer container) {
        List<PredictedRule> result = new ArrayList<>();
        Set<ReactionRule> seen = new HashSet<>();
        double temp = container.getTemperature();
        // §19.27 Stage 2：温度门槛随压强移动（汽化/冷凝），故按"有效压强"取门槛
        double pressureAtm = container.getEffectivePressureAtm();
        for (IonType ion : container.getPresentIons()) {
            for (ReactionEdge edge : ReactionGraph.getInstance().getEdgesFrom(ion)) {
                ReactionRule rule = edge.getRule();
                if (!seen.add(rule)) continue;
                String ruleId = rule.getId().getPath();

                // 1. 电解/电功未通电
                if (rule.getElectricalWorkPerMol() > 0 && !container.isElectricallyPowered()) {
                    result.add(new PredictedRule(rule, PredictedRule.Status.NOT_POWERED, List.of()));
                    continue;
                }
                // 2. 温度不足
                if (temp < rule.getMinTemperatureAt(pressureAtm)) {
                    result.add(new PredictedRule(rule, PredictedRule.Status.TEMPERATURE_TOO_LOW,
                            List.of(fmt1(temp), fmt1(rule.getMinTemperatureAt(pressureAtm)))));
                    continue;
                }
                // 3. 前置条件缺失
                if (!rule.checkPreconditions(container)) {
                    result.add(new PredictedRule(rule, PredictedRule.Status.MISSING_PRECONDITION, List.of()));
                    continue;
                }
                // 4. 反应物种类不齐
                if (!container.containsAll(rule.getReactants())) {
                    result.add(new PredictedRule(rule, PredictedRule.Status.MISSING_REACTANT,
                            List.of(missingReactants(container, rule))));
                    continue;
                }
                // 5. 已达平衡（引擎已标记或 Q≥K）
                if (container.isRuleBalanced(rule)) {
                    result.add(new PredictedRule(rule, PredictedRule.Status.ALREADY_BALANCED, List.of()));
                    continue;
                }
                double Q = calculateQ(container, rule);
                double K = rule.calculateEquilibriumConstant(temp, container);
                if (Q >= K) {
                    result.add(new PredictedRule(rule, PredictedRule.Status.ALREADY_BALANCED,
                            List.of(fmtSci(Q), fmtSci(K))));
                    continue;
                }
                // 6. 可执行
                result.add(new PredictedRule(rule, PredictedRule.Status.EXECUTABLE,
                        List.of(fmtSci(Q), fmtSci(K))));
            }
        }
        return result;
    }

    // ============================================================
    // 三、核心入口方法
    // ============================================================

    /**
     * 事件驱动入口
     * 当有新粒子进入容器时调用
     * 将粒子加入待处理队列，不立即执行
     */
    public static void trigger(IChemicalContainer container, IonType source) {
        if (container == null || source == null) {
            Main.LOGGER.debug("[Engine] trigger called with null container or source");
            return;
        }

        // 将粒子加入队列（关联所属容器），延迟到对应容器下一 Tick 处理
        PENDING_QUEUE.offer(new QueuedEntry(container, source));

        // 重置该容器的平衡状态（新粒子的加入可能打破平衡）
        container.resetBalancedRules();

        Main.LOGGER.debug("[Engine] trigger() called for {} (queue size: {})",
                source.getId().getPath(), PENDING_QUEUE.size());    }

    /**
     * Tick 轮询驱动入口
     * 在容器的 tick() 方法中调用
     */
    public static void tick(IChemicalContainer container) {
        if (container == null || container.isRemoved()) {
//            Main.LOGGER.info("[Engine] ❌ tick() skipped: container is null or removed");
            return;
        }

        long gameTime = container.getLevel().getGameTime();
//        Main.LOGGER.info("[Engine] ⏰ tick() called (gameTime: {}, queue size: {}, pending rules: {})",
//                gameTime, PENDING_QUEUE.size());

        // ====== 1. 处理待处理队列（事件驱动） ======
        // 只处理属于当前容器的队列项；其他容器的项保留在队列中，由其自身的 tick 处理
        int processed = 0;
        var iterator = PENDING_QUEUE.iterator();
        while (iterator.hasNext() && processed < getMaxChainReactionsPerTick()) {
            QueuedEntry entry = iterator.next();
            if (entry.container() != container) continue;
            iterator.remove();
            Main.LOGGER.debug("[Engine] Processing queued ion: {}", entry.ion().getId().getPath());
            processTrigger(container, entry.ion());
            processed++;
        }

        // ====== 2. 定期执行 Tick 轮询（慢速反应 + 挂起规则重检） ======
        if (gameTime % getPollingInterval() == 0) {
//            Main.LOGGER.info("[Engine] 🔄 Polling triggered (gameTime % {} == 0)", getPollingInterval());
            processPolling(container);
        }
    }

    // ============================================================
    // 四、核心处理方法
    // ============================================================

    /**
     * 处理单个离子的触发事件
     */
    private static void processTrigger(IChemicalContainer container, IonType source) {
        List<ReactionEdge> edges = ReactionGraph.getInstance().getEdgesFrom(source);

        if (edges.isEmpty()) {
            Main.LOGGER.debug("[Engine] No edges from {}", source.getId().getPath());
            return;
        }

        List<ReactionEdge> executableEdges = new ArrayList<>();
        double pressureAtm = container.getEffectivePressureAtm();
        for (ReactionEdge edge : edges) {
            ReactionRule rule = edge.getRule();

            if (container.isRuleBalanced(rule)) {
                continue;
            }

            if (!rule.checkPreconditions(container)) {
                continue;
            }

            if (container.getTemperature() < rule.getMinTemperatureAt(pressureAtm)) {
                continue;
            }

            if (!container.containsAll(rule.getReactants())) {
                // 反应物不齐全：跳过（轮询会在反应物齐全后处理所有可执行规则）
                continue;
            }

            executableEdges.add(edge);
        }

        if (!executableEdges.isEmpty()) {
            executableEdges.sort((a, b) -> {
                double scoreA = a.getRule().calculatePriority(container);
                double scoreB = b.getRule().calculatePriority(container);
                return Double.compare(scoreB, scoreA);
            });

            ReactionEdge bestEdge = executableEdges.get(0);
            Main.LOGGER.debug("[Engine] Executing best edge: {} → {}",
                    bestEdge.getSource().getId().getPath(),
                    bestEdge.getTarget().getId().getPath());
            double deltaXi = executeRule(container, bestEdge);
            if (deltaXi > 0) {
                // 实际执行记录（§19.7 决策 2c）：事件路径无共享反应物竞争（每次只跑最高优先级一条）
                container.recordExecutedRule(new ExecutedRule(
                        bestEdge.getRule().getId().getPath(), deltaXi, gameTimeOf(container), List.of()));
            }
        }
    }

    /** 容器所在世界的游戏刻（无 Level 时返回 0，供诊断记录使用） */
    private static long gameTimeOf(IChemicalContainer container) {
        try {
            return container.getLevel() != null ? container.getLevel().getGameTime() : 0L;
        } catch (Exception ignored) {
            return 0L;
        }
    }

    /**
     * Tick 轮询处理
     */
    private static void processPolling(IChemicalContainer container) {
        int executed = 0;

        // ====== 处理所有当前可执行的规则（自环 + 非自环） ======
        // 非自环反应若仅靠事件驱动，一次添加只推进一个 Δξ 步后即停滞，
        // 故让所有可执行规则（含非自环）随轮询持续推进至平衡。
        // 注意必须按优先级排序：对共享反应物的竞争规则只执行最高优先级者，
        // 否则轮询会按边的迭代顺序误选产物（如碳酸盐的 HCO₃⁻/CO₂ 选择）。
        int maxRulesPerPoll = getMaxRulesPerPoll();
        if (executed < maxRulesPerPoll) {
            // 1. 收集所有可执行规则（同一规则经多条边只算一次）
            List<ReactionEdge> candidates = new ArrayList<>();
            Set<ReactionRule> seen = new HashSet<>();
            double pressureAtm = container.getEffectivePressureAtm();
            for (IonType ion : container.getPresentIons()) {
                for (ReactionEdge edge : ReactionGraph.getInstance().getEdgesFrom(ion)) {
                    ReactionRule rule = edge.getRule();
                    if (!seen.add(rule)) continue;
                    if (container.isRuleBalanced(rule)) continue;
                    if (!rule.checkPreconditions(container)
                            || container.getTemperature() < rule.getMinTemperatureAt(pressureAtm)) {
                        continue;
                    }
                    if (container.containsAll(rule.getReactants())) {
                        candidates.add(edge);
                    }
                }
            }
            // 2. 按优先级降序
            candidates.sort((a, b) -> Double.compare(
                    b.getRule().calculatePriority(container),
                    a.getRule().calculatePriority(container)));
            // 3. 贪心执行：共享反应物的竞争规则只执行最高优先级者；不相交的独立规则可并行
            Set<IonType> touched = new HashSet<>();
            // 2c：记录"谁消耗了哪个反应物"，以便把被挤掉的竞争规则挂到胜者名下（结构化，供展示层本地化）
            Map<IonType, String> touchedBy = new HashMap<>();
            Map<String, List<ExecutedRule.CompetitionNote>> competition = new LinkedHashMap<>();
            Map<String, Double> executedNow = new LinkedHashMap<>();
            for (ReactionEdge edge : candidates) {
                if (executed >= maxRulesPerPoll) break;
                ReactionRule rule = edge.getRule();
                String ruleId = rule.getId().getPath();
                boolean conflicts = false;
                for (IonType reactant : rule.getReactants().keySet()) {
                    if (touched.contains(reactant)) {
                        conflicts = true;
                        String winner = touchedBy.get(reactant);
                        if (winner != null) {
                            competition.computeIfAbsent(winner, k -> new ArrayList<>())
                                    .add(new ExecutedRule.CompetitionNote(ruleId, reactant.getId().toString()));
                        }
                    }
                }
                if (conflicts) continue;
                double deltaXi = executeRule(container, edge);
                if (deltaXi > 0) {
                    executed++;
                    executedNow.put(ruleId, deltaXi);
                    rule.getReactants().keySet().forEach(r -> {
                        touched.add(r);
                        touchedBy.put(r, ruleId);
                    });
                    rule.getProducts().keySet().forEach(touched::add);
                }
            }
            // 执行循环结束后竞争关系才完整，此时统一落盘本轮的执行记录
            long gameTime = gameTimeOf(container);
            for (Map.Entry<String, Double> e : executedNow.entrySet()) {
                container.recordExecutedRule(new ExecutedRule(e.getKey(), e.getValue(), gameTime,
                        competition.getOrDefault(e.getKey(), List.of())));
            }
        }
    }

    // ============================================================
    // 五、规则执行引擎（核心数学计算）
    // ============================================================

    /**
     * 执行一条规则
     * @return 成功时返回本次推进量 Δξ（&gt;0）；失败/被截断返回 0
     */
    private static double executeRule(IChemicalContainer container, ReactionEdge edge) {
        ReactionRule rule = edge.getRule();
        double epsilon = getEpsilon();
        // §19.27 Stage 2：按压强取门槛（汽化/冷凝的 T_b 随压强移动）
        double pressureAtm = container.getEffectivePressureAtm();

        // 再次验证所有条件（安全）
        if (!rule.checkPreconditions(container)) {
            recordFailure(container, ReactionFailure.Type.PRECONDITION_MISSING, rule,
                    "前置条件未满足（催化剂/介质等）");
            return 0.0;
        }
        if (container.getTemperature() < rule.getMinTemperatureAt(pressureAtm)) {
            recordFailure(container, ReactionFailure.Type.TEMPERATURE_TOO_LOW, rule,
                    String.format("温度 %.1fK < 最低 %.1fK", container.getTemperature(), rule.getMinTemperatureAt(pressureAtm)),
                    fmt1(container.getTemperature()), fmt1(rule.getMinTemperatureAt(pressureAtm)));
            return 0.0;
        }
        // 温度窗口上界（§19.17.1）：相变的"凝固/冷凝"侧门槛，也适用于热分解类规则
        if (container.getTemperature() > rule.getMaxTemperatureAt(pressureAtm)) {
            recordFailure(container, ReactionFailure.Type.TEMPERATURE_TOO_HIGH, rule,
                    String.format("温度 %.1fK > 上限 %.1fK", container.getTemperature(), rule.getMaxTemperatureAt(pressureAtm)),
                    fmt1(container.getTemperature()), fmt1(rule.getMaxTemperatureAt(pressureAtm)));
            return 0.0;
        }
        if (!container.containsAll(rule.getReactants())) {
            recordFailure(container, ReactionFailure.Type.REACTANT_MISSING, rule,
                    "反应物种类不齐（缺少 " + missingReactants(container, rule) + "）",
                    missingReactants(container, rule));
            return 0.0;
        }

        // 电解反应（需电功）未通电时不可运行
        if (rule.getElectricalWorkPerMol() > 0 && !container.isElectricallyPowered()) {
            recordFailure(container, ReactionFailure.Type.NOT_POWERED, rule,
                    "电解/电功反应未通电");
            return 0.0;
        }

        // ====== 1. 计算当前浓度 ======
        Map<IonType, Double> concentrations = new HashMap<>();
        for (IonType ion : rule.getReactants().keySet()) {
            concentrations.put(ion, container.getEffectiveConcentration(ion));
        }

        // ====== 2. 计算反应商 Q ======
        double Q = calculateQ(container, rule);

        // ====== 3. 计算当前平衡常数 K ======
        double K = rule.calculateEquilibriumConstant(container.getTemperature(), container);

        // ====== 4. 如果 Q >= K，反应已经处于平衡或逆向，不执行 ======
        if (Q >= K) {
            container.markRuleBalanced(rule);
            recordFailure(container, ReactionFailure.Type.ALREADY_BALANCED, rule,
                    String.format("已达平衡 Q=%.3e ≥ K=%.3e", Q, K), fmtSci(Q), fmtSci(K));
            return 0.0;
        }

        // ====== 5. 计算速率常数 k ======
        double k = rule.calculateRateConstant(container.getTemperature());

        // ====== 6. 计算正向速率 ======
        double forwardRate = k;
        for (Map.Entry<IonType, Integer> entry : rule.getReactants().entrySet()) {
            double conc = concentrations.getOrDefault(entry.getKey(), 0.0);
            forwardRate *= Math.pow(conc, entry.getValue());
        }

        // ====== 7. 计算净速率（考虑平衡限制） ======
        double netRate = forwardRate * (1.0 - Q / K);
        // Q>=K 已被步骤4拦截，此处 netRate<0 不应发生（防御性返回，但不标记平衡）
        if (netRate < 0) {
            recordFailure(container, ReactionFailure.Type.NET_RATE_NEGATIVE, rule,
                    String.format("净速率非正（Q/K=%.3e）", Q / K), fmtSci(Q / K));
            return 0.0;
        }

        // ====== 8. 计算本 Tick 的反应进度 Δξ ======
        double deltaTime = 0.05; // 1 Tick = 0.05 秒
        double deltaXiBase = netRate * deltaTime;
        double deltaXi = deltaXiBase;
        // 电解规则：工作电压档位放大本 Tick 进度（§19.11，模拟高电流高产出）。
        // 注意：到此已通过 NOT_POWERED 检查（电解规则必已通电），烧杯等 factor=1 不受影响。
        double voltageFactor = container.getVoltageFactor();
        if (rule.getElectricalWorkPerMol() > 0 && voltageFactor > 1.0) {
            deltaXi = deltaXiBase * voltageFactor;
        }

        // ====== 9. 进度截断（防 Zeno） ======
        // 注意：不在此标记 BALANCED——Δξ 过小可能只是低温等暂态，
        // 标记会导致加热后反应无法恢复（平衡标记仅由 Q>=K 与 addThermalEnergy 管理）
        if (deltaXi < epsilon) {
            recordFailure(container, ReactionFailure.Type.EPSILON_TRUNCATED, rule,
                    String.format("Δξ=%.3e 低于截断阈值（低温/低浓度暂态）", deltaXi), fmtSci(deltaXi));
            return 0.0;
        }

        // ====== 10. 检查是否有足够的反应物 ======
        for (Map.Entry<IonType, Integer> entry : rule.getReactants().entrySet()) {
            IonType ion = entry.getKey();
            double required = entry.getValue() * deltaXi;
            if (container.getAmount(ion) < required - epsilon) {
                // 反应物不足，调整 deltaXi
                double maxDelta = container.getAmount(ion) / entry.getValue();
                deltaXi = Math.min(deltaXi, maxDelta);
            }
        }

        // 如果调整后太小，放弃
        if (deltaXi < epsilon) {
            recordFailure(container, ReactionFailure.Type.REACTANT_INSUFFICIENT, rule,
                    "反应物存量不足以支撑本 Tick 进度（Δξ 被钳制后过小）");
            return 0.0;
        }

        // ====== 10.4 平衡约束（防欧拉越界） ======
        // 高速反应（高温/高浓度）Δξ 可能一步冲过平衡（Q 越过 K）。
        // 二分缩小 Δξ，使推进后的 Q ≈ K，保证不越过平衡点。
        if (deltaXi > 0) {
            double qAfter = calculateQAfter(container, rule, deltaXi);
            if (qAfter > K) {
                double lo = 0.0, hi = deltaXi;
                for (int i = 0; i < 8; i++) {
                    double mid = (lo + hi) / 2.0;
                    if (calculateQAfter(container, rule, mid) > K) {
                        hi = mid;
                    } else {
                        lo = mid;
                    }
                }
                deltaXi = lo;
                if (deltaXi < epsilon) {
                    recordFailure(container, ReactionFailure.Type.BALANCE_CLAMPED, rule,
                            "平衡约束二分后 Δξ 过小（接近平衡点）");
                    return 0.0;
                }
            }
        }

        // ====== 10.5 电解耗电 ======
        // 注入电功 = 每 mol 实际电功(W × 档位factor) × 本 Tick 推进量(Δξ，已含档位放大)。
        // 档位越高：每 mol 电功越高 × 产率越高 → 注入电功按 factor² 上升（高挡更费电，现实电压升高效率下降）
        double electricalWork = rule.getElectricalWorkPerMol();
        if (electricalWork > 0 && !container.consumeElectricalEnergy(electricalWork * voltageFactor * deltaXi)) {
            recordFailure(container, ReactionFailure.Type.INSUFFICIENT_ENERGY, rule,
                    String.format("电能不足（需 %.1f kJ）", electricalWork * voltageFactor * deltaXi),
                    fmt1(electricalWork * voltageFactor * deltaXi));
            return 0.0;
        }

        // ====== 11. 应用反应 ======
        // 消耗反应物（不触发引擎）
        for (Map.Entry<IonType, Integer> entry : rule.getReactants().entrySet()) {
            IonType ion = entry.getKey();
            double consumed = entry.getValue() * deltaXi;
            container.removeIon(ion, consumed, false);
        }

        // 生成产物（不触发引擎）
        for (Map.Entry<IonType, Integer> entry : rule.getProducts().entrySet()) {
            IonType ion = entry.getKey();
            double produced = entry.getValue() * deltaXi;
            // 产物处理钩子（§19.10）：容器可自行分拣导出（如电解槽）；返回 true 则不入 contents、不触发连锁
            if (container.onProductGenerated(rule, ion, produced)) {
                continue;
            }
            container.addIon(ion, produced, false);
            // 手动入队到 PENDING_QUEUE（仅当有出边时，关联所属容器）
            if (!ReactionGraph.getInstance().getEdgesFrom(ion).isEmpty()) {
                PENDING_QUEUE.offer(new QueuedEntry(container, ion));
            }
        }

        // ====== 12. 热力学反馈（净热账，§19.11） ======
        // 净热 = 电解注入电功 − 产物化学能需求(ΔH)：
        //   注入 = (W × factor) × Δξ   （每 mol 实际电功 × 推进量）
        //   - 非电解规则（电功=0）→ −ΔH×Δξ（原行为：放热升温/吸热降温）
        //   - 电解 factor=1（理论最低）→ (W − ΔH)×Δξ = −TΔS < 0 → 温和吸热降温
        //   - factor 超过热中性点(≈ΔH/ΔG) → 净发热（过电位/电阻热，档越高越烫）
        double workInjectedKj = electricalWork > 0
                ? electricalWork * voltageFactor * deltaXi
                : 0.0;
        double reactionHeat = workInjectedKj - rule.getDeltaH() * deltaXi; // kJ（正值为净发热）
        container.addThermalEnergy(reactionHeat * 1000); // 转为 J

        // 记录最近反应（环形缓冲区）
        recordReaction(rule, deltaXi, reactionHeat, container.getLevel().getGameTime());

        Main.LOGGER.debug("[Engine] Executed {}: Δξ={}, heat={}kJ",
                rule.getId(), String.format("%.6f", deltaXi), String.format("%.2f", reactionHeat));

        return deltaXi;
    }

    // ============================================================
    // 六、辅助计算函数
    // ============================================================

    /** 列出规则反应物中容器内缺失（存量≈0）的种类名（化学式显示），用于诊断参数 */
    private static String missingReactants(IChemicalContainer container, ReactionRule rule) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<IonType, Integer> entry : rule.getReactants().entrySet()) {
            if (container.getAmount(entry.getKey()) <= 1e-9) {
                if (sb.length() > 0) sb.append("、");
                sb.append(IonDisplay.format(entry.getKey().getId().getPath()));
            }
        }
        return sb.length() > 0 ? sb.toString() : "?";
    }

    /** 诊断参数格式化：1 位小数 */
    private static String fmt1(double v) {
        return String.format("%.1f", v);
    }

    /** 诊断参数格式化：科学计数（3 位有效） */
    private static String fmtSci(double v) {
        if (!Double.isFinite(v)) return "∞";
        return String.format("%.3e", v);
    }

    /**
     * 计算反应商 Q
     */
    private static double calculateQ(IChemicalContainer container, ReactionRule rule) {
        double numerator = 1.0;
        double denominator = 1.0;

        for (Map.Entry<IonType, Integer> entry : rule.getProducts().entrySet()) {
            double conc = container.getEffectiveConcentration(entry.getKey());
            numerator *= Math.pow(conc, entry.getValue());
        }

        for (Map.Entry<IonType, Integer> entry : rule.getReactants().entrySet()) {
            double conc = container.getEffectiveConcentration(entry.getKey());
            denominator *= Math.pow(conc, entry.getValue());
        }

        if (denominator < getEpsilon()) return Double.MAX_VALUE;
        return numerator / denominator;
    }

    /**
     * 计算按给定 Δξ 推进后的反应商 Q（不实际修改容器）。
     * 用于平衡约束：避免高速反应（高温/高浓度）一步越过平衡（Q 冲过 K）。
     */
    private static double calculateQAfter(IChemicalContainer container, ReactionRule rule, double deltaXi) {
        double numerator = 1.0;
        double denominator = 1.0;
        double volume = container.getVolume();
        for (Map.Entry<IonType, Integer> entry : rule.getProducts().entrySet()) {
            IonType ion = entry.getKey();
            double amount = container.getAmount(ion) + entry.getValue() * deltaXi;
            double conc = getActivityOrConcentration(ion, amount, volume);
            numerator *= Math.pow(conc, entry.getValue());
        }
        for (Map.Entry<IonType, Integer> entry : rule.getReactants().entrySet()) {
            IonType ion = entry.getKey();
            double amount = container.getAmount(ion) - entry.getValue() * deltaXi;
            double conc = getActivityOrConcentration(ion, Math.max(0, amount), volume);
            denominator *= Math.pow(conc, entry.getValue());
        }
        if (denominator < 1e-9) return Double.MAX_VALUE;
        return numerator / denominator;
    }

    /** 与 IChemicalContainer.getEffectiveConcentration 同语义：LIQUID/SOLID 活度 1，GAS/AQUEOUS 浓度 n/V */
    private static double getActivityOrConcentration(IonType ion, double amount, double volume) {
        Phase phase = ion.getPhase();
        if (phase == Phase.LIQUID || phase == Phase.SOLID) return 1.0;
        if (volume <= 0) return 0;
        return amount / volume;
    }

    // ============================================================
    // 七、调试与状态查询
    // ============================================================

    public static int getPendingQueueSize() {
        return PENDING_QUEUE.size();
    }

    public static void clearPendingQueue() {
        PENDING_QUEUE.clear();
    }
}