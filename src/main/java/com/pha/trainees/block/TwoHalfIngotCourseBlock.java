package com.pha.trainees.block;

import com.pha.trainees.registry.ModBlocks;
import com.pha.trainees.registry.ModChemistry;
import com.pha.trainees.util.game.ParticleHelper;
import com.pha.trainees.util.game.Tools;
import com.pha.trainees.util.interfaces.IHoneycomb;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public class TwoHalfIngotCourseBlock {
    public static class TwoHalfIngotBlock extends Block implements IHoneycomb {
        public TwoHalfIngotBlock(Properties p_49795_) {
            super(p_49795_);
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                     Player player, InteractionHand hand, BlockHitResult hit) {
            return useHoneycomb(state, level, pos, player, hand, hit, ModBlocks.WAXED_TWO_HALF_INGOT_BLOCK.get());
        }

        @Override
        public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
            super.onPlace(state, level, pos, oldState, isMoving);

            // 只在服务器端安排计划刻
            if (!level.isClientSide && level instanceof ServerLevel serverLevel) {

                int randomTicks = Tools.randomInRange(level, 300, 450);

                // 安排一个计划刻，在 randomTicks 后执行
                serverLevel.scheduleTick(pos, this, randomTicks);
            }
        }

        @Override
        public void tick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
            super.tick(state, level, pos, random);

            if (state.getBlock() == this) {
                Block targetBlock = ModChemistry.ModChemistryBlocks.CHE_JI2O_BLOCK.get();
                // 替换方块，保持相同的 BlockState 属性（如果有）
                level.setBlock(pos, targetBlock.defaultBlockState(), 3);

                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.0F);
                ParticleHelper.send(level, ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        25, 1.0, 1.0, 1.0, 0);
            }
        }

        @Override
        public boolean isRandomlyTicking(BlockState state) {
            return false;
        }
    }

    public static class WaxedTwoHalfIngotBlock extends Block implements IHoneycomb {
        public WaxedTwoHalfIngotBlock(Properties p_49795_) {
            super(p_49795_);
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                     Player player, InteractionHand hand, BlockHitResult hit) {
            return useAxe(state, level, pos, player, hand, hit, ModBlocks.TWO_HALF_INGOT_BLOCK.get());
        }
    }

    public static class AntiTwoHalfIngotBlock extends Block {
        public AntiTwoHalfIngotBlock(Properties p_49795_) {
            super(p_49795_);
        }
    }
}
