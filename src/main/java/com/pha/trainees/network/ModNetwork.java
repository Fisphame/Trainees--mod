package com.pha.trainees.network;

import com.pha.trainees.Main;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 模组网络通道（简单网络，用于反应图可视化等客户端展示）
 */
public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Main.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, OpenGraphPacket.class,
                OpenGraphPacket::encode, OpenGraphPacket::decode, OpenGraphPacket::handle);
        CHANNEL.registerMessage(id++, AnalyzerReportPacket.class,
                AnalyzerReportPacket::encode, AnalyzerReportPacket::decode, AnalyzerReportPacket::handle);
        // 打包机面板（§19.18.5）：S2C 状态 + C2S 动作
        CHANNEL.registerMessage(id++, PackerStatePacket.class,
                PackerStatePacket::encode, PackerStatePacket::decode, PackerStatePacket::handle);
        CHANNEL.registerMessage(id++, PackerActionPacket.class,
                PackerActionPacket::encode, PackerActionPacket::decode, PackerActionPacket::handle);
        // 玩家魔力按键（§17.1）：R 注能 / 潜行+R 抽取
        CHANNEL.registerMessage(id++, ManaKeyPacket.class,
                ManaKeyPacket::encode, ManaKeyPacket::decode, ManaKeyPacket::handle);
        // 玩家精华状态（S2C，§17.1 P1.5：给 HUD 用；只在变化时发）
        CHANNEL.registerMessage(id++, ManaStatePacket.class,
                ManaStatePacket::encode, ManaStatePacket::decode, ManaStatePacket::handle);
        // 加点系统（§17.1 P3）：C2S 点亮/回退/请求，S2C 状态
        CHANNEL.registerMessage(id++, PerkPackets.Action.class,
                PerkPackets.Action::encode, PerkPackets.Action::decode, PerkPackets.Action::handle);
        CHANNEL.registerMessage(id++, PerkPackets.State.class,
                PerkPackets.State::encode, PerkPackets.State::decode, PerkPackets.State::handle);
    }

    public static SimpleChannel get() {
        return CHANNEL;
    }
}
