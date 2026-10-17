package com.pha.trainees.chemistry.material;

import com.pha.trainees.chemistry.particle.IonType;
import com.pha.trainees.config.ChemConfig;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * 物质蓝图
 * 预定义的成分映射，用于矿石、海水等大宗原材料
 * 共享数据，不存储在物品NBT中
 */
public class SubstanceBlueprint {

    private final ResourceLocation id;
    private final Map<IonType, Double> composition;     // 成分比例（归一化到1份）
    private final Set<IonType> valuableComponents; // 标记有效成分
    private final double totalMolesPerUnit;              // 每份总摩尔数
    private final String displayName;                    // 显示名称（可选）

    private SubstanceBlueprint(Builder builder) {
        this.id = builder.id;
        this.composition = Map.copyOf(builder.composition);
        this.valuableComponents = Set.copyOf(builder.valuableComponents);
        this.totalMolesPerUnit = builder.totalMolesPerUnit;
        this.displayName = builder.displayName != null ? builder.displayName : id.getPath();
    }

    public ResourceLocation getId() { return id; }
    public Map<IonType, Double> getComposition() { return composition; }
    public Set<IonType> getValuableComponents() { return valuableComponents; }
    public double getTotalMolesPerUnit() { return totalMolesPerUnit; }
    public String getDisplayName() { return displayName; }

    /**
     * 检查指定离子是否为有效成分
     */
    public boolean isValuable(IonType ion) {
        return valuableComponents.contains(ion);
    }

    /**
     * 根据份数创建成分映射（带难度缩放）。
     * 每种成分的摩尔数 = 比例 × 每份总摩尔数 × 乘数 × 份数。
     * @param units 份数
     */
    public Map<IonType, Double> createComposition(double units) {
        // 统一走档位解析入口（§7.5 方案 C：档位唯一权威）
        double valuableMultiplier = ChemConfig.DifficultyPresets.valuableMultiplier();
        double gangueMultiplier = ChemConfig.DifficultyPresets.gangueMultiplier();
        return createComposition(units, valuableMultiplier, gangueMultiplier);
    }

    /**
     * 根据份数创建成分映射（带难度缩放）。
     * 每种成分的摩尔数 = 比例 × 每份总摩尔数 × 乘数 × 份数。
     * @param units 份数
     * @param valuableMultiplier 有效成分乘数
     * @param gangueMultiplier 脉石乘数
     */
    public Map<IonType, Double> createComposition(double units, double valuableMultiplier, double gangueMultiplier) {
        Map<IonType, Double> result = new HashMap<>();
        for (Map.Entry<IonType, Double> entry : composition.entrySet()) {
            IonType ion = entry.getKey();
            double baseRatio = entry.getValue();
            double multiplier = valuableComponents.contains(ion) ? valuableMultiplier : gangueMultiplier;
            result.put(ion, baseRatio * totalMolesPerUnit * multiplier * units);
        }
        return result;
    }

    /**
     * 根据份数创建IMaterial实例（使用默认乘数1.0，用于非难度场景）
     */
    public IMaterial createMaterial(double units) {
        return new SimpleMaterial(createComposition(units, 1.0, 1.0));
    }

    /**
     * 根据份数创建IMaterial实例（带难度缩放）
     */
    public IMaterial createMaterial(double units, double valuableMultiplier, double gangueMultiplier) {
        return new SimpleMaterial(createComposition(units, valuableMultiplier, gangueMultiplier));
    }

    @Override
    public String toString() {
        return "SubstanceBlueprint{" + id + ", composition=" + composition + ", perUnit=" + totalMolesPerUnit + "}";
    }

    // ==================== Builder ====================
    public static class Builder {
        private final ResourceLocation id;
        private final Map<IonType, Double> composition = new HashMap<>();
        private final Set<IonType> valuableComponents = new HashSet<>();
        private double totalMolesPerUnit = 1.0;
        private String displayName;


        public Builder(ResourceLocation id) {
            this.id = id;
        }

        /**
         * 添加一种成分（比例）
         */
        public Builder component(IonType ion, double ratio) {
            composition.put(ion, ratio);
            return this;
        }

        /**
         * 标记一种或多种成分为有效成分（矿石中有价值的成分）
         */
        public Builder valuable(IonType... ions) {
            valuableComponents.addAll(Arrays.asList(ions));
            return this;
        }

        public Builder totalMolesPerUnit(double val) {
            this.totalMolesPerUnit = val;
            return this;
        }

        public Builder displayName(String name) {
            this.displayName = name;
            return this;
        }

        public SubstanceBlueprint build() {
            if (composition.isEmpty()) {
                throw new IllegalStateException("Blueprint must have at least one component");
            }
            // 如果未标记有效成分，默认将所有成分视为有效
            if (valuableComponents.isEmpty()) {
                valuableComponents.addAll(composition.keySet());
            }
            // 检查有效成分是否都在成分中
            for (IonType ion : valuableComponents) {
                if (!composition.containsKey(ion)) {
                    throw new IllegalStateException("Valuable component " + ion + " not found in composition");
                }
            }
            // 归一化检查
            double sum = composition.values().stream().mapToDouble(Double::doubleValue).sum();
            if (sum < 1e-9) {
                throw new IllegalStateException("Blueprint components sum to near zero");
            }
            return new SubstanceBlueprint(this);
        }
    }
}