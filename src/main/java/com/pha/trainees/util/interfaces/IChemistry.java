package com.pha.trainees.util.interfaces;

import com.pha.trainees.util.game.chemistry.ChemicalReaction;
import com.pha.trainees.util.game.chemistry.ReactionSystem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.jetbrains.annotations.NotNull;

public interface IChemistry {
    default boolean levelOn(ItemStack stack, @NotNull ItemEntity entity) {
        if (!entity.level().isClientSide) {
            return ReactionSystem.ReactionRegistry.triggerReactions(stack, entity);
        }
        return false;
    }

    default InteractionResult JiBpCombination_1(UseOnContext context, int number){
        if (ChemicalReaction.JiBpCombination_1(context, number)){
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }


}
