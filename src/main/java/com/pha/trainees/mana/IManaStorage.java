package com.pha.trainees.mana;

/**
 * 魔力存储契约（蓝本 §17.1：独立 {@code IManaStorage}，**形状对齐 {@code IEnergyStorage}**）。
 *
 * <p>刻意与 FE 的接口逐条对应，于是所有"能量类"代码（调试命令、能力转发、未来的管道/缓存单元）
 * 都能照抄同一套写法，脑子不用切换模式：</p>
 * <table>
 *   <tr><th>魔力</th><th>电力</th></tr>
 *   <tr><td>{@link #receiveMana(int, boolean)}</td><td>receiveEnergy</td></tr>
 *   <tr><td>{@link #extractMana(int, boolean)}</td><td>extractEnergy</td></tr>
 *   <tr><td>{@link #getManaStored()}</td><td>getEnergyStored</td></tr>
 *   <tr><td>{@link #getMaxManaStored()}</td><td>getMaxEnergyStored</td></tr>
 *   <tr><td>{@link #canExtract()} / {@link #canReceive()}</td><td>同名</td></tr>
 * </table>
 *
 * <p>单位就是"魔力"（Mana），整数计。**数量级尚未定稿**：与作者另开对话专门讨论，
 * 目前所有数值走配置占位（§17.1 补充）。</p>
 */
public interface IManaStorage {

    /**
     * 注入魔力。
     *
     * @param maxReceive 最多接收多少
     * @param simulate   true = 只试算不改动
     * @return 实际（或试算下会）接收的数量
     */
    int receiveMana(int maxReceive, boolean simulate);

    /**
     * 抽取魔力。
     *
     * @param maxExtract 最多抽取多少
     * @param simulate   true = 只试算不改动
     * @return 实际（或试算下会）抽取的数量
     */
    int extractMana(int maxExtract, boolean simulate);

    /** 当前存量。 */
    int getManaStored();

    /** 容量上限。 */
    int getMaxManaStored();

    boolean canExtract();

    boolean canReceive();
}
