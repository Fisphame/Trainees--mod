package com.pha.trainees.util.game.chemistry;

import com.pha.trainees.fluid.BaseChemicalFluid;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用于机械方块的化学方程式类。
 * 包含反应物、生成物、反应条件、反应序号、时长（tick）、标签等信息。
 */
public class MachineChemicalEquation {

    private final String equationId;
    private final String name;
    private final String description;
    private final List<ChemicalComponent> reactants;
    private final List<ChemicalComponent> products;
    private final List<FluidComponent> fluidReactants;
    private final List<FluidComponent> fluidProducts;
    private final List<MachineCondition> conditions;
    private final int sequence;           // 反应序号
    private final int duration;            // 反应所需时长（单位：tick）
    private final Set<String> tags;        // 标签，如 "machine", "world"

    private MachineChemicalEquation(Builder builder) {
        this.equationId = builder.equationId;
        this.name = builder.name;
        this.description = builder.description;
        this.reactants = List.copyOf(builder.reactants);
        this.products = List.copyOf(builder.products);
        this.fluidReactants = List.copyOf(builder.fluidReactants);
        this.fluidProducts = List.copyOf(builder.fluidProducts);
        this.conditions = List.copyOf(builder.conditions);
        this.sequence = builder.sequence;
        this.duration = builder.duration;
        this.tags = Set.copyOf(builder.tags);
    }

    // ==================== Getter 方法 ====================

    public String getEquationId() { return equationId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<ChemicalComponent> getReactants() { return reactants; }
    public List<ChemicalComponent> getProducts() { return products; }
    public List<FluidComponent> getFluidReactants() { return fluidReactants; }
    public List<FluidComponent> getFluidProducts() { return fluidProducts; }
    public List<MachineCondition> getConditions() { return conditions; }
    public int getSequence() { return sequence; }
    public int getDuration() { return duration; }
    public Set<String> getTags() { return tags; }

    /**
     * 根据反应次数生成产物物品堆列表
     * @param reactionCount 反应次数
     * @return 产物列表（每个产物为一个 ItemStack）
     */
    public List<ItemStack> generateProducts(int reactionCount) {
        return products.stream()
                .map(comp -> comp.createStack(reactionCount))
                .collect(Collectors.toList());
    }

    // ==================== 构建器 ====================

    public static class Builder {
        private final String equationId;
        private String name;
        private String description = "";
        private final List<ChemicalComponent> reactants = new ArrayList<>();
        private final List<ChemicalComponent> products = new ArrayList<>();
        private final List<FluidComponent> fluidReactants = new ArrayList<>();
        private final List<FluidComponent> fluidProducts = new ArrayList<>();
        private final List<MachineCondition> conditions = new ArrayList<>();
        private int sequence = 1;               // 默认序号为1
        private int duration = 0;                // 默认时长为0（瞬时反应）
        private final Set<String> tags = new HashSet<>();

        public Builder(String equationId) {
            this.equationId = equationId;
            this.name = equationId; // 默认名称等于ID
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        /**
         * 添加反应物
         * @param coefficient 系数（摩尔数）
         * @param item 物品
         */
        public Builder addReactant(int coefficient, Item item) {
            reactants.add(new ChemicalComponent(coefficient, item));
            return this;
        }
        public Builder addFluidReactant(double moles, BaseChemicalFluid fluid) {
            fluidReactants.add(new FluidComponent(moles, fluid));
            return this;
        }

        /**
         * 添加生成物
         * @param coefficient 系数（摩尔数）
         * @param item 物品
         */
        public Builder addProduct(int coefficient, Item item) {
            products.add(new ChemicalComponent(coefficient, item));
            return this;
        }
        public Builder addFluidProduct(double moles, BaseChemicalFluid fluid) {
            fluidProducts.add(new FluidComponent(moles, fluid));
            return this;
        }

        /**
         * 添加一个反应条件
         */
        public Builder addCondition(MachineCondition condition) {
            conditions.add(condition);
            return this;
        }

        /**
         * 设置反应序号
         */
        public Builder withSequence(int sequence) {
            this.sequence = sequence;
            return this;
        }

        /**
         * 设置反应所需时长（单位：tick）
         */
        public Builder withDuration(int duration) {
            this.duration = duration;
            return this;
        }

        /**
         * 添加标签
         */
        public Builder addTag(String tag) {
            tags.add(tag);
            return this;
        }

        public MachineChemicalEquation build() {
            if (reactants.isEmpty()) {
                throw new IllegalStateException("Machine chemical equation must have at least one reactant");
            }
            if (products.isEmpty()) {
                throw new IllegalStateException("Machine chemical equation must have at least one product");
            }
            // 条件可以为空，所以不强制
            return new MachineChemicalEquation(this);
        }
    }
}
