package com.pha.trainees.util.game.structure;

import com.pha.trainees.Main;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 多方块结构模式
 */
public class MultiblockPattern {

    // 结构尺寸
    private final int width;   // X方向
    private final int height;  // Y方向
    private final int depth;   // Z方向

    // 结构原点偏移（匹配位置 -> 结构原点）
    private final BlockPos originOffset;

    // 匹配位置（通常是结构中的关键方块）
    private final BlockPos matchPos;

    // 激活处理器
    @Nullable
    private final IActivationHandler activationHandler;

    // 方块条件映射：相对位置 -> 条件
    private final Map<BlockPos, ConditionEntry> conditions = new HashMap<>();

    // 忽略的位置（通常是空气）
    private final Set<BlockPos> ignoredPositions = new HashSet<>();

    // 允许的朝向（默认只有 NORTH，见 Builder#facings）
    private final List<Direction> validFacings;

    // 数量约束（例如"网络扩展接口最多一个"）：位置判据管不了的，靠这个补
    private final List<CountConstraint> countConstraints = new ArrayList<>();

    /** 最近一次校验失败的原因（成功时为 null）——给调用方做玩家提示用。 */
    @Nullable
    private String lastFailReason;

    /** 数量约束：某种方块（按方块或按 tag 匹配）在整个结构里的数量必须落在 [min, max]。 */
    public record CountConstraint(java.util.function.Predicate<BlockState> matches, int min, int max,
                                  String description) {
    }

    /**
     * 结构匹配结果
     */
    public static class MatchResult {
        public final BlockPos matchPos;       // 匹配的位置（传递给激活处理器）
        public final BlockPos originPos;      // 结构原点位置
        public final Direction facing;        // 结构朝向
        /** 本结构所有"有判据"的局部坐标（相对原点）：由模式带进来，用于枚举成员位置。 */
        private final List<BlockPos> relativePositions;


        public MatchResult(BlockPos matchPos, BlockPos originPos, Direction facing) {
            this(matchPos, originPos, facing, List.of());
        }

        public MatchResult(BlockPos matchPos, BlockPos originPos, Direction facing,
                           List<BlockPos> relativePositions) {
            this.matchPos = matchPos;
            this.originPos = originPos;
            this.facing = facing;
            this.relativePositions = List.copyOf(relativePositions);
        }

        /**
         * 获取结构中的绝对位置（考虑朝向）
         */
        public BlockPos getAbsolutePos(BlockPos relativePos) {
            return rotatePosition(originPos, relativePos, facing);
        }

        /**
         * 获取所有结构方块位置（绝对坐标，已按朝向旋转）。
         *
         * <p>以前这个方法返回**空集**（一个坑：调用方以为能拿到成员列表）。现在由模式把它所有
         * 有判据的局部坐标带进来，于是"枚举成员方块"不再需要调用方自己手写三重循环。</p>
         */
        public Set<BlockPos> getAllPositions() {
            Set<BlockPos> positions = new HashSet<>(relativePositions.size());
            for (BlockPos relative : relativePositions) {
                positions.add(rotatePosition(originPos, relative, facing));
            }
            return positions;
        }
    }

    public static class SafeMatchResult {
        public final MatchResult matchResult;
        public final String error;
        public final boolean success;

        public SafeMatchResult(MatchResult matchResult) {
            this.matchResult = matchResult;
            this.error = null;
            this.success = true;
        }

        public SafeMatchResult(String error) {
            this.matchResult = null;
            this.error = error;
            this.success = false;
        }
    }

    /**
     * 条件条目
     */
    private static class ConditionEntry {
        final IBlockPredicate predicate;
        final boolean required;
        final String description;

        ConditionEntry(IBlockPredicate predicate, boolean required, String description) {
            this.predicate = predicate;
            this.required = required;
            this.description = description;
        }
    }

    public MultiblockPattern(Builder builder) {
        this.width = builder.width;
        this.height = builder.height;
        this.depth = builder.depth;
        this.originOffset = builder.originOffset;
        this.matchPos = builder.matchPos;
        this.activationHandler = builder.activationHandler;
        this.conditions.putAll(builder.conditions);
        this.ignoredPositions.addAll(builder.ignoredPositions);
        this.validFacings = List.copyOf(builder.validFacings);
        this.countConstraints.addAll(builder.countConstraints);
    }

    /**
     * 检查结构是否完整
     */
    @Nullable
    public MatchResult check(Level level, BlockPos checkPos) {
        // 计算结构原点
        BlockPos originPos = checkPos.offset(originOffset);

        // 检查所有方向
        List<Direction> validFacings = getValidFacings();

        for (Direction facing : validFacings) {
            if (checkAtFacing(level, originPos, facing)) {
                BlockPos actualMatchPos = rotatePosition(originPos, matchPos, facing);
                lastFailReason = null;
                return new MatchResult(actualMatchPos, originPos, facing, new ArrayList<>(conditions.keySet()));
            }
        }

        return null;
    }

    /** 最近一次校验失败的原因（成功为 null）：数量约束失败或区块未加载时会有值。 */
    @Nullable
    public String getLastFailReason() {
        return lastFailReason;
    }

    /**
     * 安全地检查结构完整性，避免异常传播
     */
    @Nullable
    public SafeMatchResult checkSafe(Level level, BlockPos checkPos) {
        try {
            MatchResult result = check(level, checkPos);
            return result != null ? new SafeMatchResult(result) : null;
        } catch (Exception e) {
            Main.LOGGER.error("Error checking multiblock pattern at {}", checkPos, e);
            return new SafeMatchResult(e.getMessage());
        }
    }

    /**
     * 检查特定朝向的结构
     */
    private boolean checkAtFacing(Level level, BlockPos originPos, Direction facing) {
//        Main.LOGGER.info("Checking structure at origin {} facing {}", originPos, facing);

        for (Map.Entry<BlockPos, ConditionEntry> entry : conditions.entrySet()) {
            BlockPos relativePos = entry.getKey();
            ConditionEntry condition = entry.getValue();

            // 旋转位置（考虑朝向）
            BlockPos absolutePos = rotatePosition(originPos, relativePos, facing);

            // 区块未加载：**判定为不成立，绝不触发区块加载**（作者 2026-10-03 确认的口径）
            if (!level.isLoaded(absolutePos)) {
                lastFailReason = "reason.trainees.structure.chunk_unloaded";
                return false;
            }

            BlockState state = level.getBlockState(absolutePos);

            // 记录当前位置信息
            String blockName = state.getBlock().getDescriptionId();

            // 执行条件检查
            boolean testResult = condition.predicate.test(level, absolutePos, state);

//            Main.LOGGER.info("  Pos {} -> {} (expected {}) => {}",
//                    absolutePos, state.getBlock().getDescriptionId(),
//                    condition.description, testResult);

            if (!testResult && condition.required) {
                Main.LOGGER.warn("Condition failed at {}", absolutePos);
                lastFailReason = condition.description;
                return false;
            }
        }

        // 数量约束：位置判据管不了的（例如"某类功能方块最多一个"）
        if (!countConstraints.isEmpty()) {
            for (CountConstraint constraint : countConstraints) {
                int count = 0;
                for (BlockPos relativePos : conditions.keySet()) {
                    // 数量统计只扫"有判据的位置"，也就是结构的成员格
                    BlockPos absolutePos = rotatePosition(originPos, relativePos, facing);
                    if (!level.isLoaded(absolutePos)) continue;
                    if (constraint.matches().test(level.getBlockState(absolutePos))) {
                        count++;
                    }
                }
                if (count < constraint.min() || count > constraint.max()) {
                    lastFailReason = constraint.description();
                    return false;
                }
            }
        }


        // 检查忽略的位置
        if (!ignoredPositions.isEmpty()) {
            for (BlockPos relativePos : ignoredPositions) {
                BlockPos absolutePos = rotatePosition(originPos, relativePos, facing);
                if (!level.isEmptyBlock(absolutePos)) {
                    Main.LOGGER.info("   ⚠ [Ignored] Position {} has block: {}",
                            absolutePos, level.getBlockState(absolutePos).getBlock().getDescriptionId());
                }
            }
        }

        return true;
    }

    /**
     * 获取有效的朝向
     */
    private List<Direction> getValidFacings() {
        // 默认只支持北朝向（保持化学线现有多方块的行为不变）；
        // 需要"朝任意面激活"的结构在 Builder 里用 facings(...) 显式指定。
        return validFacings;
    }

    /**
     * 旋转位置（考虑朝向）
     */
    private static BlockPos rotatePosition(BlockPos origin, BlockPos relativePos, Direction facing) {
        int x = relativePos.getX();
        int y = relativePos.getY();
        int z = relativePos.getZ();

        // 根据朝向旋转
        return switch (facing) {
            case EAST -> origin.offset(-z, y, x);
            case SOUTH -> origin.offset(-x, y, -z);
            case WEST -> origin.offset(z, y, -x);
            default -> origin.offset(x, y, z);
        };
    }

    /**
     * 获取结构尺寸
     */
    public BlockPos getDimensions() {
        return new BlockPos(width, height, depth);
    }

    /**
     * 获取激活处理器
     */
    @Nullable
    public IActivationHandler getActivationHandler() {
        return activationHandler;
    }

    /**
     * 获取结构匹配位置（相对位置）
     */
    public BlockPos getMatchPos() {
        return matchPos;
    }

    /**
     * 获取结构原点偏移
     */
    public BlockPos getOriginOffset() {
        return originOffset;
    }

    /**
     * 构建器类
     */
    public static class Builder {
        private int width = 1;
        private int height = 1;
        private int depth = 1;
        private BlockPos originOffset = BlockPos.ZERO;
        private BlockPos matchPos = BlockPos.ZERO;
        @Nullable
        private IActivationHandler activationHandler = null;
        private final Map<BlockPos, ConditionEntry> conditions = new HashMap<>();
        private final Set<BlockPos> ignoredPositions = new HashSet<>();
        /** 允许的朝向：默认只有 NORTH（保持既有结构不变）；要"朝任意面激活"就在这里显式给全。 */
        private List<Direction> validFacings = Collections.singletonList(Direction.NORTH);

        /** 数量约束（位置判据管不了的，例如"某类功能方块最多一个"）。 */
        private final List<CountConstraint> countConstraints = new ArrayList<>();

        /** 设置允许的朝向（例如四个水平方向）。 */
        public Builder facings(List<Direction> facings) {
            if (facings != null && !facings.isEmpty()) {
                this.validFacings = List.copyOf(facings);
            }
            return this;
        }

        /**
         * 加一条"必须是某类方块"的判据（按**方块 tag**）——比逐个方块写死更适合让整合包换材料。
         */
        public Builder addTagCondition(BlockPos relativePos, net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> tag) {
            return addCondition(relativePos, (level, pos, state) -> state.is(tag), true, "#" + tag.location());
        }

        /**
         * 数量约束：某个方块在结构里的数量必须落在 [min, max]。
         *
         * <p>为什么需要它：位置判据只能表达"某一格是什么"，表达不了"这种东西最多几个"——
         * 而"网络扩展接口最多一个"正是后者（作者 §17.1 ④ 的第二条限定）。</p>
         *
         * <p><b>参数必须是 Supplier 而不是 Block</b>：结构注册发生在类初始化阶段，那时注册事件还没跑，
         * 直接传 {@code ModBlocks.X.get()} 会抛 "Registry Object not present"（我们真踩过，服务端起不来）。
         * 传 {@code ModBlocks.X::get} 就是懒求值，校验时才解析。</p>
         */
        public Builder addBlockCount(java.util.function.Supplier<? extends net.minecraft.world.level.block.Block> block,
                                     int min, int max, String description) {
            countConstraints.add(new CountConstraint(state -> state.is(block.get()), min, max, description));
            return this;
        }

        /** 数量约束（最底层形式：自己给判据，天然懒求值）。 */
        public Builder addCount(java.util.function.Predicate<BlockState> matches, int min, int max, String description) {
            countConstraints.add(new CountConstraint(matches, min, max, description));
            return this;
        }

        /** 数量约束（按 tag 统计）：TagKey 不是注册对象，可以直接传。 */
        public Builder addTagCount(net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> tag,
                                   int min, int max, String description) {
            countConstraints.add(new CountConstraint(state -> state.is(tag), min, max, description));
            return this;
        }

        /**
         * 设置结构尺寸
         */
        public Builder dimensions(int width, int height, int depth) {
            this.width = width;
            this.height = height;
            this.depth = depth;
            return this;
        }

        /**
         * 设置原点偏移
         */
        public Builder originOffset(BlockPos offset) {
            this.originOffset = offset;
            return this;
        }

        /**
         * 设置匹配位置（相对位置）
         */
        public Builder matchPos(BlockPos relativePos) {
            this.matchPos = relativePos;
            return this;
        }

        /**
         * 设置激活处理器
         */
        public Builder activationHandler(IActivationHandler handler) {
            this.activationHandler = handler;
            return this;
        }

        /**
         * 添加方块条件
         */
        public Builder addCondition(BlockPos relativePos, IBlockPredicate predicate) {
            return addCondition(relativePos, predicate, true, "custom condition");
        }

        /**
         * 添加方块条件（带描述）
         */
        public Builder addCondition(BlockPos relativePos, IBlockPredicate predicate, boolean required, String description) {
            conditions.put(relativePos, new ConditionEntry(predicate, required, description));
            return this;
        }

        /**
         * 添加忽略位置（通常是空气位置）
         */
        public Builder addIgnoredPosition(BlockPos relativePos) {
            ignoredPositions.add(relativePos);
            return this;
        }

        /**
         * 构建结构模式
         */
        public MultiblockPattern build() {
            // 验证必要的设置
            if (width <= 0 || height <= 0 || depth <= 0) {
                throw new IllegalArgumentException("Dimensions must be positive");
            }

            if (conditions.isEmpty()) {
                Main.LOGGER.warn("Building MultiblockPattern with no conditions");
            }

            return new MultiblockPattern(this);
        }
    }
}