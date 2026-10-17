package com.pha.trainees.registry;

import com.pha.trainees.Main;
import com.pha.trainees.entity.*;
//import com.pha.trainees.entity.KunTraineesEntity;
//import com.pha.trainees.entity.RopeEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings({"removal"})
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Main.MODID);
    // 注册自定义剑实体
    public static final RegistryObject<EntityType<CalledSwordEntity>> CALLED_SWORD =
            ENTITIES.register("called_sword",
                    () -> EntityType.Builder.of(
                                    CalledSwordEntity::new, // 实体工厂方法
                                    MobCategory.MISC // 实体分类（不属于生物）
                            )
                            .sized(1.0F, 1.0F) // 碰撞箱大小（宽1.0，高1.0）
                            .clientTrackingRange(8) // 客户端跟踪范围（4区块）
                            .build("called_sword") // 内部名称
            );

    public static final RegistryObject<EntityType<ParticleEntity>> PARTICLE_ENTITY =
            ENTITIES.register("particle_entity",
                    () -> EntityType.Builder.of(
                                    ParticleEntity::new, // 实体工厂方法
                                    MobCategory.MISC // 实体分类（不属于生物）
                            )
                            .sized(0.1F, 0.1F) // 碰撞箱大小（宽1.0，高1.0）
                            .clientTrackingRange(4) // 客户端跟踪范围（4区块）
                            .build("particle_entity") // 内部名称
            );

    public static final RegistryObject<EntityType<KunTraineesEntity>> KUN_TRAINEES =
            ENTITIES.register("kun_trainees",
                    () -> EntityType.Builder.of(KunTraineesEntity::new, MobCategory.CREATURE)
                            .sized(0.4F, 0.7F)
                            .clientTrackingRange(16)
                            .build(new ResourceLocation(Main.MODID, "kun_trainees").toString())
            );

    public static final RegistryObject<EntityType<KunAntiEntity>> KUN_ANTI =
            ENTITIES.register("kun_anti",
                    () -> EntityType.Builder.of(KunAntiEntity::new, MobCategory.CREATURE)
                            .sized(0.4F, 0.7F)
                            .clientTrackingRange(16)
                            .build(new ResourceLocation(Main.MODID, "kun_anti").toString())
            );

    public static final RegistryObject<EntityType<GoldChickenEntity>> GOLD_CHICKEN =
            ENTITIES.register("gold_chicken",
                    () -> EntityType.Builder.of(GoldChickenEntity::new, MobCategory.CREATURE)
                            .sized(0.4F, 0.7F)
                            .clientTrackingRange(16)
                            .build(new ResourceLocation(Main.MODID, "gold_chicken").toString())
            );

    public static final RegistryObject<EntityType<KunAntiBossEntity>> KUN_ANTI_BOSS =
            ENTITIES.register("kun_anti_boss",
                    () -> EntityType.Builder.of(KunAntiBossEntity::new, MobCategory.MONSTER)
                            .sized(1.5F, 2.5F)   // 宽度1.5格，高度2.5格
                            .clientTrackingRange(16)
                            .build(new ResourceLocation(Main.MODID, "kun_anti_boss").toString())
            );

    public static final RegistryObject<EntityType<GasEntities.HydrogenEntity>> HYDROGEN =
            ENTITIES.register("hydrogen",
                    () -> EntityType.Builder.<GasEntities.HydrogenEntity>of(
                            GasEntities.HydrogenEntity::new, MobCategory.MISC
                            )
                            .sized(1.0F, 1.0F) // 初始尺寸
                            .clientTrackingRange(4)
                            .build(new ResourceLocation(Main.MODID, "hydrogen").toString())
            );

    public static final RegistryObject<EntityType<GasEntities.OxygenEntity>> OXYGEN =
            ENTITIES.register("oxygen",
                    () -> EntityType.Builder.<GasEntities.OxygenEntity>of(
                                    GasEntities.OxygenEntity::new, MobCategory.MISC
                            )
                            .sized(1.0F, 1.0F) // 初始尺寸
                            .clientTrackingRange(4)
                            .build(new ResourceLocation(Main.MODID, "oxygen").toString())
            );
}

