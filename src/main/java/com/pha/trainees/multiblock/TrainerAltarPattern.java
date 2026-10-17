package com.pha.trainees.multiblock;

import com.pha.trainees.Main;
import com.pha.trainees.block.AbsorbBlock;
import com.pha.trainees.blockentity.AbsorbBlockEntity;
import com.pha.trainees.blockentity.AltarCoreBlockEntity;
import com.pha.trainees.blockentity.KunAltarBlockEntity;
import com.pha.trainees.recipe.TrainerAltarRecipe;
import com.pha.trainees.registry.ModBlocks;
import com.pha.trainees.registry.ModRecipes;
import com.pha.trainees.util.game.enums.AbsorbWorkModel;
import com.pha.trainees.util.game.BlockCourse;
import com.pha.trainees.util.game.EntityWay;
import com.pha.trainees.util.game.ParticleHelper;
import com.pha.trainees.util.game.Tools;
import com.pha.trainees.util.game.structure.*;
import com.pha.trainees.util.interfaces.IHoverText;
import com.pha.trainees.util.interfaces.ITraversal;
import com.pha.trainees.util.interfaces.TextSignals;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;


import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

/**
 * Trainer Altar 多方块结构
 */
@SuppressWarnings({"removal"})
public class TrainerAltarPattern implements IHoverText {
    public static final String STRUCTURE_ID = "trainees:trainer_altar";
    // 1 - north  2 - east  3 - south  4 - west
    // 掉落位置允许的范围（相对于核心方块 matchPos）
    private static final int MIN_X = -5;
    private static final int MAX_X = 5;
    private static final int MIN_Y = -2;
    private static final int MAX_Y = 5;
    private static final int MIN_Z = -5;
    private static final int MAX_Z = 5;

    public static void register(ResourceManager resourceManager) {
        // 1. 加载结构 NBT
        CompoundTag structureNbt;
        ResourceLocation nbtLoc = new ResourceLocation(Main.MODID, "structures/trainer_altar.nbt");

        try {
            // 使用 ResourceManager 获取结构 NBT（客户端与服务器共用，避免结构仅在客户端注册）
            Resource resource = resourceManager.getResource(nbtLoc)
                    .orElseThrow(() -> new FileNotFoundException("Structure nbt not found: " + nbtLoc));

            try (InputStream inputStream = resource.open()) { // 使用 open() 获取输入流
                // 使用 NbtIo.readCompressed 并传入 NbtAccounter
                structureNbt = NbtIo.readCompressed(inputStream);
            }
        } catch (Exception e) {
            Main.LOGGER.error("Failed to load trainer_altar structure NBT", e);
            return;
        }

        // 2. 解析 palette 和 blocks
        ListTag paletteTag = structureNbt.getList("palette", Tag.TAG_COMPOUND);
        ListTag blocksTag = structureNbt.getList("blocks", Tag.TAG_COMPOUND);

        // 获取空气的 palette 索引
        int airIndex = -1;
        for (int i = 0; i < paletteTag.size(); i++) {
            CompoundTag entry = paletteTag.getCompound(i);
            String name = entry.getString("Name");
            if ("minecraft:air".equals(name)) {
                airIndex = i;
                break;
            }
        }
        if (airIndex == -1) {
            Main.LOGGER.warn("No air found in palette, assuming index 2");
            airIndex = 2; // fallback
        }

        // 核心方块在 NBT 中的相对坐标（从你的NBT中固定为 (5,1,5)）
        BlockPos coreRelative = new BlockPos(5, 1, 5);

        // 3. 构建结构模式
        MultiblockPattern.Builder builder = new MultiblockPattern.Builder()
                .dimensions(11, 4, 11)
                .originOffset(BlockPos.ZERO)
                .matchPos(BlockPos.ZERO)
                .activationHandler(new TrainerAltarActivationHandler());

        // 遍历所有方块，添加条件
        for (int i = 0; i < blocksTag.size(); i++) {
            CompoundTag blockTag = blocksTag.getCompound(i);
            int state = blockTag.getInt("state");
            if (state == airIndex) continue; // 跳过空气

            ListTag posList = blockTag.getList("pos", Tag.TAG_INT);
            int x = posList.getInt(0);
            int y = posList.getInt(1);
            int z = posList.getInt(2);
            // 计算相对核心的偏移
            BlockPos relativePos = new BlockPos(x - coreRelative.getX(),
                    y - coreRelative.getY(),
                    z - coreRelative.getZ());

            // 获取该 state 对应的方块名称
            CompoundTag paletteEntry = paletteTag.getCompound(state);
            String blockName = paletteEntry.getString("Name");
            // 构建 IBlockPredicate
            IBlockPredicate predicate = createPredicateFromBlockName(blockName);
            if (predicate != null) {
                builder.addCondition(relativePos, predicate, true, blockName);
            } else {
                Main.LOGGER.warn("Unsupported block: {}", blockName);
            }
        }

        MultiblockPattern pattern = builder.build();
        MultiblockStructure.registerStructure(STRUCTURE_ID, pattern);
        Main.LOGGER.info("[TrainerAltar] Structure registered from NBT");
    }

    private static IBlockPredicate createPredicateFromBlockName(String blockName) {
        Block block = BuiltInRegistries.BLOCK.get(new ResourceLocation(blockName));
        if (block == Blocks.AIR) return null;
        return (level, pos, state) -> state.is(block);
    }

    /**
     * 激活处理器实现
     */
    private static class TrainerAltarActivationHandler implements IActivationHandler, ITraversal {

        // 祭坛配方缓存：datapack reload 会替换 RecipeManager 实例，故以实例引用判断失效
        private static List<TrainerAltarRecipe> cachedRecipes = null;
        private static RecipeManager cachedRecipeManager = null;

        @Override
        public void onActivate(Level level, BlockPos matchPos) {
            if (level.isClientSide) return;
            Main.LOGGER.info("Trainer Altar activated at {}", matchPos);
            double x = matchPos.getX(); double y = matchPos.getY(); double z = matchPos.getZ();
            Vec3 center = BlockCourse.getCenter(matchPos);
            double cx = center.x; double cy = center.y; double cz = center.z;

            // 记录激活状态
            ActiveStructureManager manager = ActiveStructureManager.get(level);
            BlockPos defaultDrop = matchPos.above(2);
            manager.addActiveStructure(level, STRUCTURE_ID, matchPos, defaultDrop);

            level.playSound(null, matchPos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);
            ParticleHelper.send(level, ParticleTypes.SOUL_FIRE_FLAME, cx, cy, cz, 200, 0.5, 0, 0.5, 0.15);
            ParticleHelper.send(level, ParticleTypes.FLAME, cx, cy, cz, 200, 0.5, 0, 0.5, 0.15);



            saveActivationData(level, matchPos);
        }

        @Override
        public void onActivationFailed(Level level, BlockPos matchPos, String reason) {
            double x = matchPos.getX(); double y = matchPos.getY(); double z = matchPos.getZ();
            // 给玩家反馈
            Player nearestPlayer = level.getNearestPlayer(x, y, z, 10, false);
            if (nearestPlayer != null) {
                nearestPlayer.displayClientMessage(
                        Component.literal("---!---：" + reason).withStyle(ChatFormatting.RED), true
                );
            }
        }

        @Override
        public void onLogicTick(Level level, BlockPos matchPos, long activeTime) {
            if (level.isClientSide) return;

            BlockPos altar1Pos = getAltarNorth(matchPos); BlockPos altar2Pos = getAltarSouth(matchPos);
            BlockPos altar3Pos = getAltarWest(matchPos); BlockPos altar4Pos = getAltarEast(matchPos);
            BlockPos[] altarPos = {altar1Pos, altar2Pos, altar3Pos, altar4Pos};

            if (!(level.getBlockEntity(altar1Pos) instanceof KunAltarBlockEntity altar1)) return;
            if (!(level.getBlockEntity(altar2Pos) instanceof KunAltarBlockEntity altar2)) return;
            if (!(level.getBlockEntity(altar3Pos) instanceof KunAltarBlockEntity altar3)) return;
            if (!(level.getBlockEntity(altar4Pos) instanceof KunAltarBlockEntity altar4)) return;

            ItemStack[] altarStacks = {
                    altar1.getStoredItem(), // 北
                    altar2.getStoredItem(), // 南
                    altar3.getStoredItem(), // 西
                    altar4.getStoredItem()  // 东
            };

            RecipeManager recipeManager = Objects.requireNonNull(level.getServer()).getRecipeManager();
            // 缓存配方列表，避免每 tick 全量拉取（RecipeManager 实例变化时重新加载）
            if (cachedRecipes == null || cachedRecipeManager != recipeManager) {
                cachedRecipeManager = recipeManager;
                cachedRecipes = recipeManager.getAllRecipesFor(ModRecipes.TRAINER_ALTAR_TYPE.get())
                        .stream()
                        .toList();
            }
            List<TrainerAltarRecipe> recipes = cachedRecipes;

            // 遍历配方
            for (TrainerAltarRecipe recipe : recipes) {
                if (recipe.matches(altarStacks[0], altarStacks[1], altarStacks[2], altarStacks[3])) {

                    ActiveStructureManager manager = ActiveStructureManager.get(level);
                    BlockPos dropPos = manager.getDropPos(level, matchPos);

                    // 1.掉落位置判断：位置为null或被其他方块占用，回退默认
                    if (dropPos == null) dropPos = matchPos.above(2);
                    BlockState dropState = level.getBlockState(dropPos);
                    boolean isAir = dropState.isAir();
                    boolean isAbsorb = dropState.getBlock() instanceof AbsorbBlock;
                    if (!isAir && !isAbsorb) {
                        dropPos = matchPos.above(2);
                        dropState = level.getBlockState(dropPos);
                        isAir = dropState.isAir();
                        isAbsorb = dropState.getBlock() instanceof AbsorbBlock;
                    }

                    // 2. 允许合成判断（根据工作模式）：若无法合成，直接返回，不清除祭坛物品
                    if (!canCraft(level, dropPos, isAbsorb, isAir)) return;

                    // 2.5 §17.1 增强层：强力配方要耗魔力（RE），从祭坛主方块的缓存里扣。
                    //     魔力不够 → 什么都不做、也不清物品（与 canCraft 失败同一纪律），并提醒玩家
                    int manaCost = recipe.getManaCost();
                    AltarCoreBlockEntity core = manaCost > 0 ? findCore(level, matchPos) : null;
                    if (manaCost > 0 && (core == null || core.getManaStored() < manaCost)) {
                        if (core != null) warnInsufficientMana(level, matchPos, manaCost, core.getManaStored());
                        return;
                    }

                    ItemStack resultItem = recipe.getResultItem().copy();

                    // 3. 操作
                    doCraft(level, matchPos, dropPos, altarPos, resultItem, isAbsorb, altar1, altar2, altar3, altar4);
                    // 3.5 合成成立后才扣魔力（合成失败不该吃掉魔力）
                    if (manaCost > 0) {
                        core.consumeMana(manaCost);
                    }
                    break;
                }
            }
        }

        /**
         * 结构里的魔力来源：**祭坛主方块**（§17.1，作者 2026-10-02：魔力存主方块）。
         *
         * <p>兼容两种结构原点约定：先看 {@code matchPos}，再看它上方一格。</p>
         */
        private static AltarCoreBlockEntity findCore(Level level, BlockPos matchPos) {
            if (level.getBlockEntity(matchPos) instanceof AltarCoreBlockEntity core) return core;
            if (level.getBlockEntity(matchPos.above()) instanceof AltarCoreBlockEntity core) return core;
            return null;
        }

        /** 上次"精华不足"提示的游戏时间：每条结构自己记，20 tick 内不重复提醒（防刷屏）。 */
        private long lastManaWarnGameTime = -1000L;

        /** 精炼精华不足时提醒附近玩家（动作条）。 */
        private void warnInsufficientMana(Level level, BlockPos pos, int need, int have) {
            long now = level.getGameTime();
            if (now - lastManaWarnGameTime < 20L) return;
            lastManaWarnGameTime = now;
            for (Player player : level.getEntitiesOfClass(Player.class,
                    new net.minecraft.world.phys.AABB(pos).inflate(8.0))) {
                player.displayClientMessage(
                        Component.translatable("message.trainees.altar.mana_lack", need, have), true);
            }
        }

        private void clearStoredItems(KunAltarBlockEntity... entities) {            for (KunAltarBlockEntity entity : entities) {
                entity.clearStoredItem();
            }
        }

        private boolean canCraft(Level level, BlockPos dropPos, boolean isAbsorb, boolean isAir) {
            // 空气位置：总是允许
            if (isAbsorb) {
                BlockEntity be = level.getBlockEntity(dropPos);
                if (be instanceof AbsorbBlockEntity absorb) {
                    AbsorbWorkModel model = absorb.getModel();
                    ItemStack stored = absorb.getStoredItem();
                    if (model == AbsorbWorkModel.HINDERING) {
                        // HINDERING: 仅当存储为空时才允许合成
                        return stored.isEmpty();
                    } else return model == AbsorbWorkModel.DROPPING; // DROPPING: 总是允许合成
                }
            } else return isAir;
            return false;
        }

        private void doCraft(Level level, BlockPos matchPos, BlockPos dropPos, BlockPos[] altarPos,
                            ItemStack resultItem, boolean isAbsorb,
                             KunAltarBlockEntity... entities) {
            if (isAbsorb) {
                BlockEntity be = level.getBlockEntity(dropPos);
                if (be instanceof AbsorbBlockEntity absorb) {
                    AbsorbWorkModel model = absorb.getModel();
                    ItemStack stored = absorb.getStoredItem();
                    if (model == AbsorbWorkModel.HINDERING) {
                        absorb.setStoredItem(resultItem);
                    } else if (model == AbsorbWorkModel.DROPPING) {
                        if (!stored.isEmpty()) {
                            if (ItemStack.isSameItemSameTags(stored, resultItem)) {
                                // 合并同类，按最大堆叠封顶，超出部分掉落（避免合成后超 64 堆叠）
                                int maxStack = stored.getMaxStackSize();
                                int newCount = stored.getCount() + resultItem.getCount();
                                int overflow = 0;
                                if (newCount > maxStack) {
                                    overflow = newCount - maxStack;
                                    newCount = maxStack;
                                }
                                ItemStack combined = stored.copy();
                                combined.setCount(newCount);
                                absorb.setStoredItem(combined);
                                if (overflow > 0) {
                                    ItemStack drop = resultItem.copy();
                                    drop.setCount(overflow);
                                    EntityWay.spawnItemEntity(level, dropPos, drop);
                                }
                            } else {
                                absorb.dropStoredItem();
                                absorb.setStoredItem(resultItem);
                            }
                        } else {
                            absorb.setStoredItem(resultItem);
                        }
                    }
                    int[] random = {Tools.randomInRange(level, 1, 2), Tools.randomInRange(level, 1, 2),
                            Tools.randomInRange(level, 1, 2), Tools.randomInRange(level, 1, 2)};
                    for (int i = 0; i <= 3; i++) {
                        ParticleHelper.spawnArcParticle(level,
                                random[i] == 1 ? ParticleTypes.FLAME : ParticleTypes.SOUL_FIRE_FLAME,
                                BlockCourse.getCenter(altarPos[i]),
                                BlockCourse.getCenter(dropPos)
                        );
                    }
                }
            } else { // isAir
                EntityWay.spawnItemEntity(level, dropPos, resultItem);
                Vec3 center = BlockCourse.getCenter(matchPos);
                double cx = center.x; double cy = center.y; double cz = center.z;
                ParticleHelper.send(level, ParticleTypes.FLAME, cx, cy, cz, 25, 0.5, 0.5, 0.5, 0.1);
            }
            clearStoredItems(entities);
            level.playSound(null, matchPos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.6F, 1.0F);
        }

        @Override
        public BlockPos getAltarNorth(BlockPos pos) {
            return pos.offset(0, 2, -3);
        }

        @Override
        public BlockPos getAltarSouth(BlockPos pos) {
            return pos.offset(0, 2, 3);
        }

        @Override
        public BlockPos getAltarWest(BlockPos pos) {
            return pos.offset(-3, 2, 0);
        }

        @Override
        public BlockPos getAltarEast(BlockPos pos) {
            return pos.offset(3, 2, 0);
        }


        @Override
        public void onEffectTick(Level level, BlockPos matchPos, long activeTime) {
            if (level.isClientSide) return;
            Vec3 pos = BlockCourse.getCenter(matchPos);
            double x = pos.x; double y = pos.y; double z = pos.z;
            ParticleHelper.send(
                    level, ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 5, 3, 1, 3, 0.01
            );
            ParticleHelper.send(
                    level, ParticleTypes.FLAME, x, y, z, 5, 3, 1, 3, 0.01
            );
        }

        @Override
        public int getLogicTickInterval() {
            return 1; // 每 tick 检测合成
        }

        @Override
        public int getEffectTickInterval() {
            return 40; // 每 40 tick 显示一次粒子
        }

        @Override
        public void onStructureBroken(Level level, BlockPos matchPos) {
            Main.LOGGER.info("Trainer Altar structure broken at {}", matchPos);

            // 播放破坏音效
            level.playSound(null, matchPos,
                    SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.BLOCKS,
                    1.0F, 1.0F);

            // 通知玩家
            Player nearestPlayer = level.getNearestPlayer(matchPos.getX(), matchPos.getY(), matchPos.getZ(), 10, false);
            if (nearestPlayer != null) {
                nearestPlayer.displayClientMessage(TextSignals.K.component().withStyle(ChatFormatting.RED), true);
            }
        }

        /**
         * 保存激活数据
         */
        private void saveActivationData(Level level, BlockPos matchPos) {
            // 可以使用持久化数据保存激活状态
            // 或者使用方块实体存储数据
            // 这里只是一个示例
            if (level.getBlockEntity(matchPos) != null) {
                // 设置方块实体的激活状态
            }
        }

        // 检查位置是否在允许范围内
        public static boolean isInRange(BlockPos matchPos, BlockPos pos) {
            int dx = pos.getX() - matchPos.getX();
            int dy = pos.getY() - matchPos.getY();
            int dz = pos.getZ() - matchPos.getZ();
            return dx >= MIN_X && dx <= MAX_X &&
                    dy >= MIN_Y && dy <= MAX_Y &&
                    dz >= MIN_Z && dz <= MAX_Z;
        }

        // 检查位置是否合法（空气或 AbsorbBlock）
        public static boolean isValidDropPos(Level level, BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            return state.isAir() || state.getBlock() instanceof AbsorbBlock;
        }
    }

    /**
     * 设置指定祭坛的物品掉落位置
     * @param level 世界
     * @param matchPos 核心方块位置
     * @param newDropPos 新的掉落位置
     * @return 是否设置成功（位置必须在允许范围内且合法）
     */
    public static boolean setDropPosition(Level level, BlockPos matchPos, BlockPos newDropPos) {
        if (!TrainerAltarActivationHandler.isInRange(matchPos, newDropPos) ||
                !TrainerAltarActivationHandler.isValidDropPos(level, newDropPos)) {
            return false;
        }
        ActiveStructureManager manager = ActiveStructureManager.get(level);
        return manager.setDropPos(level, matchPos, newDropPos);
    }
}

