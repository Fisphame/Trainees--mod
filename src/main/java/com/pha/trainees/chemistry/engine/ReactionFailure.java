package com.pha.trainees.chemistry.engine;

import java.util.List;

/**
 * 一次规则执行失败的诊断记录（§19.6/19.7 失败反馈与反应预测器的公共底座）。
 * 由 ReactionEngine.executeRule 在每次失败时生成：
 * - 容器侧保留最近 N 条（BeakerBlockEntity.recentFailures，仅运行时、不存 NBT），供分析仪/预测器按容器读取；
 * - 引擎侧保留全局最近 M 条（环形缓冲），供 /chemtester 等调试命令汇总查看。
 *
 * <p>{@code detail} 是服务端调试用的可读文本（日志/调试命令）；
 * {@code args} 是给展示层本地化用的结构化参数（温度、Q、K、缺失物种…），
 * 玩家可见文本一律由语言键 + args 渲染（§19.16 语言键一并处理）。</p>
 */
public record ReactionFailure(Type type, String ruleId, String detail, long gameTime, List<String> args) {

    public ReactionFailure {
        args = args == null ? List.of() : List.copyOf(args);
    }

    /** 简化构造：无结构化参数（固定文案的失败类型） */
    public ReactionFailure(Type type, String ruleId, String detail, long gameTime) {
        this(type, ruleId, detail, gameTime, List.of());
    }

    /** 失败类型 */
    public enum Type {
        /** 温度低于规则最低触发温度 */
        TEMPERATURE_TOO_LOW,
        /** 温度高于规则可运行上限（相变温度窗口、热分解窗口） */
        TEMPERATURE_TOO_HIGH,
        /** 前置条件缺失（催化剂/介质等，检测存在但不消耗） */
        PRECONDITION_MISSING,
        /** 反应物种类不齐（存在性检查失败） */
        REACTANT_MISSING,
        /** 电解/电功反应未通电 */
        NOT_POWERED,
        /** Q ≥ K，已达平衡（规则被标记为平衡，跳过执行） */
        ALREADY_BALANCED,
        /** 净速率非正（防御分支，Q≥K 已被拦截，理论上不应出现） */
        NET_RATE_NEGATIVE,
        /** Δξ 小于截断阈值（低温/低浓度暂态，非真平衡） */
        EPSILON_TRUNCATED,
        /** 反应物存量不足以支撑本 Tick 进度（Δξ 被调整后过小） */
        REACTANT_INSUFFICIENT,
        /** 平衡约束二分后 Δξ 过小（高速反应被钳制在平衡点附近） */
        BALANCE_CLAMPED,
        /** 电能不足，无法扣除本 Tick 电功 */
        INSUFFICIENT_ENERGY
    }
}
