package com.pha.trainees.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pha.trainees.blockentity.KunAltarBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class Renderer {

    public static class KunAltarBlockEntityRenderer implements BlockEntityRenderer<KunAltarBlockEntity> {

        public KunAltarBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        }

        @Override
        public void render(KunAltarBlockEntity blockEntity, float partialTick,
                           PoseStack poseStack, MultiBufferSource bufferSource,
                           int packedLight, int packedOverlay) {

            ItemStack displayItem = blockEntity.getDisplayItem();
            if (displayItem.isEmpty()) return;

            ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
            BakedModel bakedModel = itemRenderer.getModel(displayItem, blockEntity.getLevel(), null, 0);

            poseStack.pushPose();

            // 将物品移到方块中心
            poseStack.translate(0.5, 0.5, 0.5);

            // 旋转物品
            poseStack.mulPose(Axis.YP.rotationDegrees((System.currentTimeMillis() / 20) % 360));

            // 缩放
            float scale = 0.5f;
            poseStack.scale(scale, scale, scale);

            // 渲染物品
            itemRenderer.render(displayItem, ItemDisplayContext.FIXED, false,
                    poseStack, bufferSource, packedLight, packedOverlay, bakedModel);

            poseStack.popPose();
        }
    }


}
