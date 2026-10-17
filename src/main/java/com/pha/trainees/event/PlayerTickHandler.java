package com.pha.trainees.event;

import com.pha.trainees.Main;
import com.pha.trainees.util.game.ArmorChecker;
import com.pha.trainees.util.game.ParticleHelper;
import com.pha.trainees.util.game.Tools;
import com.pha.trainees.util.interfaces.TextSignals;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = Main.MODID)
public class PlayerTickHandler {
    // 用于跟踪状态变化，避免重复触发
    private static final WeakHashMap<Player, Boolean> armorStateMap = new WeakHashMap<>();
    private static final int CHECK_INTERVAL = 100;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        // 用玩家自身的 tickCount 节流，避免全局静态计数器在多玩家间互相干扰
        if (player.tickCount % CHECK_INTERVAL != 0) return;

        // 检查是否穿着完整套装
        // 获取之前的状态
        boolean isWearingSet = ArmorChecker.isWearingFullArmorSet(player);
        Boolean wasWearingSet = armorStateMap.get(player);


        // 如果状态发生变化
        if (wasWearingSet == null || wasWearingSet != isWearingSet) {
            armorStateMap.put(player, isWearingSet);

            if (isWearingSet) {
                onFullArmorEquipped(player);
            } else if (wasWearingSet != null) {
                onFullArmorRemoved(player);
            }
        }

        if (wasWearingSet != null && wasWearingSet) {
            triggerFullArmor(player);
        }
    }

    private static void onFullArmorEquipped(Player player) {
        player.displayClientMessage(
                TextSignals.UP.component().withStyle(ChatFormatting.GREEN),
                true
        );
    }

    private static void onFullArmorRemoved(Player player) {
        player.displayClientMessage(
                TextSignals.DOWN.component().withStyle(ChatFormatting.RED),
                true
        );
        ParticleHelper.send(player.level(), ParticleTypes.SOUL, player.getX(), player.getY(), player.getZ(),
                55, 0, 0, 0, 0.15);
        player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        player.removeEffect(MobEffects.FIRE_RESISTANCE);
    }

    private static void triggerFullArmor(Player player){
        ParticleHelper.send(player.level(), ParticleTypes.LAVA, player.getX(), player.getY(), player.getZ(),
                20, 0, 0, 0, 0.15);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 110, 2));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 110));
    }

}
