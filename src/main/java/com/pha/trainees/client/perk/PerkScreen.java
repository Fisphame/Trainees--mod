package com.pha.trainees.client.perk;

import com.pha.trainees.network.ModNetwork;
import com.pha.trainees.network.PerkPackets;
import com.pha.trainees.perk.PerkTable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 属性点屏幕（§17.1 P3）。
 *
 * <p>布局：**一行一个类别**（生存/战斗/机动/魔感/功能），行内是该类别的属性按钮；
 * 按钮上写"已点/上限"，悬停看详情（名称、下一级价格、前置、操作提示）。
 * **左键点亮、右键回退**（作者：可自由回退、原价返还）。</p>
 *
 * <p>服务端权威：这里只负责"画"和"发请求"，点数、前置、满级都在服务端判。
 * 打开时会发一个空 id 的请求把最新状态拉回来。</p>
 */
public class PerkScreen extends Screen {

    private static final int BUTTON = 26;
    private static final int SPACING = 30;
    private static final int ROW_HEIGHT = 34;
    private static final int LABEL_X = 20;
    private static final int START_X = 104;

    public PerkScreen() {
        super(Component.translatable("screen.trainees.perks"));
    }

    @Override
    protected void init() {
        super.init();
        // 打开就拉一次状态（空 id = 只请求状态）
        ModNetwork.get().sendToServer(new PerkPackets.Action("", false));
    }

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);
        gui.drawCenteredString(font, title, width / 2, 12, 0xFFFFFFFF);
        gui.drawCenteredString(font, Component.translatable("screen.trainees.perks.points",
                        ClientPerkState.available()),
                width / 2, 26, 0xFFD0A0FF);

        int rowY = 48;
        for (PerkTable.Category category : PerkTable.Category.values()) {
            List<PerkTable> row = new ArrayList<>();
            for (PerkTable perk : PerkTable.all()) {
                if (perk.category == category) row.add(perk);
            }
            gui.drawString(font, Component.translatable(category.nameKey), LABEL_X, rowY + 9, 0xFFAAAAAA);

            int x = START_X;
            for (PerkTable perk : row) {
                drawPerk(gui, perk, x, rowY, mouseX, mouseY);
                x += SPACING;
            }
            rowY += ROW_HEIGHT;
        }

        gui.drawCenteredString(font, Component.translatable("screen.trainees.perks.hint"),
                width / 2, height - 16, 0xFF808080);
        drawRespecButton(gui, mouseX, mouseY);
        super.render(gui, mouseX, mouseY, partialTick);
    }

    // ==================== A3：洗点按钮（逻辑早就有，只差一个入口） ====================

    private static final int RESPEC_WIDTH = 96;
    private static final int RESPEC_HEIGHT = 16;

    private int respecX() {
        return width / 2 - RESPEC_WIDTH / 2;
    }

    private int respecY() {
        return height - 36;
    }

    private void drawRespecButton(GuiGraphics gui, int mouseX, int mouseY) {
        int x = respecX();
        int y = respecY();
        boolean hovered = mouseX >= x && mouseX < x + RESPEC_WIDTH && mouseY >= y && mouseY < y + RESPEC_HEIGHT;
        gui.fill(x, y, x + RESPEC_WIDTH, y + RESPEC_HEIGHT, hovered ? 0xFF6A2A6A : 0xFF402040);
        gui.fill(x, y, x + RESPEC_WIDTH, y + 1, 0xFFB060FF);
        gui.fill(x, y + RESPEC_HEIGHT - 1, x + RESPEC_WIDTH, y + RESPEC_HEIGHT, 0xFFB060FF);
        gui.drawCenteredString(font, Component.translatable("screen.trainees.perks.respec"),
                x + RESPEC_WIDTH / 2, y + 4, 0xFFFFFFFF);
    }

    private void drawPerk(GuiGraphics gui, PerkTable perk, int x, int y, int mouseX, int mouseY) {
        int rank = ClientPerkState.rank(perk.id);
        boolean maxed = rank >= perk.maxRank;
        boolean prereqOk = perk.prereq == null || ClientPerkState.rank(perk.prereq.perkId()) >= perk.prereq.rank();
        boolean hovered = mouseX >= x && mouseX < x + BUTTON && mouseY >= y && mouseY < y + BUTTON;

        int fill = switch (perk.category) {
            case VITALITY -> 0xFF3A5A3A;
            case MIGHT -> 0xFF5A3A3A;
            case MOBILITY -> 0xFF3A4A5A;
            case ESSENCE -> 0xFF4A2A5A;
            case UTILITY -> 0xFF4A4A2A;
        };
        int alpha = prereqOk ? 0xFF000000 : 0x60000000;
        gui.fill(x, y, x + BUTTON, y + BUTTON, alpha | (fill & 0xFFFFFF));
        int border = maxed ? 0xFFFFD24A : (hovered ? 0xFFB060FF : (prereqOk ? 0xFF808080 : 0xFF404040));
        gui.fill(x, y, x + BUTTON, y + 1, border);
        gui.fill(x, y + BUTTON - 1, x + BUTTON, y + BUTTON, border);
        gui.fill(x, y, x + 1, y + BUTTON, border);
        gui.fill(x + BUTTON - 1, y, x + BUTTON, y + BUTTON, border);

        // 只有已点的属性才显示名字缩写，避免过密（详情看悬停）
        gui.drawCenteredString(font, rank + "/" + perk.maxRank, x + BUTTON / 2, y + 9,
                rank > 0 ? 0xFFFFFFFF : 0xFF909090);

        if (hovered) {
            renderTooltip(gui, perk, rank, prereqOk, mouseX, mouseY);
        }
    }

    private void renderTooltip(GuiGraphics gui, PerkTable perk, int rank, boolean prereqOk,
                               int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(perk.nameKey));
        // A2：描述文案（每条一句，数值与 PerkTable 一致；文案本身带颜色码，不用再 withStyle）
        lines.add(Component.translatable(perk.nameKey + ".desc"));
        lines.add(Component.translatable("screen.trainees.perks.rank", rank, perk.maxRank));
        int next = perk.costFor(rank);
        if (next >= 0) {
            lines.add(Component.translatable("screen.trainees.perks.next_cost", next));
        }
        if (perk.prereq != null && !prereqOk) {
            PerkTable required = PerkTable.byId(perk.prereq.perkId());
            lines.add(Component.translatable("screen.trainees.perks.requires",
                    required == null ? Component.literal(perk.prereq.perkId())
                            : Component.translatable(required.nameKey),
                    perk.prereq.rank()));
        }
        lines.add(Component.translatable("screen.trainees.perks.tip"));
        gui.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 && button != 1) return super.mouseClicked(mouseX, mouseY, button);

        // A3：洗点按钮（空 id 以外的特殊值 "*" = 一键洗点并全额返还）
        int rx = respecX();
        int ry = respecY();
        if (button == 0 && mouseX >= rx && mouseX < rx + RESPEC_WIDTH
                && mouseY >= ry && mouseY < ry + RESPEC_HEIGHT) {
            ModNetwork.get().sendToServer(new PerkPackets.Action("*", false));
            return true;
        }

        int rowY = 48;
        for (PerkTable.Category category : PerkTable.Category.values()) {
            int x = START_X;
            for (PerkTable perk : PerkTable.all()) {
                if (perk.category != category) continue;
                if (mouseX >= x && mouseX < x + BUTTON && mouseY >= rowY && mouseY < rowY + BUTTON) {
                    ModNetwork.get().sendToServer(new PerkPackets.Action(perk.id, button == 1));
                    return true;
                }
                x += SPACING;
            }
            rowY += ROW_HEIGHT;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
