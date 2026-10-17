package com.pha.trainees.registry;

import com.pha.trainees.Main;
import com.pha.trainees.util.interfaces.IHoverText;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;



public class ModCreativeModeTabs implements IHoverText {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Main.MODID);

    // 练习生
    public static final RegistryObject<CreativeModeTab> KUN_TAB = CREATIVE_MODE_TABS.register("kun_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.trainees.kun_tab"))
                    .icon(() -> new ItemStack(ModItems.KUN_NUGGET.get()))
                    .displayItems((parm,output) -> {
                        ModItems.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                    })
                    .build()
    );

    // 整蛊
    public static final RegistryObject<CreativeModeTab> PRANK_TAB = CREATIVE_MODE_TABS.register("prank_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.trainees.prank_tab"))
                    .icon(() -> new ItemStack(Something.PrankItems.REAL_DIAMOND_PICKAXE.get()))
                    .displayItems((parm,output) -> {
                        Something.PrankItems.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                    })
                    .build()
    );

    // 化学讲堂
    // 排列顺序（§19.34，作者指定）：化学书 → 工具 → 工作方块 → 组件 → 水桶 → 物质基类 → 消毒液 → 杂质 → 遗留物品
    // 说明：新内容逐个登记以固定顺序；「遗留物品」= 旧「鸡/黑」化学线（che_* 命名）与练习生材料，
    //       按注册名前缀自动收集，只保留可得性，不再参与新化学（作者：不删除，但放最后）。
    public static final RegistryObject<CreativeModeTab> CHEMISTRY_TAB = CREATIVE_MODE_TABS.register("chemistry_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.trainees.chemistry_tab"))
                    .icon(() -> new ItemStack(ModChemistry.ModChemistryItems.CHEMISTRY_BOOK.get()))
                    .displayItems( (parm, output) -> {
                        var cheItems = ModChemistry.ModChemistryItems.ITEMS;
                        var cheBlocks = ModChemistry.ModChemistryBlockItems.ITEMS;

                        // ① 化学书
                        output.accept(ModChemistry.ModChemistryItems.CHEMISTRY_BOOK.get());

                        // ② 工具：分析仪（普通/创造）、扳手、毛刷
                        output.accept(ModChemistry.ModChemistryItems.ANALYZER.get());
                        output.accept(ModChemistry.ModChemistryItems.ANALYZER_CREATIVE.get());
                        output.accept(ModChemistry.ModChemistryItems.WRENCH.get());
                        output.accept(ModChemistry.ModChemistryItems.BRUSH.get());

                        // ③ 工作方块：容器 → 电解槽 → 恒温槽 → 打包机 → 反应釜 → 创造热源
                        output.accept(ModChemistry.ModChemistryBlockItems.BEAKER.get());
                        output.accept(ModChemistry.ModChemistryBlockItems.METAL_CRUCIBLE.get());
                        output.accept(ModChemistry.ModChemistryBlockItems.QUARTZ_CRUCIBLE.get());
                        output.accept(ModChemistry.ModChemistryBlockItems.CERAMIC_CRUCIBLE.get());
                        output.accept(ModChemistry.ModChemistryBlockItems.ELECTROLYSIS_CELL.get());
                        output.accept(ModChemistry.ModChemistryBlockItems.THERMOSTATIC_BATH.get());
                        output.accept(ModChemistry.ModChemistryBlockItems.PACKER.get());
                        output.accept(ModChemistry.ModChemistryBlockItems.GLASS_REACTOR.get());
                        output.accept(ModChemistry.ModChemistryBlockItems.METAL_REACTOR.get());
                        output.accept(ModChemistry.ModChemistryBlockItems.QUARTZ_REACTOR.get());
                        output.accept(ModChemistry.ModChemistryBlockItems.CERAMIC_REACTOR.get());
                        output.accept(ModChemistry.ModChemistryBlockItems.GLASS_LINED_REACTOR.get());
                        output.accept(ModChemistry.ModChemistryBlockItems.CREATIVE_HEAT_SOURCE.get());
                        output.accept(ModChemistry.ModChemistryBlockItems.CREATIVE_ENERGY_SOURCE.get());

                        // ④ 组件：阀门 / 搅拌器 / 夹套（方块）＋ 密封盖 / 离子交换膜（物品）
                        output.accept(ModChemistry.ModChemistryBlockItems.VALVE.get());
                        output.accept(ModChemistry.ModChemistryBlockItems.STIRRER.get());
                        output.accept(ModChemistry.ModChemistryBlockItems.JACKET.get());
                        output.accept(ModChemistry.ModChemistryItems.SEALED_HEAD.get());
                        output.accept(ModChemistry.ModChemistryItems.ION_MEMBRANE.get());

                        // ⑤ 水桶（水是反应物/溶剂，桶装便于灌注）
                        output.accept(Items.WATER_BUCKET.asItem());

                        // ⑥ 物质基类：物质基类物品 + 一级物质（1 物品 = 1 mol）
                        output.accept(ModChemistry.ModChemistryItems.SUBSTANCE.get());
                        output.accept(ModChemistry.ModChemistryItems.HYDROCHLORIC_ACID.get());
                        output.accept(ModChemistry.ModChemistryItems.SULFURIC_ACID.get());
                        output.accept(ModChemistry.ModChemistryItems.SODIUM_HYDROXIDE.get());
                        output.accept(ModChemistry.ModChemistryItems.SODIUM_CARBONATE.get());
                        output.accept(ModChemistry.ModChemistryItems.HYDROGEN_PEROXIDE.get());
                        output.accept(ModChemistry.ModChemistryItems.BENZENE.get());
                        output.accept(ModChemistry.ModChemistryItems.SULFUR.get());
                        output.accept(ModChemistry.ModChemistryItems.SODIUM_CHLORIDE.get());

                        // ⑦ 消毒液（次氯酸钠成品）
                        output.accept(ModChemistry.ModChemistryItems.BLEACH_GOOD.get());
                        output.accept(ModChemistry.ModChemistryItems.BLEACH_PREMIUM.get());

                        // ⑧ 杂质（不合格品与残渣）
                        output.accept(ModChemistry.ModChemistryItems.DEFECTIVE_CHLORINE.get());
                        output.accept(ModChemistry.ModChemistryItems.DEFECTIVE_ALKALI.get());
                        output.accept(ModChemistry.ModChemistryItems.DEFECTIVE_ACID.get());
                        output.accept(ModChemistry.ModChemistryItems.DEFECTIVE_CHLORATE.get());
                        output.accept(ModChemistry.ModChemistryItems.IMPERFECTION.get());

                        // ⑨ 遗留物品
                        output.accept(ModItems.TWO_HALF_INGOT_BLOCK_ITEM.get());
                        output.accept(ModItems.TWO_HALF_INGOT.get());
                        output.accept(ModItems.KUN_NUGGET.get());
                        output.accept(ModItems.POWDER_ANTI.get());
                        output.accept(ModItems.POWDER_ANTI_4.get());
                        output.accept(ModItems.POWDER_ANTI_9.get());
                        cheBlocks.getEntries().stream()
                                .filter(entry -> entry.getId().getPath().startsWith("che_"))
                                .forEach(entry -> output.accept(entry.get()));
                        cheItems.getEntries().stream()
                                .filter(entry -> entry.getId().getPath().startsWith("che_"))
                                .forEach(entry -> output.accept(entry.get()));
                    })
                    .build()
    );
}
