package com.pha.trainees.item;

import com.pha.trainees.util.interfaces.IChargeable;
import com.pha.trainees.util.interfaces.IHoverText;
import com.pha.trainees.util.interfaces.IKineticWeapon;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AuriversiteRapierItem extends SwordItem implements IChargeable, IKineticWeapon, IHoverText {
    public static final String TAG_KINETIC_UPDATE_ENABLED = "KineticUpdateEnabled";

    public AuriversiteRapierItem(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        float damage = applyKineticDamage(stack, target, attacker);
        applyParticle(stack, target, attacker, damage);

        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (!level.isClientSide) {
            if (IKineticWeapon.isKineticUpdateEnabled(stack)) {
                updateKineticEnergy(level, entity, stack, isSelected);
            }
        }
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!player.isCrouching()) {
            return charge(level, player, hand);
        }
        if (!level.isClientSide()) {
            changeEnabled(stack, player);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());

    }

    @Override
    public int getChargeCooldown() {
        return 15;
    }

    @Override
    public void appendHoverText(ItemStack stack, @javax.annotation.Nullable Level level, List<Component> tooltipComponents, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltipComponents, flag);

        if (shiftDown()) {
            boolean enabled = IKineticWeapon.isKineticUpdateEnabled(stack);
            String id = "auriversite_rapier";
            tooltipComponents.add(getTooltip(id));
            tooltipComponents.add(getTooltip(id, 2, 1));
            tooltipComponents.add(getTooltip(id, 2, 2));
            tooltipComponents.add(getTooltip(id, "state")
                    .append(tr(enabled ? ky(id, "on") : ky(id, "off"))
                            .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED)));
        } else {
            addTip(tooltipComponents);
        }
    }
}