package com.pha.trainees.util.game.chemistry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/**
 * 催化剂条件。
 * 需要机械方块的催化槽中含有指定的物品，并且反应完成时有概率消耗。
 */
public class CatalystCondition implements MachineCondition {

    private final List<ItemStack> requiredCatalysts; // 需要的催化剂列表（每个物品可能指定数量）
    private final double consumeChance;               // 消耗概率（0~1），反应完成时触发

    public CatalystCondition(List<ItemStack> requiredCatalysts, double consumeChance) {
        this.requiredCatalysts = requiredCatalysts;
        this.consumeChance = consumeChance;
    }

    @Override
    public boolean test(BlockEntity machine, Level level, BlockPos pos) {
        // 假设机械方块有 getCatalystInventory() 方法返回一个容器（如 IItemHandler）
        // 这里只是示例，实际需要根据你的机械方块实现来编写检查逻辑
        // 暂返回 true，表示条件满足（由你后续实现）
        return true;
    }

    @Override
    public void onReactionStart(BlockEntity machine, Level level, BlockPos pos) {
        // 反应开始时，按概率消耗催化剂
        if (level.random.nextDouble() < consumeChance) {
            // 从催化槽中扣除 requiredCatalysts 对应的物品
            // 实现取决于你的机器实体
        }
    }

    public List<ItemStack> getRequiredCatalysts() {
        return requiredCatalysts;
    }

    public double getConsumeChance() {
        return consumeChance;
    }
}
