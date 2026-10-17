package com.pha.trainees.mana;

/**
 * {@link IManaStorage} 的通用实现：夹取、试算、变更回调。
 *
 * <p>纯逻辑、零 MC 依赖（由 {@code ManaStorageTest} 锁住），供祭坛主方块、缓存单元、
 * 调试命令与未来的魔力管道共用。</p>
 */
public class ManaStorage implements IManaStorage {

    private int mana;
    private int capacity;
    private final Runnable onChanged;

    /** 变更时不需要通知任何人的场合（测试、纯计算）用这个。 */
    public ManaStorage(int capacity) {
        this(capacity, () -> {
        });
    }

    /**
     * @param capacity  容量上限（≤0 视为 1，避免"零容量却存着魔力"的怪状态）
     * @param onChanged 存量/容量真正变化时回调（方块实体拿它 {@code setChanged()} + 同步）
     */
    public ManaStorage(int capacity, Runnable onChanged) {
        this.capacity = Math.max(1, capacity);
        this.onChanged = onChanged == null ? () -> {
        } : onChanged;
    }

    @Override
    public int receiveMana(int maxReceive, boolean simulate) {
        if (maxReceive <= 0 || !canReceive()) return 0;
        int accepted = Math.min(maxReceive, capacity - mana);
        if (accepted <= 0) return 0;
        if (!simulate) {
            mana += accepted;
            onChanged.run();
        }
        return accepted;
    }

    @Override
    public int extractMana(int maxExtract, boolean simulate) {
        if (maxExtract <= 0 || !canExtract()) return 0;
        int extracted = Math.min(maxExtract, mana);
        if (extracted <= 0) return 0;
        if (!simulate) {
            mana -= extracted;
            onChanged.run();
        }
        return extracted;
    }

    @Override
    public int getManaStored() {
        return mana;
    }

    @Override
    public int getMaxManaStored() {
        return capacity;
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return true;
    }

    // ==================== 内部记账（方块实体 / 存档 / 调试命令用） ====================

    /** 直接设定存量（夹取到 [0, 容量]）；变化时触发回调。 */
    public void setMana(int value) {
        int clamped = Math.max(0, Math.min(value, capacity));
        if (clamped != mana) {
            mana = clamped;
            onChanged.run();
        }
    }

    /** 直接扣除（不够就扣到 0），返回是否扣成功——给"消耗型"逻辑用。 */
    public boolean consume(int amount) {
        if (amount <= 0) return true;
        if (mana < amount) return false;
        mana -= amount;
        onChanged.run();
        return true;
    }

    /** 扩容（缓存单元接入时用）：容量只会变大，存量不变。 */
    public void setCapacity(int newCapacity) {
        int clamped = Math.max(1, newCapacity);
        if (clamped != capacity) {
            capacity = clamped;
            if (mana > capacity) mana = capacity;
            onChanged.run();
        }
    }

    /**
     * 静默设容量：**不触发回调**。
     *
     * <p>专供"世界还没准备好"的时机（方块实体 {@code load()} / {@code onLoad()}）——
     * 那时候回调里的 {@code sendBlockUpdated} 会在区块反序列化/加载过程中发包，
     * 是已知会炸/死锁的操作。载入之后由 tick 或交互去触发一次真正的刷新。</p>
     */
    public void setCapacitySilently(int newCapacity) {
        int clamped = Math.max(1, newCapacity);
        capacity = clamped;
        if (mana > capacity) mana = capacity;
    }

    /** 静默设存量：同上，不触发回调（供 {@code load()} 用）。 */
    public void setManaSilently(int value) {
        mana = Math.max(0, Math.min(value, capacity));
    }

    /** 是否已满（缓存单元/管道判断"还要不要送"）。 */
    public boolean isFull() {
        return mana >= capacity;
    }
}
