package com.pha.trainees.util.math;

public record LogarithmicFunc(double a) {

    public double getX(double y) {
        return Math.pow(a, y);
    }

    public double getY(double x) {
        return MathT.log(x, a);
    }
}
