package com.pha.trainees.client.machine;

import com.pha.trainees.machine.FaceConfig;
import com.pha.trainees.menu.MachineHost;
import com.pha.trainees.menu.MachineMenu;
import com.pha.trainees.util.NumberFormatUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

/**
 * 魔法机器的统一屏幕（§17.1 魔法机器 GUI 化）：**一个类，按 {@link MachineMenu.Kind} 参数化**。
 *
 * <p>**全部代码绘制、不用贴图**（与本 mod 的 JEI 分类页同一套做法）：面板底、槽位框、电量条、
 * 进度箭头、富集仓的 4 个液面——都按常量画，所以不存在"贴图尺寸和槽位对不上"的经典毛病。</p>
 *
 * <p>数值全部来自 {@code ContainerData}（服务端权威、自动同步），屏幕只负责画与交互；
 * Shift+点击的快速转移在菜单里实现。</p>
 */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {

    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int SLOT_BG = 0xFF373737;
    private static final int SLOT_EDGE = 0xFF8B8B8B;
    private static final int ENERGY_COLOR = 0xFFB060FF;
    private static final int PROGRESS_COLOR = 0xFF55FF88;

    public MachineScreen(MachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = menu.kind().playerInvY() + 84;
        this.inventoryLabelY = menu.kind().playerInvY() - 12;
    }

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        gui.fill(x, y, x + imageWidth, y + imageHeight, PANEL);
        gui.fill(x, y, x + imageWidth, y + 1, 0xFFFFFFFF);
        gui.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, PANEL_DARK);
        gui.fill(x, y, x + 1, y + imageHeight, 0xFFFFFFFF);
        gui.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, PANEL_DARK);

        // 槽位框（含机器槽与玩家背包槽）
        for (Slot slot : menu.slots) {
            gui.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, SLOT_EDGE);
            gui.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, SLOT_BG);
        }

        drawEnergyBar(gui, x + 8, y + 8);
        drawProgressArrow(gui, x + 76, y + 32);
        if (menu.kind() == MachineMenu.Kind.ENRICHMENT) {
            drawTanks(gui, x + 150, y + 12);
        }
        drawFaceButtons(gui, x + imageWidth - 18, y + 8, mouseX, mouseY);
        if (menu.kind() == MachineMenu.Kind.PORT) {
            drawPortButtons(gui, x + imageWidth - 18, y + 8, mouseX, mouseY);
        }
    }

    // ==================== 端口吞吐调节按钮（§17.1 ④） ====================
    // 视觉由作者微调：这里是 3 个可点方块（减半 / 加倍 / 恢复默认），逻辑走原版菜单按钮。

    private static final int PORT_BTN_SIZE = 10;
    private static final int PORT_BTN_STEP = 12;
    private static final int[] PORT_BTN_IDS = {
            MachineMenu.BUTTON_THROUGHPUT_HALF,
            MachineMenu.BUTTON_THROUGHPUT_DOUBLE,
            MachineMenu.BUTTON_THROUGHPUT_DEFAULT
    };

    private void drawPortButtons(GuiGraphics gui, int bx, int by, int mouseX, int mouseY) {
        int[] colors = {0xFFAA5555, 0xFF55AA55, 0xFF888888};
        for (int i = 0; i < PORT_BTN_IDS.length; i++) {
            int y = by + i * PORT_BTN_STEP;
            gui.fill(bx, y, bx + PORT_BTN_SIZE, y + PORT_BTN_SIZE, 0xFF202020);
            gui.fill(bx + 1, y + 1, bx + PORT_BTN_SIZE - 1, y + PORT_BTN_SIZE - 1, colors[i]);
            if (isInside(mouseX, mouseY, bx, y, PORT_BTN_SIZE)) {
                gui.fill(bx, y, bx + PORT_BTN_SIZE, y + 1, 0xFFFFFFFF);
                gui.fill(bx, y + PORT_BTN_SIZE - 1, bx + PORT_BTN_SIZE, y + PORT_BTN_SIZE, 0xFFFFFFFF);
                gui.fill(bx, y, bx + 1, y + PORT_BTN_SIZE, 0xFFFFFFFF);
                gui.fill(bx + PORT_BTN_SIZE - 1, y, bx + PORT_BTN_SIZE, y + PORT_BTN_SIZE, 0xFFFFFFFF);
            }
        }
    }

    // ==================== 按面 I/O 按钮（§17.1） ====================
    // 视觉部分由作者设计：这里只是"6 个可点方块 + 1 个重置"，逻辑走原版菜单按钮，无需网络包。

    private static final int FACE_BUTTON_SIZE = 12;
    private static final int FACE_BUTTON_STEP = 15;

    /** 每个面画一个小方块，颜色代表模式；最下面一个画重置。 */
    private void drawFaceButtons(GuiGraphics gui, int x, int y, int mouseX, int mouseY) {
        if (!menu.hasFaceConfig()) return;
        for (int face = 0; face < FaceConfig.FACE_COUNT; face++) {
            int by = y + face * FACE_BUTTON_STEP;
            boolean hovered = isInside(mouseX, mouseY, x, by);
            int color = modeColor(menu.faceMode(face));
            gui.fill(x, by, x + FACE_BUTTON_SIZE, by + FACE_BUTTON_SIZE, 0xFF202020);
            gui.fill(x + 1, by + 1, x + FACE_BUTTON_SIZE - 1, by + FACE_BUTTON_SIZE - 1, color);
            if (hovered) {
                gui.fill(x, by, x + FACE_BUTTON_SIZE, by + 1, 0xFFFFFFFF);
                gui.fill(x, by + FACE_BUTTON_SIZE - 1, x + FACE_BUTTON_SIZE, by + FACE_BUTTON_SIZE, 0xFFFFFFFF);
                gui.fill(x, by, x + 1, by + FACE_BUTTON_SIZE, 0xFFFFFFFF);
                gui.fill(x + FACE_BUTTON_SIZE - 1, by, x + FACE_BUTTON_SIZE, by + FACE_BUTTON_SIZE, 0xFFFFFFFF);
            }
        }
        // 重置按钮（画成空心方块）
        int ry = y + FaceConfig.FACE_COUNT * FACE_BUTTON_STEP + 4;
        gui.fill(x, ry, x + FACE_BUTTON_SIZE, ry + FACE_BUTTON_SIZE, 0xFF202020);
        gui.fill(x + 2, ry + 2, x + FACE_BUTTON_SIZE - 2, ry + FACE_BUTTON_SIZE - 2, 0xFF808080);
    }

    private int modeColor(int ordinal) {
        FaceConfig.Mode[] modes = FaceConfig.Mode.values();
        FaceConfig.Mode mode = modes[Math.max(0, Math.min(modes.length - 1, ordinal))];
        return switch (mode) {
            case UNSET -> 0xFF606060;
            case ITEM_IN -> 0xFF4A90D9;
            case FLUID_IN -> 0xFF33AACC;
            case ITEM_OUT -> 0xFFD9A04A;
            case FLUID_OUT -> 0xFFCC8833;
            case DISABLED -> 0xFF303030;
        };
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y) {
        return isInside(mouseX, mouseY, x, y, FACE_BUTTON_SIZE);
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int size) {
        return mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && minecraft != null && minecraft.gameMode != null) {
            // 端口的吞吐调节按钮
            if (menu.kind() == MachineMenu.Kind.PORT) {
                int bx = leftPos + imageWidth - 18;
                int by = topPos + 8;
                for (int i = 0; i < PORT_BTN_IDS.length; i++) {
                    if (isInside(mouseX, mouseY, bx, by + i * PORT_BTN_STEP, PORT_BTN_SIZE)) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, PORT_BTN_IDS[i]);
                        return true;
                    }
                }
            }
            // 面配置按钮
            if (menu.hasFaceConfig()) {
                int x = leftPos + imageWidth - 18;
                int y = topPos + 8;
                for (int face = 0; face < FaceConfig.FACE_COUNT; face++) {
                    if (isInside(mouseX, mouseY, x, y + face * FACE_BUTTON_STEP)) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, face);
                        return true;
                    }
                }
                int ry = y + FaceConfig.FACE_COUNT * FACE_BUTTON_STEP + 4;
                if (isInside(mouseX, mouseY, x, ry)) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, MachineMenu.BUTTON_RESET_FACES);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** 电量条：左侧竖条，按 存量/容量 填充。 */
    private void drawEnergyBar(GuiGraphics gui, int x, int y) {
        int height = 52;
        gui.fill(x - 1, y - 1, x + 13, y + height + 1, SLOT_EDGE);
        gui.fill(x, y, x + 12, y + height, SLOT_BG);
        int fill = Math.round(height * (menu.energy() / (float) menu.capacity()));
        if (fill > 0) {
            gui.fill(x, y + height - fill, x + 12, y + height, ENERGY_COLOR);
        }
    }

    /** 进度箭头：中间一条横条（总时长为 0 时不画）。 */
    private void drawProgressArrow(GuiGraphics gui, int x, int y) {
        int total = menu.totalTicks();
        gui.fill(x - 1, y - 1, x + 25, y + 9, SLOT_EDGE);
        gui.fill(x, y, x + 24, y + 8, SLOT_BG);
        if (total > 0) {
            int fill = Math.round(24 * Math.min(1.0F, menu.progress() / (float) total));
            if (fill > 0) {
                gui.fill(x, y, x + fill, y + 8, PROGRESS_COLOR);
            }
        }
    }

    /** 富集仓的 4 个液面（输入 / 催化剂 / 两个输出），按 mB / 4000 填充。 */
    private void drawTanks(GuiGraphics gui, int x, int y) {
        MachineHost host = menu.host();
        IFluidHandler tanks = host == null ? null : host.fluids();
        if (tanks == null) return;
        for (int i = 0; i < Math.min(4, tanks.getTanks()); i++) {
            int tx = x + i * 6;
            gui.fill(tx, y - 1, tx + 5, y + 53, SLOT_EDGE);
            gui.fill(tx + 1, y, tx + 4, y + 52, SLOT_BG);
            int amount = tanks.getFluidInTank(i).getAmount();
            int capacity = Math.max(1, tanks.getTankCapacity(i));
            int fill = Math.round(52 * (amount / (float) capacity));
            if (fill > 0) {
                gui.fill(tx + 1, y + 52 - fill, tx + 4, y + 52, 0xFF4A90D9);
            }
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(font, title, 8, 6, 0x404040, false);
        gui.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0x404040, false);
        // 数值读数（贴在电量条下方，避免和标题挤）；大数字用 k/M 缩写
        gui.drawString(font, NumberFormatUtil.compact(menu.energy()) + " / "
                + NumberFormatUtil.compact(menu.capacity()) + " RE", 4, imageHeight - 62, 0x606060, false);
        if (menu.kind() == MachineMenu.Kind.PORT) {
            drawPortInfo(gui);
        }
    }

    /**
     * 精练精华网端口的网络信息（§17.1 ④）。作者说"先写上，后续微调"，
     * 所以这里只把该看的数摆出来：结构 / 缓存单元 / 吞吐 / 驱动 / 是否截断。
     */
    private void drawPortInfo(GuiGraphics gui) {
        int y = 22;
        int step = 11;
        gui.drawString(font, Component.translatable(menu.structureFormed()
                ? "screen.trainees.port.formed" : "screen.trainees.port.not_formed"), 30, y, 0x404040, false);
        y += step;
        gui.drawString(font, Component.translatable("screen.trainees.port.units",
                NumberFormatUtil.compact(menu.networkUnits())), 30, y, 0x404040, false);
        y += step;
        gui.drawString(font, Component.translatable("screen.trainees.port.throughput",
                NumberFormatUtil.compact(menu.throughput())), 30, y, 0x404040, false);
        y += step;
        gui.drawString(font, Component.translatable(menu.poweredByUpkeep()
                ? "screen.trainees.port.powered" : "screen.trainees.port.unpowered"),
                30, y, menu.poweredByUpkeep() ? 0x2E7D4F : 0xA03030, false);
        if (menu.networkTruncated()) {
            gui.drawString(font, Component.translatable("screen.trainees.port.truncated"),
                    30, y + step, 0xA03030, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
