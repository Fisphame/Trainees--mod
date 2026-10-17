package com.pha.trainees.network;

import com.pha.trainees.chemistry.blockentity.PackerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * 打包机动作包（C2S，§19.18.5）：GUI 上的按钮与"刷新"都走这里。
 *
 * <p>服务端**必须自行校验**（客户端可以伪造包）：方块实体确实是 {@link PackerBlockEntity}、
 * 且玩家在允许距离内。动作执行后**回发 {@link PackerStatePacket}**，客户端据此就地刷新面板
 * （所以点【取出产物】后输出槽会立刻变空，不需要重开界面或额外轮询）。</p>
 */
public record PackerActionPacket(BlockPos pos, Action action) {

    public enum Action {
        /** 只要状态（GUI 的"刷新"） */
        REQUEST_STATE,
        /** 打包一瓶（auto 模式：不合格不产不扣） */
        PACK_NOW,
        /** 人工强制灌装（唯一能产危险次品的入口） */
        FORCE_FILL,
        /** 取走输出槽产物 */
        TAKE_OUTPUT
    }

    /** 允许的最大交互距离（格）：超出即视为非法调用，直接忽略 */
    private static final double MAX_DISTANCE_SQR = 64.0 * 64.0;

    public static void encode(PackerActionPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeEnum(msg.action);
    }

    public static PackerActionPacket decode(FriendlyByteBuf buf) {
        return new PackerActionPacket(buf.readBlockPos(), buf.readEnum(Action.class));
    }

    public static void handle(PackerActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        ServerPlayer sender = context.getSender();
        context.enqueueWork(() -> {
            if (sender == null) return;
            if (sender.distanceToSqr(Vec3.atCenterOf(msg.pos)) > MAX_DISTANCE_SQR) return;

            Level level = sender.level();
            if (!(level.getBlockEntity(msg.pos) instanceof PackerBlockEntity packer)) return;

            switch (msg.action) {
                case REQUEST_STATE -> { }
                case PACK_NOW -> packer.packOnce(level, msg.pos, false, sender);
                case FORCE_FILL -> packer.packOnce(level, msg.pos, true, sender);
                case TAKE_OUTPUT -> {
                    // 取到了才提示（复用 message.trainees.packer.output_taken，避免悬空语言键）
                    if (packer.takeOutput(sender)) {
                        sender.displayClientMessage(
                                Component.translatable("message.trainees.packer.output_taken"), true);
                    }
                }
            }

            // 无论哪个动作都回发最新状态：面板就地刷新，无需重开
            ModNetwork.get().send(PacketDistributor.PLAYER.with(() -> sender), packer.buildStatePacket());
        });
        context.setPacketHandled(true);
    }
}
