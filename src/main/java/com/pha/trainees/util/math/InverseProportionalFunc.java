package com.pha.trainees.util.math;

public record InverseProportionalFunc(double k) {
    public double getX(double y){
        return k / y;
    }

    public double getY(double x){
        return k / x;
    }
}
