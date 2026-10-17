package com.pha.trainees.util.math;

public record Pair(double x1, double x2) {

    @Override
    public String toString() {
        return String.format("(%.2f, %.2f)", x1, x2);
    }
}
