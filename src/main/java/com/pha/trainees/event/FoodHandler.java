package com.pha.trainees.event;

import com.pha.trainees.registry.ModItems;
import com.pha.trainees.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class FoodHandler {
    @SubscribeEvent
    public static void onFoodEaten(LivingEntityUseItemEvent.Finish event) {
        // 仅处理玩家食用食物事件
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;

        Item item = event.getItem().getItem();
        Item a1 = ModItems.AHKUN_APPLE.get(), a2 = ModItems.BIG_AHKUN_APPLE.get(), a3 = ModItems.REAL_APPLE.get();
        if (item == a1) {
            //类型 时间 等级
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 7 * 60 * 20, 0));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 15 * 20, 1));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 2 * 20, 5));
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 10 * 20, 0));
            player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 7 * 60 * 20, 2));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 7 * 60 * 20, 4));
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 7 * 60 * 20, 0));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 3 * 60 * 20, 1));

        } else if (item == a2) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 10 * 60 * 20, 2));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 20, 1));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 2 * 20, 8));
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 10 * 20, 2));
            player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 10 * 60 * 20, 4));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 10 * 60 * 20, 8));
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 10 * 60 * 20, 0));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 10 * 60 * 20, 4));

            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.AHKUN_SOUND.get(), SoundSource.PLAYERS, 1.0f, 1.0f);

        } else if (item == a3){
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 10 * 60 * 20,4));
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 2 * 60 * 20, 4));
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 3 * 20, 4));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30 * 20, 4));
        }
    }
}
