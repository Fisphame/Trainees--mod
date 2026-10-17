package com.pha.trainees.client;

import com.pha.trainees.Main;
import com.pha.trainees.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class RenderersRegisterEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CALLED_SWORD.get(), Renderers.CalledSwordRenderer::new);
        event.registerEntityRenderer(ModEntities.PARTICLE_ENTITY.get(), Renderers.PatrticleEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.KUN_TRAINEES.get(), Renderers.KunTraineesRenderer::new);
        event.registerEntityRenderer(ModEntities.KUN_ANTI.get(), Renderers.KunAntiRenderer::new);
        event.registerEntityRenderer(ModEntities.GOLD_CHICKEN.get(), Renderers.GoldChickenRenderer::new);
        event.registerEntityRenderer(ModEntities.KUN_ANTI_BOSS.get(), Renderers.KunAntiBossRenderer::new);
        event.registerEntityRenderer(ModEntities.HYDROGEN.get(), Renderers.HydrogenRenderer::new);
        event.registerEntityRenderer(ModEntities.OXYGEN.get(), Renderers.OxygenRenderer::new);
    }
}