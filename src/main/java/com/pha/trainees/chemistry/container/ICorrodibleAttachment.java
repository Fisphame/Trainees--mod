package com.pha.trainees.chemistry.container;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * 可被腐蚀的附着件（§19.32 的"本批未做"项：搅拌器轴 / 夹套的腐蚀）。
 *
 * <p><b>为什么不是 {@link IContainerAttachment} 上的方法</b>：那个接口是**方块**实现的，
 * 而方块是单例、按位置查表，**存不了每格状态**（腐蚀度必须跟着方块走，拆了就该没）。
 * 所以状态这一半单独放在**方块实体**上，容器扫描邻格时顺手问一下方块实体。</p>
 *
 * <p>约定：不实现本接口的附着件就**不会被腐蚀**（默认安全，不会因为漏实现而悄悄烂掉）。</p>
 */
public interface ICorrodibleAttachment {

    /**
     * 附着件自身的材质（决定它对各类侵蚀的敏感度）。
     *
     * <p>默认**耐热钢**：搅拌轴、电加热夹套都是金属件；搪玻璃夹套之类的变体覆盖它即可。</p>
     */
    default ContainerMaterial attachmentMaterial() {
        return ContainerMaterial.METAL;
    }

    /** 当前腐蚀度 0~1。 */
    double getAttachmentCorrosion();

    /**
     * 累加腐蚀度；实现里直接委托 {@link AttachmentCorrosion#apply} 即可
     * （它负责钳制、预警、以及"吃透即报废"的表现）。
     */
    void applyAttachmentCorrosion(Level level, BlockPos pos, double delta);
}
