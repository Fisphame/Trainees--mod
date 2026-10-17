package com.pha.trainees.event;


import com.pha.trainees.Main;
import com.pha.trainees.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings({"deprecation", "removal"})
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LootTableEventHandler {

    // 定义要修改的战利品表列表
    // 所有村庄

    private static final List<ResourceLocation> CHEST_LOOT_TABLES_VILLAGE = Arrays.asList(
            new ResourceLocation("minecraft:chests/village/village_desert_house"),
            new ResourceLocation("minecraft:chests/village/village_plains_house"),
            new ResourceLocation("minecraft:chests/village/village_savanna_house"),
            new ResourceLocation("minecraft:chests/village/village_snowy_house"),
            new ResourceLocation("minecraft:chests/village/village_taiga_house"),
            new ResourceLocation("minecraft:chests/village/village_tannery"),
            new ResourceLocation("minecraft:chests/village/village_temple"),
            new ResourceLocation("minecraft:chests/village/village_toolsmith"),
            new ResourceLocation("minecraft:chests/village/village_weaponsmith"),

            new ResourceLocation("minecraft:chests/simple_dungeon"),
            new ResourceLocation("minecraft:chests/abandoned_mineshaft"),
            new ResourceLocation("minecraft:chests/stronghold_corridor"),
            new ResourceLocation("minecraft:chests/stronghold_crossing"),
            new ResourceLocation("minecraft:chests/stronghold_library"),
            new ResourceLocation("minecraft:chests/woodland_mansion"),
            new ResourceLocation("minecraft:chests/buried_treasure"),
            new ResourceLocation("minecraft:chests/shipwreck_treasure"),
            new ResourceLocation("minecraft:chests/jungle_temple"),
            new ResourceLocation("minecraft:chests/desert_pyramid")
    );

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        // 检查是否是我们要修改的战利品表
        if (CHEST_LOOT_TABLES_VILLAGE.contains(event.getName())) {
            // 创建一个新的奖励池，设置随机几率为15%
            LootPool pool = LootPool.lootPool()
                    .name("trainees_duihuanquan_pool")
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(ModItems.duihuanquan.get())
                            .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1)))
                    )
                    .when(LootItemRandomChanceCondition.randomChance(0.15F)) // 15%的几率
                    .build();

            // 将奖励池添加到战利品表中
            event.getTable().addPool(pool);
        }
    }
}

