package com.pha.trainees.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SandBlock;
import net.minecraft.world.level.block.state.BlockState;

public class PowderAnti99Block extends SandBlock {
    private final int dustColor;

    public PowderAnti99Block(int p_55967_, Properties p_55968_) {
        super(p_55967_, p_55968_);
        this.dustColor = p_55967_;
    }

    @Override
    public int getDustColor(BlockState p_55970_, BlockGetter p_55971_, BlockPos p_55972_) {
        return this.dustColor;
    }
}
