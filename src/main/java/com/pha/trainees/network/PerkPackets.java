package com.pha.trainees.network;

import com.pha.trainees.perk.PerkData;
import com.pha.trainees.perk.PerkEffects;
import com.pha.trainees.perk.PerkTable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Supplier;

/**
 * 加点系统的两个包（§17.1 P3）。
 *
 * <p>{@link Action}（C2S）：点亮 / 回退 / **空 id = 请求状态**（打开屏幕时用）。
 * 服务端**必须自己校验**（客户端可伪造）：点数、前置、满级、回退时有没有被别的属性当前置依赖。</p>
 *
 * <p>{@link State}（S2C）：把"已点等级 + 背包里可用的精炼点"回给**该玩家自己**，屏幕据此重画。</p>
 */
public final class PerkPackets {

    private PerkPackets() {
    }

    // ==================== C2S ====================

    public record Action(String perkId, boolean refund) {

        public static void encode(Action msg, FriendlyByteBuf buf) {
            buf.writeUtf(msg.perkId, 64);
            buf.writeBoolean(msg.refund);
        }

        public static Action decode(FriendlyByteBuf buf) {
            return new Action(buf.readUtf(64), buf.readBoolean());
        }

        public static void handle(Action msg, Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = ctx.get();
            ServerPlayer sender = context.getSender();
            context.enqueueWork(() -> {
                if (sender == null) return;

                // 空 id = 只请求状态（打开屏幕时）
                if (msg.perkId.isEmpty()) {
                    reply(sender);
                    return;
                }

                // "*" = 一键洗点：全部回退并原价返还（A3）
                if (msg.perkId.equals("*")) {
                    int refunded = PerkData.refundAll(sender);
                    sender.displayClientMessage(Component.translatable(refunded > 0
                            ? "perk.trainees.msg.respec"
                            : "perk.trainees.msg.respec_none", refunded), true);
                    reply(sender);
                    return;
                }

                PerkTable perk = PerkTable.byId(msg.perkId);
                if (perk == null) return;

                if (msg.refund) {
                    boolean ok = PerkData.refund(sender, perk);
                    sender.displayClientMessage(Component.translatable(ok
                            ? "perk.trainees.msg.refunded"
                            : "perk.trainees.msg.refund_blocked"), true);
                } else {
                    String reason = PerkData.whyCannotUnlock(sender, perk);
                    if (reason == null) {
                        PerkData.unlock(sender, perk);
                        sender.displayClientMessage(Component.translatable("perk.trainees.msg.unlocked",
                                Component.translatable(perk.nameKey)), true);
                    } else {
                        sender.displayClientMessage(Component.translatable(reason), true);
                    }
                }
                reply(sender);
            });
            context.setPacketHandled(true);
        }
    }

    // ==================== S2C ====================

    public record State(CompoundTag ranks, int available) {

        public static State of(ServerPlayer player) {
            CompoundTag tag = new CompoundTag();
            for (Map.Entry<String, Integer> entry : PerkData.ranks(player).entrySet()) {
                tag.putInt(entry.getKey(), entry.getValue());
            }
            return new State(tag, PerkData.availablePoints(player));
        }

        public static void encode(State msg, FriendlyByteBuf buf) {
            buf.writeNbt(msg.ranks);
            buf.writeVarInt(msg.available);
        }

        public static State decode(FriendlyByteBuf buf) {
            CompoundTag tag = buf.readNbt();
            return new State(tag == null ? new CompoundTag() : tag, buf.readVarInt());
        }

        public static void handle(State msg, Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = ctx.get();
            context.enqueueWork(() -> {
                if (FMLEnvironment.dist == Dist.CLIENT) {
                    com.pha.trainees.client.perk.ClientPerkState.set(msg.ranks, msg.available);
                }
            });
            context.setPacketHandled(true);
        }
    }

    /** 把最新状态回给玩家自己。 */
    public static void reply(@Nullable ServerPlayer player) {
        if (player == null) return;
        PerkEffects.refresh(player);
        ModNetwork.get().send(PacketDistributor.PLAYER.with(() -> player), State.of(player));
    }
}
