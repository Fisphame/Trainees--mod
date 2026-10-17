package com.pha.trainees.chemistry.util;

import com.pha.trainees.chemistry.particle.IonType;
import com.pha.trainees.registry.ModChemistry.ModIons;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.HashMap;
import java.util.Map;

public class FluidIonMapper {

    private static final Map<Fluid, IonType> FLUID_TO_ION = new HashMap<>();
    private static final Map<IonType, Fluid> ION_TO_FLUID = new HashMap<>();

    static {
        // 水 -> H2O
        register(Fluids.WATER, ModIons.H2O);
        register(Fluids.FLOWING_WATER, ModIons.H2O);
        // 后续可添加更多流体
    }

    private static void register(Fluid fluid, IonType ion) {
        FLUID_TO_ION.put(fluid, ion);
        ION_TO_FLUID.put(ion, fluid);
    }

    public static IonType getIonForFluid(Fluid fluid) {
        return FLUID_TO_ION.get(fluid);
    }

    public static Fluid getFluidForIon(IonType ion) {
        return ION_TO_FLUID.get(ion);
    }

    public static boolean isMappableFluid(Fluid fluid) {
        return FLUID_TO_ION.containsKey(fluid);
    }
}