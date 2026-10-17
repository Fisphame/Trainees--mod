//package com.pha.trainees.util.game.structure;
//
//import net.minecraft.core.BlockPos;
//import net.minecraft.core.Direction;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.level.block.state.pattern.BlockPattern;
//
//import javax.annotation.Nullable;
//
//public class VanillaPatternAdapter extends MultiblockPattern {
//    private final BlockPattern vanillaPattern;
//
//    private VanillaPatternAdapter(BlockPattern pattern, Builder builder) {
//        super(builder);
//        this.vanillaPattern = pattern;
//    }
//
//    @Override
//    @Nullable
//    public MatchResult check(Level level, BlockPos checkPos) {
//        // 使用原版模式匹配
//        BlockPattern.BlockPatternMatch match = vanillaPattern.find(level, checkPos);
//        if (match != null) {
//            BlockPos matchPos = match.getFrontTopLeft();
//            return new MatchResult(matchPos, matchPos, Direction.NORTH);
//        }
//        return null;
//    }
//
//    /**
//     * 从原版 BlockPattern 创建适配器
//     */
//    public static VanillaPatternAdapter fromVanilla(BlockPattern pattern, int width, int height, int depth) {
//        Builder builder = new Builder()
//                .dimensions(width, height, depth)
//                .originOffset(BlockPos.ZERO);
//
//        return new VanillaPatternAdapter(pattern, builder);
//    }
//}