package com.pha.trainees.item;

import com.pha.trainees.util.interfaces.IHoverText;
import com.pha.trainees.util.interfaces.IScythe;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class ScytheCourseItem{
    public static abstract class BaseScytheItem extends SwordItem implements IScythe, IHoverText {
        private final int tier;
        private final String itemId;

        public BaseScytheItem(Tier tier, int attackDamage, float attackSpeed,
                              Properties properties, int tierIndex, String itemId) {
            super(tier, attackDamage, attackSpeed, properties);
            this.tier = tierIndex;
            this.itemId = itemId;
        }

        @Override
        public int getTierIndex() {
            return tier;
        }

        @Override
        public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            return onHurtEnemy(stack, target, attacker, tier) &&
                    super.hurtEnemy(stack, target, attacker);
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level,
                                    List<Component> tooltipComponents, TooltipFlag flag) {
            super.appendHoverText(stack, level, tooltipComponents, flag);
            addHoverText(stack, level, tooltipComponents, flag, itemId);
        }
    }

    public static class ScytheItem extends BaseScytheItem {
        public ScytheItem(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
            super(tier, attackDamage, attackSpeed, properties, 0, "scythe_item");
        }
    }

    public static class CompoundScytheItem extends BaseScytheItem {
        public CompoundScytheItem(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
            super(tier, attackDamage, attackSpeed, properties, 1, "compound_scythe_item");
        }
    }

    public static class BlackPowderScytheItem extends BaseScytheItem {
        public BlackPowderScytheItem(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
            super(tier, attackDamage, attackSpeed, properties, 2, "black_powder_scythe_item");
        }
    }
}
