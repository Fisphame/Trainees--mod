package com.pha.trainees.chemistry.material;

import com.pha.trainees.chemistry.particle.IonType;

import java.util.Map;

/**
 * 物质基类接口
 * 统一表示纯净物和混合物
 */
public interface IMaterial {

    /**
     * 获取成分列表：IonType → 摩尔数
     */
    Map<IonType, Double> getComposition();

    /**
     * 获取总摩尔数（所有成分之和）
     */
    double getTotalMoles();

    /**
     * 是否为纯净物（只有一种成分）
     */
    default boolean isPure() {
        return getComposition().size() == 1;
    }

    /**
     * 获取唯一成分（仅对纯净物有效）
     * @throws IllegalStateException 如果不是纯净物
     */
    default IonType getPrimaryIon() {
        Map<IonType, Double> comp = getComposition();
        if (comp.size() != 1) {
            throw new IllegalStateException("Not a pure substance: contains " + comp.size() + " components");
        }
        return comp.keySet().iterator().next();
    }

    /**
     * 获取主要成分的摩尔数（仅对纯净物有效）
     */
    default double getPrimaryMoles() {
        Map<IonType, Double> comp = getComposition();
        if (comp.size() != 1) {
            throw new IllegalStateException("Not a pure substance: contains " + comp.size() + " components");
        }
        return comp.values().iterator().next();
    }

    /**
     * 按比例缩放所有成分
     * @param factor 缩放因子（>0）
     * @return 新的IMaterial实例
     */
    IMaterial scale(double factor);

    /**
     * 检查是否包含指定离子
     */
    default boolean contains(IonType ion) {
        return getComposition().containsKey(ion);
    }

    /**
     * 获取指定离子的摩尔数
     */
    default double getAmount(IonType ion) {
        return getComposition().getOrDefault(ion, 0.0);
    }

    /**
     * 获取成分数量
     */
    default int getComponentCount() {
        return getComposition().size();
    }
}