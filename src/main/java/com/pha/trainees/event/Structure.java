//package com.pha.trainees.event;
//
//import net.minecraft.resources.ResourceLocation;
//import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
//import net.minecraftforge.eventbus.api.SubscribeEvent;
//import net.minecraftforge.registries.ForgeRegistries;
//import net.minecraftforge.registries.RegisterEvent;
//
//public class Structure {
//    // 在ModEventBus订阅者中添加
//    @SubscribeEvent
//    public static void registerFeatures(RegisterEvent event) {
//        event.register(ForgeRegistries.Keys.STRUCTURE_FEATURES, helper -> {
//            helper.register(new ResourceLocation("trainees", "chicken_2"),
//                    new JigsawStructure(STRUCTURE_CONFIG, true)); // 需要提供合适的STRUCTURE_CONFIG
//        });
//    }
//}
