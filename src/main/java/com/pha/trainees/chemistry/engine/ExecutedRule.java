package com.pha.trainees.chemistry.engine;

import java.util.List;

/**
 * 一次**实际执行成功**的反应记录（§19.7 补充决策 2c）。
 *
 * <p>与 {@link ReactionFailure}（执行失败）配对：失败记录回答"为什么没反应"，
 * 本记录回答"原料被谁消耗了"——{@code competition} 列出本次执行顺带挤掉的竞争规则，
 * 避免诊断只报"缺少 X"而玩家不知道 X 被哪条反应抢走。</p>
 *
 * @param ruleId      实际执行的规则 id（path）
 * @param deltaXi     本次推进量 Δξ（mol）
 * @param gameTime    执行时的游戏刻
 * @param competition 被本规则挤掉的竞争说明（结构化，供展示层本地化渲染）
 */
public record ExecutedRule(String ruleId, double deltaXi, long gameTime, List<CompetitionNote> competition) {

    public ExecutedRule {
        competition = competition == null ? List.of() : List.copyOf(competition);
    }

    /**
     * 一条竞争说明：{@code blockedRuleId} 因为共享反应物 {@code reactantId} 被本规则抢先消耗而未能执行。
     * 只存 id，玩家可见文本由展示层用语言键 + 化学式拼装（§19.16）。
     */
    public record CompetitionNote(String blockedRuleId, String reactantId) {}
}
