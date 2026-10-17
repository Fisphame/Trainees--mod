package com.pha.trainees.chemistry.container;

import com.pha.trainees.chemistry.particle.IonTags;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 侵蚀类型（§19.32 腐蚀层）：由物种身上的 {@link IonTags} 推出，<b>不新增任何化学数据</b>。
 *
 * <p>一个物种可以同时是多种侵蚀源（浓硫酸 = {@link #ACID} + {@link #OXIDIZER}），
 * 所以是"取全部匹配"而不是"取第一个"。</p>
 */
public enum ErosionType {

    /** 酸蚀（可提供 H⁺） */
    ACID(IonTags.ACID),
    /** 碱蚀（可提供 OH⁻ 或接受 H⁺）——热碱腐蚀玻璃正是靠这一条 */
    BASE(IonTags.BASE),
    /** 卤素阴离子点蚀（Cl⁻ 对不锈钢：氯碱工业的经典难题） */
    HALIDE(IonTags.HALIDE),
    /** 氧化性侵蚀 */
    OXIDIZER(IonTags.OXIDIZER),
    /** 熔融盐侵蚀 */
    MOLTEN_SALT(IonTags.MOLTEN_SALT);

    private final ResourceLocation tag;

    ErosionType(ResourceLocation tag) {
        this.tag = tag;
    }

    public ResourceLocation tag() {
        return tag;
    }

    /** 该物种身上全部的侵蚀类型（有序，与枚举声明序一致） */
    public static List<ErosionType> of(Set<ResourceLocation> tags) {
        if (tags == null || tags.isEmpty()) return List.of();
        List<ErosionType> found = new ArrayList<>(2);
        for (ErosionType type : values()) {
            if (tags.contains(type.tag)) {
                found.add(type);
            }
        }
        return found;
    }
}
