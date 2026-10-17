//package com.pha.trainees.util.game.structure;
//
//import net.minecraft.core.BlockPos;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.level.block.state.BlockState;
//
//public class StructureDebugger {
//
//    /**
//     * 调试结构检测，返回详细信息
//     */
//    public static String debugStructure(Level level, BlockPos checkPos, String structureId) {
//        MultiblockPattern pattern = MultiblockStructure.REGISTERED_STRUCTURES.get(structureId);
//        if (pattern == null) {
//            return "Unknown structure: " + structureId;
//        }
//
//        StringBuilder sb = new StringBuilder();
//        sb.append("=== Structure Debug: ").append(structureId).append(" ===\n");
//        sb.append("Check position: ").append(checkPos).append("\n");
//
//        // 计算原点
//        BlockPos originPos = checkPos.offset(pattern.getOriginOffset());
//        sb.append("Origin position: ").append(originPos).append("\n");
//        sb.append("Match position (relative): ").append(pattern.getMatchPos()).append("\n");
//
//        // 检查所有条件
//        sb.append("\nChecking conditions:\n");
//
//        // 这里需要访问私有字段，我们可以添加一个调试方法到 MultiblockPattern
//        // 或者使用反射（不推荐）
//        // 简单起见，我们可以创建一个公共的调试方法
//
//        return sb.toString();
//    }
//
//    /**
//     * 可视化结构位置
//     */
//    public static void visualizePositions(Level level, BlockPos corePos) {
//        // 根据结构定义计算所有关键位置
//        BlockPos[] positions = {
//                corePos.below(1),  // (0,0,0)
//                corePos,           // (0,1,0) - 核心
//                corePos.above(1),  // (0,2,0)
//                corePos.below(1).west(2),   // (-2,0,0)
//                corePos.below(1).east(2),   // (2,0,0)
//                corePos.below(1).north(2),  // (0,0,-2)
//                corePos.below(1).south(2)   // (0,0,2)
//        };
//
//        String[] names = {
//                "two_half_ingot_block (0,0,0)",
//                "altar_core_block (0,1,0)",
//                "two_half_ingot_block (0,2,0)",
//                "kun_altar (-2,0,0)",
//                "kun_altar (2,0,0)",
//                "kun_altar (0,0,-2)",
//                "kun_altar (0,0,2)"
//        };
//
//        for (int i = 0; i < positions.length; i++) {
//            BlockState state = level.getBlockState(positions[i]);
//            System.out.printf("%-30s: %s at %s\n",
//                    names[i],
//                    state.getBlock().getDescriptionId(),
//                    positions[i]);
//        }
//    }
//}
