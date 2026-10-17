package com.pha.trainees.chemistry.util;

import java.util.Map;
import java.util.Set;

/**
 * 离子 ID → 化学式显示名格式化器。
 * 内部 ID 遵循规范（用 plus/minus 表示价态、小写拼接），显示层转为正常化学式：
 * - 原子数下标：h2o → H₂O、so4 → SO₄
 * - 价态上标：h_plus → H⁺、fe_2 → Fe²⁺、so4_2minus → SO₄²⁻
 * - 物态后缀：_molten → (熔)、_gas → (g)
 */
public final class IonDisplay {

    private static final char[] SUB = {'₀', '₁', '₂', '₃', '₄', '₅', '₆', '₇', '₈', '₉'};
    private static final char[] SUP = {'⁰', '¹', '²', '³', '⁴', '⁵', '⁶', '⁷', '⁸', '⁹'};
    private static final char SUP_PLUS = '⁺';
    private static final char SUP_MINUS = '⁻';

    // 1 字母元素符号
    private static final Set<String> ONE_LETTER = Set.of(
            "h", "b", "c", "n", "o", "f", "p", "s", "k", "v", "y", "i", "w", "u");

    // 2 字母元素符号（本模组用到的 + 常见未来元素）。
    // 注意：刻意排除 "co"（钴），否则 co2/co3 会被误解析为 Co₂/Co₃（本模组中 co 恒为 碳+氧）。
    private static final Set<String> TWO_LETTER = Set.of(
            "he", "li", "be", "ne", "na", "mg", "al", "si", "cl", "ar", "ca", "sc", "ti",
            "cr", "mn", "fe", "ni", "cu", "zn", "ga", "ge", "as", "se", "br", "kr",
            "rb", "sr", "zr", "nb", "mo", "tc", "ru", "rh", "pd", "ag", "cd", "in",
            "sn", "sb", "te", "xe", "cs", "ba", "la", "ce", "hf", "ta", "re", "os",
            "ir", "pt", "au", "hg", "tl", "pb", "bi", "po", "at", "rn", "fr", "ra",
            "ac", "th", "pa", "np", "pu");

    // 非公式形式的整词名（如元素全称）→ 元素符号
    private static final Map<String, String> NAME_TO_SYMBOL = Map.of(
            "sulfur", "S"
    );

    // 完整特殊显示名（含物态后缀、无法由通用解析得到的化学式）
    private static final Map<String, String> EXTRA_FORMATS = Map.of(
            "hcl_aq", "HCl(aq)"
    );

    private IonDisplay() {}

    public static String format(String id) {
        if (id == null || id.isEmpty()) return id;

        // 特殊显示名（如 hcl_aq → HCl(aq)）
        if (EXTRA_FORMATS.containsKey(id)) return EXTRA_FORMATS.get(id);

        // 整词名特殊处理（如 sulfur）
        if (NAME_TO_SYMBOL.containsKey(id)) return NAME_TO_SYMBOL.get(id);

        String base = id;
        String charge = "";
        String phase = "";

        int idx = id.lastIndexOf('_');
        if (idx >= 0) {
            String suffix = id.substring(idx + 1);
            base = id.substring(0, idx);
            if (suffix.equals("plus")) {
                charge = String.valueOf(SUP_PLUS);
            } else if (suffix.equals("minus")) {
                charge = String.valueOf(SUP_MINUS);
            } else if (suffix.matches("\\d+")) {
                charge = superscript(suffix, "1") + SUP_PLUS;
            } else if (suffix.matches("\\d+minus")) {
                String num = suffix.substring(0, suffix.length() - "minus".length());
                charge = superscript(num, "1") + SUP_MINUS;
            } else if (suffix.equals("molten")) {
                phase = "(熔)";
            } else if (suffix.equals("gas")) {
                phase = "(g)";
            } else {
                // 未识别后缀：视为 id 的一部分，不拆
                base = id;
            }
        }

        return formatFormula(base) + charge + phase;
    }

    /**
     * 把下标数字转上标，若数字为 1 则省略（标准化学式写法：Na⁺ 而非 Na¹⁺）
     */
    private static String superscript(String digits, String omitIfEquals) {
        if (omitIfEquals != null && digits.equals(omitIfEquals)) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : digits.toCharArray()) {
            sb.append(SUP[c - '0']);
        }
        return sb.toString();
    }

    /**
     * 解析基础公式：贪婪匹配 2 字母元素（本模组集合），再 1 字母；数字作为下标。
     */
    private static String formatFormula(String base) {
        if (base.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < base.length()) {
            // 尝试 2 字母元素
            if (i + 1 < base.length()) {
                String two = base.substring(i, i + 2);
                if (TWO_LETTER.contains(two)) {
                    sb.append(Character.toUpperCase(two.charAt(0))).append(two.charAt(1));
                    i += 2;
                } else {
                    String one = base.substring(i, i + 1);
                    sb.append(Character.toUpperCase(one.charAt(0)));
                    i += 1;
                }
            } else {
                String one = base.substring(i, i + 1);
                sb.append(Character.toUpperCase(one.charAt(0)));
                i += 1;
            }
            // 数字 → 下标
            int start = i;
            while (i < base.length() && Character.isDigit(base.charAt(i))) {
                i++;
            }
            if (i > start) {
                for (char c : base.substring(start, i).toCharArray()) {
                    sb.append(SUB[c - '0']);
                }
            }
        }
        return sb.toString();
    }
}
