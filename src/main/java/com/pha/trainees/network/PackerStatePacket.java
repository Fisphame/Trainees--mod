package com.pha.trainees.network;

import com.pha.trainees.client.ClientPacketHandlers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 打包机状态包（S2C，§19.18.5）。
 *
 * <p><b>为什么不用 {@code AbstractContainerMenu}/{@code MenuType}</b>：本项目既有的机器面板
 * （分析仪、反应图）都走"**S2C 状态包 → 客户端 Screen；Screen 发 C2S 动作包**"这一套。
 * Menu 那套要额外维护窗口 id、槽位同步、容器数据包与 MenuType 注册，而打包机只有
 * "几个数值 + 一个输出槽"，状态包足够且更好改。</p>
 *
 * <p>包里全是**可本地化的键**（{@code productId} 用短名拼语言键，{@code lastReasonKey} 直接是语言键），
 * 客户端负责渲染文本（§19.16）。</p>
 *
 * <p><b>为什么"现在"和"历史"要占两个字段</b>（{@code readyReasonKey} vs {@code lastReasonKey}）：</p>
 * <ul>
 *   <li>{@code readyReasonKey} 回答"**此刻**为什么打不了瓶"（由服务端**只读判定** {@code dryRun} 得出，就绪时为空串）；</li>
 *   <li>{@code lastReasonKey} 回答"**上一次**实际尝试为什么失败"（历史，成功时清空）。</li>
 * </ul>
 * <p>二者回答的是不同问题、也会不同步（比如玩家已经补了碱，此刻仍"未就绪"的原因已变成电量不足，
 * 而最近一次失败记录仍是"有效氯不足"）。若只留一个字段，每次刷新都会把历史覆盖成当前状态——
 * 玩家就再也看不到"上次为什么没打出来"了。多一个短字符串换两类信息，值。</p>
 */
public record PackerStatePacket(BlockPos pos,
                                int energyStored,
                                int energyCapacity,
                                ItemStack output,
                                String productId,
                                boolean canPackNow,
                                String readyReasonKey,
                                String lastReasonKey) {

    public static void encode(PackerStatePacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeVarInt(msg.energyStored);
        buf.writeVarInt(msg.energyCapacity);
        buf.writeItem(msg.output == null ? ItemStack.EMPTY : msg.output);
        buf.writeUtf(msg.productId == null ? "" : msg.productId);
        buf.writeBoolean(msg.canPackNow);
        buf.writeUtf(msg.readyReasonKey == null ? "" : msg.readyReasonKey);
        buf.writeUtf(msg.lastReasonKey == null ? "" : msg.lastReasonKey);
    }

    public static PackerStatePacket decode(FriendlyByteBuf buf) {
        return new PackerStatePacket(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(),
                buf.readItem(), buf.readUtf(), buf.readBoolean(), buf.readUtf(), buf.readUtf());
    }

    public static void handle(PackerStatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        // 客户端代码集中在 client-only 处理器里：服务器端永不加载本 lambda 的目标类（§19.21）
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandlers.openPacker(msg)));
        ctx.get().setPacketHandled(true);
    }
}
