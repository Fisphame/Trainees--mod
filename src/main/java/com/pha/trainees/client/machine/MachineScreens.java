package com.pha.trainees.client.machine;

import com.pha.trainees.Main;
import com.pha.trainees.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 把三个机器菜单接到它们的屏幕（§17.1 魔法机器 GUI 化）。
 *
 * <p>三个 MenuType 共用同一个 {@link MachineScreen}——屏幕本身就是按 {@code MachineMenu.Kind}
 * 参数化的，所以这里只是三行注册，没有第二份屏幕代码。</p>
 *
 * <p>用 {@code FMLClientSetupEvent + MenuScreens.register}（客户端初始化时在游戏线程上注册）：
 * 这是最稳的口径，不依赖某个版本才有的专用事件。</p>
 */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MachineScreens {

    private MachineScreens() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.EXTRACTOR.get(), MachineScreen::new);
            MenuScreens.register(ModMenus.AGGREGATOR.get(), MachineScreen::new);
            MenuScreens.register(ModMenus.ENRICHMENT.get(), MachineScreen::new);
            MenuScreens.register(ModMenus.ESSENCE_PORT.get(), MachineScreen::new);
        });
    }
}
