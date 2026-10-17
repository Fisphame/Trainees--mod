package com.pha.trainees.chemistry.item;

import com.pha.trainees.chemistry.product.PackOutcome;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Locale;

/**
 * 日化品次品（§19.18 / Step ④-4 行为钩子）。
 *
 * <p>次品的**反噬取决于违规种类**，而不是"它是哪种产品"——所以按失败类别共用物品、
 * 每个类别一个效果：游离氯 → 放毒（中毒）；碱超标 → 化学烧伤；酸超标 → 自毁放气（伤害 + 直接消耗）；
 * 浓度不符 → 无危害，只是没用。这正是"质检从门槛升级为安全机制"的落点。</p>
 *
 * <p>创造模式不消耗物品（但提示照给），避免调试时被吃掉。</p>
 */
public class DefectiveProductItem extends Item {

    private final PackOutcome.DefectKind kind;

    public DefectiveProductItem(PackOutcome.DefectKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public PackOutcome.DefectKind getDefectKind() {
        return kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        boolean creative = player.getAbilities().instabuild;
        switch (kind) {
            case CHLORINE -> {
                // 游离氯：放毒（次氯酸盐 + 酸 → Cl₂ 的经典事故）
                player.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 0));
                if (!creative) player.hurt(player.damageSources().generic(), 1.0F);
            }
            case ALKALI -> {
                // 碱超标：化学烧伤
                if (!creative) player.hurt(player.damageSources().generic(), 3.0F);
            }
            case ACID -> {
                // 酸超标：自毁放气（物品直接报销）
                if (!creative) player.hurt(player.damageSources().generic(), 2.0F);
            }
            case CHLORATE -> {
                // 氯酸盐：氧化性杂质，误服中毒（高铁血红蛋白血症）
                player.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
            }
            default -> {
                // 只有危险类别会被实例化：浓度不符不灌装（§19.18 决策），因此不存在对应物品
            }
        }

        player.displayClientMessage(Component.translatable(
                "message.trainees.defective." + kind.name().toLowerCase(Locale.ROOT)), true);
        if (!creative) {
            stack.shrink(1);
        }
        return InteractionResultHolder.consume(stack);
    }
}
