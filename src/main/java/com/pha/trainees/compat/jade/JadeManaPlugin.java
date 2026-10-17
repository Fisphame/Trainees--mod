package com.pha.trainees.compat.jade;

import com.pha.trainees.Main;
import com.pha.trainees.mana.ManaCapabilities;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade 集成（作者 2026-10-02 要求）：**所有暴露魔力能力的方块**都显示缓存内的能量（单位 RE）。
 *
 * <p>关键设计：**不逐个登记方块**——组件注册在 {@link Block.class} 上，运行时再查那个方块的
 * {@code IManaStorage} 能力。于是以后新加的魔力设备（缓存单元、魔力池、魔力管道…）
 * 只要老老实实暴露能力，就**自动**出现在 Jade 上，不需要回来改这里。</p>
 *
 * <p>两种"没有就不显示"的情形：方块没有方块实体、或方块实体不提供魔力能力（`ifPresent` 直接跳过），
 * 于是普通方块的血条不会多出一行无关的 0/0。</p>
 *
 * <p>数值来源：客户端方块实体（见 {@code AltarCoreBlockEntity#getUpdateTag}，魔力变化即同步）。
 * 将来若某台设备不再自行同步，再补一个 common 侧的 {@code IServerDataProvider} 即可。</p>
 */
@WailaPlugin
public class JadeManaPlugin implements IWailaPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(ManaComponentProvider.INSTANCE, Block.class);
    }

    /** Jade 组件：显示 "RE: 存量 / 容量"。 */
    public enum ManaComponentProvider implements IBlockComponentProvider {
        INSTANCE;

        private static final ResourceLocation UID = new ResourceLocation(Main.MODID, "mana");

        @Override
        public ResourceLocation getUid() {
            return UID;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            BlockEntity be = accessor.getBlockEntity();
            if (be == null) return;
            be.getCapability(ManaCapabilities.mana()).ifPresent(storage ->
                    tooltip.add(Component.literal("RE: " + storage.getManaStored()
                            + " / " + storage.getMaxManaStored())));
        }
    }
}
