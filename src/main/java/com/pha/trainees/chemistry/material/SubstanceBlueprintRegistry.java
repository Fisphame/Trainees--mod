package com.pha.trainees.chemistry.material;

import com.pha.trainees.Main;
import com.pha.trainees.registry.ModChemistry.ModIons;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * 物质蓝图注册表
 * 存储所有预定义的蓝图，游戏启动时初始化
 */
@SuppressWarnings({"deprecation", "removal"})
public class SubstanceBlueprintRegistry {


    private static final Map<ResourceLocation, SubstanceBlueprint> BLUEPRINTS = new HashMap<>();

    /**
     * 注册所有蓝图
     * 在 FMLCommonSetupEvent 中调用
     */
    public static void registerAll() {
        // ====== 铁矿石蓝图 ======
        register(new SubstanceBlueprint.Builder(
                new ResourceLocation(Main.MODID, "iron_ore_poor"))
                .component(ModIons.Fe2O3, 0.30)
                .component(ModIons.SiO2, 0.50)
                .component(ModIons.H2O, 0.20)
                .valuable(ModIons.Fe2O3)
                .totalMolesPerUnit(10.0)
                .displayName("贫铁矿石")
                .build()
        );

        register(new SubstanceBlueprint.Builder(
                new ResourceLocation(Main.MODID, "iron_ore_normal"))
                .component(ModIons.Fe2O3, 0.55)
                .component(ModIons.SiO2, 0.35)
                .component(ModIons.H2O, 0.10)
                .valuable(ModIons.Fe2O3)
                .totalMolesPerUnit(10.0)
                .displayName("普通铁矿石")
                .build()
        );

        register(new SubstanceBlueprint.Builder(
                new ResourceLocation(Main.MODID, "iron_ore_rich"))
                .component(ModIons.Fe2O3, 0.80)
                .component(ModIons.SiO2, 0.15)
                .component(ModIons.H2O, 0.05)
                .totalMolesPerUnit(10.0)
                .valuable(ModIons.Fe2O3)
                .displayName("富铁矿石")
                .build()
        );

        // ====== 铜矿石蓝图 ======
        register(new SubstanceBlueprint.Builder(
                new ResourceLocation(Main.MODID, "copper_ore_normal"))
                .component(ModIons.Cu2O, 0.50)    // 假设Cu2O已注册
                .component(ModIons.SiO2, 0.40)
                .component(ModIons.H2O, 0.10)
                .totalMolesPerUnit(10.0)
                .valuable(ModIons.Cu2O)
                .displayName("普通铜矿石")
                .build()
        );

        // ====== 海水蓝图 ======
        register(new SubstanceBlueprint.Builder(
                new ResourceLocation(Main.MODID, "seawater"))
                .component(ModIons.H2O, 0.965)
                .component(ModIons.Na_1, 0.010)
                .component(ModIons.Cl_minus, 0.010)
                .component(ModIons.Mg_2, 0.005)   // 假设Mg_2已注册
                .component(ModIons.SO4_2minus, 0.005)
                .component(ModIons.K_1, 0.003)
                .component(ModIons.Ca_2, 0.002)
                .totalMolesPerUnit(50.0)
                .valuable(ModIons.Na_1, ModIons.Cl_minus, ModIons.Mg_2, ModIons.SO4_2minus, ModIons.K_1, ModIons.Ca_2)
                .displayName("海水")
                .build()
        );

        // ====== 原油蓝图（示例，为后续石化准备） ======
        register(new SubstanceBlueprint.Builder(
                new ResourceLocation(Main.MODID, "crude_oil"))
                .component(ModIons.C8H18, 0.30)   // 辛烷
                .component(ModIons.C10H22, 0.25)  // 癸烷
                .component(ModIons.C12H26, 0.20)  // 十二烷
                .component(ModIons.C6H6, 0.15)    // 苯
                .component(ModIons.SULFUR, 0.10)  // 硫
                .valuable(ModIons.C8H18, ModIons.C10H22, ModIons.C12H26, ModIons.C6H6)
                .totalMolesPerUnit(20.0)
                .displayName("原油")
                .build()
        );

        Main.LOGGER.info("[Chemistry] Registered {} substance blueprints", BLUEPRINTS.size());
    }

    public static void register(SubstanceBlueprint blueprint) {
        BLUEPRINTS.put(blueprint.getId(), blueprint);
    }

    public static SubstanceBlueprint get(ResourceLocation id) {
        SubstanceBlueprint blueprint = BLUEPRINTS.get(id);
        if (blueprint == null) {
            Main.LOGGER.warn("[Chemistry] Blueprint not found: {}", id);
        }
        return blueprint;
    }

    public static boolean contains(ResourceLocation id) {
        return BLUEPRINTS.containsKey(id);
    }

    public static Map<ResourceLocation, SubstanceBlueprint> getAll() {
        return Map.copyOf(BLUEPRINTS);
    }

    /**
     * 清空注册表（用于调试/热加载）
     */
    public static void clear() {
        BLUEPRINTS.clear();
    }
}