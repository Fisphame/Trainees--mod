package com.pha.trainees.blockentity;


import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public abstract class ItemHandlerBlockEntity extends BlockEntity {
    public ItemHandlerBlockEntity(BlockEntityType<?> p_155228_, BlockPos p_155229_, BlockState p_155230_) {
        super(p_155228_, p_155229_, p_155230_);
    }

    protected final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            // 当物品变化时，同步数据到客户端
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public int getSlotLimit(int slot) {
            // 槽位容量由子类 getLimit() 决定（汲取方块=无限堆叠，祭坛=1）
            return getLimit();
        }

        @Override
        @NotNull
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) return ItemStack.EMPTY;
            if (!isItemValid(slot, stack)) return stack;

            ItemStack existing = getStackInSlot(slot);
            if (!existing.isEmpty()) {
                // 非空槽：严格匹配，直接合并（继承槽位容量语义）
                if (!ItemStack.isSameItemSameTags(existing, stack)) {
                    return stack;
                }
                if (!simulate) {
                    existing.grow(stack.getCount());
                    onContentsChanged(slot);
                }
                return ItemStack.EMPTY;
            } else {
                // 空槽：若子类槽位上限超过物品最大堆叠（如汲取方块的无限堆叠语义），
                // 空槽首插也按槽位上限；否则遵守物品自身最大堆叠（Forge 惯例）
                int limit = getSlotLimit(slot);
                if (limit <= stack.getMaxStackSize()) {
                    limit = Math.min(limit, stack.getMaxStackSize());
                }
                int insertCount = Math.min(stack.getCount(), limit);
                if (!simulate) {
                    ItemStack toInsert = stack.copy();
                    toInsert.setCount(insertCount);
                    setStackInSlot(slot, toInsert);
                }
                ItemStack remainder = stack.copy();
                remainder.shrink(insertCount);
                return remainder.isEmpty() ? ItemStack.EMPTY : remainder;
            }
        }

        @Override
        @NotNull
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack existing = getStackInSlot(slot);
            if (existing.isEmpty()) return ItemStack.EMPTY;

            // 限制单次提取数量不超过物品自身最大堆叠
            int maxExtract = Math.min(amount, existing.getMaxStackSize());
            int toExtract = Math.min(maxExtract, existing.getCount());

            ItemStack extracted = existing.copy();
            extracted.setCount(toExtract);

            if (!simulate) {
                existing.shrink(toExtract);
                if (existing.isEmpty()) {
                    setStackInSlot(slot, ItemStack.EMPTY);
                } else {
                    onContentsChanged(slot);
                }
            }
            return extracted;
        }
    };
    protected final LazyOptional<IItemHandler> handler = LazyOptional.of(() -> itemHandler);

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public LazyOptional<IItemHandler> getHandler() {
        return handler;
    }

    public abstract int getLimit();

    // NBT数据保存/加载
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", itemHandler.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("inventory")) {
            itemHandler.deserializeNBT(tag.getCompound("inventory"));
        }
    }

    // 客户端数据同步
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        handleUpdateTag(pkt.getTag());
    }

    // 能力系统
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return handler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        handler.invalidate();
    }

    // 获取存储的物品
    public ItemStack getStoredItem() {
        return itemHandler.getStackInSlot(0).copy();
    }

    // 设置存储的物品
    public void setStoredItem(ItemStack itemStack) {
        itemHandler.setStackInSlot(0, itemStack);
    }

    // 清空存储的物品
    public void clearStoredItem() {
        itemHandler.setStackInSlot(0, ItemStack.EMPTY);
    }

    // 判断是否有物品
    public boolean hasStoredItem() {
        return !itemHandler.getStackInSlot(0).isEmpty();
    }

    // 掉落存储的物品
    public void dropStoredItem() {
        if (level == null || level.isClientSide()) return;

        ItemStack totalStack = itemHandler.getStackInSlot(0);
        if (totalStack.isEmpty()) return;

        int maxStackSize = totalStack.getMaxStackSize();
        int remaining = totalStack.getCount();

        while (remaining > 0) {
            int dropCount = Math.min(remaining, maxStackSize);
            ItemStack dropStack = totalStack.copy();
            dropStack.setCount(dropCount);

            double x = worldPosition.getX() + 0.5;
            double y = worldPosition.getY() + 1.0;
            double z = worldPosition.getZ() + 0.5;

            ItemEntity itemEntity = new ItemEntity(level, x, y, z, dropStack);
            itemEntity.setDefaultPickUpDelay();
            level.addFreshEntity(itemEntity);

            remaining -= dropCount;
        }

        itemHandler.setStackInSlot(0, ItemStack.EMPTY);
    }

    /**
     * 根据存储物品数量计算比较器输出强度
     * 空槽: 0，满64: 15，中间线性插值
     */
    public int getComparatorOutput() {
        ItemStack stack = itemHandler.getStackInSlot(0);
        if (stack.isEmpty()) {
            return 0;
        }

        int count = stack.getCount();
        // 线性映射：0 → 0，64 → 15，超过64也按15处理
        int strength = (int) Math.floor(15.0 * count / 64.0);
        return Math.min(15, Math.max(0, strength));
    }
}
