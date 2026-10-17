package com.pha.trainees.util.game;

import com.pha.trainees.item.AuriversiteRapierItem;
import com.pha.trainees.item.KunCourseItem;
import com.pha.trainees.item.ScytheCourseItem;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ItemClassifier {
    public static Boolean scythe(Item item){
        return item instanceof ScytheCourseItem.ScytheItem || item instanceof ScytheCourseItem.CompoundScytheItem;
    }
    public static Boolean repair(Item item){
        return item instanceof AuriversiteRapierItem;
    }
    public static Boolean kunSword(Item item){
        return item instanceof KunCourseItem.KunSwordItem;
    }
    public static Boolean tag(Item item, TagKey<Item> tag) {
        return item.getDefaultInstance().is(tag);
    }

}
