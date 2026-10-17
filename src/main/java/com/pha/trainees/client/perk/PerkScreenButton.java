package com.pha.trainees.client.perk;

import com.pha.trainees.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 物品栏界面左上角的"属性点"入口按钮（§17.1 P3，作者定的入口方式）。
 *
 * <p>为什么不用"手持物品右键"或按键：作者要**界面上的按钮**——找得到、不占键位、不依赖手持物。</p>
 *
 * <p>位置：面板左上角的**外侧**一点点（`left − 20, top`），这样不会压到原版的 "Crafting" 标签；
 * 想挪到面板内侧只改 {@link #BUTTON_SIZE}/{@link #OUTSIDE_GAP} 那两个常量。
 * 图标按作者要求**先空着**：只画底与边框。</p>
 *
 * <p>只认生存物品栏与创造物品栏（两者的 GUI 尺寸不同，见 {@link #layoutOf(Screen)}）。</p>
 */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class PerkScreenButton {

    private static final int BUTTON_SIZE = 18;
    /** 与面板左边缘的水平间距（正数 = 在面板外/左）。 */
    private static final int OUTSIDE_GAP = 20;

    private PerkScreenButton() {
    }

    /** 按钮在屏幕上的位置；不支持界面返回 null。 */
    private static int[] buttonPos(Screen screen) {
        int[] layout = layoutOf(screen);
        if (layout == null) return null;
        int left = (screen.width - layout[0]) / 2;
        int top = (screen.height - layout[1]) / 2;
        return new int[]{Math.max(2, left - OUTSIDE_GAP), top};
    }

    /** {宽, 高}：原版生存物品栏 176×166、创造物品栏 195×136。 */
    private static int[] layoutOf(Screen screen) {
        if (screen instanceof InventoryScreen) return new int[]{176, 166};
        if (screen instanceof CreativeModeInventoryScreen) return new int[]{195, 136};
        return null;
    }

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        int[] pos = buttonPos(screen);
        if (pos == null) return;

        GuiGraphics gui = event.getGuiGraphics();
        int x = pos[0];
        int y = pos[1];
        boolean hovered = event.getMouseX() >= x && event.getMouseX() < x + BUTTON_SIZE
                && event.getMouseY() >= y && event.getMouseY() < y + BUTTON_SIZE;

        // 底 + 边框（图标先空着；悬停变亮，给一点"能点"的反馈）
        gui.fill(x, y, x + BUTTON_SIZE, y + BUTTON_SIZE, hovered ? 0xFF3A2A55 : 0xFF241A38);
        int border = hovered ? 0xFFB060FF : 0xFF6A4A9A;
        gui.fill(x, y, x + BUTTON_SIZE, y + 1, border);
        gui.fill(x, y + BUTTON_SIZE - 1, x + BUTTON_SIZE, y + BUTTON_SIZE, border);
        gui.fill(x, y, x + 1, y + BUTTON_SIZE, border);
        gui.fill(x + BUTTON_SIZE - 1, y, x + BUTTON_SIZE, y + BUTTON_SIZE, border);
    }

    @SubscribeEvent
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != 0) return;
        int[] pos = buttonPos(event.getScreen());
        if (pos == null) return;
        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        if (mouseX < pos[0] || mouseX >= pos[0] + BUTTON_SIZE
                || mouseY < pos[1] || mouseY >= pos[1] + BUTTON_SIZE) {
            return;
        }

        event.setCanceled(true);
        Minecraft minecraft = Minecraft.getInstance();
        // 延到下一帧再开屏：正处在原版界面的输入处理里，直接 setScreen 容易踩到重入
        minecraft.execute(() -> minecraft.setScreen(new PerkScreen()));
    }
}
