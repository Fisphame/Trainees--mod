package com.pha.trainees.chemistry.particle;

import com.pha.trainees.chemistry.util.IonDisplay;
import com.pha.trainees.util.math.MathT;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings({"deprecation", "removal"})
public class IonType {

    private final ResourceLocation id;
    private final Phase phase;
    private final double molarMass;          // g/mol
    private final double specificHeat;       // J/(mol·K)
    private final double formationEnthalpy;  // ΔHf° kJ/mol
    private final double formationGibbs;     // ΔGf° kJ/mol
    private final int toxicityLevel;         // 0-3
    private final int flameColor;            // ARGB 用于焰色/气体颜色
    private final int displayColor;          // ARGB 物质外观色（渲染 tint 用；§19.15）
    private final Form form;                 // 物质形态（渲染属性，不参与反应判定；§19.15）
    private final Set<ResourceLocation> tags;

    // 可选：用于经验的温度范围或浓度阈值（后续可接入配置）
    private final double minStableTemp;       // 最低稳定温度（K），默认 0
    private final double maxStableTemp;       // 最高稳定温度（K），默认 9999

    private IonType(Builder builder) {
        this.id = builder.id;
        this.phase = builder.phase;
        this.molarMass = builder.molarMass;
        this.specificHeat = builder.specificHeat;
        this.formationEnthalpy = builder.formationEnthalpy;
        this.formationGibbs = builder.formationGibbs;
        this.toxicityLevel = builder.toxicityLevel;
        this.flameColor = builder.flameColor;
        this.displayColor = builder.displayColor;
        // 形态可由 Builder 覆盖；未指定时按物态派生默认形态（§19.15：形态仅渲染属性）
        this.form = builder.form != null ? builder.form : Form.defaultFor(builder.phase);
        this.tags = Set.copyOf(builder.tags);
        this.minStableTemp = builder.minStableTemp;
        this.maxStableTemp = builder.maxStableTemp;
    }

    // ---------- Getters ----------
    public ResourceLocation getId() { return id; }
    public Phase getPhase() { return phase; }
    public double getMolarMass() { return molarMass; }
    public double getSpecificHeat() { return specificHeat; }
    public double getFormationEnthalpy() { return formationEnthalpy; }
    public double getFormationGibbs() { return formationGibbs; }
    public int getToxicityLevel() { return toxicityLevel; }
    public int getFlameColor() { return flameColor; }
    /** 物质外观色（ARGB），供物品 tint / JEI 条目渲染（§19.15） */
    public int getDisplayColor() { return displayColor; }
    /** 物质形态（渲染属性；默认由物态派生，可被 Builder 覆盖） */
    public Form getForm() { return form; }
    public Set<ResourceLocation> getTags() { return tags; }

    /** 稳定温度下限（K）——导出/展示需要，之前只给了判定方法没有 getter */
    public double getMinStableTemp() { return minStableTemp; }

    /** 稳定温度上限（K） */
    public double getMaxStableTemp() { return maxStableTemp; }

    public boolean isStableAt(double temperatureKelvin) {
        return MathT.isInInterval(temperatureKelvin, minStableTemp, maxStableTemp);
    }

    /**
     * 化学式显示名（原子数下标、价态上标），用于指令回复与反应图等玩家可见文本。
     * 例：so4_2minus → SO₄²⁻、h2o → H₂O、fe_3 → Fe³⁺
     */
    public String getDisplayName() {
        return IonDisplay.format(id.getPath());
    }

    // 判断是否匹配标签（用于有机官能团匹配）
    public boolean hasTag(ResourceLocation tag) {
        return tags.contains(tag);
    }

    @Override
    public String toString() {
        return id.toString();
    }

    // ---------- 物质形态（渲染属性，§19.15） ----------

    /**
     * 物质形态：决定物品/JEI 条目用哪张底图（白/灰阶贴图 + tint 上色）。
     * <b>仅渲染语义，不参与任何反应判定</b>——同一物质的形态差异不应分裂成多个 {@link IonType}。
     */
    public enum Form {
        /** 粉末 */
        POWDER,
        /** 晶体 */
        CRYSTAL,
        /** 颗粒 */
        GRANULE,
        /** 块状/锭状 */
        BULK,
        /** 纯液体 */
        LIQUID,
        /** 溶液 */
        SOLUTION,
        /** 气体 */
        GAS;

        /** 按物态派生默认形态（SOLID 默认粉末，可按物质覆盖为晶体/颗粒/块） */
        public static Form defaultFor(Phase phase) {
            return switch (phase) {
                case GAS -> GAS;
                case LIQUID -> LIQUID;
                case AQUEOUS -> SOLUTION;
                case SOLID -> POWDER;
            };
        }

        /** 语言键（tooltip / JEI 条目 / 物品提示共用） */
        public String getTranslationKey() {
            return "trainees.form." + name().toLowerCase();
        }
    }

    // ---------- Builder ----------
    public static class Builder {
        private final ResourceLocation id;
        private final Phase phase;
        private double molarMass = 0;
        private double specificHeat = 1.0; // 默认值
        private double formationEnthalpy = 0;
        private double formationGibbs = 0;
        private int toxicityLevel = 0;
        private int flameColor = 0xFFFFFF; // 白色默认
        private int displayColor = 0xFFFFFF; // 白 = 不上色（渲染为贴图原色）
        private Form form = null;          // null = 按物态派生
        private final Set<ResourceLocation> tags = new HashSet<>();
        private double minStableTemp = 0;
        private double maxStableTemp = 9999;

        public Builder(ResourceLocation id, Phase phase) {
            this.id = id;
            this.phase = phase;
        }

        public Builder molarMass(double val) { this.molarMass = val; return this; }
        public Builder specificHeat(double val) { this.specificHeat = val; return this; }
        public Builder formationEnthalpy(double val) { this.formationEnthalpy = val; return this; }
        public Builder formationGibbs(double val) { this.formationGibbs = val; return this; }
        public Builder toxicityLevel(int val) { this.toxicityLevel = val; return this; }
        public Builder flameColor(int val) { this.flameColor = val; return this; }
        /** 物质外观色（RGB，0xRRGGBB；用于物品 tint 与 JEI 条目；与 flameColor 分工） */
        public Builder displayColor(int rgb) { this.displayColor = 0xFF000000 | (rgb & 0xFFFFFF); return this; }
        /** 物质形态（渲染属性；不指定则按物态派生） */
        public Builder form(Form val) { this.form = val; return this; }
        public Builder tag(ResourceLocation tag) { this.tags.add(tag); return this; }
        public Builder stableRange(double minK, double maxK) { this.minStableTemp = minK; this.maxStableTemp = maxK; return this; }

        public IonType build() {
            return new IonType(this);
        }
    }
}