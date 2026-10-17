package com.pha.trainees.fluid;

import com.pha.trainees.Main;
import com.pha.trainees.item.ChemicalItem;
import com.pha.trainees.registry.ModChemistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/**
 * 基础化学流体，用于溶液或液体化学品。
 * 关联一个固体化学物品（BaseChemicalItem），从中获取化学式和摩尔质量，
 * 并拥有自己的密度（g/mL）。用于机器配方中的流体反应物/生成物。
 */
public abstract class BaseChemicalFluid extends ForgeFlowingFluid.Source {

    protected BaseChemicalFluid(Properties properties) {
        super(properties);
    }


    public abstract ChemicalItem.BaseChemicalItem getSolidForm();
    public abstract double getDensity();

    public String getFormula() {
        return getSolidForm().getFormula();
    }
    public double getMolarMass() {
        return getSolidForm().getMolarMass();
    }



    /**
     * 根据物质的量（mol）计算体积（mB）
     * @param moles 物质的量（mol）
     * @return 体积（mB）
     */
    public int calculateVolumeFromMoles(double moles) {
        double mass = moles * getMolarMass();          // 质量 = 摩尔数 × 摩尔质量 (g)
        double volume = mass / getDensity();            // 体积 = 质量 / 密度 (mL)
        return (int) Math.round(volume);                // 1 mL = 1 mB，返回整数
    }

    /**
     * 根据体积（mB）计算物质的量（mol）
     * @param volume 体积（mB）
     * @return 物质的量（mol）
     */
    public double calculateMolesFromVolume(int volume) {
        double mass = volume * getDensity();            // 质量 = 体积 × 密度 (g)
        return mass / getMolarMass();                   // 物质的量 = 质量 / 摩尔质量 (mol)
    }

    /**
     * 创建流体属性（用于注册）
     * @param fluidType 流体类型注册对象
     * @param bucket 桶物品注册对象
     * @return ForgeFlowingFluid.Properties
     */
    public static Properties createProperties(RegistryObject<FluidType> fluidType,
                                                 Supplier<? extends Fluid> source,
                                                 RegistryObject<Item> bucket) {
        // 流动流体也使用源流体的 Supplier
        return new Properties(fluidType, source, source)
                .bucket(bucket)
//                .canMultiply(false)      // 不产生无限源
//                .flowSpeed(0)            // 不流动
                .levelDecreasePerBlock(0);
    }
}
