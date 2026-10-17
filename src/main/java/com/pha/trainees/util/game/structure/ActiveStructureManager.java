package com.pha.trainees.util.game.structure;

import com.pha.trainees.Main;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理激活的多方块结构
 */
@SuppressWarnings({"removal"})
@Mod.EventBusSubscriber(modid = Main.MODID)
/**
 * 已激活结构的存档记录（§17.1 的"B 方案"）。
 *
 * @deprecated 正在逐步弃用。理由（作者 2026-10-03 确认）：这张注册表不感知成员被破坏、
 *     不感知区块卸载，且管理器是静态单例（跨存档会缓存旧实例），必须靠启动自愈兜底；
 *     而"每 20 tick 由控制器重算结构"（A 方案）永远不会与存档不一致。
 *     新代码用 {@code MultiblockStructure#isFormedNow}；现有调用点（只因祭坛 / 只因棒 /
 *     破坏事件 / 等级事件）保持可用，迁移单独排期并需要上机验证，不做盲改。
 */
@Deprecated
public class ActiveStructureManager extends SavedData {

    private static final String DATA_NAME = "trainees_active_structures";
    private final Object modificationLock = new Object();

    // 存储激活的结构：维度ID -> 结构ID -> 位置列表
    private final Map<ResourceLocation, Map<String, Set<ActiveStructureData>>> activeStructures = new ConcurrentHashMap<>();

    // 冷却时间（游戏刻）
    private static final int REACTIVATE_COOLDOWN = 100; // 5秒

    // 单例实例
    private static ActiveStructureManager INSTANCE;

    private ActiveStructureManager() {
        super();
    }

    /**
     * 获取管理器实例
     */
    public static ActiveStructureManager get(Level level) {
        if (level.isClientSide) {
            throw new RuntimeException("Cannot access ActiveStructureManager from client side");
        }

        if (INSTANCE != null) {
            return INSTANCE;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        DimensionDataStorage storage = serverLevel.getServer().overworld().getDataStorage();
        INSTANCE = storage.computeIfAbsent(ActiveStructureManager::load, ActiveStructureManager::new, DATA_NAME);

        return INSTANCE;
    }

    public boolean addActiveStructure(Level level, String structureId, BlockPos matchPos) {
        return addActiveStructure(level, structureId, matchPos, null);
    }

    /**
     * 添加激活的结构
     */
    public boolean addActiveStructure(Level level, String structureId, BlockPos matchPos, BlockPos dropPos) {
        ResourceLocation dimension = level.dimension().location();

        // 检查是否已经激活
        if (isPositionActive(level, structureId, matchPos)) {
            Main.LOGGER.debug("Structure {} already active at {} in {}", structureId, matchPos, dimension);
            return false;
        }

        // 获取结构模式
        MultiblockPattern pattern = MultiblockStructure.REGISTERED_STRUCTURES.get(structureId);
        if (pattern == null) {
            Main.LOGGER.error("Unknown structure id: {}", structureId);
            return false;
        }

        // 创建激活数据
        ActiveStructureData data = new ActiveStructureData(
                structureId,
                matchPos,
                System.currentTimeMillis(),
                level.getGameTime(),
                dropPos
        );

        // 存储数据
        activeStructures
                .computeIfAbsent(dimension, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(structureId, k -> ConcurrentHashMap.newKeySet())
                .add(data);

        Main.LOGGER.info("Activated structure {} at {} in {}", structureId, matchPos, dimension);
        setDirty();
        return true;
    }

    /**
     * 移除激活的结构
     */
    public boolean removeActiveStructure(Level level, String structureId, BlockPos matchPos) {
        ResourceLocation dimension = level.dimension().location();

        Map<String, Set<ActiveStructureData>> dimensionMap = activeStructures.get(dimension);
        if (dimensionMap == null) return false;

        Set<ActiveStructureData> structureSet = dimensionMap.get(structureId);
        if (structureSet == null) return false;

        boolean removed = structureSet.removeIf(data -> data.matchPos.equals(matchPos));

        // 清理空集合
        if (structureSet.isEmpty()) {
            dimensionMap.remove(structureId);
        }
        if (dimensionMap.isEmpty()) {
            activeStructures.remove(dimension);
        }

        if (removed) {
            Main.LOGGER.info("Deactivated structure {} at {} in {}", structureId, matchPos, dimension);
            setDirty();

            // 调用取消激活的回调
            MultiblockPattern pattern = MultiblockStructure.REGISTERED_STRUCTURES.get(structureId);
            if (pattern != null && pattern.getActivationHandler() != null) {
                pattern.getActivationHandler().onStructureBroken(level, matchPos);
            }
        }

        return removed;
    }

    /**
     * 检查位置是否已激活
     */
    public boolean isPositionActive(Level level, String structureId, BlockPos matchPos) {
        ResourceLocation dimension = level.dimension().location();
        Map<String, Set<ActiveStructureData>> dimensionMap = activeStructures.get(dimension);
        if (dimensionMap == null) return false;
        Set<ActiveStructureData> structureSet = dimensionMap.get(structureId);
        if (structureSet == null) return false;
        return structureSet.stream().anyMatch(data -> data.matchPos.equals(matchPos));
    }

    /**
     * 线程安全地添加激活结构
     */
    public boolean addActiveStructureSafely(Level level, String structureId, BlockPos matchPos) {
        synchronized (modificationLock) {
            return addActiveStructure(level, structureId, matchPos);
        }
    }

    /**
     * 线程安全地移除激活结构
     */
    public boolean removeActiveStructureSafely(Level level, String structureId, BlockPos matchPos) {
        synchronized (modificationLock) {
            return removeActiveStructure(level, structureId, matchPos);
        }
    }

    /**
     * 线程安全地批量更新激活结构
     */
    public void updateActiveStructuresSafely(Level level) {
        synchronized (modificationLock) {
            updateActiveStructures(level);
        }
    }

//    /**
//     * 检查结构是否应该被激活（考虑冷却时间）
//     */
//    public boolean shouldActivate(Level level, String structureId, BlockPos matchPos) {
//        // 如果位置已激活，检查是否允许重新激活
//        return !isPositionActive(level, structureId, matchPos);
//    }

    /**
     * 获取所有激活的结构数据
     */
    public List<ActiveStructureData> getActiveStructures(Level level, @Nullable String structureId) {
        ResourceLocation dimension = level.dimension().location();
        List<ActiveStructureData> result = new ArrayList<>();

        Map<String, Set<ActiveStructureData>> dimensionMap = activeStructures.get(dimension);
        if (dimensionMap == null) return result;

        if (structureId != null) {
            Set<ActiveStructureData> structureSet = dimensionMap.get(structureId);
            if (structureSet != null) {
                result.addAll(structureSet);
            }
        } else {
            dimensionMap.values().forEach(result::addAll);
        }

        return result;
    }

    /**
     * 更新所有激活的结构
     */
    public void updateActiveStructures(Level level) {
        ResourceLocation dimension = level.dimension().location();
        Map<String, Set<ActiveStructureData>> dimensionMap = activeStructures.get(dimension);

        if (dimensionMap == null) return;

        long currentTime = System.currentTimeMillis();
        long currentTick = level.getGameTime();

        // 收集需要移除的结构数据
        List<RemovalEntry> removalsToProcess = new ArrayList<>();

        for (Map.Entry<String, Set<ActiveStructureData>> entry : dimensionMap.entrySet()) {
            String structureId = entry.getKey();
            Set<ActiveStructureData> structureSet = entry.getValue();
            MultiblockPattern pattern = MultiblockStructure.REGISTERED_STRUCTURES.get(structureId);
            if (pattern == null || pattern.getActivationHandler() == null) {
                // 标记需要移除整个结构集合
                removalsToProcess.add(new RemovalEntry(structureId, null));
                continue;
            }

            IActivationHandler handler = pattern.getActivationHandler();
            int logicInterval = handler.getLogicTickInterval();
            int effectInterval = handler.getEffectTickInterval();
            // 检查该结构的所有激活实例
            List<ActiveStructureData> toRemove = new ArrayList<>();

            for (ActiveStructureData data : structureSet) {
                try {
                    // 检查结构是否仍然完整
                    BlockPos originPos = calculateOriginFromMatch(data.matchPos, pattern);
//                    MultiblockPattern.MatchResult match = pattern.check(level, originPos);
                    MultiblockPattern.SafeMatchResult safeMatch = pattern.checkSafe(level, originPos);
                    if (safeMatch == null || !safeMatch.success) {
                        // 结构不完整或检查出错，标记为需要移除
                        toRemove.add(data);
                        Main.LOGGER.info("Structure {} broken at {} (reason: {})",
                                structureId, data.matchPos,
                                safeMatch != null ? safeMatch.error : "check returned null");
                        continue;
                    }

                    // 更新激活时间
                    // 逻辑 tick
                    if (logicInterval > 0 && currentTick - data.lastLogicTick >= logicInterval) {
                        handler.onLogicTick(level, data.matchPos, currentTick - data.activationTime);
                        data.lastLogicTick = currentTick;
                    }
                    // 效果 tick
                    if (effectInterval > 0 && currentTick - data.lastEffectTick >= effectInterval) {
                        handler.onEffectTick(level, data.matchPos, currentTick - data.activationTime);
                        data.lastEffectTick = currentTick;
                    }
                    setDirty();

                } catch (Exception e) {
                    Main.LOGGER.error("Error updating active structure {} at {}", structureId, data.matchPos, e);
                    // 出错时也标记为需要移除，避免无限出错
                    toRemove.add(data);
                }
            }

            // 批量移除该结构中需要删除的数据
            if (!toRemove.isEmpty()) {
                structureSet.removeAll(toRemove);
                setDirty();

                // 调用取消激活的回调
                for (ActiveStructureData data : toRemove) {
                    try {
                        pattern.getActivationHandler().onStructureBroken(level, data.matchPos);
                    } catch (Exception e) {
                        Main.LOGGER.error("Error calling onStructureBroken for {} at {}",
                                structureId, data.matchPos, e);
                    }
                }
            }

            // 如果该结构的激活集合为空，标记为需要移除整个结构条目
            if (structureSet.isEmpty()) {
                removalsToProcess.add(new RemovalEntry(structureId, null));
            }
        }

        // 处理需要移除的条目（在迭代完成后）
        for (RemovalEntry removal : removalsToProcess) {
            if (removal.structureId != null && removal.data == null) {
                // 移除整个结构集合
                dimensionMap.remove(removal.structureId);
                Main.LOGGER.debug("Removed empty structure set: {}", removal.structureId);
            }
        }

        // 清理空维度
        if (dimensionMap.isEmpty()) {
            activeStructures.remove(dimension);
            Main.LOGGER.debug("Removed empty dimension: {}", dimension);
        }
    }

    // 移除条目辅助类
    private static class RemovalEntry {
        final String structureId;
        final ActiveStructureData data;

        RemovalEntry(String structureId, ActiveStructureData data) {
            this.structureId = structureId;
            this.data = data;
        }
    }

    /**
     * 清理无效的激活结构数据
     */
    public void cleanupInvalidStructures(Level level) {
        ResourceLocation dimension = level.dimension().location();
        Map<String, Set<ActiveStructureData>> dimensionMap = activeStructures.get(dimension);

        if (dimensionMap == null) return;

        int removedCount = 0;
        List<String> structuresToRemove = new ArrayList<>();

        for (Map.Entry<String, Set<ActiveStructureData>> entry : dimensionMap.entrySet()) {
            String structureId = entry.getKey();
            Set<ActiveStructureData> structureSet = entry.getValue();

            // 清理无效的数据
            Iterator<ActiveStructureData> iterator = structureSet.iterator();
            while (iterator.hasNext()) {
                ActiveStructureData data = iterator.next();
                if (data == null || data.matchPos == null) {
                    iterator.remove();
                    removedCount++;
                    setDirty();
                }
            }

            // 如果集合为空，标记为需要移除
            if (structureSet.isEmpty()) {
                structuresToRemove.add(structureId);
            }
        }

        // 移除空的结构集合
        for (String structureId : structuresToRemove) {
            dimensionMap.remove(structureId);
            removedCount++;
        }

        // 如果维度为空，移除维度
        if (dimensionMap.isEmpty()) {
            activeStructures.remove(dimension);
        }

        if (removedCount > 0) {
            Main.LOGGER.info("Cleaned up {} invalid structure entries in dimension {}", removedCount, dimension);
        }
    }

    // 定期调用清理方法（例如在服务器启动时）
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        event.getServer().getAllLevels().forEach(level -> {
            ActiveStructureManager manager = get(level);
            manager.cleanupInvalidStructures(level);
        });
    }

    /**
     * 根据匹配位置计算原点位置（需要根据你的结构定义调整）
     */
    private BlockPos calculateOriginFromMatch(BlockPos matchPos, MultiblockPattern pattern) {
        // 这里需要根据你的结构定义来计算
        // 例如：如果matchPos是核心方块，而原点是核心方块下方一格
        // 需要根据pattern的originOffset和matchPos来计算
        return matchPos.subtract(pattern.getMatchPos());// 示例：假设原点是核心下方一格
    }

    /**
     * 服务器tick事件
     */
    // 修复的服务器tick事件处理方法
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            try {
                // 更新所有维度的激活结构
                event.getServer().getAllLevels().forEach(level -> {
                    try {
                        ActiveStructureManager manager = get(level);
                        manager.updateActiveStructures(level);
                    } catch (Exception e) {
                        Main.LOGGER.error("Error updating active structures for level {}", level.dimension().location(), e);
                    }
                });
            } catch (Exception e) {
                Main.LOGGER.error("Error in onServerTick", e);
            }
        }
    }

    // NBT保存/加载
    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag dimensionsList = new ListTag();

        for (Map.Entry<ResourceLocation, Map<String, Set<ActiveStructureData>>> dimensionEntry : activeStructures.entrySet()) {
            CompoundTag dimensionTag = new CompoundTag();
            dimensionTag.putString("Dimension", dimensionEntry.getKey().toString());

            ListTag structuresList = new ListTag();
            for (Map.Entry<String, Set<ActiveStructureData>> structureEntry : dimensionEntry.getValue().entrySet()) {
                for (ActiveStructureData data : structureEntry.getValue()) {
                    structuresList.add(data.serializeNBT());
                }
            }

            dimensionTag.put("Structures", structuresList);
            dimensionsList.add(dimensionTag);
        }

        tag.put("Dimensions", dimensionsList);
        return tag;
    }

    public static ActiveStructureManager load(CompoundTag tag) {
        ActiveStructureManager manager = new ActiveStructureManager();

        if (tag.contains("Dimensions", Tag.TAG_LIST)) {
            ListTag dimensionsList = tag.getList("Dimensions", Tag.TAG_COMPOUND);

            for (int i = 0; i < dimensionsList.size(); i++) {
                CompoundTag dimensionTag = dimensionsList.getCompound(i);
                ResourceLocation dimension = new ResourceLocation(dimensionTag.getString("Dimension"));

                if (dimensionTag.contains("Structures", Tag.TAG_LIST)) {
                    ListTag structuresList = dimensionTag.getList("Structures", Tag.TAG_COMPOUND);

                    for (int j = 0; j < structuresList.size(); j++) {
                        CompoundTag structureTag = structuresList.getCompound(j);
                        ActiveStructureData data = ActiveStructureData.deserializeNBT(structureTag);

                        manager.activeStructures
                                .computeIfAbsent(dimension, k -> new ConcurrentHashMap<>())
                                .computeIfAbsent(data.structureId, k -> ConcurrentHashMap.newKeySet())
                                .add(data);
                    }
                }
            }
        }

        Main.LOGGER.info("Loaded {} active structures",
                manager.activeStructures.values().stream()
                        .flatMap(m -> m.values().stream())
                        .mapToInt(Set::size)
                        .sum());

        return manager;
    }

    public boolean setDropPos(Level level, BlockPos matchPos, BlockPos newDropPos) {
        ResourceLocation dimension = level.dimension().location();
        Map<String, Set<ActiveStructureData>> dimensionMap = activeStructures.get(dimension);
        if (dimensionMap == null) return false;
        for (Set<ActiveStructureData> set : dimensionMap.values()) {
            for (ActiveStructureData data : set) {
                if (data.matchPos.equals(matchPos)) {
                    data.dropPos = newDropPos;
                    setDirty();
                    return true;
                }
            }
        }
        return false;
    }

    // 获取 dropPos 的方法
    public BlockPos getDropPos(Level level, BlockPos matchPos) {
        ResourceLocation dimension = level.dimension().location();
        Map<String, Set<ActiveStructureData>> dimensionMap = activeStructures.get(dimension);
        if (dimensionMap == null) return null;
        for (Set<ActiveStructureData> set : dimensionMap.values()) {
            for (ActiveStructureData data : set) {
                if (data.matchPos.equals(matchPos)) {
                    return data.dropPos;
                }
            }
        }
        return null;
    }

    /**
     * 激活结构数据类
     */
    public static class ActiveStructureData {
        public final String structureId;
        public final BlockPos matchPos;
        public final long activationTime;
        public long lastLogicTick;
        public long lastEffectTick;
        public BlockPos dropPos; // 新增：物品掉落位置

        public ActiveStructureData(String structureId, BlockPos matchPos, long activationTime, long currentTick, BlockPos dropPos) {
            this.structureId = structureId;
            this.matchPos = matchPos;
            this.activationTime = activationTime;
            this.lastLogicTick = currentTick;
            this.lastEffectTick = currentTick;
            this.dropPos = dropPos;
        }

        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putString("StructureId", structureId);
            tag.put("MatchPos", NbtUtils.writeBlockPos(matchPos));
            tag.putLong("ActivationTime", activationTime);
            tag.putLong("LastLogicTick", lastLogicTick);
            tag.putLong("LastEffectTick", lastEffectTick);
            if (dropPos != null) {
                tag.put("DropPos", NbtUtils.writeBlockPos(dropPos));
            }
            return tag;
        }

        public static ActiveStructureData deserializeNBT(CompoundTag tag) {
            String structureId = tag.getString("StructureId");
            BlockPos matchPos = NbtUtils.readBlockPos(tag.getCompound("MatchPos"));
            long activationTime = tag.getLong("ActivationTime");
            long lastLogicTick = tag.getLong("LastLogicTick");
            long lastEffectTick = tag.getLong("LastEffectTick");
            BlockPos dropPos = tag.contains("DropPos") ? NbtUtils.readBlockPos(tag.getCompound("DropPos")) : null;
            ActiveStructureData data = new ActiveStructureData(structureId, matchPos, activationTime, lastLogicTick, dropPos);
            data.lastEffectTick = lastEffectTick;
            return data;
        }
    }
}
