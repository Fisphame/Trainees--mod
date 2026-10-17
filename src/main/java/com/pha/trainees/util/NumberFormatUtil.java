package com.pha.trainees.util;

import java.util.Locale;

/**
 * 大数字缩写（k / M / G / T）——§17.1 巨型输出总线要用它显示数量。
 *
 * <p>为什么自己写：**Forge 与原版都没有这个工具**（查过 `forge-1.20.1-47.4.10-universal.jar`：
 * `forge.common.util` 里只有 `TextTable` 这种表格工具；GT 的 `GTUtility.formatNumbers`、AE2 的文本工具
 * 都是各自实现的）。这里给一份统一口径，供 GUI / Jade / JEI / HUD 共用。</p>
 *
 * <p>规则：<1000 直接显示；否则按 3 位一档缩写，保留 1 位小数并去掉多余的 `.0`。</p>
 */
public final class NumberFormatUtil {

    private static final String[] SUFFIXES = {"", "k", "M", "G", "T", "P"};

    private NumberFormatUtil() {
    }

    /** 1234 → "1.2k"；999 → "999"；1234567 → "1.2M"；负数保留符号。 */
    public static String compact(long value) {
        if (value > -1000 && value < 1000) {
            return Long.toString(value);
        }
        boolean negative = value < 0;
        double amount = Math.abs((double) value);
        int tier = 0;
        while (amount >= 1000.0 && tier < SUFFIXES.length - 1) {
            amount /= 1000.0;
            tier++;
        }
        String text = String.format(Locale.ROOT, "%.1f", amount);
        if (text.endsWith(".0")) {
            text = text.substring(0, text.length() - 2);
        }
        return (negative ? "-" : "") + text + SUFFIXES[tier];
    }

    /** "1234 / 5678" 这种读数的便捷写法。 */
    public static String compactPair(long current, long max) {
        return compact(current) + " / " + compact(max);
    }
}
