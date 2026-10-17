package com.pha.trainees.item;

import com.pha.trainees.util.interfaces.IEggs;
import com.pha.trainees.util.interfaces.IHoverText;
import com.pha.trainees.registry.ModEntities;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;


import javax.annotation.Nullable;
import java.util.List;

public class EggCourseItem {
    public static abstract class BaseEggItem extends EggItem implements IEggs, IHoverText {
        private final int whichEgg;
        private final String itemId;

        public BaseEggItem(Properties properties, int whichEgg, String itemId) {
            super(properties);
            this.whichEgg = whichEgg;
            this.itemId = itemId;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            return useEgg(level, player, hand, this, whichEgg);
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag flag) {
            super.appendHoverText(stack, level, tooltipComponents, flag);
            addHoverText(stack, level, tooltipComponents, flag, itemId);
        }
    }

    public abstract static class BaseThrownEggItem extends ThrownEgg implements IEggs {
        private final Entity targetEntity;

        public BaseThrownEggItem(Level level, Player player, Entity targetEntity) {
            super(level, player);
            this.targetEntity = targetEntity;
        }

        @Override
        protected void onHit(@NotNull HitResult hitResult) {
            super.onHit(hitResult);
            hit(this, targetEntity);
        }
    }

    public static class KunEggItem extends BaseEggItem {

        public KunEggItem(Properties properties) {
            super(properties, 1, "kun_egg_item");
        }
    }

    public static class BlackEggItem extends BaseEggItem {

        public BlackEggItem(Properties properties) {
            super(properties, 2, "black_egg_item");
        }
    }

    public static class GoldEggItem extends BaseEggItem {

        public GoldEggItem(Properties properties) {
            super(properties, 3, "gold_egg_item");
        }
    }

    public static class ThrownKunEgg extends BaseThrownEggItem {

        public ThrownKunEgg(Level level, Player player) {
            super(level, player, ModEntities.KUN_TRAINEES.get().create(level));
        }
    }

    public static class ThrownBlackEgg extends BaseThrownEggItem {

        public ThrownBlackEgg(Level level, Player player) {
            super(level, player, ModEntities.KUN_ANTI.get().create(level));
        }
    }

    public static class ThrownGoldEgg extends BaseThrownEggItem {

        public ThrownGoldEgg(Level level, Player player) {
            super(level, player, ModEntities.GOLD_CHICKEN.get().create(level));
        }
    }
}
