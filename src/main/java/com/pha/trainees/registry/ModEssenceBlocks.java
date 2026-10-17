package com.pha.trainees.registry;

import com.pha.trainees.Main;
import com.pha.trainees.block.EssenceBusBlock;
import com.pha.trainees.block.EssenceCableBlock;
import com.pha.trainees.block.EssenceExpansionInterfaceBlock;
import com.pha.trainees.block.EssenceNetworkPortBlock;
import com.pha.trainees.blockentity.EssenceNetworkPortBlockEntity;
import com.pha.trainees.multiblock.EssencePortPattern;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 精练精华网（§17.1 ④）的方块注册。
 *
 * <p>单独一个注册类（不塞进 {@code ModBlocks}）：魔法线的东西自成一块，符合"魔法不混进化学"的准则，
 * 也避免改动化学线那两个巨大的注册文件。</p>
 *
 * <p>方块表（作者 2026-10-03 确认）：端口主方块 / 端口墙壁 / 网络扩展接口 /
 * （无线）输入输出总线 / 导线。前 7 个是**完整方块**，都会参与多方块结构校验（见 {@link EssencePortPattern}）。</p>
 */
public final class ModEssenceBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Main.MODID);

    /** 端口主方块：多方块的关键方块，也是整个网络的聚合点。 */
    public static final RegistryObject<Block> ESSENCE_NETWORK_PORT = BLOCKS.register("essence_network_port",
            () -> new EssenceNetworkPortBlock(machine(MapColor.COLOR_PURPLE)));

    /** 端口墙壁方块：结构中心的 1×1×4 柱子必须是它。 */
    public static final RegistryObject<Block> ESSENCE_PORT_WALL = BLOCKS.register("essence_port_wall",
            () -> new Block(machine(MapColor.COLOR_BLACK)));

    /** 网络扩展接口：从这里接导线出去（**最多一个**）。 */
    public static final RegistryObject<Block> ESSENCE_EXPANSION_INTERFACE = BLOCKS.register(
            "essence_expansion_interface", () -> new EssenceExpansionInterfaceBlock(machine(MapColor.COLOR_CYAN)));

    public static final RegistryObject<Block> ESSENCE_INPUT_BUS = BLOCKS.register("essence_input_bus",
            () -> new EssenceBusBlock(EssenceBusBlock.Mode.INPUT, false, machine(MapColor.COLOR_BLUE)));

    public static final RegistryObject<Block> ESSENCE_OUTPUT_BUS = BLOCKS.register("essence_output_bus",
            () -> new EssenceBusBlock(EssenceBusBlock.Mode.OUTPUT, false, machine(MapColor.COLOR_ORANGE)));

    public static final RegistryObject<Block> ESSENCE_WIRELESS_INPUT_BUS = BLOCKS.register(
            "essence_wireless_input_bus",
            () -> new EssenceBusBlock(EssenceBusBlock.Mode.INPUT, true, machine(MapColor.COLOR_LIGHT_BLUE)));

    public static final RegistryObject<Block> ESSENCE_WIRELESS_OUTPUT_BUS = BLOCKS.register(
            "essence_wireless_output_bus",
            () -> new EssenceBusBlock(EssenceBusBlock.Mode.OUTPUT, true, machine(MapColor.COLOR_YELLOW)));

    /** 导线：不存 RE，只做网络连通（可接缓存单元）。 */
    public static final RegistryObject<Block> ESSENCE_CABLE = BLOCKS.register("essence_cable",
            () -> new EssenceCableBlock(BlockBehaviour.Properties.of()
                    .strength(0.6f)
                    .sound(SoundType.WOOL)
                    .mapColor(MapColor.COLOR_PURPLE)
                    .noOcclusion()));

    static {
        // 在类加载时注册多方块结构（纯静态表，不依赖注册事件）
        EssencePortPattern.register();
    }

    /** 方块实体：只有端口需要（总线/扩展接口/导线目前是纯方块）。 */
    public static final class ModEssenceBlockEntities {

        public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
                DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Main.MODID);

        public static final RegistryObject<BlockEntityType<EssenceNetworkPortBlockEntity>> ESSENCE_NETWORK_PORT =
                BLOCK_ENTITIES.register("essence_network_port",
                        () -> BlockEntityType.Builder.of(
                                EssenceNetworkPortBlockEntity::new,
                                ModEssenceBlocks.ESSENCE_NETWORK_PORT.get()
                        ).build(null));

        /** 四类总线共用同一个方块实体类型（模式由方块决定）。 */
        public static final RegistryObject<BlockEntityType<com.pha.trainees.blockentity.EssenceBusBlockEntity>> ESSENCE_BUS =
                BLOCK_ENTITIES.register("essence_bus",
                        () -> BlockEntityType.Builder.of(
                                com.pha.trainees.blockentity.EssenceBusBlockEntity::new,
                                ModEssenceBlocks.ESSENCE_INPUT_BUS.get(),
                                ModEssenceBlocks.ESSENCE_OUTPUT_BUS.get(),
                                ModEssenceBlocks.ESSENCE_WIRELESS_INPUT_BUS.get(),
                                ModEssenceBlocks.ESSENCE_WIRELESS_OUTPUT_BUS.get()
                        ).build(null));

        private ModEssenceBlockEntities() {
        }
    }

    private ModEssenceBlocks() {
    }

    /** 机器方块的统一手感（与魔法线其它机器一致）。 */
    private static BlockBehaviour.Properties machine(MapColor color) {
        return BlockBehaviour.Properties.of()
                .strength(3.5f, 6.0f)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL)
                .mapColor(color);
    }
}
