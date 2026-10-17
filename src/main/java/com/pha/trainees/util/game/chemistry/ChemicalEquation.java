package com.pha.trainees.util.game.chemistry;

import com.pha.trainees.util.types.NumedItemEntities;
import com.pha.trainees.util.math.MathT;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import static com.pha.trainees.util.game.chemistry.ReactionConditions.*;
import static com.pha.trainees.util.game.Tools.AddEntity;
import static com.pha.trainees.util.game.Tools.AddNIEs;

import java.util.*;
import com.pha.trainees.util.game.chemistry.ReactionSystem.*;

/**
 * 化学方程式类
 * 表示一个完整的化学反应
 */
public class ChemicalEquation {
    // ==================== 内部类定义 ====================

    /**
     * 反应物/生成物项
     */
    public static class ChemicalComponent {
        public final Item item;
        public final int coefficient;  // 系数（摩尔数）
        public final String description; // 可选描述

        public ChemicalComponent(Item item, int coefficient) {
            this(item, coefficient, "");
        }

        public ChemicalComponent(Item item, int coefficient, String description) {
            this.item = item;
            this.coefficient = coefficient;
            this.description = description;
        }

        /**
         * 创建物品堆栈
         */
        public ItemStack createStack(int reactionCount) {
            int count = coefficient * reactionCount;
            return new ItemStack(item, count);
        }

        @Override
        public String toString() {
            return coefficient + " " + item.getDescriptionId() +
                    (description.isEmpty() ? "" : " (" + description + ")");
        }
    }

    /**
     * 反应条件组
     */
    public static class EquationConditions {
        public final RCondition mainCondition;
        public final IDurationProvider durationProvider; // 时间提供器
        public final boolean isTimed; // 是否为时间反应

        public EquationConditions(RCondition mainCondition) {
            this(mainCondition, null, false);
        }

        public EquationConditions(RCondition mainCondition,
                                  IDurationProvider durationProvider
                                  ) {
            this(mainCondition, durationProvider, true);
        }

        private EquationConditions(RCondition mainCondition,
                                   IDurationProvider durationProvider,
                                   boolean isTimed) {
            this.mainCondition = mainCondition;
            this.durationProvider = durationProvider;
            this.isTimed = isTimed;
        }
    }

    // ==================== 方程属性 ====================

    private final String equationId;          // 方程ID
    private final String name;               // 方程名称（可显示）
    private final String description;        // 方程描述

    // 反应物（可以有多个）
    private final List<ChemicalComponent> reactants;
    // 生成物（可以有多个）
    private final List<ChemicalComponent> products;
    // 反应条件
    private final EquationConditions conditions;
    // 催化剂（可选）
    private final List<ChemicalComponent> catalysts;
    // 反应类型标签
    private final Set<String> tags;

    // 最小反应次数（基于最小公倍数）
    private final int minReactionCount;

    // ==================== 构建器模式 ====================

    public static class Builder {
        private final String equationId;
        private String name;
        private String description = "";
        private final List<ChemicalComponent> reactants = new ArrayList<>();
        private final List<ChemicalComponent> products = new ArrayList<>();
        private EquationConditions conditions = null;
        private final List<ChemicalComponent> catalysts = new ArrayList<>();
        private final Set<String> tags = new HashSet<>();

        public Builder(String equationId) {
            this.equationId = equationId;
            this.name = equationId;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder addReactant(Item item, int coefficient) {
            return addReactant(item, coefficient, "");
        }

        public Builder addReactant(Item item, int coefficient, String desc) {
            reactants.add(new ChemicalComponent(item, coefficient, desc));
            return this;
        }

        public Builder addProduct(Item item, int coefficient) {
            return addProduct(item, coefficient, "");
        }

        public Builder addProduct(Item item, int coefficient, String desc) {
            products.add(new ChemicalComponent(item, coefficient, desc));
            return this;
        }

        public Builder withConditions(RCondition condition) {
            this.conditions = new EquationConditions(condition);
            return this;
        }

        public Builder withTimedConditions(RCondition condition,
                                           IDurationProvider durationProvider
                                           ) {
            this.conditions = new EquationConditions(condition, durationProvider);
            return this;
        }

        public Builder addCatalyst(Item item, int coefficient) {
            return addCatalyst(item, coefficient, "");
        }

        public Builder addCatalyst(Item item, int coefficient, String desc) {
            catalysts.add(new ChemicalComponent(item, coefficient, desc));
            return this;
        }

        public Builder addTag(String tag) {
            tags.add(tag);
            return this;
        }

        public ChemicalEquation build() {
            if (reactants.isEmpty()) {
                throw new IllegalStateException("Chemical equation must have at least one reactant");
            }
            if (products.isEmpty()) {
                throw new IllegalStateException("Chemical equation must have at least one product");
            }
            if (conditions == null) {
                throw new IllegalStateException("Chemical equation must have conditions");
            }

            return new ChemicalEquation(this);
        }
    }

    // ==================== 构造函数 ====================

    private ChemicalEquation(Builder builder) {
        this.equationId = builder.equationId;
        this.name = builder.name;
        this.description = builder.description;
        this.reactants = List.copyOf(builder.reactants);
        this.products = List.copyOf(builder.products);
        this.conditions = builder.conditions;
        this.catalysts = List.copyOf(builder.catalysts);
        this.tags = Set.copyOf(builder.tags);

        // 计算最小反应次数（所有反应物系数的最小公倍数）
        this.minReactionCount = calculateMinReactionCount();
    }

    // ==================== 计算方法 ====================

    /**
     * 计算最小反应次数（基于反应物系数的最小公倍数）
     */
    private int calculateMinReactionCount() {
        if (reactants.isEmpty()) return 1;

        // 对于多个反应物，我们需要找到系数的最大公约数
        // 然后确定每个反应物至少需要多少个才能反应
        int[] coefficients = reactants.stream()
                .mapToInt(c -> c.coefficient)
                .toArray();

        // 使用欧几里得算法求最大公约数
        int gcd = coefficients[0];
        for (int i = 1; i < coefficients.length; i++) {
            gcd = MathT.gcd(gcd, coefficients[i]);
        }

        // 最小反应次数是每个反应物系数除以最大公约数后的最小公倍数
        // 但简化处理：我们只需要确保至少能满足最小整数倍
        return MathT.lcmArray(coefficients);
    }



    // ==================== 核心方法 ====================

    /**
     * 检查给定物品堆栈是否满足反应物要求
     */
    public boolean canReact(ItemStack stack) {
        // 目前只支持单一反应物的情况
        // 未来可以扩展为多反应物检查
        if (reactants.size() != 1) {
            // 多反应物情况需要特殊处理
            return false;
        }

        ChemicalComponent reactant = reactants.get(0);
        return stack.getItem() == reactant.item &&
                stack.getCount() >= reactant.coefficient;
    }

    /**
     * 计算最大反应次数
     * @param stack 反应物堆栈
     * @return 可以进行的反应次数
     */
    public int calculateReactionCount(ItemStack stack) {
        if (!canReact(stack)) return 0;

        ChemicalComponent reactant = reactants.get(0);
        return stack.getCount() / reactant.coefficient;
    }

    /**
     * 执行反应
     * @param level 世界
     * @param entity 物品实体
     * @param stack 物品堆栈
     * @return 是否成功执行反应
     */
    public boolean executeReaction(Level level,
                                   ItemEntity entity,
                                   ItemStack stack) {
        if (!canReact(stack)) return false;

        int reactionCount = calculateReactionCount(stack);
        if (reactionCount == 0) return false;

        // 计算消耗数量
        ChemicalComponent reactant = reactants.get(0);
        int consumed = reactionCount * reactant.coefficient;
        int remaining = stack.getCount() - consumed;

        // 处理原实体
        entity.discard();

        // 如果有剩余，创建剩余物品实体
        if (remaining > 0) {
            ItemStack remainderStack = new ItemStack(reactant.item, remaining);
            NumedItemEntities itemEntities = AddEntity(level, entity, remainderStack, 1);
            AddNIEs(entity, itemEntities, false);
        }

        // 生成所有生成物
        for (ChemicalComponent product : products) {
            ItemStack productStack = product.createStack(reactionCount);
            if (!productStack.isEmpty()) {
                NumedItemEntities itemEntities = AddEntity(level, entity, productStack, 1);
                AddNIEs(entity, itemEntities, false);
            }
        }

        return true;
    }

    // ==================== 注册相关方法 ====================

    /**
     * 将化学方程式注册到反应系统中
     */
    public void register(int priority) {
        if (conditions.isTimed) {
            // 注册为时间反应
            registerAsTimedReaction(priority);
        } else {
            // 注册为即时反应
            registerAsInstantReaction(priority);
        }
    }

    private void registerAsInstantReaction(int priority) {
        // 创建条件：需要满足方程式的条件，并且有足够的反应物
        RCondition combinedCondition = and(
                conditions.mainCondition,
                (stack, entity) -> canReact(stack)
        );

        ReactionSystem.ReactionRegistry.registerReaction(
                equationId,
                combinedCondition,
                this::executeReaction,
                priority
        );
    }

    private void registerAsTimedReaction(int priority) {
        // 对于时间反应，需要包装条件
        RCondition baseCondition = and(
                conditions.mainCondition,
                (stack, entity) -> canReact(stack)
        );

        ReactionSystem.ReactionRegistry.registerTimedReaction(
                equationId,
                baseCondition,
                this::executeReaction,
                conditions.durationProvider,
                priority
        );
    }

    // ==================== Getter方法 ====================

    public String getEquationId() { return equationId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<ChemicalComponent> getReactants() { return reactants; }
    public List<ChemicalComponent> getProducts() { return products; }
    public EquationConditions getConditions() { return conditions; }
    public List<ChemicalComponent> getCatalysts() { return catalysts; }
    public Set<String> getTags() { return tags; }
    public int getMinReactionCount() { return minReactionCount; }

    // ==================== 格式化方法 ====================

    /**
     * 获取化学方程式的字符串表示
     */
    public String getEquationString() {
        StringBuilder sb = new StringBuilder();

        // 反应物
        for (int i = 0; i < reactants.size(); i++) {
            if (i > 0) sb.append(" + ");
            ChemicalComponent reactant = reactants.get(i);
            sb.append(reactant.coefficient).append(" ")
                    .append(reactant.item.getDescriptionId());
        }

        sb.append(conditions.isTimed ? " ==[条件]=> " : " ==> ");

        // 生成物
        for (int i = 0; i < products.size(); i++) {
            if (i > 0) sb.append(" + ");
            ChemicalComponent product = products.get(i);
            sb.append(product.coefficient).append(" ")
                    .append(product.item.getDescriptionId());
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return name + ": " + getEquationString();
    }
}