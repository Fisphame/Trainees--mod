package com.pha.trainees.chemistry.blockentity;

import com.pha.trainees.chemistry.heat.HeatSourceTracker;
import com.pha.trainees.chemistry.heat.IHeatSource;
import com.pha.trainees.registry.ModChemistry;
import com.pha.trainees.registry.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
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
 * 电加热夹套（§19.29 第 2 步）：贴在容器邻面上的<b>附着件形态热源</b>。
 *
 * <p><b>现实依据</b>：真实反应釜的加热方式之一是"把电阻丝缠绕在筒体的绝缘层上"，
 * 适用加热温度在 <b>400 ℃ 以下</b>，且<b>热效率 &lt;85%</b>（§19.29 调研）。
 * 本方块照此建模：设定点上限 673 K、按"交付热量 ÷ η"计费。</p>
 *
 * <p><b>为什么按交付热量计费（而不是固定 FE/tick）</b>：这样"把大容器烧到高温"自然更贵，
 * 而不需要任何针对容积的特判；同时让 {@link IHeatSource#payHeatCost} 变成真正的能量记账——
 * 电不够就本 tick 不供热。</p>
 *
 * <p><b>只能加热、不能制冷</b>：这正是电加热夹套的真实局限（要降温得靠水浴/冰盐浴从下方，
 * 或将来的冷却夹套）。冷却时热量为负，不计费也不生效。</p>
 */
public class JacketBlockEntity extends BlockEntity implements IHeatSource,
        com.pha.trainees.chemistry.container.ICorrodibleAttachment {

    /** 设定点步长（K）：右键升、潜行右键降 */
    public static final double STEP_K = 5.0;
    /** 设定点下限：电加热只往热了做，低于室温无意义 */
    public static final double MIN_SETPOINT_K = 293.15;
    /** 设定点上限：现实电阻丝加热 ≤400 ℃（§19.29 调研） */
    public static final double MAX_SETPOINT_K = 673.15;

    private static final String NBT_SETPOINT = "Setpoint";
    private static final String NBT_ENERGY = "Energy";

    private double setpoint = 373.15;

    private final JacketEnergy energy = new JacketEnergy();
    private final LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> energy);

    /** 有电才工作（每 tick 重算，不持久化） */
    private boolean running = false;

    public JacketBlockEntity(BlockPos pos, BlockState state) {
        this(ModChemistry.ModChemistryBlockEntities.JACKET.get(), pos, state);
    }

    protected JacketBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, JacketBlockEntity jacket) {
        if (level.isClientSide) return;
        boolean powered = jacket.energy.stored() > 0;
        if (jacket.running != powered) {
            jacket.running = powered;
            jacket.setChanged();
        }
        // §19.38 A2：夹套也是热源，登记给气体网格（关机/没电 → 不参与）
        HeatSourceTracker.update(level, pos, jacket.getSourceTemperature(), jacket.isHeatSourceActive());
    }

    // ==================== 生命周期（热源登记表，§19.38 A2） ====================

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null) {
            HeatSourceTracker.update(level, worldPosition, getSourceTemperature(), isHeatSourceActive());
        }
    }

    @Override
    public void setRemoved() {
        if (level != null) HeatSourceTracker.unregister(level, worldPosition);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        if (level != null) HeatSourceTracker.unregister(level, worldPosition);
        super.onChunkUnloaded();
    }

    // ==================== IHeatSource ====================

    @Override
    public double getSourceTemperature() {
        return setpoint;
    }

    @Override
    public boolean isHeatSourceActive() {
        return running;
    }

    @Override
    public boolean isSettable() {
        return true;
    }

    @Override
    public boolean setSourceTemperature(double kelvin) {
        setSetpoint(kelvin);
        return true;
    }

    /** 按交付热量扣电：交付 1 J 热量需付 1/η J 电（1 FE = 1 J）。付不起返回 false。 */
    @Override
    public boolean payHeatCost(double joules) {
        if (joules <= 0) return true;
        double efficiency = Mth.clamp(ModConfig.COMMON.jacketEfficiency.get(), 0.05, 1.0);
        int costFe = (int) Math.ceil(joules / efficiency);
        if (energy.stored() < costFe) return false;
        energy.consume(costFe);
        return true;
    }

    // ==================== 状态 ====================

    public boolean isRunning() {
        return running;
    }

    public double getSetpoint() {
        return setpoint;
    }

    /** 设定设定点（夹取到电加热的现实区间），返回实际生效值。 */
    public double setSetpoint(double kelvin) {
        double clamped = Mth.clamp(kelvin, MIN_SETPOINT_K, MAX_SETPOINT_K);
        if (Math.abs(clamped - setpoint) > 1e-6) {
            setpoint = clamped;
            markDirtyAndSync();
            if (level != null) {
                HeatSourceTracker.update(level, worldPosition, setpoint, isHeatSourceActive());
            }
        }
        return setpoint;
    }

    public double adjustSetpoint(double deltaK) {
        return setSetpoint(setpoint + deltaK);
    }

    public int getEnergyStored() {
        return energy.stored();
    }

    public int getMaxEnergyStored() {
        return energy.getMaxEnergyStored();
    }

    private void markDirtyAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
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

    // ==================== 持久化 ====================

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putDouble(NBT_SETPOINT, setpoint);
        tag.putInt(NBT_ENERGY, energy.stored());
        tag.putDouble(com.pha.trainees.chemistry.container.AttachmentCorrosion.NBT_KEY, attachmentCorrosion);
    }

    // ==================== 腐蚀（§19.32 附着件腐蚀） ====================

    /** 夹套内壁的腐蚀度 0~1（持久化在本方块实体里，跟着方块走）。 */
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

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        attachmentCorrosion = tag.getDouble(com.pha.trainees.chemistry.container.AttachmentCorrosion.NBT_KEY);
        if (tag.contains(NBT_SETPOINT)) {
            setpoint = Mth.clamp(tag.getDouble(NBT_SETPOINT), MIN_SETPOINT_K, MAX_SETPOINT_K);
        }
        energy.setStored(tag.getInt(NBT_ENERGY));
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putDouble(NBT_SETPOINT, setpoint);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** 夹套电费缓冲：外部只充不抽；内部按交付热量扣。 */
    private class JacketEnergy implements IEnergyStorage {
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
            return Math.max(1, ModConfig.COMMON.jacketEnergyCapacity.get());
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
