package com.pha.trainees.blockentity;

import com.pha.trainees.machine.FaceConfig;
import com.pha.trainees.machine.essence.EssenceNetwork;
import com.pha.trainees.mana.IManaStorage;
import com.pha.trainees.mana.ManaCapabilities;
import com.pha.trainees.mana.ManaStorage;
import com.pha.trainees.menu.MachineHost;
import com.pha.trainees.multiblock.EssencePortPattern;
import com.pha.trainees.registry.ModEssenceBlocks;
import com.pha.trainees.util.game.structure.MultiblockPattern;
import com.pha.trainees.util.game.structure.MultiblockStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 精练精华网端口方块实体（§17.1 ④）：整个网络的**唯一聚合点**。
 *
 * <p><b>激活（成形）状态怎么管</b>（作者 2026-10-03 确认的口径）：</p>
 * <ul>
 *   <li>**不走** {@code ActiveStructureManager} 那套持久化激活——每 {@value #SCAN_INTERVAL} tick
 *       重算一次结构就是唯一真相：拆一块下一秒就反映、重登自动恢复、**不存在"存档说成形实际被拆"**。</li>
 *   <li>额外提供**"成形沿"**：{@code false→true} 触发一次性动作（缓存结构快照、播音效），
 *       {@code true→false} 做清理（容量回基础、清空网络计数）。这样既有"事件"可用，又不用持久化。</li>
 *   <li>**结构快照**：成形时把 {@link MultiblockPattern.MatchResult}（原点 + 朝向）存下来，
 *       之后找扩展接口/功能方块都用快照换算，**不再重复全量检查**（原来一次刷新检查两遍，浪费）。</li>
 *   <li>{@code structureFormed} **不存 NBT**：重登最多晚 1 秒显示为未成形，比"存档说成形"安全。</li>
 * </ul>
 *
 * <p><b>端口自己不搬 RE</b>：它只提供 {@link #receiveThroughput}/{@link #extractThroughput}
 * （受吞吐限制）+ 聚合容量；"主动推拉"由**总线**在各自 tick 里做（职责清晰，也不会两边搬同一份）。</p>
 */
public class EssenceNetworkPortBlockEntity extends BlockEntity implements MachineHost {

    /** 端口自身基础容量：3,600,000 RE（= 祭坛核心 720,000 的 5 倍；其余容量靠接缓存单元）。 */
    public static final int BASE_CAPACITY = 3_600_000;
    /** 驱动耗：每 tick 4 RE。 */
    public static final int UPKEEP_PER_TICK = 4;
    /** 默认吞吐：单次输入/输出上限 20,480 RE/t（可调，上限 int 最大值）。 */
    public static final int DEFAULT_THROUGHPUT = 20_480;
    /** 结构与网络的复检间隔（tick）。 */
    public static final int SCAN_INTERVAL = 20;

    private static final String NBT_MANA = "Mana";
    private static final String NBT_THROUGHPUT = "Throughput";

    private final ManaStorage mana = new ManaStorage(BASE_CAPACITY, this::onManaChanged);
    private final LazyOptional<IManaStorage> manaCap = LazyOptional.of(this::aggregate);

    /** GUI 用的空物品槽（端口本身没有物品槽）。 */
    private final ItemStackHandler emptyInventory = new ItemStackHandler(0);

    private int throughput = DEFAULT_THROUGHPUT;
    private int networkUnits;
    private boolean networkTruncated;
    private boolean powered = true;
    private int tickCounter;

    // ===== 成形状态（全是瞬时值，不存 NBT）=====
    private boolean formed;
    /** 成形时的结构快照（原点 + 朝向），用于换算任意局部坐标。 */
    @Nullable
    private MultiblockPattern.MatchResult snapshot;
    /** 结构里的网络扩展接口（最多一个）。 */
    @Nullable
    private BlockPos expansionInterface;

    public EssenceNetworkPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModEssenceBlocks.ModEssenceBlockEntities.ESSENCE_NETWORK_PORT.get(), pos, state);
    }

    // ==================== 每 tick ====================

    public void serverTick() {
        if (level == null || level.isClientSide) return;

        // 驱动耗：付不起就停传输（不销毁数据）
        powered = mana.getManaStored() >= UPKEEP_PER_TICK;
        if (powered) {
            mana.consume(UPKEEP_PER_TICK);
        }

        if (++tickCounter % SCAN_INTERVAL == 0) {
            refresh();
        }
    }

    /** 结构 + 网络的一次刷新（**结构只检查一次**）。 */
    public void refresh() {
        if (level == null) return;

        MultiblockPattern.MatchResult match =
                MultiblockStructure.checkStructure(level, worldPosition, EssencePortPattern.STRUCTURE_ID);

        if (match == null) {
            if (formed) {
                onStructureBroken();
            }
            return;   // 未成形：没有扩展接口 → 也就没有网络（容量回基础）
        }
        if (!formed) {
            onStructureFormed(match);
        }
        linkBuses(match, true);   // 认领结构里的总线

        // 用快照找扩展接口（不重复全量检查）
        expansionInterface = findExpansionInterface(snapshot);
        EssenceNetwork.Result result = EssenceNetwork.scan(level,
                expansionInterface != null ? expansionInterface : worldPosition);
        networkUnits = result.cacheUnitCount();
        networkTruncated = result.truncated();
        applyCapacity();
        setChanged();
    }

    /** 成形沿（false → true）：缓存快照 + 音效。 */
    private void onStructureFormed(MultiblockPattern.MatchResult match) {
        formed = true;
        snapshot = match;
        if (level != null && !level.isClientSide) {
            level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.6F, 1.4F);
        }
    }

    /** 成形沿（true → false）：清理 + 容量回基础。 */
    private void onStructureBroken() {
        linkBuses(snapshot, false);   // 先用旧快照把总线的"主端口"清掉
        formed = false;
        snapshot = null;
        expansionInterface = null;
        networkUnits = 0;
        networkTruncated = false;
        applyCapacity();
        if (level != null && !level.isClientSide) {
            level.playSound(null, worldPosition, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.5F, 0.8F);
        }
        setChanged();
    }

    /** 容量 = 自身基础 + 连到的缓存单元 × 单个体容量（饱和运算）。 */
    private void applyCapacity() {
        int capacity = EssenceNetwork.saturatingAdd(BASE_CAPACITY,
                EssenceNetwork.saturatingMultiply(networkUnits,
                        com.pha.trainees.config.ChemConfig.MANA_CACHE_UNIT_CAPACITY.get()));
        if (capacity != mana.getMaxManaStored()) {
            mana.setCapacitySilently(capacity);
            if (mana.getManaStored() > capacity) {
                mana.setManaSilently(capacity);
            }
        }
    }

    /** 用快照在结构范围内找网络扩展接口（最多一个）。 */
    @Nullable
    private BlockPos findExpansionInterface(@Nullable MultiblockPattern.MatchResult match) {
        if (level == null || match == null) return null;
        for (int y = 0; y < 4; y++) {
            for (int z = 0; z < 3; z++) {
                for (int x = 0; x < 3; x++) {
                    if (x == 1 && z == 1) continue;   // 中心柱是墙壁
                    BlockPos absolute = match.getAbsolutePos(new BlockPos(x, y, z));
                    if (level.getBlockState(absolute).is(ModEssenceBlocks.ESSENCE_EXPANSION_INTERFACE.get())) {
                        return absolute;
                    }
                }
            }
        }
        return null;
    }

    /** 结构范围内有方块变动时，让下一次 tick 立刻重查（端口自己的邻居变动会调到这里）。 */
    public void invalidateStructure() {
        tickCounter = SCAN_INTERVAL - 1;
    }

    /**
     * 认领/释放结构里的总线：把主端口坐标回填给它们（拆结构时传 null）。
     *
     * <p>为什么由端口主动做：总线自己找端口就得每 tick 全图搜索，而端口本来就在遍历结构范围找扩展接口，
     * 顺手做掉是零成本；总线只管"搬"，不知道结构长什么样。</p>
     */
    private void linkBuses(@Nullable MultiblockPattern.MatchResult match, boolean link) {
        if (level == null || match == null) return;
        for (int y = 0; y < 4; y++) {
            for (int z = 0; z < 3; z++) {
                for (int x = 0; x < 3; x++) {
                    if (x == 1 && z == 1) continue;   // 中心柱是墙壁
                    BlockPos absolute = match.getAbsolutePos(new BlockPos(x, y, z));
                    if (level.getBlockEntity(absolute) instanceof EssenceBusBlockEntity bus) {
                        bus.setHostPort(link ? worldPosition : null);
                    }
                }
            }
        }
    }

    private void onManaChanged() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ==================== 对外能力：吞吐受限的聚合视图 ====================

    /** 存量/容量直接来自内部存储；**单次进出受 {@link #getThroughput()} 限制**。 */
    private IManaStorage aggregate() {
        return new IManaStorage() {
            @Override
            public int receiveMana(int amount, boolean simulate) {
                if (!powered || !formed) return 0;
                return mana.receiveMana(Math.min(amount, throughput), simulate);
            }

            @Override
            public int extractMana(int amount, boolean simulate) {
                if (!powered || !formed) return 0;
                return mana.extractMana(Math.min(amount, throughput), simulate);
            }

            @Override
            public int getManaStored() {
                return mana.getManaStored();
            }

            @Override
            public int getMaxManaStored() {
                return mana.getMaxManaStored();
            }

            @Override
            public boolean canReceive() {
                return powered && formed;
            }

            @Override
            public boolean canExtract() {
                return powered && formed;
            }
        };
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ManaCapabilities.mana()) {
            return manaCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        manaCap.invalidate();
    }

    // ==================== 存档与同步 ====================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(NBT_MANA, mana.getManaStored());
        tag.putInt(NBT_THROUGHPUT, throughput);
        // 注意：结构/网络状态是瞬时的，**故意不存**（重登后 1 秒内自动重算）
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        throughput = tag.contains(NBT_THROUGHPUT) ? tag.getInt(NBT_THROUGHPUT) : DEFAULT_THROUGHPUT;
        mana.setCapacitySilently(BASE_CAPACITY);
        mana.setManaSilently(tag.getInt(NBT_MANA));
        formed = false;          // 重登后重新判定
        snapshot = null;
        expansionInterface = null;
        manaCap.invalidate();
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putInt(NBT_MANA, mana.getManaStored());
        tag.putInt(NBT_THROUGHPUT, throughput);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ==================== MachineHost（复用魔法机器的 GUI 框架） ====================

    @Override
    public ItemStackHandler inventory() {
        return emptyInventory;
    }

    @Override
    public IManaStorage mana() {
        return mana;
    }

    @Override
    public int progress() {
        return 0;
    }

    @Override
    public int totalTicks() {
        return 0;
    }

    @Override
    public FaceConfig faces() {
        return null;   // 端口的进出由总线负责，不做按面配置
    }

    @Override
    public int networkUnits() {
        return networkUnits;
    }

    @Override
    public boolean networkTruncated() {
        return networkTruncated;
    }

    @Override
    public int throughput() {
        return throughput;
    }

    @Override
    public boolean structureFormed() {
        return formed;
    }

    @Override
    public boolean poweredByUpkeep() {
        return powered;
    }

    // ==================== 供总线/GUI/命令用 ====================

    public int getThroughput() {
        return throughput;
    }

    /** 调整吞吐（GUI 里做调节控件；上限 int 最大值）。 */
    @Override
    public void setThroughput(int value) {
        throughput = Math.max(1, value);
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean isStructureFormed() {
        return formed;
    }

    /** 成形后的结构快照（总线要按它找自己/找端口时可以复用）。 */
    @Nullable
    public MultiblockPattern.MatchResult getSnapshot() {
        return snapshot;
    }

    @Nullable
    public BlockPos getExpansionInterface() {
        return expansionInterface;
    }

    /** 总线调这个把 RE 送进端口（受吞吐限制）。 */
    public int receiveThroughput(int amount, boolean simulate) {
        return aggregate().receiveMana(amount, simulate);
    }

    /** 总线调这个从端口取 RE（受吞吐限制）。 */
    public int extractThroughput(int amount, boolean simulate) {
        return aggregate().extractMana(amount, simulate);
    }
}
