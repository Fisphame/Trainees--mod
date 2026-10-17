package com.pha.trainees.item;

import com.pha.trainees.registry.ModEntities;
import com.pha.trainees.util.interfaces.IHoverText;
import com.pha.trainees.util.interfaces.TextSignals;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber
public class TenebrisPlumeClarionItem extends Item implements IHoverText {

    private static final String TOO_DARK_MSG = "message.trainees.tenebris_plume_clarion.too_dark";
    private static final int DELAY_TICKS = 100; // 5秒
    private static final int SUMMON_COOLDOWN = 60; // 3秒

    // 存储每个玩家的延迟召唤数据
    private static final Map<UUID, DelayedSummonData> PENDING_SUMMONS = new ConcurrentHashMap<>();

    public TenebrisPlumeClarionItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        // 检查是否为夜晚
        if (!level.isNight()) {
            player.displayClientMessage(
                    Component.translatable(TOO_DARK_MSG).withStyle(ChatFormatting.GRAY),
                    true
            );
            return InteractionResultHolder.fail(stack);
        }

        // 检查玩家是否已有待召唤任务
        if (PENDING_SUMMONS.containsKey(player.getUUID())) {
            player.displayClientMessage(TextSignals.FAIL3.component(), true);
            return InteractionResultHolder.fail(stack);
        }

        // 记录召唤位置（玩家正上方2格）
        BlockPos spawnPos = player.blockPosition().above(2);
        ServerLevel serverLevel = (ServerLevel) level;


        level.playSound(player, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(0).get(), SoundSource.PLAYERS, 2.0F, 0.8F);

        // 保存延迟任务
        DelayedSummonData data = new DelayedSummonData(serverLevel.dimension().location().toString(),
                spawnPos, serverLevel.getGameTime() + DELAY_TICKS);
        PENDING_SUMMONS.put(player.getUUID(), data);

        // 提示玩家
        player.displayClientMessage(TextSignals.COOLDOWN.component().withStyle(ChatFormatting.GRAY), true);

        // 设置物品冷却（防止短时间内多次使用）
        player.getCooldowns().addCooldown(stack.getItem(), SUMMON_COOLDOWN);

        return InteractionResultHolder.success(stack);
    }

    // 每tick检查并处理延迟召唤
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        PENDING_SUMMONS.entrySet().removeIf(entry -> {
            UUID playerId = entry.getKey();
            DelayedSummonData data = entry.getValue();

            // 获取玩家（可能已离线）
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(playerId);
            if (player == null || !player.isAlive()) {
                // 玩家离线或死亡，取消召唤
                return true;
            }

            ServerLevel level = event.getServer().getLevel(player.level().dimension());
            if (level == null) return true;

            long currentTime = level.getGameTime();

            // 在召唤位置持续播放粒子效果（酝酿特效）
            BlockPos pos = data.spawnPos;
            level.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    3, 0.3, 0.3, 0.3, 0.05
            );
            // 添加灵魂沙漏粒子
            level.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.PORTAL,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    1, 0.2, 0.2, 0.2, 0.1
            );

            // 检查是否到达召唤时间
            if (currentTime >= data.summonTime) {
                // 召唤Boss
                Entity boss = ModEntities.KUN_ANTI_BOSS.get().create(level);
                if (boss != null) {
                    boss.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                    level.addFreshEntity(boss);

                    // 召唤爆发特效
                    level.sendParticles(
                            net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                            pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
                            50, 2.0, 1.0, 2.0, 0.2
                    );
                    level.sendParticles(
                            net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            10, 1.0, 1.0, 1.0, 0.0
                    );
                    level.playSound(null, pos,
                            SoundEvents.ENDERMAN_TELEPORT,
                            SoundSource.HOSTILE,
                            2.0F, 0.6F
                    );

                    // 通知玩家
                    player.displayClientMessage(TextSignals.K.component().withStyle(ChatFormatting.RED), true);
                }
                // 移除任务
                return true;
            }
            return false;
        });
    }

        // 存储延迟召唤数据
        private record DelayedSummonData(String dimension, BlockPos spawnPos, long summonTime) {
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        addHoverText(stack, level, tooltip, flag, "tenebris_plume_clarion");
    }
}