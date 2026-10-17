package com.pha.trainees.registry;

import com.pha.trainees.Main;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * 运行时可见的物品标签键（§19.37）：数据生成器与使用方共用同一份定义。
 *
 * <p>标签文件本身仍由 {@code ModItemTagsProvider} 生成（不手写 JSON），
 * 这里只放"键"——否则 JEI 那边只能自己拼字符串，早晚拼错。</p>
 */
public final class ModItemTags {

    private ModItemTags() {}

    /** 全部消毒液：合格品、优质品与四类危险次品。JEI 图鉴与打包机规格页都读它。 */
    public static final TagKey<Item> BLEACH = traineeItem("bleach");

    private static TagKey<Item> traineeItem(String path) {
        return TagKey.create(Registries.ITEM, new ResourceLocation(Main.MODID, path));
    }
}
