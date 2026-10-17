package com.pha.trainees.block;

import com.pha.trainees.blockentity.KunAltarBlockEntity;
import com.pha.trainees.util.game.enums.KunAltarType;
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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;



public class KunAltarBlock extends BaseEntityBlock implements IMachine {

    public static final EnumProperty<KunAltarType> TYPE = EnumProperty.create("type", KunAltarType.class);

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TYPE);
    }

    public KunAltarBlock(Properties p_49224_) {
        super(p_49224_);
        this.registerDefaultState(this.stateDefinition.any().setValue(TYPE, KunAltarType.COMPLETE));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KunAltarBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof KunAltarBlockEntity blockEntity) {
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

        if (blockEntity instanceof KunAltarBlockEntity kunAltarEntity) {
            ItemStack storedItem = kunAltarEntity.getStoredItem();
            if (!storedItem.isEmpty()){
                if (silkTouch) {
                    // 使用精准采集时，将方块实体数据保存到掉落物中
                    ItemStack itemStack = new ItemStack(this);

                    // 将方块实体数据保存到ItemStack的NBT中
                    CompoundTag tag = kunAltarEntity.saveWithoutMetadata();
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
        if (tag != null && level.getBlockEntity(pos) instanceof KunAltarBlockEntity ka) {
            ka.load(tag);
        }
    }

    @Override
    public @NotNull RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(TYPE).getShape();
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public @NotNull VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    public static KunAltarType getKunAltarType(BlockGetter level, BlockPos pos){
        if (level.getBlockEntity(pos) instanceof KunAltarBlockEntity blockEntity) {
            return blockEntity.getAltarType();
        }
        return KunAltarType.COMPLETE;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true; // 声明此方块可输出比较器信号
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof KunAltarBlockEntity entity) {
            return entity.getComparatorOutput();
        }
        return 0;
    }
}
