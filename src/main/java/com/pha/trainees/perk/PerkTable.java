package com.pha.trainees.perk;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.ForgeMod;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 属性点定义表（§17.1 P3）。
 *
 * <p><b>每条都必须落到一个真实机制</b>，只有三类：</p>
 * <ol>
 *   <li>{@link Kind#ATTRIBUTE}：原版/Forge 属性（用 {@link AttributeModifier}，回退即按名字移除）</li>
 *   <li>{@link Kind#EFFECT}：常驻药水效果（每 tick 刷新、隐藏图标）</li>
 *   <li>{@link Kind#SPECIAL}：需要一点专门逻辑的，枚举在 {@link Special}，实现统一收敛在 {@code PerkEffects}</li>
 * </ol>
 *
 * <p>价格/前置按草案 v1（逐级递增、部分需前置）；全部**可自由回退**（作者定）。
 * 上一版这张表我拿占位属性糊过（再生塞成免疫摔伤之类），已被我自己撤掉重写——**这张不许再出现"没有实现"的条目**。</p>
 */
public final class PerkTable {

    public enum Category {
        VITALITY("perk.trainees.category.vitality"),
        MIGHT("perk.trainees.category.might"),
        MOBILITY("perk.trainees.category.mobility"),
        ESSENCE("perk.trainees.category.essence"),
        UTILITY("perk.trainees.category.utility");

        public final String nameKey;

        Category(String nameKey) {
            this.nameKey = nameKey;
        }
    }

    public enum Kind {
        ATTRIBUTE,
        EFFECT,
        SPECIAL
    }

    /** 需要专门逻辑的条目（实现在 PerkEffects，一处收敛）。 */
    public enum Special {
        /** 生命再生：每 N tick 回 1 点（N = 20/10/5 秒 × 等级） */
        REGENERATION,
        /** 减伤：按比例削弱受到的伤害 */
        DAMAGE_REDUCTION,
        /** 免疫坠落伤害 */
        NO_FALL_DAMAGE,
        /** 节食：降低饥饿消耗速率 */
        LESS_HUNGER,
        /** 暴击：按概率放大一次伤害 */
        CRIT,
        /** 吸血：击杀回血 */
        LIFESTEAL,
        /** 轻羽：按比例削减坠落伤害 */
        FALL_REDUCTION,
        /** 精华容量：玩家 RE 上限加成 */
        ESSENCE_CAPACITY,
        /** 注能效率：R 单次注入量倍率 */
        CHANNEL_POWER,
        /** 抽取效率：潜行+R 单次抽取量倍率 */
        CHANNEL_DRAIN,
        /** 注能距离：R 作用距离加成（格） */
        CHANNEL_RANGE,
        /** 经验强化：经验获取倍率 */
        XP_BONUS
    }

    /** 前置：某条属性至少到 rank 级。 */
    public record Prereq(String perkId, int rank) {
    }

    public final String id;
    public final Category category;
    public final String nameKey;
    public final int maxRank;
    /** 每级价格（精炼点），长度 = maxRank。 */
    public final int[] costs;
    @Nullable
    public final Prereq prereq;
    public final Kind kind;
    @Nullable
    public final Attribute attribute;
    public final double amountPerRank;
    public final AttributeModifier.Operation operation;
    @Nullable
    public final MobEffect effect;
    @Nullable
    public final Special special;

    private PerkTable(String id, Category category, int maxRank, int[] costs, @Nullable Prereq prereq, Kind kind,
                      @Nullable Attribute attribute, double amountPerRank, AttributeModifier.Operation operation,
                      @Nullable MobEffect effect, @Nullable Special special) {
        this.id = id;
        this.category = category;
        this.nameKey = "perk.trainees." + id;
        this.maxRank = maxRank;
        this.costs = costs;
        this.prereq = prereq;
        this.kind = kind;
        this.attribute = attribute;
        this.amountPerRank = amountPerRank;
        this.operation = operation;
        this.effect = effect;
        this.special = special;
    }

    // ==================== 工厂 ====================

    private static final int[] ONE = {1};

    private static PerkTable attribute(String id, Category category, int maxRank, int[] costs,
                                       @Nullable Prereq prereq, Attribute attribute, double perRank) {
        return new PerkTable(id, category, maxRank, costs, prereq, Kind.ATTRIBUTE, attribute, perRank,
                AttributeModifier.Operation.ADDITION, null, null);
    }

    private static PerkTable attributeMultiplied(String id, Category category, int maxRank, int[] costs,
                                                 @Nullable Prereq prereq, Attribute attribute, double perRank) {
        return new PerkTable(id, category, maxRank, costs, prereq, Kind.ATTRIBUTE, attribute, perRank,
                AttributeModifier.Operation.MULTIPLY_TOTAL, null, null);
    }

    private static PerkTable effect(String id, Category category, int cost, @Nullable Prereq prereq, MobEffect effect) {
        return new PerkTable(id, category, 1, new int[]{cost}, prereq, Kind.EFFECT, null, 0,
                AttributeModifier.Operation.ADDITION, effect, null);
    }

    private static PerkTable special(String id, Category category, int[] costs, @Nullable Prereq prereq,
                                     Special special) {
        return new PerkTable(id, category, costs.length, costs, prereq, Kind.SPECIAL, null, 0,
                AttributeModifier.Operation.ADDITION, null, special);
    }

    // ==================== 全表（25 条，逐条落到真实机制） ====================

    private static final List<PerkTable> ALL = new ArrayList<>();
    private static final Map<String, PerkTable> BY_ID = new LinkedHashMap<>();

    private static void add(PerkTable perk) {
        ALL.add(perk);
        BY_ID.put(perk.id, perk);
    }

    static {
        // ---------- 生存 ----------
        add(attribute("vitality", Category.VITALITY, 5, new int[]{1, 2, 3, 5, 8}, null,
                Attributes.MAX_HEALTH, 4.0));
        add(special("regeneration", Category.VITALITY, new int[]{3, 5, 8},
                new Prereq("vitality", 3), Special.REGENERATION));
        add(special("resistance", Category.VITALITY, new int[]{4, 8, 12},
                new Prereq("vitality", 2), Special.DAMAGE_REDUCTION));
        add(special("fall_immunity", Category.VITALITY, ONE, new Prereq("lightness", 1),
                Special.NO_FALL_DAMAGE));
        add(special("diet", Category.VITALITY, new int[]{3, 6}, null, Special.LESS_HUNGER));

        // ---------- 战斗 ----------
        add(attribute("might", Category.MIGHT, 5, new int[]{1, 2, 3, 5, 8}, null,
                Attributes.ATTACK_DAMAGE, 1.0));
        add(attributeMultiplied("attack_speed", Category.MIGHT, 3, new int[]{3, 6, 10},
                new Prereq("might", 2), Attributes.ATTACK_SPEED, 0.1));
        add(special("crit", Category.MIGHT, new int[]{4, 8, 12},
                new Prereq("might", 3), Special.CRIT));
        add(attribute("knockback", Category.MIGHT, 2, new int[]{3, 6}, null,
                Attributes.ATTACK_KNOCKBACK, 0.5));
        add(special("lifesteal", Category.MIGHT, new int[]{6, 12},
                new Prereq("might", 4), Special.LIFESTEAL));

        // ---------- 机动 ----------
        add(attributeMultiplied("swiftness", Category.MOBILITY, 5, new int[]{1, 2, 3, 5, 8}, null,
                Attributes.MOVEMENT_SPEED, 0.05));
        add(attribute("feather", Category.MOBILITY, 3, new int[]{3, 6, 9},
                new Prereq("swiftness", 2), Attributes.KNOCKBACK_RESISTANCE, 0.05));
        add(special("lightness", Category.MOBILITY, new int[]{3, 6, 9},
                new Prereq("swiftness", 2), Special.FALL_REDUCTION));
        add(attribute("leap", Category.MOBILITY, 2, new int[]{4, 8},
                new Prereq("lightness", 2), Attributes.JUMP_STRENGTH, 0.1));
        add(attributeMultiplied("swim", Category.MOBILITY, 2, new int[]{3, 6}, null,
                ForgeMod.SWIM_SPEED.get(), 0.25));
        add(attribute("step_up", Category.MOBILITY, 1, ONE, new Prereq("swiftness", 3),
                ForgeMod.STEP_HEIGHT_ADDITION.get(), 0.6));

        // ---------- 魔感 ----------
        add(special("essence_capacity", Category.ESSENCE, new int[]{2, 4, 6, 10, 14}, null,
                Special.ESSENCE_CAPACITY));
        add(special("channel_power", Category.ESSENCE, new int[]{4, 8, 12},
                new Prereq("essence_capacity", 2), Special.CHANNEL_POWER));
        add(special("channel_drain", Category.ESSENCE, new int[]{4, 8, 12},
                new Prereq("essence_capacity", 2), Special.CHANNEL_DRAIN));
        add(special("channel_range", Category.ESSENCE, new int[]{3, 6, 9}, null,
                Special.CHANNEL_RANGE));

        // ---------- 功能 ----------
        add(effect("night_vision", Category.UTILITY, 8, null, MobEffects.NIGHT_VISION));
        add(effect("water_breathing", Category.UTILITY, 8, null, MobEffects.WATER_BREATHING));
        add(effect("fire_resistance", Category.UTILITY, 12,
                new Prereq("resistance", 2), MobEffects.FIRE_RESISTANCE));
        add(attribute("luck", Category.UTILITY, 3, new int[]{6, 12, 18}, null, Attributes.LUCK, 1.0));
        add(special("experience", Category.UTILITY, new int[]{4, 8, 12}, null, Special.XP_BONUS));
        add(attribute("reach", Category.UTILITY, 2, new int[]{3, 6}, null,
                ForgeMod.BLOCK_REACH.get(), 1.0));
    }

    public static List<PerkTable> all() {
        return List.copyOf(ALL);
    }

    @Nullable
    public static PerkTable byId(String id) {
        return BY_ID.get(id);
    }

    /** 点亮下一级要花多少精炼点；已满级返回 -1。 */
    public int costFor(int currentRank) {
        if (currentRank < 0 || currentRank >= maxRank) return -1;
        return costs[Math.min(currentRank, costs.length - 1)];
    }

    /** 属性修饰符的名字（回退时按名字移除，所以必须唯一且稳定）。 */
    public String modifierName() {
        return "trainees.perk." + id;
    }
}
