package com.pha.trainees.network;

import com.pha.trainees.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 反应图数据包：服务端命令收集 ReactionGraph 后发送给玩家客户端，打开可视化界面。
 * 节点格式 "id>物态"，边格式 "source>target>ruleId"。
 */
public class OpenGraphPacket {
    private final List<String> nodes;
    private final List<String> edges;

    public OpenGraphPacket(List<String> nodes, List<String> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }

    public static void encode(OpenGraphPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.nodes.size());
        for (String n : msg.nodes) buf.writeUtf(n);
        buf.writeVarInt(msg.edges.size());
        for (String e : msg.edges) buf.writeUtf(e);
    }

    public static OpenGraphPacket decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<String> nodes = new ArrayList<>(n);
        for (int i = 0; i < n; i++) nodes.add(buf.readUtf());
        int m = buf.readVarInt();
        List<String> edges = new ArrayList<>(m);
        for (int i = 0; i < m; i++) edges.add(buf.readUtf());
        return new OpenGraphPacket(nodes, edges);
    }

    public static void handle(OpenGraphPacket msg, Supplier<NetworkEvent.Context> ctx) {
        // 客户端代码集中在 client-only 处理器里：服务器端永不加载本 lambda 的目标类（§19.21）
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandlers.openReactionGraph(msg.nodes, msg.edges)));
        ctx.get().setPacketHandled(true);
    }
}
