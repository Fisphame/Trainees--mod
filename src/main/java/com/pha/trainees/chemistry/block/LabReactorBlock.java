package com.pha.trainees.chemistry.block;

import com.pha.trainees.chemistry.blockentity.BeakerBlockEntity;
import com.pha.trainees.chemistry.blockentity.LabReactorBlockEntity;
import com.pha.trainees.chemistry.container.ContainerMaterial;
import com.pha.trainees.registry.ModChemistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 实验室反应釜方块（§19.30 方案 1）：单方块宿主，<b>10 L</b>，四档材质。
 *
 * <p>继承 {@link BeakerBlock} 是刻意的：投料、取样、测厚、流体接口、材料档位全部照用同一套，
 * 所以"反应釜 = 更大的烧杯"在代码里就是继承关系，而不是复制一份容器逻辑
 * （先例：{@code ElectrolysisCellBlockEntity extends BeakerBlockEntity}）。</p>
 *
 * <p>它比烧杯多出来的东西只有三样，都在 BE 里：
 * ① 容积 10 L；② 端口可由阀门"开孔"（见 {@code portFaces()}）；③ 尺寸放大效应（传热 ∝ V^(−1/3)）。</p>
 */
public class LabReactorBlock extends BeakerBlock {

    /** 釜体轮廓：比烧杯更大更方（10 L 的视觉暗示；正式美术会换成专有模型） */
    private static final VoxelShape SHAPE = Shapes.box(0.0625, 0.0, 0.0625, 0.9375, 0.9375, 0.9375);

    public LabReactorBlock(Properties properties, ContainerMaterial material) {
        super(properties, material);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level,
                                        @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new LabReactorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state,
                                                                  @NotNull BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        // 釜沿用烧杯的 tick 逻辑（引擎 + 换热 + 逐面气体交换），只是 BE 类型不同
        return createTickerHelper(type, ModChemistry.ModChemistryBlockEntities.LAB_REACTOR.get(),
                BeakerBlockEntity::tick);
    }

    // ==================== 交互：密封釜头 + 清洗（§19.30.2） ====================

    /**
     * 比烧杯多一个动作：手持<b>密封釜头</b>右键 = 装上（全焊态）。
     *
     * <p>⚠ 拆下釜头改用<b>扳手</b>、清洗改用<b>清洁刷</b>（§19.33）——这两件事都是明确的意图，
     * 不该塞进"潜行 + 右键"这种容易与其它交互混淆的手势里。
     * 其余交互（投料/取样/桶/分析仪）全部交回 {@link BeakerBlock#use}。</p>
     */
    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                          @NotNull Player player, @NotNull InteractionHand hand,
                                          @NotNull BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof LabReactorBlockEntity reactor)) {
            return super.use(state, level, pos, player, hand, hit);
        }
        ItemStack held = player.getItemInHand(hand);

        if (held.is(ModChemistry.ModChemistryItems.SEALED_HEAD.get())) {
            if (reactor.hasSealedHead()) {
                player.displayClientMessage(Component.translatable(
                        "message.trainees.reactor.head_already"), true);
                return InteractionResult.CONSUME;
            }
            reactor.installHead(held);
            if (!player.isCreative()) {
                held.shrink(1);
            }
            player.displayClientMessage(Component.translatable(
                    "message.trainees.reactor.head_on"), true);
            return InteractionResult.CONSUME;
        }

        return super.use(state, level, pos, player, hand, hit);
    }

    /** 挖掉釜时把密封釜头一起吐出来（掉落不销毁）。 */
    @Override
    public void playerDestroy(@NotNull Level level, @NotNull Player player, @NotNull BlockPos pos,
                              @NotNull BlockState state, @Nullable BlockEntity blockEntity,
                              @NotNull ItemStack tool) {
        if (blockEntity instanceof LabReactorBlockEntity reactor) {
            ItemStack head = reactor.removeHead();
            if (!head.isEmpty()) {
                popResource(level, pos, head);
            }
        }
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }
}
