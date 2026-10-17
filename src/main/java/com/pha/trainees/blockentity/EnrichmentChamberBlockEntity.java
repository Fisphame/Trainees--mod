package com.pha.trainees.blockentity;

import com.pha.trainees.block.EnrichmentChamberBlock;
import com.pha.trainees.machine.FaceConfig;
import com.pha.trainees.machine.MachineFluidHandler;
import com.pha.trainees.machine.MachineItemHandler;
import com.pha.trainees.mana.IManaStorage;
import com.pha.trainees.mana.ManaCapabilities;
import com.pha.trainees.mana.ManaStorage;
import com.pha.trainees.mana.TierMath;
import com.pha.trainees.recipe.EnrichmentRecipe;
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
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 富集仓方块实体（§17.1）：按"富集"配方把**多个原料聚成富集态**，之后提取更划算。
 *
 * <p>槽位按作者规格：**4 个物品槽**（0 消耗 / 1 催化剂 / 2,3 输出）+ **4 个流体罐**
 * （0 消耗 / 1 催化剂 / 2,3 输出）。档位来自方块本身（5 个注册方块 = 5 档），
 * 所需 RE 与处理时间都走 {@link TierMath} 的多项式（高级档更省电也更快）。</p>
 *
 * <p>与其它机器一致的口径：能量**一次性付**、处理中存档（进度 + 配方 id）、
 * 输出放不下就掉在机器旁（不凭空消失）、暴露 {@code IManaStorage} 让 Jade 显示缓存。</p>
 */
public class EnrichmentChamberBlockEntity extends BlockEntity {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_CATALYST = 1;
    public static final int SLOT_OUTPUT_A = 2;
    public static final int SLOT_OUTPUT_B = 3;
    public static final int SLOT_COUNT = 4;

    public static final int TANK_INPUT = 0;
    public static final int TANK_CATALYST = 1;
    public static final int TANK_OUTPUT_A = 2;
    public static final int TANK_OUTPUT_B = 3;
    public static final int TANK_COUNT = 4;
    /** 流体槽容量：作者要"不限"——int 上限 ≈ 21 亿 mB（≈210 万桶），实际等于无限。 */
    public static final int TANK_CAPACITY = Integer.MAX_VALUE;

    private static final String NBT_MANA = "Mana";
    private static final String NBT_CAPACITY = "Capacity";
    private static final String NBT_PROGRESS = "Progress";
    private static final String NBT_ACTIVE = "Active";

    private static RecipeManager cachedRecipeManager;
    private static List<EnrichmentRecipe> cachedRecipes;

    public static final int MANA_CAPACITY = 2_000_000;

    /** 物品槽：输出槽不接受自动输入（与提取机同规矩）。 */
    private class ChamberInventory extends ItemStackHandler {

        ChamberInventory() {
            super(SLOT_COUNT);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == SLOT_INPUT || slot == SLOT_CATALYST;
        }

        /**
         * 输出槽是**巨型输出总线**（§17.1 路线 i，作者定）：上限取 int 最大值 + 同种自动合并。
         *
         * <p>两处都要覆盖：Forge 的 {@code insertItem} 实际用的是
         * {@code getStackLimit(slot, stack) = min(getSlotLimit(slot), stack.getMaxStackSize())}，
         * 只改 {@code getSlotLimit} 会被 64 卡住。</p>
         */
        @Override
        public int getSlotLimit(int slot) {
            return isOutput(slot) ? Integer.MAX_VALUE : 64;
        }

        @Override
        protected int getStackLimit(int slot, @NotNull ItemStack stack) {
            return isOutput(slot) ? Integer.MAX_VALUE : super.getStackLimit(slot, stack);
        }

        private boolean isOutput(int slot) {
            return slot == SLOT_OUTPUT_A || slot == SLOT_OUTPUT_B;
        }

        /** 库存布局若再变，载入时把规格拉回当前值（聚合机那次崩服的教训）。 */
        void forceSize() {
            setSize(SLOT_COUNT);
        }
    }

    private final ChamberInventory inventory = new ChamberInventory();
    private final LazyOptional<IItemHandler> inventoryCap = LazyOptional.of(() -> inventory);

    /** 4 个流体罐：输入 / 催化剂 / 两个输出。 */
    private final FluidTank[] tanks = new FluidTank[TANK_COUNT];
    private final CombinedTanks tanksHandler = new CombinedTanks();
    private final LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> tanksHandler);

    private final ManaStorage mana = new ManaStorage(MANA_CAPACITY, this::onManaChanged);
    private final LazyOptional<IManaStorage> manaCap = LazyOptional.of(() -> mana);

    /** 按面 I/O 配置（§17.1，作者定）：未指定状态全给；已指定状态每个面只给对应角色。 */
    private final FaceConfig faceConfig = new FaceConfig();

    /** 按面暴露用的包装（懒创建，缓存起来避免每次 getCapability 都新建）。 */
    private final MachineItemHandler itemInputHandler =
            new MachineItemHandler(inventory, new int[]{SLOT_INPUT, SLOT_CATALYST}, true, false, SLOT_CATALYST);
    private final MachineItemHandler itemOutputHandler =
            new MachineItemHandler(inventory, new int[]{SLOT_OUTPUT_A, SLOT_OUTPUT_B}, false, true, -1);
    private final LazyOptional<IItemHandler> itemInputCap = LazyOptional.of(() -> itemInputHandler);
    private final LazyOptional<IItemHandler> itemOutputCap = LazyOptional.of(() -> itemOutputHandler);

    private final MachineFluidHandler fluidInputHandler =
            new MachineFluidHandler(tanks, new int[]{TANK_INPUT, TANK_CATALYST}, true, false);
    private final MachineFluidHandler fluidOutputHandler =
            new MachineFluidHandler(tanks, new int[]{TANK_OUTPUT_A, TANK_OUTPUT_B}, false, true);
    private final LazyOptional<IFluidHandler> fluidInputCap = LazyOptional.of(() -> fluidInputHandler);
    private final LazyOptional<IFluidHandler> fluidOutputCap = LazyOptional.of(() -> fluidOutputHandler);

    private int progressTicks;
    @Nullable
    private net.minecraft.resources.ResourceLocation activeRecipeId;

    public EnrichmentChamberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.ModBlockEntities.ENRICHMENT_CHAMBER.get(), pos, state);
        for (int i = 0; i < TANK_COUNT; i++) {
            tanks[i] = new FluidTank(TANK_CAPACITY) {
                @Override
                protected void onContentsChanged() {
                    setChanged();
                }
            };
        }
    }

    public int getTier() {
        if (getBlockState().getBlock() instanceof EnrichmentChamberBlock block) {
            return block.getTier();
        }
        return TierMath.MIN_TIER;
    }

    public ChamberInventory getInventory() {
        return inventory;
    }

    public FluidTank getTank(int index) {
        return tanks[Math.max(0, Math.min(TANK_COUNT - 1, index))];
    }

    public IManaStorage getManaStorage() {
        return mana;
    }

    public int getProgressTicks() {
        return progressTicks;
    }

    /** 给方块交互（桶灌注/取液）用的流体处理器。 */
    public IFluidHandler getFluidHandler() {
        return tanksHandler;
    }

    /** 把 4 个罐拼成一个流体处理器（外部按 0..3 访问）。 */
    private class CombinedTanks implements IFluidHandler {

        @Override
        public int getTanks() {
            return TANK_COUNT;
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            return getTank(tank).getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return TANK_CAPACITY;
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            // 离散纯流体槽（作者 2026-10-03）：只有输入/催化剂槽可进，且一个槽只装一种流体
            if (tank != TANK_INPUT && tank != TANK_CATALYST) return false;
            FluidStack current = getTank(tank).getFluid();
            return current.isEmpty() || current.getFluid().isSame(stack.getFluid());
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            // 离散：优先灌输入槽，装不下再灌催化剂槽；同槽不混流体（FluidTank 本身也拒绝）
            int filled = getTank(TANK_INPUT).fill(resource, action);
            if (filled < resource.getAmount()) {
                filled += getTank(TANK_CATALYST).fill(
                        new FluidStack(resource.getFluid(), resource.getAmount() - filled), action);
            }
            return filled;
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            // 离散：**整槽取走**，不抽一半
            if (resource.isEmpty()) return FluidStack.EMPTY;
            for (int index : new int[]{TANK_OUTPUT_A, TANK_OUTPUT_B}) {
                FluidTank tank = getTank(index);
                if (tank.getFluidAmount() > 0 && tank.getFluid().getFluid().isSame(resource.getFluid())) {
                    return tank.drain(tank.getFluidAmount(), action);
                }
            }
            return FluidStack.EMPTY;
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            // 离散：**整槽取走**（忽略 maxDrain，除非它比整槽还小——那就按它能接多少给多少）
            for (int index : new int[]{TANK_OUTPUT_A, TANK_OUTPUT_B}) {
                FluidTank tank = getTank(index);
                if (tank.getFluidAmount() > 0) {
                    return tank.drain(Math.min(maxDrain, tank.getFluidAmount()), action);
                }
            }
            return FluidStack.EMPTY;
        }
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

        EnrichmentRecipe recipe = findMatchingRecipe();
        if (recipe == null) return;
        int tier = getTier();
        if (mana.getManaStored() < recipe.energyAt(tier)) return;
        if (!outputsFit(recipe)) return;

        mana.consume(recipe.energyAt(tier));    // 一次性付
        progressTicks = recipe.timeAt(tier);
        activeRecipeId = recipe.getId();
        setChanged();
    }

    /** 逐条试：原料对得上、催化剂（若有）齐、流体够、输出放得下。 */
    @Nullable
    private EnrichmentRecipe findMatchingRecipe() {
        for (EnrichmentRecipe recipe : recipes()) {
            if (matches(recipe) && outputsFit(recipe)) return recipe;
        }
        return null;
    }

    private boolean matches(EnrichmentRecipe recipe) {
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        if (recipe.getInputItem() != null && !recipe.getInputItem().test(input)) return false;
        if (recipe.getInputItem() == null && input.isEmpty()) return false;

        if (recipe.getCatalystItem() != null
                && !recipe.getCatalystItem().test(inventory.getStackInSlot(SLOT_CATALYST))) {
            return false;
        }
        if (recipe.getInputFluid() != null
                && !tanks[TANK_INPUT].getFluid().containsFluid(recipe.getInputFluid())) {
            return false;
        }
        return recipe.getCatalystFluid() == null
                || tanks[TANK_CATALYST].getFluid().containsFluid(recipe.getCatalystFluid());
    }

    private boolean outputsFit(EnrichmentRecipe recipe) {
        List<ItemStack> results = recipe.getItemResults();
        for (int i = 0; i < results.size(); i++) {
            ItemStack slot = inventory.getStackInSlot(SLOT_OUTPUT_A + i);
            if (!slot.isEmpty() && !ItemStack.isSameItemSameTags(slot, results.get(i))) return false;
            if (!slot.isEmpty() && slot.getCount() + results.get(i).getCount() > slot.getMaxStackSize()) return false;
        }
        for (int i = 0; i < recipe.getFluidResults().size(); i++) {
            FluidTank tank = tanks[TANK_OUTPUT_A + i];
            EnrichmentRecipe.FluidOutput output = recipe.getFluidResults().get(i);
            if (tank.getFluidAmount() > 0 && !tank.getFluid().getFluid().isSame(output.fluid())) return false;
            if (tank.getFluidAmount() + output.amount() > TANK_CAPACITY) return false;
        }
        return true;
    }

    private void finish() {
        progressTicks = 0;
        EnrichmentRecipe recipe = activeRecipeId == null ? null : recipeById(activeRecipeId);
        activeRecipeId = null;
        if (recipe == null) return;

        // 扣原料（物品按 1 个，流体按配方量）
        if (recipe.getInputItem() != null) {
            inventory.getStackInSlot(SLOT_INPUT).shrink(1);
        }
        if (recipe.getCatalystItem() != null) {
            inventory.getStackInSlot(SLOT_CATALYST).shrink(1);
        }
        if (recipe.getInputFluid() != null) {
            tanks[TANK_INPUT].drain(recipe.getInputFluid().getAmount(), IFluidHandler.FluidAction.EXECUTE);
        }
        if (recipe.getCatalystFluid() != null) {
            tanks[TANK_CATALYST].drain(recipe.getCatalystFluid().getAmount(), IFluidHandler.FluidAction.EXECUTE);
        }

        // 产出
        for (int i = 0; i < recipe.getItemResults().size(); i++) {
            ItemStack result = recipe.getItemResults().get(i).copy();
            int slot = SLOT_OUTPUT_A + i;
            ItemStack existing = inventory.getStackInSlot(slot);
            if (existing.isEmpty()) {
                inventory.setStackInSlot(slot, result);
            } else {
                existing.grow(result.getCount());
                inventory.setStackInSlot(slot, existing);
            }
        }
        for (int i = 0; i < recipe.getFluidResults().size(); i++) {
            EnrichmentRecipe.FluidOutput output = recipe.getFluidResults().get(i);
            tanks[TANK_OUTPUT_A + i].fill(new FluidStack(output.fluid(), output.amount()),
                    IFluidHandler.FluidAction.EXECUTE);
        }
        setChanged();
    }

    // ==================== 配方 ====================

    @Nullable
    private EnrichmentRecipe recipeById(net.minecraft.resources.ResourceLocation id) {
        for (EnrichmentRecipe recipe : recipes()) {
            if (recipe.getId().equals(id)) return recipe;
        }
        return null;
    }

    private List<EnrichmentRecipe> recipes() {
        RecipeManager manager = level == null || level.getServer() == null
                ? null
                : level.getServer().getRecipeManager();
        if (manager == null) return List.of();
        if (cachedRecipes == null || cachedRecipeManager != manager) {
            cachedRecipeManager = manager;
            cachedRecipes = manager.getAllRecipesFor(ModRecipes.ENRICHMENT_TYPE.get()).stream().toList();
        }
        return cachedRecipes;
    }

    // ==================== 能力 ====================

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        // 按面 I/O（§17.1）：side == null = 无方向查询（内部/工具）→ 给全套；
        // 否则按 FaceConfig 决定这个面暴露"输入"还是"输出"，还是什么都不给。
        boolean everything = side == null || faceConfig.isAllUnset();

        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            if (everything) return inventoryCap.cast();
            if (faceConfig.acceptsItemInput(side)) return itemInputCap.cast();
            if (faceConfig.allowsItemOutput(side)) return itemOutputCap.cast();
            return LazyOptional.empty();
        }
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            if (everything) return fluidCap.cast();
            if (faceConfig.acceptsFluidInput(side)) return fluidInputCap.cast();
            if (faceConfig.allowsFluidOutput(side)) return fluidOutputCap.cast();
            return LazyOptional.empty();
        }
        if (cap == ManaCapabilities.mana()) {
            return manaCap.cast();
        }
        return super.getCapability(cap, side);
    }

    /** 面配置（菜单要读它画按钮/同步给客户端）。 */
    public FaceConfig getFaceConfig() {
        return faceConfig;
    }

    /** 面配置改了：落存档 + 让客户端重画。 */
    public void onFaceConfigChanged() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        inventoryCap.invalidate();
        fluidCap.invalidate();
        manaCap.invalidate();
        itemInputCap.invalidate();
        itemOutputCap.invalidate();
        fluidInputCap.invalidate();
        fluidOutputCap.invalidate();
    }

    // ==================== 持久化与同步 ====================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        CompoundTag fluidTag = new CompoundTag();
        for (int i = 0; i < TANK_COUNT; i++) {
            fluidTag.put("Tank" + i, tanks[i].writeToNBT(new CompoundTag()));
        }
        tag.put("Fluids", fluidTag);
        tag.putInt(NBT_MANA, mana.getManaStored());
        tag.putInt(NBT_PROGRESS, progressTicks);
        tag.putIntArray("Faces", faceConfig.toOrdinals());
        if (activeRecipeId != null) {
            tag.putString(NBT_ACTIVE, activeRecipeId.toString());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) {
            CompoundTag inv = tag.getCompound("Inventory");
            ItemStackHandler source = new ItemStackHandler();
            source.deserializeNBT(inv);
            inventory.forceSize();
            for (int i = 0; i < inventory.getSlots(); i++) {
                inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
            for (int i = 0; i < Math.min(inventory.getSlots(), source.getSlots()); i++) {
                inventory.setStackInSlot(i, source.getStackInSlot(i));
            }
        }
        if (tag.contains("Fluids")) {
            CompoundTag fluidTag = tag.getCompound("Fluids");
            for (int i = 0; i < TANK_COUNT; i++) {
                tanks[i].readFromNBT(fluidTag.getCompound("Tank" + i));
            }
        }
        mana.setCapacitySilently(MANA_CAPACITY);
        mana.setManaSilently(tag.getInt(NBT_MANA));
        progressTicks = tag.getInt(NBT_PROGRESS);
        if (tag.contains("Faces")) {
            faceConfig.fromOrdinals(tag.getIntArray("Faces"));
        }
        activeRecipeId = tag.contains(NBT_ACTIVE)
                ? net.minecraft.resources.ResourceLocation.tryParse(tag.getString(NBT_ACTIVE))
                : null;
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
