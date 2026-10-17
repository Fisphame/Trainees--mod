package com.pha.trainees.mana;

import com.pha.trainees.Main;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

/**
 * 魔力能力的注册与查询（蓝本 §17.1：独立 {@code IManaStorage}）。
 *
 * <p>与 FE 的 {@code ForgeCapabilities.ENERGY} 同构，只是魔力是我们自己的能力类型，
 * 因此需要自己注册一次。{@link #mana()} <b>惰性获取</b>：绝不在静态初始化里
 * {@code CapabilityManager.get}——那个时机可能早于 {@link RegisterCapabilitiesEvent}，
 * 会直接抛"capability not registered"。</p>
 */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ManaCapabilities {

    private static Capability<IManaStorage> mana;

    private ManaCapabilities() {
    }

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.register(IManaStorage.class);
    }

    /** 魔力能力（首次调用时解析；此后就是一次 field 读取）。 */
    public static Capability<IManaStorage> mana() {
        if (mana == null) {
            mana = CapabilityManager.get(new CapabilityToken<>() {
            });
        }
        return mana;
    }

    /** 便捷查询：拿不到就返回空。 */
    public static LazyOptional<IManaStorage> of(@Nullable BlockEntity be, @Nullable Direction side) {
        return be == null ? LazyOptional.empty() : be.getCapability(mana(), side);
    }
}
