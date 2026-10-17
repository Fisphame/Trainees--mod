package com.pha.trainees.machine;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;

/**
 * 按面暴露用的流体 handler 包装（§17.1 按面 I/O）：把机器的某些**流体槽**包成只能灌/只能抽。
 *
 * <p>离散流体槽的语义由被包装的 {@link FluidTank} 自己保证（一槽一种流体、整份进出），
 * 这里只管"这个面允许什么方向"。</p>
 */
public class MachineFluidHandler implements IFluidHandler {

    private final FluidTank[] tanks;
    private final int[] slots;
    private final boolean allowFill;
    private final boolean allowDrain;

    public MachineFluidHandler(FluidTank[] tanks, int[] slots, boolean allowFill, boolean allowDrain) {
        this.tanks = tanks;
        this.slots = slots.clone();
        this.allowFill = allowFill;
        this.allowDrain = allowDrain;
    }

    @Override
    public int getTanks() {
        return slots.length;
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        return tanks[slots[tank]].getFluid();
    }

    @Override
    public int getTankCapacity(int tank) {
        return tanks[slots[tank]].getCapacity();
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return allowFill && tanks[slots[tank]].isFluidValid(stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (!allowFill || resource.isEmpty()) return 0;
        // 离散槽：按顺序找第一个能整份收下的槽
        for (int slot : slots) {
            FluidTank tank = tanks[slot];
            FluidStack current = tank.getFluid();
            if (!current.isEmpty() && !current.getFluid().isSame(resource.getFluid())) continue;
            if (tank.getCapacity() - tank.getFluidAmount() < resource.getAmount()) continue;
            return tank.fill(resource, action);
        }
        return 0;
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        if (!allowDrain || resource.isEmpty()) return FluidStack.EMPTY;
        for (int slot : slots) {
            FluidTank tank = tanks[slot];
            if (tank.getFluidAmount() > 0 && tank.getFluid().getFluid().isSame(resource.getFluid())) {
                return tank.drain(tank.getFluidAmount(), action);   // 整槽
            }
        }
        return FluidStack.EMPTY;
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        if (!allowDrain || maxDrain <= 0) return FluidStack.EMPTY;
        for (int slot : slots) {
            FluidTank tank = tanks[slot];
            if (tank.getFluidAmount() > 0) {
                return tank.drain(Math.min(maxDrain, tank.getFluidAmount()), action);
            }
        }
        return FluidStack.EMPTY;
    }
}
