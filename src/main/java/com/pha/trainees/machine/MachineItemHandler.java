package com.pha.trainees.machine;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

/**
 * 按面暴露用的物品 handler 包装（§17.1 按面 I/O）。
 *
 * <p>把机器的某个**槽位子集**包成一个只读/只写的 {@link IItemHandler}：
 * 于是"这个面只能进物品""这个面只能抽产物"就是换个包装，不用给机器写两套库存。</p>
 *
 * <p>{@code forbiddenInsertSlot} 用来落实作者的"**输入不能进催化剂槽**"：自动输入（漏斗/管道）
 * 走的是这个包装，在 {@link #isItemValid} 里把那一个槽位挡掉即可（玩家在 GUI 里仍可手动放）。</p>
 */
public class MachineItemHandler implements IItemHandler {

    private final ItemStackHandler delegate;
    private final int[] slots;
    private final boolean allowInsert;
    private final boolean allowExtract;
    /** 自动输入要挡掉的槽位（-1 = 不挡）。 */
    private final int forbiddenInsertSlot;

    public MachineItemHandler(ItemStackHandler delegate, int[] slots,
                              boolean allowInsert, boolean allowExtract, int forbiddenInsertSlot) {
        this.delegate = delegate;
        this.slots = slots.clone();
        this.allowInsert = allowInsert;
        this.allowExtract = allowExtract;
        this.forbiddenInsertSlot = forbiddenInsertSlot;
    }

    @Override
    public int getSlots() {
        return slots.length;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        return delegate.getStackInSlot(slots[slot]);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (!allowInsert) return stack;
        int real = slots[slot];
        if (real == forbiddenInsertSlot) return stack;
        return delegate.insertItem(real, stack, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!allowExtract) return ItemStack.EMPTY;
        return delegate.extractItem(slots[slot], amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return delegate.getSlotLimit(slots[slot]);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        int real = slots[slot];
        if (!allowInsert || real == forbiddenInsertSlot) return false;
        return delegate.isItemValid(real, stack);
    }
}
