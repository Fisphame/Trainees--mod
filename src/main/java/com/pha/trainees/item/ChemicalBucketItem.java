package com.pha.trainees.item;

import com.pha.trainees.fluid.BaseChemicalFluid;
import net.minecraft.world.item.BucketItem;

public class ChemicalBucketItem extends BucketItem {
   private final BaseChemicalFluid fluid;

    public ChemicalBucketItem(BaseChemicalFluid fluid, Properties properties) {
        super(fluid, properties);
        this.fluid = fluid;
    }

    public BaseChemicalFluid getChemicalFluid() {
        return fluid;
    }
}
