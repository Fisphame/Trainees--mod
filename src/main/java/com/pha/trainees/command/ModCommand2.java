package com.pha.trainees.command;

import com.mojang.brigadier.CommandDispatcher;
import com.pha.trainees.Main;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Main.MODID)
public class ModCommand2 {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        DebugCommand.register(dispatcher);
        // 魔法线自己的命令树（§17.1）：精炼精华不是化学内容，别塞进 /chemtester
        EssenceCommand.register(dispatcher);

        Main.LOGGER.info("Commands registered successfully.");
    }

}
