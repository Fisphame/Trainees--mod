package com.pha.trainees;

import com.mojang.logging.LogUtils;
import com.pha.trainees.api.DeepSeekClient;
import com.pha.trainees.chemistry.material.SubstanceBlueprintRegistry;
import com.pha.trainees.command.DebugCommand;
import com.pha.trainees.config.ChemConfig;
import com.pha.trainees.event.*;
import com.pha.trainees.multiblock.TrainerAltarPattern;
import com.pha.trainees.network.ModNetwork;
import com.pha.trainees.registry.*;
import com.pha.trainees.util.game.chemistry.ChemicalReaction;
import com.pha.trainees.util.game.chemistry.ReactionConditions;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

@SuppressWarnings({"deprecation", "removal"})
@Mod(Main.MODID)
public class Main {

    public static final String MODID = "trainees";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static DeepSeekClient deepSeekClient;



    public Main() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus ebus = MinecraftForge.EVENT_BUS;
        ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON,
                ModConfig.COMMON_SPEC, "trainees-common.toml");
        ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.SERVER,
                ChemConfig.SERVER_SPEC, "trainees-server.toml");

        ModBlocks.BLOCKS.register(bus);
        ModChemistry.ModChemistryBlocks.BLOCKS.register(bus);
        ModBlocks.ModBlockEntities.BLOCK_ENTITIES.register(bus);
        ModChemistry.ModChemistryBlockEntities.BLOCK_ENTITIES.register(bus);
        // 魔法机器的 GUI 菜单类型（§17.1）
        com.pha.trainees.registry.ModMenus.MENUS.register(bus);
        // 精练精华网（§17.1 ④）：端口/墙壁/总线/扩展接口/导线
        com.pha.trainees.registry.ModEssenceBlocks.BLOCKS.register(bus);
        com.pha.trainees.registry.ModEssenceBlocks.ModEssenceBlockEntities.BLOCK_ENTITIES.register(bus);
        com.pha.trainees.registry.ModEssenceItems.ITEMS.register(bus);
        ModSounds.SOUNDS.register(bus);
        ModEnchantments.ENCHANTMENTS.register(bus);
        ModItems.ITEMS.register(bus);
        ModChemistry.ModChemistryBlockItems.ITEMS.register(bus);
        ModChemistry.ModChemistryItems.ITEMS.register(bus);
        ModEntities.ENTITIES.register(bus);
        Something.PrankBlocks.BLOCKS.register(bus);
        Something.PrankItems.ITEMS.register(bus);
        Something.Paintings.PAINTING_VARIANTS.register(bus);
        HiddenItem.BLOCKS.register(bus);
        HiddenItem.ITEMS.register(bus);
        ModCreativeModeTabs.CREATIVE_MODE_TABS.register(bus);
        ModRecipes.SERIALIZERS.register(bus);
        ModRecipes.TYPES.register(bus);
        ModFluids.FLUID_TYPES.register(bus);
        ModFluids.FLUIDS.register(bus);
//        ModChemistry.ModFluids.FLUID_TYPES.register(bus);
//        ModChemistry.ModFluids.FLUIDS.register(bus);
//        ModChemistry.ModFluids.FLUID_BLOCKS.register(bus);
        ModCommand.register();
        ModNetwork.register();
        ebus.register(ModCommand.AskCommand.class);  // 注册命令
//        bus.addListener(this::onClientSetup);


        bus.register(new Register());
        bus.addListener(this::commonSetup);
        // 数据生成器入口通过 @Mod.EventBusSubscriber 自动注册（见 datagen/ModDataGenerators）
        ebus.register(AbilityHandler.class);
        ebus.register(FoodHandler.class);
        ebus.register(this);


    }

    private void commonSetup(final @NotNull FMLCommonSetupEvent event) {
        Main.LOGGER.info("Chemistry System: Starting commonSetup");
        event.enqueueWork(() -> {
            Main.LOGGER.info("Chemistry System: Executing enqueueWork");
            try {
                // Pre-initialize conditions
                Main.LOGGER.info("Chemistry System: Pre-initializing conditions...");
                ReactionConditions.IS_JI.get();
                ReactionConditions.hbpoDecomposeCondition.get();
                ReactionConditions.bp2AndWaterCondition.get();
                Main.LOGGER.info("Chemistry System: Condition pre-initialization complete");

                // Register reactions
                Main.LOGGER.info("Chemistry System: Calling registerAllReactions()");
                ChemicalReaction.registerAllReactions();
                Main.LOGGER.info("Chemistry System: registerAllReactions() call complete");

                // 注册物质蓝图
                Main.LOGGER.info("Chemistry System: Registering substance blueprints...");
                SubstanceBlueprintRegistry.registerAll();
                Main.LOGGER.info("Chemistry System: Substance blueprints registered");

                // 注册一级物质（定组成表 + 代表物品；§19.15）
                Main.LOGGER.info("Chemistry System: Registering first-class substances...");
                ModChemistry.ModChemistryItems.registerFirstClassSubstances();
                Main.LOGGER.info("Chemistry System: First-class substances registered");


            } catch (Exception e) {
                Main.LOGGER.error("Chemistry System: Initialization failed", e);
            }
        });

        Main.LOGGER.info("Chemistry System: commonSetup completed");
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        // 服务器启动完成后，从配置读取 API Key 并初始化客户端
        String apiKey = ModConfig.COMMON.deepSeekApiKey.get();
        if (apiKey != null && !apiKey.isEmpty()) {
            deepSeekClient = new DeepSeekClient(apiKey);
            LOGGER.info("DeepSeek Client initialized.");
        } else {
            LOGGER.warn("DeepSeek API Key not set! Please configure it in config/trainees-common.toml");
        }

        // 注册反应（此时 SERVER 配置已加载）
        Main.LOGGER.info("Chemistry System: Registering reactions after server start...");
        try {
            ModChemistry.Reactions.registerAll();
            Main.LOGGER.info("Chemistry System: Reactions registered successfully");
        } catch (Exception e) {
            Main.LOGGER.error("Chemistry System: Failed to register reactions", e);
        }

        // 注册多方块结构（服务器侧），使专用服务器也能识别祭坛结构
        try {
            TrainerAltarPattern.register(event.getServer().getResourceManager());
        } catch (Exception e) {
            LOGGER.error("Failed to register Trainer Altar structure", e);
        }
    }

    // 提供 getter 供命令使用
    public static DeepSeekClient getDeepSeekClient() {
        return deepSeekClient;
    }
}