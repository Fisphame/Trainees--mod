package com.pha.trainees.chemistry.blockentity;

import com.pha.trainees.chemistry.block.ElectrolysisCellBlock;
import com.pha.trainees.chemistry.engine.ReactionEngine;
import com.pha.trainees.chemistry.item.SubstanceItem;
import com.pha.trainees.chemistry.particle.IonType;
import com.pha.trainees.chemistry.reaction.ReactionRule;
import com.pha.trainees.chemistry.report.ReportLine;
import com.pha.trainees.config.ChemConfig;
import com.pha.trainees.registry.ModChemistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 电解槽方块实体（§19.10 Step 3：产物分拣 + 隔膜纯度）。
 * - 继承烧杯容器能力（contents/温度/热力学/诊断/预测）；
 * - 3 槽：0=膜槽（放隔膜组件）、1=输出A、2=输出B；
 * - 产物分拣：电解规则（electricalWorkPerMol>0）的产物按 阴极/阳极 分类积累，
 *   有膜 → 分侧纯输出（A=阳极侧、B=阴极侧）；无膜 → 混合输出走 A；
 * - 打包：每累计满 1 mol 生成一个 SubstanceItem（成分按当前比例切片，同成分可堆叠）；
 * - 输出槽放不下 → 本 tick 停产（类似熔炉槽满熄火）。
 */
public class ElectrolysisCellBlockEntity extends BeakerBlockEntity {

    /** 单面 FE 缓冲上限（1 FE = 1 J） */
    public static final int ENERGY_CAPACITY = 10000;
    /** 打包阈值（mol）：累计满 1 mol 生成一个物质基类物品 */
    public static final double PACK_MOLES = 1.0;
    /** 单侧积累上限（mol），防止槽满停产前无限囤积 */
    public static final double MAX_ACC_MOLES = 64.0;

    // ===== 工作电压档位（§19.11）：1~5 档 → 电压倍率 = 理论分解电压 × factor =====
    // 档位语义：1=低速(理论工作点,温和) 2=标准(近热中性) 3=快速 4=极速 5=过载(最快最烫最耗)
    // 档名文案已迁至语言键 trainees.voltage.1..5（§19.16 本地化），不再用代码常量
    public static final double[] VOLTAGE_FACTORS = {1.0, 1.2, 1.5, 1.8, 2.2};
    public static final int MAX_VOLTAGE_LEVEL = VOLTAGE_FACTORS.length;
    private int voltageLevel = 2; // 默认标准档

    public int getVoltageLevel() {
        return voltageLevel;
    }

    public void cycleVoltageLevel() {
        voltageLevel = voltageLevel % MAX_VOLTAGE_LEVEL + 1;
        setChanged();
    }

    @Override
    public double getVoltageFactor() {
        return VOLTAGE_FACTORS[Math.max(0, Math.min(MAX_VOLTAGE_LEVEL - 1, voltageLevel - 1))];
    }

    public static final int SLOT_MEMBRANE = 0;
    public static final int SLOT_OUT_A = 1;
    public static final int SLOT_OUT_B = 2;

    private int anodeEnergy = 0;
    private int cathodeEnergy = 0;

    // 产物积累（运行时 + NBT）：阴极侧（还原产物 H₂/金属）与阳极侧（氧化产物 O₂/Cl₂）
    private final Map<IonType, Double> cathodeAcc = new HashMap<>();
    private final Map<IonType, Double> anodeAcc = new HashMap<>();

    private final ItemStackHandler inventory = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> inventory);
    private final CellEnergyStorage anodeStorage = new CellEnergyStorage(true);
    private final CellEnergyStorage cathodeStorage = new CellEnergyStorage(false);
    private final LazyOptional<IEnergyStorage> anodeCap = LazyOptional.of(() -> anodeStorage);
    private final LazyOptional<IEnergyStorage> cathodeCap = LazyOptional.of(() -> cathodeStorage);

    public ElectrolysisCellBlockEntity(BlockPos pos, BlockState state) {
        super(ModChemistry.ModChemistryBlockEntities.ELECTROLYSIS_CELL.get(), pos, state);
    }

    public ElectrolysisCellBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ==================== 产物分拣（§19.10 Step 3） ====================

    @Override
    public boolean onProductGenerated(ReactionRule rule, IonType ion, double moles) {
        // 只分拣电解规则的产物；其余（非电解副反应）按默认进 contents
        if (rule.getElectricalWorkPerMol() <= 0) return false;
        Side side = classifyProduct(ion);
        if (side == Side.UNKNOWN) return false;
        // 有膜：阳极产物 → 阳极积累、阴极产物 → 阴极积累（纯分离）；
        // 无膜：不分侧，全部进阳极积累（打包时作为混合产物输出）
        Map<IonType, Double> acc = hasMembrane()
                ? (side == Side.CATHODE ? cathodeAcc : anodeAcc)
                : anodeAcc;
        acc.merge(ion, moles, Double::sum);
        // 保护上限：防止槽长期满时无限囤积
        if (countAccMoles(acc) > MAX_ACC_MOLES) {
            // 超过上限则回退进 contents（不丢弃物质）
            removeFromAcc(acc, ion, moles);
            return false;
        }
        setChanged();
        return true;
    }

    private void removeFromAcc(Map<IonType, Double> acc, IonType ion, double moles) {
        acc.computeIfPresent(ion, (k, v) -> v - moles <= 1e-9 ? null : v - moles);
    }

    /** 膜槽非空 = 有隔膜（分离两室） */
    public boolean hasMembrane() {
        return !inventory.getStackInSlot(SLOT_MEMBRANE).isEmpty();
    }

    /** 写入膜槽（交互用） */
    public void setMembraneStack(ItemStack stack) {
        inventory.setStackInSlot(SLOT_MEMBRANE, stack.copyWithCount(1));
        setChanged();
    }

    /** 读取膜槽（交互用） */
    public ItemStack getMembraneStack() {
        return inventory.getStackInSlot(SLOT_MEMBRANE).copy();
    }

    private enum Side { CATHODE, ANODE, UNKNOWN }

    /** 产物属于哪个电极（现实：阴极=还原产物 H₂/金属，阳极=氧化产物 O₂/Cl₂） */
    private static Side classifyProduct(IonType ion) {
        String id = ion.getId().getPath();
        return switch (id) {
            // 阴极（还原）
            case "h2", "na", "cu", "fe" -> Side.CATHODE;
            // 阳极（氧化）
            case "o2", "cl2" -> Side.ANODE;
            default -> Side.UNKNOWN;
        };
    }

    /**
     * 把积累满 1 mol 的产物打包成 SubstanceItem（成分按当前比例切片，同成分可堆叠）。
     * 有膜：阳极→输出A、阴极→输出B；无膜：混合产物→输出A。
     */
    public void tryPackageProducts(Level level) {
        if (level.isClientSide) return;
        if (hasMembrane()) {
            packageAcc(anodeAcc, SLOT_OUT_A);
            packageAcc(cathodeAcc, SLOT_OUT_B);
        } else {
            packageAcc(anodeAcc, SLOT_OUT_A);
        }
    }

    /** 输出槽阻塞（无法接收任何新产物物品）时返回 true，用于停产判定 */
    public boolean isOutputBlocked() {
        if (hasMembrane()) {
            return !canInsert(SLOT_OUT_A) || !canInsert(SLOT_OUT_B);
        }
        return !canInsert(SLOT_OUT_A);
    }

    private boolean canInsert(int slot) {
        ItemStack existing = inventory.getStackInSlot(slot);
        if (existing.isEmpty()) return true;
        if (existing.getCount() >= existing.getMaxStackSize()) return false;
        // 同成分 SubstanceItem 可继续堆叠
        return existing.getItem() instanceof SubstanceItem;
    }

    /** 把指定积累按 1 mol/份打包进槽（放不下则保留积累，待槽清空后再打） */
    private void packageAcc(Map<IonType, Double> acc, int slot) {
        while (countAccMoles(acc) >= PACK_MOLES) {
            // 按当前比例切 1 mol 样本（从摩尔数大的离子开始）
            Map<IonType, Double> sample = new HashMap<>();
            double remaining = PACK_MOLES;
            for (Map.Entry<IonType, Double> e : acc.entrySet()) {
                if (remaining <= 1e-9) break;
                double share = Math.min(e.getValue(), remaining);
                if (share > 1e-9) {
                    sample.put(e.getKey(), share);
                    remaining -= share;
                }
            }
            ItemStack leftover = insertIntoSlot(slot, SubstanceItem.fromComposition(sample));
            if (!leftover.isEmpty()) {
                return; // 槽满，保留积累等待下 tick
            }
            // 扣除已打包样本
            for (Map.Entry<IonType, Double> e : sample.entrySet()) {
                removeFromAcc(acc, e.getKey(), e.getValue());
            }
        }
    }

    private double countAccMoles(Map<IonType, Double> acc) {
        return acc.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    private ItemStack insertIntoSlot(int slot, ItemStack stack) {
        ItemStack existing = inventory.getStackInSlot(slot);
        if (existing.isEmpty()) {
            inventory.setStackInSlot(slot, stack);
            return ItemStack.EMPTY;
        }
        if (ItemStack.isSameItemSameTags(existing, stack) && existing.getCount() < existing.getMaxStackSize()) {
            existing.grow(1);
            inventory.setStackInSlot(slot, existing);
            return ItemStack.EMPTY;
        }
        return stack; // 放不下
    }

    // ==================== Tick（引擎 + 打包 + 停产） ====================

    public static void tick(Level level, BlockPos pos, BlockState state, ElectrolysisCellBlockEntity cell) {
        if (level.isClientSide) return;
        // 环境换热（牛顿冷却，对称）：低于环境温度时回温，避免只跌不升（§19.11 修复）
        double envTemp = cell.getEnvironmentTemperature(level, pos);
        double heatDelta = ChemConfig.AMBIENT_HEAT_TRANSFER_COEFFICIENT.get()
                * (envTemp - cell.getTemperature()) * 0.05;
        if (Math.abs(heatDelta) > 1e-6) {
            cell.addThermalEnergy(heatDelta * cell.getTotalHeatCapacity());
        }
        // 输出槽阻塞（满且不可堆叠/堆叠满）→ 停产；否则跑引擎并尝试打包
        if (!cell.isOutputBlocked()) {
            ReactionEngine.tick(cell);
            // 开口面气体交换（Phase 9）：电解槽敞口与上方网格双向交换
            cell.exchangeGasWithGrid(level, pos);
        }
        cell.tryPackageProducts(level);
    }

    // ==================== 双面供电（§19.10） ====================

    @Override
    public boolean isElectricallyPowered() {
        return ChemConfig.ELECTROLYZER_FREE_POWER.get() || (anodeEnergy > 0 && cathodeEnergy > 0);
    }

    @Override
    public boolean consumeElectricalEnergy(double kilojoules) {
        if (ChemConfig.ELECTROLYZER_FREE_POWER.get()) return true;
        int need = (int) Math.ceil(kilojoules * 1000); // kJ → J(FE)
        if (anodeEnergy <= 0 || cathodeEnergy <= 0) return false;
        int per = need / 2;
        int rem = need - per;
        if (anodeEnergy < per || cathodeEnergy < rem) return false;
        anodeEnergy -= per;
        cathodeEnergy -= rem;
        setChanged();
        return true;
    }

    public int getAnodeEnergy() {
        return anodeEnergy;
    }

    public int getCathodeEnergy() {
        return cathodeEnergy;
    }

    /**
     * 电解槽专属诊断行（§19.14 点 1）：原先空手右键刷聊天栏的状态报告迁移到这里，
     * 统一由分析仪诊断页显示；电解槽自身不再输出查询类信息。
     * 采用结构化行（翻译键 + 参数），客户端本地化（§19.16）。
     */
    @Override
    public void appendDiagnostics(List<ReportLine> lines) {
        Direction facing = getBlockState().hasProperty(ElectrolysisCellBlock.FACING)
                ? getBlockState().getValue(ElectrolysisCellBlock.FACING)
                : Direction.NORTH;
        int level = getVoltageLevel();
        lines.add(ReportLine.of("gui.trainees.analyzer.diag.electrodes",
                facing.getName(), facing.getOpposite().getName()));
        lines.add(ReportLine.raw("gui.trainees.analyzer.diag.voltage", List.of(
                ReportLine.ReportArg.of(String.valueOf(level)),
                ReportLine.ReportArg.key("trainees.voltage." + level))));
        lines.add(ReportLine.of(hasMembrane()
                ? "gui.trainees.analyzer.diag.membrane.yes"
                : "gui.trainees.analyzer.diag.membrane.no"));
        lines.add(ReportLine.of("gui.trainees.analyzer.diag.energy",
                String.valueOf(anodeEnergy), String.valueOf(cathodeEnergy)));
        lines.add(ReportLine.of(isElectricallyPowered()
                        ? "gui.trainees.analyzer.diag.circuit.on"
                        : "gui.trainees.analyzer.diag.circuit.off",
                String.valueOf(ChemConfig.ELECTROLYZER_FREE_POWER.get())));
        lines.add(ReportLine.of(isOutputBlocked()
                ? "gui.trainees.analyzer.diag.output.blocked"
                : "gui.trainees.analyzer.diag.output.ok"));
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    // ==================== 能力 ====================

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemCap.cast();
        }
        if (cap == ForgeCapabilities.ENERGY) {
            Direction facing = getBlockState().getValue(ElectrolysisCellBlock.FACING);
            if (side == facing) return anodeCap.cast();
            if (side == facing.getOpposite()) return cathodeCap.cast();
            return LazyOptional.empty();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCap.invalidate();
        anodeCap.invalidate();
        cathodeCap.invalidate();
    }

    // ==================== NBT ====================

    private static final String KEY_ANODE_ENERGY = "AnodeEnergy";
    private static final String KEY_CATHODE_ENERGY = "CathodeEnergy";
    private static final String KEY_MEMBRANE = "Membrane";
    private static final String KEY_OUT_A = "OutA";
    private static final String KEY_OUT_B = "OutB";
    private static final String KEY_CATHODE_ACC = "CathodeAcc";
    private static final String KEY_ANODE_ACC = "AnodeAcc";
    private static final String KEY_VOLTAGE_LEVEL = "VoltageLevel";

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(KEY_ANODE_ENERGY, anodeEnergy);
        tag.putInt(KEY_CATHODE_ENERGY, cathodeEnergy);
        tag.putInt(KEY_VOLTAGE_LEVEL, voltageLevel);
        tag.put(KEY_MEMBRANE, inventory.getStackInSlot(SLOT_MEMBRANE).save(new CompoundTag()));
        tag.put(KEY_OUT_A, inventory.getStackInSlot(SLOT_OUT_A).save(new CompoundTag()));
        tag.put(KEY_OUT_B, inventory.getStackInSlot(SLOT_OUT_B).save(new CompoundTag()));
        tag.put(KEY_CATHODE_ACC, saveAcc(cathodeAcc));
        tag.put(KEY_ANODE_ACC, saveAcc(anodeAcc));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.anodeEnergy = tag.getInt(KEY_ANODE_ENERGY);
        this.cathodeEnergy = tag.getInt(KEY_CATHODE_ENERGY);
        this.voltageLevel = tag.contains(KEY_VOLTAGE_LEVEL)
                ? Math.max(1, Math.min(MAX_VOLTAGE_LEVEL, tag.getInt(KEY_VOLTAGE_LEVEL)))
                : 2;
        inventory.setStackInSlot(SLOT_MEMBRANE, ItemStack.of(tag.getCompound(KEY_MEMBRANE)));
        inventory.setStackInSlot(SLOT_OUT_A, ItemStack.of(tag.getCompound(KEY_OUT_A)));
        inventory.setStackInSlot(SLOT_OUT_B, ItemStack.of(tag.getCompound(KEY_OUT_B)));
        loadAcc(cathodeAcc, tag.getList(KEY_CATHODE_ACC, Tag.TAG_COMPOUND));
        loadAcc(anodeAcc, tag.getList(KEY_ANODE_ACC, Tag.TAG_COMPOUND));
    }

    private static ListTag saveAcc(Map<IonType, Double> acc) {
        ListTag list = new ListTag();
        for (Map.Entry<IonType, Double> e : acc.entrySet()) {
            CompoundTag sub = new CompoundTag();
            sub.putString("id", e.getKey().getId().toString());
            sub.putDouble("mol", e.getValue());
            list.add(sub);
        }
        return list;
    }

    private static void loadAcc(Map<IonType, Double> acc, ListTag list) {
        acc.clear();
        if (list == null) return;
        for (Tag t : list) {
            CompoundTag sub = (CompoundTag) t;
            IonType ion = findIon(sub.getString("id"));
            if (ion != null) {
                acc.put(ion, sub.getDouble("mol"));
            }
        }
    }

    /** 按 id（含命名空间）在全局离子注册表（ModIons.ALL_IONS）中查找 */
    private static IonType findIon(String id) {
        for (IonType ion : ModChemistry.ModIons.ALL_IONS) {
            if (ion.getId().toString().equals(id)) return ion;
        }
        return null;
    }

    // ==================== 内部能量存储 ====================

    /** 单面 FE 缓冲：只进不出，存满为止 */
    private class CellEnergyStorage implements IEnergyStorage {
        private final boolean anode;

        CellEnergyStorage(boolean anode) {
            this.anode = anode;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (maxReceive <= 0) return 0;
            int current = anode ? anodeEnergy : cathodeEnergy;
            int accepted = Math.min(maxReceive, ENERGY_CAPACITY - current);
            if (accepted <= 0) return 0;
            if (!simulate) {
                if (anode) {
                    anodeEnergy += accepted;
                } else {
                    cathodeEnergy += accepted;
                }
                setChanged();
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0; // 电极只接收电能，不对外输出
        }

        @Override
        public int getEnergyStored() {
            return anode ? anodeEnergy : cathodeEnergy;
        }

        @Override
        public int getMaxEnergyStored() {
            return ENERGY_CAPACITY;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }
}
