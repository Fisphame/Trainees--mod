package com.pha.trainees.util.game;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 模组兼容性工具类 - 用于安全地获取其他模组的注册对象
 */
public class ModGet {

    // 缓存以提高性能
    private static final Map<ResourceLocation, Optional<Item>> ITEM_CACHE = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Optional<Block>> BLOCK_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> MOD_LOADED_CACHE = new ConcurrentHashMap<>();

    /**
     * 检查模组是否已加载
     * @param modId 模组ID
     * @return 是否已加载
     */
    public static boolean isModLoaded(String modId) {
        return MOD_LOADED_CACHE.computeIfAbsent(modId, ModList.get()::isLoaded);
    }

    /**
     * 安全地获取物品
     * @param modId 模组ID
     * @param itemId 物品ID
     * @return Optional包裹的物品
     */
    public static Optional<Item> getItem(String modId, String itemId) {
        ResourceLocation rl = new ResourceLocation(modId, itemId);
        return ITEM_CACHE.computeIfAbsent(rl, key ->
                Optional.ofNullable(ForgeRegistries.ITEMS.getValue(key))
        );
    }

    public static Item getItem(Optional<Item> optional) {
        return optional.orElse(Items.AIR);
    }
    public static Block getBlock(Optional<Block> optional) {
        return optional.orElse(Blocks.AIR);
    }

    /**
     * 安全地获取方块
     * @param modId 模组ID
     * @param blockId 方块ID
     * @return Optional包裹的方块
     */
    public static Optional<Block> getBlock(String modId, String blockId) {
        ResourceLocation rl = new ResourceLocation(modId, blockId);
        return BLOCK_CACHE.computeIfAbsent(rl, key ->
                Optional.ofNullable(ForgeRegistries.BLOCKS.getValue(key))
        );
    }
    /**
     * 获取方块状态
     * @param modId 模组ID
     * @param blockId 方块ID
     * @return Optional包裹的默认方块状态
     */
    public static Optional<BlockState> getBlockState(String modId, String blockId) {
        return getBlock(modId, blockId).map(Block::defaultBlockState);
    }
    /**
     * 通用方法：从Forge注册表获取对象
     * @param registry Forge注册表
     * @param modId 模组ID
     * @param id 对象ID
     * @param <T> 对象类型
     * @return Optional包裹的对象
     */
    public static <T> Optional<T> getFromForgeRegistry(IForgeRegistry<T> registry, String modId, String id) {
        if (registry == null) {
            return Optional.empty();
        }
        ResourceLocation rl = new ResourceLocation(modId, id);
        return Optional.ofNullable(registry.getValue(rl));
    }
    /**
     * 获取实体类型
     * @param modId 模组ID
     * @param entityId 实体ID
     * @return Optional包裹的实体类型
     */
    public static Optional<EntityType<?>> getEntityType(String modId, String entityId) {
        ResourceLocation rl = new ResourceLocation(modId, entityId);
        return Optional.ofNullable(ForgeRegistries.ENTITY_TYPES.getValue(rl));
    }

    /**
     * 获取药水效果
     * @param modId 模组ID
     * @param effectId 效果ID
     * @return Optional包裹的药水效果
     */
    public static Optional<MobEffect> getMobEffect(String modId, String effectId) {
        ResourceLocation rl = new ResourceLocation(modId, effectId);
        return Optional.ofNullable(ForgeRegistries.MOB_EFFECTS.getValue(rl));
    }

    /**
     * 延迟获取物品（用于静态字段初始化）
     * @param modId 模组ID
     * @param itemId 物品ID
     * @return Supplier包裹的物品
     */
    public static Supplier<Item> lazyItem(String modId, String itemId) {
        return () -> getItem(modId, itemId).orElse(null);
    }

    /**
     * 延迟获取方块（用于静态字段初始化）
     * @param modId 模组ID
     * @param blockId 方块ID
     * @return Supplier包裹的方块
     */
    public static Supplier<Block> lazyBlock(String modId, String blockId) {
        return () -> getBlock(modId, blockId).orElse(null);
    }

    /**
     * 检查物品是否存在
     * @param modId 模组ID
     * @param itemId 物品ID
     * @return 是否存在
     */
    public static boolean doesItemExist(String modId, String itemId) {
        return getItem(modId, itemId).isPresent();
    }

    /**
     * 检查方块是否存在
     * @param modId 模组ID
     * @param blockId 方块ID
     * @return 是否存在
     */
    public static boolean doesBlockExist(String modId, String blockId) {
        return getBlock(modId, blockId).isPresent();
    }

    /**
     * 获取所有注册物品的ResourceLocation
     * @param modId 模组ID（null表示所有模组）
     * @return ResourceLocation列表
     */
    public static List<ResourceLocation> getAllItemLocations(@Nullable String modId) {
        List<ResourceLocation> result = new ArrayList<>();
        for (Map.Entry<ResourceKey<Item>, Item> entry : BuiltInRegistries.ITEM.entrySet()) {
            ResourceLocation location = entry.getKey().location();
            if (modId == null || modId.equals(location.getNamespace())) {
                result.add(location);
            }
        }
        return result;
    }

    /**
     * 清除缓存（谨慎使用，通常不需要）
     */
    public static void clearCache() {
        ITEM_CACHE.clear();
        BLOCK_CACHE.clear();
        MOD_LOADED_CACHE.clear();
    }

    /**
     * 预加载常用物品到缓存
     */
    public static void preloadCommonItems() {

    }
}
