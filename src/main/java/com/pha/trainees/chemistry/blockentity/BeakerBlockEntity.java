package com.pha.trainees.chemistry.blockentity;

import com.pha.trainees.Main;
import com.pha.trainees.chemistry.block.BeakerBlock;
import com.pha.trainees.chemistry.container.IChemicalContainer;
import com.pha.trainees.chemistry.container.ContainerMaterial;
import com.pha.trainees.chemistry.container.ErosionType;
import com.pha.trainees.chemistry.container.CorrosionMath;
import com.pha.trainees.chemistry.container.CorrosionTable;
import com.pha.trainees.chemistry.container.IContainerAttachment;
import com.pha.trainees.chemistry.container.IContainerPort;
import com.pha.trainees.chemistry.engine.ExecutedRule;
import com.pha.trainees.chemistry.engine.ReactionEngine;
import com.pha.trainees.chemistry.engine.ReactionFailure;
import com.pha.trainees.chemistry.gas.GasGridManager;
import com.pha.trainees.chemistry.gas.GasMixture;
import com.pha.trainees.chemistry.gas.GasPhysics;
import com.pha.trainees.chemistry.heat.IHeatSource;
import com.pha.trainees.chemistry.item.SubstanceItem;
import com.pha.trainees.chemistry.particle.IonType;
import com.pha.trainees.chemistry.particle.Phase;
import com.pha.trainees.chemistry.reaction.ReactionRule;
import com.pha.trainees.chemistry.util.FluidIonMapper;
import com.pha.trainees.config.ChemConfig;
import com.pha.trainees.registry.ModChemistry;
import com.pha.trainees.registry.ModChemistry.ModIons;
import com.pha.trainees.registry.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BeakerBlockEntity extends BlockEntity implements IChemicalContainer, IFluidHandler {

    private static final double DEFAULT_TEMPERATURE = ChemConfig.DEFAULT_TEMPERATURE.get();
    private static final double DEFAULT_VOLUME = ChemConfig.DEFAULT_VOLUME.get();

    /** 一个游戏 tick 的秒数：导热系数（W/K）换算成"每 tick 交付的焦耳"要用它（§19.38 十一） */
    private static final double SECONDS_PER_TICK = 0.05;

    private static final Map<Block, Double> HEAT_SOURCE_TEMPS = new HashMap<>();
    static {
        HEAT_SOURCE_TEMPS.put(Blocks.FIRE, ChemConfig.HEAT_SOURCE_FIRE.get());
        HEAT_SOURCE_TEMPS.put(Blocks.SOUL_FIRE, ChemConfig.HEAT_SOURCE_SOUL_FIRE.get());
        HEAT_SOURCE_TEMPS.put(Blocks.LAVA, ChemConfig.HEAT_SOURCE_LAVA.get());
//        HEAT_SOURCE_TEMPS.put(Blocks.FLOWING_LAVA, 1500.0);
        HEAT_SOURCE_TEMPS.put(Blocks.MAGMA_BLOCK, ChemConfig.HEAT_SOURCE_MAGMA.get());
        HEAT_SOURCE_TEMPS.put(Blocks.CAMPFIRE, ChemConfig.HEAT_SOURCE_CAMPFIRE.get());
        HEAT_SOURCE_TEMPS.put(Blocks.SOUL_CAMPFIRE, ChemConfig.HEAT_SOURCE_SOUL_CAMPFIRE.get());
    }

    private final Map<IonType, Double> contents = new ConcurrentHashMap<>();
    /**
     * 逐面逸散缓冲（#10）：容器扣减后先按面累积，达到网格消散阈值才抛入<b>该面外侧</b>的格
     * （防微量尾巴格）。烧杯只有上口，行为与历史的单一缓冲逐字一致。
     */
    private final Map<Direction, Map<IonType, Double>> gridLeakBuffers = new EnumMap<>(Direction.class);

    /** 端口面：烧杯/电解槽只有上口——玻璃壁不是"口"。将来多口反应釜/盖子覆写此集合或加 override。 */
    private static final Set<Direction> PORT_FACES = Set.of(Direction.UP);

    /** 逐面开口覆盖（null = 未设置 → 按"端口 ∩ 外侧未被阻挡"判定）。供将来的盖子/阀门/贴片。 */
    private final Map<Direction, Boolean> faceOpenOverride = new EnumMap<>(Direction.class);

    /**
     * 上一 tick 是否密闭。用于"揭盖喷一股"的瞬时判定：只有"密闭 → 相通"的那一刻才喷超压。
     * <b>初值 false（当作一直是敞口）</b>——否则重新载入存档时会把攒在敞口烧杯里的气一次喷光，
     * 破坏既有玩法与测试锚点（§19.27 决策 A）。
     */
    private boolean wasSealed = false;
    private double temperature = DEFAULT_TEMPERATURE;
    private double volume = DEFAULT_VOLUME;
    private double totalHeatCapacity = ChemConfig.BEAKER_BASE_HEAT_CAPACITY.get();

    private final Set<ReactionRule> balancedRules = ConcurrentHashMap.newKeySet();


    public BeakerBlockEntity(BlockPos pos, BlockState state) {
        this(ModChemistry.ModChemistryBlockEntities.BEAKER.get(), pos, state);
    }

    /** 子类（如电解槽）可传入自己的 BE 类型，复用烧杯的全部容器实现 */
    protected BeakerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ==================== IChemicalContainer 实现 ====================

    @Override
    public Map<IonType, Double> getContents() {
        return Map.copyOf(contents);
    }

    @Override
    public double getAmount(IonType ion) {
        return contents.getOrDefault(ion, 0.0);
    }


    @Override
    public synchronized double addIon(IonType ion, double moles) {
        return addIon(ion, moles, true);
    }

    @Override
    public synchronized double addIon(IonType ion, double moles, boolean triggerEngine) {
        if (moles <= 0 || ion == null) return 0;
        double current = contents.getOrDefault(ion, 0.0);
        double newAmount = current + moles;
        contents.put(ion, newAmount);
        recalcHeatCapacity();
        setChanged();
        syncToClient();
        resetBalancedRules();
        if (triggerEngine && level != null && !level.isClientSide) {
            ReactionEngine.trigger(this, ion);
        }
        return moles;
    }

    @Override
    public synchronized double removeIon(IonType ion, double moles) {
        return removeIon(ion, moles, true);
    }

    @Override
    public synchronized double removeIon(IonType ion, double moles, boolean triggerEngine) {
        if (moles <= 0 || ion == null) return 0;
        double current = contents.getOrDefault(ion, 0.0);
        double removed = Math.min(current, moles);
        if (removed <= 0) return 0;
        double newAmount = current - removed;
        if (newAmount < 1e-9) {
            contents.remove(ion);
        } else {
            contents.put(ion, newAmount);
        }
        recalcHeatCapacity();
        setChanged();
        syncToClient();
        resetBalancedRules();
        if (triggerEngine && level != null && !level.isClientSide) {
            ReactionEngine.trigger(this, ion);
        }
        return removed;
    }


    @Override
    public boolean contains(IonType ion, double minMoles) {
        return contents.getOrDefault(ion, 0.0) >= minMoles - 1e-9;
    }

    @Override
    public Set<IonType> getPresentIons() {
        return contents.keySet();
    }

    @Override
    public double getTemperature() {
        return temperature;
    }

    @Override
    public void setTemperature(double temperatureKelvin) {
        this.temperature = Math.max(0, temperatureKelvin);
        setChanged();
        syncToClient();
    }

    @Override
    public double getVolume() {
        return volume;
    }

    @Override
    public void setVolume(double volumeLiters) {
        if (volumeLiters > 0) {
            this.volume = volumeLiters;
            setChanged();
            syncToClient();
        }
    }

    @Override
    public double getPressure() {
        // 压强仅由气相粒子贡献（蓝本 §4.2 / §19.27）：统一走 GasPhysics，容器与网格格共用同一公式。
        // 单位：V 以升(L)计，nRT/V 得到 kPa（1 J/L = 1 kPa），再除 101.325 得 atm。
        return getIdealPressureAtm();
    }

    // ==================== 开口与密封（#10 / §19.27） ====================

    /** 端口面（可通气方向，§19.28）。默认上口；多口宿主（反应釜 / 多口盖）覆写此方法即可。 */
    protected Set<Direction> portFaces() {
        return PORT_FACES;
    }

    @Override
    public Set<Direction> getPortFaces() {
        return portFaces();
    }

    @Override
    public boolean isFaceOpen(Direction face) {
        Boolean override = faceOpenOverride.get(face);
        if (override != null) return override;
        if (!getPortFaces().contains(face)) return false;
        Level level = getLevel();
        if (level == null) return false;
        BlockPos outside = getBlockPos().relative(face);
        BlockState outsideState = level.getBlockState(outside);
        // 端口方块（阀门；将来的法兰与管道接口）自报通断；不是端口就沿用"非空气即阻挡"（§19.28）
        if (outsideState.getBlock() instanceof IContainerPort port) {
            return port.isPortPassable(level, outside, face.getOpposite());
        }
        return outsideState.isAir();
    }

    /**
     * 设置/清除某面的开口覆盖：true = 强制相通，false = 强制封闭，null = 交还默认判定。
     * Stage 1 无人调用——将来的盖子/阀门/管道贴片走这里，不必改交换逻辑。
     */
    public void setFaceOpenOverride(Direction face, @Nullable Boolean open) {
        if (open == null) {
            faceOpenOverride.remove(face);
        } else {
            faceOpenOverride.put(face, open);
        }
        setChanged();
    }

    /**
     * 有效压强（相变门槛用，§19.27 Stage 2）。带<b>每 tick 记忆</b>：引擎会按规则反复查询，
     * 而密闭判定要查 6 个邻格方块状态，缓存后每 tick 只算一次。
     */
    private long effectivePressureTick = Long.MIN_VALUE;
    private double effectivePressureAtm = 1.0;

    @Override
    public double getEffectivePressureAtm() {
        Level level = getLevel();
        if (level == null) return GasPhysics.ambientAtm();
        long now = level.getGameTime();
        if (now != effectivePressureTick) {
            effectivePressureTick = now;
            effectivePressureAtm = isSealed() ? getIdealPressureAtm() : GasPhysics.ambientAtm();
        }
        return effectivePressureAtm;
    }

    // ==================== 附着件（§19.29） ====================

    /** 一次邻格附着件扫描的结果 */
    private record AttachmentRef(IContainerAttachment attachment, BlockPos pos) {}

    /** 附着件扫描缓存（每 tick 一次）：6 邻格里的 {@link IContainerAttachment} */
    private long attachmentTick = Long.MIN_VALUE;
    private List<AttachmentRef> attachmentCache = List.of();

    protected List<AttachmentRef> attachments() {
        Level level = getLevel();
        if (level == null) return List.of();
        long now = level.getGameTime();
        if (now != attachmentTick) {
            attachmentTick = now;
            List<AttachmentRef> found = new ArrayList<>(6);
            for (Direction d : Direction.values()) {
                BlockPos p = getBlockPos().relative(d);
                if (level.getBlockState(p).getBlock() instanceof IContainerAttachment attachment) {
                    found.add(new AttachmentRef(attachment, p));
                }
            }
            attachmentCache = List.copyOf(found);
        }
        return attachmentCache;
    }

    /** 传热倍率 = 放大效应 × (1 + Σ面积加成) × Π系数倍率（§19.29/§19.30） */
    @Override
    public double getHeatTransferFactor() {
        Level level = getLevel();
        if (level == null) return baseHeatTransferFactor();
        double area = 1.0;
        double coefficient = 1.0;
        for (AttachmentRef ref : attachments()) {
            area += ref.attachment().heatTransferAreaBonus(level, ref.pos());
            coefficient *= ref.attachment().heatTransferFactor(level, ref.pos());
        }
        return baseHeatTransferFactor() * area * coefficient;
    }

    /** 气液传质倍率：所有附着件的因子相乘（默认 1.0） */
    @Override
    public double getGasTransferFactor() {
        Level level = getLevel();
        if (level == null) return 1.0;
        double factor = 1.0;
        for (AttachmentRef ref : attachments()) {
            factor *= ref.attachment().gasTransferFactor(level, ref.pos());
        }
        return factor;
    }

    @Override
    public double getTotalHeatCapacity() {
        return totalHeatCapacity;
    }

    @Override
    public void addThermalEnergy(double joules) {
        if (totalHeatCapacity < 1e-9) return;
        double deltaT = joules / totalHeatCapacity;
        double newTemp = temperature + deltaT;
        double oldTemp = temperature;
        this.temperature = Math.min(Math.max(0, newTemp), getMaxSafeTemperature());
        // 温度变化时重置平衡标记：K 随温度按范特霍夫变化，需重新评估平衡状态，
        // 否则加热/冷却后已达平衡的反应会被永久跳过
        if (Math.abs(this.temperature - oldTemp) > 1e-9) {
            resetBalancedRules();
        }
        setChanged();
        syncToClient();
    }

    @Override
    public Level getLevel() {
        return super.getLevel();
    }

    @Override
    public @NotNull BlockPos getBlockPos() {
        return super.getBlockPos();
    }

    @Override
    public boolean isRemoved() {
        return super.isRemoved();
    }

    // ==================== 内部方法 ====================

    private void recalcHeatCapacity() {
        double sum = 0;
        for (Map.Entry<IonType, Double> entry : contents.entrySet()) {
            IonType ion = entry.getKey();
            double moles = entry.getValue();
            sum += moles * ion.getSpecificHeat();
        }
        this.totalHeatCapacity = sum
                + ChemConfig.BEAKER_BASE_HEAT_CAPACITY.get() * getContainerMaterial().capacityScale();
        if (this.totalHeatCapacity < ChemConfig.MIN_HEAT_CAPACITY.get()) {
            this.totalHeatCapacity = ChemConfig.MIN_HEAT_CAPACITY.get();
        }
    }

    private void syncToClient() {
        if (getLevel() != null && !getLevel().isClientSide) {
            getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * 忽略阈值（mol）：低于此量的物种视为不存在。
     * 与显示精度（%.4f）对齐——反应跑完后留下的浮点残渣会被清理，
     * 而有意义的平衡量（如碳酸根交叉产物 ~0.0015）远高于此、不受影响。
     */
    private static final double NEGLIGIBLE_AMOUNT = 1e-4;

    /**
     * 清理低于忽略阈值的物种（仅剩"已完成反应"的浮点尾巴时，让显示为诚实的 0）
     */
    private void sweepNegligibleSpecies() {
        var it = contents.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue() < NEGLIGIBLE_AMOUNT) {
                it.remove();
            }
        }
    }

    /**
     * 格式化当前内容物，用于调试日志（如 "h_plus=1.0000, co2=0.5000"）
     */
    private String formatContents() {
        if (contents.isEmpty()) return "empty";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<IonType, Double> e : contents.entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(e.getKey().getId().getPath()).append('=')
                    .append(String.format("%.4f", e.getValue()));
        }
        return sb.toString();
    }

    // ==================== 网络同步（核心修复） ====================

    @Override
    public CompoundTag getUpdateTag() {
        // 客户端在加载时调用的更新标签（容器内容物/温度等仍需同步给客户端 HUD）
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        // 客户端接收更新标签时调用，直接加载数据
        load(tag);
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        // 服务端发送更新包
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        // 客户端接收更新包时调用
        if (pkt.getTag() != null) {
            handleUpdateTag(pkt.getTag());
        }
    }

    // ==================== Tick 逻辑 ====================

    public static void tick(Level level, BlockPos pos, BlockState state, BeakerBlockEntity beaker) {
        if (level.isClientSide) return;

        beaker.detectAndApplyHeat(level, pos);

        // 环境换热（牛顿冷却，对称）：高于环境散热、低于环境回温。
        // 使用独立的环境系数（弱于热源），使烧杯有热惯量：可被热源烧热并保持住，
        // 不会瞬间弹回环境温度。修复原"只散热不回温"不对称
        double envTemp = beaker.getEnvironmentTemperature(level, pos);
        double heatDelta = ChemConfig.AMBIENT_HEAT_TRANSFER_COEFFICIENT.get()
                * beaker.getHeatTransferFactor() * (envTemp - beaker.temperature) * 0.05;
        if (Math.abs(heatDelta) > 1e-6) {
            beaker.addThermalEnergy(heatDelta * beaker.getTotalHeatCapacity());
        }

        ReactionEngine.tick(beaker);

        // 开口面气体交换（Phase 9）：敞口容器与上方网格格按浓度差双向交换（逸散/回吸）
        beaker.exchangeGasWithGrid(level, pos);

        // 压力危险（§19.31）：默认关；只有密闭容器才可能超压
        beaker.checkPressureHazard(level, pos);

        // 腐蚀（§19.32）：默认关；由现有 IonTags 驱动
        beaker.applyCorrosion(level, pos);

        // 每 20 tick（1秒）清理忽略阈值下的残渣，并输出烧杯内容物与温度（调试用）
        if (level.getGameTime() % 20 == 0) {
            beaker.sweepNegligibleSpecies();
            Main.LOGGER.debug("[Beaker] {} contents: {} | T={}K",
                    pos, beaker.formatContents(), String.format("%.1f", beaker.temperature));
        }

        // 临界温度警告节流：每 100 tick（5秒）最多输出一次，避免持续刷屏
        if (beaker.temperature >= beaker.getMaxSafeTemperature() * ChemConfig.CRITICAL_TEMPERATURE_RATIO.get()
                && level.getGameTime() % 100 == 0) {
            Main.LOGGER.warn("[Beaker] Beaker at {} is reaching critical temperature! {}K",
                    pos, String.format("%.1f", beaker.temperature));
        }
    }

    private void detectAndApplyHeat(Level level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        Block belowBlock = belowState.getBlock();

        IHeatSource source = null;
        Double heatSourceTemp = null;
        boolean setpointSource = false;

        // ① 设定点型热源（恒温浴/创造热源等，§19.24）：语义是"目标温度"而非"上限"，
        //    因此**双向**生效——热源比容器冷时会从容器吸热（冰浴/干冰/液氮同样可复现）
        BlockEntity belowEntity = level.getBlockEntity(below);
        if (belowEntity instanceof IHeatSource belowSource && belowSource.isHeatSourceActive()) {
            source = belowSource;
            heatSourceTemp = belowSource.getSourceTemperature();
            setpointSource = true;
        } else {
            // ② 燃料型热源（火/岩浆/营火等）：只能单向烤热，温度由燃料天然决定
            heatSourceTemp = HEAT_SOURCE_TEMPS.get(belowBlock);
        }

        // ③ 附着件形态的热源（§19.29 电加热夹套）：贴在容器邻面上的 IHeatSource 从侧面供热。
        //    取"驱动力最强"（|T_源 − T_容器| 最大）的一个。**只有附着件**才从侧面生效，
        //    所以把恒温浴挪到旁边不会突然开始加热（保持既有行为）。
        for (AttachmentRef ref : attachments()) {
            if (!(level.getBlockEntity(ref.pos()) instanceof IHeatSource attached)
                    || !attached.isHeatSourceActive()) {
                continue;
            }
            double attachedTemp = attached.getSourceTemperature();
            if (heatSourceTemp == null
                    || Math.abs(attachedTemp - temperature) > Math.abs(heatSourceTemp - temperature)) {
                source = attached;
                heatSourceTemp = attachedTemp;
                setpointSource = true;
            }
        }

        if (heatSourceTemp == null) return;

        double gap = heatSourceTemp - temperature;
        if (setpointSource ? Math.abs(gap) < 1e-3 : gap <= 0) return;

        // §19.38 十一：改成**导热系数**模型（W/K × 温差 × 时间 = J）。
        // 旧版是"每 tick 吃掉 10% 温差"，与热容无关 ⇒ 容器被瞬间钳到热源温度
        // （作者 2026-10-02 报告：红热烧杯放上室温水浴，瞬间变室温）。
        double conductance = ChemConfig.BEAKER_SOURCE_CONDUCTANCE_W_PER_K.get()
                * getContainerMaterial().transferScale() * getHeatTransferFactor();
        double heatEnergy = conductance * gap * SECONDS_PER_TICK;
        // 热源自己能搬多少功率是上限：浴槽按额定加热功率给（否则容器把它当无限大热库）
        if (source != null) {
            double capWatts = source.maxTransferWatts();
            if (Double.isFinite(capWatts)) {
                double capJoules = capWatts * SECONDS_PER_TICK;
                heatEnergy = Math.copySign(Math.min(Math.abs(heatEnergy), capJoules), heatEnergy);
            }
            // 电加热类按交付热量付费；有介质的设备把热量收进/取出自己的介质（§19.29 / §19.38）
            if (!source.payHeatCost(heatEnergy)) return;
        }
        addThermalEnergy(heatEnergy);
    }

    /**
     * 本容器的材料档位（§6.3 / ③-3）：坩埚与烧杯共用 {@link BeakerBlockEntity}，
     * 材料由方块类型决定（{@code BeakerBlock.getContainerMaterial()}），非化学容器方块回落到玻璃档。
     */
    public com.pha.trainees.chemistry.container.ContainerMaterial getContainerMaterial() {
        // 搪玻璃衬层被腐蚀透后降级为钢基体（§19.32），降级态优先
        if (degradedMaterial != null) return degradedMaterial;
        Block block = getBlockState().getBlock();
        // 声明式解析（§19.30.1）：凡实现 IContainerMaterialProvider 的方块都自报材料；
        // 否则回落玻璃档（非化学容器方块）。
        return block instanceof com.pha.trainees.chemistry.container.IContainerMaterialProvider provider
                ? provider.getContainerMaterial()
                : com.pha.trainees.chemistry.container.ContainerMaterial.GLASS;
    }

    /** 本容器的安全温度上限：由材料决定（玻璃 773 K → 陶瓷 2273 K），而非全局配置。 */
    public double getMaxSafeTemperature() {
        return getContainerMaterial().maxSafeTemperatureK();
    }

    /** 本容器的安全压力上限（atm，§19.31）：由材料档位决定；反应釜再乘釜盖系数。 */
    @Override
    public double getMaxSafePressureAtm() {
        return getContainerMaterial().maxSafePressureAtm();
    }

    // ==================== 压力危险（§19.31，默认关） ====================

    /**
     * 每 tick 检查压力危险。<b>只有密闭容器才可能超压</b>——敞口容器恒为环境压（§19.27），
     * 所以"敞口烧杯里攒氯气"永远不会炸，这一条保证了既有玩法不受影响。
     *
     * <ul>
     *   <li>压力 ≥ 耐压上限：{@code rupture} 打开 → <b>破裂</b>；否则只<b>泄压</b>（封印"漏了"）。</li>
     *   <li>压力 ≥ 预警比例：每 3 秒向附近玩家提示一次。</li>
     * </ul>
     *
     * <p>⚠ <b>负压不施加后果</b>：我们的模型里"气相很少的密闭容器"就是真空（§19.27 的简化），
     * 于是负压是密闭容器的<b>常态</b>，施加内爆惩罚会在每次密封时触发。负压仍然计算与显示。</p>
     */
    protected void checkPressureHazard(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        if (!ChemConfig.PRESSURE_HAZARD_ENABLED.get()) return;
        if (!isSealed()) return;
        double rating = getMaxSafePressureAtm();
        if (!Double.isFinite(rating) || rating <= 0) return;
        double pressure = getIdealPressureAtm();
        double ratio = pressure / rating;

        if (ratio >= 1.0) {
            if (ChemConfig.PRESSURE_RUPTURE_ENABLED.get()) {
                ruptureVessel(level, pos, pressure, rating);
            } else {
                relievePressure(level, pos, rating);
                if (level.getGameTime() % 60 == 0) {
                    notifyNearby(level, pos, "message.trainees.pressure.relieved",
                            String.format("%.2f", pressure), String.format("%.2f", rating));
                }
            }
        } else if (ratio >= ChemConfig.PRESSURE_WARNING_RATIO.get() && level.getGameTime() % 60 == 0) {
            notifyNearby(level, pos, "message.trainees.pressure.warning",
                    String.format("%.2f", pressure), String.format("%.2f", rating));
        }
    }

    /** 泄压：把气相摩尔数降到"耐压上限对应"的容纳量，多出来的按组成比例抛到正上方网格格。 */
    private void relievePressure(Level level, BlockPos pos, double targetAtm) {
        double nGas = getGasMoles();
        if (nGas <= 1e-9) return;
        double nTarget = GasPhysics.molesAtAmbient(getVolume(), getTemperature(), targetAtm);
        double excess = nGas - nTarget;
        if (excess <= 1e-9) return;
        double share = excess / nGas;
        Map<IonType, Double> vented = new HashMap<>();
        for (IonType ion : new ArrayList<>(contents.keySet())) {
            if (ion.getPhase() != Phase.GAS) continue;
            double removed = removeIon(ion, contents.getOrDefault(ion, 0.0) * share, false);
            if (removed > 0) vented.merge(ion, removed, Double::sum);
        }
        if (!vented.isEmpty() && level instanceof ServerLevel serverLevel) {
            GasGridManager manager = GasGridManager.get(serverLevel);
            for (Map.Entry<IonType, Double> e : vented.entrySet()) {
                manager.addGas(serverLevel, pos.above(), e.getKey(), e.getValue());
            }
        }
        setChanged();
    }

    /** 破裂：内容物作为物质样本掉落（**不销毁**）→ 伤害附近生物 → 毁掉容器本体。 */
    private void ruptureVessel(Level level, BlockPos pos, double pressure, double rating) {
        destroyVessel(level, pos, String.format("ruptured: %.2f atm > rating %.2f atm", pressure, rating));
    }

    /**
     * 毁掉容器但保留内容物（§19.31 压力破裂 / §19.32 腐蚀穿共用）：
     * 内容物作为物质样本掉落 → 4 格内玩家受伤 → 容器本体作为掉落物被破坏。
     */
    private void destroyVessel(Level level, BlockPos pos, String logReason) {
        Map<IonType, Double> spill = new HashMap<>(contents);
        contents.clear();
        recalcHeatCapacity();
        if (!spill.isEmpty()) {
            ItemStack sample = SubstanceItem.fromComposition(spill);
            if (!sample.isEmpty()) {
                Block.popResource(level, pos, sample);
            }
        }
        float damage = (float) Math.max(0.0, ChemConfig.PRESSURE_HAZARD_DAMAGE.get());
        if (damage > 0) {
            for (Player player : level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(4.0))) {
                player.hurt(level.damageSources().generic(), damage);
            }
        }
        Main.LOGGER.warn("[Vessel] {} destroyed at {}: {}", getClass().getSimpleName(), pos, logReason);
        level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.0f, 1.2f);
        level.destroyBlock(pos, true);
    }

    /** 向附近玩家发一条提示（节流由调用方控制）。 */
    private void notifyNearby(Level level, BlockPos pos, String key, Object... args) {
        Component message = Component.translatable(key, args);
        for (Player player : level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(8.0))) {
            player.displayClientMessage(message, true);
        }
    }

    // ==================== 腐蚀（§19.32，默认关） ====================

    /** 腐蚀度 0~1（持久化） */
    private double corrosion = 0.0;
    /** 搪玻璃衬层被吃透后降级到的材质；null = 用方块声明的材质 */
    private ContainerMaterial degradedMaterial = null;
    /** 上次腐蚀推进的游戏刻（用于算真实 dt，与 tick 节流无关） */
    private long corrosionLastTick = Long.MIN_VALUE;

    @Override
    public double getCorrosionLevel() {
        return corrosion;
    }

    /** 容器当前有没有腐蚀损伤（需要维护）——给"衬层修复/重搪"判断用。 */
    public boolean hasCorrosionDamage() {
        return degradedMaterial != null || corrosion > 1e-9;
    }

    /**
     * 衬层修复 / 重搪（§19.32 的"本批未做"项）。
     *
     * <p>口径：把腐蚀度清零、并把**降级材质复位**——也就是"把搪玻璃重新烧上去"，
     * 材质恢复成方块声明的那个（搪玻璃釜 → 搪玻璃、钢釜 → 钢）。</p>
     *
     * <p><b>必须先清空容器</b>：往一釜酸里补衬层是不合物理的（现实里也必须先清釜、再重搪）。
     * 判据用已有的 {@code getTotalMoles()}，不需要新接口。</p>
     *
     * @return true = 确实修了（有损伤）；false = 容器里还有东西，先清空
     */
    public boolean restoreLining() {
        if (getTotalMoles() > 1e-9) return false;
        degradedMaterial = null;
        corrosion = 0.0;
        corrosionLastTick = Long.MIN_VALUE;   // 重新开始计（否则会把"修复前那段 dt"也算进去）
        setChanged();
        // 材质变了 → 热容/安全温度/耐压都跟着变，让客户端也刷新
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        return true;
    }

    @Override
    public String getMaterialId() {
        return getContainerMaterial().id();
    }

    /**
     * 每 tick 的腐蚀推进（§19.32，默认关）。<b>驱动数据全部来自现有 {@code IonTags}，不新增物种。</b>
     *
     * <ul>
     *   <li>载荷 = Σ(各物种真实浓度 × 材质对该侵蚀类型的敏感度)；</li>
     *   <li><b>钢 + 酸走真反应</b>：Fe + 2H⁺ → Fe²⁺ + H₂↑ —— 真消耗 H⁺、真溶出 Fe²⁺、真冒氢气，
     *       而且**受可用酸量限制**（没酸就停，符合"钝化"直觉）；</li>
     *   <li>其余材质/侵蚀类型只累计腐蚀度（简化：不把被腐蚀的材料写进内容物）；</li>
     *   <li>到 1.0：搪玻璃 → <b>降级为钢基体</b>；其余 → 破裂（复用压力破裂那条路径）。</li>
     * </ul>
     */
    protected void applyCorrosion(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        if (!ModConfig.COMMON.corrosionEnabled.get()) return;

        long now = level.getGameTime();
        double dtSeconds = corrosionLastTick == Long.MIN_VALUE
                ? 0.05
                : Math.min(1.0, (now - corrosionLastTick) * 0.05);
        corrosionLastTick = now;
        if (dtSeconds <= 0) return;

        ContainerMaterial material = getContainerMaterial();
        double volumeL = Math.max(getVolume(), 1e-9);

        // ---- 附着件腐蚀（§19.32 的"本批未做"项）：泡在同一釜里，但按**各自的材质**重算载荷 ----
        // 刻意放在容器自己那套判断之前：玻璃容器对 Cl⁻ 免疫，而钢搅拌轴会点蚀，
        // 所以"容器载荷为 0"并不代表附着件安全。
        for (AttachmentRef ref : attachments()) {
            if (!(level.getBlockEntity(ref.pos())
                    instanceof com.pha.trainees.chemistry.container.ICorrodibleAttachment corrodible)) {
                continue;   // 没实现就永不被腐蚀（默认安全）
            }
            ContainerMaterial attachmentMaterial = corrodible.attachmentMaterial();
            double attachmentLoad = 0;
            for (Map.Entry<IonType, Double> e : contents.entrySet()) {
                double concentration = e.getValue() / volumeL;
                if (concentration <= 0) continue;
                for (ErosionType type : ErosionType.of(e.getKey().getTags())) {
                    attachmentLoad += CorrosionMath.load(attachmentMaterial, type, concentration);
                }
            }
            if (attachmentLoad <= 0) continue;
            double dAttachment = CorrosionMath.ratePerSecond(attachmentLoad,
                    ModConfig.COMMON.corrosionBaseRate.get(), getTemperature()) * dtSeconds;
            if (dAttachment > 0) {
                corrodible.applyAttachmentCorrosion(level, ref.pos(), dAttachment);
            }
        }

        double totalLoad = 0;
        for (Map.Entry<IonType, Double> e : contents.entrySet()) {
            double concentration = e.getValue() / volumeL;   // mol/L（各相都按真实浓度，接触即侵蚀）
            if (concentration <= 0) continue;
            for (ErosionType type : ErosionType.of(e.getKey().getTags())) {
                totalLoad += CorrosionMath.load(material, type, concentration);
            }
        }
        if (totalLoad <= 0) return;

        double dFraction = CorrosionMath.ratePerSecond(
                totalLoad, ModConfig.COMMON.corrosionBaseRate.get(), getTemperature()) * dtSeconds;
        if (dFraction <= 0) return;

        double applied;
        if (material == ContainerMaterial.METAL) {
            // 钢溶出：受可用 H⁺ 限制
            double vesselMoles = Math.max(1e-9,
                    ModConfig.COMMON.corrosionMaterialMolesPerLiter.get() * volumeL);
            double wantedSteel = dFraction * vesselMoles;
            double hPlus = getAmount(ModIons.H_plus);
            double feasible = Math.min(wantedSteel, hPlus / 2.0);
            if (feasible <= 1e-9) return;                 // 没酸就不腐蚀
            removeIon(ModIons.H_plus, feasible * 2.0, false);
            addIon(ModIons.Fe_2, feasible, false);
            addIon(ModIons.H2, feasible, false);
            applied = feasible / vesselMoles;
        } else {
            applied = dFraction;
        }

        corrosion = Math.min(1.0, corrosion + applied);
        setChanged();
        if (corrosion >= 1.0) {
            onCorrosionComplete(level, pos);
        } else if (corrosion >= ModConfig.COMMON.corrosionWarningThreshold.get()
                && now % 60 == 0) {
            notifyNearby(level, pos, "message.trainees.corrosion.warning",
                    String.format("%.1f", corrosion * 100),
                    Component.translatable("trainees.material." + getMaterialId()));
        }
    }

    /** 腐蚀透顶：搪玻璃 → 降级为钢基体（真实失效模式）；其余材质 → 破裂。 */
    private void onCorrosionComplete(Level level, BlockPos pos) {
        ContainerMaterial degraded = CorrosionTable.degradedMaterial(getContainerMaterial());
        if (degraded != null) {
            degradedMaterial = degraded;
            corrosion = 0.0;                  // 换了新基体，重新开始计
            setChanged();
            notifyNearby(level, pos, "message.trainees.corrosion.lining_gone",
                    Component.translatable("trainees.material." + degraded.id()));
            return;
        }
        notifyNearby(level, pos, "message.trainees.corrosion.destroyed");
        destroyVessel(level, pos, "corroded through");
    }

    /**
     * 直接设定容积（L），<b>不带任何同步/标记副作用</b>——供子类在构造里设定自己的默认容积
     * （反应釜 10 L）。存档里的 volume 仍按原路径读写。
     */
    protected void setVolumeInternal(double volumeLiters) {
        if (volumeLiters > 0) {
            this.volume = volumeLiters;
        }
    }

    /**
     * 尺寸放大效应（§19.30）：真实反应釜的夹套换热面积 ∝ L²、容积 ∝ L³，
     * 所以<b>单位体积的传热能力 ∝ V^(−1/3)</b>——这正是大釜必须靠搅拌 + 内盘管 + 夹套层数补的原因。
     * 烧杯（1 L）是参考点，返回 1.0；反应釜覆写为 {@code (1/volume)^(1/3)}。
     */
    protected double baseHeatTransferFactor() {
        return 1.0;
    }

    /** 容量上限的容积倍率：烧杯 = 1.0，10 L 反应釜 = 10（批次更大） */
    protected double volumeCapacityScale() {
        return volume / DEFAULT_VOLUME;
    }

    @Override
    public double getMaxTotalMoles() {
        return ChemConfig.MAX_TOTAL_MOLES.get() * volumeCapacityScale();
    }

    @Override
    public double getMaxMolesPerComponent() {
        return ChemConfig.MAX_MOLES_PER_COMPONENT.get() * volumeCapacityScale();
    }

    // ==================== 残留与清洗（§19.30.2） ====================

    /**
     * 残留地板（mol）：取样/出料**取不走**的那部分——死体积与挂壁液膜。
     * 烧杯返回 0（行为不变）；反应釜按容积给，且装了密封釜头时更高（死角冲不到）。
     */
    public double residueFloorMoles() {
        return 0.0;
    }

    /** 清洗后能达到的地板：能擦净的（可拆盖）= 0；全焊 = {@link #residueFloorMoles()}。 */
    public double cleanableFloorMoles() {
        return 0.0;
    }

    /**
     * 清洗容器（§19.30.2）：把内容物降到"清洗地板"。
     *
     * <p>可拆盖能擦到一点不剩；全焊（密封釜头）只能冲掉浮的，死角里的永远留在地板上——
     * 于是全焊容器会**持续把上一批的残留带进下一批**，撞上规格的 LIMIT 规则就会判 REJECTED。</p>
     *
     * @return 本次清掉的摩尔数
     */
    public double cleanVessel() {
        double before = getTotalMoles();
        if (before <= 1e-9) return 0;
        double floor = Math.max(0.0, cleanableFloorMoles());
        if (floor <= 1e-9) {
            contents.clear();
        } else if (before > floor) {
            double scale = floor / before;
            for (IonType ion : new ArrayList<>(contents.keySet())) {
                contents.put(ion, contents.getOrDefault(ion, 0.0) * scale);
            }
        }
        recalcHeatCapacity();
        setChanged();
        syncToClient();
        return before - getTotalMoles();
    }

    /**
     * 逐面气体交换（#10 / §19.27）。两件事<b>分开</b>，不要再混成一个"泄漏"：
     * <ol>
     *   <li><b>分压扩散</b>（逐面、双向、持续）：每个相通的面与其外侧网格格按浓度差交换，
     *       沿用历史的"格雷姆 × 温度"系数、单源 50% 封顶与缓冲纪律。烧杯只有上口，
     *       所以行为与历史的顶面单片模型逐字一致。</li>
     *   <li><b>超压喷出</b>（瞬时、组成不变）：容器上一 tick 还是密闭、本 tick 刚相通时，
     *       若气相摩尔数超过"环境压下的容纳量"，把超出部分按组成比例整体喷出。
     *       这才是"压差决定方向"的可感形态：盖住 → 憋压 → 揭盖 → 喷一股。</li>
     * </ol>
     * <b>敞口容器不喷</b>：有意保留"烧杯里攒气"的既有玩法（敞口容器理想总压可远超 1 atm，
     * §19.27 决策 A）。盖子/阀门将来走 {@link #setFaceOpenOverride}，不必改这里。
     */
    protected void exchangeGasWithGrid(Level level, BlockPos pos) {
        if (!ChemConfig.GAS_CONTAINER_LEAK_ENABLED.get()) return;
        if (level.isClientSide) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        Set<Direction> openFaces = getOpenFaces();
        if (openFaces.isEmpty()) {
            // 密闭：不做任何交换。压强在此累积，等"揭盖"那一刻由超压喷出释放。
            wasSealed = true;
            return;
        }
        boolean justOpened = wasSealed;
        wasSealed = false;

        GasGridManager manager = GasGridManager.get(serverLevel);
        if (justOpened) {
            ventOverpressure(serverLevel, manager, pos, openFaces);
        }

        double temp = getTemperature();
        double volL = Math.max(getVolume(), 0.001);
        double rateBase = ChemConfig.GAS_CONTAINER_LEAK_RATE.get() * getGasTransferFactor();
        double maxPerComponent = getMaxMolesPerComponent();
        double faceShare = 1.0 / openFaces.size();
        boolean changed = false;

        for (Direction face : openFaces) {
            BlockPos outside = pos.relative(face);
            GasMixture cell = manager.get(outside);

            // 1) 向外逸散（容器中气相；先入该面的缓冲，累积足量再入网格，避免微量尾巴格）
            for (Map.Entry<IonType, Double> e : contents.entrySet()) {
                if (e.getKey().getPhase() != Phase.GAS) continue;
                double n = e.getValue();
                if (n <= 1e-9) continue;
                double cIn = n / volL; // mol/L
                double cOut = (cell == null ? 0 : cell.getAmount(e.getKey())) / GasPhysics.CELL_VOLUME_L;
                if (cIn <= cOut) continue;
                double rate = rateBase
                        * Math.sqrt(29.0 / Math.max(1, e.getKey().getMolarMass()))
                        * Math.sqrt(Math.max(1, temp) / 293.0);
                double move = rate * (cIn - cOut) * volL * faceShare; // → mol
                move = Math.min(move, n * 0.5);
                if (move > 1e-12) {
                    removeIon(e.getKey(), move, false); // 容器持续扣减（敞口会漏光）
                    bufferLeak(face, e.getKey(), move);
                    changed = true;
                }
            }

            // 2) 向内回吸（收集：外部浓度高于容器，且容器未满）
            if (cell == null) continue;
            double totalIn = getTotalMoles();
            double totalMax = getMaxTotalMoles();
            for (Map.Entry<IonType, Double> e : new HashMap<>(cell.getContents()).entrySet()) {
                if (e.getKey().getPhase() != Phase.GAS) continue;
                double nOut = e.getValue();
                if (nOut <= 1e-9) continue;
                double cOut = nOut / GasPhysics.CELL_VOLUME_L; // mol/L
                double cIn = getAmount(e.getKey()) / volL; // mol/L
                if (cOut <= cIn) continue;
                double rate = rateBase
                        * Math.sqrt(29.0 / Math.max(1, e.getKey().getMolarMass()))
                        * Math.sqrt(Math.max(1, temp) / 293.0);
                double move = rate * (cOut - cIn) * volL * faceShare; // → mol（进入容器）
                // 容量约束
                move = Math.min(move, maxPerComponent - getAmount(e.getKey()));
                move = Math.min(move, totalMax - totalIn);
                move = Math.min(move, nOut * 0.5);
                if (move > 1e-12) {
                    manager.removeGas(serverLevel, outside, e.getKey(), move);
                    addIon(e.getKey(), move, false);
                    totalIn += move;
                    changed = true;
                }
            }
        }

        // 3) 缓冲足量 → 一次性抛给网格（消散阈值 GasGridManager.dissolveThreshold()）
        if (flushLeakBuffers(serverLevel, manager, pos)) {
            changed = true;
        }

        if (changed) {
            setChanged();
        }
    }

    /**
     * 超压喷出（§19.27 决策 A）：把"超出环境压容纳量"的气相按<b>组成比例</b>整体推出。
     * 组成不变＝整体流动，与"按分压渗"的扩散是两回事——这正是压差驱动的部分。
     * 单 tick 上限 = 超出量 × {@code pressureVentRate}，且不超过气相总量的一半（沿用数值稳定纪律）；
     * 各开放面均分，缓冲后抛入各自外侧的格。
     */
    private void ventOverpressure(ServerLevel level, GasGridManager manager, BlockPos pos, Set<Direction> openFaces) {
        double nGas = getGasMoles();
        if (nGas <= 1e-9) return;
        double volL = Math.max(getVolume(), 0.001);
        double excess = nGas - GasPhysics.molesAtAmbient(volL, getTemperature());
        if (excess <= 1e-9) return;
        double vent = Math.min(excess * ChemConfig.PRESSURE_VENT_RATE.get(), nGas * 0.5);
        if (vent <= 1e-9) return;
        double share = vent / nGas / openFaces.size();
        for (Direction face : openFaces) {
            for (Map.Entry<IonType, Double> e : contents.entrySet()) {
                if (e.getKey().getPhase() != Phase.GAS) continue;
                double move = e.getValue() * share;
                if (move <= 1e-12) continue;
                double removed = removeIon(e.getKey(), move, false);
                if (removed > 0) bufferLeak(face, e.getKey(), removed);
            }
        }
    }

    /** 逐面逸散缓冲 */
    private void bufferLeak(Direction face, IonType ion, double moles) {
        gridLeakBuffers.computeIfAbsent(face, k -> new HashMap<>()).merge(ion, moles, Double::sum);
    }

    /** 缓冲足量者抛入该面外侧的格；返回是否有气体被抛入网格 */
    private boolean flushLeakBuffers(ServerLevel level, GasGridManager manager, BlockPos pos) {
        if (gridLeakBuffers.isEmpty()) return false;
        double threshold = GasGridManager.dissolveThreshold();
        boolean flushed = false;
        var faceIt = gridLeakBuffers.entrySet().iterator();
        while (faceIt.hasNext()) {
            var faceEntry = faceIt.next();
            BlockPos target = pos.relative(faceEntry.getKey());
            Map<IonType, Double> buffer = faceEntry.getValue();
            var it = buffer.entrySet().iterator();
            while (it.hasNext()) {
                var b = it.next();
                if (b.getValue() >= threshold) {
                    manager.addGas(level, target, b.getKey(), b.getValue());
                    it.remove();
                    flushed = true;
                }
            }
            if (buffer.isEmpty()) faceIt.remove();
        }
        return flushed;
    }

    /** 环境温度（K）：基准 293K，随高度递减 0.6K/100m（子类 tick 复用） */
    protected double getEnvironmentTemperature(Level level, BlockPos pos) {
        double base = ChemConfig.ENVIRONMENT_TEMPERATURE_BASE.get();
        double yFactor = Math.max(0, (pos.getY() - 64) / 100.0) * ChemConfig.ENVIRONMENT_TEMPERATURE_LAPSE_RATE.get();
        // 钳制非负：极端高 Y 时递减项不应把环境温度推到负值
        return Math.max(0, base - yFactor);
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        tag.putDouble("temperature", temperature);
        tag.putDouble("volume", volume);
        tag.putDouble("Corrosion", corrosion);
        if (degradedMaterial != null) {
            tag.putString("DegradedMaterial", degradedMaterial.id());
        }

        ListTag contentsList = new ListTag();
        for (Map.Entry<IonType, Double> entry : contents.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("id", entry.getKey().getId().toString());
            entryTag.putDouble("moles", entry.getValue());
            contentsList.add(entryTag);
        }
        tag.put("contents", contentsList);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        temperature = tag.getDouble("temperature");
        if (temperature <= 0) temperature = DEFAULT_TEMPERATURE;

        volume = tag.getDouble("volume");
        if (volume <= 0) volume = DEFAULT_VOLUME;

        contents.clear();
        ListTag contentsList = tag.getList("contents", Tag.TAG_COMPOUND);

        // §19.32 腐蚀状态
        corrosion = tag.getDouble("Corrosion");
        degradedMaterial = null;
        if (tag.contains("DegradedMaterial")) {
            for (ContainerMaterial candidate : ContainerMaterial.values()) {
                if (candidate.id().equals(tag.getString("DegradedMaterial"))) {
                    degradedMaterial = candidate;
                    break;
                }
            }
        }

        for (int i = 0; i < contentsList.size(); i++) {
            CompoundTag entryTag = contentsList.getCompound(i);
            String idStr = entryTag.getString("id");
            double moles = entryTag.getDouble("moles");
            if (moles > 1e-9) {
                ResourceLocation id = ResourceLocation.tryParse(idStr);
                if (id != null) {
                    IonType ion = ModIons.getById(id);
                    if (ion != null) {
                        contents.put(ion, moles);
                    }
                }
            }
        }

        recalcHeatCapacity();
    }

    @Override
    public void markRuleBalanced(ReactionRule rule) {
        balancedRules.add(rule);
    }

    @Override
    public boolean isRuleBalanced(ReactionRule rule) {
        return balancedRules.contains(rule);
    }

    @Override
    public void resetBalancedRules() {
        if (!balancedRules.isEmpty()) {
            balancedRules.clear();
            Main.LOGGER.debug("[Beaker] Balanced rules reset due to container change");
        }
    }

    // ==================== IFluidHandler（Forge 流体桥接，开发计划 §17.2） ====================

    private final LazyOptional<IFluidHandler> fluidHandler = LazyOptional.of(() -> this);

    /** 1 mol 对应的 mB：1 桶(1000mB) = BUCKET_TO_MOL_WATER mol（严格映射） */
    private static double mbPerMol() {
        return 1000.0 / ChemConfig.BUCKET_TO_MOL_WATER.get();
    }

    @Override
    public int getTanks() { return 1; }

    @Override
    public FluidStack getFluidInTank(int tank) {
        IonType best = null;
        double bestMoles = 0;
        for (Map.Entry<IonType, Double> e : contents.entrySet()) {
            if (FluidIonMapper.getFluidForIon(e.getKey()) != null && e.getValue() > bestMoles) {
                best = e.getKey();
                bestMoles = e.getValue();
            }
        }
        if (best == null) return FluidStack.EMPTY;
        return new FluidStack(FluidIonMapper.getFluidForIon(best), (int) (bestMoles * mbPerMol()));
    }

    @Override
    public int getTankCapacity(int tank) {
        return (int) (getMaxMolesPerComponent() * mbPerMol());
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return FluidIonMapper.getIonForFluid(stack.getFluid()) != null;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return 0;
        IonType ion = FluidIonMapper.getIonForFluid(resource.getFluid());
        if (ion == null) return 0;
        double maxAdd = getMaxMolesPerComponent() - getAmount(ion);
        double toAdd = Math.min(resource.getAmount() / mbPerMol(), maxAdd);
        if (toAdd <= 0) return 0;
        if (action == FluidAction.EXECUTE) {
            addIon(ion, toAdd);
        }
        return (int) (toAdd * mbPerMol());
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return FluidStack.EMPTY;
        IonType ion = FluidIonMapper.getIonForFluid(resource.getFluid());
        if (ion == null) return FluidStack.EMPTY;
        int toDrain = (int) Math.min(resource.getAmount(), getAmount(ion) * mbPerMol());
        if (toDrain <= 0) return FluidStack.EMPTY;
        if (action == FluidAction.EXECUTE) {
            removeIon(ion, toDrain / mbPerMol());
        }
        return new FluidStack(resource.getFluid(), toDrain);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        IonType best = null;
        double bestMoles = 0;
        for (Map.Entry<IonType, Double> e : contents.entrySet()) {
            if (FluidIonMapper.getFluidForIon(e.getKey()) != null && e.getValue() > bestMoles) {
                best = e.getKey();
                bestMoles = e.getValue();
            }
        }
        if (best == null) return FluidStack.EMPTY;
        return drain(new FluidStack(FluidIonMapper.getFluidForIon(best), maxDrain), action);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return fluidHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidHandler.invalidate();
    }

    // ==================== 失败诊断记录（§19.6/19.7） ====================

    /** 容器侧保留的最近失败记录条数（仅运行时，不存 NBT） */
    private static final int MAX_RECENT_FAILURES = 10;

    private final ArrayDeque<ReactionFailure> recentFailures = new ArrayDeque<>();

    @Override
    public void recordFailure(ReactionFailure failure) {
        if (failure == null) return;
        // 仅保留运行时记录供分析仪诊断读取；不同步客户端、不触发更新包（§19.14：HUD 失败提示已移除）
        synchronized (recentFailures) {
            if (recentFailures.size() >= MAX_RECENT_FAILURES) {
                recentFailures.removeFirst();
            }
            recentFailures.addLast(failure);
        }
    }

    @Override
    public List<ReactionFailure> getRecentFailures() {
        synchronized (recentFailures) {
            return new ArrayList<>(recentFailures);
        }
    }

    // ==================== 实际执行记录（§19.7 决策 2c） ====================

    /** 容器侧保留的最近执行记录条数（仅运行时，不存 NBT）；§19.14 点 3：扩到 20 条 */
    private static final int MAX_RECENT_EXECUTED = 20;

    private final ArrayDeque<ExecutedRule> recentExecuted = new ArrayDeque<>();

    @Override
    public void recordExecutedRule(ExecutedRule record) {
        if (record == null) return;
        synchronized (recentExecuted) {
            if (recentExecuted.size() >= MAX_RECENT_EXECUTED) {
                recentExecuted.removeFirst();
            }
            recentExecuted.addLast(record);
        }
    }

    @Override
    public List<ExecutedRule> getRecentExecutedRules() {
        synchronized (recentExecuted) {
            return new ArrayList<>(recentExecuted);
        }
    }
}