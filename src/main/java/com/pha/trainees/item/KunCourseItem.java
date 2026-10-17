package com.pha.trainees.item;

import com.pha.trainees.util.interfaces.IBackStab;
import com.pha.trainees.util.interfaces.IChemistry;
import com.pha.trainees.util.interfaces.IHoverText;
import com.pha.trainees.util.interfaces.IMineBlock;
import com.pha.trainees.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class KunCourseItem {

    public static class KunNuggetItem extends Item implements IChemistry {

        public KunNuggetItem(Properties properties) {
            super(properties);
        }

        @Override
        public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
            return levelOn(stack, entity);
        }

    }

    public static class TwoHalfIngotItem extends Item implements IChemistry {

        public TwoHalfIngotItem(Properties properties) {
            super(properties);
        }

        @Override
        public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
            return levelOn(stack, entity);
        }
    }

    public static class TwoHalfIngotBlockItem extends BlockItem implements IChemistry {

        public TwoHalfIngotBlockItem(Block p_40565_, Properties p_40566_) {
            super(p_40565_, p_40566_);
        }

        @Override
        public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
            return levelOn(stack, entity);
        }
    }

    public static class KunPickaxeFinal extends PickaxeItem implements IMineBlock, IHoverText {
        public KunPickaxeFinal(Tier p_42961_, int p_42962_, float p_42963_, Properties p_42964_) {
            super(p_42961_, p_42962_, p_42963_, p_42964_);
        }

        @Override
        public boolean isCorrectToolForDrops(@NotNull ItemStack stack, @NotNull BlockState state) {
            if (state.is(ModBlocks.MYBLOCK.get())) {
                return true;
            }
            return state.is(BlockTags.MINEABLE_WITH_PICKAXE);
        }

        @Override
        public float getDestroySpeed(ItemStack stack, BlockState state) {
            return super.getDestroySpeed(stack, state) * 4.0f;
        }

        @Override
        public boolean mineBlock(ItemStack stack, @NotNull Level level, BlockState state,
                                 BlockPos pos, LivingEntity miningEntity) {
            boolean result = super.mineBlock(stack, level, state, pos, miningEntity);
            finalMining(level, pos);
            return result;
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag flag) {
            super.appendHoverText(stack, level, tooltipComponents, flag);
            addHoverText(stack, level, tooltipComponents, flag, "kun_pickaxe_final_item");
        }
    }

    public static class KunSwordItem extends SwordItem{

        public KunSwordItem(Tier p_43269_, int p_43270_, float p_43271_, Properties p_43272_) {
            super(p_43269_, p_43270_, p_43271_, p_43272_);
        }
    }

    public static class KunDaggerItem extends KunSwordItem implements IBackStab {

        public KunDaggerItem(Tier p_43269_, int p_43270_, float p_43271_, Properties p_43272_) {
            super(p_43269_, p_43270_, p_43271_, p_43272_);
        }

        public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand interaction) {
            return useIt(level, player, interaction);
        }
    }
}
