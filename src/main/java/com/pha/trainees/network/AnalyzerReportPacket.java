package com.pha.trainees.network;

import com.pha.trainees.chemistry.report.ReportLine;
import com.pha.trainees.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 分析仪诊断报告包（§19.6/19.7 方案 a：服务端组装 → 客户端 GUI）。
 *
 * <p>内容是**结构化的** {@link ReportLine}（翻译键 + 参数），客户端负责本地化渲染（§19.16），
 * 因此同一份报告在 zh_cn / en_us 客户端各自显示对应语言；客户端不做任何化学计算。</p>
 */
public class AnalyzerReportPacket {

    private final String containerId;
    private final List<ReportLine> summary;
    private final List<ReportLine> contents;
    private final List<ReportLine> failures;
    private final List<ReportLine> executed;
    private final List<ReportLine> executable;
    private final List<ReportLine> blocked;

    public AnalyzerReportPacket(String containerId,
                                List<ReportLine> summary, List<ReportLine> contents,
                                List<ReportLine> failures, List<ReportLine> executed,
                                List<ReportLine> executable, List<ReportLine> blocked) {
        this.containerId = containerId;
        this.summary = summary;
        this.contents = contents;
        this.failures = failures;
        this.executed = executed;
        this.executable = executable;
        this.blocked = blocked;
    }

    public static void encode(AnalyzerReportPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.containerId);
        writeLines(buf, msg.summary);
        writeLines(buf, msg.contents);
        writeLines(buf, msg.failures);
        writeLines(buf, msg.executed);
        writeLines(buf, msg.executable);
        writeLines(buf, msg.blocked);
    }

    public static AnalyzerReportPacket decode(FriendlyByteBuf buf) {
        String id = buf.readUtf();
        return new AnalyzerReportPacket(id,
                readLines(buf), readLines(buf), readLines(buf),
                readLines(buf), readLines(buf), readLines(buf));
    }

    private static void writeLines(FriendlyByteBuf buf, List<ReportLine> lines) {
        buf.writeVarInt(lines.size());
        for (ReportLine line : lines) {
            buf.writeUtf(line.key());
            buf.writeVarInt(line.args().size());
            for (ReportLine.ReportArg arg : line.args()) {
                buf.writeBoolean(arg.translatable());
                buf.writeUtf(arg.value());
            }
        }
    }

    private static List<ReportLine> readLines(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<ReportLine> lines = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            String key = buf.readUtf();
            int argc = buf.readVarInt();
            List<ReportLine.ReportArg> args = new ArrayList<>(argc);
            for (int j = 0; j < argc; j++) {
                boolean translatable = buf.readBoolean();
                String value = buf.readUtf();
                args.add(new ReportLine.ReportArg(value, translatable));
            }
            lines.add(ReportLine.raw(key, args));
        }
        return lines;
    }

    public static void handle(AnalyzerReportPacket msg, Supplier<NetworkEvent.Context> ctx) {
        // 客户端代码集中在 client-only 处理器里：服务器端永不加载本 lambda 的目标类（§19.21）
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandlers.openAnalyzer(msg)));
        ctx.get().setPacketHandled(true);
    }

    // ==================== 客户端读取 ====================

    public String containerId() { return containerId; }

    public List<ReportLine> summary() { return summary; }

    public List<ReportLine> contents() { return contents; }

    public List<ReportLine> failures() { return failures; }

    public List<ReportLine> executed() { return executed; }

    public List<ReportLine> executable() { return executable; }

    public List<ReportLine> blocked() { return blocked; }
}
