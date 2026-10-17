package com.pha.trainees.chemistry.block;

import com.pha.trainees.chemistry.blockentity.ValveBlockEntity;
import com.pha.trainees.chemistry.container.IContainerPort;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 阀门（§19.28）：<b>让"密封"变成玩家可控的操作</b>。
 *
 * <p>把它装在容器"口"所在的那一格（烧杯的口朝上，所以就是放在烧杯正上方），
 * 右键切换通/闭：关 → 该面被封住（容器密闭，压强开始累积、T_b(P) 生效）；
 * 开 → 容器通过这一面与外界相通，<b>交换目标是阀门自己那一格</b>（随后由网格向外扩散）。</p>
 *
 * <p><b>为什么开/闭做成方块状态而不是方块实体</b>：状态自动进存档、自动同步客户端、无需 NBT、
 * 无需 tick，也不会有"方块实体丢失"这类事故。开关的判定入口只有
 * {@link IContainerPort#isPortPassable} 一处，容器每 tick 重新问一次，所以无需通知机制。</p>
 *
 * <p><b>默认关闭</b>（阀门语义）：放下去就是密封，右键才通气——这样"我把它封住了"是明确动作。</p>
 *
 * <p><b>2026-10-04 变更</b>：为了 §19.32 的"附着件腐蚀"，阀门改成 {@link BaseEntityBlock}。
 * 开/关**仍然是方块状态**（上面那条设计理由不变），方块实体只承载**腐蚀度**这一件
 * "跟着方块走的状态量"——这是"状态量"与"开关量"的正确分工。</p>
 */
public class ValveBlock extends BaseEntityBlock implements IContainerPort {

    /** 是否通气 */
    public static final BooleanProperty OPEN = BooleanProperty.create("open");

    public ValveBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new ValveBlockEntity(pos, state);
    }

    /** BaseEntityBlock 默认返回 INVISIBLE——不覆盖会把阀门渲染没了。 */
    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean isPortPassable(Level level, BlockPos portPos, Direction sideToContainer) {
        BlockState state = level.getBlockState(portPos);
        return state.getBlock() == this && state.getValue(OPEN);
    }

    @Override
    public @NotNull InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            // 客户端只回 SUCCESS 让手臂摆动；真正的翻转在服务端做
            return InteractionResult.SUCCESS;
        }
        boolean open = !state.getValue(OPEN);
        level.setBlock(pos, state.setValue(OPEN, open), Block.UPDATE_ALL);
        level.playSound(null, pos,
                open ? SoundEvents.IRON_TRAPDOOR_OPEN : SoundEvents.IRON_TRAPDOOR_CLOSE,
                SoundSource.BLOCKS, 0.6f, 1.0f);
        player.displayClientMessage(Component.translatable(
                open ? "message.trainees.valve.open" : "message.trainees.valve.closed"), true);
        return InteractionResult.CONSUME;
    }
}
