package com.pha.trainees.registry;

import com.pha.trainees.Main;
import com.pha.trainees.fluid.BaseChemicalFluid;
import com.pha.trainees.fluid.CheHbpFluid;
import com.pha.trainees.item.ChemicalBucketItem;
import com.pha.trainees.item.ChemicalItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.*;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, Main.MODID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(ForgeRegistries.FLUIDS, Main.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Main.MODID);

    public static final RegistryObject<FluidType> CHE_HBP_FLUID_TYPE = FLUID_TYPES.register("che_hbp",
            () -> new FluidType(FluidType.Properties.create()
                    .density(1500)
                    .viscosity(2000)
                    .temperature(300)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
            ));

    // 为每个流体创建一个 FluidType（通常只需要一个，但为了扩展可单独创建）
    public static final RegistryObject<FluidType> BASE_TYPE = FLUID_TYPES.register("base_solution",
            () -> new FluidType(FluidType.Properties.create()
                    .density(1200)        // 可选，影响流体在流体中的上下位置
                    .temperature(300)
            )
    );

//    // 氢氧化鸡溶液
//    public static final RegistryObject<Fluid> JIOH_SOLUTION = FLUIDS.register("jioh_solution",
//            () -> new BaseChemicalFluid(
//                    BaseChemicalFluid.createProperties(
//                            BASE_TYPE,
//                            JIOH_SOLUTION, // 源流体自身 Supplier
//                            ModFluidBuckets.JIOH_SOLUTION_BUCKET
//                    )
//            ) {
//                @Override
//                public ChemicalItem.BaseChemicalItem getSolidForm() {
//                    return (ChemicalItem.BaseChemicalItem) ModChemistry.ModChemistryItems.CHE_JIOH_INGOT.get();
//                }
//
//                @Override
//                public double getDensity() {
//                    return 1.2;
//                }
//            });

//    public static class ModFluidBuckets {
//
//        public static final RegistryObject<Item> JIOH_SOLUTION_BUCKET = ITEMS.register("jioh_solution_bucket",
//                () -> new ChemicalBucketItem(
//                        (BaseChemicalFluid) ModFluids.JIOH_SOLUTION.get(),
//                        new Item.Properties().stacksTo(1)
//                ));
//
//
//    }

//            {
//                // 因为流体不会在世界中渲染，覆盖 getStillTexture 返回空纹理即可
//                @Override
//                public ResourceLocation getStillTexture() {
//                    return new ResourceLocation(Main.MODID, "block/empty");
//                }
//                @Override
//                public ResourceLocation getFlowingTexture() {
//                    return new ResourceLocation(Main.MODID, "block/empty");
//                }
//            }

    private static <T extends Fluid> T register(String p_76198_, T p_76199_) {
        return Registry.register(BuiltInRegistries.FLUID, p_76198_, p_76199_);
    }

    public static final RegistryObject<FlowingFluid> SOURCE_CHE_HBP = FLUIDS.register("che_hbp_fluid",
            () -> new CheHbpFluid.Source(ModFluids.CHE_HBP_PROPERTIES));

    public static final RegistryObject<FlowingFluid> FLOWING_CHE_HBP = FLUIDS.register("flowing_che_hbp",
            () -> new CheHbpFluid.Flowing(ModFluids.CHE_HBP_PROPERTIES));

    public static final ForgeFlowingFluid.Properties CHE_HBP_PROPERTIES = new ForgeFlowingFluid.Properties(
            CHE_HBP_FLUID_TYPE,
            SOURCE_CHE_HBP,
            FLOWING_CHE_HBP)
            .slopeFindDistance(2)
            .levelDecreasePerBlock(2);
//            .block(ModBlocks.CHE_HBP_BLOCK)
//            .bucket(ModItems.CHE_HBP_BUCKET);
}