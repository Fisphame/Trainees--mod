package com.pha.trainees.item;

import com.pha.trainees.util.game.ChemistryTools;
import com.pha.trainees.util.game.Tools;
import com.pha.trainees.util.interfaces.IChemistry;
import com.pha.trainees.util.interfaces.IHoverText;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChemicalItem {

    public static class ChemistryBookItem extends BookItem {


        public ChemistryBookItem(Properties properties) {
            super(properties);
        }
    }

    public static final Map<String, Double> ELEMENT_MASS = new HashMap<>();
    static {
        // 使用最新的 IUPAC 标准原子量（基于2024年及之前的数据）
        // 数据来源：IUPAC 官方 Gold Book 及 CIAAW 最新修订
        // 参考: IUPAC Gold Book (https://goldbook.iupac.org) [citation:1][citation:6]
        // 参考: IUPAC CIAAW 2024年修订 (Gd: 157.249, Lu: 174.96669, Zr: 91.222) [citation:7][citation:10]

        // 第一周期
        ELEMENT_MASS.put("H", 1.0080);      // 氢 [citation:2][citation:8]
        ELEMENT_MASS.put("He", 4.0026);     // 氦 [citation:2][citation:8]

        // 第二周期
        ELEMENT_MASS.put("Li", 6.94);       // 锂 (6.938-6.997) 常用 6.94
        ELEMENT_MASS.put("Be", 9.0122);     // 铍 [citation:2]
        ELEMENT_MASS.put("B", 10.81);       // 硼 (10.806-10.821) 常用 10.81 [citation:2]
        ELEMENT_MASS.put("C", 12.011);      // 碳 [citation:2][citation:8]
        ELEMENT_MASS.put("N", 14.007);      // 氮 [citation:2][citation:8]
        ELEMENT_MASS.put("O", 15.999);      // 氧 [citation:2][citation:8]
        ELEMENT_MASS.put("F", 18.998);      // 氟 [citation:2][citation:8]
        ELEMENT_MASS.put("Ne", 20.18);      // 氖 [citation:2]

        // 第三周期
        ELEMENT_MASS.put("Na", 22.990);     // 钠 [citation:2][citation:8]
        ELEMENT_MASS.put("Mg", 24.305);     // 镁 [citation:2][citation:8]
        ELEMENT_MASS.put("Al", 26.982);     // 铝 [citation:2][citation:8]
        ELEMENT_MASS.put("Si", 28.085);     // 硅 [citation:2][citation:8]
        ELEMENT_MASS.put("P", 30.974);      // 磷 [citation:2][citation:8]
        ELEMENT_MASS.put("S", 32.06);       // 硫 [citation:2][citation:8]
        ELEMENT_MASS.put("Cl", 35.45);      // 氯 [citation:2][citation:8]
        ELEMENT_MASS.put("Ar", 39.95);      // 氩 [citation:2]

        // 第四周期
        ELEMENT_MASS.put("K", 39.098);      // 钾 [citation:8]
        ELEMENT_MASS.put("Ca", 40.078);     // 钙 [citation:8]
        ELEMENT_MASS.put("Sc", 44.956);     // 钪
        ELEMENT_MASS.put("Ti", 47.867);     // 钛
        ELEMENT_MASS.put("V", 50.942);      // 钒
        ELEMENT_MASS.put("Cr", 51.996);     // 铬
        ELEMENT_MASS.put("Mn", 54.938);     // 锰
        ELEMENT_MASS.put("Fe", 55.845);     // 铁 [citation:8]
        ELEMENT_MASS.put("Co", 58.933);     // 钴
        ELEMENT_MASS.put("Ni", 58.693);     // 镍
        ELEMENT_MASS.put("Cu", 63.546);     // 铜 [citation:3][citation:8]
        ELEMENT_MASS.put("Zn", 65.38);      // 锌 [citation:8]
        ELEMENT_MASS.put("Ga", 69.723);     // 镓
        ELEMENT_MASS.put("Ge", 72.630);     // 锗
        ELEMENT_MASS.put("As", 74.922);     // 砷
        ELEMENT_MASS.put("Se", 78.971);     // 硒
        ELEMENT_MASS.put("Br", 79.904);     // 溴
        ELEMENT_MASS.put("Kr", 83.798);     // 氪

        // 第五周期
        ELEMENT_MASS.put("Rb", 85.468);     // 铷
        ELEMENT_MASS.put("Sr", 87.62);      // 锶
        ELEMENT_MASS.put("Y", 88.906);      // 钇
        ELEMENT_MASS.put("Zr", 91.222);     // 锆 (2024年修订) [citation:7][citation:10]
        ELEMENT_MASS.put("Nb", 92.906);     // 铌
        ELEMENT_MASS.put("Mo", 95.95);      // 钼
        ELEMENT_MASS.put("Tc", 98.0);       // 锝 (放射性，无稳定同位素)
        ELEMENT_MASS.put("Ru", 101.07);     // 钌
        ELEMENT_MASS.put("Rh", 102.91);     // 铑
        ELEMENT_MASS.put("Pd", 106.42);     // 钯
        ELEMENT_MASS.put("Ag", 107.87);     // 银 [citation:8]
        ELEMENT_MASS.put("Cd", 112.41);     // 镉
        ELEMENT_MASS.put("In", 114.82);     // 铟
        ELEMENT_MASS.put("Sn", 118.71);     // 锡
        ELEMENT_MASS.put("Sb", 121.76);     // 锑
        ELEMENT_MASS.put("Te", 127.60);     // 碲
        ELEMENT_MASS.put("I", 126.90);      // 碘 [citation:8]
        ELEMENT_MASS.put("Xe", 131.29);     // 氙

        // 第六周期
        ELEMENT_MASS.put("Cs", 132.91);     // 铯
        ELEMENT_MASS.put("Ba", 137.33);     // 钡
        ELEMENT_MASS.put("La", 138.91);     // 镧
        ELEMENT_MASS.put("Ce", 140.12);     // 铈
        ELEMENT_MASS.put("Pr", 140.91);     // 镨
        ELEMENT_MASS.put("Nd", 144.24);     // 钕
        ELEMENT_MASS.put("Pm", 145.0);      // 钷 (放射性)
        ELEMENT_MASS.put("Sm", 150.36);     // 钐
        ELEMENT_MASS.put("Eu", 151.96);     // 铕
        ELEMENT_MASS.put("Gd", 157.249);    // 钆 (2024年修订) [citation:7][citation:10]
        ELEMENT_MASS.put("Tb", 158.93);     // 铽
        ELEMENT_MASS.put("Dy", 162.50);     // 镝
        ELEMENT_MASS.put("Ho", 164.93);     // 钬
        ELEMENT_MASS.put("Er", 167.26);     // 铒
        ELEMENT_MASS.put("Tm", 168.93);     // 铥
        ELEMENT_MASS.put("Yb", 173.05);     // 镱
        ELEMENT_MASS.put("Lu", 174.96669);  // 镥 (2024年修订) [citation:7][citation:10]
        ELEMENT_MASS.put("Hf", 178.49);     // 铪
        ELEMENT_MASS.put("Ta", 180.95);     // 钽
        ELEMENT_MASS.put("W", 183.84);      // 钨
        ELEMENT_MASS.put("Re", 186.21);     // 铼
        ELEMENT_MASS.put("Os", 190.23);     // 锇
        ELEMENT_MASS.put("Ir", 192.22);     // 铱
        ELEMENT_MASS.put("Pt", 195.08);     // 铂
        ELEMENT_MASS.put("Au", 196.97);     // 金
        ELEMENT_MASS.put("Hg", 200.59);     // 汞 [citation:8]
        ELEMENT_MASS.put("Tl", 204.38);     // 铊
        ELEMENT_MASS.put("Pb", 207.2);      // 铅 [citation:8]
        ELEMENT_MASS.put("Bi", 208.98);     // 铋
        ELEMENT_MASS.put("Po", 209.0);      // 钋 (放射性)
        ELEMENT_MASS.put("At", 210.0);      // 砹 (放射性)
        ELEMENT_MASS.put("Rn", 222.0);      // 氡 (放射性)

        // 第七周期 (放射性元素，取最稳定同位素质量数)
        ELEMENT_MASS.put("Fr", 223.0);      // 钫
        ELEMENT_MASS.put("Ra", 226.0);      // 镭
        ELEMENT_MASS.put("Ac", 227.0);      // 锕
        ELEMENT_MASS.put("Th", 232.04);     // 钍
        ELEMENT_MASS.put("Pa", 231.04);     // 镤
        ELEMENT_MASS.put("U", 238.03);      // 铀
        ELEMENT_MASS.put("Np", 237.0);      // 镎
        ELEMENT_MASS.put("Pu", 244.0);      // 钚
        ELEMENT_MASS.put("Am", 243.0);      // 镅
        ELEMENT_MASS.put("Cm", 247.0);      // 锔
        ELEMENT_MASS.put("Bk", 247.0);      // 锫
        ELEMENT_MASS.put("Cf", 251.0);      // 锎
        ELEMENT_MASS.put("Es", 252.0);      // 锿
        ELEMENT_MASS.put("Fm", 257.0);      // 镄
        ELEMENT_MASS.put("Md", 258.0);      // 钔
        ELEMENT_MASS.put("No", 259.0);      // 锘
        ELEMENT_MASS.put("Lr", 266.0);      // 铹
        ELEMENT_MASS.put("Rf", 267.0);      // 𬬻
        ELEMENT_MASS.put("Db", 268.0);      // 𬭊
        ELEMENT_MASS.put("Sg", 269.0);      // 𬭳
        ELEMENT_MASS.put("Bh", 270.0);      // 𬭛
        ELEMENT_MASS.put("Hs", 269.0);      // 𬭶
        ELEMENT_MASS.put("Mt", 278.0);      // 鿏
        ELEMENT_MASS.put("Ds", 281.0);      // 𫟼
        ELEMENT_MASS.put("Rg", 282.0);      // 𬬭
        ELEMENT_MASS.put("Cn", 285.0);      // 鎶
        ELEMENT_MASS.put("Nh", 286.0);      // 鉨
        ELEMENT_MASS.put("Fl", 289.0);      // 𫓧
        ELEMENT_MASS.put("Mc", 290.0);      // 镆
        ELEMENT_MASS.put("Lv", 293.0);      // 𫟷
        ELEMENT_MASS.put("Ts", 294.0);      // 鿬
        ELEMENT_MASS.put("Og", 294.0);      // 气奥

        // 钅鸡 (Ji) - 原子序数 119，与 Na 同族
        ELEMENT_MASS.put("Ji", 298.87);
        // 石黑 (Bp) - 原子序数 117，与 Cl 同族
        ELEMENT_MASS.put("Bp", 294.24);
    }

    public static class BaseChemicalItem extends Item implements IChemistry, IHoverText {
        private final String id;
        private final String formula;
        private final double molarMass;
        public BaseChemicalItem(Properties properties, String id, String formula) {
            super(properties);
            this.id = id;
            this.formula = formula;
            this.molarMass = ChemistryTools.calculateMolarMassApproximation(formula, ELEMENT_MASS);
        }

        public String getFormula() {
            return formula;
        }

        public double getMolarMass() {
            return molarMass;
        }

        @Override
        public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                    @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag flag) {
            super.appendHoverText(stack, level, tooltipComponents, flag);
            addChemicalFormula(stack, level, tooltipComponents, flag, id);
            addChemicalMolarMass(stack, level, tooltipComponents, flag, molarMass);
        }
    }

    public static class BaseChemicalBlockItem extends BlockItem implements IChemistry, IHoverText {
        private final String id;
        private final String formula;
        public BaseChemicalBlockItem(Block block, Properties properties, String id, String formula) {
            super(block, properties);
            this.id = id;
            this.formula = formula;
        }

        public String getFormula() {
            return formula;
        }

        @Override
        public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                    @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag flag) {
            super.appendHoverText(stack, level, tooltipComponents, flag);
            addChemicalFormula(stack, level, tooltipComponents, flag, id);
        }
    }




    /*
        Ji   钅鸡（以下称鸡）  原子序数119   金属元素    +1
        Bp   石黑（以下称黑）  原子序数117   非金属元素   +1 -1 -3 -5 -7
        JiBp   黑化鸡
        JiOH   氢氧化鸡
        HBp    黑化氢   相酸：黑化氢的水溶液
        Ji2O   氧化鸡
        Ji2SO4 硫酸鸡
        JiNO3  硝酸鸡
        Ji2CO3 碳酸鸡
        BpNH4  铵黑

        反应：
            Bp2 + H2O == HBpO + HBp
            Bp2 + 2O2 =点燃= 2BpO2
            2HBpO =光照= 2HBp + O2↑
            2BpO2 + 2H2O == 2HBpO3 + H2↑
            2BpO2 + O2 =催化剂= 2BpO3
            2BpO3 + 2H2O == 2HBpO4 + H2↑



        1.  Ji + Bp == JiBp
        2.  2Ji + 2H2O == 2JiOH + H2↑

        4.
        4.  JiOH + HBp == JiBp + H2O
        5.  2Ji + O2 =点燃= Ji2O
        6.  Ji2SO4 + 2Na == Na2SO4 + Ji2


    */

    // ₁₂₃₄₅₆₇₈₉₁₀

    // 氧化鸡
    public static class Ji2O extends BaseChemicalItem {
        public Ji2O(Properties p_41383_) {
            super(p_41383_, "ji2o", "Ji₂O");
        }
    }
    public static class Ji2O_B extends BaseChemicalBlockItem {
        public Ji2O_B(Block block, Properties properties) {
            super(block, properties, "ji2o", "Ji₂O");
        }
    }
    public static class Waxed_Ji2O_B extends BaseChemicalBlockItem {
        public Waxed_Ji2O_B(Block block, Properties properties) {
            super(block, properties, "ji2o2", "Ji₂O");
        }
    }

    // 过氧化鸡
    public static class Ji2O2 extends BaseChemicalItem {
        public Ji2O2(Properties p_41383_) {
            super(p_41383_, "ji2o2", "Ji₂O₂");
        }
    }
    public static class Ji2O2_B extends BaseChemicalBlockItem {
        public Ji2O2_B(Block block, Properties properties) {
            super(block, properties, "ji2o2", "Ji₂O₂");
        }
    }


    // 二氧化黑
    public static class BpO2 extends BaseChemicalItem {
        public BpO2(Properties p_41383_) {
            super(p_41383_, "bpo2", "BpO₂");
        }
    }

    // 三氧化黑
    public static class BpO3 extends BaseChemicalItem {
        public BpO3(Properties p_41383_) {
            super(p_41383_, "bpo3", "BpO₃");
        }
    }

    // 黑化氢
    public static class HBp extends BaseChemicalItem {
        public HBp(Properties p_41383_) {
            super(p_41383_, "hbp", "HBp");
        }
    }

    // 次黑酸
    public static class HBpO extends BaseChemicalItem {
        public HBpO(Properties p_41383_) {
            super(p_41383_, "hbpo", "HBpO");
        }

        @Override
        public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
            return levelOn(stack, entity);
        }
    }

    // 黑酸
    public static class HBpO3 extends BaseChemicalItem {
        public HBpO3(Properties p_41383_) {
            super(p_41383_, "hbpo3", "HBpO₃");
        }
    }

    // 高黑酸
    public static class HBpO4 extends BaseChemicalItem {
        public HBpO4(Properties p_41383_) {
            super(p_41383_, "hbpo4", "HBpO₄");
        }
    }

    // 氢氧化鸡
    public static class JiOH extends BaseChemicalItem {
        public JiOH(Properties p_41383_) {
            super(p_41383_, "jioh", "JiOH");
        }
    }
    public static class JiOH_B extends BaseChemicalBlockItem {
        public JiOH_B(Block block, Properties properties) {
            super(block, properties, "jioh", "JiOH");
        }
    }

    // 黑化鸡
    public static class JiBp extends BaseChemicalItem {
        public JiBp(Properties p_41383_) {
            super(p_41383_, "jibp", "JiBp");
        }
    }
    public static class JiBp_B extends BaseChemicalBlockItem {

        public JiBp_B(Block block, Properties properties) {
            super(block, properties, "jibp", "JiBp");
        }
    }

    // 次黑酸鸡
    public static class JiBpO extends BaseChemicalItem {
        public JiBpO(Properties p_41383_) {
            super(p_41383_, "jibpo", "JiBpO");
        }
    }

    // 黑酸鸡
    public static class JiBpO3 extends BaseChemicalItem {
        public JiBpO3(Properties p_41383_) {
            super(p_41383_, "jibpo3", "JiBpO₃");
        }
    }

    // 高黑酸鸡
    public static class JiBpO4 extends BaseChemicalItem  {
        public JiBpO4(Properties p_41383_) {
            super(p_41383_, "jibpo4", "JiBpO₄");
        }
    }
}


