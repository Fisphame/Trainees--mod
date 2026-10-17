package com.pha.trainees.util.game.chemistry;

import com.pha.trainees.Main;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ReactionSystem {

    // ==================== 核心接口 ====================

    /**
     * 通用条件接口
     */
    @FunctionalInterface
    public interface RCondition {
        boolean test(ItemStack stack, ItemEntity entity);
    }

    /**
     * 反应结果接口
     */
    @FunctionalInterface
    public interface RResult {
        void execute(Level level, ItemEntity entity, ItemStack stack);
    }

    /**
     * 时间提供器接口 - 动态计算反应所需时间
     */
    @FunctionalInterface
    public interface IDurationProvider {
        /**
         * 计算反应所需时间（秒）
         * @param stack 物品堆栈
         * @param entity 物品实体
         * @return 所需时间（秒）
         */
        double getRequiredTime(ItemStack stack, ItemEntity entity);
    }

    // ==================== 条件组合器 ====================

    /**
     * 与条件（AND）
     */
    public static class AndCondition implements RCondition {
        private final RCondition[] conditions;

        public AndCondition(RCondition... conditions) {
            this.conditions = conditions;
        }

        @Override
        public boolean test(ItemStack stack, ItemEntity entity) {
            for (RCondition condition : conditions) {
                if (!condition.test(stack, entity)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * 或条件（OR）
     */
    public static class OrCondition implements RCondition {
        private final RCondition[] conditions;

        public OrCondition(RCondition... conditions) {
            this.conditions = conditions;
        }

        @Override
        public boolean test(ItemStack stack, ItemEntity entity) {
            for (RCondition condition : conditions) {
                if (condition.test(stack, entity)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * 非条件（NOT）
     */
    public static class NotCondition implements RCondition {
        private final RCondition condition;

        public NotCondition(RCondition condition) {
            this.condition = condition;
        }

        @Override
        public boolean test(ItemStack stack, ItemEntity entity) {
            return !condition.test(stack, entity);
        }
    }

    /**
     * 时间条件 - 封装计时逻辑
     */
    public static class TimedCondition implements RCondition {
        private final RCondition baseCondition;
        private final IDurationProvider durationProvider; // 使用时间提供器接口
        private final String timerId;
        private final boolean resetOnConditionFail;

        // 构造函数：使用时间提供器
        public TimedCondition(RCondition baseCondition, IDurationProvider durationProvider,
                              String timerId, boolean resetOnConditionFail) {
            this.baseCondition = baseCondition;
            this.durationProvider = durationProvider;
            this.timerId = timerId;
            this.resetOnConditionFail = resetOnConditionFail;
        }

        // 构造函数：使用时间提供器，默认reset为true
        public TimedCondition(RCondition baseCondition, IDurationProvider durationProvider, String timerId) {
            this(baseCondition, durationProvider, timerId, true);
        }

        // 构造函数：使用固定时间（向后兼容）
        public TimedCondition(RCondition baseCondition, double fixedTime, String timerId) {
            this(baseCondition, (stack, entity) -> fixedTime, timerId, true);
        }

        @Override
        public boolean test(ItemStack stack, ItemEntity entity) {
            // 检查基础条件
            if (!baseCondition.test(stack, entity)) {
                if (resetOnConditionFail && TimeManager.isTimerActive(entity, timerId)) {
                    TimeManager.resetTimer(entity, timerId);
                }
                return false;
            }

            // 如果计时器未激活，开始计时
            if (!TimeManager.isTimerActive(entity, timerId)) {
                double requiredTime = durationProvider.getRequiredTime(stack, entity);
                if (requiredTime > 0) {
                    TimeManager.startTimer(entity, timerId, requiredTime);
                }
                return false;
            }

            // 检查是否应该触发反应
            return TimeManager.shouldReact(entity, timerId, durationProvider.getRequiredTime(stack, entity));
        }
    }

    // ==================== 反应定义 ====================

    /**
     * 反应定义类
     */
    public static class ChemicalReaction {
        public final String id;
        public final RCondition condition;
        public final RResult result;
        public final int priority;

        public ChemicalReaction(String id, RCondition condition,
                                RResult result, int priority) {
            this.id = id;
            this.condition = condition;
            this.result = result;
            this.priority = priority;
        }
    }

    // ==================== 反应注册表 ====================

    /**
     * 反应注册表
     */
    public static class ReactionRegistry {
        private static final Map<String, ChemicalReaction> REACTIONS = new ConcurrentHashMap<>();
        private static final List<ChemicalReaction> SORTED_REACTIONS = new ArrayList<>();
        private static boolean needsSorting = false;

        /**
         * 注册反应
         */
        public static void registerReaction(String id, RCondition condition,
                                            RResult result) {
            registerReaction(id, condition, result, 0);
        }

        public static void registerReaction(String id, RCondition condition,
                                            RResult result, int priority) {
            try {
                Main.LOGGER.info("ReactionRegistry: Registering reaction {}", id);
                Main.LOGGER.debug("ReactionRegistry: Condition = {}, Result = {}, Priority = {}",
                        condition.getClass().getSimpleName(),
                        result.getClass().getSimpleName(),
                        priority);

                REACTIONS.put(id, new ChemicalReaction(id, condition, result, priority));
                Main.LOGGER.info("ReactionRegistry: Reaction {} added to REACTIONS map", id);
                needsSorting = true;

                Main.LOGGER.info("ReactionRegistry: Current REACTIONS size = {}", REACTIONS.size());

            } catch (Exception e) {
                Main.LOGGER.error("ReactionRegistry: Error registering reaction {}", id, e);
                throw e; // Don't swallow exceptions
            }
        }

        /**
         * 注册时间反应（使用时间提供器）
         */
        public static void registerTimedReaction(String id, RCondition baseCondition,
                                                 RResult result,
                                                 IDurationProvider durationProvider) {
            registerTimedReaction(id, baseCondition, result, durationProvider, 0);
        }

        public static void registerTimedReaction(String id, RCondition baseCondition,
                                                 RResult result,
                                                 IDurationProvider durationProvider,
                                                 int priority) {
            String timerId = id + "_timer";
            RCondition timedCondition = new TimedCondition(baseCondition, durationProvider, timerId);
            registerReaction(id, timedCondition, result, priority);
        }

        /**
         * 注册时间反应（使用固定时间 - 向后兼容）
         */
        public static void registerTimedReaction(String id, RCondition baseCondition,
                                                 RResult result, double fixedTime) {
            registerTimedReaction(id, baseCondition, result, fixedTime, 0);
        }

        public static void registerTimedReaction(String id, RCondition baseCondition,
                                                 RResult result, double fixedTime,
                                                 int priority) {
            IDurationProvider fixedDuration = (stack, entity) -> fixedTime;
            registerTimedReaction(id, baseCondition, result, fixedDuration, priority);
        }

        /**
         * 触发反应
         */
        public static boolean triggerReactions(ItemStack stack, ItemEntity entity) {
            if (needsSorting) {
                sortReactions();
            }

            if (REACTIONS.isEmpty()) {
                return false;
            }

            for (ChemicalReaction reaction : SORTED_REACTIONS) {
                if (reaction.condition.test(stack, entity)) {
                    reaction.result.execute(entity.level(), entity, stack);
                    return true; // 一次只触发一个反应
                }
            }
            return false;
        }

        private static void sortReactions() {
            SORTED_REACTIONS.clear();
            SORTED_REACTIONS.addAll(REACTIONS.values());
            SORTED_REACTIONS.sort((a, b) -> Integer.compare(b.priority, a.priority));
            needsSorting = false;
        }

        public static Collection<ChemicalReaction> getAllReactions() {
            return REACTIONS.values();
        }

        public static void clearAllReactions() {
            REACTIONS.clear();
            SORTED_REACTIONS.clear();
            needsSorting = false;
        }
    }

    // ==================== 时间管理器 ====================

    /**
     * 时间管理器
     */
    public static class TimeManager {
        private static final String START_TIME = "reaction_start_time";
        private static final String REQUIRED_TIME = "reaction_required_time";

        public static boolean shouldReact(ItemEntity entity, String timerId, double requiredSeconds) {
            var tag = entity.getPersistentData();
            String startKey = START_TIME + "_" + timerId;
            String requiredKey = REQUIRED_TIME + "_" + timerId;

            if (!tag.contains(startKey) || !tag.contains(requiredKey)) {
                return false;
            }

            long start = tag.getLong(startKey);
            double requiredTicks = requiredSeconds * 20;
            long current = entity.level().getGameTime();

            if (current - start >= requiredTicks) {
                // 触发后清除计时器
                tag.remove(startKey);
                tag.remove(requiredKey);
                return true;
            }
            return false;
        }

        public static void startTimer(ItemEntity entity, String timerId, double requiredSeconds) {
            var tag = entity.getPersistentData();
            String startKey = START_TIME + "_" + timerId;
            String requiredKey = REQUIRED_TIME + "_" + timerId;

            tag.putLong(startKey, entity.level().getGameTime());
            tag.putDouble(requiredKey, requiredSeconds * 20);
        }

        public static void resetTimer(ItemEntity entity, String timerId) {
            var tag = entity.getPersistentData();
            String startKey = START_TIME + "_" + timerId;
            String requiredKey = REQUIRED_TIME + "_" + timerId;
            tag.remove(startKey);
            tag.remove(requiredKey);
        }

        public static boolean isTimerActive(ItemEntity entity, String timerId) {
            var tag = entity.getPersistentData();
            String startKey = START_TIME + "_" + timerId;
            return tag.contains(startKey);
        }
    }
}