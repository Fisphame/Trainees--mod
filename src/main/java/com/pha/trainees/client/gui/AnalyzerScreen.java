package com.pha.trainees.client.gui;

import com.pha.trainees.chemistry.report.ReportLine;
import com.pha.trainees.network.AnalyzerReportPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 分析仪诊断面板（§19.6/19.7 方案 a）：标签页 = 诊断 / 预测。
 *
 * <p>服务端只发结构化行（翻译键 + 参数），这里负责**本地化渲染**（§19.16），
 * 因此语言的切换随客户端语言生效；客户端不做任何化学计算。</p>
 */
public class AnalyzerScreen extends Screen {

    private static final int TAB_DIAGNOSIS = 0;
    private static final int TAB_PREDICTION = 1;

    private static final int MARGIN = 16;
    private static final int TOP = 56;
    private static final int BOTTOM_RESERVED = 40;

    private final AnalyzerReportPacket report;
    private final List<AbstractWidget> widgets = new ArrayList<>();

    private int tab = TAB_DIAGNOSIS;
    private int scroll = 0;
    private int maxScroll = 0;
    private Button diagTabButton;
    private Button predTabButton;

    public AnalyzerScreen(AnalyzerReportPacket report) {
        super(Component.translatable("gui.trainees.analyzer.screen_title"));
        this.report = report;
    }

    @Override
    protected void init() {
        super.init();
        this.widgets.clear();

        int tabWidth = 90;
        int gap = 6;
        int startX = width / 2 - tabWidth - gap / 2;

        diagTabButton = Button.builder(
                Component.translatable("gui.trainees.analyzer.tab.diagnosis"),
                b -> switchTab(TAB_DIAGNOSIS)
        ).bounds(startX, 26, tabWidth, 20).build();

        predTabButton = Button.builder(
                Component.translatable("gui.trainees.analyzer.tab.prediction"),
                b -> switchTab(TAB_PREDICTION)
        ).bounds(startX + tabWidth + gap, 26, tabWidth, 20).build();

        Button closeButton = Button.builder(
                Component.translatable("gui.trainees.analyzer.button.close"),
                b -> onClose()
        ).bounds(width / 2 - 40, height - 28, 80, 20).build();

        widgets.add(diagTabButton);
        widgets.add(predTabButton);
        widgets.add(closeButton);
        for (AbstractWidget w : widgets) {
            this.addRenderableWidget(w);
        }
        refreshTabs();
    }

    private void switchTab(int newTab) {
        if (tab == newTab) return;
        tab = newTab;
        scroll = 0;
        refreshTabs();
    }

    private void refreshTabs() {
        if (diagTabButton != null) diagTabButton.active = tab != TAB_DIAGNOSIS;
        if (predTabButton != null) predTabButton.active = tab != TAB_PREDICTION;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        Minecraft mc = Minecraft.getInstance();

        // 标题：分析仪 + 容器名
        Component title = Component.translatable("gui.trainees.analyzer.screen_title")
                .withStyle(ChatFormatting.GOLD);
        guiGraphics.drawString(mc.font, title, MARGIN, 10, 0xFFFFFF, false);
        guiGraphics.drawString(mc.font,
                Component.translatable(report.containerId()).withStyle(ChatFormatting.GRAY),
                MARGIN + mc.font.width(title) + 6, 10, 0xAAAAAA, false);

        // 内容区
        List<Component> lines = collectLines();
        int lineHeight = mc.font.lineHeight + 2;
        int viewTop = TOP;
        int viewBottom = height - BOTTOM_RESERVED;
        int viewHeight = Math.max(1, viewBottom - viewTop);
        int contentHeight = lines.size() * lineHeight + 8;
        maxScroll = Math.max(0, contentHeight - viewHeight);
        scroll = Math.max(0, Math.min(maxScroll, scroll));

        // 面板背景
        guiGraphics.fill(MARGIN - 4, viewTop - 4, width - MARGIN + 4, viewBottom + 4, 0xCC000000);
        guiGraphics.fill(MARGIN - 4, viewTop - 4, width - MARGIN + 4, viewTop - 3, 0xFF555555);
        guiGraphics.fill(MARGIN - 4, viewBottom + 3, width - MARGIN + 4, viewBottom + 4, 0xFF555555);

        // 逐行绘制（含滚动裁剪）
        int y = viewTop + 4 - scroll;
        for (Component line : lines) {
            if (y + mc.font.lineHeight >= viewTop && y <= viewBottom) {
                guiGraphics.drawString(mc.font, line, MARGIN, y, 0xFFFFFF, false);
            }
            y += lineHeight;
        }

        // 滚动提示
        if (maxScroll > 0) {
            String hint = Component.translatable("gui.trainees.analyzer.hint.scroll").getString();
            guiGraphics.drawString(mc.font, Component.literal("§8" + hint),
                    width - MARGIN - mc.font.width(hint), height - 38, 0x888888, false);
        }

        // 组件（标签页 + 关闭）最后绘制，保证在面板之上
        for (AbstractWidget w : widgets) {
            w.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0) {
            scroll = Math.max(0, Math.min(maxScroll, (int) (scroll - delta * 12)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }

    private void addGroup(List<Component> out, String headerKey, List<ReportLine> body) {
        out.add(Component.translatable(headerKey).withStyle(ChatFormatting.GOLD));
        for (ReportLine line : body) {
            out.add(render(line));
        }
        out.add(Component.empty());
    }

    /** 结构化行 → 本地化组件：参数里的"嵌套翻译键"再翻译一次 */
    private Component render(ReportLine line) {
        Object[] args = line.args().stream()
                .map(arg -> arg.translatable() ? (Object) Component.translatable(arg.value()) : (Object) arg.value())
                .toArray();
        return Component.translatable(line.key(), args);
    }

    private List<Component> collectLines() {
        List<Component> out = new ArrayList<>();
        if (tab == TAB_DIAGNOSIS) {
            addGroup(out, "gui.trainees.analyzer.section.summary", report.summary());
            addGroup(out, "gui.trainees.analyzer.section.contents", report.contents());
            addGroup(out, "gui.trainees.analyzer.section.failures", report.failures());
            addGroup(out, "gui.trainees.analyzer.section.executed", report.executed());
        } else {
            addGroup(out, "gui.trainees.analyzer.section.executable", report.executable());
            addGroup(out, "gui.trainees.analyzer.section.blocked", report.blocked());
        }
        return out;
    }
}
