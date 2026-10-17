package com.pha.trainees.blockentity;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.MEStorage;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

public class AbsorbInventoryAdapter implements MEStorage {

    private final AbsorbBlockEntity absorb;

    public AbsorbInventoryAdapter(AbsorbBlockEntity absorb) {
        this.absorb = absorb;
    }

    // ===== 必须实现的抽象方法 =====
    @Override
    public Component getDescription() {
        // 显示在 AE2 终端中的名称（可本地化）
        return Component.literal("Absorb Block Storage");
    }

    // ===== 重写 insert/extract（基类默认返回 0） =====

    /**
     * **对网络只可抽、不可插**（§19.21）。
     *
     * <p>原因：汲取方块自己也在网格存储里挂了这个适配器；若它接受插入，
     * 我们的主动推送（往网格 storage.insert）就会被路由回自己，形成死循环。
     * 方块内容物只能来自"从邻接机器吸取"与玩家交互。</p>
     */
    @Override
    public long insert(@Nonnull AEKey what, long amount, @Nonnull Actionable mode, @Nonnull IActionSource source) {
        return 0;
    }

    @Override
    public long extract(@Nonnull AEKey what, long amount, @Nonnull Actionable mode, @Nonnull IActionSource source) {
        if (!(what instanceof AEItemKey itemKey)) return 0;

        ItemStack stored = absorb.getStoredItem();
        if (stored.isEmpty()) return 0;

        AEItemKey storedKey = AEItemKey.of(stored);
        if (storedKey == null || !storedKey.equals(itemKey)) {
            return 0;
        }

        long extractable = Math.min(amount, stored.getCount());

        if (mode == Actionable.SIMULATE) {
            return extractable;
        }

        int toExtract = (int) extractable;
        ItemStack newStack = stored.copy();
        newStack.shrink(toExtract);

        if (newStack.isEmpty()) {
            absorb.clearStoredItem();
        } else {
            absorb.setStoredItem(newStack);
        }

        return extractable;
    }

    // ===== 重写 getAvailableStacks(KeyCounter)（基类有默认实现，但我们需要提供内容） =====
    @Override
    public void getAvailableStacks(KeyCounter out) {
        ItemStack stored = absorb.getStoredItem();
        if (!stored.isEmpty()) {
            AEItemKey key = AEItemKey.of(stored);
            if (key != null) {
                out.add(key, stored.getCount());
            }
        }
    }

    // 如果只实现 getAvailableStacks(KeyCounter)，则 getAvailableStacks() 无参方法会自动调用它，无需重写。
}