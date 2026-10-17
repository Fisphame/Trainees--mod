package com.pha.trainees.client;

import com.pha.trainees.Main;

import com.pha.trainees.chemistry.item.SubstanceItem;
import com.pha.trainees.chemistry.material.FixedCompositionTable;
import com.pha.trainees.registry.ModBlocks;
import com.pha.trainees.registry.ModChemistry;
import com.pha.trainees.registry.ModRecipes;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterRecipeBookCategoriesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.List;

@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // 注册GUI屏幕和流体渲染（净化站/反应机菜单尚未实现，见开发蓝本 TODO）
        event.enqueueWork(() -> {
        });

        event.enqueueWork(() -> {
            BlockEntityRenderers.register(ModBlocks.ModBlockEntities.KUN_ALTAR_ENTITY.get(),
                    Renderer.KunAltarBlockEntityRenderer::new);
        });

        // 物质基类：按形态选择模型（§19.15，模型 overrides 谓词 trainees:form）
        event.enqueueWork(() -> ItemProperties.register(
                ModChemistry.ModChemistryItems.SUBSTANCE.get(),
                new ResourceLocation(Main.MODID, "form"),
                (stack, level, entity, seed) -> SubstanceItem.getForm(stack).ordinal()));

        // 注意：Trainer Altar 多方块结构的注册已移至服务器端（Main.onServerStarted），
        // 因为结构匹配/激活逻辑全部在服务端运行，且结构 NBT 位于 data/ 命名空间，
        // 客户端的资源管理器（仅索引 assets/）无法读取。

    }

    /**
     * 物质基类染色（§19.15）：形态底图是白/灰阶贴图，颜色由物质数据 tint 上来
     * （与草/树叶同理，只是颜色源换成 IonType.displayColor；多组分按摩尔分数加权）。
     */
    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        // 基类物质：形态底图 + 物质色 tint（layer0 吃色，layer1 是试剂瓶瓶身不上色）
        event.register((stack, tintIndex) -> tintIndex == 0 ? SubstanceItem.getDisplayColor(stack) : 0xFFFFFFFF,
                ModChemistry.ModChemistryItems.SUBSTANCE.get());
        // 一级物质真物品（定组成，§19.15）：共用同一套染色逻辑，颜色源仍是物质数据
        for (FixedCompositionTable.Entry entry : FixedCompositionTable.all()) {
            event.register((stack, tintIndex) -> tintIndex == 0 ? SubstanceItem.getDisplayColor(stack) : 0xFFFFFFFF,
                    entry.item());
        }
    }

    // 土申祭坛配方在原版配方书中的分类（消除 "Unknown recipe category" 警告）
    // 惰性创建：不能在静态初始化中解析 RegistryObject（此时注册表尚未就绪，会 NPE）
    private static RecipeBookCategories trainerAltarCategory;

    private static RecipeBookCategories getTrainerAltarCategory() {
        if (trainerAltarCategory == null) {
            trainerAltarCategory = RecipeBookCategories.create("trainees_trainer_altar",
                    new ItemStack(ModBlocks.ALTAR_CORE_BLOCK.get()));
        }
        return trainerAltarCategory;
    }

    @SubscribeEvent
    public static void onRegisterRecipeBookCategories(RegisterRecipeBookCategoriesEvent event) {
        // 将祭坛配方类型映射到分类：消除警告，并让原版配方书正确归类祭坛配方
        RecipeBookCategories category = getTrainerAltarCategory();
        event.registerRecipeCategoryFinder(ModRecipes.TRAINER_ALTAR_TYPE.get(),
                recipe -> category);
        // 注册为独立的配方书标签，供原版配方书展示
        event.registerAggregateCategory(category, List.of(category));
    }

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("chemical_container_info", new ChemicalContainerHudOverlay());
        event.registerAboveAll("analyzer_info", new AnalyzerHudOverlay());
    }
}