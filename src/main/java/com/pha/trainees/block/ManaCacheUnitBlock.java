package com.pha.trainees.block;

import com.pha.trainees.blockentity.AltarCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 精炼精华缓存单元（§17.1，作者 2026-10-02 定名）：给**相邻**的祭坛主方块扩容。
 *
 * <p>规则（作者定）：相邻贴着主方块、每个 +144,000 RE、可叠多个、**不做链式**。
 * 作者日后可能改成线缆或无线连接——所以本类**只负责"变了就通知邻居重算"**，
 * 真正的容量算法只在 {@link AltarCoreBlockEntity#getCapacity()} 一处，
 * 换连接方式时改那一段即可，别把"相邻扫描"散到别处。</p>
 *
 * <p>它自己不存魔力，因此没有方块实体、也不上 Jade 的魔力行（缓存量体现在主方块的总容量里）。</p>
 */
public class ManaCacheUnitBlock extends Block {

    public ManaCacheUnitBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) notifyAdjacentCores(level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        // 换成了同一个方块就不必重算（避免破坏/替换时的无意义刷新）
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            notifyAdjacentCores(level, pos);
        }
    }

    /** 让周围 6 格的祭坛主方块按新接线情况重算容量。 */
    private static void notifyAdjacentCores(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (level.getBlockEntity(pos.relative(dir)) instanceof AltarCoreBlockEntity core) {
                core.refreshCapacity();
            }
        }
    }
}
