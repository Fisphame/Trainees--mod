package com.pha.trainees.block;

import com.pha.trainees.blockentity.EssenceBusBlockEntity;
import com.pha.trainees.registry.ModEssenceItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 精练精华网总线（§17.1 ④）：四类总线共用这一个方块类，靠 {@link Mode} + {@code wireless} 区分。
 *
 * <p>它们都是**完整方块**（作者强调），并且必须是端口多方块结构的一部分（在结构 tag 里）才会工作
 * ——端口每次刷新会把结构里的总线"认领"（回填主端口坐标），总线自己不去搜端口。</p>
 *
 * <p><b>无线总线</b>的绑定方式（作者定的"用绑定物品"）：先用绑定器右键**目标方块**记录维度 + 坐标，
 * 再拿绑定器右键**无线总线**把目标写进总线；潜行 + 空手右键可清除绑定。</p>
 */
public class EssenceBusBlock extends BaseEntityBlock {

    /** 总线方向：输入 = 往端口里搬；输出 = 从端口往外搬。 */
    public enum Mode {
        INPUT,
        OUTPUT
    }

    private final Mode mode;
    private final boolean wireless;

    public EssenceBusBlock(Mode mode, boolean wireless, Properties properties) {
        super(properties);
        this.mode = mode;
        this.wireless = wireless;
    }

    public Mode getMode() {
        return mode;
    }

    public boolean isWireless() {
        return wireless;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EssenceBusBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof EssenceBusBlockEntity bus) bus.serverTick();
        };
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof EssenceBusBlockEntity bus)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);

        // 潜行 + 空手：清除绑定
        if (wireless && player.isShiftKeyDown() && held.isEmpty()) {
            bus.clearBinding();
            player.displayClientMessage(Component.translatable("message.trainees.bus.unbound"), true);
            return InteractionResult.CONSUME;
        }

        // 拿绑定器右键无线总线：把绑定器里记的目标写进总线
        if (wireless && held.is(ModEssenceItems.ESSENCE_BINDING_ITEM.get())) {
            var dimension = EssenceBindingItem.getBoundDimension(held);
            BlockPos target = EssenceBindingItem.getBoundPos(held);
            if (dimension == null || target == null) {
                player.displayClientMessage(Component.translatable("message.trainees.bus.binder_empty"), true);
                return InteractionResult.CONSUME;
            }
            if (!dimension.equals(level.dimension())) {
                player.displayClientMessage(Component.translatable("message.trainees.bus.wrong_dimension"), true);
                return InteractionResult.CONSUME;
            }
            bus.bind(dimension, target);
            player.displayClientMessage(Component.translatable("message.trainees.bus.bound",
                    target.getX(), target.getY(), target.getZ()), true);
            return InteractionResult.CONSUME;
        }

        // 有线总线：右键给个状态提示（免得玩家以为坏了）
        if (!wireless) {
            player.displayClientMessage(Component.translatable(bus.getHostPort() == null
                    ? "message.trainees.bus.no_port" : "message.trainees.bus.linked"), true);
        }
        return InteractionResult.CONSUME;
    }
}
