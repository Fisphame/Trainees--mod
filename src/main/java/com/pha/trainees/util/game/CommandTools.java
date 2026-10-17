package com.pha.trainees.util.game;

import com.pha.trainees.registry.ModCommand;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

public class CommandTools {
    public static boolean getEnchantBrokenBlockDrop(Level level) {
        if (level == null) return false;
        GameRules rules = level.getGameRules();
        return rules.getBoolean(ModCommand.ENCHANT_BROKEN_BLOCK_DROP);
    }
    public static boolean getEnchantBreakBlock(Level level) {
        if (level == null) return false;
        GameRules rules = level.getGameRules();
        return rules.getBoolean(ModCommand.ENCHANT_BREAK_BLOCK);
    }
    public static boolean isReactionExplode(Level level) {
        if (level == null) return false;
        GameRules rules = level.getGameRules();
        return rules.getBoolean(ModCommand.ALLOW_REACTION_EXPLODE);
    }

    // 设置规则值（需要服务器权限）
    public static void setEnchantBrokenBlockDrop(Level level, boolean value) {
        if (level == null || level.isClientSide()) return;
        GameRules rules = level.getGameRules();
        rules.getRule(ModCommand.ENCHANT_BROKEN_BLOCK_DROP).set(value, level.getServer());
    }
    public static void setEnchantBreakBlock(Level level, boolean value) {
        if (level == null || level.isClientSide()) return;
        GameRules rules = level.getGameRules();
        rules.getRule(ModCommand.ENCHANT_BREAK_BLOCK).set(value, level.getServer());
    }
    public static void setAllowReactionExplode(Level level, boolean value) {
        if (level == null || level.isClientSide()) return;
        GameRules rules = level.getGameRules();
        rules.getRule(ModCommand.ALLOW_REACTION_EXPLODE).set(value, level.getServer());
    }
}
