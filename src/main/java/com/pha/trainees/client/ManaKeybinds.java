package com.pha.trainees.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.pha.trainees.Main;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * 玩家魔力按键（§17.1）：**R** 往视线里的方块注能，**潜行 + R** 从它抽取。
 *
 * <p>默认键位改成别的也不影响逻辑：判定潜行是按键时刻读的，不是两个键位。</p>
 */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ManaKeybinds {

    public static final String CATEGORY = "key.categories.trainees";

    public static final KeyMapping USE_MANA = new KeyMapping(
            "key.trainees.use_mana",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY);

    private ManaKeybinds() {
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(USE_MANA);
    }
}
