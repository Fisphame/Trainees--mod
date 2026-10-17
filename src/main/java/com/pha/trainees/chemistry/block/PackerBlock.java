package com.pha.trainees.chemistry.block;

import com.pha.trainees.chemistry.blockentity.PackerBlockEntity;
import com.pha.trainees.chemistry.util.AnalyzerAccess;
import com.pha.trainees.network.ModNetwork;
import com.pha.trainees.registry.ModChemistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * 打包机（§19.18 日化品 / Step ④ + §19.25 自动化）。
 *
 * <p><b>定位</b>：标准驱动（spec-driven）的**质检 + 灌装**，不产生新化学。它读取**正上方容器**
 * （烧杯/坩埚/电解槽，任何 {@code IChemicalContainer}）里的溶液，按目标产品规格判定后灌装成瓶。</p>
 *
 * <p><b>现在会自己干活了</b>：方块实体每 N tick 自动尝试打包一次（auto 模式，不合格不产不扣），
 * 产物进**输出槽**；输出槽通过 {@code IItemHandler} 对 ME 网络/漏斗/管道可提取，FE 通过
 * {@code IEnergyStorage} 可充入（每瓶固定电费，默认 20 kJ）。于是自动输入输出成立：
 * 外部只管**给上方容器补料**、**从输出槽取货**，中间的"溶液"从不离开容器（§19.25 路线 1）。</p>
 *
 * <p><b>交互</b>：</p>
 * <ul>
 *   <li>空手右键：**打开面板**（§19.18.5）——「取出产物」已移进面板按钮，避免两种入口语义重叠；</li>
 *   <li>潜行空手右键：**人工强制灌装一次**（可产危险次品，使用时反噬）——自驱永远是 auto，绝不强制；</li>
 *   <li>手持物品右键：不拦截（交给物品自己处理）；</li>
 *   <li>分析仪右键：诊断面板（优先级最高）。</li>
 * </ul>
 */
public class PackerBlock extends BaseEntityBlock {

    public PackerBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PackerBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModChemistry.ModChemistryBlockEntities.PACKER.get(),
                PackerBlockEntity::tick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof PackerBlockEntity packer)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        InteractionResult analyzerResult = AnalyzerAccess.tryUse(level, pos, player, held);
        if (analyzerResult != InteractionResult.PASS) {
            return analyzerResult;
        }

        // 手持物品：不拦截，避免抢掉别的模组的物品交互
        if (!held.isEmpty()) return InteractionResult.PASS;

        if (player.isShiftKeyDown()) {
            // 人工强制灌装：唯一能产出危险次品的入口（熟手快捷方式，不开面板）
            packer.packOnce(level, pos, true, player);
            return InteractionResult.CONSUME;
        }

        // 空手右键 = 打开面板（§19.18.5）：把当前状态发给该玩家，客户端据此开屏。
        // 「取出产物」已移进面板按钮；这里不再直接取物，避免两种入口语义重叠。
        if (player instanceof ServerPlayer serverPlayer) {
            ModNetwork.get().send(PacketDistributor.PLAYER.with(() -> serverPlayer), packer.buildStatePacket());
        }
        return InteractionResult.CONSUME;
    }
}
