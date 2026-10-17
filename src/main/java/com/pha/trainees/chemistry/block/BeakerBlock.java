package com.pha.trainees.chemistry.block;

import com.pha.trainees.Main;
import com.pha.trainees.chemistry.blockentity.BeakerBlockEntity;
import com.pha.trainees.chemistry.item.SubstanceItem;
import com.pha.trainees.chemistry.particle.IonType;
import com.pha.trainees.chemistry.particle.Phase;
import com.pha.trainees.chemistry.util.AnalyzerAccess;
import com.pha.trainees.chemistry.util.FluidIonMapper;
import com.pha.trainees.chemistry.util.IonDisplay;
import com.pha.trainees.chemistry.util.SolidIonMapper;
import com.pha.trainees.config.ChemConfig;
import com.pha.trainees.registry.ModChemistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class BeakerBlock extends BaseEntityBlock implements com.pha.trainees.chemistry.container.IContainerMaterialProvider {

    private static final VoxelShape SHAPE = Shapes.box(0.125, 0.0, 0.125,
            0.875, 0.75, 0.875);
    private static final DecimalFormat DF = new DecimalFormat("#0.000");

    /** 容器材料档位（§6.3 / ③-3）：决定耐温上限、热容与换热强度；玻璃烧杯即 GLASS 档。 */
    private final com.pha.trainees.chemistry.container.ContainerMaterial material;

    public BeakerBlock(Properties properties) {
        this(properties, com.pha.trainees.chemistry.container.ContainerMaterial.GLASS);
    }

    public BeakerBlock(Properties properties, com.pha.trainees.chemistry.container.ContainerMaterial material) {
        super(properties);
        this.material = material;
    }

    /** 该方块对应的容器材料档位（坩埚与烧杯共用同一套引擎，只有材料不同）。 */
    public com.pha.trainees.chemistry.container.ContainerMaterial getContainerMaterial() {
        return material;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BeakerBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof BeakerBlockEntity beaker)) {
            return InteractionResult.PASS;
        }

        ItemStack heldItem = player.getItemInHand(hand);
        if (heldItem.isEmpty()) {
            Main.LOGGER.info("[Beaker] Empty hand right-clicked beaker at {}", pos);
            return InteractionResult.CONSUME;
        }

        // ====== 分析仪（§19.6/19.7）：右键 → 诊断 GUI；潜行右键 → 聊天简报 ======
        InteractionResult analyzerResult = AnalyzerAccess.tryUse(level, pos, player, heldItem);
        if (analyzerResult != InteractionResult.PASS) {
            return analyzerResult;
        }

        // ====== 衬层修复 / 重搪（§19.32 的"本批未做"项）======
        // 手持玻璃右键：把腐蚀度清零并把降级材质复位（"重新搪玻璃"）。要求容器已清空。
        if (heldItem.is(Items.GLASS)) {
            if (!beaker.hasCorrosionDamage()) {
                player.displayClientMessage(
                        Component.translatable("message.trainees.lining.not_needed"), true);
                return InteractionResult.CONSUME;
            }
            if (!beaker.restoreLining()) {
                player.displayClientMessage(
                        Component.translatable("message.trainees.lining.not_empty"), true);
                return InteractionResult.CONSUME;
            }
            if (!player.isCreative()) {
                heldItem.shrink(1);
            }
            player.displayClientMessage(Component.translatable("message.trainees.lining.restored",
                    Component.translatable("trainees.material." + beaker.getMaterialId())), true);
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.ANVIL_USE,
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.6f, 1.4f);
            return InteractionResult.CONSUME;
        }

        // ====== 流体桶/瓶（倒入） ======
        if (heldItem.getItem() instanceof BucketItem) {
            // 使用 Forge 工具获取流体
            FluidStack fluidStack = FluidUtil.getFluidContained(heldItem).orElse(null);
            if (fluidStack != null) {
                Fluid fluid = fluidStack.getFluid();
                if (fluid != Fluids.EMPTY) {
                    IonType ion = FluidIonMapper.getIonForFluid(fluid);
                    if (ion != null) {
                        // 严格映射（蓝本 §7.3）：1 桶(1000mB) = BUCKET_TO_MOL_WATER mol
                        double bucketMoles = ChemConfig.BUCKET_TO_MOL_WATER.get();
                        double added = beaker.addIon(ion, bucketMoles);
                        if (added > 0) {
                            // 消耗桶，返回空桶（创造模式不消耗）
                            if (!player.isCreative()) {
                                ItemStack emptyBucket = new ItemStack(Items.BUCKET);
                                player.setItemInHand(hand, emptyBucket);
                            }
                            Main.LOGGER.info("[Beaker] Added {} mol of {} to beaker at {}", added, ion.getId().getPath(), pos);
                            return InteractionResult.CONSUME;
                        }
                    } else {
                        player.displayClientMessage(
                                Component.translatable("message.trainees.beaker.fluid_unknown",
                                        fluid.getFluidType().getDescription()),
                                true
                        );
                        return InteractionResult.FAIL;
                    }
                }
            }
            return InteractionResult.PASS;
        }

        // ====== 空桶（取出） ======
        if (heldItem.getItem() == Items.BUCKET) {
            // 找物质的量最多的液态离子
            IonType targetIon = null;
            double maxMoles = 0;
            for (Map.Entry<IonType, Double> entry : beaker.getContents().entrySet()) {
                IonType ion = entry.getKey();
                double moles = entry.getValue();
                if (ion.getPhase() == Phase.LIQUID && moles > maxMoles) {
                    targetIon = ion;
                    maxMoles = moles;
                }
            }

            if (targetIon != null && maxMoles >= 1.0
                    && beaker.getTotalMoles() - beaker.residueFloorMoles() >= 1.0) {
                // 检查是否有对应的流体映射
                Fluid fluid = FluidIonMapper.getFluidForIon(targetIon);
                if (fluid != null && fluid != Fluids.EMPTY) {
                    double removed = beaker.removeIon(targetIon, 1.0);
                    if (removed > 0) {
                        FluidStack stack = new FluidStack(fluid, 1000);
                        ItemStack bucketStack = fluid.getFluidType().getBucket(stack);
                        if (!bucketStack.isEmpty()) {
                            player.setItemInHand(hand, bucketStack);
                            Main.LOGGER.info("[Beaker] Removed {} mol of {} from beaker at {}, gave bucket of {}",
                                    removed, targetIon.getId().getPath(), pos, fluid.getFluidType().getDescription().getString());
                            return InteractionResult.CONSUME;
                        } else {
                            Main.LOGGER.warn("[Beaker] Fluid {} has no bucket item!", fluid.getFluidType().getDescription().getString());
                            player.displayClientMessage(Component.translatable(
                                    "message.trainees.beaker.no_bucket_for_fluid"), true);
                            // 重要：虽然取不出，但我们已经移除了离子，需要回滚！
                            // 由于移除失败，需要把离子加回去
                            beaker.addIon(targetIon, removed);
                            return InteractionResult.FAIL;
                        }
                    }
                } else {
                    player.displayClientMessage(Component.translatable(
                            "message.trainees.beaker.liquid_unmapped"), true);
                    Main.LOGGER.warn("[Beaker] No fluid mapping for ion: {}", targetIon.getId().getPath());
                    return InteractionResult.FAIL;
                }
            }

            player.displayClientMessage(Component.translatable(
                    "message.trainees.beaker.no_liquid"), true);
            return InteractionResult.FAIL;
        }

        // ====== 空瓶取液（返回 IMaterial 样本，按比例抽取，开发计划 §8.4） ======
        if (heldItem.getItem() == Items.GLASS_BOTTLE) {
            Map<IonType, Double> contents = beaker.getContents();
            if (contents.isEmpty()) {
                player.displayClientMessage(Component.translatable(
                        "message.trainees.beaker.empty"), true);
                return InteractionResult.FAIL;
            }
            double total = contents.values().stream().mapToDouble(Double::doubleValue).sum();
            // 取样下限 0.1 mol（§19.11：实验室小量制备量级；不足则提示累积）
            if (total < 0.1) {
                player.displayClientMessage(Component.translatable(
                        "message.trainees.beaker.too_little_to_sample",
                        String.format("%.3f", total)), true);
                return InteractionResult.FAIL;
            }
            // 残留地板（§19.30.2）：取样取不走死体积那一部分，只能靠清洗
            double extractable = total - beaker.residueFloorMoles();
            if (extractable < 0.1) {
                player.displayClientMessage(Component.translatable(
                        "message.trainees.beaker.residue_only",
                        String.format("%.3f", Math.max(0, extractable)),
                        String.format("%.3f", beaker.residueFloorMoles())), true);
                return InteractionResult.FAIL;
            }
            double sampleTotal = Math.min(extractable, 1.0);
            // 按比例抽取最多 1 mol 样本
            Map<IonType, Double> sample = new HashMap<>();
            for (Map.Entry<IonType, Double> e : contents.entrySet()) {
                sample.put(e.getKey(), e.getValue() * (sampleTotal / total));
            }
            // 从烧杯移除样本量
            for (Map.Entry<IonType, Double> e : sample.entrySet()) {
                beaker.removeIon(e.getKey(), e.getValue());
            }
            ItemStack sampleItem = SubstanceItem.fromComposition(sample);
            heldItem.shrink(1);
            if (!player.getInventory().add(sampleItem)) {
                player.drop(sampleItem, false);
            }
            player.displayClientMessage(Component.translatable(
                    "message.trainees.beaker.sampled", String.format("%.3f", sampleTotal)), true);
            return InteractionResult.CONSUME;
        }

        // ====== 处理物质基类物品 ======
        if (heldItem.getItem() instanceof SubstanceItem) {
            // 获取成分
            Map<IonType, Double> composition = SubstanceItem.getComposition(heldItem);
            if (composition.isEmpty()) {
                player.displayClientMessage(Component.translatable(
                        "message.trainees.beaker.substance_empty"), true);
                return InteractionResult.FAIL;
            }

            // 检查是否超过容量上限
            double totalMoles = composition.values().stream().mapToDouble(Double::doubleValue).sum();
            double currentTotal = beaker.getContents().values().stream().mapToDouble(Double::doubleValue).sum();
            double maxCapacity = beaker.getMaxTotalMoles();

            if (currentTotal + totalMoles > maxCapacity) {
                player.displayClientMessage(Component.translatable(
                        "message.trainees.beaker.capacity_full",
                        String.format("%.3f", currentTotal), String.format("%.3f", maxCapacity)), true);
                return InteractionResult.FAIL;
            }

            // 逐成分加入烧杯
            boolean anyAdded = false;
            for (Map.Entry<IonType, Double> entry : composition.entrySet()) {
                IonType ion = entry.getKey();
                double moles = entry.getValue();
                // 检查单成分上限
                double currentIon = beaker.getAmount(ion);
                double maxPerComponent = beaker.getMaxMolesPerComponent();
                if (currentIon + moles > maxPerComponent) {
                    player.displayClientMessage(Component.translatable(
                            "message.trainees.beaker.component_limit",
                            IonDisplay.format(ion.getId().getPath()),
                            String.format("%.3f", maxPerComponent)), true);
                    return InteractionResult.FAIL;
                }
                beaker.addIon(ion, moles);
                anyAdded = true;
            }

            if (anyAdded) {
                heldItem.shrink(1);
                Main.LOGGER.info("[Beaker] Added substance ({} components, {} mol total) to beaker at {}",
                        composition.size(), String.format("%.3f", totalMoles), pos);
                return InteractionResult.CONSUME;
            }
            return InteractionResult.FAIL;
        }

        // ====== 固体物品（溶解/添加） ======
        IonType ion = SolidIonMapper.getIonForItem(heldItem.getItem());
        if (ion != null) {
            // 粒按 1/9 mol 折算（9 粒 = 1 锭），其余固体按 1 mol/个
            double moles = heldItem.getItem() == Items.IRON_NUGGET
                    ? ChemConfig.SOLID_NUGGET_TO_MOL.get()
                    : ChemConfig.SOLID_INGOT_TO_MOL.get();
            double added = beaker.addIon(ion, moles);
            if (added > 0) {
                heldItem.shrink(1);
                Main.LOGGER.info("[Beaker] Added {} mol of {} from solid item to beaker at {}", added, ion.getId().getPath(), pos);
                return InteractionResult.CONSUME;
            }
        }

        // 手持方块物品：不吞掉这次交互，让位给正常放置（阀门/端口要装在容器的"口"上，§19.28）
        if (heldItem.getItem() instanceof BlockItem) {
            return InteractionResult.PASS;
        }

        player.displayClientMessage(Component.translatable(
                "message.trainees.beaker.cannot_insert"), true);
        return InteractionResult.FAIL;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof BeakerBlockEntity beaker) {
                Main.LOGGER.info("[Beaker] Beaker at {} broken! Contents: {}", pos, beaker.getContents().size());
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        // 使用 createTickerHelper 将 BeakerBlockEntity 的静态 tick 方法绑定到正确的 BlockEntityType
        return createTickerHelper(type, ModChemistry.ModChemistryBlockEntities.BEAKER.get(), BeakerBlockEntity::tick);
    }

}