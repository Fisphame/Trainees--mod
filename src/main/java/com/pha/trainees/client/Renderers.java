package com.pha.trainees.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pha.trainees.Main;
import com.pha.trainees.entity.*;
import net.minecraft.client.model.ChickenModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings({"deprecation", "removal"})
public class Renderers {
    public static class CalledSwordRenderer extends EntityRenderer<CalledSwordEntity> {
        public CalledSwordRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public ResourceLocation getTextureLocation(CalledSwordEntity entity) {
            return new ResourceLocation(Main.MODID, "textures/entity/called_sword.png");
        }
    }

    public static class PatrticleEntityRenderer extends EntityRenderer<ParticleEntity> {
        public PatrticleEntityRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public ResourceLocation getTextureLocation(ParticleEntity entity) {
            return new ResourceLocation(Main.MODID, "textures/entity/particle_entity.png");
        }
    }

    public static class KunTraineesRenderer extends MobRenderer<KunTraineesEntity, ChickenModel<KunTraineesEntity>> {
        public KunTraineesRenderer(EntityRendererProvider.Context context) {
            super(context, new ChickenModel<>(context.bakeLayer(ModelLayers.CHICKEN)), 0.3F);
        }

        @Override
        public ResourceLocation getTextureLocation(KunTraineesEntity entity) {
            return new ResourceLocation(Main.MODID, "textures/entity/kun_trainees.png");
        }
    }

    public static class KunAntiRenderer extends MobRenderer<KunAntiEntity, ChickenModel<KunAntiEntity>> {
        public KunAntiRenderer(EntityRendererProvider.Context context) {
            super(context, new ChickenModel<>(context.bakeLayer(ModelLayers.CHICKEN)), 0.3F);
        }

        @Override
        public ResourceLocation getTextureLocation(KunAntiEntity entity) {
            return new ResourceLocation(Main.MODID, "textures/entity/kun_anti.png");
        }
    }

    public static class GoldChickenRenderer extends MobRenderer<GoldChickenEntity, ChickenModel<GoldChickenEntity>> {
        public GoldChickenRenderer(EntityRendererProvider.Context context) {
            super(context, new ChickenModel<>(context.bakeLayer(ModelLayers.CHICKEN)), 0.3F);
        }

        @Override
        public ResourceLocation getTextureLocation(GoldChickenEntity entity) {
            return new ResourceLocation(Main.MODID, "textures/entity/gold_chicken.png");
        }
    }

    public static class HydrogenRenderer extends EntityRenderer<GasEntities.HydrogenEntity> {
        public HydrogenRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public ResourceLocation getTextureLocation(GasEntities.HydrogenEntity entity) {
            return new ResourceLocation(Main.MODID, "textures/entity/hydrogen.png");
        }
    }

    public static class OxygenRenderer extends EntityRenderer<GasEntities.OxygenEntity> {
        public OxygenRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public ResourceLocation getTextureLocation(GasEntities.OxygenEntity entity) {
            return new ResourceLocation(Main.MODID, "textures/entity/oxygen.png");
        }
    }

    public static class KunAntiBossRenderer extends MobRenderer<KunAntiBossEntity, ChickenModel<KunAntiBossEntity>> {

        public KunAntiBossRenderer(EntityRendererProvider.Context context) {
            super(context, new ChickenModel<>(context.bakeLayer(ModelLayers.CHICKEN)), 0.8F);
        }

        @Override
        protected void scale(KunAntiBossEntity entity, PoseStack poseStack, float partialTick) {
            // 将模型放大 2 倍
            poseStack.scale(2.0F, 2.0F, 2.0F);
        }

        @Override
        public ResourceLocation getTextureLocation(KunAntiBossEntity entity) {
            return new ResourceLocation(Main.MODID, "textures/entity/kun_anti_boss.png");
        }
    }
}
