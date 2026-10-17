package com.pha.trainees.util.game;

import java.util.Map;

public class ChemistryTools {

    /**
     * 根据化学式计算摩尔质量，结果以 0.5 为单位四舍五入
     * @param formula 化学式字符串
     * @return 四舍五入到最近 0.5 的摩尔质量
     */
    public static double calculateMolarMassApproximation(String formula, Map<String, Double> elementMassMap) {
        double exactMass = calculateMolarMass(formula, elementMassMap);
        return Math.round(exactMass * 2) / 2.0;
    }

    /**
     * 计算化学式的摩尔质量，结果保留三位小数（未四舍五入）
     * @param formula 化学式字符串
     * @param elementMassMap 元素名->摩尔质量映射
     * @return 摩尔质量 (g/mol)
     */
    public static double calculateMolarMass(String formula, Map<String, Double> elementMassMap) {
        int[] idx = new int[]{0};
        double result = parseExpression(formula, idx, elementMassMap);
        if (idx[0] < formula.length()) {
            throw new IllegalArgumentException("Extra characters at end: " + formula.substring(idx[0]));
        }
        return result;
    }

    // expression ::= term ('.' term)*
    private static double parseExpression(String s, int[] idx, Map<String, Double> map) {
        double total = parseTerm(s, idx, map);
        while (idx[0] < s.length() && (s.charAt(idx[0]) == '·' || s.charAt(idx[0]) == '.')) {
            idx[0]++; // skip dot
            int coeff = 1;
            if (idx[0] < s.length() && isDigit(s.charAt(idx[0]))) {
                int start = idx[0];
                while (idx[0] < s.length() && isDigit(s.charAt(idx[0]))) idx[0]++;
                coeff = Integer.parseInt(s.substring(start, idx[0]));
            }
            total += coeff * parseTerm(s, idx, map);
        }
        return total;
    }

    // term ::= factor (factor)*
    private static double parseTerm(String s, int[] idx, Map<String, Double> map) {
        double total = parseFactor(s, idx, map);
        while (idx[0] < s.length() && (isUpperLetter(s.charAt(idx[0])) || isLeftBracket(s.charAt(idx[0])))) {
            total += parseFactor(s, idx, map);
        }
        return total;
    }

    // factor ::= element | '(' expression ')' | '[' expression ']' | '{' expression '}'
    // followed by optional subscript
    private static double parseFactor(String s, int[] idx, Map<String, Double> map) {
        double value;
        char c = s.charAt(idx[0]);
        if (isLeftBracket(c)) {
            char open = c;
            idx[0]++; // skip open bracket
            double inner = parseExpression(s, idx, map);
            char expectedClose;
            if (open == '(') expectedClose = ')';
            else if (open == '[') expectedClose = ']';
            else expectedClose = '}';
            if (idx[0] >= s.length() || s.charAt(idx[0]) != expectedClose) {
                throw new IllegalArgumentException("Missing closing bracket " + expectedClose + " at position " + idx[0]);
            }
            idx[0]++; // skip close bracket
            value = inner;
        } else if (isUpperLetter(c)) {
            int start = idx[0];
            idx[0]++; // first uppercase
            while (idx[0] < s.length() && isLowerLetter(s.charAt(idx[0]))) {
                idx[0]++;
            }
            String element = s.substring(start, idx[0]);
            Double mass = map.get(element);
            if (mass == null) {
                mass = 9999.0;
            }
            value = mass;
        } else {
            throw new IllegalArgumentException("Unexpected character: " + c + " at position " + idx[0]);
        }
        // parse subscript
        int subscript = parseSubscript(s, idx);
        return value * subscript;
    }

    private static int parseSubscript(String s, int[] idx) {
        if (idx[0] >= s.length()) return 1;
        int result = 0;
        boolean found = false;
        while (idx[0] < s.length()) {
            char c = s.charAt(idx[0]);
            if (isDigit(c)) {
                result = result * 10 + (c - '0');
                idx[0]++;
                found = true;
            } else if (isSubscript(c)) {
                result = result * 10 + (c - 0x2080);
                idx[0]++;
                found = true;
            } else {
                break;
            }
        }
        return found ? result : 1;
    }

    // Character checks
    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isSubscript(char c) {
        return c >= '\u2080' && c <= '\u2089';
    }

    private static boolean isLeftBracket(char c) {
        return c == '(' || c == '[' || c == '{';
    }

    private static boolean isRightBracket(char c) {
        return c == ')' || c == ']' || c == '}';
    }

    private static boolean isUpperLetter(char c) {
        return c >= 'A' && c <= 'Z';
    }

    private static boolean isLowerLetter(char c) {
        return c >= 'a' && c <= 'z';
    }
}
