package com.pha.trainees.registry;

import com.pha.trainees.Main;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 精练精华网（§17.1 ④）的物品注册：7 个结构方块 + 导线 + 无线绑定物品。
 *
 * <p>绑定物品（{@link #ESSENCE_BINDING_ITEM}）：**同维度无视距离**传输/抽取的钥匙，
 * 现在先注册成可堆叠 1 的普通物品，绑定逻辑跟无线总线一起做。</p>
 */
public final class ModEssenceItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Main.MODID);

    public static final RegistryObject<Item> ESSENCE_NETWORK_PORT_ITEM = ITEMS.register("essence_network_port",
            () -> new BlockItem(ModEssenceBlocks.ESSENCE_NETWORK_PORT.get(), new Item.Properties()));

    public static final RegistryObject<Item> ESSENCE_PORT_WALL_ITEM = ITEMS.register("essence_port_wall",
            () -> new BlockItem(ModEssenceBlocks.ESSENCE_PORT_WALL.get(), new Item.Properties()));

    public static final RegistryObject<Item> ESSENCE_EXPANSION_INTERFACE_ITEM = ITEMS.register(
            "essence_expansion_interface",
            () -> new BlockItem(ModEssenceBlocks.ESSENCE_EXPANSION_INTERFACE.get(), new Item.Properties()));

    public static final RegistryObject<Item> ESSENCE_INPUT_BUS_ITEM = ITEMS.register("essence_input_bus",
            () -> new BlockItem(ModEssenceBlocks.ESSENCE_INPUT_BUS.get(), new Item.Properties()));

    public static final RegistryObject<Item> ESSENCE_OUTPUT_BUS_ITEM = ITEMS.register("essence_output_bus",
            () -> new BlockItem(ModEssenceBlocks.ESSENCE_OUTPUT_BUS.get(), new Item.Properties()));

    public static final RegistryObject<Item> ESSENCE_WIRELESS_INPUT_BUS_ITEM = ITEMS.register(
            "essence_wireless_input_bus",
            () -> new BlockItem(ModEssenceBlocks.ESSENCE_WIRELESS_INPUT_BUS.get(), new Item.Properties()));

    public static final RegistryObject<Item> ESSENCE_WIRELESS_OUTPUT_BUS_ITEM = ITEMS.register(
            "essence_wireless_output_bus",
            () -> new BlockItem(ModEssenceBlocks.ESSENCE_WIRELESS_OUTPUT_BUS.get(), new Item.Properties()));

    public static final RegistryObject<Item> ESSENCE_CABLE_ITEM = ITEMS.register("essence_cable",
            () -> new BlockItem(ModEssenceBlocks.ESSENCE_CABLE.get(), new Item.Properties()));

    /** 无线绑定物品（同维度无视距离）。 */
    public static final RegistryObject<Item> ESSENCE_BINDING_ITEM = ITEMS.register("essence_binding_item",
            () -> new com.pha.trainees.block.EssenceBindingItem(new Item.Properties().stacksTo(1)));

    private ModEssenceItems() {
    }
}
