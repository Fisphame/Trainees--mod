package com.pha.trainees.client;

import net.minecraft.Util;

/**
 * 客户端侧的玩家精华缓存（§17.1 P1.5）：由 {@code ManaStatePacket} 更新，给 HUD 读。
 *
 * <p>服务端**权威**：这里只是"最近一次服务端告诉我的值"，任何判断都不该以它为准。</p>
 */
public final class ClientManaState {

    private static int stored;
    private static int capacity;
    /** 最近一次变化的时间戳：让 HUD 在"刚抽空"之后还停留一会儿，而不是瞬间消失。 */
    private static long lastChangeMillis;

    private ClientManaState() {
    }

    public static void set(int newStored, int newCapacity) {
        if (newStored != stored || newCapacity != capacity) {
            lastChangeMillis = Util.getMillis();
        }
        stored = newStored;
        capacity = newCapacity;
    }

    public static int stored() {
        return stored;
    }

    public static int capacity() {
        return capacity;
    }

    /** 是否该显示 HUD：有精华、或者刚刚才变化过（抽空后不立刻消失）。 */
    public static boolean shouldShow() {
        if (capacity <= 0) return false;
        return stored > 0 || Util.getMillis() - lastChangeMillis < 3000L;
    }

    public static float fillRatio() {
        return capacity <= 0 ? 0f : (float) stored / capacity;
    }

    /** 退出世界时清空，免得下一个世界开局先闪一下上个世界的数值。 */
    public static void reset() {
        stored = 0;
        capacity = 0;
        lastChangeMillis = 0L;
    }
}
