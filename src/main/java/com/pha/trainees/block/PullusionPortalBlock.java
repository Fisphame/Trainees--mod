package com.pha.trainees.block;

import com.pha.trainees.Main;
import com.pha.trainees.registry.ModDimensions;
import com.pha.trainees.util.types.Teleporter;
import com.pha.trainees.util.math.MathT;
import com.pha.trainees.util.math.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class PullusionPortalBlock extends Block {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    protected static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D,
            16.0D, 12.0D, 16.0D);

    private static final long COOLDOWN_MS = 1000; // 1秒冷却
    // 按传送门方块位置记录上次使用时间（Block 是单例，不能把冷却放实例字段）
    private static final Map<BlockPos, Long> LAST_USE = new HashMap<>();

    public PullusionPortalBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(ACTIVE, false));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player,
                                 @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // 冷却检查（按传送门位置）
        long currentTime = System.currentTimeMillis();
        Long lastUse = LAST_USE.get(pos);
        if (lastUse != null && currentTime - lastUse < COOLDOWN_MS) {
            return InteractionResult.PASS;
        }
        LAST_USE.put(pos, currentTime);

        // 检查玩家是否可以传送
        if (!player.canChangeDimensions()) {
            return InteractionResult.PASS;
        }

        // 激活传送门
        level.setBlock(pos, state.setValue(ACTIVE, true), 3);
        level.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);

        if (player instanceof ServerPlayer serverPlayer) {
            // 延迟1 tick执行传送，确保服务器端处理完成
            serverPlayer.server.execute(() -> {
                if (!teleportToDimension(serverPlayer, pos)) {
                    // 传送失败，重置传送门状态
                    level.setBlock(pos, state.setValue(ACTIVE, false), 3);
                }
            });
        }
        return InteractionResult.PASS;
    }

    private boolean teleportToDimension(ServerPlayer player, BlockPos portalPos) {
        MinecraftServer server = player.getServer();
        if (server == null) return false;

        ResourceKey<Level> currentDim = player.level().dimension();
        ResourceKey<Level> destination;

        // 判断目标维度
        if (currentDim == ModDimensions.PULLUSION_LEVEL) {
            destination = Level.OVERWORLD;
        } else {
            destination = ModDimensions.PULLUSION_LEVEL;
        }

        ServerLevel destinationLevel = server.getLevel(destination);
        if (destinationLevel == null) {
            Main.LOGGER.error("无法加载维度: {}", destination.location());
            return false;
        }

        // 计算安全的目标位置
        BlockPos destPos = findSafeSpawnLocation(destinationLevel, portalPos);

        Main.LOGGER.debug("传送: {} -> {}", currentDim.location(), destination.location());
        Main.LOGGER.debug("目标位置: {}", destPos);

        // 使用自定义传送器
        player.changeDimension(destinationLevel, new Teleporter(destPos));

        return true;
    }

    private static final int[] dx = {0, 0, 0, 0, -1, 1};  // X方向
    private static final int[] dy = {1, -1, 0, 0, 0, 0};  // Y方向（上下）
    private static final int[] dz = {0, 0, -1, 1, 0, 0};  // Z方向

    /**
     * 在指定范围内搜索安全的落脚点（BFS，状态均为局部变量，避免跨调用共享）
     */
    public static BlockPos find(Level level, BlockPos pos, Pair xRange, Pair yRange, Pair zRange) {
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.offer(pos);
        visited.add(pos);
        while (!queue.isEmpty()) {
            BlockPos uPos = queue.poll();
            int ux = uPos.getX();
            int uy = uPos.getY();
            int uz = uPos.getZ();

            for (int i = 0; i < 6; i++) {
                int vx = ux + dx[i];
                int vy = uy + dy[i];
                int vz = uz + dz[i];
                BlockPos vPos = new BlockPos(vx, vy, vz);

                if (isAllowed(vPos, xRange, yRange, zRange, visited)) {
                    visited.add(vPos);
                    if (isTarget(vPos, level)) {
                        return vPos;
                    }
                    queue.offer(vPos);
                }
            }
        }
        return null;
    }

    public static Boolean isAllowed(BlockPos pos, Pair xRange, Pair yRange, Pair zRange, Set<BlockPos> visited) {
        return MathT.isInInterval(pos.getX(), xRange)
                && MathT.isInInterval(pos.getY(), yRange)
                && MathT.isInInterval(pos.getZ(), zRange)
                && !visited.contains(pos);
    }

    public static Boolean isTarget(BlockPos pos, Level level) {
        return level.getBlockState(pos.below()).isSolid() && level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above());
    }

    /**
     * 寻找安全的生成位置
     */
    private BlockPos findSafeSpawnLocation(ServerLevel level, BlockPos pos) {
        Pair xRange = new Pair(pos.getX() - 8, pos.getX() + 8);
        Pair yRange = new Pair(level.getMinBuildHeight() + 1, level.getMaxBuildHeight() - 2);
        Pair zRange = new Pair(pos.getZ() - 8, pos.getZ() + 8);
        BlockPos newPos = find(level, pos, xRange, yRange, zRange);
        if (newPos != null) {
            return newPos;
        }
        return new BlockPos(pos.getX(), level.getSeaLevel(), pos.getZ());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(ACTIVE, false);
    }
}
