package com.pha.trainees.client;

import com.pha.trainees.Main;
import com.pha.trainees.client.gui.ChemConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEvents2 {

    // 图片路径：assets/trainees/textures/gui/config_button.png
    private static final ResourceLocation CONFIG_BUTTON_TEXTURE =
            new ResourceLocation(Main.MODID, "textures/gui/config_button.png");
    // 按钮尺寸（与纹理图片像素尺寸一致，建议 20x20）
    private static final int BUTTON_SIZE = 20;

    @SubscribeEvent
    public static void onGuiInit(ScreenEvent.Init event) {

        if (!(event.getScreen() instanceof PauseScreen screen)) {
            return;
        }

        int x = screen.width / 2 + 100;
        int y = screen.height / 4 + 48 + 72;

        event.addListener(new ImageButton(
                x, y,
                BUTTON_SIZE, BUTTON_SIZE,
                0, 0,  // 纹理偏移（若有多状态纹理，可扩展）
                BUTTON_SIZE,  // 纹理高度（Y偏移）
                CONFIG_BUTTON_TEXTURE,
                BUTTON_SIZE,  // 纹理宽度
                BUTTON_SIZE,  // 纹理高度
                btn -> {
                    Minecraft.getInstance().setScreen(
                            new ChemConfigScreen(screen)
                    );
                }
        ));

    }

}
