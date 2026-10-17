//package com.pha.trainees.chemistry.fluid;
//
//import com.pha.trainees.registry.ModChemistry;
//import net.minecraft.resources.ResourceLocation;
//import net.minecraft.world.level.block.Block;
//import net.minecraft.world.level.material.Fluid;
//import net.minecraft.world.level.material.FluidState;
//import net.minecraft.world.level.material.Fluids;
//import net.minecraftforge.fluids.FluidType;
//import net.minecraftforge.fluids.ForgeFlowingFluid;
//import net.minecraftforge.registries.RegistryObject;
//
//import static com.pha.trainees.registry.ModChemistry.ModFluids.*;
//
///**
// * 盐酸流体（HCl 水溶液）
// * 强电解质，在水中完全电离为 H+ 和 Cl-
// */
//public class HydrochloricAcidFluid {
//
//    // 定义流体属性
//    public static final FluidType.Properties PROPERTIES = FluidType.Properties.create()
//            .descriptionId("block.trainees.hydrochloric_acid")
//            .temperature(293)
//            .viscosity(1000)
//            .density(1100) // 比水略重
//            .canConvertToSource(false);
//
//    // 注册流体类型（用于世界渲染和物理行为）
//    public static final RegistryObject<FluidType> TYPE = ModChemistry.ModFluids.FLUID_TYPES.register(
//            "hydrochloric_acid",
//            () -> new FluidType(PROPERTIES) {
//                @Override
//                public ResourceLocation getStillTexture() {
//                    return new ResourceLocation("trainees", "block/hydrochloric_acid_still");
//                }
//
//                @Override
//                public ResourceLocation getFlowingTexture() {
//                    return new ResourceLocation("trainees", "block/hydrochloric_acid_flow");
//                }
//            }
//    );
//
//    // 注册流体本身（Source / Flowing）
//    public static final RegistryObject<ForgeFlowingFluid.Source> SOURCE = ModChemistry.ModFluids.FLUIDS.register(
//            "hydrochloric_acid",
//            () -> new ForgeFlowingFluid.Source(FLUID_PROPERTIES)
//    );
//
//    public static final RegistryObject<ForgeFlowingFluid.Flowing> FLOWING = ModChemistry.ModFluids.FLUIDS.register(
//            "hydrochloric_acid_flowing",
//            () -> new ForgeFlowingFluid.Flowing(FLUID_PROPERTIES)
//    );
//
//    // 流体属性（用于桶装和容器交互）
//    public static final ForgeFlowingFluid.Properties FLUID_PROPERTIES = new ForgeFlowingFluid.Properties(
//            TYPE,
//            SOURCE,
//            FLOWING
//    ).bucket(HYDROCHLORIC_ACID_BUCKET)
//            .block(() -> ModChemistry.ModFluids.HYDROCHLORIC_ACID_BLOCK.get());
//
//}