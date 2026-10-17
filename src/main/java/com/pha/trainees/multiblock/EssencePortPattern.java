package com.pha.trainees.multiblock;

import com.pha.trainees.Main;
import com.pha.trainees.registry.ModEssenceBlocks;
import com.pha.trainees.util.game.structure.MultiblockPattern;
import com.pha.trainees.util.game.structure.MultiblockStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * 精练精华网端口的多方块结构（§17.1 ④，作者 2026-10-03 定的规格）。
 *
 * <pre>
 * 尺寸：3（X）× 高 4（Y）× 3（Z）
 * 局部坐标（facing = NORTH 时）：z = 0 是**正面**那一排；y = 0 是第 1 层（最底）
 *
 *  第4层   . . .        . = 任意"结构方块"（见 tag）
 *  第3层   . . .
 *  第2层   P # .        P = 端口主方块（正面第 2 层中间）
 *  第1层   . # .        # = 端口墙壁方块（3×3 中间那格，纵向全部 4 层）
 * </pre>
 *
 * <p><b>限定的两条</b>（作者原话）：</p>
 * <ol>
 *   <li><b>哪些方块可行</b>：正面第 2 层中间必须是端口主方块；3×3 中间那格纵向全部必须是墙壁方块；
 *       其余格子必须是 tag {@code trainees:essence_port_structure} 里的任意方块
 *       （墙壁方块 / （无线）输入输出总线 / 网络扩展接口）。</li>
 *   <li><b>功能方块数量</b>：（无线）输入输出总线数量不限；**网络扩展接口最多 1 个、最少 0 个**。</li>
 * </ol>
 */
public final class EssencePortPattern {

    public static final String STRUCTURE_ID = "essence_network_port";

    /** 端口主方块在局部坐标里的位置（正面第 2 层中间）。 */
    public static final BlockPos PORT_LOCAL = new BlockPos(1, 1, 0);
    /** 结构原点相对端口主方块的偏移（原点 = 端口 + 这个偏移）。 */
    public static final BlockPos ORIGIN_FROM_PORT = new BlockPos(-1, -1, 0);

    private static final int WIDTH = 3;
    private static final int HEIGHT = 4;
    private static final int DEPTH = 3;

    /** "端口多方块结构方块"tag：墙壁、四类总线、网络扩展接口都在里面。 */
    public static final TagKey<Block> STRUCTURE_BLOCKS =
            TagKey.create(Registries.BLOCK, new ResourceLocation(Main.MODID, "essence_port_structure"));

    private EssencePortPattern() {
    }

    /** 注册结构（由 mod 初始化调用一次）。 */
    public static void register() {
        MultiblockPattern.Builder builder = new MultiblockPattern.Builder()
                .dimensions(WIDTH, HEIGHT, DEPTH)
                .originOffset(ORIGIN_FROM_PORT)
                .matchPos(PORT_LOCAL)
                // 作者要求"可以朝任意面激活"：给四个水平朝向（默认只有 NORTH，见 MultiblockPattern）
                .facings(List.of(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST));

        for (int y = 0; y < HEIGHT; y++) {
            for (int z = 0; z < DEPTH; z++) {
                for (int x = 0; x < WIDTH; x++) {
                    BlockPos local = new BlockPos(x, y, z);

                    // 规则 1a：正面第 2 层中间 = 端口主方块
                    if (local.equals(PORT_LOCAL)) {
                        builder.addCondition(local, (level, pos, state) ->
                                state.is(ModEssenceBlocks.ESSENCE_NETWORK_PORT.get()), true, "端口主方块");
                        continue;
                    }
                    // 规则 1b：3×3 中间那格（纵向全部）= 墙壁方块
                    if (x == 1 && z == 1) {
                        builder.addCondition(local, (level, pos, state) ->
                                state.is(ModEssenceBlocks.ESSENCE_PORT_WALL.get()), true, "端口墙壁方块");
                        continue;
                    }
                    // 规则 1c：其余格子 = 结构 tag 里任意方块（框架新加的标签判据）
                    builder.addTagCondition(local, STRUCTURE_BLOCKS);
                }
            }
        }

        // 规则 2：功能方块数量——框架的数量约束（原来是手写循环，现在归框架管）
        // 注意传方法引用 `::get`：结构注册在类初始化阶段，那时注册对象还没解析（直接 .get() 会崩）
        builder.addBlockCount(ModEssenceBlocks.ESSENCE_EXPANSION_INTERFACE::get, 0, 1,
                "message.trainees.essence_port.too_many_interfaces");

        MultiblockStructure.registerStructure(STRUCTURE_ID, builder.build());
        Main.LOGGER.info("[EssencePort] 已注册多方块结构 {}（{}×{}×{}）", STRUCTURE_ID, WIDTH, HEIGHT, DEPTH);
    }

    /**
     * 规则 2（功能方块数量）**现在由框架的数量约束负责**（见 {@code register()} 里的 addBlockCount）。
     *
     * <p>这个方法保留只是为了让旧的调用点不用改：它把框架记录的失败原因透出去。</p>
     */
    @Deprecated
    public static String validateFunctionBlocks(Level level, MultiblockPattern.MatchResult match) {
        MultiblockPattern pattern = MultiblockStructure.REGISTERED_STRUCTURES.get(STRUCTURE_ID);
        return pattern == null ? null : pattern.getLastFailReason();
    }
}
