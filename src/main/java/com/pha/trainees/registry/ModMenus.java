package com.pha.trainees.registry;

import com.pha.trainees.Main;
import com.pha.trainees.menu.MachineMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

/**
 * 魔法机器的菜单类型注册（§17.1 魔法机器 GUI 化）。
 *
 * <p>三台机器共用一个 {@link MachineMenu}，靠 {@link MachineMenu.Kind} 区分——
 * 所以这里注册三个 MenuType（客户端才知道该开哪个屏幕），但**代码只有一份**。</p>
 *
 * <p>用 {@code IForgeMenuType.create(...)}：菜单需要额外数据（方块坐标 + 机器种类），
 * 走 Forge 的"带数据菜单"通道，不用自己写开屏包。</p>
 */
public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, Main.MODID);

    private static final Map<MachineMenu.Kind, RegistryObject<MenuType<MachineMenu>>> BY_KIND =
            new EnumMap<>(MachineMenu.Kind.class);

    public static final RegistryObject<MenuType<MachineMenu>> EXTRACTOR =
            register(MachineMenu.Kind.EXTRACTOR, "extractor");
    public static final RegistryObject<MenuType<MachineMenu>> AGGREGATOR =
            register(MachineMenu.Kind.AGGREGATOR, "aggregator");
    public static final RegistryObject<MenuType<MachineMenu>> ENRICHMENT =
            register(MachineMenu.Kind.ENRICHMENT, "enrichment");

    /** 精练精华网端口（§17.1 ④）：显示能量与网络信息。 */
    public static final RegistryObject<MenuType<MachineMenu>> ESSENCE_PORT =
            register(MachineMenu.Kind.PORT, "essence_port");

    private ModMenus() {
    }

    private static RegistryObject<MenuType<MachineMenu>> register(MachineMenu.Kind kind, String name) {
        RegistryObject<MenuType<MachineMenu>> object = MENUS.register(name,
                // 注意：客户端只从额外数据里读**坐标**；机器种类由"哪个 MenuType"决定（闭包捕获），
                // 不再往包里塞种类——否则客户端多读一次就错位/崩。
                () -> IForgeMenuType.create((windowId, inventory, data) -> {
                    net.minecraft.core.BlockPos pos = data.readBlockPos();
                    return new MachineMenu(windowId, inventory, pos, kind);
                }));
        BY_KIND.put(kind, object);
        return object;
    }

    /** 按机器种类取菜单类型（{@link MachineMenu} 构造时用）。 */
    public static RegistryObject<MenuType<MachineMenu>> menuType(MachineMenu.Kind kind) {
        return BY_KIND.get(kind);
    }
}
