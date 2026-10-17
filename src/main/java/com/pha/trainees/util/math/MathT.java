package com.pha.trainees.util.math;

public class MathT {
    // 有关数学的拓展方法

    //实际上应该是387420489F，电脑算力还是有限的（，但其实也受加载距离影响。
    public static final float MATH99 = 38742F;

    public static final int[] POW_9 = {
            1,        // 9^0
            9,        // 9^1
            81,       // 9^2
            729,      // 9^3
            6561,     // 9^4
            59049,    // 9^5
            531441,   // 9^6
            4782969,  // 9^7
            43046721, // 9^8
            387420489 // 9^9

    };

    public static double log(double value, double base) {
        return Math.log(value) / Math.log(base);
    }
    //    double result = MAth.log(8, 2); // log2(8) = 3

    // 计算最大公约数
    public static int gcd(int a, int b) {
        while (b != 0) {
            int temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }

    // 计算最小公倍数
    public static int lcm(int a, int b) {
        return Math.abs(a * b) / gcd(a, b);
    }

    // 计算数组的最小公倍数
    public static int lcmArray(int[] numbers) {
        int result = numbers[0];
        for (int i = 1; i < numbers.length; i++) {
            result = lcm(result, numbers[i]);
        }
        return result;
    }

    // 值是否位于区间
    public static boolean isInInterval(double a, double min, double max, boolean left, boolean right){
        if (!left && !right) {
            return a > min && a < max;
        }
        if (left && right) {
            return a >= min && a <= max;
        }
        if (left) {
            return a >= min && a < max;
        }
        return a > min && a <= max;
    }
    public static boolean isInInterval(double a, double min, double max){
        return isInInterval(a, min, max, true, true);
    }
    public static boolean isInInterval(double a, Pair pair){
        return isInInterval(a, pair.x1(), pair.x2(), true, true);
    }

    public static double inInterval(double x, double min, double max) {
        return Math.max(Math.min(x, max), min);
    }
    public static float inInterval(float x, float min, float max) {
        return Math.max(Math.min(x, max), min);
    }
    public static int inInterval(int x, int min, int max) {
        return Math.max(Math.min(x, max), min);
    }

}
