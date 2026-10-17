package com.pha.trainees.blockentity;

import com.pha.trainees.mana.IManaStorage;
import com.pha.trainees.mana.ManaCapabilities;
import com.pha.trainees.mana.ManaStorage;
import com.pha.trainees.recipe.AggregationRecipe;
import com.pha.trainees.registry.ModBlocks;
import com.pha.trainees.registry.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
 * 聚合机方块实体（§17.1）：**RE → 物品**，1 档、处理很快。
 *
 * <p>只用一条默认配方（1600 RE → 1 精炼点），但走数据驱动：以后写更贵的聚合产物只加 JSON。
 * 能量靠玩家 R/潜行+R 进出（它自身是 {@code IManaStorage}），产物从唯一输出槽取。</p>
 */
public class RefinedAggregatorBlockEntity extends BlockEntity {

    /** 模板槽（§17.1，作者选 B）：**把想要的产物放进来**，机器就跑产出它的那条聚合配方。 */
    public static final int SLOT_TEMPLATE = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_COUNT = 2;

    private static final String NBT_MANA = "Mana";
    private static final String NBT_CAPACITY = "Capacity";
    private static final String NBT_PROGRESS = "Progress";
    private static final String NBT_ACTIVE = "Active";

    /** 配方表缓存：RecipeManager 实例变了才重取。 */
    private static RecipeManager cachedRecipeManager;
    private static List<AggregationRecipe> cachedRecipes;

    public static final int CAPACITY = 1_000_000;

    /** 库存（模板槽 + 输出槽）。做成**具名内部类**，才能把 {@link #forceSize()} 暴露给迁移逻辑用。 */
    private class AggregatorInventory extends ItemStackHandler {

        AggregatorInventory() {
            super(SLOT_COUNT);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == SLOT_TEMPLATE;   // 模板槽可插；输出槽不接受自动输入
        }

        /**
         * 把格子数拉回**当前规格**。
         *
         * <p>必须做：Forge 的 {@code deserializeNBT} 会按存档里的 {@code Size} 调 {@code setSize}，
         * 于是"上一版只有 1 格"的旧方块实体会被缩回 1 格，接着 {@code getStackInSlot(1)} 直接
         * 抛 {@code Slot 1 not in valid range} <b>崩服</b>（2026-10-03 作者就踩了）。</p>
         */
        void forceSize() {
            setSize(SLOT_COUNT);
        }
    }

    private final AggregatorInventory inventory = new AggregatorInventory();
    private final LazyOptional<IItemHandler> inventoryCap = LazyOptional.of(() -> inventory);

    private final ManaStorage mana = new ManaStorage(CAPACITY, this::onManaChanged);
    private final LazyOptional<IManaStorage> manaCap = LazyOptional.of(() -> mana);

    private int progressTicks;
    @Nullable
    private net.minecraft.resources.ResourceLocation activeRecipeId;

    public RefinedAggregatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.ModBlockEntities.REFINED_AGGREGATOR.get(), pos, state);
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
                finish();
            }
            setChanged();
            return;
        }

        // 空闲：输出槽空着才开工（一次只聚合一份，避免堆叠塞不下）
        if (!inventory.getStackInSlot(SLOT_OUTPUT).isEmpty()) return;
        AggregationRecipe recipe = selectedRecipe();
        if (recipe == null) return;
        if (mana.getManaStored() < recipe.getEnergy()) return;
        mana.consume(recipe.getEnergy());     // 一次性付（作者口径：所有能量消耗一次性）
        progressTicks = recipe.getTime();
        activeRecipeId = recipe.getId();
        setChanged();
    }

    private void finish() {
        progressTicks = 0;
        AggregationRecipe recipe = activeRecipeId == null ? null : recipeById(activeRecipeId);
        activeRecipeId = null;
        if (recipe == null) return;
        ItemStack out = recipe.getResult().copy();
        if (!inventory.getStackInSlot(SLOT_OUTPUT).isEmpty()) return;   // 满了（物理上不该发生，防御）
        inventory.setStackInSlot(SLOT_OUTPUT, out);
        setChanged();
    }

    // ==================== 配方 ====================

    /**
     * 按**模板槽里的产物**挑配方（作者选的 B 方案）。
     *
     * <p>模板为空 = 停机（不猜玩家想做什么）；匹配只比物品本体（不比数量/NBT），
     * 因为聚合的产物是机器给的、玩家手里那颗只是"点单"。多条配方产出同一种物品时取第一条。</p>
     */
    @Nullable
    private AggregationRecipe selectedRecipe() {
        ItemStack template = inventory.getStackInSlot(SLOT_TEMPLATE);
        if (template.isEmpty()) return null;
        for (AggregationRecipe recipe : recipes()) {
            if (!recipe.getResult().isEmpty() && recipe.getResult().getItem() == template.getItem()) {
                return recipe;
            }
        }
        return null;
    }

    @Nullable
    private AggregationRecipe recipeById(net.minecraft.resources.ResourceLocation id) {
        for (AggregationRecipe recipe : recipes()) {
            if (recipe.getId().equals(id)) return recipe;
        }
        return null;
    }

    private List<AggregationRecipe> recipes() {
        RecipeManager manager = level == null || level.getServer() == null
                ? null
                : level.getServer().getRecipeManager();
        if (manager == null) return List.of();
        if (cachedRecipes == null || cachedRecipeManager != manager) {
            cachedRecipeManager = manager;
            cachedRecipes = manager.getAllRecipesFor(ModRecipes.AGGREGATION_TYPE.get()).stream().toList();
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
            loadInventory(tag.getCompound("Inventory"));
        }
        mana.setCapacitySilently(CAPACITY);
        mana.setManaSilently(tag.getInt(NBT_MANA));
        progressTicks = tag.getInt(NBT_PROGRESS);
        activeRecipeId = tag.contains(NBT_ACTIVE)
                ? net.minecraft.resources.ResourceLocation.tryParse(tag.getString(NBT_ACTIVE))
                : null;
    }

    /**
     * 载入库存 + **旧布局迁移**。
     *
     * <p>布局变过一次：最初只有 1 格（那时它就是输出槽），后来加了模板槽变成 2 格
     * （0 = 模板、1 = 输出）。旧存档只有 1 格，直接反序列化会把格子缩回 1 个导致越界崩服，
     * 所以这里先按旧规格读进临时处理器，再按新布局摆放：旧的那一格是**产物** → 放进输出槽。</p>
     */
    private void loadInventory(CompoundTag inv) {
        boolean legacySingleSlot = inv.getInt("Size") <= 1;

        ItemStackHandler source = new ItemStackHandler();
        source.deserializeNBT(inv);

        inventory.forceSize();
        for (int i = 0; i < inventory.getSlots(); i++) {
            inventory.setStackInSlot(i, ItemStack.EMPTY);
        }
        if (legacySingleSlot) {
            inventory.setStackInSlot(SLOT_OUTPUT, source.getStackInSlot(0));
        } else {
            for (int i = 0; i < Math.min(inventory.getSlots(), source.getSlots()); i++) {
                inventory.setStackInSlot(i, source.getStackInSlot(i));
            }
        }
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
