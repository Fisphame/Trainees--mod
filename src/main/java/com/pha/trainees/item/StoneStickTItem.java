package com.pha.trainees.item;

import com.pha.trainees.Main;
import com.pha.trainees.block.AbsorbBlock;
import com.pha.trainees.multiblock.TrainerAltarPattern;
import com.pha.trainees.registry.ModBlocks;
import com.pha.trainees.util.game.ParticleHelper;
import com.pha.trainees.util.game.Tools;
import com.pha.trainees.util.game.structure.ActiveStructureManager;
import com.pha.trainees.util.interfaces.IHoverText;
import com.pha.trainees.util.interfaces.TextSignals;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

/**
 * 传送石棍 T：把"祭坛核心"与一个"投放位置"绑定。
 *
 * <p>历史上还有一个"输入模式（绑定发送方块）"，已随 {@code SenderBlock} 一并删除
 * （作者决定 2026-09-25；模组尚未发布，不考虑向前兼容）。</p>
 */
public class StoneStickTItem extends Item implements IHoverText {

    // ========== 持久数据键 ==========
    private static final String CONFIG_KEY = Main.MODID + "_config";
    private static final String SETTING_MODE_KEY = "SettingMode";
    private static final String SETTING_ALTAR_POS_KEY = "SettingAltarPos";

    public StoneStickTItem(Properties p_41383_) {
        super(p_41383_);
    }

    public @NotNull InteractionResult useOn(UseOnContext context){
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        BlockPos clickedPos = context.getClickedPos();
        ItemStack itemStack = context.getItemInHand();
        BlockState state = level.getBlockState(clickedPos);

        // 获取玩家持久数据
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag configData = persistentData.getCompound(CONFIG_KEY);


        Vector3f color = new Vector3f(0, 255, 0);
        DustParticleOptions options = new DustParticleOptions(color, 1.0f);
        ParticleHelper.sendSurfaces(level, options, clickedPos, 15, 0.25, 0.25, 0.25, 0.0);

        // ========== 1. 读取设置状态 ==========
        boolean isSetting = configData.getBoolean(SETTING_MODE_KEY);
        long altarPosLong = configData.getLong(SETTING_ALTAR_POS_KEY);

        // ========== 2. 处理点击核心方块 ==========
        if (level.getBlockState(clickedPos).is(ModBlocks.ALTAR_CORE_BLOCK.get())) {
            // 检查核心是否已激活
            ActiveStructureManager manager = ActiveStructureManager.get(level);
            boolean isActive = manager.isPositionActive(level, TrainerAltarPattern.STRUCTURE_ID, clickedPos);

            if (!isActive) {
                player.displayClientMessage(
                        Component.literal("目标核心未激活").withStyle(ChatFormatting.RED),
                        true
                );
                return InteractionResult.FAIL;
            }

            if (!isSetting) {
                // 进入设置模式
                configData.putBoolean(SETTING_MODE_KEY, true);
                configData.putLong(SETTING_ALTAR_POS_KEY, clickedPos.asLong());
                persistentData.put(CONFIG_KEY, configData);

                player.displayClientMessage(
                        Component.literal("进入设置模式（设置投放位置），右键目标方块完成绑定").withStyle(ChatFormatting.GRAY),
                        true
                );
            } else {
                // 已在设置模式，再次点击核心则取消设置
                configData.remove(SETTING_MODE_KEY);
                configData.remove(SETTING_ALTAR_POS_KEY);
                persistentData.put(CONFIG_KEY, configData);

                player.displayClientMessage(
                        Component.literal("已取消设置").withStyle(ChatFormatting.GRAY),
                        true
                );
            }
            player.getCooldowns().addCooldown(itemStack.getItem(), 5);
            return InteractionResult.SUCCESS;
        }


        // ========== 3. 点击其他方块（处理设置模式） ==========
        if (!isSetting) {
            // 未处于设置模式，忽略
            return InteractionResult.PASS;
        }

        // 读取核心位置
        BlockPos altarPos = BlockPos.of(altarPosLong);

        // 验证核心是否仍然激活
        ActiveStructureManager manager = ActiveStructureManager.get(level);
        if (!manager.isPositionActive(level, TrainerAltarPattern.STRUCTURE_ID, altarPos)) {
            // 核心已失效，自动退出设置模式
            configData.remove(SETTING_MODE_KEY);
            configData.remove(SETTING_ALTAR_POS_KEY);
            persistentData.put(CONFIG_KEY, configData);
            player.displayClientMessage(tr("hint.altar_invalid_setting_canceled").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        // ===== 设置掉落位置 =====
        BlockPos candidate = null;
        if (state.getBlock() instanceof AbsorbBlock) {
            candidate = clickedPos;
        } else {
            BlockPos above = clickedPos.above();
            if (level.isEmptyBlock(above)) {
                candidate = above;
            }
        }

        if (candidate != null) {
            boolean success = TrainerAltarPattern.setDropPosition(level, altarPos, candidate);
            if (success) {
                // 清除设置状态
                configData.remove(SETTING_MODE_KEY);
                configData.remove(SETTING_ALTAR_POS_KEY);
                persistentData.put(CONFIG_KEY, configData);
                player.displayClientMessage(TextSignals.SUCCESS2.component(), true);
            } else {
                player.displayClientMessage(tr("hint.invalid_drop_pos_out_of_range_or_block_unavailable")
                                .withStyle(ChatFormatting.RED), true
                );
            }
        } else {
            player.displayClientMessage(tr("hint.target_block_must_absorb_block_or_air_above")
                    .withStyle(ChatFormatting.RED), true);
        }
        return InteractionResult.SUCCESS;

    }
}
