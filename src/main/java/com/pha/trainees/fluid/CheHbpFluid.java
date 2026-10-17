package com.pha.trainees.fluid;

import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.fluids.ForgeFlowingFluid;

public class CheHbpFluid extends ForgeFlowingFluid {
    protected CheHbpFluid(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isSource(FluidState p_76140_) {
        return true;
    }

    @Override
    public int getAmount(FluidState p_164509_) {
        return 8;
    }

//    public static class Source extends CheHbpFluid {
//        public Source(Properties properties) {
//            super(properties);
//        }
//
//        @Override
//        public int getAmount(FluidState state) {
//            return 8;
//        }
//
//        @Override
//        public boolean isSource(FluidState state) {
//            return true;
//        }
//    }
//
//    public static class Flowing extends CheHbpFluid {
//        public Flowing(Properties properties) {
//            super(properties);
//        }
//
//        @Override
//        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
//            super.createFluidStateDefinition(builder);
//            builder.add(LEVEL);
//        }
//
//        @Override
//        public int getAmount(FluidState state) {
//            return state.getValue(LEVEL);
//        }
//
//        @Override
//        public boolean isSource(FluidState state) {
//            return false;
//        }
//    }
}