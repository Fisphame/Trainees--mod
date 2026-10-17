package com.pha.trainees.util.interfaces;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * 玩家可见的 UI 状态信号（原 IHoverText 接口常量）。
 * 用于多方块结构激活、物品使用等反馈提示。
 */
public enum TextSignals {
    SUCCESS("--[ - ]--", ChatFormatting.GREEN),
    SUCCESS2("-- [-√-] --", ChatFormatting.GREEN),
    FAIL("--x--", ChatFormatting.RED),
    FAIL2("-[-x-]-", ChatFormatting.RED),
    FAIL3("--[-x-]--", ChatFormatting.RED),
    COOLDOWN("···", null),
    UP("--[ ↑ ]--", null),
    DOWN("--[ ↓ ]--", null),
    LOSE("--?--", ChatFormatting.RED),
    WARN("--- ! ---", null),
    K("§k-------", null);

    private final String text;
    private final ChatFormatting style;

    TextSignals(String text, ChatFormatting style) {
        this.text = text;
        this.style = style;
    }

    public String text() {
        return text;
    }

    /** 生成带默认样式的文本组件（调用方可再叠加 withStyle） */
    public MutableComponent component() {
        MutableComponent c = Component.literal(text);
        if (style != null) {
            c.withStyle(style);
        }
        return c;
    }
}
