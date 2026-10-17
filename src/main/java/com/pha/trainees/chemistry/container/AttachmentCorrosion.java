package com.pha.trainees.chemistry.container;

import com.pha.trainees.registry.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 附着件的腐蚀（§19.32 的"本批未做"项之一：搅拌器轴 / 夹套）。
 *
 * <p><b>物理口径</b>：附着件泡在同一个容器里，所以侵蚀它的就是**同一釜内容物**；
 * 但它有**自己的材质**，所以载荷要按自己的材质重算一遍（钢轴怕 Cl⁻ 点蚀、玻璃衬夹套怕热碱），
 * 而不是直接抄容器的腐蚀度——这是这个类存在的唯一理由。</p>
 *
 * <p><b>与容器腐蚀的三点差别（都是有意的）</b>：</p>
 * <ol>
 *   <li>附着件**不参与"钢遇酸溶出 Fe²⁺ 与 H₂"的记账**——那是容器基体的账；附着件只累计自己的腐蚀度。</li>
 *   <li>附着件没有"降级"这一说（它不是衬层结构），**吃透即报废**：破坏方块且**不掉落**
 *       （烂掉的搅拌轴没有回收价值），并给附近一次提示。</li>
 *   <li>阈值沿用容器的配置（{@code corrosionWarningThreshold}），不另立一套数值。</li>
 * </ol>
 *
 * <p>纯逻辑尽量小：本类只负责"累加 + 到点处理 + 提示"，状态仍然存在附着件自己的 NBT 里。</p>
 */
public final class AttachmentCorrosion {

    /** NBT 键：附着件的腐蚀度（存在附着件自己身上，跟着方块走）。 */
    public static final String NBT_KEY = "AttachmentCorrosion";

    /** 提示半径（格）。 */
    private static final double NOTIFY_RADIUS = 16.0;

    private AttachmentCorrosion() {
    }

    /**
     * 累加腐蚀度并处理后果。
     *
     * @param materialKey 附着件材质的语言键后缀（例如 "metal"），只用于提示文案
     * @return 累加后的腐蚀度（调用方存回自己的字段）
     */
    public static double apply(Level level, BlockPos pos, double current, double delta, String materialKey) {
        if (level == null || level.isClientSide() || delta <= 0) {
            return current;
        }
        double updated = Math.min(1.0, Math.max(0.0, current + delta));

        if (updated >= 1.0) {
            level.playSound(null, pos, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 0.8f, 0.7f);
            notifyNearby(level, pos, "message.trainees.corrosion.attachment_broken",
                    Component.translatable("trainees.material." + materialKey));
            level.destroyBlock(pos, false);   // 不掉落：报废件
            return updated;
        }

        double threshold = ModConfig.COMMON.corrosionWarningThreshold.get();
        if (updated >= threshold && level.getGameTime() % 100 == 0) {
            notifyNearby(level, pos, "message.trainees.corrosion.attachment_warning",
                    String.format("%.1f", updated * 100),
                    Component.translatable("trainees.material." + materialKey));
        }
        return updated;
    }

    /** 给附近玩家一条 actionbar 提示（附着件没有容器那套 notifyNearby，所以自带一份）。 */
    private static void notifyNearby(Level level, BlockPos pos, String key, Object... args) {
        for (Player player : level.players()) {
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                    <= NOTIFY_RADIUS * NOTIFY_RADIUS) {
                player.displayClientMessage(Component.translatable(key, args), true);
            }
        }
    }
}
