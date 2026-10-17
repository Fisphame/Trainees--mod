package com.pha.trainees.chemistry.blockentity;

import com.pha.trainees.chemistry.gas.GasGridManager;
import com.pha.trainees.chemistry.heat.BathThermalModel;
import com.pha.trainees.chemistry.heat.HeatSourceTracker;
import com.pha.trainees.chemistry.heat.IHeatSource;
import com.pha.trainees.config.ChemConfig;
import com.pha.trainees.registry.ModChemistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 恒温浴（§19.24 ③-4 → §19.38 A1 相变平台）。
 *
 * <p><b>这批的核心变化</b>：它不再"报一个设定点给容器"，而是**自己就是一个有介质、有热容、
 * 有热损失的物体**：加热器按设定点比例调功、环境按牛顿冷却散热、介质发生相变时把温度
 * 钉在转变点（冰水浴 0 ℃、沸水浴 100 ℃ 由此**涌现**）。容器侧完全不用改——
 * {@link #getSourceTemperature()} 现在返回**浴槽的真实温度**（平台期就是那个平台温度）。</p>
 *
 * <p><b>介质会耗尽</b>：冰化完变水、水烧干即停机（{@link #isHeatSourceActive()} 为 false）。
 * "水浴 ≤100 ℃"因此是**物理结果**，`Medium` 上原来的 {@code min/maxSetpointK} 硬编码已删除。</p>
 *
 * <p><b>已知简化</b>（蓝本 §19.38 记录）：油/沙只给工作上限、不建模分解损毁；冰盐共晶点
 * 252 K 是混合物常数（依数性属于 §19.17.1 V2）；换介质会把浴槽温度直接设为新介质的初始温度
 * （"你倒进去的冰盐本来就是冷的"）。</p>
 */
public class ThermostaticBathBlockEntity extends BlockEntity implements IHeatSource {

    /** 介质：给"热物性 + 初始温度"（§19.38：可达温度由物理决定，不再有区间硬编码）。 */
    public enum Medium {
        /** 冰 + 食盐冷冻混合物：平台取**共晶点 252 K**（混合物常数，见类注释） */
        ICE_SALT("ice_salt",
                new BathThermalModel.Medium(252.0, Double.NaN, 6.01, 0, 38.0, 75.3, Double.NaN, 1.0),
                252.0, 252.0),
        /** 水浴：熔点/沸点/潜热全部取自 PhaseChangeTable 的水三相（不另写真源） */
        WATER("water", BathThermalModel.water(), 333.15, 293.15),
        /** 油浴（硅油）：无相变数据，只有工作上限 523.15 K；比热取混合物估计值 */
        OIL("oil",
                new BathThermalModel.Medium(Double.NaN, Double.NaN, 0, 0, 0, 600.0, 523.15, 0),
                400.0, 293.15),
        /** 沙浴：同上，上限 873.15 K；比热取 SiO₂ 的 44.6 J/(mol·K) */
        SAND("sand",
                new BathThermalModel.Medium(Double.NaN, Double.NaN, 0, 0, 0, 44.6, 873.15, 0),
                500.0, 293.15);

        private final String id;
        private final BathThermalModel.Medium spec;
        private final double defaultSetpointK;
        private final double initialTemperatureK;

        Medium(String id, BathThermalModel.Medium spec, double defaultSetpointK, double initialTemperatureK) {
            this.id = id;
            this.spec = spec;
            this.defaultSetpointK = defaultSetpointK;
            this.initialTemperatureK = initialTemperatureK;
        }

        public String id() {
            return id;
        }

        public BathThermalModel.Medium spec() {
            return spec;
        }

        public double defaultSetpointK() {
            return defaultSetpointK;
        }

        public double initialTemperatureK() {
            return initialTemperatureK;
        }

        /** 装填时是否以固相存在：初始温度不高于熔点 ⇒ 冰。 */
        public boolean startsSolid() {
            return spec.hasMelting() && initialTemperatureK <= spec.meltingPointK() + 1e-9;
        }
    }

    /** 设定点调整步长（K）：右键升、潜行右键降 */
    public static final double STEP_K = 5.0;
    /** 设定点可取范围（K）：与创造热源的量级一致；**能不能达到由介质物理决定** */
    public static final double MIN_SETPOINT_K = BathThermalModel.MIN_TEMPERATURE_K;
    public static final double MAX_SETPOINT_K = 3000.0;
    /** 1 tick 的秒数（功率 W × 时间 = J） */
    private static final double TICK_SECONDS = 0.05;
    /** 温度变化超过这个量才广播给客户端（避免每 tick 发包） */
    private static final double SYNC_TEMPERATURE_DELTA_K = 0.5;

    private Medium medium = Medium.WATER;
    private double setpoint = Medium.WATER.defaultSetpointK();
    private double temperatureK = Medium.WATER.initialTemperatureK();
    private double solidMoles;
    private double liquidMoles;
    private int saveTicker;

    /** 电费缓冲（§19.38 六）：外部只充不抽，加热器按**自己搬运的热量**扣。 */
    private final BathEnergy energy = new BathEnergy();
    private final LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> energy);

    public ThermostaticBathBlockEntity(BlockPos pos, BlockState state) {
        super(ModChemistry.ModChemistryBlockEntities.THERMOSTATIC_BATH.get(), pos, state);
        fillMedium(Medium.WATER);
    }

    // ==================== 每 tick 热力学 ====================

    /** 由 {@code ThermostaticBathBlock.getTicker} 驱动（服务端）。 */
    public void tick() {
        if (level == null || level.isClientSide) return;
        double ambient = GasGridManager.environmentTemperature(level, worldPosition);

        // 比例控温：温差决定给功大小与方向（正 = 加热，负 = 制冷），额定功率封顶
        double band = Math.max(0.1, ChemConfig.BATH_PROPORTIONAL_BAND_K.get());
        double watts = ChemConfig.BATH_HEATER_WATTS.get();
        double demand = Mth.clamp((setpoint - temperatureK) / band, -1.0, 1.0);
        double requested = demand * watts;
        // 能耗账（§19.38 六）：设备只为**自己搬运的热量**付电费；环境散热是物理过程，不花钱。
        // 付不起 → 本 tick 只走环境散热（"没电 = 不加热也不制冷"是自然结果，不是特判）。
        double heater = drawHeaterPower(requested * TICK_SECONDS) ? requested : 0;
        // 牛顿冷却：浴槽与环境换热（低温浴靠它回温，热浴靠它耗掉多余热量）
        double loss = -ChemConfig.BATH_AMBIENT_LOSS_W_PER_K.get() * (temperatureK - ambient);

        BathThermalModel.StepResult result = BathThermalModel.step(
                new BathThermalModel.BathState(temperatureK, solidMoles, liquidMoles),
                medium.spec(), (heater + loss) * TICK_SECONDS,
                ChemConfig.BATH_VESSEL_HEAT_CAPACITY_J.get());

        double previousTemperature = temperatureK;
        double previousMoles = totalMoles();
        temperatureK = result.state().temperatureK();
        solidMoles = result.state().solidMoles();
        liquidMoles = result.state().liquidMoles();

        boolean materialChange = Math.abs(totalMoles() - previousMoles) >= 0.05
                || (previousMoles > BathThermalModel.EMPTY_MOLES) != (totalMoles() > BathThermalModel.EMPTY_MOLES);
        if (materialChange) {
            saveTicker = 0;
            markDirtyAndSync();
        } else if (Math.abs(temperatureK - previousTemperature) >= SYNC_TEMPERATURE_DELTA_K) {
            saveTicker = 0;
            markDirtyAndSync();
        } else if (++saveTicker >= 20) {
            // 小步变化不必广播，但每 20 tick 落一次存档，断电/退出不丢温度
            saveTicker = 0;
            setChanged();
        }
        // §19.38 A2：把"浴槽当前温度"登记给气体网格（登记表是无区块访问的纯内存表）
        HeatSourceTracker.update(level, worldPosition, temperatureK, isHeatSourceActive());
    }

    // ==================== 生命周期（热源登记表，§19.38 A2） ====================

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null) {
            HeatSourceTracker.update(level, worldPosition, temperatureK, isHeatSourceActive());
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

    /**
     * 按设备**自己搬运的热量**扣电（§19.38 六）：1 FE = 1 J，效率 η 计入。
     * 加热与制冷都计费（制冷是热泵，同样要干活）；付不起返回 false → 本 tick 不供热。
     *
     * @param joules 本 tick 搬运的热量（J，正 = 加热、负 = 制冷）
     */
    private boolean drawHeaterPower(double joules) {
        if (joules == 0) return true;
        double efficiency = Mth.clamp(ChemConfig.BATH_HEATER_EFFICIENCY.get(), 0.05, 1.0);
        int cost = (int) Math.ceil(Math.abs(joules) / efficiency);
        if (energy.stored() < cost) return false;
        energy.consume(cost);
        return true;
    }

    // ==================== IHeatSource ====================

    /** 返回**浴槽真实温度**（平台期就是相变温度），而不是设定点。 */
    @Override
    public double getSourceTemperature() {
        return temperatureK;
    }

    /** 没介质就不供热（烧干 / 冰化完的水浴仍然是水浴，只有烧干才停机）。 */
    @Override
    public boolean isHeatSourceActive() {
        return !isEmpty();
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

    /** 浴槽搬运热量的上限 = 它的额定加热功率（否则容器会把它当无限大热库）。 */
    @Override
    public double maxTransferWatts() {
        return ChemConfig.BATH_HEATER_WATTS.get();
    }

    /**
     * §19.38 十一：浴槽**真的**参与换热，不再是"白拿白给"的边界。
     *
     * <p>容器比浴热（{@code joules < 0}）⇒ 热量**收进介质**：水会被烧开、甚至烧干；
     * 容器比浴冷（{@code joules > 0}）⇒ 热量**从介质取出**：这正是浴槽给容器降温的物理来源，
     * 也是为什么浴槽需要加热器维持温度。</p>
     *
     * <p>不计电费：电费只在"加热器自己搬运热量"时收（{@link #tick()} 里那笔）；
     * 这里只是介质与容器之间的传导。</p>
     */
    @Override
    public boolean payHeatCost(double joules) {
        if (joules == 0) return true;
        if (isEmpty()) return false;
        applyMediumJoules(-joules);
        return true;
    }

    /**
     * 把一笔热量施加到介质上（正 = 介质吸热），并同步温度/登记表。
     * 加热器（tick）与容器换热（payHeatCost）共用这一条通路。
     */
    private void applyMediumJoules(double joules) {
        double previousTemperature = temperatureK;
        BathThermalModel.StepResult result = BathThermalModel.step(
                new BathThermalModel.BathState(temperatureK, solidMoles, liquidMoles),
                medium.spec(), joules, ChemConfig.BATH_VESSEL_HEAT_CAPACITY_J.get());
        temperatureK = result.state().temperatureK();
        solidMoles = result.state().solidMoles();
        liquidMoles = result.state().liquidMoles();
        if (Math.abs(temperatureK - previousTemperature) >= SYNC_TEMPERATURE_DELTA_K) {
            markDirtyAndSync();
        }
        if (level != null) {
            HeatSourceTracker.update(level, worldPosition, temperatureK, isHeatSourceActive());
        }
    }

    // ==================== 状态 ====================

    public Medium getMedium() {
        return medium;
    }

    public double getSetpoint() {
        return setpoint;
    }

    public double getTemperatureK() {
        return temperatureK;
    }

    public double totalMoles() {
        return solidMoles + liquidMoles;
    }

    public boolean isEmpty() {
        return totalMoles() <= BathThermalModel.EMPTY_MOLES;
    }

    /** 固相（冰）剩余摩尔数；非相变介质恒为 0。 */
    public double getSolidMoles() {
        return solidMoles;
    }

    /** 液相剩余摩尔数；油/沙这类无相变介质把全部质量记在这里（只是热量载体，不代表物态）。 */
    public double getLiquidMoles() {
        return liquidMoles;
    }

    /** 设定设定点（夹进物理允许的全局范围，返回实际生效值）。 */
    public double setSetpoint(double kelvin) {
        double clamped = Mth.clamp(kelvin, MIN_SETPOINT_K, MAX_SETPOINT_K);
        if (Math.abs(clamped - setpoint) > 1e-6) {
            setpoint = clamped;
            markDirtyAndSync();
        }
        return setpoint;
    }

    /** 按步长调整设定点。 */
    public double adjustSetpoint(double deltaK) {
        return setSetpoint(setpoint + deltaK);
    }

    /**
     * 更换介质：装填新介质（摩尔数取配置）并把浴温设为该介质的初始温度——
     * 你倒进去的冰盐本来就是冷的、沙子本来就是室温的。设定点跟随介质默认值。
     */
    public void setMedium(Medium newMedium) {
        if (newMedium == null || newMedium == medium) return;
        fillMedium(newMedium);
        setpoint = newMedium.defaultSetpointK();
        markDirtyAndSync();
    }

    private void fillMedium(Medium target) {
        medium = target;
        double moles = Math.max(0.1, ChemConfig.BATH_MEDIUM_MOLES.get());
        temperatureK = target.initialTemperatureK();
        solidMoles = target.startsSolid() ? moles : 0;
        liquidMoles = target.startsSolid() ? 0 : moles;
    }

    private void markDirtyAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ==================== 能力（§19.38 六） ====================

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

    /** 当前电量（FE）：供提示/后续 GUI 读数。 */
    public int getEnergyStored() {
        return energy.stored();
    }

    // ==================== 持久化 ====================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Medium", medium.id());
        tag.putDouble("Setpoint", setpoint);
        tag.putDouble("Temperature", temperatureK);
        tag.putDouble("SolidMoles", solidMoles);
        tag.putDouble("LiquidMoles", liquidMoles);
        tag.putInt("Energy", energy.stored());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Medium")) {
            for (Medium candidate : Medium.values()) {
                if (candidate.id().equals(tag.getString("Medium"))) {
                    medium = candidate;
                    break;
                }
            }
        }
        setpoint = Mth.clamp(tag.contains("Setpoint") ? tag.getDouble("Setpoint") : medium.defaultSetpointK(),
                MIN_SETPOINT_K, MAX_SETPOINT_K);
        // 缺字段的旧存档：按新介质重新装填（不把"0 mol"当成烧干）
        boolean hasFill = tag.contains("SolidMoles") || tag.contains("LiquidMoles");
        if (hasFill) {
            temperatureK = tag.getDouble("Temperature");
            solidMoles = tag.getDouble("SolidMoles");
            liquidMoles = tag.getDouble("LiquidMoles");
        } else {
            fillMedium(medium);
        }
        energy.setStored(tag.getInt("Energy"));
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putString("Medium", medium.id());
        tag.putDouble("Setpoint", setpoint);
        tag.putDouble("Temperature", temperatureK);
        tag.putDouble("SolidMoles", solidMoles);
        tag.putDouble("LiquidMoles", liquidMoles);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** 浴槽电费缓冲：外部只充不抽；加热器按交付热量扣（与电加热夹套同款，§19.29/§19.38）。 */
    private class BathEnergy implements IEnergyStorage {
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
            return Math.max(1, ChemConfig.BATH_ENERGY_CAPACITY.get());
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
