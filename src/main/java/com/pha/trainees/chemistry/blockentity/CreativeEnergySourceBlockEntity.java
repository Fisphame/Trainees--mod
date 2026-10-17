package com.pha.trainees.chemistry.blockentity;

import com.pha.trainees.registry.ModChemistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 创造能源方块（§19.38 附）：**无限 FE 源**，专供测试用电设备（恒温浴加热器、电加热夹套、打包机…）。
 *
 * <p>与"创造热源"同一性质：它是调试/创造设备，不是玩法内容——所以它**只出电、不接受充电**，
 * 输出上限由抽取方的请求上限决定（线缆要多少给多少）。要多少给多少正是"无限源"的定义，
 * 别在它上面再叠一个限流参数。</p>
 */
public class CreativeEnergySourceBlockEntity extends BlockEntity {

    /** 对外报告的电量：只要大到任何比较都过，就够用（它不是储能，别按储能理解）。 */
    private static final int INDICATED_STORAGE = 1_000_000;

    private final LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(InfiniteEnergy::new);

    public CreativeEnergySourceBlockEntity(BlockPos pos, BlockState state) {
        super(ModChemistry.ModChemistryBlockEntities.CREATIVE_ENERGY_SOURCE.get(), pos, state);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCap.invalidate();
    }

    /** 无限源：抽取请求照单全收，充电一律拒绝。 */
    private static final class InfiniteEnergy implements IEnergyStorage {

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return Math.max(0, maxExtract);
        }

        @Override
        public int getEnergyStored() {
            return INDICATED_STORAGE;
        }

        @Override
        public int getMaxEnergyStored() {
            return INDICATED_STORAGE;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    }
}
