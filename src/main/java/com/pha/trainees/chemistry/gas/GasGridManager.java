package com.pha.trainees.chemistry.gas;

import com.pha.trainees.Main;
import com.pha.trainees.chemistry.heat.HeatSourceTracker;
import com.pha.trainees.chemistry.particle.IonType;
import com.pha.trainees.config.ChemConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 开放气体网格管理器（Phase 9 / §19.13）。
 * 每维度一份（SavedData）；两级 LOD：
 * - 精细场 fineGrid：仅已加载区块内的"有气体格子" Map<BlockPos, GasMixture>；
 * - 粗账 summaries：已卸载/存档的区块级气体总量+重心（持久化）。
 * 精度档位（ChemConfig.gasPrecisionMode）：A 全粗（默认）B 半精 C 全精。
 */
public class GasGridManager extends SavedData {

    public static final String DATA_NAME = "trainees_gas_grid";

    // ===== 精度档 =====
    public static final int MODE_A_FULL_COARSE = 0; // 卸载+退出全坍缩粗账（默认）
    public static final int MODE_B_HALF = 1;        // 卸载坍缩，退出时仍加载区块精存
    public static final int MODE_C_FULL_FINE = 2;   // 永不粗化（精存一切）

    /** 精细场：只含已加载区块内"有气体"的格子 */
    private final Map<BlockPos, GasMixture> fineGrid = new ConcurrentHashMap<>();
    /** 粗账：区块级气体总量+重心（持久化） */
    private final Map<ChunkPos, GasChunkSummary> summaries = new ConcurrentHashMap<>();
    /** 脏区块（有变化，等待扩散处理），运行期 */
    private final Map<ChunkPos, Long> dirtyChunks = new ConcurrentHashMap<>();
    /** 精存快照（档 B：退出时仍加载的区块；档 C：全部）——存档层，加载时优先于此恢复 */
    private final Map<ChunkPos, Map<Long, GasMixture>> fineSnapshot = new ConcurrentHashMap<>();
    /** 待水合区块队列（粗账 → 精场）：只在服务端 tick 消费，绝不在区块加载事件里处理（防自死锁） */
    private final java.util.Queue<ChunkPos> pendingHydrate = new java.util.concurrent.ConcurrentLinkedQueue<>();
    /** 每 tick 水合预算（区块数） */
    public static final int HYDRATE_BUDGET_PER_TICK = 4;

    // 运行时持有维度（SavedData 反序列化后需重绑）
    private transient ResourceKey<Level> dimensionKey;

    private GasGridManager() {
        super();
    }

    // ==================== 获取单例（每维度） ====================

    public static GasGridManager get(ServerLevel level) {
        // 维度自己的 dataStorage → 文件落在各自维度目录，key 同名互不冲突
        return level.getDataStorage().computeIfAbsent(GasGridManager::load, GasGridManager::new, DATA_NAME);
    }

    public static GasGridManager load(CompoundTag tag) {
        GasGridManager manager = new GasGridManager();
        ListTag summariesTag = tag.getList("summaries", Tag.TAG_COMPOUND);
        for (int i = 0; i < summariesTag.size(); i++) {
            CompoundTag st = summariesTag.getCompound(i);
            manager.summaries.put(new ChunkPos(st.getLong("pos")),
                    GasChunkSummary.fromNbt(st.getCompound("data")));
        }
        ListTag snapshotTag = tag.getList("fine_snapshot", Tag.TAG_COMPOUND);
        for (int i = 0; i < snapshotTag.size(); i++) {
            CompoundTag ct = snapshotTag.getCompound(i);
            ChunkPos cp = new ChunkPos(ct.getLong("pos"));
            ListTag cells = ct.getList("cells", Tag.TAG_COMPOUND);
            Map<Long, GasMixture> chunkCells = new HashMap<>();
            for (int j = 0; j < cells.size(); j++) {
                CompoundTag cell = cells.getCompound(j);
                chunkCells.put(cell.getLong("pos"), GasCodec.readMixture(cell.getCompound("mix")));
            }
            manager.fineSnapshot.put(cp, chunkCells);
        }
        return manager;
    }

    /** 运行时绑定维度键（用于跨维度区分与调试） */
    public void bind(ResourceKey<Level> key) {
        this.dimensionKey = key;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        int mode = ChemConfig.GAS_PRECISION_MODE.get();
        if (mode == MODE_C_FULL_FINE) {
            // C：永不粗化——精存全部 fine（卸载也不坍缩，fine 常驻内存）
            tag.put("fine_snapshot", packSnapshot(fineGrid));
            return tag;
        }
        // A/B：写粗账（现有 + 把当前加载区 fine 坍缩进快照副本，不修改运行内存）
        Map<ChunkPos, GasChunkSummary> merged = new HashMap<>();
        for (Map.Entry<ChunkPos, GasChunkSummary> e : summaries.entrySet()) {
            merged.put(e.getKey(), e.getValue());
        }
        for (Map.Entry<BlockPos, GasMixture> cell : fineGrid.entrySet()) {
            BlockPos p = cell.getKey();
            ChunkPos cp = new ChunkPos(p);
            GasChunkSummary s = merged.computeIfAbsent(cp, c -> new GasChunkSummary());
            for (Map.Entry<IonType, Double> ce : cell.getValue().getContents().entrySet()) {
                s.absorb(ce.getKey(), ce.getValue(), p.getX() & 15, p.getY(), p.getZ() & 15,
                        cell.getValue().getTemperature());
            }
        }
        ListTag list = new ListTag();
        for (Map.Entry<ChunkPos, GasChunkSummary> e : merged.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            CompoundTag st = new CompoundTag();
            st.putLong("pos", e.getKey().toLong());
            st.put("data", e.getValue().toNbt());
            list.add(st);
        }
        tag.put("summaries", list);
        if (mode == MODE_B_HALF) {
            // B：额外把"当前仍加载"的区块按精数据保存（玩家常驻区精复原）
            tag.put("fine_snapshot", packSnapshot(fineGrid));
        }
        return tag;
    }

    /** 把指定精区打包成按区块分组的快照 NBT */
    private static ListTag packSnapshot(Map<BlockPos, GasMixture> source) {
        Map<ChunkPos, Map<Long, GasMixture>> byChunk = new HashMap<>();
        for (Map.Entry<BlockPos, GasMixture> e : source.entrySet()) {
            ChunkPos cp = new ChunkPos(e.getKey());
            byChunk.computeIfAbsent(cp, c -> new HashMap<>()).put(e.getKey().asLong(), e.getValue());
        }
        ListTag list = new ListTag();
        for (Map.Entry<ChunkPos, Map<Long, GasMixture>> e : byChunk.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            CompoundTag ct = new CompoundTag();
            ct.putLong("pos", e.getKey().toLong());
            ListTag cells = new ListTag();
            for (Map.Entry<Long, GasMixture> c : e.getValue().entrySet()) {
                CompoundTag cell = new CompoundTag();
                cell.putLong("pos", c.getKey());
                cell.put("mix", GasCodec.writeMixture(c.getValue()));
                cells.add(cell);
            }
            ct.put("cells", cells);
            list.add(ct);
        }
        return list;
    }

    // ==================== 精细场查询/写入 ====================

    /** 是否含某格气体 */
    public boolean hasGas(BlockPos pos) {
        return fineGrid.containsKey(pos);
    }

    /** 读取某格混合气（无则返回 null） */
    @Nullable
    public GasMixture get(BlockPos pos) {
        return fineGrid.get(pos);
    }

    /** 读取某格某气体 mol */
    public double getAmount(BlockPos pos, IonType ion) {
        GasMixture g = fineGrid.get(pos);
        return g != null ? g.getAmount(ion) : 0.0;
    }

    /** 取或建某格混合气（建时温度=环境温度）；仅已加载区块有效 */
    @Nullable
    /** 该格的总压（atm，由 nRT/V 导出，§19.27）；空格返回 0。只读查询（诊断/调试）。 */
    public double cellPressureAtm(BlockPos pos) {
        GasMixture gas = fineGrid.get(pos);
        if (gas == null || gas.isEmpty()) return 0;
        return GasPhysics.pressureAtm(gas.totalMoles(), GasPhysics.CELL_VOLUME_L, gas.getTemperature());
    }

    public GasMixture getOrCreate(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) return null;
        GasMixture g = fineGrid.get(pos);
        if (g == null) {
            g = new GasMixture(environmentTemperature(level, pos)
                    + heatOffset(level, pos, environmentTemperature(level, pos)));
            fineGrid.put(pos, g);
            markDirty(new ChunkPos(pos));
        }
        return g;
    }

    /** 向某格加入气体（mol）；自动建格/清理空格 */
    public void addGas(ServerLevel level, BlockPos pos, IonType ion, double moles) {
        if (moles <= 1e-12) return;
        GasMixture g = getOrCreate(level, pos);
        if (g == null) return;
        g.addGas(ion, moles);
        if (g.isEmpty()) {
            fineGrid.remove(pos);
        }
        markDirty(new ChunkPos(pos));
        setDirty();
    }

    /** 从某格移除气体，返回实际移除量 */
    public double removeGas(ServerLevel level, BlockPos pos, IonType ion, double moles) {
        GasMixture g = fineGrid.get(pos);
        if (g == null) return 0;
        double removed = g.removeGas(ion, moles);
        if (g.isEmpty()) {
            fineGrid.remove(pos);
        }
        markDirty(new ChunkPos(pos));
        if (removed > 0) setDirty();
        return removed;
    }

    // ==================== 脏标记（活性判定基础，§19.13） ====================

    public void markDirty(ChunkPos chunk) {
        dirtyChunks.put(chunk, System.currentTimeMillis());
    }

    public boolean isDirty(ChunkPos chunk) {
        return dirtyChunks.containsKey(chunk);
    }

    public void clearDirty(ChunkPos chunk) {
        dirtyChunks.remove(chunk);
    }

    // ==================== 区块加载/卸载（LOD 坍缩与水合） ====================

    /** 区块卸载（LOD：精区 → 粗账；档 C 例外不坍缩、精数据常驻内存） */
    public void onChunkUnload(ServerLevel level, ChunkPos chunkPos) {
        int mode = ChemConfig.GAS_PRECISION_MODE.get();
        if (mode == MODE_C_FULL_FINE) {
            // C：保留精格（退出存档时由 save() 全量精存 fineSnapshot）
            dirtyChunks.remove(chunkPos);
            return;
        }
        // A/B：坍缩该区块所有格 → 粗账
        List<BlockPos> toRemove = new ArrayList<>();
        GasChunkSummary summary = null;
        boolean any = false;
        for (Map.Entry<BlockPos, GasMixture> e : fineGrid.entrySet()) {
            BlockPos p = e.getKey();
            if (chunkPos.x == p.getX() >> 4 && chunkPos.z == p.getZ() >> 4) {
                if (summary == null) summary = new GasChunkSummary();
                for (Map.Entry<IonType, Double> ce : e.getValue().getContents().entrySet()) {
                    summary.absorb(ce.getKey(), ce.getValue(),
                            p.getX() & 15, p.getY(), p.getZ() & 15, e.getValue().getTemperature());
                }
                toRemove.add(p);
                any = true;
            }
        }
        if (any) {
            if (summary != null && !summary.isEmpty()) {
                summaries.put(chunkPos, summary);
            }
            fineGrid.keySet().removeAll(toRemove);
            fineSnapshot.remove(chunkPos); // 失效旧精存快照（已被坍缩覆盖）
            dirtyChunks.remove(chunkPos);
            setDirty();
        }
    }

    /** 区块加载：优先精存快照（B/C 的复原路径）→ 否则**排队**粗账水合（不在加载事件里访问世界） */
    public void onChunkLoad(ServerLevel level, ChunkPos chunkPos) {
        // 1) 精存快照（档 B 退出时保存的加载区 / 档 C 全量）：纯内存操作，可安全内联
        Map<Long, GasMixture> snapshot = fineSnapshot.remove(chunkPos);
        if (snapshot != null && !snapshot.isEmpty()) {
            for (Map.Entry<Long, GasMixture> e : snapshot.entrySet()) {
                fineGrid.put(BlockPos.of(e.getKey()), e.getValue());
            }
            setDirty();
            return;
        }
        // 2) 粗账水合：会读方块状态（热源扫描）→ 必须挪出区块加载事件，否则强制加载区块 → 自死锁
        if (summaries.containsKey(chunkPos)) {
            pendingHydrate.offer(chunkPos);
        }
    }

    /**
     * 在**服务端 tick** 中处理排队的水合（每 tick 有限预算，避免卡顿）。
     *
     * <p>为什么不在 `ChunkEvent.Load` 里直接水合：水合会调用 {@code redistribute → addGas → heatOffset}，
     * 其中热源扫描属于世界访问；在区块加载回调内触发同步区块加载会形成自死锁（历史事故）。</p>
     */
    public void processPendingHydration(ServerLevel level, int budget) {
        for (int i = 0; i < budget; i++) {
            ChunkPos cp = pendingHydrate.poll();
            if (cp == null) return;
            // 期间可能又被卸载：丢弃本次（粗账仍在 summaries 里，下次加载会重新排队）
            if (!level.isLoaded(cp.getWorldPosition())) continue;
            GasChunkSummary summary = summaries.remove(cp);
            if (summary == null || summary.isEmpty()) continue;
            int ox = cp.getMinBlockX();
            int oz = cp.getMinBlockZ();
            for (GasChunkSummary.Entry e : summary.getEntries().values()) {
                redistribute(level, ox, oz, e);
            }
            setDirty();
        }
    }

    /** 把单气体粗账按重心散布回 3³ 邻域（距离线性衰减），余量落重心格 */
    private void redistribute(ServerLevel level, int ox, int oz, GasChunkSummary.Entry e) {
        int cx = ox + (int) Math.floor(e.cx);
        int cy = (int) Math.floor(e.cy);
        int cz = oz + (int) Math.floor(e.cz);
        if (!level.isLoaded(new BlockPos(cx, cy, cz))) return; // 加载事件内理应已加载

        // 26 邻域（不含中心）按 1/距离 权重分配
        List<int[]> neighbors = new ArrayList<>();
        double weightSum = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    neighbors.add(new int[]{dx, dy, dz});
                    weightSum += 1.0 / dist;
                }
            }
        }
        double remaining = e.totalMoles;
        for (int[] off : neighbors) {
            double dist = Math.sqrt(off[0] * off[0] + off[1] * off[1] + off[2] * off[2]);
            double share = e.totalMoles * (1.0 / dist) / weightSum;
            if (share > 1e-9) {
                addGas(level, new BlockPos(cx + off[0], cy + off[1], cz + off[2]), e.ion, share);
                remaining -= share;
            }
        }
        if (remaining > 1e-9) {
            addGas(level, new BlockPos(cx, cy, cz), e.ion, remaining);
        }
    }

    // ==================== 扩散（9-3，菲克定律显式离散，§19.13） ====================

    /** 参考空气分子量（D ∝ 1/√M 归一化基准） */
    private static final double REF_MOLAR_MASS = 29.0;

    // ==================== 解析热源场（9-6 / §19.13） ====================

    /** 热源方块基准温度（K，与容器热源表同值） */
    private static final Map<Block, Double> HEAT_SOURCE_TEMPS = new HashMap<>();

    static {
        HEAT_SOURCE_TEMPS.put(Blocks.FIRE, 1300.0);
        HEAT_SOURCE_TEMPS.put(Blocks.SOUL_FIRE, 1200.0);
        HEAT_SOURCE_TEMPS.put(Blocks.LAVA, 1500.0);
        HEAT_SOURCE_TEMPS.put(Blocks.MAGMA_BLOCK, 1300.0);
        HEAT_SOURCE_TEMPS.put(Blocks.CAMPFIRE, 800.0);
        HEAT_SOURCE_TEMPS.put(Blocks.SOUL_CAMPFIRE, 750.0);
        HEAT_SOURCE_TEMPS.put(Blocks.TORCH, 400.0);
    }

    /** 热源影响衰减长度（格） */
    private static final double HEAT_LAMBDA = 2.5;
    /** 热场扫描半径（格） */
    private static final int HEAT_SCAN_RADIUS = 2;

    /**
     * 计算某格由周围热源叠加的温度抬升 δT = max( (T_源−T_环境) × exp(−d/λ) )。
     * 解析近似：只取最近/最强热源贡献（不逐格迭代导热，稳态场一次成型）。
     *
     * <p><b>绝对禁止</b>在这里用 {@code level.getBlockState()}：扫描会跨到相邻（可能未加载）区块，
     * 而本方法可能在 `ChunkEvent.Load` 内部被调用——强制同步加载区块会与"当前正在完成该区块加载的线程"
     * 形成**自死锁**（历史事故：旧存档加载卡死）。故一律走 {@link #stateNoLoad}。</p>
     */
    private static double heatOffset(Level level, BlockPos pos, double envTemp) {
        // §19.38 A2：**带符号**取"绝对值最大"的贡献。旧实现 `if (contrib > best)` 只保留正贡献，
        // 于是冰浴/干冰/液氮对气体完全无效；现在冷源能把气体降温（凝露/低压手感靠它）。
        double best = 0;
        int r = HEAT_SCAN_RADIUS;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    net.minecraft.world.level.block.state.BlockState state =
                            stateNoLoad(level, pos.offset(dx, dy, dz));
                    if (state == null) continue;
                    Double src = HEAT_SOURCE_TEMPS.get(state.getBlock());
                    if (src == null) continue;
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    double contrib = (src - envTemp) * Math.exp(-dist / HEAT_LAMBDA);
                    if (Math.abs(contrib) > Math.abs(best)) {
                        best = contrib;
                    }
                }
            }
        }

        // 本模组的热源（恒温浴 / 电加热夹套 / 创造热源）走**登记表**：不碰区块、不碰方块实体，
        // 因此依旧可以在 ChunkEvent.Load 里安全调用（§19.38 A2 / 历史自死锁事故）
        double tracked = HeatSourceTracker.contribution(level, pos, envTemp, HEAT_SCAN_RADIUS, HEAT_LAMBDA);
        if (Math.abs(tracked) > Math.abs(best)) best = tracked;
        return best;
    }

    /**
     * 读取方块状态但**绝不强制加载区块**：未加载 → null（视为无热源）。
     * 服务端用非阻塞的 {@code getChunkNow}；客户端退回 isLoaded 判断。
     */
    @Nullable
    private static net.minecraft.world.level.block.state.BlockState stateNoLoad(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            net.minecraft.world.level.chunk.LevelChunk chunk =
                    serverLevel.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            return chunk == null ? null : chunk.getBlockState(pos);
        }
        return level.isLoaded(pos) ? level.getBlockState(pos) : null;
    }

    /**
     * 执行一次扩散遍历（调用方节流，如每 N tick 一次）。
     * - 每对相邻格**只处理一次**（仅向 +X/+Y/+Z 三个正方向），杜绝双向重复迁移；
     * - 单源总流出封顶（每轮每格至多逸散 MAX_OUT_FRACTION），保证数值稳定、总量单调收敛；
     * - 微量阈值：迁移量过小忽略、新建格需足量（不产生"微量尾巴格"）；
     * - 尾部清扫：低于 MIN_CELL_MOLES 的格子移除。
     */
    public void tickDiffusion(ServerLevel level) {
        if (fineGrid.isEmpty()) return;

        record Transfer(BlockPos source, BlockPos target, IonType ion, double delta) {}
        List<Transfer> transfers = new ArrayList<>();
        Map<BlockPos, Double> outSum = new HashMap<>();

        // ===== 1) 收集迁移（每边一次：source 只向三个正方向邻居看） =====
        for (Map.Entry<BlockPos, GasMixture> cell : fineGrid.entrySet()) {
            BlockPos pos = cell.getKey();
            if (!level.isLoaded(pos)) continue;
            GasMixture gas = cell.getValue();
            if (gas.isEmpty()) continue;
            double envT = environmentTemperature(level, pos);
            double effT = envT + heatOffset(level, pos, envT);
            if (Math.abs(gas.getTemperature() - effT) > 0.5) {
                gas.setTemperature(effT);
            }
            double temp = effT;

            for (Map.Entry<IonType, Double> comp : gas.getContents().entrySet()) {
                IonType ion = comp.getKey();
                double n = comp.getValue();
                if (n < dissolveThreshold()) continue;
                double k = diffusionCoefficient(ion, temp);
                for (Direction d : Direction.values()) {
                    BlockPos nbrPos = pos.relative(d);
                    if (!level.isLoaded(nbrPos)) continue;
                    boolean nbrExists = fineGrid.containsKey(nbrPos);
                    // 每对相邻格只处理一次：负方向且邻格已存在 → 由对方（正方向看过来）处理
                    if (nbrExists && !isPositiveDirection(d)) continue;
                    double nN = nbrExists ? getAmount(nbrPos, ion) : 0.0;
                    double dm = k * (n - nN); // >0: source→nbr；<0: nbr→source（仅正向有气邻会出现负向）
                    if (Math.abs(dm) < MIN_TRANSFER) continue;
                    if (dm > 0) {
                        transfers.add(new Transfer(pos, nbrPos, ion, dm));
                        outSum.merge(pos, dm, Double::sum);
                    } else {
                        // 反向流入（邻格浓度高于本格，邻格在正方向必已存在）
                        transfers.add(new Transfer(nbrPos, pos, ion, -dm));
                        outSum.merge(nbrPos, -dm, Double::sum);
                    }
                }
            }
        }
        if (transfers.isEmpty()) return;

        // ===== 2) 单源封顶（每轮源格至多逸散其存量 MAX_OUT_FRACTION） =====
        Map<BlockPos, Double> sourceCap = new HashMap<>();
        for (BlockPos p : outSum.keySet()) {
            GasMixture g = fineGrid.get(p);
            if (g != null) {
                sourceCap.put(p, g.totalMoles() * MAX_OUT_FRACTION);
            }
        }
        // 按源缩放各源自己的流出
        Map<BlockPos, Double> applied = new HashMap<>();
        for (Transfer t : transfers) {
            double cap = sourceCap.getOrDefault(t.source(), Double.MAX_VALUE);
            double already = applied.getOrDefault(t.source(), 0.0);
            double totalOut = outSum.getOrDefault(t.source(), 0.0);
            double scale = cap <= 0 ? 0 : Math.min(1.0, cap / Math.max(totalOut, 1e-12));
            double delta = t.delta() * scale;
            applied.merge(t.source(), delta, Double::sum);
            applyTransfer(level, t.source(), t.target(), t.ion(), delta);
        }

        // ===== 3) 尾部清扫（微量格消散） =====
        sweepTinyCells();

        for (BlockPos p : applied.keySet()) {
            markDirty(new ChunkPos(p));
        }
        setDirty();
    }

    /** 单条迁移应用：源扣减、目标增加（目标不足 MIN_CELL 不建新格） */
    private void applyTransfer(ServerLevel level, BlockPos source, BlockPos target, IonType ion, double delta) {
        if (delta <= MIN_TRANSFER) return;
        GasMixture src = fineGrid.get(source);
        if (src == null) return;
        src.removeGas(ion, delta);
        if (src.isEmpty()) {
            fineGrid.remove(source);
        }
        GasMixture dst = fineGrid.get(target);
        if (dst != null) {
            dst.addGas(ion, delta);
            if (dst.isEmpty()) fineGrid.remove(target);
        } else if (delta >= dissolveThreshold()) {
            // 新格只接收足量气体（不产生 1e-9 尾巴）
            dst = new GasMixture(environmentTemperature(level, target));
            dst.addGas(ion, delta);
            fineGrid.put(target, dst);
        }
    }

    /** 清除总摩尔低于阈值的格子（防止数值尾巴堆积） */
    private void sweepTinyCells() {
        fineGrid.entrySet().removeIf(e -> e.getValue().totalMoles() < dissolveThreshold());
    }

    /** +X / +Y / +Z 为正方向（每条边由"正方向一侧"的格负责处理一次） */
    private static boolean isPositiveDirection(Direction d) {
        return d == Direction.EAST || d == Direction.UP || d == Direction.SOUTH;
    }

    /** 单源每轮最大逸散比例（< 1，保数值稳定） */
    private static final double MAX_OUT_FRACTION = 0.5;
    /** 单条迁移最小量（低于则忽略） */
    private static final double MIN_TRANSFER = 1e-9;

    /** 气体湮灭阈值（mol/格，ChemConfig.gasDissolveThreshold 可配）：低于视为消散 */
    public static double dissolveThreshold() {
        return ChemConfig.GAS_DISSOLVE_THRESHOLD.get();
    }

    /** 迁移系数（供网格扩散与容器泄漏共用）：基准(可配) × √(29/M)（格雷姆）× √(T/293) */
    public static double diffusionCoefficient(IonType ion, double tempK) {
        double molar = ion.getMolarMass() > 0 ? ion.getMolarMass() : REF_MOLAR_MASS;
        double massFactor = Math.sqrt(REF_MOLAR_MASS / molar);
        double tempFactor = Math.sqrt(Math.max(1, tempK) / 293.0);
        return ChemConfig.GAS_DIFFUSION_BASE.get() * massFactor * tempFactor;
    }

    // ==================== 工具 ====================

    /** 环境温度（K）：基准 293K − 高度递减（与容器体系一致） */
    public static double environmentTemperature(Level level, BlockPos pos) {
        double base = ChemConfig.ENVIRONMENT_TEMPERATURE_BASE.get();
        double lapse = Math.max(0, (pos.getY() - 64) / 100.0)
                * ChemConfig.ENVIRONMENT_TEMPERATURE_LAPSE_RATE.get();
        return Math.max(0, base - lapse);
    }

    public Map<BlockPos, GasMixture> getFineGrid() {
        return fineGrid;
    }

    public Map<ChunkPos, GasChunkSummary> getSummaries() {
        return summaries;
    }

    @Nullable
    public ResourceKey<Level> getDimensionKey() {
        return dimensionKey;
    }

    // 调试统计
    public int fineCellCount() {
        return fineGrid.size();
    }

    public int summaryCount() {
        return summaries.size();
    }

    // ==================== 查询 API（9-7，供污染/渲染/蒸馏/收集使用） ====================

    /** 清空全部气体数据（调试/故障恢复用） */
    public void reset() {
        fineGrid.clear();
        summaries.clear();
        fineSnapshot.clear();
        dirtyChunks.clear();
        setDirty();
    }

    /** 读取某区块粗账（可能为空）；用于污染/渲染的区块级查询 */
    @Nullable
    public GasChunkSummary getChunkSummary(ChunkPos chunkPos) {
        return summaries.get(chunkPos);
    }

    /**
     * 半径内某气体的总 mol（仅统计已加载的精细场；粗账区不展开）。
     * 玩家附近/污染评估用：粗账侧按区块调用 getChunkSummary 另行聚合。
     */
    public double queryGasAround(ServerLevel level, BlockPos center, int radius, IonType ion) {
        double sum = 0;
        int bx = center.getX(), by = center.getY(), bz = center.getZ();
        for (Map.Entry<BlockPos, GasMixture> e : fineGrid.entrySet()) {
            BlockPos p = e.getKey();
            int dx = p.getX() - bx, dy = p.getY() - by, dz = p.getZ() - bz;
            if (Math.abs(dx) > radius || Math.abs(dy) > radius || Math.abs(dz) > radius) continue;
            if (dx * dx + dy * dy + dz * dz > radius * radius) continue;
            sum += e.getValue().getAmount(ion);
        }
        return sum;
    }

    static {
        Main.LOGGER.debug("GasGridManager loaded");
    }
}
