package com.pha.trainees.block;

import com.pha.trainees.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BasketballAntiBlock extends Block {
    public BasketballAntiBlock(Properties p_49795_) { super(p_49795_); }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        super.playerWillDestroy(level, pos, state, player);

        if (!level.isClientSide && !player.isCreative()) {
            ItemStack itemStack = new ItemStack(ModItems.BASKETBALL_ANTI.get());
            popResource(level, pos, itemStack);
        }
    }

}
