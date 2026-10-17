package com.pha.trainees.chemistry.reaction;

import com.pha.trainees.chemistry.container.IChemicalContainer;
import com.pha.trainees.chemistry.particle.IonType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 有向图存储所有反应规则
 * 邻接表：Map<IonType, List<ReactionEdge>>
 */
public class ReactionGraph {

    private static ReactionGraph INSTANCE;

    private final Map<IonType, List<ReactionEdge>> adjacencyMap = new ConcurrentHashMap<>();

    /** 规则 id → 规则 的懒建索引（诊断显示用；图变化时置空失效） */
    private volatile Map<String, ReactionRule> ruleIndex;

    private ReactionGraph() {}

    public static ReactionGraph getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ReactionGraph();
        }
        return INSTANCE;
    }

    /**
     * 添加一条有向边
     * 如果 source 和 target 相同，只添加一条自环（避免重复）
     */
    public void addEdge(IonType source, IonType target, ReactionRule rule) {
        if (source == null || target == null || rule == null) {
            throw new IllegalArgumentException("Source, target and rule cannot be null");
        }
        ruleIndex = null; // 图变化 → 索引失效

        // 自环去重
        if (source.equals(target)) {
            List<ReactionEdge> edges = adjacencyMap.computeIfAbsent(source, k -> new ArrayList<>());
            // 检查是否已存在相同的自环规则
            for (ReactionEdge edge : edges) {
                if (edge.isSelfLoop() && edge.getRule().getId().equals(rule.getId())) {
                    return; // 已存在，不重复添加
                }
            }
            edges.add(new ReactionEdge(source, target, rule));
            return;
        }

        // 普通边
        adjacencyMap.computeIfAbsent(source, k -> new ArrayList<>())
                .add(new ReactionEdge(source, target, rule));
    }

    /**
     * 批量添加双向边（用于多反应物完全触发矩阵）
     */
    public void addBidirectionalEdges(IonType a, IonType b, ReactionRule rule) {
        addEdge(a, b, rule);
        addEdge(b, a, rule);
    }

    /**
     * 获取从某个节点出发的所有边
     */
    public List<ReactionEdge> getEdgesFrom(IonType source) {
        return adjacencyMap.getOrDefault(source, Collections.emptyList());
    }

    /**
     * 获取所有以 source 为起点，且 target 在容器中存在的边
     * 用于事件驱动匹配
     */
    public List<ReactionEdge> getMatchingEdges(IonType source, IChemicalContainer container) {
        List<ReactionEdge> result = new ArrayList<>();
        List<ReactionEdge> edges = getEdgesFrom(source);
        for (ReactionEdge edge : edges) {
            IonType target = edge.getTarget();
            // 自环：检查容器中是否有足够的反应物
            if (edge.isSelfLoop()) {
                ReactionRule rule = edge.getRule();
                boolean allPresent = true;
                for (Map.Entry<IonType, Integer> entry : rule.getReactants().entrySet()) {
                    if (container.getAmount(entry.getKey()) < entry.getValue()) {
                        allPresent = false;
                        break;
                    }
                }
                if (allPresent) {
                    result.add(edge);
                }
            } else {
                // 普通边：检查目标粒子是否存在
                if (container.getAmount(target) > 0) {
                    result.add(edge);
                }
            }
        }
        return result;
    }

    /**
     * 获取图的所有节点
     */
    public Set<IonType> getAllNodes() {
        return adjacencyMap.keySet();
    }

    /**
     * 获取所有边（用于调试）
     */
    public List<ReactionEdge> getAllEdges() {
        List<ReactionEdge> allEdges = new ArrayList<>();
        for (List<ReactionEdge> edges : adjacencyMap.values()) {
            allEdges.addAll(edges);
        }
        return allEdges;
    }

    /**
     * 按规则 id 查找规则（§19.14：诊断记录只保存 ruleId，显示时需要还原方程式）。
     * 支持 `path` 或 `namespace:path` 两种写法；找不到返回 null。
     * 索引懒构建，注册/清空图时失效。
     */
    public ReactionRule getRuleById(String ruleId) {
        if (ruleId == null) return null;
        Map<String, ReactionRule> index = ruleIndex;
        if (index == null) {
            index = buildRuleIndex();
            ruleIndex = index;
        }
        ReactionRule rule = index.get(ruleId);
        if (rule != null) return rule;
        // 允许用 "trainees:xxx" 形式查询
        int sep = ruleId.indexOf(':');
        return sep >= 0 ? index.get(ruleId.substring(sep + 1)) : null;
    }

    private Map<String, ReactionRule> buildRuleIndex() {
        Map<String, ReactionRule> index = new HashMap<>();
        for (List<ReactionEdge> edges : adjacencyMap.values()) {
            for (ReactionEdge edge : edges) {
                ReactionRule rule = edge.getRule();
                if (rule == null || rule.getId() == null) continue;
                index.putIfAbsent(rule.getId().getPath(), rule);
                index.putIfAbsent(rule.getId().toString(), rule);
            }
        }
        return index;
    }

    /**
     * 清空图（用于热加载或调试）
     */
    public void clear() {
        adjacencyMap.clear();
        ruleIndex = null;
    }
}