package com.pha.trainees.chemistry.particle;

public enum Phase {
    GAS,        // 气体（参与压强计算，遵守理想气体方程）
    LIQUID,     // 纯液体（如水、苯，浓度视为常数或由质量决定）
    AQUEOUS,    // 水溶液中的离子（活度与浓度挂钩，但不受总压影响）
    SOLID;      // 固体（活度视为1，参与热容但不参与速率方程浓度乘积）
}
