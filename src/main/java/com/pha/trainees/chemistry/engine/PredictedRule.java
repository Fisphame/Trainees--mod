package com.pha.trainees.chemistry.engine;

import com.pha.trainees.chemistry.reaction.ReactionRule;

/**
 * 反应预测条目（§19.6 反应预测器 / §19.7 失败反馈的正向版数据源）。
 * 由 ReactionEngine.predictReactions 对容器内所有可达规则生成：当前可执行，或缺少什么条件。
 *
 * <p>{@code args} 是该状态的**结构化参数**（温度、Q、K、缺失物种…），
 * 由展示层（分析仪报告 / JEI）配上语言键本地化渲染，不在引擎里拼人造句子。</p>
 */
public record PredictedRule(ReactionRule rule, Status status, java.util.List<String> args) {

    public PredictedRule {
        args = args == null ? java.util.List.of() : java.util.List.copyOf(args);
    }

    /** 规则在当前容器状态下的预测状态 */
    public enum Status {
        /** 当前即可执行 */
        EXECUTABLE,
        /** 缺少前置条件（催化剂/介质等，检测存在但不消耗） */
        MISSING_PRECONDITION,
        /** 缺少反应物种类 */
        MISSING_REACTANT,
        /** 温度低于最低触发温度 */
        TEMPERATURE_TOO_LOW,
        /** 电解/电功反应未通电 */
        NOT_POWERED,
        /** Q ≥ K，已达平衡（或已被引擎标记平衡） */
        ALREADY_BALANCED
    }
}
