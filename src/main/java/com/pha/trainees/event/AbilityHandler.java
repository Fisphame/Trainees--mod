package com.pha.trainees.event;

import com.pha.trainees.entity.CalledSwordEntity;
import com.pha.trainees.registry.ModEnchantments;
import com.pha.trainees.registry.ModEntities;
import com.pha.trainees.util.game.CommandTools;
import com.pha.trainees.util.game.ItemClassifier;
import com.pha.trainees.util.game.Tools;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class AbilityHandler {

    private static final int COOLDOWN_TICKS = 10 * 20;
    private static final int CREATIVE_COOLDOWN_TICKS = 10;
    private static final int HURT_BREAK = 5;

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Level level = event.getLevel();
        Player player = event.getEntity();
        ItemStack stack = player.getMainHandItem();

        // 检查是否持有剑且附魔存在
        if (!(stack.getItem() instanceof SwordItem) || stack.getEnchantmentLevel(ModEnchantments.ten_thousand_sword.get()) <= 0) {
            return;
        }

        Item item = stack.getItem();
        int degree = stack.getEnchantmentLevel(ModEnchantments.ten_thousand_sword.get());
        float basicDamage = ((SwordItem) stack.getItem()).getDamage();
        float enchantDamage = EnchantmentHelper.getDamageBonus(stack, MobType.UNDEFINED);
        // 阵列参数（方法内局部变量，避免多玩家并发互相覆盖）
        double baseDistance;
        double arrayRadius;
        double verticalSpread;
        if (ItemClassifier.scythe(item)){
            basicDamage *= 2;
            degree += 1;
            baseDistance = 2.0;
            arrayRadius = 3.5;
            verticalSpread = 0.5;
        }
        else if (ItemClassifier.repair(item)){
            basicDamage /= 2;
            degree -= 1;
            baseDistance = 0.5;
            arrayRadius = 1.5;
            verticalSpread = 1.5;
        }
        else if (ItemClassifier.kunSword(item)){
            degree += 1;
            baseDistance = 3.0;
            arrayRadius = 2.5;
            verticalSpread = 2.0;
        }
        else {
            baseDistance = 3.0;
            arrayRadius = 2.5;
            verticalSpread = 1.5;
        }
        float damage = basicDamage + enchantDamage * 2;
        if (player.getCooldowns().isOnCooldown(item)){
            player.displayClientMessage(
                    Component.translatable("message.trainees.ability.cooldown").withStyle(ChatFormatting.RED),
                    true
            );
            return;
        }
        level.playSound(null, new BlockPos(player.getBlockX(), player.getBlockY(), player.getBlockZ()),
                SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.5F, 1.0F);

        // 触发技能
        activateAbility(player, damage, degree, level, baseDistance, arrayRadius, verticalSpread);

        if (!ItemClassifier.repair(item)) {
            if (player.isCreative()) {
                player.getCooldowns().addCooldown(item, CREATIVE_COOLDOWN_TICKS);
            }
            else if (!player.isCreative()) {
                player.getCooldowns().addCooldown(item, COOLDOWN_TICKS);
            }
        }
        if (!player.isCreative()) {
            stack.hurtAndBreak(HURT_BREAK, player, (e) -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        }
    }

    private static void activateAbility(Player player, float damage, int degree, Level level,
                                        double baseDistance, double arrayRadius, double verticalSpread) {
        spawnSwords(player, degree, damage, level, baseDistance, arrayRadius, verticalSpread);
    }

    private static void spawnSwords(Player player, int degree, float damage, Level level,
                                    double baseDistance, double arrayRadius, double verticalSpread) {
        // 获取玩家视线方向（水平方向）
        Vec3 lookVec = player.getLookAngle();
        Vec3 forward = new Vec3(lookVec.x, 0, lookVec.z).normalize();

        // 如果视线完全垂直（比如看天或地），使用玩家的朝向
        if (forward.lengthSqr() < 0.001) {
            forward = Vec3.atLowerCornerOf(player.getDirection().getNormal());
        }

        // 计算右向量和上向量
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = forward.cross(up).normalize();

        // 计算基础位置（玩家位置向前推baseDistance距离）
        Vec3 playerPos = player.position().add(0, player.getEyeHeight() * 0.7, 0);
        Vec3 basePos = playerPos.add(forward.scale(baseDistance));

        // 计算伤害
        float re_damage = damage * 2.0f;

        // 计算要生成的剑数量
        int totalSwords = degree * 4;

        // 生成阵列
        for (int i = 0; i < totalSwords; i++) {
            // 计算剑在阵列中的位置
            Vec3 swordPos;
            if (player.isShiftKeyDown()){
                swordPos = calculateSwordPosition(basePos, forward, right, up, i, totalSwords, arrayRadius, verticalSpread);
            }
            else {
                swordPos = calculateSwordPositionAlternative(basePos, forward, right, up, i, totalSwords, arrayRadius);
            }

            // 生成剑实体
            CalledSwordEntity swordEntity = new CalledSwordEntity(
                    ModEntities.CALLED_SWORD.get(),
                    player.level()
            );

            // 设置参数
            swordEntity.setDamage(re_damage);
            swordEntity.setMoveDirection(forward); // 所有剑都沿视线方向移动
            swordEntity.setPos(swordPos.x, swordPos.y, swordPos.z);
            swordEntity.setOffPlayer(player);
            swordEntity.setEnchantBreakBlock(CommandTools.getEnchantBreakBlock(level));
            swordEntity.setEnchantBrokenBlockDrop(CommandTools.getEnchantBrokenBlockDrop(level));
            player.level().addFreshEntity(swordEntity);
        }
    }

    /**
     * 计算单个剑在阵列中的位置
     *
     * @param basePos   阵列基础位置
     * @param forward   前进方向
     * @param right     右方向
     * @param up        上方向
     * @param index     剑的索引
     * @param total     总剑数
     * @return          剑的位置
     */
    private static Vec3 calculateSwordPosition(Vec3 basePos, Vec3 forward, Vec3 right, Vec3 up,
                                               int index, int total, double arrayRadius, double verticalSpread) {
        // 计算阵列布局（弧形/圆形阵列）

        // 1. 计算角度（0到2π）
        double angle = (2.0 * Math.PI * index) / total;

        // 2. 计算在阵列平面上的偏移
        double radius = arrayRadius;
        double xOffset = Math.cos(angle) * radius;
        double zOffset = Math.sin(angle) * radius;

        // 3. 计算垂直偏移（形成立体阵列）
        double yOffset = Math.sin(angle * 2) * verticalSpread;

        // 4. 将偏移应用到局部坐标系
        //    注意：这里我们需要将(xOffset, zOffset)映射到(right, forward)平面上
        //    但通常阵列应该垂直于视线方向，所以我们要旋转一下
        //    简单起见，我们直接使用极坐标的x和z作为两个方向的偏移

        // 创建一个旋转矩阵，使阵列始终面向玩家视线方向
        // 这里简化处理：阵列围绕视线方向旋转
        Vec3 position = basePos
                .add(right.scale(xOffset))      // 左右偏移
                .add(up.scale(yOffset))         // 上下偏移
                .add(forward.scale(zOffset * 0.3)); // 前后少量偏移，形成层次感

        return position;
    }

    /**
     * 替代方案：保持原阵列形状但沿视线方向移动
     */
    private static Vec3 calculateSwordPositionAlternative(Vec3 basePos, Vec3 forward, Vec3 right,
                                                          Vec3 up, int index, int total, double arrayRadius) {
        // 这是原阵列的简化版本，保持类似形状但沿视线方向展开

        int pairIndex = index / 2;
        double offset = pairIndex * 0.5;
        double yOffset = 0;

        // 计算左右偏移（原阵列的sideOffset）
        double sideOffset = (index % 2 == 0) ? -offset : offset;
        sideOffset *= (arrayRadius / 2);

        // 在垂直于forward的平面上布置阵列
        Vec3 position = basePos
                .add(right.scale(sideOffset))                // 左右展开
                .add(up.scale(yOffset))                     // 垂直方向
                .add(forward.scale(-offset * 0.5));         // 前后错开

        return position;
    }
}