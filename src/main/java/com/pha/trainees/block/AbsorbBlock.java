package com.pha.trainees.block;

import com.pha.trainees.blockentity.AbsorbBlockEntity;
import com.pha.trainees.registry.ModBlocks;
import com.pha.trainees.util.game.EntityWay;
import com.pha.trainees.util.game.Tools;
import com.pha.trainees.util.interfaces.IMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AbsorbBlock extends BaseEntityBlock implements IMachine {
    public AbsorbBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AbsorbBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof AbsorbBlockEntity blockEntity) {
            // 处理物品交互
            return blockEntity.handleInteraction(player, hand);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos,
                              BlockState state, @Nullable BlockEntity blockEntity,
                              ItemStack tool) {
        if (level.isClientSide()) return;
        // 检查是否使用精准采集
        boolean silkTouch = tool.getEnchantmentLevel(Enchantments.SILK_TOUCH) > 0;

        if (blockEntity instanceof AbsorbBlockEntity absorbBlockEntity) {
            ItemStack storedItem = absorbBlockEntity.getStoredItem();
            if (!storedItem.isEmpty()){
                if (silkTouch) {
                    // 使用精准采集时，将方块实体数据保存到掉落物中
                    ItemStack itemStack = new ItemStack(this);

                    // 将方块实体数据保存到ItemStack的NBT中
                    CompoundTag tag = absorbBlockEntity.saveWithoutMetadata();
                    if (!tag.isEmpty()) {
                        itemStack.addTagElement("BlockEntityTag", tag);
                    }
                    EntityWay.spawnItemEntity(level, pos, itemStack);

                    return;
                }
                EntityWay.spawnItemEntity(level, pos, storedItem.copy());
            }
        }
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;
        CompoundTag tag = stack.getTagElement("BlockEntityTag");
        if (tag != null && level.getBlockEntity(pos) instanceof AbsorbBlockEntity be) {
            be.load(tag);
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // 只在服务端执行 tick；如果客户端也需要（如粒子动画），可以两端都返回
        if (level.isClientSide) {
            return null;
        }

        // 类型匹配：确保 ticker 只附加到正确的 BlockEntityType
        return type == ModBlocks.ModBlockEntities.ABSORB_BLOCK_ENTITY.get() ? AbsorbBlockEntity::tick : null;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true; // 声明此方块可输出比较器信号
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof AbsorbBlockEntity absorbEntity) {
            return absorbEntity.getComparatorOutput();
        }
        return 0;
    }

    @Override
    public @NotNull RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
