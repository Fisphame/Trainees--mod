package com.pha.trainees.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pha.trainees.chemistry.util.IonDisplay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 反应图可视化界面（趣味调试功能）。
 * 节点按物态着色（GAS 蓝 / LIQUID 青 / AQUEOUS 绿 / SOLID 灰），边按规则着色。
 * 反应多了之后布局会重叠，本功能不追求完美，后期反应规模大后建议关闭。
 */
@OnlyIn(Dist.CLIENT)
public class ReactionGraphScreen extends Screen {

    private static final int[] EDGE_PALETTE = {
            0xFF4FC3F7, 0xFF81C784, 0xFFFFB74D, 0xFFE57373, 0xFFBA68C8,
            0xFF4DB6AC, 0xFFFFF176, 0xFFA1887F, 0xFF90A4AE, 0xFFF06292
    };

    private record NodeInfo(String id, int color) {}
    private record EdgeInfo(String source, String target, String rule) {}

    private final List<NodeInfo> nodes = new ArrayList<>();
    private final List<EdgeInfo> edges = new ArrayList<>();
    private final Map<String, Integer> edgeColors = new HashMap<>();

    public ReactionGraphScreen(List<String> nodeStrs, List<String> edgeStrs) {
        super(Component.translatable("gui.trainees.reaction_graph.title")
                .withStyle(net.minecraft.ChatFormatting.BOLD));
        for (String n : nodeStrs) {
            String[] p = n.split(">");
            if (p.length == 2) nodes.add(new NodeInfo(p[0], phaseColor(p[1])));
        }
        int colorIdx = 0;
        for (String e : edgeStrs) {
            String[] p = e.split(">");
            if (p.length == 3) {
                edges.add(new EdgeInfo(p[0], p[1], p[2]));
                edgeColors.putIfAbsent(p[2], EDGE_PALETTE[colorIdx++ % EDGE_PALETTE.length]);
            }
        }
    }

    private static int phaseColor(String phase) {
        return switch (phase) {
            case "GAS" -> 0xFF42A5F5;     // 蓝
            case "LIQUID" -> 0xFF26C6DA;  // 青
            case "AQUEOUS" -> 0xFF66BB6A; // 绿
            case "SOLID" -> 0xFFBCAAA4;   // 灰
            default -> 0xFFBDBDBD;
        };
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gg);
        if (edges.isEmpty()) {
            gg.drawCenteredString(this.font, "反应图为空", this.width / 2, this.height / 2, 0xFFFFFF);
            super.render(gg, mouseX, mouseY, partialTick);
            return;
        }

        // 圆形布局：节点均匀分布在一个圆上
        int cx = this.width / 2, cy = this.height / 2;
        int radius = Math.min(this.width, this.height) / 2 - 50;
        Map<String, int[]> pos = new HashMap<>();
        for (int i = 0; i < nodes.size(); i++) {
            double angle = 2.0 * Math.PI * i / nodes.size();
            pos.put(nodes.get(i).id(),
                    new int[]{cx + (int) (Math.cos(angle) * radius), cy + (int) (Math.sin(angle) * radius)});
        }

        // 先画边（连线）
        for (EdgeInfo e : edges) {
            int[] a = pos.get(e.source());
            int[] b = pos.get(e.target());
            if (a == null || b == null) continue;
            drawLine(gg, a[0], a[1], b[0], b[1], edgeColors.getOrDefault(e.rule(), 0xFFFFFFFF), 2);
        }
        // 边上的规则名
        for (EdgeInfo e : edges) {
            int[] a = pos.get(e.source());
            int[] b = pos.get(e.target());
            if (a == null || b == null) continue;
            gg.drawString(this.font, e.rule(),
                    (a[0] + b[0]) / 2, (a[1] + b[1]) / 2, edgeColors.getOrDefault(e.rule(), 0xFFFFFFFF));
        }
        // 节点方块 + 标签
        for (NodeInfo n : nodes) {
            int[] p = pos.get(n.id());
            if (p == null) continue;
            gg.fill(p[0] - 5, p[1] - 5, p[0] + 5, p[1] + 5, n.color());
            gg.fill(p[0] - 5, p[1] - 5, p[0] - 4, p[1] + 5, 0xFF000000);
            gg.fill(p[0] + 4, p[1] - 5, p[0] + 5, p[1] + 5, 0xFF000000);
            gg.fill(p[0] - 5, p[1] - 5, p[0] + 5, p[1] - 4, 0xFF000000);
            gg.fill(p[0] - 5, p[1] + 4, p[0] + 5, p[1] + 5, 0xFF000000);
            // 节点标签用化学式显示名
            gg.drawString(this.font, IonDisplay.format(n.id()), p[0] + 7, p[1] - 5, 0xFFFFFFFF);
        }
        gg.drawCenteredString(this.font,
                "ESC 关闭 | " + nodes.size() + " 节点 / " + edges.size() + " 边",
                this.width / 2, this.height - 20, 0xFFAAAAAA);

        super.render(gg, mouseX, mouseY, partialTick);
    }

    private void drawLine(GuiGraphics gg, int x1, int y1, int x2, int y2, int color, int thickness) {
        int dx = x2 - x1, dy = y2 - y1;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1) return;
        double angle = Math.atan2(dy, dx);
        PoseStack pose = gg.pose();
        pose.pushPose();
        pose.translate(x1, y1, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) Math.toDegrees(angle)));
        gg.fill(0, -thickness / 2, (int) len, thickness / 2, color);
        pose.popPose();
    }
}
