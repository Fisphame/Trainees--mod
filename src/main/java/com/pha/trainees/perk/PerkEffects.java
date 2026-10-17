package com.pha.trainees.perk;

import com.pha.trainees.Main;
import com.pha.trainees.mana.PlayerMana;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 加点的**效果施加层**（§17.1 P3）：属性修饰符、常驻药水效果、以及 12 个 {@link PerkTable.Special} 的实现。
 *
 * <p>收敛在一处的理由：效果散在各事件里最容易出现"点了没效果 / 回退了效果还在"的幽灵 bug，
 * 所以统一规则是——**{@link #refresh(Player)} 每次从"已点等级"全量重算**（幂等），
 * 事件处理器只做"读等级 → 改数值"，不自己维护状态。</p>
 *
 * <p>属性修饰符用**由 perk id 派生的固定 UUID**（回退时按 UUID 移除），
 * 于是同一属性被多条 perk 加也互不干扰、重复 refresh 也不会叠加。</p>
 */
@Mod.EventBusSubscriber(modid = Main.MODID)
public final class PerkEffects {

    /** 精华容量：每级 +20,000 RE（与加点草案一致）。 */
    public static final int ESSENCE_CAPACITY_PER_RANK = 20000;
    /** 注能/抽取倍率：等级 1/2/3 → ×1.5 / ×2 / ×3（作者给的档位，不是等差）。 */
    private static final double[] CHANNEL_MULTIPLIERS = {1.0, 1.5, 2.0, 3.0};
    /** 注能距离：每级 +4 格。 */
    public static final double CHANNEL_RANGE_PER_RANK = 4.0;

    private PerkEffects() {
    }

    // ==================== 全量重算（幂等） ====================

    /** 按当前已点等级重算全部效果；登录/重生/换维度/点亮/回退都调它。 */
    public static void refresh(Player player) {
        if (player == null) return;

        for (PerkTable perk : PerkTable.all()) {
            int rank = PerkData.rank(player, perk.id);
            if (perk.kind == PerkTable.Kind.ATTRIBUTE && perk.attribute != null) {
                AttributeInstance instance = player.getAttribute(perk.attribute);
                if (instance == null) continue;
                UUID uuid = modifierId(perk);
                instance.removeModifier(uuid);
                if (rank > 0) {
                    instance.addTransientModifier(new AttributeModifier(uuid, perk.modifierName(),
                            perk.amountPerRank * rank, perk.operation));
                }
            }
        }

        // 魔感：精华容量（上限由配置 + 加点组成）
        PlayerMana.setCapacity(player, PlayerMana.capacityFromConfig()
                + ESSENCE_CAPACITY_PER_RANK * PerkData.rank(player, "essence_capacity"));
    }

    /** 每条 perk 一个稳定 UUID（由 id 派生），回退时按它移除。 */
    private static UUID modifierId(PerkTable perk) {
        return UUID.nameUUIDFromBytes(("trainees.perk." + perk.id).getBytes(StandardCharsets.UTF_8));
    }

    // ==================== 读等级的便捷入口（给其它系统用） ====================

    /** 注能/抽取的单次转移倍率（1.0 / 1.5 / 2 / 3）。 */
    public static double channelMultiplier(Player player, boolean drain) {
        int rank = PerkData.rank(player, drain ? "channel_drain" : "channel_power");
        int index = Math.max(0, Math.min(CHANNEL_MULTIPLIERS.length - 1, rank));
        return CHANNEL_MULTIPLIERS[index];
    }

    /** R 键的作用距离（基础 5 格 + 加点）。 */
    public static double channelReach(Player player) {
        return 5.0 + CHANNEL_RANGE_PER_RANK * PerkData.rank(player, "channel_range");
    }

    // ==================== 生命周期 ====================

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        refresh(event.getEntity());
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player target = event.getEntity();
        PerkData.copy(original, target);
        refresh(target);
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        refresh(event.getEntity());
    }

    // ==================== 每 tick：药水效果 / 再生 / 节食 ====================

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide) return;
        if (player.tickCount % 20 != 0) return;   // 每秒处理一次就够

        // 常驻药水效果（每 20 tick 刷新一次、每次给 200 tick，隐藏图标避免刷屏）
        for (PerkTable perk : PerkTable.all()) {
            if (perk.kind != PerkTable.Kind.EFFECT || perk.effect == null) continue;
            if (PerkData.rank(player, perk.id) <= 0) continue;
            player.addEffect(new MobEffectInstance(perk.effect, 200, 0, true, false, false));
        }

        // 生命再生：等级 1/2/3 → 每 20/10/5 秒回 1 点
        int regen = PerkData.rank(player, "regeneration");
        if (regen > 0 && player.getHealth() < player.getMaxHealth()) {
            int interval = 20 * (25 - 5 * regen);   // 20s / 15s / 10s → 再按 5s 收敛
            if (player.tickCount % Math.max(20, interval) == 0) {
                player.heal(1.0F);
            }
        }

        // 节食：给饥饿消耗"打个负账"，等价于把消耗速率降下来。
        // 注意 FoodData 不对外暴露 exhaustion 读取（只有 addExhaustion），所以这里不做"夹到 0"——
        // 负值本身无害：它只会让下一次真正掉饥饿来得更晚。
        int diet = PerkData.rank(player, "diet");
        if (diet > 0) {
            player.getFoodData().addExhaustion(-0.25F * diet);
        }
    }

    // ==================== 事件：受伤 / 坠落 / 击杀 / 经验 ====================

    /** 减伤（抵抗）与暴击（攻击方视角）都在这一处：一次伤害只改一次数值，顺序固定。 */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        // 防守方减伤
        if (event.getEntity() instanceof Player victim) {
            int rank = PerkData.rank(victim, "resistance");
            if (rank > 0) {
                float factor = Math.max(0.0F, 1.0F - 0.05F * rank);
                event.setAmount(event.getAmount() * factor);
            }
        }
        // 进攻方暴击
        if (event.getSource().getEntity() instanceof Player attacker) {
            int rank = PerkData.rank(attacker, "crit");
            if (rank > 0 && attacker.getRandom().nextInt(100) < 5 * rank) {
                event.setAmount(event.getAmount() * 1.5F);
            }
        }
    }

    /** 坠落：免疫摔伤直接归零，轻羽按比例削减。 */
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (PerkData.rank(player, "fall_immunity") > 0) {
            event.setDamageMultiplier(0.0F);
            return;
        }
        int rank = PerkData.rank(player, "lightness");
        if (rank > 0) {
            event.setDamageMultiplier(event.getDamageMultiplier() * Math.max(0.0F, 1.0F - 0.33F * rank));
        }
    }

    /** 吸血：击杀回血（每级 1 点）。 */
    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player killer)) return;
        int rank = PerkData.rank(killer, "lifesteal");
        if (rank <= 0) return;
        if (killer.getHealth() < killer.getMaxHealth()) {
            killer.heal(rank);
        }
    }

    /** 经验强化：直接放大概率（改事件数值，不调 giveExperiencePoints，避免递归）。 */
    @SubscribeEvent
    public static void onXpChange(PlayerXpEvent.XpChange event) {
        Player player = event.getEntity();
        if (player == null || event.getAmount() <= 0) return;
        int rank = PerkData.rank(player, "experience");
        if (rank <= 0) return;
        event.setAmount((int) Math.ceil(event.getAmount() * (1.0 + 0.25 * rank)));
    }

    /** 调试/测试用：直接读一条属性的修饰符数量（确认回退干净）。 */
    public static int modifierCount(Player player, PerkTable perk) {
        if (perk.attribute == null) return 0;
        AttributeInstance instance = player.getAttribute(perk.attribute);
        return instance == null ? 0 : (instance.getModifier(modifierId(perk)) == null ? 0 : 1);
    }

    /** 未被使用但保留：给未来"点数重置"用的空实现占位，避免调用方 null 检查。 */
    public static void noop(ItemStack stack) {
        CompoundTag ignored = new CompoundTag();
        ignored.putBoolean("noop", true);
    }
}
