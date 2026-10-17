package com.pha.trainees.datagen;

import com.pha.trainees.Main;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 数据生成器入口（§19.19 S0 / §19.21）。
 *
 * <p>原则：**我们写代码、产出 JSON**——原生内容因此变成"默认数据集"，
 * 整合包作者可以用数据包路径覆盖或字段补丁来改（§19.19.3）。</p>
 *
 * <p>运行：{@code gradlew runData}（输出到 {@code src/generated/resources}，
 * 该目录已在 build.gradle 里挂到 resources，运行时与打包均生效）。</p>
 */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModDataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper helper = event.getExistingFileHelper();

        ModBlockTagsProvider blockTags =
                new ModBlockTagsProvider(output, event.getLookupProvider(), helper);
        generator.addProvider(event.includeServer(), blockTags);
        generator.addProvider(event.includeServer(),
                new ModItemTagsProvider(output, event.getLookupProvider(), blockTags));
    }
}
