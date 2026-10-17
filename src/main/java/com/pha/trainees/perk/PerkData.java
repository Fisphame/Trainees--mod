package com.pha.trainees.perk;

import com.pha.trainees.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 玩家的加点数据（§17.1 P3，作者定：存玩家 persistent data）。
 *
 * <p><b>货币在背包里</b>（作者原话"消耗背包内的精炼点来点亮"）：可用点数 = 背包里精炼点的**总数**，
 * 点亮就从背包扣、回退就往背包塞——不做"余额"字段，于是也没有"余额与物品对不上"的可能。</p>
 *
 * <p><b>服务端权威</b>：所有校验（点数够不够、前置点没点、是否满级）都在这里，客户端只发请求。</p>
 */
public final class PerkData {

    private static final String KEY = "trainees_perks";

    private PerkData() {
    }

    /** 该玩家所有已点等级（perkId → rank）。 */
    public static Map<String, Integer> ranks(Player player) {
        CompoundTag tag = data(player);
        Map<String, Integer> result = new LinkedHashMap<>();
        for (String key : tag.getAllKeys()) {
            int rank = tag.getInt(key);
            if (rank > 0 && PerkTable.byId(key) != null) {
                result.put(key, rank);
            }
        }
        return result;
    }

    public static int rank(Player player, String perkId) {
        return data(player).getInt(perkId);
    }

    /** 背包里可用的精炼点总数。 */
    public static int availablePoints(Player player) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.REFINED_POINT.get())) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /** 从背包扣掉 amount 个精炼点；不够返回 false（什么都不扣）。 */
    public static boolean consumePoints(Player player, int amount) {
        if (amount <= 0) return true;
        if (availablePoints(player) < amount) return false;
        int left = amount;
        for (int i = 0; i < player.getInventory().getContainerSize() && left > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.is(ModItems.REFINED_POINT.get())) continue;
            int take = Math.min(left, stack.getCount());
            stack.shrink(take);
            left -= take;
        }
        player.getInventory().setChanged();
        return true;
    }

    /** 往背包塞精炼点（放不下就掉在脚下）。 */
    public static void givePoints(Player player, int amount) {
        if (amount <= 0) return;
        ItemStack stack = new ItemStack(ModItems.REFINED_POINT.get(), amount);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        player.getInventory().setChanged();
    }

    // ==================== 校验与操作（服务端权威） ====================

    /** 前置是否满足。 */
    public static boolean prereqMet(Player player, PerkTable perk) {
        if (perk.prereq == null) return true;
        return rank(player, perk.prereq.perkId()) >= perk.prereq.rank();
    }

    /** 能否点亮下一级；不能则返回原因（语言键）。 */
    @Nullable
    public static String whyCannotUnlock(Player player, PerkTable perk) {
        int current = rank(player, perk.id);
        if (current >= perk.maxRank) return "perk.trainees.reason.maxed";
        if (!prereqMet(player, perk)) return "perk.trainees.reason.prereq";
        if (availablePoints(player) < perk.costFor(current)) return "perk.trainees.reason.points";
        return null;
    }

    /**
     * 点亮一级：扣点 → 加等级 → 立即重算效果。
     *
     * @return true = 成功
     */
    public static boolean unlock(Player player, PerkTable perk) {
        if (whyCannotUnlock(player, perk) != null) return false;
        int current = rank(player, perk.id);
        if (!consumePoints(player, perk.costFor(current))) return false;
        data(player).putInt(perk.id, current + 1);
        PerkEffects.refresh(player);
        return true;
    }

    /**
     * 回退一级：**原价返还**（作者定：可自由回退）→ 降等级 → 退还该级花掉的精炼点。
     *
     * @return true = 成功
     */
    public static boolean refund(Player player, PerkTable perk) {
        int current = rank(player, perk.id);
        if (current <= 0) return false;
        // 被别的属性当前置依赖时不许回退，否则会出现"前置没了但后继还在"的坏状态
        for (PerkTable other : PerkTable.all()) {
            if (other.prereq != null && other.prereq.perkId().equals(perk.id)
                    && rank(player, other.id) > 0 && current - 1 < other.prereq.rank()) {
                return false;
            }
        }
        data(player).putInt(perk.id, current - 1);
        givePoints(player, perk.costFor(current - 1));
        PerkEffects.refresh(player);
        return true;
    }

    /** 清空所有加点并全额返还（调试/洗点用；作者要"自由回退"，这里给个一键版）。 */
    public static int refundAll(Player player) {
        CompoundTag tag = data(player);
        int returned = 0;
        for (Map.Entry<String, Integer> entry : ranks(player).entrySet()) {
            PerkTable perk = PerkTable.byId(entry.getKey());
            if (perk == null) continue;
            for (int r = 0; r < entry.getValue(); r++) {
                returned += perk.costFor(r);
            }
            tag.remove(entry.getKey());
        }
        if (returned > 0) {
            givePoints(player, returned);
        }
        PerkEffects.refresh(player);
        return returned;
    }

    /** 存档搬运：死亡重生 / 换维度时把加点数据带过去（persistent data 不会自动跟着走）。 */
    public static void copy(Player from, Player to) {
        to.getPersistentData().put(KEY, data(from).copy());
    }

    private static CompoundTag data(Player player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(KEY)) {
            root.put(KEY, new CompoundTag());
        }
        return root.getCompound(KEY);
    }
}
