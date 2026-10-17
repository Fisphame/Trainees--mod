package com.pha.trainees.util.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

public interface ITraversal {
    // 六方向邻居偏移（Y上下 + X/Z 四向），供 AbsorbBlockEntity 等做邻居扫描
    int[] dx3 = {0, 0, 0, 0, -1, 1};  // X方向
    int[] dy3 = {1, -1, 0, 0, 0, 0};  // Y方向（上下）
    int[] dz3 = {0, 0, -1, 1, 0, 0};  // Z方向

    // ⚠ 以下为旧版祭坛设计（Y=-1）遗留，已被 TrainerAltarPattern 中的
    // getAltarNorth/South/West/East 覆写（核心上方 +2、十字四端 ±3）：
    // 仅作默认值保留，切勿依赖其坐标。
    int[] altarDx = {-3, 3, 0, 0};
    int[] altarDz = {0, 0, -3, 3};
    Vec3i[] altars = {
            new Vec3i(-3, -1, 0),
            new Vec3i(3, -1, 0),
            new Vec3i(0, -1, -3),
            new Vec3i(0, -1, 3)
    };

    default BlockPos getAltarNorth(BlockPos pos) {
        return pos.offset(altars[0]);
    }
    default BlockPos getAltarSouth(BlockPos pos) {
        return pos.offset(altars[1]);
    }
    default BlockPos getAltarWest(BlockPos pos) {
        return pos.offset(altars[2]);
    }
    default BlockPos getAltarEast(BlockPos pos) {
        return pos.offset(altars[3]);
    }
}
