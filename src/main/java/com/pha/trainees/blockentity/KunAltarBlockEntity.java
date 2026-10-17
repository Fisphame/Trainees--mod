package com.pha.trainees.blockentity;

import com.pha.trainees.block.KunAltarBlock;
import com.pha.trainees.registry.ModBlocks;
import com.pha.trainees.registry.ModItems;
import com.pha.trainees.util.game.enums.KunAltarType;
import com.pha.trainees.util.game.ParticleHelper;
import com.pha.trainees.util.game.Tools;
import com.pha.trainees.util.interfaces.IMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class KunAltarBlockEntity extends ItemHandlerBlockEntity implements IMachine {

    private KunAltarType altarType = KunAltarType.COMPLETE;

    public KunAltarBlockEntity(BlockEntityType<?> p_155228_, BlockPos p_155229_, BlockState p_155230_) {
        super(p_155228_, p_155229_, p_155230_);
    }

    @Override
    public int getLimit() {
        return 1;
    }

    public KunAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.ModBlockEntities.KUN_ALTAR_ENTITY.get(), pos, state);
    }

    // 处理玩家交互
    public InteractionResult handleInteraction(Player player, InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);
        ItemStack storedItem = itemHandler.getStackInSlot(0);

        if (heldItem.getItem() == ModItems.STONE_STICK.get() && !player.isShiftKeyDown()) {
            // 切换祭坛类型
            toggleAltarType();

            // 同步数据到客户端
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }

            return InteractionResult.SUCCESS;
        }

        // 潜行+空手：取出物品
        if (!storedItem.isEmpty() && player.isShiftKeyDown() && player.getUseItem().isEmpty()) {
            player.getInventory().add(storedItem);
            itemHandler.setStackInSlot(0, ItemStack.EMPTY);
            return InteractionResult.SUCCESS;
        }

        if (heldItem.getItem() == storedItem.getItem()){
            return InteractionResult.CONSUME;
        }

        if (!storedItem.isEmpty()) {
            // 如果方块中已有物品，先将其掉落
            dropStoredItem();
        }

        if (!heldItem.isEmpty()) {
            // 存储玩家手中的一个物品
            ItemStack toStore = heldItem.copyWithCount(1);
            itemHandler.setStackInSlot(0, toStore);

            // 减少玩家手中的物品数量
            if (!player.isCreative()) {
                heldItem.shrink(1);
            }

            if (level != null && !level.isClientSide()) {
                level.playSound(null, worldPosition,
                        SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 0.5F, 1.0F
                );
            }

            if (level != null) {
                ParticleHelper.send(
                        level, ParticleTypes.SOUL_FIRE_FLAME, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                        5, 0.3, 0.3, 0.3, 0.1
                );
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    // 切换祭坛类型
    public void toggleAltarType() {
        setAltarType(altarType.next());

        // 播放切换音效
        if (level != null && !level.isClientSide()) {
            level.playSound(null, worldPosition,
                    SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 0.5F, 1.0F
            );
        }

        if (level != null) {
            ParticleHelper.send(
                    level, ParticleTypes.SOUL_FIRE_FLAME,
                    worldPosition.getX() + 0.5, worldPosition.getY() + 0.2, worldPosition.getZ() + 0.5,
                    7, 0.5, 0.2, 0.5, 0.1
            );
        }
    }

    // 获取当前祭坛类型
    public KunAltarType getAltarType() {
        return altarType;
    }

    // 设置祭坛类型
    public void setAltarType(KunAltarType type) {
        this.altarType = type;
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = level.getBlockState(worldPosition);
            // 更新方块状态中的 TYPE 属性，flag 为 3 表示通知客户端并更新形状
            level.setBlock(worldPosition, state.setValue(KunAltarBlock.TYPE, type), 3);
        }
    }

    // NBT数据保存/加载
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("AltarType", altarType.getSerializedName());
    }


    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        KunAltarType oldType = this.altarType;
        if (tag.contains("AltarType")) {
            String typeName = tag.getString("AltarType");
            // 解析类型（兼容新旧格式）
            altarType = parseAltarType(typeName);
        }
        // 如果类型变化且服务端，更新 blockstate
        if (level != null && !level.isClientSide() && oldType != altarType) {
            BlockState state = level.getBlockState(worldPosition);
            level.setBlock(worldPosition, state.setValue(KunAltarBlock.TYPE, altarType), 3);
        }
    }

    private KunAltarType parseAltarType(String name) {
        for (KunAltarType t : KunAltarType.values()) {
            if (t.getSerializedName().equals(name) || t.name().equals(name)) {
                return t;
            }
        }
        return KunAltarType.COMPLETE; // 默认
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        handleUpdateTag(pkt.getTag());
    }

    // 获取存储的物品（供渲染器使用）
    public ItemStack getDisplayItem() {
        return getStoredItem();
    }

    @Override
    public int getComparatorOutput() {
        return hasStoredItem() ? 15 : 0;
    }

    @Override
    public void setStoredItem(ItemStack stack) {
        if (!stack.isEmpty() && stack.getCount() > 1) {
            stack = stack.copyWithCount(1);
        }
        super.setStoredItem(stack);
    }
}
