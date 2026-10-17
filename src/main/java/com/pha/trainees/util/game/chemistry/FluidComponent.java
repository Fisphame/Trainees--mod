package com.pha.trainees.util.game.chemistry;

import com.pha.trainees.fluid.BaseChemicalFluid;

public class FluidComponent {
    private final BaseChemicalFluid fluid;
    private final double moles;   // 物质的量（mol）

    public FluidComponent(double moles, BaseChemicalFluid fluid) {
        this.moles = moles;
        this.fluid = fluid;
    }

    public BaseChemicalFluid getFluid() {
        return fluid;
    }
    public double getMoles() {
        return moles;
    }

    // 根据反应次数计算所需/产生的体积（mB）
    public int getVolumeForReactionCount(int reactionCount) {
        return fluid.calculateVolumeFromMoles(moles * reactionCount);
    }
}
