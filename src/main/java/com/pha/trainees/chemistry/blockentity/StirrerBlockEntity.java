package com.pha.trainees.chemistry.blockentity;

import com.pha.trainees.registry.ModChemistry;
import com.pha.trainees.registry.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 搅拌器方块实体（§19.29）：内部 FE 缓冲（外部只充不抽，1 FE = 1 J 同口径），
 * 每 tick 扣一次电费；<b>扣得动就转，扣不动就停</b>。
 *
 * <p>对外只暴露两个乘性因子给容器读（{@code heatTransferFactor} / {@code gasTransferFactor}），
 * 不工作时都返回 1.0。</p>
 */
public class StirrerBlockEntity extends BlockEntity
        implements com.pha.trainees.chemistry.container.ICorrodibleAttachment {

    private static final String NBT_ENERGY = "Energy";

    private final StirrerEnergy energy = new StirrerEnergy();
    private final LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> energy);

    /** 上一 tick 是否在转（每 tick 重算，不持久化） */
    private boolean running = false;

    public StirrerBlockEntity(BlockPos pos, BlockState state) {
        this(ModChemistry.ModChemistryBlockEntities.STIRRER.get(), pos, state);
    }

    protected StirrerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, StirrerBlockEntity stirrer) {
        if (level.isClientSide) return;
        int cost = Math.max(0, ModConfig.COMMON.stirrerEnergyPerTick.get());
        boolean canRun = cost == 0 || stirrer.energy.stored() >= cost;
        if (canRun && cost > 0) {
            stirrer.energy.consume(cost);
        }
        if (stirrer.running != canRun) {
            stirrer.running = canRun;
            stirrer.setChanged();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public double heatTransferFactor() {
        return running ? Math.max(1.0, ModConfig.COMMON.stirrerHeatFactor.get()) : 1.0;
    }

    public double gasTransferFactor() {
        return running ? Math.max(1.0, ModConfig.COMMON.stirrerGasFactor.get()) : 1.0;
    }

    public int getEnergyStored() {
        return energy.stored();
    }

    public int getMaxEnergyStored() {
        return energy.getMaxEnergyStored();
    }

    // ==================== 能力 ====================

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

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(NBT_ENERGY, energy.stored());
        tag.putDouble(com.pha.trainees.chemistry.container.AttachmentCorrosion.NBT_KEY, attachmentCorrosion);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        energy.setStored(tag.getInt(NBT_ENERGY));
        attachmentCorrosion = tag.getDouble(com.pha.trainees.chemistry.container.AttachmentCorrosion.NBT_KEY);
    }

    // ==================== 腐蚀（§19.32 附着件腐蚀） ====================

    /** 搅拌轴的腐蚀度 0~1（持久化在本方块实体里，跟着方块走）。 */
    private double attachmentCorrosion = 0.0;

    @Override
    public double getAttachmentCorrosion() {
        return attachmentCorrosion;
    }

    @Override
    public void applyAttachmentCorrosion(net.minecraft.world.level.Level level,
                                         net.minecraft.core.BlockPos pos, double delta) {
        attachmentCorrosion = com.pha.trainees.chemistry.container.AttachmentCorrosion.apply(
                level, pos, attachmentCorrosion, delta, attachmentMaterial().id());
        setChanged();
    }

    /** 搅拌器电费缓冲：外部只能充、不能抽；内部每 tick 按配置扣。 */
    private class StirrerEnergy implements IEnergyStorage {
        private int stored = 0;

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int capacity = getMaxEnergyStored();
            int accepted = Math.min(Math.max(0, maxReceive), Math.max(0, capacity - stored));
            if (!simulate && accepted > 0) {
                stored += accepted;
                setChanged();
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;   // 只进不出
        }

        @Override
        public int getEnergyStored() {
            return stored;
        }

        @Override
        public int getMaxEnergyStored() {
            return Math.max(1, ModConfig.COMMON.stirrerEnergyCapacity.get());
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }

        int stored() {
            return stored;
        }

        void setStored(int value) {
            stored = Math.max(0, Math.min(value, getMaxEnergyStored()));
        }

        void consume(int amount) {
            int used = Math.min(stored, Math.max(0, amount));
            if (used > 0) {
                stored -= used;
                setChanged();
            }
        }
    }
}
