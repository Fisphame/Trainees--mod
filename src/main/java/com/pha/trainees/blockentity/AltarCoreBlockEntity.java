package com.pha.trainees.blockentity;

import com.pha.trainees.config.ChemConfig;
import com.pha.trainees.mana.IManaStorage;
import com.pha.trainees.mana.ManaCapabilities;
import com.pha.trainees.mana.ManaStorage;
import com.pha.trainees.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 祭坛主方块实体（§17.1 魔力的载体，作者 2026-10-02 决定）。
 *
 * <p>魔力存<b>主方块</b>里，主方块自带较大缓存；另外可以连接"缓存单元"扩容
 * （单元方块与连接规则待定，见 {@link #getCapacity()} 的注释）。4 个 KunAltar 子块只是输入槽，
 * 不持有魔力。</p>
 *
 * <p>表现先走**粒子**（{@link com.pha.trainees.block.AltarCoreBlock#animateTick}）：
 * 越满越密，模型与液面留到后续可视化批次。为此存量要同步到客户端——但魔力是**事件性变化**
 * （注入/消耗），不是每 tick 变的，所以只在真正变化时发一次更新包。</p>
 */
public class AltarCoreBlockEntity extends BlockEntity {

    private static final String NBT_MANA = "Mana";
    /** 容量只随同步包走（存档里不存：它是从"相邻缓存单元"推出来的）。 */
    private static final String NBT_CAPACITY = "Capacity";

    /** 容量重算周期（tick）：载入后最多 1 秒内把"相邻缓存单元"的容量补齐。 */
    private static final int CAPACITY_CHECK_INTERVAL = 20;

    private int capacityCheckTicker;

    private final ManaStorage mana = new ManaStorage(configuredCapacity(), this::onManaChanged);
    private final LazyOptional<IManaStorage> manaCap = LazyOptional.of(() -> mana);

    public AltarCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.ModBlockEntities.ALTAR_CORE_ENTITY.get(), pos, state);
    }

    private static int configuredCapacity() {
        return ChemConfig.MANA_ALTAR_CORE_CAPACITY.get();
    }

    /** 存量/容量真的变化时：落存档 + 通知客户端（粒子要读）。 */
    private void onManaChanged() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * 当前容量 = 主方块基础缓存 + Σ(已连接缓存单元)。
     *
     * <p>缓存单元方块本体与"连接"规则（相邻？链式？最大数量？）还没定，因此这里目前只算基础值；
     * 等作者拍板后在 {@link #refreshCapacity()} 里加一段扫描即可，其它代码不用动。</p>
     */
    public int getCapacity() {
        int capacity = configuredCapacity();
        // 相邻的"精炼精华缓存单元"扩容（§17.1 作者定：相邻、可叠、不链式）。
        // 将来换成线缆/无线连接时**只改这里**——别把连接方式散到别处。
        if (level != null) {
            int units = 0;
            for (Direction dir : Direction.values()) {
                if (level.getBlockState(worldPosition.relative(dir)).is(ModBlocks.MANA_CACHE_UNIT.get())) {
                    units++;
                }
            }
            capacity += units * ChemConfig.MANA_CACHE_UNIT_CAPACITY.get();
        }
        return capacity;
    }

    /** 按当前接线情况刷新容量（放置/拆除缓存单元、tick、交互时调用）。 */
    public void refreshCapacity() {
        mana.setCapacity(getCapacity());
    }

    /**
     * 服务端周期 tick（由方块的 {@code getTicker} 驱动）：**载入后在这里补上容量重算**。
     *
     * <p>为什么不在 {@code onLoad} 里算：{@code onLoad} 会在**区块反序列化/加载**过程中被调用，
     * 那时 {@code level.getBlockState} 可能去加载邻区块、{@code sendBlockUpdated} 也会在加载中发包
     * ——正是蓝本里记过的"绝不在区块加载中碰世界"那类历史事故。所以载入时只做**不碰世界**的事，
     * 真正的重算推迟到这里（区块肯定已经就绪、且在服务端主线程 tick 上）。</p>
     */
    public void serverTick() {
        if (level == null || level.isClientSide) return;
        if (++capacityCheckTicker < CAPACITY_CHECK_INTERVAL) return;
        capacityCheckTicker = 0;
        refreshCapacity();
    }

    // ==================== 生命周期 ====================

    @Override
    public void onLoad() {
        super.onLoad();
        // ⚠ 这里**绝不能**碰世界（不 getBlockState、不发包）：见 serverTick 的注释。
        if (level != null && !level.isClientSide) {
            mana.setCapacitySilently(configuredCapacity());
        }
    }

    // ==================== 能力 ====================

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

    // ==================== 读数（命令 / 粒子 / 未来的配方消耗） ====================

    public IManaStorage getManaStorage() {
        return mana;
    }

    public int getManaStored() {
        return mana.getManaStored();
    }

    public int getManaCapacity() {
        return mana.getMaxManaStored();
    }

    public void setMana(int value) {
        mana.setMana(value);
    }

    /**
     * 扣魔力（消耗型逻辑用，例如祭坛强力配方）。
     *
     * <p>对外只暴露 {@link IManaStorage}（能力视图），但"够不够/扣掉"这种记账动作走这里——
     * 于是消耗方不用关心底层是 {@link ManaStorage} 还是别的实现。</p>
     *
     * @return true = 扣成功；false = 不够（一点不扣）
     */
    public boolean consumeMana(int amount) {
        return mana.consume(amount);
    }

    /** 充满度 0~1：粒子密度与后续可视化都读它。 */
    public float getManaFillRatio() {
        int capacity = mana.getMaxManaStored();
        return capacity <= 0 ? 0f : (float) mana.getManaStored() / capacity;
    }

    // ==================== 持久化 ====================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(NBT_MANA, mana.getManaStored());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        // 同样静默：load 发生在区块反序列化里，不能触发同步发包
        // Capacity 只存在于"同步包"里（服务端存档不存它，因为容量是从接线推出来的）；
        // 客户端读它才能让 Jade 显示带缓存单元的总容量（否则永远只显示主方块基础值）
        mana.setCapacitySilently(tag.contains(NBT_CAPACITY) ? tag.getInt(NBT_CAPACITY) : configuredCapacity());
        mana.setManaSilently(tag.getInt(NBT_MANA));
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putInt(NBT_MANA, mana.getManaStored());
        tag.putInt(NBT_CAPACITY, mana.getMaxManaStored());
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
