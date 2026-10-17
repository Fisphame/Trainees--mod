package com.pha.trainees.chemistry.heat;

/**
 * 设定点型热源（§19.24 热源分级）。
 *
 * <p><b>与"燃料型热源"的区别</b>：火/岩浆/营火只能**单向把容器烤热**，且温度由燃料天然决定
 * （语义是"最高能给到多热"）；而恒温浴、加热套、低温槽、冷冻混合物这类设备，语义是
 * **设定点（setpoint）**——它同时具备加热与吸热能力，容器会稳定在设定点附近，
 * 而不是一路冲向热源自身的温度。现实实验室的精确控温（水浴/油浴/沙浴/加热套/恒温循环槽）
 * 全部属于后者。</p>
 *
 * <p>实现方应为方块实体（BlockEntity）。烧杯每 tick 检查**正下方**的方块是否实现本接口：
 * 若实现且处于工作状态，则按设定点做双向换热；否则回落到燃料型热源表。
 * 此外，贴在容器**邻面上**的"附着件形态热源"（§19.29 电加热夹套）也会从侧面供热——
 * 前提是它的方块实现了 {@code IContainerAttachment}，所以旁边的恒温浴不会突然开始加热。</p>
 */
public interface IHeatSource {

    /** 目标温度（K）。容器会向它趋近：高于容器温度则加热，低于则吸热冷却。 */
    double getSourceTemperature();

    /** 是否处于工作状态（关机、缺少介质时应返回 false，容器按"无热源"处理）。 */
    default boolean isHeatSourceActive() {
        return true;
    }

    /** 目标温度是否可被玩家/命令设定（恒温设备为 true，被动热源为 false）。 */
    default boolean isSettable() {
        return false;
    }

    /**
     * 设定目标温度。
     *
     * @return 是否成功设定；不可设定的实现直接返回 false
     */
    default boolean setSourceTemperature(double kelvin) {
        return false;
    }

    /**
     * 与容器换热时，该热源最多能以多大功率搬运热量（W）。
     *
     * <p>默认无上限（火/岩浆/创造热源这类"要多少给多少"的源）。设定点型设备应给出自己的额定值：
     * 恒温浴靠加热器维持温度，接口传热不能比它能搬走的热量更猛——否则容器会把浴槽当"无限大热库"（§19.38 十一）。</p>
     */
    default double maxTransferWatts() {
        return Double.POSITIVE_INFINITY;
    }

    /**
     * 为该次供热支付外部能量（§19.29 电加热夹套）。
     *
     * <p>容器在<b>真正把热量加进去之前</b>调用一次。燃料型与创造型不记账，默认返回 true；
     * 电加热设备的实现应在此扣费并返回是否付得起——<b>付不起就本 tick 不供热</b>，
     * 于是"电不够 = 不热"是自然结果而不是特判。计费口径是<b>按交付热量</b>
     * （1 FE = 1 J，交付 1 J 热量付 1/η J 电），所以热量需求越大越费电——大容器自然更贵。</p>
     *
     * <p>有介质的设备（恒温浴）在这里做的是另一件事：**把热量收进/取出自己的介质**，
     * 于是"红热容器把水浴烧开、甚至烧干"成为物理结果（§19.38 十一）。</p>
     *
     * @param joules 本次要交付给容器的热量（J）；负值 = 从容器吸热（冷却不计费）
     * @return true = 可以供热（如需计费则已扣费）；false = 供不起/接不住，本 tick 放弃供热
     */
    default boolean payHeatCost(double joules) {
        return true;
    }
}
