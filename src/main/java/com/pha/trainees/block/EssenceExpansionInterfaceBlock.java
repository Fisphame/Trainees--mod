package com.pha.trainees.block;

import com.pha.trainees.machine.essence.EssenceNetwork;
import net.minecraft.world.level.block.Block;

/**
 * 网络扩展接口（§17.1 ④）：**导线从这里接出去**，连到缓存单元。
 *
 * <p>它自己就是个**完整方块**（作者强调）+ 实现 {@link EssenceNetwork.Conductive}，
 * 于是端口的洪泛会从这里一路找下去。结构里**最多一个**（见 {@code EssencePortPattern}）。</p>
 */
public class EssenceExpansionInterfaceBlock extends Block implements EssenceNetwork.Conductive {

    public EssenceExpansionInterfaceBlock(Properties properties) {
        super(properties);
    }
}
