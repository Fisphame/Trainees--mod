package com.pha.trainees.block;

import com.pha.trainees.blockentity.AltarCoreBlockEntity;
import com.pha.trainees.config.ChemConfig;
import com.pha.trainees.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 祭坛主方块（§17.1）：魔力的载体，也是"增强层"配方的力量来源。
 *
 * <p>方块实体在 {@link AltarCoreBlockEntity}，魔力存里面。表现先走**粒子**（作者 2026-10-02：
 * "先粒子吧"）：越满越密、缓缓上浮的附魔粒子——一眼能看出"这个祭坛有魔"。模型/液面留到后续批次。</p>
 */
public class AltarCoreBlock extends BaseEntityBlock {

    /** 满魔时每 tick 的期望粒子数（实际按充满度缩放，空的一颗都不冒）。 */
    private static final float MAX_PARTICLES_PER_TICK = 3.0f;

    public AltarCoreBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AltarCoreBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * 服务端 ticker（§17.1）：只用来**周期性补算容量**。
     *
     * <p>容量要数相邻的缓存单元，而这件事在 {@code onLoad} 里做是危险的（区块加载中碰世界），
     * 所以推迟到 tick：载入后最多 1 秒内容量就正确了，交互路径上还会再刷一次。</p>
     */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof AltarCoreBlockEntity core) core.serverTick();
        };
    }

    /**
     * 右键 = 查看当前存量。
     *
     * <p>作者 2026-10-04 决定：**"献祭"玩法整套删除**，RE 只从**提取机**（数据驱动配方）出来。
     * 于是祭坛退回它本来的职责——大容量缓存 + 缓存单元宿主，不再接受物品换 RE。</p>
     */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof AltarCoreBlockEntity core)) return InteractionResult.PASS;

        // 交互路径上先补一次容量（区块此刻一定就绪），免得"刚放好缓存单元就读到旧容量"
        core.refreshCapacity();
        player.displayClientMessage(Component.translatable("message.trainees.altar.mana_info",
                core.getManaStored(), core.getManaCapacity()), true);
        return InteractionResult.CONSUME;
    }

    /**
     * 客户端粒子：只读同步过来的存量（{@code getUpdateTag} 里带 Mana），
     * 所以空祭坛完全不冒粒子、满祭坛明显"冒气"。
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof AltarCoreBlockEntity core)) return;
        float fill = core.getManaFillRatio();
        if (fill <= 0.001f) return;

        // 期望值 + 小数部分掷骰，避免"刚好满的时候恒定几颗"这种死板感
        float expected = fill * MAX_PARTICLES_PER_TICK;
        int count = (int) expected;
        if (random.nextFloat() < expected - count) count++;

        for (int i = 0; i < count; i++) {
            double x = pos.getX() + 0.15 + random.nextDouble() * 0.7;
            double y = pos.getY() + 1.0 + random.nextDouble() * 0.35;
            double z = pos.getZ() + 0.15 + random.nextDouble() * 0.7;
            double vx = (random.nextDouble() - 0.5) * 0.02;
            double vy = 0.015 + random.nextDouble() * 0.03;
            double vz = (random.nextDouble() - 0.5) * 0.02;
            level.addParticle(ParticleTypes.ENCHANT, x, y, z, vx, vy, vz);
        }

        // 满得比较厉害时偶尔来一颗亮一点的（每 tick 1/8 概率）
        if (fill > 0.75f && random.nextInt(8) == 0) {
            level.addParticle(ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5,
                    0.0, 0.01, 0.0);
        }
    }
}
