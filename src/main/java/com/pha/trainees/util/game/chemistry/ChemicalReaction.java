package com.pha.trainees.util.game.chemistry;

import com.pha.trainees.registry.ModBlocks;
import com.pha.trainees.registry.ModChemistry;
import com.pha.trainees.registry.ModEntities;
import com.pha.trainees.util.types.NumedItemEntities;
import com.pha.trainees.util.game.CommandTools;
import com.pha.trainees.util.game.Tools;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;


import static com.pha.trainees.util.game.chemistry.ReactionConditions.*;
import static com.pha.trainees.util.game.Tools.*;
import static com.pha.trainees.entity.GasEntities.*;

public class ChemicalReaction {

//    public static void executeResult(ChemicalEquation equation,Level level, ItemEntity entity, ItemStack stack){
//        Item disItem = stack.getItem();
//        if (stack.getCount() < 2) return;
//        int reactions = stack.getCount() / 2;
//        int remainder = stack.getCount() % 2;
//        int producedHBp = reactions * 2;
//
//        entity.discard();
//
//        if (producedHBp > 0) {
//            NumedItemEntities itemEntities = AddEntity(level, entity,
//                    new ItemStack(ModChemistry.ModChemistryItems.CHE_HBP_POWDER.get(), producedHBp), true);
//            AddNIEs(entity, itemEntities, false);
//        }
//        if (remainder > 0) {
//            ItemStack remainStack = stack.copy();
//            remainStack.setCount(remainder);
//            ItemEntity remainderEntity = new ItemEntity(
//                    level, entity.getX(), entity.getY(), entity.getZ(), remainStack
//            );
//            level.addFreshEntity(remainderEntity);
//        }
//
//        for(ChemicalEquation.ChemicalComponent component : equation.getProducts() ){
//            Item porduceItem = component.item; int cf = component.coefficient;
//            if (Tools.isElementary())
//        }
//    }

    // Ji + Bp == JiBp
    public static boolean JiBpCombination_1(UseOnContext context, int num){
        //检测端
        if (context.getLevel().isClientSide()) return false;

        //检测数量
        Player player = context.getPlayer();
        if (context.getItemInHand().getCount() < num && !player.isCreative()) return false;

        //检测方块
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        BlockState clickedState = context.getLevel().getBlockState(clickedPos);
        ResourceLocation clickedBlockId = ForgeRegistries.BLOCKS.getKey(clickedState.getBlock());
        BlockPos belowPos = clickedPos.below();
        BlockState belowState = level.getBlockState(belowPos);
        ResourceLocation belowBlockId = ForgeRegistries.BLOCKS.getKey(belowState.getBlock());
        double x = clickedPos.getX(), y = clickedPos.getY(), z = clickedPos.getZ();
        boolean isRight = clickedBlockId != null && clickedBlockId == ModBlocks.TWO_HALF_INGOT_BLOCK.getId()
                && belowBlockId != null && belowBlockId.toString().equals("minecraft:campfire");

        if (!isRight) return false;

        //检测篝火状态
        boolean isLit = false;
        if (belowState.hasProperty(net.minecraft.world.level.block.CampfireBlock.LIT)) {
            isLit = belowState.getValue(net.minecraft.world.level.block.CampfireBlock.LIT);
        }

        if (!isLit) return false;


        Tools.DoTnt_6(level, x, y, z, 6.0F, 6, 1);
        //实现逻辑
//        Block targetBlock = ForgeRegistries.BLOCKS.getValue(ModBlocks.BASKETBALL_ANTI_BLOCK_RGT.getId());
        BlockState blockState = ModChemistry.ModChemistryBlocks.CHE_JIBP_BLOCK_RGT.get().defaultBlockState();

        // 替换方块
        context.getLevel().setBlock(clickedPos, blockState, 3);
        if (player != null && !player.isCreative()) {
            context.getItemInHand().shrink(num);
        }
        context.getLevel().playSound(null, clickedPos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);

        return true;

    }


    public static void registerAllReactions() {
        ReactionSystem.ReactionRegistry.clearAllReactions();

        // ==================== HBpO 光照分解反应 ====================
        // 2HBpO =光照= 2HBp + O2↑
//        ModChemistry.ModChemistryEquations.HBPO_DECOMPOSITION.register(10);

        ChemicalEquation r1 = ModChemistry.ModChemistryEquations.HBPO_DECOMPOSITION;
        ChemicalEquation r2 = ModChemistry.ModChemistryEquations.BP2_WATER_REACTION;
        ChemicalEquation r3 = ModChemistry.ModChemistryEquations.JI_WATER_REACTION;



        ReactionBuilder.create("hbpo_decompose")
                .withTimedCondition(
                        r1.getConditions().mainCondition, r1.getConditions().durationProvider
                )
                .withResult((level, entity, stack) -> {
                    if (stack.getCount() < 2) return;
                    int reactions = stack.getCount() / 2;
                    int remainder = stack.getCount() % 2;
                    int producedHBp = reactions * 2;
                    double x = entity.getX(), y = entity.getY(),  z = entity.getZ();
                    entity.discard();

                    if (producedHBp > 0) {
                        NumedItemEntities itemEntities = AddEntity(level, entity,
                                new ItemStack(ModChemistry.ModChemistryItems.CHE_HBP_POWDER.get(), producedHBp), 1);
                        AddNIEs(entity, itemEntities, false);
                    }
                    if (remainder > 0) {
                        ItemStack remainStack = stack.copy();
                        remainStack.setCount(remainder);
                        ItemEntity remainderEntity = new ItemEntity(
                                level, x, y, z, remainStack
                        );
                        level.addFreshEntity(remainderEntity);
                    }
                    OxygenEntity oxygen = new OxygenEntity(
                            ModEntities.OXYGEN.get(), level,
                            1.0f,
                            Math.max((float) reactions / 2.0f, 1.0f),
                            0.03f
                    );
                    oxygen.setPos(x, y, z);
                    level.addFreshEntity(oxygen);
                })
                .withPriority(10)
                .register();

        // ==================== 黑单质（黑 Bp2）与水的反应 ====================
        // Bp2 + H2O == HBp + HBpO
//        ModChemistry.ModChemistryEquations.BP2_WATER_REACTION.register(5);

        ReactionBuilder.create("bp2_water_reaction")
                .withTimedCondition(
                        r2.getConditions().mainCondition, r2.getConditions().durationProvider
                )
                .withResult((level, entity, stack) -> {

                    int simpleSubstance = 2;
                    int multiplier = Tools.getPowderMultiplier(stack.getItem());
                    int count = stack.getCount() * multiplier * simpleSubstance / 2;

                    if (count > 0) {
                        entity.discard();
                        ItemStack itemStack_1 = new ItemStack(ModChemistry.ModChemistryItems.CHE_HBP_POWDER.get(), count);
                        ItemStack itemStack_2 = new ItemStack(ModChemistry.ModChemistryItems.CHE_HBPO_POWDER.get(), count);
                        NumedItemEntities itemEntities_1 = AddEntity(level, entity, itemStack_1, 1);
                        NumedItemEntities itemEntities_2 = AddEntity(level, entity, itemStack_2, 1);
                        AddNIEs(entity, itemEntities_1, false);
                        AddNIEs(entity, itemEntities_2, false);
                    }
                })
                .withPriority(5)
                .register();

        // ==================== Ji与水的反应 ====================
        // 2Ji + 2H2O == 2JiOH + H2↑
        ReactionBuilder.create("ji_water_reaction")
                .withCondition(
                        and(
                                IS_JI.get(), or(IN_RAIN, IN_WATER), hasMinCount(2)
                        )
                )
                .withResult((level, entity, stack) -> {
                    int reactions = stack.getCount() / 2;
                    int remainder = stack.getCount() % 2;
                    double x = entity.getX(), y = entity.getY(),  z = entity.getZ();
                    entity.discard();

                    if (reactions > 0) {
                        ItemStack productStack = new ItemStack(getWhichJiProduct(stack, 1), reactions);
                        NumedItemEntities itemEntities = AddEntity(level, entity, productStack , 3);
                        AddNIEs(entity, itemEntities, true);
                    }
                    if (remainder > 0) {
                        ItemStack remainderStack = new ItemStack(stack.getItem(), remainder);
                        ItemEntity remainderEntity = new ItemEntity(level, x, y, z, remainderStack);
                        level.addFreshEntity(remainderEntity);
                    }
                    HydrogenEntity hydrogen = new HydrogenEntity(
                            ModEntities.HYDROGEN.get(), level,
                            1.0f,
                            Math.max((float) reactions / 2.0f, 1.0f),
                            0.03f
                    );
                    hydrogen.setPos(x, y, z);
                    level.addFreshEntity(hydrogen);

                    if (CommandTools.isReactionExplode(level)){
                        Tools.DoTnt_center(level, x, y, z);
                    }
                })
                .withPriority(5)
                .register();

        // ==================== Ji金属燃烧反应 ====================
        // 2Ji + O2（燃烧）== Ji2O2
        ReactionBuilder.create("ji_burning_reaction")
                .withCondition(
                        and(
                                IS_JI.get(), IS_BURNING, not(IN_WATER), hasMinCount(2)
                        )
                )
                .withResult((level, entity, stack) -> {
                    int reactions = stack.getCount() / 2;
                    int remainder = stack.getCount() % 2;
                    // 扑灭火焰
                    entity.setRemainingFireTicks(0);
                    entity.discard();

                    if (reactions > 0) {
                        ItemStack productStack = new ItemStack(getWhichJiProduct(stack, 3), reactions);
                        NumedItemEntities itemEntities = AddEntity(level, entity, productStack , 3);
                        AddNIEs(entity, itemEntities, true);
                    }
                    if (remainder > 0) {
                        ItemStack remainderStack = new ItemStack(stack.getItem(), remainder);
                        ItemEntity remainderEntity = new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), remainderStack);
                        level.addFreshEntity(remainderEntity);
                    }

                    level.playSound(null, entity.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS,
                            0.7f, 0.8f);
//                    Tools.DoTnt_center(level, entity.getX(), entity.getY(), entity.getZ(), 2.0f);
                    if (CommandTools.isReactionExplode(level)){
                        level.explode(null, entity.getX(), entity.getY(), entity.getZ(),
                                0.5f, Level.ExplosionInteraction.MOB);
                    }

                })
                .withPriority(20)
                .register();
    }
}
