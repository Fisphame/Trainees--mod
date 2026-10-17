package com.pha.trainees.chemistry.container;

/**
 * "我这块方块是什么材料做的"（§19.30.1）。
 *
 * <p><b>为什么需要它</b>：{@code BeakerBlockEntity} 原先靠 {@code block instanceof BeakerBlock} 解析材料，
 * 不是 BeakerBlock 的容器就回落 GLASS（773 K）。电解槽正是这种方块
 * （{@code ElectrolysisCellBlock extends BaseEntityBlock}），于是**它被钉在玻璃档**——
 * NaCl 熔点 1074 K &gt; 773 K，**熔盐电解实际上做不了**。
 * 这个接口把"材料从哪来"从"继承关系"改成"声明关系"，任何容器方块都能自报材料。</p>
 */
public interface IContainerMaterialProvider {

    /** 该方块对应的容器材料档位 */
    ContainerMaterial getContainerMaterial();
}
