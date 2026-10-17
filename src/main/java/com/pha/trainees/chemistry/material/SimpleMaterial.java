package com.pha.trainees.chemistry.material;

import com.pha.trainees.chemistry.particle.IonType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * IMaterial 的简单实现
 * 用于动态混合物
 */
public class SimpleMaterial implements IMaterial {

    private final Map<IonType, Double> composition;
    private final double totalMoles;

    public SimpleMaterial(Map<IonType, Double> composition) {
        this.composition = new HashMap<>(composition);
        // 过滤掉 <= 0 的条目
        this.composition.entrySet().removeIf(e -> e.getValue() <= 1e-9);
        this.totalMoles = this.composition.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    @Override
    public Map<IonType, Double> getComposition() {
        return Map.copyOf(composition);
    }

    @Override
    public double getTotalMoles() {
        return totalMoles;
    }

    @Override
    public IMaterial scale(double factor) {
        if (factor <= 0) {
            throw new IllegalArgumentException("Scale factor must be positive");
        }
        Map<IonType, Double> scaled = new HashMap<>();
        for (Map.Entry<IonType, Double> entry : composition.entrySet()) {
            scaled.put(entry.getKey(), entry.getValue() * factor);
        }
        return new SimpleMaterial(scaled);
    }

    /**
     * 创建纯净物
     */
    public static SimpleMaterial of(IonType ion, double moles) {
        Map<IonType, Double> comp = new HashMap<>();
        comp.put(ion, moles);
        return new SimpleMaterial(comp);
    }

    /**
     * 从蓝图创建
     */
    public static SimpleMaterial fromBlueprint(SubstanceBlueprint blueprint, double units) {
        return new SimpleMaterial(blueprint.createComposition(units));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleMaterial that = (SimpleMaterial) o;
        return Double.compare(totalMoles, that.totalMoles) == 0 &&
                Objects.equals(composition, that.composition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(composition, totalMoles);
    }

    @Override
    public String toString() {
        return "SimpleMaterial{" + composition + ", total=" + totalMoles + "}";
    }
}