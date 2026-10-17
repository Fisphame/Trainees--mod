package com.pha.trainees.util.game;

import com.pha.trainees.Main;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class Achievement {

    /**
     * 授予特定进度给玩家
     * @param player 玩家
     * @param advancementId 进度ID，格式为 "modid:path/to/advancement"
     */
    public static void grantSpecificAchievement(ServerPlayer player, String advancementId) {
        try {
            MinecraftServer server = player.getServer();
            if (server == null) return;
            // 获取进度
            Advancement advancement = server.getAdvancements().getAdvancement(new ResourceLocation(advancementId));

            if (advancement == null) return;

            // 获取进度进度
            AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);

            // 检查是否已经完成
            if (progress.isDone()) return;

            // 授予进度
            for (String criterion : progress.getRemainingCriteria()) {
                player.getAdvancements().award(advancement, criterion);
            }

        } catch (Exception e) {
            Main.LOGGER.error("Error granting achievement: " + advancementId, e);
        }
    }

    /**
     * 检查玩家是否拥有特定进度
     * @param player 玩家
     * @param advancementId 进度ID
     * @return 是否拥有该进度
     */
    public static boolean hasAdvancement(ServerPlayer player, String advancementId) {
        try {
            MinecraftServer server = player.getServer();
            if (server == null) return false;
            Advancement advancement = server.getAdvancements().getAdvancement(new ResourceLocation(advancementId));
            if (advancement == null) return false;
            AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
            return progress.isDone();
        } catch (Exception e) {
            Main.LOGGER.error("Error checking achievement: {}", advancementId, e);
            return false;
        }
    }
}
