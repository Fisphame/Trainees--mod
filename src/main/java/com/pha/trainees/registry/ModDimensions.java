package com.pha.trainees.registry;

import com.pha.trainees.Main;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

public class ModDimensions {
    public static final String PULLUSION_ID = "pullusion";

    // 资源键 - 用于游戏中引用维度
    public static final ResourceKey<Level> PULLUSION_LEVEL =
            ResourceKey.create(Registries.DIMENSION,
                    ResourceLocation.fromNamespaceAndPath(Main.MODID, PULLUSION_ID));

    public static final ResourceKey<DimensionType> PULLUSION_TYPE =
            ResourceKey.create(Registries.DIMENSION_TYPE,
                    ResourceLocation.fromNamespaceAndPath(Main.MODID, PULLUSION_ID));
}
