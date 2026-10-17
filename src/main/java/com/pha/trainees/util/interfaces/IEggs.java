package com.pha.trainees.util.interfaces;

import com.pha.trainees.item.EggCourseItem;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface IEggs {


    default InteractionResultHolder<ItemStack> useEgg(Level level, Player player, InteractionHand hand,
                                                      EggItem egg, int whichEgg) {
        ItemStack itemstack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EGG_THROW, SoundSource.PLAYERS,
                0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!level.isClientSide) {
            ThrownEgg thrownEgg;
            switch (whichEgg) {
                case 1 -> thrownEgg = new EggCourseItem.ThrownKunEgg(level, player);
                case 2 -> thrownEgg = new EggCourseItem.ThrownBlackEgg(level, player);
                case 3 -> thrownEgg = new EggCourseItem.ThrownGoldEgg(level, player);
                default -> {
                    return InteractionResultHolder.fail(itemstack);
                }
            }
            thrownEgg.setItem(itemstack);
            thrownEgg.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            level.addFreshEntity(thrownEgg);
        }

        player.awardStat(Stats.ITEM_USED.get(egg));
        if (!player.getAbilities().instabuild) {
            itemstack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }

    default void hit(ThrownEgg thrownEgg, Entity entity) {
        if (!thrownEgg.level().isClientSide && entity instanceof Chicken chicken) {
            chicken.setAge(-24000); // 设置为幼年
            chicken.moveTo(thrownEgg.getX(), thrownEgg.getY(), thrownEgg.getZ(), thrownEgg.getYRot(), 0.0F);
            thrownEgg.level().addFreshEntity(chicken);

            thrownEgg.level().broadcastEntityEvent(thrownEgg, (byte)3);
            thrownEgg.discard();
        }
    }
}
