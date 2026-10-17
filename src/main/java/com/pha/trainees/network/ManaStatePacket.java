package com.pha.trainees.network;

import com.pha.trainees.mana.PlayerMana;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 玩家精华状态同步（S2C，§17.1 P1.5）：给 HUD 用。
 *
 * <p>只在**真正变化**时发（按键转移、命令设定、登录/重生补一次），不是每 tick 发。
 * 所以一条包就够，不需要"节流"逻辑。</p>
 */
public record ManaStatePacket(int stored, int capacity) {

    /** 从玩家身上取值构造（服务端用）。 */
    public static ManaStatePacket of(Player player) {
        return new ManaStatePacket(PlayerMana.get(player), PlayerMana.getCapacity(player));
    }

    public static void encode(ManaStatePacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.stored);
        buf.writeVarInt(msg.capacity);
    }

    public static ManaStatePacket decode(FriendlyByteBuf buf) {
        return new ManaStatePacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(ManaStatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            // 只可能在客户端收到；加个 dist 判断是为了让专用服务器不会去加载客户端类
            if (FMLEnvironment.dist == Dist.CLIENT) {
                com.pha.trainees.client.ClientManaState.set(msg.stored, msg.capacity);
            }
        });
        context.setPacketHandled(true);
    }
}
