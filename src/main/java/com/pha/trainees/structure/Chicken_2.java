//package com.pha.trainees.structure;
//
//import com.mojang.serialization.Codec;
//import com.mojang.serialization.codecs.RecordCodecBuilder;
//import com.pha.trainees.Main;
//import net.minecraft.commands.arguments.TimeArgument;
//import net.minecraft.core.BlockPos;
//import net.minecraft.core.Holder;
//import net.minecraft.core.registries.Registries;
//import net.minecraft.resources.ResourceKey;
//import net.minecraft.resources.ResourceLocation;
//import net.minecraft.server.packs.resources.ResourceManager;
//import net.minecraft.world.level.levelgen.Heightmap;
//import net.minecraft.world.level.levelgen.structure.Structure;
//import net.minecraft.world.level.levelgen.structure.StructureType;
//import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
//import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
//import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
//import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
//import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
//
//import java.util.Optional;
//
//public class Chicken_2 extends Structure {
//    public static final Codec<Chicken_2> CODEC = RecordCodecBuilder.<Chicken_2>mapCodec(instance ->
//            instance.group(settingsCodec(instance))
//                    .apply(instance, Chicken_2::new)).codec();
//
//    public Chicken_2(Structure.StructureSettings config) {
//        super(config);
//    }
//
//    @Override
//    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
//        return onTopOfChunkCenter(context, Heightmap.Types.WORLD_SURFACE_WG, builder -> {
//            generatePieces(builder, context);
//        });
//    }
//
//    private static void generatePieces(StructurePiecesBuilder builder, GenerationContext context) {
//        // 获取区块中心位置
//        int x = context.chunkPos().getMinBlockX() + 7;
//        int z = context.chunkPos().getMinBlockZ() + 7;
//
//        // 获取地表高度
//        int y = context.chunkGenerator().getFirstFreeHeight(
//                x, z, Heightmap.Types.WORLD_SURFACE_WG,
//                context.heightAccessor(), context.randomState()
//        );
//
//        BlockPos blockpos = new BlockPos(x, y, z);
//
//        // 获取模板池
//        Holder<StructureTemplatePool> pool = context.registryAccess()
//                .registryOrThrow(Registries.TEMPLATE_POOL)
//                .getHolderOrThrow(ResourceKey.create(
//                        Registries.TEMPLATE_POOL,
//                        new ResourceLocation(Main.MODID, "chicken_2")
//                ));
//
//
//        // 生成结构
//        JigsawPlacement.addPieces(
//                context,
//                pool,
//                Optional.empty(),
//                1,
//                blockpos,
//                false,
//                Optional.empty(),
//                1
//        );
//    }
//
//    @Override
//    public StructureType<?> type() {
//        return Main.CHICKEN_2.get(); // 引用主类中注册的结构类型
//    }
//}
