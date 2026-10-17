package com.pha.trainees.client;

import com.pha.trainees.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 精炼精华 HUD（§17.1 P1.5）：**左上角**一行"RE 存量 / 上限" + 一条细能量条。
 *
 * <p>为什么放左上角：经验条那一带已经被动能 HUD 用了，右上/右下是药水与状态区；
 * 左上角干净、不挡视野，也不会和原版元素打架（想挪位置只改这里的 {@code X/Y}）。</p>
 *
 * <p>显示规则（{@link ClientManaState#shouldShow()}）：有精华就常驻；抽空之后还停留 3 秒
 * ——否则一抽到底数字就消失，玩家会以为没生效。</p>
 */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ManaHud {

    private static final int X = 6;
    private static final int Y = 6;
    private static final int BAR_WIDTH = 96;
    private static final int BAR_HEIGHT = 4;

    private ManaHud() {
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.EXPERIENCE_BAR.id())) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) return;
        if (!ClientManaState.shouldShow()) return;

        GuiGraphics gui = event.getGuiGraphics();
        String text = ClientManaState.stored() + " / " + ClientManaState.capacity() + " RE";
        gui.drawString(minecraft.font, text, X, Y, 0xFFD0A0FF);

        int barY = Y + minecraft.font.lineHeight + 1;
        gui.fill(X, barY, X + BAR_WIDTH, barY + BAR_HEIGHT, 0xC0000000);
        int fill = (int) (BAR_WIDTH * Math.min(1f, Math.max(0f, ClientManaState.fillRatio())));
        if (fill > 0) {
            gui.fill(X, barY, X + fill, barY + BAR_HEIGHT, 0xFFB060FF);
        }
    }
}
