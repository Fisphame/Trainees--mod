package com.pha.trainees.util.game;

import com.pha.trainees.util.math.MathT;
import net.minecraft.world.entity.item.ItemEntity;

public class Burning {

    /**
     * 检测实体是否在燃烧（有火焰动画）
     */
    public static boolean isBurning(ItemEntity entity) {
        return entity.isOnFire();
        // && entity.displayFireAnimation()
    }
    /**
     * 检测实体是否即将因燃烧（剩余燃烧时间小于tick）而消失
     * @param tick 刻
     * <p>
     *  掉落物燃烧消失的逻辑：
     *  1. 必须正在燃烧
     *  2. 剩余时间很少（通常<10 tick）
     *  3. 不是刚被点燃
     */
    public static boolean isAboutToVanishFromFire(ItemEntity entity, int tick) {
        return MathT.isInInterval(getAboutToVanishFromFire(entity), 0, tick, false, true);
    }
    public static boolean isAboutToVanishFromFire(ItemEntity entity) {
        return isAboutToVanishFromFire(entity, 10);
    }
    /**
     * 获取实体剩余燃烧tick
     */
    public static int getAboutToVanishFromFire(ItemEntity entity) {
        if (!isBurning(entity)) return -114;
        // 获取剩余燃烧时间
        return entity.getRemainingFireTicks();
    }


//        /**
//         * 检测实体是否在燃烧中（忽略刚开始燃烧的阶段）
//         * 可以设置一个最小燃烧时间阈值
//         */
//        public static boolean isBurningForAWhile(ItemEntity entity, int minTicks) {
//            if (!entity.isOnFire()) return false;
//
//            // 获取实体的最大燃烧时间
//            int maxFireTicks = entity.getEntityData().get(Entity.DATA_MAX_FIRE_TICKS);
//            int currentTicks = maxFireTicks - entity.getRemainingFireTicks();
//
//            return currentTicks >= minTicks;
//        }
//
//        /**
//         * 获取燃烧进度（0.0-1.0）
//         * 0 = 刚开始燃烧，1 = 即将消失
//         */
//        public static float getBurnProgress(ItemEntity entity) {
//            if (!entity.isOnFire()) return 0.0f;
//
//            // 获取实体的最大燃烧时间
//            Integer maxFireTicks = entity.getEntityData().get(Entity);
//            if (maxFireTicks == null || maxFireTicks <= 0) return 0.0f;
//
//            int remainingTicks = entity.getRemainingFireTicks();
//            return 1.0f - ((float) remainingTicks / maxFireTicks);
//        }
}
