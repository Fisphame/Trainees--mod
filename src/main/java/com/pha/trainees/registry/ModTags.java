package com.pha.trainees.registry;

import com.pha.trainees.Main;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

@SuppressWarnings({"deprecation", "removal"})
public class ModTags {

    public static final TagKey<Item> POWDER_ANTI_2 = create("powder_anti_2");
    public static final TagKey<Item> KUN_ITEMS = create("kun_items");
    public static final TagKey<Item> CHEMISTRY_ITEMS = create("chemistry_items");

    private static TagKey<Item> create(String name) {
        return TagKey.create(Registries.ITEM, new ResourceLocation(Main.MODID, name));
    }
}
