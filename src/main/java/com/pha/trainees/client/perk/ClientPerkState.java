package com.pha.trainees.client.perk;

import net.minecraft.nbt.CompoundTag;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 客户端侧的加点状态缓存（§17.1 P3）：由 {@code PerkPackets.State} 更新，给属性点屏幕读。
 *
 * <p>服务端权威：这里只是"最近一次服务端告诉我的值"，任何判断都以服务端为准。</p>
 */
public final class ClientPerkState {

    private static Map<String, Integer> ranks = new LinkedHashMap<>();
    private static int available;

    private ClientPerkState() {
    }

    public static void set(CompoundTag tag, int availablePoints) {
        Map<String, Integer> copy = new LinkedHashMap<>();
        for (String key : tag.getAllKeys()) {
            int value = tag.getInt(key);
            if (value > 0) {
                copy.put(key, value);
            }
        }
        ranks = copy;
        available = availablePoints;
    }

    public static int rank(String perkId) {
        return ranks.getOrDefault(perkId, 0);
    }

    public static int available() {
        return available;
    }

    public static void reset() {
        ranks = new LinkedHashMap<>();
        available = 0;
    }
}
