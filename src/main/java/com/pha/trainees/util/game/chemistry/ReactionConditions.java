package com.pha.trainees.util.game.chemistry;

import com.pha.trainees.registry.ModChemistry;
import com.pha.trainees.registry.ModTags;
import com.pha.trainees.util.game.Burning;
import com.pha.trainees.util.game.ItemClassifier;
import com.pha.trainees.util.game.Tools;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import com.pha.trainees.util.game.chemistry.ReactionSystem.*;
import net.minecraftforge.common.util.Lazy;


public class ReactionConditions {

    // ==================== 物品条件 ====================

    /**
     * 检查物品是否为指定物品
     */
    public static RCondition isItem(Item item) {
        return (stack, entity) -> stack.getItem() == item;
    }

    /**
     * 检查物品是否有指定标签
     */
    public static RCondition hasTag(TagKey<Item> tag) {
        return (stack, entity) -> ItemClassifier.tag(stack.getItem(), tag);
    }

    // Ji相关条件（Ji有多种形态）
    public static final Lazy<RCondition> IS_JI = Lazy.of(
            () -> (stack, entity) -> Tools.isJi(stack)
    );


    // ==================== 环境条件 ====================

    /**
     * 在水中
     */
    public static final RCondition IN_WATER = (stack, entity) -> entity.isInWater();

    /**
     * 在阳光下
     */
    public static final RCondition IN_SUNLIGHT = (stack, entity) -> Tools.isInSunlight(entity);

    /**
     * 下雨/雷暴天气
     */
    public static final RCondition IN_RAIN = (stack, entity) -> entity.level().isRaining() || entity.level().isThundering();

    /**
     * 白天
     */
    public static final RCondition IS_DAYTIME = (stack, entity) -> entity.level().isDay();

    /**
     * 晚上
     */
    public static final RCondition IS_NIGHT = (stack, entity) -> entity.level().isNight();

    public static final RCondition IN_H2O = or(IN_WATER, IN_RAIN);


    // ==================== 物品属性条件 ====================

    /**
     * 物品数量大于等于指定值
     */
    public static RCondition hasMinCount(int minCount) {
        return (stack, entity) -> stack.getCount() >= minCount;
    }

    /**
     * 物品有NBT标签
     */
    public static RCondition hasNbtKey(String key) {
        return (stack, entity) -> {
            if (stack.getTag() != null) {
                return stack.hasTag() && stack.getTag().contains(key);
            }
            return false;
        };
    }

    // ==================== 组合条件构建器 ====================

    /**
     * 与条件（AND）
     */
    public static RCondition and(RCondition... conditions) {
        return new AndCondition(conditions);
    }

    /**
     * 或条件（OR）
     */
    public static RCondition or(RCondition... conditions) {
        return new OrCondition(conditions);
    }

    /**
     * 非条件（NOT）
     */
    public static RCondition not(RCondition condition) {
        return new NotCondition(condition);
    }
// ==================== 时间条件构建器 ====================

    /**
     * 创建时间条件（使用时间提供器）
     */
    public static RCondition timed(String timerId,
                                                  RCondition baseCondition,
                                                  IDurationProvider durationProvider) {
        return new TimedCondition(baseCondition, durationProvider, timerId);
    }

    /**
     * 创建时间条件（使用时间提供器，带重置选项）
     */
    public static RCondition timed(String timerId,
                                                  RCondition baseCondition,
                                                  IDurationProvider durationProvider,
                                                  boolean resetOnConditionFail) {
        return new TimedCondition(baseCondition, durationProvider, timerId, resetOnConditionFail);
    }

    /**
     * 创建时间条件（使用固定时间）
     */
    public static RCondition timed(String timerId,
                                                  RCondition baseCondition,
                                                  double fixedTime) {
        return new TimedCondition(baseCondition, fixedTime, timerId);
    }

    /**
     * 一些反应条件
     */
    public static final Lazy<RCondition> hbpoDecomposeCondition = Lazy.of(
            () -> and(
            isItem(ModChemistry.ModChemistryItems.CHE_HBPO_POWDER.get()),
            IN_SUNLIGHT,
            ReactionConditions.hasMinCount(2))
    );

    public static final Lazy<IDurationProvider> hbpoDurationProvider = Lazy.of(
            () -> (stack, entity) -> {
                double punishTime = Tools.punishmentTimeToSunlight(entity);
                if (punishTime >= 9999) return Double.MAX_VALUE;
                double randomTime = Tools.randomInRange(entity.level(), 0.0, 0.5);
                return randomTime + punishTime;
            }
    );

    public static final Lazy<RCondition> bp2AndWaterCondition = Lazy.of(
            () -> and(
                hasTag(ModTags.POWDER_ANTI_2),
                IN_H2O
            )
    );

    public static final Lazy<RCondition> jiAndWaterCondition = Lazy.of(
            () -> and(
                    IS_JI.get(),
                    IN_H2O
            )
    );

    public static IDurationProvider inverseCountBased(int baseTime) {
        return (stack, entity) -> (double) baseTime / Math.max(1, stack.getCount());
    }

    public static final IDurationProvider random5to10 =
            (stack, entity) -> 5 + entity.level().random.nextInt(6);

    public static final IDurationProvider dur0 =
            (stack, entity) -> 0;

    /**
     * 检测是否在燃烧中
     */
    public static final RCondition IS_BURNING = (stack, entity) ->
            entity.isOnFire() && entity.displayFireAnimation();

    /**
     * 检测是否即将因燃烧消失
     */
    public static final RCondition isAboutToVanishFromFire = (stack, entity) ->
            Burning.isAboutToVanishFromFire(entity, 5);

}