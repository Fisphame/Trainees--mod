package com.pha.trainees.chemistry.blockentity;

import com.pha.trainees.chemistry.container.IContainerPort;
import com.pha.trainees.config.ChemConfig;
import com.pha.trainees.registry.ModChemistry;
import com.pha.trainees.registry.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 实验室反应釜方块实体（§19.30 方案 1）：<b>10 L</b> 单方块容器。
 *
 * <p>相对烧杯只有三处差别，其余（反应引擎、换热、逐面气体交换、附着件、容量、材料、诊断）
 * 全部继承：</p>
 * <ol>
 *   <li><b>容积 10 L</b>：把水的沸点实验拉进 0.1~3 atm 的"旋钮区"
 *       （1 L 烧杯里 5 mol 气 @1100 K 是 451 atm 的炸弹量级，10 L 只有 45 atm）；
 *       容量上限也随之 ×10（批次更大）。</li>
 *   <li><b>端口可以由阀门"开孔"</b>：釜口（上）之外，任何面上装了 {@link IContainerPort}
 *       就多一个口——"多端口"是<b>建出来的</b>，不是写死 5 个面。烧杯不这么做（玻璃壁不是口），
 *       这是两种容器的一处真实差别。</li>
 *   <li><b>尺寸放大效应</b>：单位体积传热 ∝ V^(−1/3)，10 L 时为 0.46 ——
 *       真实反应釜"越大越难控温"，也是搅拌/夹套/内盘管的用武之地。</li>
 * </ol>
 */
public class LabReactorBlockEntity extends BeakerBlockEntity {

    /** 实验室釜有效容积（L）：烧杯的 10 倍 */
    public static final double LAB_REACTOR_VOLUME_L = 10.0;

    /**
     * 密封釜头槽（1 格）：装上 = <b>全焊态</b>——釜体全部面强制封闭，且残留更大、清洗到不了底。
     * 拆下即回到可拆盖（能彻底清洗）。这就是"可拆盖 vs 全焊"这个真实取舍的开关。
     */
    private final ItemStackHandler headSlot = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(ModChemistry.ModChemistryItems.SEALED_HEAD.get());
        }
    };

    public LabReactorBlockEntity(BlockPos pos, BlockState state) {
        this(ModChemistry.ModChemistryBlockEntities.LAB_REACTOR.get(), pos, state);
    }

    protected LabReactorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setVolumeInternal(LAB_REACTOR_VOLUME_L);
    }

    /**
     * 端口面（§19.30）：釜口（上）之外，<b>装了端口方块就多一个口</b>。
     * 于是"在釜体侧面加一个加料口/测温口"是玩家用阀门做出来的动作，
     * 而不是我们预先写死方向——这也正是 `IContainerPort` 这个抽象存在的意义。
     */
    @Override
    protected Set<Direction> portFaces() {
        Set<Direction> ports = new LinkedHashSet<>();
        ports.add(Direction.UP);
        Level level = getLevel();
        if (level != null) {
            for (Direction d : Direction.values()) {
                if (d == Direction.UP) continue;
                if (level.getBlockState(getBlockPos().relative(d)).getBlock() instanceof IContainerPort) {
                    ports.add(d);
                }
            }
        }
        return ports;
    }

    /**
     * 尺寸放大效应（§19.30）：单位体积传热能力 ∝ V^(−1/3)。
     * 以烧杯（1 L）为参考点：10 L → 0.46，1000 L → 0.10。
     */
    @Override
    protected double baseHeatTransferFactor() {
        double reference = Math.max(1e-6, ChemConfig.DEFAULT_VOLUME.get());
        return Math.pow(reference / Math.max(1e-6, getVolume()), 1.0 / 3.0);
    }

    // ==================== 密封釜头 / 残留 / 清洗（§19.30.2） ====================

    public boolean hasSealedHead() {
        return !headSlot.getStackInSlot(0).isEmpty();
    }

    /** 装上密封釜头：占一格，并把<b>所有面强制封闭</b>（密封釜头不靠阀门，§19.27 的开口覆盖正是为此存在）。 */
    public void installHead(ItemStack stack) {
        ItemStack one = stack.copy();
        one.setCount(1);
        headSlot.setStackInSlot(0, one);
        applyHeadSeal(true);
    }

    /** 拆下密封釜头并返回（空 = 本来就没有）。 */
    public ItemStack removeHead() {
        ItemStack current = headSlot.getStackInSlot(0);
        if (current.isEmpty()) return ItemStack.EMPTY;
        headSlot.setStackInSlot(0, ItemStack.EMPTY);
        applyHeadSeal(false);
        return current;
    }

    /** 全焊态把所有面封死；还原时交回默认判定（釜口 + 阀门开孔）。 */
    private void applyHeadSeal(boolean sealed) {
        for (Direction d : Direction.values()) {
            setFaceOpenOverride(d, sealed ? Boolean.FALSE : null);
        }
        setChanged();
    }

    /**
     * 残留地板：基础残留 × 容积 ×（全焊 ? 倍率 : 1）。
     * 10 L 敞口盖体 → 0.05×10 = 0.5 mol；装上密封釜头 → 1.5 mol（死角冲不到）。
     */
    @Override
    public double residueFloorMoles() {
        double perLiter = Math.max(0.0, ModConfig.COMMON.reactorResiduePerLiterMoles.get());
        double factor = hasSealedHead()
                ? Math.max(1.0, ModConfig.COMMON.sealedHeadResidueFactor.get()) : 1.0;
        return perLiter * getVolume() * factor;
    }

    /** 清洗地板：可拆盖能擦净（0）；全焊只能冲掉浮的，死角仍在（= 残留地板）。 */
    @Override
    public double cleanableFloorMoles() {
        return hasSealedHead() ? residueFloorMoles() : 0.0;
    }

    /**
     * 耐压上限 = 材料 × 釜盖系数（§19.31）：**全焊（密封釜头）1.5 倍，可拆盖 1.0**。
     * 这就是真实取舍里"全焊耐压更高"那一半——现在危险层做完了，它终于有效果。
     */
    @Override
    public double getMaxSafePressureAtm() {
        double base = super.getMaxSafePressureAtm();
        return hasSealedHead() ? base * 1.5 : base;
    }

    // ==================== 持久化（釜头格） ====================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Head", headSlot.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Head")) {
            headSlot.deserializeNBT(tag.getCompound("Head"));
        }
        applyHeadSeal(hasSealedHead());   // 存档恢复后重新施加密封
    }
}
