package com.pha.trainees.chemistry.container;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * 容器附着件（§19.29）：贴在容器某一面上的"做功部件"，用<b>乘性因子</b>影响容器的传热与气液传质。
 *
 * <p>与 {@link IContainerPort} 是姊妹抽象：<b>端口</b>管"这一面通不通气"，<b>附着件</b>管"容器整体做多少功"。
 * 两者都由容器<b>扫 6 个邻格</b>发现（每 tick 缓存一次），所以新增部件的接线成本只有
 * "实现接口 + 注册"两件事。</p>
 *
 * <p><b>为什么是传热/传质，而不是反应速率</b>：我们的容器模型本来就是<b>理想混合</b>
 * （单一浓度 + 单一温度，没有空间维度），"混合充分"已经是前提假设——再给反应速率乘一个倍率
 * 是没有物理依据的假机制（也违反"不手填经验值"的纪律）。搅拌真实且可表达的作用只有两个：
 * 强化壁面传热（打破滞留层）与强化气液传质（表面更新），正好对应下面两个因子。</p>
 *
 * <p><b>约定</b>：不工作时（没电/停机）必须返回 1.0，即"等于没装"。</p>
 */
public interface IContainerAttachment {

    /** 传热系数倍率（乘性；1.0 = 不影响）——用于"改变传热系数 h"的部件（搅拌器）。 */
    default double heatTransferFactor(Level level, BlockPos pos) {
        return 1.0;
    }

    /**
     * 传热"面积/接触"加成（<b>加性</b>；0 = 不影响）。
     *
     * <p>用于"增加换热面积"的部件（夹套、内盘管）。<b>为什么必须与倍率分开</b>：
     * 夹套的物理作用是加大换热面积 A（多贴一面就多一块面积，线性叠加），
     * 而搅拌器改的是传热系数 h。若把夹套也做成乘性，贴四面就是 1.6⁴ = 6.6，
     * 而按面积叠加只有 1+0.6×4 = 3.4——后者才是对的。
     * 容器把它们合成为 {@code (1 + Σ加成) × Π倍率}。</p>
     */
    default double heatTransferAreaBonus(Level level, BlockPos pos) {
        return 0.0;
    }

    /** 气液传质（容器 ↔ 气体网格交换速率）倍率（乘性；1.0 = 不影响） */
    default double gasTransferFactor(Level level, BlockPos pos) {
        return 1.0;
    }
}
