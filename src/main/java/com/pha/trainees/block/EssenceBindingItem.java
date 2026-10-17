package com.pha.trainees.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 精练精华网绑定器（§17.1 ④）：无线总线的"钥匙"。
 *
 * <p>用法（作者定的"用绑定物品"）：**右键任意方块**记录它的**维度 + 坐标**（写进物品 NBT），
 * 再拿它**右键无线总线**把目标写进总线；无线总线在**同维度、无视距离**地对该目标传输/抽取，
 * 只要求那个区块已加载。</p>
 */
public class EssenceBindingItem extends Item {

    private static final String NBT_DIM = "BindDim";
    private static final String NBT_POS = "BindPos";

    public EssenceBindingItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();

        if (!level.isClientSide) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putString(NBT_DIM, level.dimension().location().toString());
            tag.putLong(NBT_POS, pos.asLong());

            Player player = context.getPlayer();
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.trainees.binder.bound",
                        pos.getX(), pos.getY(), pos.getZ()), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    public static ResourceKey<Level> getBoundDimension(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_DIM)) return null;
        ResourceLocation id = ResourceLocation.tryParse(tag.getString(NBT_DIM));
        return id == null ? null : ResourceKey.create(Registries.DIMENSION, id);
    }

    @Nullable
    public static BlockPos getBoundPos(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null || !tag.contains(NBT_POS) ? null : BlockPos.of(tag.getLong(NBT_POS));
    }
}
