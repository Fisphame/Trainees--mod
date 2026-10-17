package com.pha.trainees.client;

import com.pha.trainees.Main;
import com.pha.trainees.network.ManaKeyPacket;
import com.pha.trainees.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 按键 → 网络包（§17.1）。客户端只负责"我按了、我有没有潜行"，
 * **转移的合法性全在服务端**（见 {@link ManaKeyPacket}）。
 */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ManaKeyHandler {

    private ManaKeyHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null) {
            // 开着界面时把积压的点击吃掉，免得关掉界面后一次性补发好几发
            while (ManaKeybinds.USE_MANA.consumeClick()) {
                // 故意空转
            }
            return;
        }
        while (ManaKeybinds.USE_MANA.consumeClick()) {
            ModNetwork.get().sendToServer(new ManaKeyPacket(player.isShiftKeyDown()));
        }
    }
}
