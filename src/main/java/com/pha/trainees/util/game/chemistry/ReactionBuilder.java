package com.pha.trainees.util.game.chemistry;

import com.pha.trainees.util.game.chemistry.ReactionSystem.*;

/**
 * 反应构建器，提供流畅API
 */
public class ReactionBuilder {
    private final String id;
    private RCondition condition;
    private RResult result;
    private int priority = 0;

    private ReactionBuilder(String id) {
        this.id = id;
    }

    /**
     * 创建构建器
     */
    public static ReactionBuilder create(String id) {
        return new ReactionBuilder(id);
    }

    /**
     * 设置条件
     */
    public ReactionBuilder withCondition(RCondition condition) {
        this.condition = condition;
        return this;
    }

    /**
     * 设置时间条件（使用时间提供器）
     */
    public ReactionBuilder withTimedCondition(RCondition baseCondition,
                                              IDurationProvider durationProvider) {
        String timerId = id + "_timer";
        this.condition = new TimedCondition(baseCondition, durationProvider, timerId, true);
        return this;
    }

    /**
     * 设置时间条件（使用固定时间）
     */
    public ReactionBuilder withTimedCondition(RCondition baseCondition,
                                              double fixedSeconds) {
        String timerId = id + "_timer";
        this.condition = new TimedCondition(baseCondition, fixedSeconds, timerId);
        return this;
    }

    /**
     * 设置结果
     */
    public ReactionBuilder withResult(RResult result) {
        this.result = result;
        return this;
    }

    /**
     * 设置优先级
     */
    public ReactionBuilder withPriority(int priority) {
        this.priority = priority;
        return this;
    }




    /**
     * 注册反应
     */
    public void register() {
        if (condition == null) {
            throw new IllegalStateException("Condition must be set before registering reaction");
        }
        if (result == null) {
            throw new IllegalStateException("Result must be set before registering reaction");
        }

        ReactionRegistry.registerReaction(id, condition, result, priority);
    }

}