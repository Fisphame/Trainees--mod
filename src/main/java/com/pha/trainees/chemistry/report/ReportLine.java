package com.pha.trainees.chemistry.report;

import java.util.ArrayList;
import java.util.List;

/**
 * 分析仪报告的一行**结构化文本**（§19.16 语言键一并处理）：
 * 服务端只发"翻译键 + 参数"，由客户端本地化渲染，避免服务端拼中文串下发。
 *
 * <p>参数分两类：</p>
 * <ul>
 *   <li>{@link ReportArg#of} 普通字面量（化学式、数字、位置串等，与语言无关）；</li>
 *   <li>{@link ReportArg#key} 嵌套翻译键（如形态名、失败类型名），由客户端再翻译一次。</li>
 * </ul>
 *
 * <p>注意：格式（颜色码 §x、缩进、加粗）写在**语言文件的值**里；方程式等需要加粗的
 * 字面量可由服务端预先包好 §l…§r 后作为普通字面量传入。</p>
 */
public record ReportLine(String key, List<ReportArg> args) {

    public ReportLine {
        args = args == null ? List.of() : List.copyOf(args);
    }

    public static ReportLine of(String key, String... args) {
        List<ReportArg> list = new ArrayList<>(args.length);
        for (String a : args) list.add(ReportArg.of(a));
        return new ReportLine(key, list);
    }

    public static ReportLine raw(String key, List<ReportArg> args) {
        return new ReportLine(key, args);
    }

    /** 单个参数：字面量 or 嵌套翻译键 */
    public record ReportArg(String value, boolean translatable) {

        public static ReportArg of(String value) {
            return new ReportArg(value == null ? "" : value, false);
        }

        public static ReportArg key(String translationKey) {
            return new ReportArg(translationKey, true);
        }
    }
}
