package com.pha.trainees.mana;

import com.pha.trainees.Main;
import com.pha.trainees.config.ChemConfig;
import com.pha.trainees.network.ManaStatePacket;
import com.pha.trainees.network.ModNetwork;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 玩家自身的精炼精华（§17.1，作者 2026-10-02 定：**复用 {@link IManaStorage}**）。
 *
 * <p>于是玩家和方块是**同一种"能装 RE 的东西"**：方块能 {@code receiveMana} 就能被 R 注能，
 * 能 {@code extractMana} 就能被潜行+R 抽取——不需要为玩家另立一套接口。</p>
 *
 * <p>数值：初始 0、上限取配置（默认 100,000），上限成长交给加点系统的「精华容量」（§17.1）。
 * 存档走能力自带的 NBT；**死亡重生要手动搬一次**（{@link PlayerEvent.Clone}，
 * 这是 Forge 玩家能力的经典坑：不搬就会一死回到 0）。</p>
 */
@Mod.EventBusSubscriber(modid = Main.MODID)
public final class PlayerMana {

    public static final ResourceLocation ID = new ResourceLocation(Main.MODID, "player_mana");

    private PlayerMana() {
    }

    /** 取玩家的精华容器（没有则空）。 */
    public static LazyOptional<IManaStorage> of(@Nullable Player player) {
        return player == null ? LazyOptional.empty() : player.getCapability(ManaCapabilities.mana());
    }

    public static int get(Player player) {
        return of(player).map(IManaStorage::getManaStored).orElse(0);
    }

    public static int getCapacity(Player player) {
        return of(player).map(IManaStorage::getMaxManaStored).orElse(0);
    }

    /** 给玩家充能，返回**实际收下**的量。 */
    public static int receive(Player player, int amount) {
        return of(player).map(storage -> storage.receiveMana(amount, false)).orElse(0);
    }

    /** 从玩家扣能，返回**实际抽走**的量。 */
    public static int extract(Player player, int amount) {
        return of(player).map(storage -> storage.extractMana(amount, false)).orElse(0);
    }

    /** 直接设定（调试命令 / 加点系统）。 */
    public static void set(Player player, int value) {
        of(player).ifPresent(storage -> {
            if (storage instanceof ManaStorage concrete) {
                concrete.setCapacity(capacityFromConfig());
                concrete.setMana(value);
            }
        });
    }

    public static int capacityFromConfig() {
        return ChemConfig.MANA_PLAYER_CAPACITY.get();
    }

    /**
     * 覆盖容量（加点系统的「精华容量」由 {@code PerkEffects.refresh} 调它）。
     *
     * <p>只动容量、不触发同步回调（玩家容器的回调是空的，所以这里没有发包问题）。</p>
     */
    public static void setCapacity(Player player, int capacity) {
        of(player).ifPresent(storage -> {
            if (storage instanceof ManaStorage concrete) {
                concrete.setCapacity(Math.max(1, capacity));
            }
        });
    }

    /**
     * 把这个玩家的精华状态发给**他自己的客户端**（HUD 用，§17.1 P1.5）。
     * 只在**真正变化**时调（转移、命令、登录/重生），不要每 tick 发。
     */
    public static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            ModNetwork.get().send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    ManaStatePacket.of(serverPlayer));
        }
    }

    // ==================== 挂载 / 搬运 ====================

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(ID, new Provider());
        }
    }

    /** 死亡重生：把精华搬过去（不搬就会一死归零），并让新客户端的 HUD 立刻对上。 */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        original.reviveCaps();
        int stored = get(original);
        if (stored > 0) {
            set(event.getEntity(), stored);
        }
        original.invalidateCaps();
        sync(event.getEntity());
    }

    /** 登录时补一次（HUD 依赖这个初始值，否则进游戏要等到第一次转移才显示）。 */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        sync(event.getEntity());
    }

    /** 能力提供者：一个玩家一份 {@link ManaStorage} + NBT 存档。 */
    public static class Provider implements ICapabilitySerializable<CompoundTag> {

        private static final String NBT_MANA = "Mana";

        private final ManaStorage storage = new ManaStorage(capacityFromConfig());
        private final LazyOptional<IManaStorage> cap = LazyOptional.of(() -> storage);

        @Override
        public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
            return capability == ManaCapabilities.mana() ? cap.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putInt(NBT_MANA, storage.getManaStored());
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            storage.setCapacitySilently(capacityFromConfig());   // 配置改了要跟着变
            storage.setManaSilently(nbt.getInt(NBT_MANA));
        }

        public void invalidate() {
            cap.invalidate();
        }
    }
}
