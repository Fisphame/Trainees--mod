package com.pha.trainees.blockentity;

import com.pha.trainees.block.EssenceBusBlock;
import com.pha.trainees.mana.IManaStorage;
import com.pha.trainees.mana.ManaCapabilities;
import com.pha.trainees.registry.ModEssenceBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 精练精华网总线方块实体（§17.1 ④）：**主动搬运 RE 的那一层**。
 *
 * <p>职责切分（作者确认的口径）：**端口不搬 RE**，只提供受吞吐限制的
 * {@code receiveThroughput/extractThroughput}；真正"吸进来/喂出去"由总线在 tick 里做。
 * 这样不会出现端口与总线同时搬同一份。</p>
 *
 * <p>两种模式 × 两种连接方式，共用一个类（由方块决定）：</p>
 * <ul>
 *   <li><b>输入总线</b>：从**相邻 6 面**（有线）或**绑定的方块**（无线）抽 RE → 送进端口</li>
 *   <li><b>输出总线</b>：从端口取 RE → 送进相邻 6 面（有线）或绑定的方块（无线）</li>
 * </ul>
 *
 * <p><b>无线</b>：作者要求"同维度无视距离"——所以绑定记录的是**维度 + 坐标**，
 * 只在维度相同且目标区块已加载时工作（加载区块是玩家自己的事）✓</p>
 *
 * <p><b>搬运顺序永远是"先模拟、后执行"</b>：先问来源能抽多少、再问目标能收多少，取小值，
 * 然后才真抽真存——这样即使中途有变化也只会少搬，不会复制物品（§17.1 的教训）。</p>
 */
public class EssenceBusBlockEntity extends BlockEntity {

    private static final String NBT_BOUND_DIM = "BoundDim";
    private static final String NBT_BOUND_POS = "BoundPos";

    /** 主端口的坐标：由端口在每次刷新时回填（总线自己是找不到端口的，也不该每 tick 全图搜）。 */
    @Nullable
    private BlockPos hostPort;

    /** 无线绑定的目标（维度 + 坐标）。 */
    @Nullable
    private ResourceKey<Level> boundDimension;
    @Nullable
    private BlockPos boundPos;

    public EssenceBusBlockEntity(BlockPos pos, BlockState state) {
        super(ModEssenceBlocks.ModEssenceBlockEntities.ESSENCE_BUS.get(), pos, state);
    }

    public boolean isWireless() {
        return getBlockState().getBlock() instanceof EssenceBusBlock bus && bus.isWireless();
    }

    public boolean isInput() {
        return getBlockState().getBlock() instanceof EssenceBusBlock bus
                && bus.getMode() == EssenceBusBlock.Mode.INPUT;
    }

    @Nullable
    public BlockPos getHostPort() {
        return hostPort;
    }

    /** 端口每次刷新时回填（结构里的总线才有效）。 */
    public void setHostPort(@Nullable BlockPos portPos) {
        if (java.util.Objects.equals(hostPort, portPos)) return;
        hostPort = portPos;
        setChanged();
    }

    @Nullable
    public ResourceKey<Level> getBoundDimension() {
        return boundDimension;
    }

    @Nullable
    public BlockPos getBoundPos() {
        return boundPos;
    }

    public void bind(ResourceKey<Level> dimension, BlockPos pos) {
        boundDimension = dimension;
        boundPos = pos.immutable();
        setChanged();
    }

    public void clearBinding() {
        boundDimension = null;
        boundPos = null;
        setChanged();
    }

    // ==================== 每 tick ====================

    public void serverTick() {
        if (level == null || level.isClientSide) return;
        if (!(level.getBlockEntity(hostPortSafe()) instanceof EssenceNetworkPortBlockEntity port)) return;
        if (!port.isStructureFormed()) return;

        if (isWireless()) {
            tickWireless(port);
        } else {
            tickWired(port);
        }
    }

    @Nullable
    private BlockPos hostPortSafe() {
        return hostPort;
    }

    /**
     * 取某个位置上的魔力能力。
     *
     * <p>走**方块实体**查询（{@code be.getCapability(cap, side)}）而不是 {@code level.getCapability(cap,pos,side)}：
     * 后者的重载在这个 Forge 版本上签名对不上（编译期就报"找不到合适的方法"）。</p>
     */
    @Nullable
    private static IManaStorage manaAt(Level level, BlockPos pos, @Nullable Direction side) {
        BlockEntity be = level.getBlockEntity(pos);
        return be == null ? null : be.getCapability(ManaCapabilities.mana(), side).orElse(null);
    }

    // ==================== 有线：相邻 6 面 ====================

    private void tickWired(EssenceNetworkPortBlockEntity port) {
        for (Direction side : Direction.values()) {
            BlockPos neighbour = worldPosition.relative(side);
            if (neighbour.equals(hostPort)) continue;   // 别对着自己的端口搬
            if (isInput()) {
                pullFrom(port, level, neighbour, side.getOpposite());
            } else {
                pushTo(port, level, neighbour, side.getOpposite());
            }
        }
    }

    // ==================== 无线：绑定的方块（同维度、无视距离） ====================

    private void tickWireless(EssenceNetworkPortBlockEntity port) {
        if (boundPos == null || boundDimension == null) return;
        if (!level.dimension().equals(boundDimension)) return;   // 只同维度
        if (!level.isLoaded(boundPos)) return;                   // 区块没加载就不工作（玩家自己的事）

        if (isInput()) {
            pullFrom(port, level, boundPos, null);
        } else {
            pushTo(port, level, boundPos, null);
        }
    }

    // ==================== 搬运核心（先模拟后执行） ====================

    /** 把 {@code sourcePos} 的 RE 抽进端口（受端口吞吐限制）。 */
    private void pullFrom(EssenceNetworkPortBlockEntity port, Level sourceLevel, BlockPos sourcePos,
                          @Nullable Direction side) {
        IManaStorage source = manaAt(sourceLevel, sourcePos, side);
        if (source == null || !source.canExtract()) return;

        int budget = port.getThroughput();
        int canPull = source.extractMana(budget, true);
        if (canPull <= 0) return;
        int canAccept = port.receiveThroughput(canPull, true);
        if (canAccept <= 0) return;

        int amount = Math.min(canPull, canAccept);
        int actuallyPulled = source.extractMana(amount, false);
        if (actuallyPulled > 0) {
            port.receiveThroughput(actuallyPulled, false);   // 实抽多少就存多少（不会凭空多）
        }
    }

    /** 把端口的 RE 送进 {@code sinkPos}（受端口吞吐限制）。 */
    private void pushTo(EssenceNetworkPortBlockEntity port, Level sinkLevel, BlockPos sinkPos,
                        @Nullable Direction side) {
        IManaStorage sink = manaAt(sinkLevel, sinkPos, side);
        if (sink == null || !sink.canReceive()) return;

        int budget = port.getThroughput();
        int canGive = port.extractThroughput(budget, true);
        if (canGive <= 0) return;
        int canTake = sink.receiveMana(canGive, true);
        if (canTake <= 0) return;

        int amount = Math.min(canGive, canTake);
        int actuallyTaken = port.extractThroughput(amount, false);
        if (actuallyTaken > 0) {
            sink.receiveMana(actuallyTaken, false);
        }
    }

    // ==================== 存档 ====================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (boundDimension != null) {
            tag.putString(NBT_BOUND_DIM, boundDimension.location().toString());
        }
        if (boundPos != null) {
            tag.putLong(NBT_BOUND_POS, boundPos.asLong());
        }
        // hostPort 是派生值：由端口每次刷新回填，不存
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        boundDimension = tag.contains(NBT_BOUND_DIM)
                ? ResourceKey.create(Registries.DIMENSION, new ResourceLocation(tag.getString(NBT_BOUND_DIM)))
                : null;
        boundPos = tag.contains(NBT_BOUND_POS) ? BlockPos.of(tag.getLong(NBT_BOUND_POS)) : null;
        hostPort = null;   // 等端口刷新回填
    }
}
