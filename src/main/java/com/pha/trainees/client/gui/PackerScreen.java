package com.pha.trainees.client.gui;

import com.pha.trainees.network.ModNetwork;
import com.pha.trainees.network.PackerActionPacket;
import com.pha.trainees.network.PackerStatePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 打包机面板（§19.18.5）：状态显示 + 三个操作按钮。
 *
 * <p>走"S2C 状态包 → 本 Screen；按钮发 C2S 动作包"的既有模式（同 {@link AnalyzerScreen}），
 * **不使用 {@code AbstractContainerMenu}**：本面板没有真实物品栏交互，只有几个数值与一个输出槽，
 * Menu 那套的窗口 id / 槽位同步 / 容器数据包都是净负担。</p>
 *
 * <p>服务端在每个动作后都会回发状态，{@code ClientPacketHandlers.openPacker} 会**就地刷新**
 * 同一个打包机的面板，所以点【取出产物】后输出槽立刻变空。</p>
 *
 * <p><b>自动刷新为什么选"客户端每 20 tick 拉一次"而不是"服务端推送"</b>（§19.18.5）：</p>
 * <ul>
 *   <li>客户端轮询**没有任何服务端状态**（不需要记"谁在看哪台机器"），关屏即停，实现最小；</li>
 *   <li>它顺带覆盖了**所有**状态变化，而不只是"自驱打出一瓶"——例如外部 AE2 输出总线把产物抽走、
 *       供电线缆正在充电、别人补了料。服务端推送方案里这些都不会触发面板更新；</li>
 *   <li>成本是 1 包/秒，只在面板打开时发生（{@link #onClose()} / {@link #removed()} 立即停止轮询）。</li>
 * </ul>
 *
 * <p><b>不暂停游戏</b>：{@link #isPauseScreen()} 返回 false——这是机器面板，玩家一边看状态一边
 * 还要看机器在动；与分析仪 Screen 的默认（会暂停）**有意不同**。</p>
 */
public class PackerScreen extends Screen {

    private static final int MARGIN = 16;
    private static final int PANEL_TOP = 42;
    /** 面板文字可用宽度（left 起算到面板右内边）：长文案自动换行，不再糊出面板 */
    private static final int PANEL_TEXT_WIDTH = 268;
    private static final int LINE_H = 10;
    private static final int ROW_GAP = 6;
    /** 标签 → 数值的缩进（沿用原来的 64 px） */
    private static final int VALUE_OFFSET = 64;

    /** 自动刷新周期（tick）：20 tick = 1 秒一次 */
    private static final int AUTO_REFRESH_INTERVAL = 20;

    private PackerStatePacket state;
    private int refreshCooldown = AUTO_REFRESH_INTERVAL;
    /** 关闭后停止轮询（tick 可能还会被调一两帧） */
    private boolean closed = false;

    public PackerScreen(PackerStatePacket state) {
        super(Component.translatable("gui.trainees.packer.title"));
        this.state = state;
    }

    /** 是否是同一个打包机（用于就地刷新，避免重开屏幕闪烁） */
    public boolean matches(BlockPos pos) {
        return state != null && state.pos().equals(pos);
    }

    public void updateState(PackerStatePacket newState) {
        this.state = newState;
    }

    @Override
    protected void init() {
        super.init();
        int buttonWidth = 100;
        int gap = 6;
        int totalWidth = buttonWidth * 3 + gap * 2;
        int startX = width / 2 - totalWidth / 2;
        int y = height - 30;

        addRenderableWidget(Button.builder(Component.translatable("gui.trainees.packer.button.take"),
                        b -> sendAction(PackerActionPacket.Action.TAKE_OUTPUT))
                .bounds(startX, y, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.trainees.packer.button.force"),
                        b -> sendAction(PackerActionPacket.Action.FORCE_FILL))
                .bounds(startX + buttonWidth + gap, y, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.trainees.packer.button.refresh"),
                        b -> sendAction(PackerActionPacket.Action.REQUEST_STATE))
                .bounds(startX + (buttonWidth + gap) * 2, y, buttonWidth, 20).build());
    }

    private void sendAction(PackerActionPacket.Action action) {
        if (state == null) return;
        ModNetwork.get().sendToServer(new PackerActionPacket(state.pos(), action));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        Minecraft mc = Minecraft.getInstance();

        // 标题 + 坐标
        Component title = Component.translatable("gui.trainees.packer.title").withStyle(ChatFormatting.GOLD);
        guiGraphics.drawString(mc.font, title, MARGIN, 12, 0xFFFFFF, false);
        if (state == null) return;
        guiGraphics.drawString(mc.font, Component.literal("§8" + state.pos().toShortString()),
                MARGIN + mc.font.width(title) + 8, 12, 0x888888, false);

        int left = width / 2 - 130;
        int right = width / 2 + 130;

        // 面板背景（与分析仪同款配色）
        guiGraphics.fill(left - 8, PANEL_TOP - 8, right + 8, height - 44, 0xCC000000);
        guiGraphics.fill(left - 8, PANEL_TOP - 8, right + 8, PANEL_TOP - 7, 0xFF555555);
        guiGraphics.fill(left - 8, height - 45, right + 8, height - 44, 0xFF555555);

        int y = PANEL_TOP;

        // 目标产品
        y = drawWrapped(guiGraphics, mc.font,
                Component.translatable("gui.trainees.packer.product", productName()),
                left, y, PANEL_TEXT_WIDTH, 0xFFFFFF) + ROW_GAP;

        // 电量：只留数值（§19.34：去掉电量条图形——它占 22 px 高却只是重复同一组数字）
        y = drawWrapped(guiGraphics, mc.font,
                Component.translatable("gui.trainees.packer.energy",
                        String.valueOf(state.energyStored()), String.valueOf(state.energyCapacity())),
                left, y, PANEL_TEXT_WIDTH, 0xFFFFFF) + ROW_GAP;

        // 输出槽：物品图标（或"空"）
        guiGraphics.drawString(mc.font, Component.translatable("gui.trainees.packer.output"), left, y, 0xFFFFFF, false);
        ItemStack output = state.output();
        if (output == null || output.isEmpty()) {
            y = drawWrapped(guiGraphics, mc.font,
                    Component.translatable("message.trainees.packer.output_empty"),
                    left + VALUE_OFFSET, y, PANEL_TEXT_WIDTH - VALUE_OFFSET, 0x888888) + ROW_GAP;
        } else {
            guiGraphics.renderItem(output, left + VALUE_OFFSET, y - 4);
            y = drawWrapped(guiGraphics, mc.font, output.getHoverName(),
                    left + VALUE_OFFSET + 22, y, PANEL_TEXT_WIDTH - VALUE_OFFSET - 22, 0xFFFFFF) + ROW_GAP;
        }

        // 最近结果（失败原因；成功时是空白）
        guiGraphics.drawString(mc.font, Component.translatable("gui.trainees.packer.last_result"), left, y, 0xFFFFFF, false);
        Component last = state.lastReasonKey() == null || state.lastReasonKey().isEmpty()
                ? Component.translatable("gui.trainees.packer.last_result.none")
                : Component.translatable(state.lastReasonKey());
        y = drawWrapped(guiGraphics, mc.font, last,
                left + VALUE_OFFSET, y, PANEL_TEXT_WIDTH - VALUE_OFFSET, 0xAAAAAA) + ROW_GAP;

        // 就绪状态：就绪 → 绿；未就绪 → 黄 + **原因**（面板最有用的信息："为什么没在打包"）
        if (state.canPackNow()) {
            drawWrapped(guiGraphics, mc.font, Component.translatable("gui.trainees.packer.ready"),
                    left, y, PANEL_TEXT_WIDTH, 0x66DD88);
        } else {
            String reasonKey = state.readyReasonKey();
            Component reason = reasonKey == null || reasonKey.isEmpty()
                    ? Component.translatable("gui.trainees.packer.not_ready")
                    : Component.translatable("gui.trainees.packer.not_ready_reason",
                            Component.translatable(reasonKey));
            drawWrapped(guiGraphics, mc.font, reason, left, y, PANEL_TEXT_WIDTH, 0xDDAA55);
        }

        // 组件（按钮）最后绘制，保证在面板之上
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    /**
     * 画一段可能超宽的文字（§19.34）：按面板宽度换行，返回**下一行的 y**。
     * 原来所有行都是单行 {@code drawString}，长文案（次品原因 / 物品名）会直接糊出面板右边。
     */
    private int drawWrapped(GuiGraphics guiGraphics, Font font, Component text, int x, int y, int maxWidth, int color) {
        List<FormattedCharSequence> lines = font.split(text, maxWidth);
        if (lines.isEmpty()) return y + LINE_H;
        for (FormattedCharSequence line : lines) {
            guiGraphics.drawString(font, line, x, y, color, false);
            y += LINE_H;
        }
        return y;
    }

    /** 目标产品显示名：规格短名拼语言键（V1 只有 bleach） */
    private Component productName() {
        String id = state == null ? null : state.productId();
        if (id == null || id.isEmpty()) return Component.literal("?");
        int colon = id.indexOf(':');
        String path = colon >= 0 ? id.substring(colon + 1) : id;
        return Component.translatable("gui.trainees.packer.product." + path);
    }

    /** 自动刷新：每 20 tick 向服务端拉一次状态（理由见类注释）；关屏后立即停止 */
    @Override
    public void tick() {
        super.tick();
        if (closed) return;
        if (--refreshCooldown > 0) return;
        refreshCooldown = AUTO_REFRESH_INTERVAL;
        sendAction(PackerActionPacket.Action.REQUEST_STATE);
    }

    /** 机器面板不该暂停游戏（与分析仪 Screen 的默认有意不同） */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        closed = true;              // 双保险：即使不经过 onClose 也停轮询
        super.removed();
    }

    @Override
    public void onClose() {
        closed = true;              // 离开界面即停止轮询，避免后台一直发包
        Minecraft.getInstance().setScreen(null);
    }
}
