package com.pha.trainees.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class ClientTools {
    @OnlyIn(Dist.CLIENT)
    public static boolean isShiftPressedClient() {
        // 使用 InputConstants
        Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        var window = minecraft.getWindow();

        // 检测左Shift或右Shift
        long windowHandle = window.getWindow();
        int leftShift = InputConstants.KEY_LSHIFT;
        int rightShift = InputConstants.KEY_RSHIFT;

        return InputConstants.isKeyDown(windowHandle, leftShift) ||
                InputConstants.isKeyDown(windowHandle, rightShift);

    }
}
