package com.pha.trainees.block;

import com.pha.trainees.util.interfaces.IChemistry;
import com.pha.trainees.util.interfaces.IHoneycomb;
import com.pha.trainees.registry.ModChemistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class ChemistryBlock {
    public static class JiOHBlock extends Block implements IChemistry {
        public JiOHBlock(Properties p_49795_) {super(p_49795_);}
    }

    public static class Ji2OBlock extends Block implements IChemistry, IHoneycomb {
        public Ji2OBlock(Properties p_49795_) {
            super(p_49795_);
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                     InteractionHand hand, BlockHitResult hit) {
            return useHoneycomb(state, level, pos, player, hand, hit, ModChemistry.ModChemistryBlocks.CHE_WAXED_JI2O_BLOCK.get());
        }

    }

    public static class WaxedJi2OBlock extends Block implements IChemistry, IHoneycomb {
        public WaxedJi2OBlock(Properties p_49795_) {
            super(p_49795_);
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                     InteractionHand hand, BlockHitResult hit) {
            return useAxe(state, level, pos, player, hand, hit, ModChemistry.ModChemistryBlocks.CHE_JI2O_BLOCK.get());
        }
    }

    public static class Ji2O2Block extends Block implements IChemistry {
        public Ji2O2Block(Properties p_49795_) {
            super(p_49795_);
        }
    }
}
