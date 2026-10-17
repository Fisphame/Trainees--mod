package com.pha.trainees.event;

import com.pha.trainees.Main;
import com.pha.trainees.entity.GoldChickenEntity;
import com.pha.trainees.entity.KunAntiBossEntity;
import com.pha.trainees.entity.KunAntiEntity;
import com.pha.trainees.entity.KunTraineesEntity;
import com.pha.trainees.loot.DuihuanquanLootModifier;
import com.pha.trainees.registry.ModEntities;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Register {
    @SubscribeEvent
    public static void entityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.KUN_TRAINEES.get(), KunTraineesEntity.createAttributes().build());
        event.put(ModEntities.KUN_ANTI.get(), KunAntiEntity.createAttributes().build());
        event.put(ModEntities.GOLD_CHICKEN.get(), GoldChickenEntity.createAttributes().build());
        event.put(ModEntities.KUN_ANTI_BOSS.get(), KunAntiBossEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerLootModifierCodecs(RegisterEvent event) {
        if (event.getRegistryKey().equals(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS)) {
            event.register(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
                    new ResourceLocation(Main.MODID, "duihuanquan"),
                    () -> DuihuanquanLootModifier.CODEC);
        }
    }
}
