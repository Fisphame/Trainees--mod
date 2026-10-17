package com.pha.trainees.blockentity;

import com.pha.trainees.block.ManaExtractorBlock;
import com.pha.trainees.mana.IManaStorage;
import com.pha.trainees.mana.ManaCapabilities;
import com.pha.trainees.mana.ManaStorage;
import com.pha.trainees.mana.TierMath;
import com.pha.trainees.recipe.ExtractionRecipe;
import com.pha.trainees.registry.ModBlocks;
import com.pha.trainees.registry.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 提取机方块实体（§17.1）：按期把输入物品榨成 RE，可能掉副产物。
 *
 * <p><b>档位来自方块本身</b>（5 个注册方块 = 5 个档位），输出的能量、副产物概率、耗时都按
 * {@link TierMath} 从配方的**初级基准值**换算。</p>
 *
 * <p><b>能量口径</b>（作者 2026-10-02）：启动成本**一次性付**且**与档位无关**；
 * 产出的能量进本机缓存，玩家用潜行+R 抽走，或由后续的端口/总线抽走。
 * 本机自身就是一个 {@code IManaStorage} 方块，所以 Jade 上直接能看到它的缓存。</p>
 */
public class ManaExtractorBlockEntity extends BlockEntity {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT_A = 1;
    public static final int SLOT_OUTPUT_B = 2;
    public static final int SLOT_COUNT = 3;

    private static final String NBT_MANA = "Mana";
    private static final String NBT_CAPACITY = "Capacity";
    private static final String NBT_PROGRESS = "Progress";
    private static final String NBT_ACTIVE = "Active";

    /** 配方表缓存：RecipeManager 实例变了才重取（与祭坛同一套做法，避免每 tick 全量拉取）。 */
    private static RecipeManager cachedRecipeManager;
    private static List<ExtractionRecipe> cachedRecipes;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == SLOT_INPUT;   // 输出槽不接受自动输入
        }
    };
    private final LazyOptional<IItemHandler> inventoryCap = LazyOptional.of(() -> inventory);

    private final ManaStorage mana = new ManaStorage(capacity(), this::onManaChanged);
    private final LazyOptional<IManaStorage> manaCap = LazyOptional.of(() -> mana);

    /** 剩余处理 tick（>0 = 正在提取）。 */
    private int progressTicks;
    /** 正在处理的配方 id（存档用：读回来才能接着算）。 */
    @Nullable
    private ResourceLocation activeRecipeId;

    public ManaExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.ModBlockEntities.MANA_EXTRACTOR.get(), pos, state);
    }

    /** 本机档位（0~4）：从方块读，所以"5 个方块 = 5 档"不需要额外状态。 */
    public int getTier() {
        if (getBlockState().getBlock() instanceof ManaExtractorBlock block) {
            return block.getTier();
        }
        return TierMath.MIN_TIER;
    }

    public static int capacity() {
        return 1_000_000;   // 单机缓存：够攒一阵，真正的"大容量"由端口/缓存单元承担（§17.1）
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public IManaStorage getManaStorage() {
        return mana;
    }

    public int getProgressTicks() {
        return progressTicks;
    }

    private void onManaChanged() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ==================== 每 tick ====================

    public void serverTick() {
        if (level == null || level.isClientSide) return;

        if (progressTicks > 0) {
            progressTicks--;
            if (progressTicks <= 0) {
                finishExtraction();
            }
            setChanged();
            return;
        }

        startIfPossible();
    }

    /** 空闲时：找配方 → 检查档位与启动能量 → 一次性扣费并开始计时。 */
    private void startIfPossible() {
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        if (input.isEmpty()) return;

        int tier = getTier();
        ExtractionRecipe recipe = findRecipe(input, tier);
        if (recipe == null) return;

        int cost = recipe.costAt(tier);
        if (mana.getManaStored() < cost) return;

        if (cost > 0) {
            mana.consume(cost);           // 一次性付（作者口径）
        }
        progressTicks = recipe.timeAt(tier);
        activeRecipeId = recipe.getId();
        setChanged();
    }

    /** 完成：吃掉 1 个输入 → 产出能量 → 掷副产物（放不下就掉在方块旁）。 */
    private void finishExtraction() {
        progressTicks = 0;
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        ExtractionRecipe recipe = activeRecipeId == null ? null : findRecipeById(activeRecipeId);
        activeRecipeId = null;
        if (recipe == null || input.isEmpty()) return;
        if (!recipe.getInput().test(input)) return;

        int tier = getTier();
        input.shrink(1);
        inventory.setStackInSlot(SLOT_INPUT, input.isEmpty() ? ItemStack.EMPTY : input);

        int energy = recipe.energyAt(tier);
        if (energy > 0) {
            mana.receiveMana(energy, false);
        }

        for (int i = 0; i < recipe.getByproducts().size(); i++) {
            int chance = recipe.chanceAt(i, tier);
            if (chance <= 0 || level.random.nextInt(100) >= chance) continue;
            ItemStack byproduct = recipe.getByproducts().get(i).stack().copy();
            if (!byproduct.isEmpty()) {
                insertOrDrop(byproduct);
            }
        }
        setChanged();
    }

    /** 先塞输出槽，塞不下就掉在机器旁边（宁可掉地上，也不要"完成时凭空消失"）。 */
    private void insertOrDrop(ItemStack stack) {
        ItemStack rest = inventory.insertItem(SLOT_OUTPUT_A, stack.copy(), false);
        rest = inventory.insertItem(SLOT_OUTPUT_B, rest, false);
        if (!rest.isEmpty() && level != null && !level.isClientSide) {
            net.minecraft.world.Containers.dropItemStack(level,
                    worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, rest);
        }
    }

    // ==================== 配方查询 ====================

    @Nullable
    private ExtractionRecipe findRecipe(ItemStack input, int tier) {
        for (ExtractionRecipe recipe : recipes()) {
            if (recipe.allowedAt(tier) && recipe.getInput().test(input)) {
                return recipe;
            }
        }
        return null;
    }

    @Nullable
    private ExtractionRecipe findRecipeById(ResourceLocation id) {
        for (ExtractionRecipe recipe : recipes()) {
            if (recipe.getId().equals(id)) return recipe;
        }
        return null;
    }

    private List<ExtractionRecipe> recipes() {
        RecipeManager manager = level == null || level.getServer() == null
                ? null
                : level.getServer().getRecipeManager();
        if (manager == null) return List.of();
        if (cachedRecipes == null || cachedRecipeManager != manager) {
            cachedRecipeManager = manager;
            cachedRecipes = manager.getAllRecipesFor(ModRecipes.EXTRACTION_TYPE.get()).stream().toList();
        }
        return cachedRecipes;
    }

    // ==================== 能力 ====================

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return inventoryCap.cast();
        }
        if (cap == ManaCapabilities.mana()) {
            return manaCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        inventoryCap.invalidate();
        manaCap.invalidate();
    }

    // ==================== 持久化与同步 ====================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        tag.putInt(NBT_MANA, mana.getManaStored());
        tag.putInt(NBT_PROGRESS, progressTicks);
        if (activeRecipeId != null) {
            tag.putString(NBT_ACTIVE, activeRecipeId.toString());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(tag.getCompound("Inventory"));
        }
        mana.setCapacitySilently(capacity());
        mana.setManaSilently(tag.getInt(NBT_MANA));
        progressTicks = tag.getInt(NBT_PROGRESS);
        activeRecipeId = tag.contains(NBT_ACTIVE) ? ResourceLocation.tryParse(tag.getString(NBT_ACTIVE)) : null;
    }

    /** Jade 要显示存量与容量：两个都带上（容量是推导值，不进存档）。 */
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
