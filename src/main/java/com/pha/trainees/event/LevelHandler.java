package com.pha.trainees.event;

import com.pha.trainees.Main;
import com.pha.trainees.util.game.structure.ActiveStructureManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Main.MODID)
public class LevelHandler {
    @SubscribeEvent
    public static void onWorldSave(LevelEvent.Save event) {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel serverLevel) {
            ActiveStructureManager manager = ActiveStructureManager.get(serverLevel);
            manager.setDirty();
        }
    }
}
