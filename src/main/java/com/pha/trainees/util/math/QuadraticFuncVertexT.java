package com.pha.trainees.util.math;

public record QuadraticFuncVertexT(double a, double h, double k) {
    public QuadraticFuncVertexT {
        if (a == 0) {
            throw new IllegalArgumentException(" 'a' can't be 0 ");
        }
    }

    public Pair getX(double y) {
        double rightSide = (y - k) / a;

        if (rightSide < 0) {
            return null;
        }

        double sqrtRightSide = Math.sqrt(rightSide);
        return new Pair(h + sqrtRightSide, h - sqrtRightSide);
    }

    public double getY(double x) {
        return a * (x - h) * (x - h) + k;
    }

    public QuadraticFuncGeneralT toGeneralType() {
        double b = -2 * a * h;
        double c = a * h * h + k;
        return new QuadraticFuncGeneralT(a, b, c);
    }
}
