package com.pha.trainees.chemistry.particle;

import com.pha.trainees.Main;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings({"deprecation", "removal"})
public final class IonTags {

    // ==================== 基础离子类型 ====================
    public static final ResourceLocation CATION = new ResourceLocation(Main.MODID, "cation");       // 阳离子
    public static final ResourceLocation ANION = new ResourceLocation(Main.MODID, "anion");         // 阴离子

    // ==================== 化学功能基团 ====================
    public static final ResourceLocation ACID = new ResourceLocation(Main.MODID, "acid");           // 酸（可提供H⁺）
    public static final ResourceLocation BASE = new ResourceLocation(Main.MODID, "base");           // 碱（可提供OH⁻或接受H⁺）
    /** 盐（§19.34）：电中性离子化合物——正盐 / 酸式盐 / 难溶盐（NaOH 这类碱不算盐） */
    public static final ResourceLocation SALT = new ResourceLocation(Main.MODID, "salt");
    public static final ResourceLocation OXIDIZER = new ResourceLocation(Main.MODID, "oxidizer");   // 氧化剂（反应中得电子）
    public static final ResourceLocation REDUCER = new ResourceLocation(Main.MODID, "reducer");     // 还原剂（反应中失电子）

    // ==================== 有机官能团 ====================
    public static final ResourceLocation ALCOHOL = new ResourceLocation(Main.MODID, "alcohol");     // 醇羟基（-OH）
    public static final ResourceLocation CARBOXYLIC_ACID = new ResourceLocation(Main.MODID, "carboxylic_acid"); // 羧基（-COOH）

    // ==================== 无机物类别 ====================
    public static final ResourceLocation HALIDE = new ResourceLocation(Main.MODID, "halide");       // 卤素阴离子（F⁻/Cl⁻/Br⁻/I⁻）
    public static final ResourceLocation HALOGEN = new ResourceLocation(Main.MODID, "halogen");     // 卤素单质（F₂/Cl₂/Br₂/I₂）
    public static final ResourceLocation METAL = new ResourceLocation(Main.MODID, "metal");         // 金属单质
    public static final ResourceLocation MOLTEN_SALT = new ResourceLocation(Main.MODID, "molten_salt"); // 熔融盐（用于电解）
    public static final ResourceLocation CATALYST = new ResourceLocation(Main.MODID, "catalyst");

    // ==================== 物理性质 ====================
    public static final ResourceLocation VOLATILE = new ResourceLocation(Main.MODID, "volatile");   // 易挥发（沸点较低）
    public static final ResourceLocation PRECIPITATE = new ResourceLocation(Main.MODID, "precipitate"); // 难溶物（沉淀）

    // ==================== 矿物与化学分类 ====================
    public static final ResourceLocation OXIDE = new ResourceLocation(Main.MODID, "oxide");           // 氧化物
    public static final ResourceLocation VALUABLE = new ResourceLocation(Main.MODID, "valuable");     // 有效成分（矿石中有价值的部分）
    public static final ResourceLocation GANGUE = new ResourceLocation(Main.MODID, "gangue");         // 脉石（杂质）

    // ==================== 有机物分类 ====================
    public static final ResourceLocation HYDROCARBON = new ResourceLocation(Main.MODID, "hydrocarbon"); // 烃类
    public static final ResourceLocation ALKANE = new ResourceLocation(Main.MODID, "alkane");           // 烷烃
    public static final ResourceLocation AROMATIC = new ResourceLocation(Main.MODID, "aromatic");       // 芳香烃
    public static final ResourceLocation NONMETAL = new ResourceLocation(Main.MODID, "nonmetal");       // 非金属单质
}