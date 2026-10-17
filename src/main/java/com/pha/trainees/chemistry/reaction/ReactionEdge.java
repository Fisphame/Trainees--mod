package com.pha.trainees.chemistry.reaction;

import com.pha.trainees.chemistry.particle.IonType;

import java.util.Objects;

/**
 * 有向边
 * source → target，携带一个 ReactionRule 引用
 */
public class ReactionEdge {

    private final IonType source;
    private final IonType target;
    private final ReactionRule rule;

    public ReactionEdge(IonType source, IonType target, ReactionRule rule) {
        this.source = source;
        this.target = target;
        this.rule = rule;
    }

    public IonType getSource() { return source; }
    public IonType getTarget() { return target; }
    public ReactionRule getRule() { return rule; }

    public boolean isSelfLoop() {
        return source.equals(target);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReactionEdge that = (ReactionEdge) o;
        return Objects.equals(source, that.source) &&
                Objects.equals(target, that.target) &&
                Objects.equals(rule, that.rule);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, target, rule);
    }

    @Override
    public String toString() {
        return source.getId().getPath() + " → " + target.getId().getPath() + " (" + rule.getId() + ")";
    }
}