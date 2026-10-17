package com.pha.trainees.chemistry.gas;

import com.pha.trainees.chemistry.particle.IonType;

import java.util.HashMap;
import java.util.Map;

/**
 * 单个方块格的混合气体（Phase 9，§19.13）。
 * 成分以 mol 计（与容器体系一致）；温度 K。
 */
public class GasMixture {

    private final Map<IonType, Double> contents = new HashMap<>();
    private double temperature;

    public GasMixture() {
    }

    public GasMixture(double temperature) {
        this.temperature = temperature;
    }

    // ==================== 成分 ====================

    /** 加入气体（mol）；不足 1e-9 视为零并清除条目 */
    public void addGas(IonType ion, double moles) {
        if (moles <= 0 || ion == null) return;
        contents.merge(ion, moles, Double::sum);
        if (contents.get(ion) <= 1e-9) {
            contents.remove(ion);
        }
    }

    /** 移除气体（mol），返回实际移除量 */
    public double removeGas(IonType ion, double moles) {
        double current = contents.getOrDefault(ion, 0.0);
        double removed = Math.min(current, Math.max(0, moles));
        double left = current - removed;
        if (left <= 1e-9) {
            contents.remove(ion);
        } else {
            contents.put(ion, left);
        }
        return removed;
    }

    public double getAmount(IonType ion) {
        return contents.getOrDefault(ion, 0.0);
    }

    public Map<IonType, Double> getContents() {
        return contents;
    }

    /** 是否为空（无任何气体） */
    public boolean isEmpty() {
        return contents.isEmpty();
    }

    public void clear() {
        contents.clear();
    }

    /** 总摩尔数 */
    public double totalMoles() {
        double sum = 0;
        for (double v : contents.values()) {
            sum += v;
        }
        return sum;
    }

    // ==================== 温度 ====================

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double kelvin) {
        this.temperature = kelvin;
    }

    @Override
    public String toString() {
        return "GasMixture{temp=" + String.format("%.0f", temperature) + "K, moles="
                + String.format("%.3f", totalMoles()) + ", species=" + contents.size() + "}";
    }
}
