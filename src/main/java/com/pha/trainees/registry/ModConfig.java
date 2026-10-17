package com.pha.trainees.registry;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class ModConfig {
    public static class Common {
        public final ForgeConfigSpec.ConfigValue<String> deepSeekApiKey;

        // ---- 汲取方块（§19.21 主动推送） ----
        public final ForgeConfigSpec.BooleanValue absorbPushEnabled;
        public final ForgeConfigSpec.IntValue absorbPushInterval;
        public final ForgeConfigSpec.IntValue absorbPushBatchSize;
        public final ForgeConfigSpec.BooleanValue absorbNeighborPush;

        // ---- 打包机（§19.18 / §19.25 自动化） ----
        public final ForgeConfigSpec.IntValue packerEnergyPerBottle;
        public final ForgeConfigSpec.IntValue packerEnergyCapacity;
        public final ForgeConfigSpec.IntValue packerCheckInterval;

        // ---- 搅拌器（§19.29：附着件，只改传热/传质，不动反应速率） ----
        public final ForgeConfigSpec.IntValue stirrerEnergyPerTick;
        public final ForgeConfigSpec.IntValue stirrerEnergyCapacity;
        public final ForgeConfigSpec.DoubleValue stirrerHeatFactor;
        public final ForgeConfigSpec.DoubleValue stirrerGasFactor;

        // ---- 电加热夹套（§19.29 第 2 步：附着件形态热源，按交付热量计费） ----
        public final ForgeConfigSpec.IntValue jacketEnergyCapacity;
        public final ForgeConfigSpec.DoubleValue jacketEfficiency;
        public final ForgeConfigSpec.DoubleValue jacketAreaBonus;

        // ---- 反应釜残留与密封釜头（§19.30.2：可拆盖 vs 全焊） ----
        public final ForgeConfigSpec.DoubleValue reactorResiduePerLiterMoles;
        public final ForgeConfigSpec.DoubleValue sealedHeadResidueFactor;

        // ---- 腐蚀（§19.32：默认关；打开后是硬核/真实模式） ----
        public final ForgeConfigSpec.BooleanValue corrosionEnabled;
        public final ForgeConfigSpec.DoubleValue corrosionBaseRate;
        public final ForgeConfigSpec.DoubleValue corrosionWarningThreshold;
        public final ForgeConfigSpec.DoubleValue corrosionMaterialMolesPerLiter;

        Common(ForgeConfigSpec.Builder builder) {
            builder.comment("DeepSeek API Settings").push("deepseek");
            deepSeekApiKey = builder
                    .comment("Your DeepSeek API Key (get from platform.deepseek.com)")
                    .define("apiKey", "");
            builder.pop();

            builder.comment("Absorb Block（汲取方块）：主动把内容物弹回 ME 网络/邻接容器，替代输出总线轮询")
                    .push("absorb");
            absorbPushEnabled = builder
                    .comment("启用主动推送（关闭则退回旧行为：只由外部总线抽取）")
                    .define("pushEnabled", true);
            absorbPushInterval = builder
                    .comment("推送周期（tick）：每隔这么多 tick 尝试弹出一批")
                    .defineInRange("pushInterval", 8, 1, 200);
            absorbPushBatchSize = builder
                    .comment("单次推送上限（个/批）")
                    .defineInRange("pushBatchSize", 64, 1, 4096);
            absorbNeighborPush = builder
                    .comment("ME 网络不可用（离线/无电）时，回退推送到邻接容器",
                            "为避免回环，推送目标会排除本模组机器与其他汲取方块")
                    .define("neighborPush", true);
            builder.pop();

            // ---- 打包机（§19.18 / §19.25 自动化：自驱 + FE 门槛） ----
            builder.comment("Packer（打包机）：自动质检灌装。1 FE = 1 J（与电解槽同口径）")
                    .push("packer");
            packerEnergyPerBottle = builder
                    .comment("每瓶电费（FE）。默认 20000 FE = 20 kJ（质检 + 灌装的机械功，远低于电解的 ΔG 量级）")
                    .defineInRange("energyPerBottle", 20000, 0, 100_000_000);
            packerEnergyCapacity = builder
                    .comment("内部电缓冲上限（FE）")
                    .defineInRange("energyCapacity", 400000, 1000, 1_000_000_000);
            packerCheckInterval = builder
                    .comment("自驱检查周期（tick）：每隔这么多 tick 尝试打包一次")
                    .defineInRange("checkInterval", 20, 1, 400);
            builder.pop();

            // ---- 搅拌器（§19.29） ----
            builder.comment("Stirrer（搅拌器）：贴在容器邻面，有电就转。只提升传热与气液传质，不改变反应速率（我们的模型本来就是理想混合）")
                    .push("stirrer");
            stirrerEnergyPerTick = builder
                    .comment("运转电费（FE/tick，1 FE = 1 J）。0 = 不耗电（调试用）")
                    .defineInRange("energyPerTick", 20, 0, 1_000_000);
            stirrerEnergyCapacity = builder
                    .comment("内部电缓冲上限（FE）")
                    .defineInRange("energyCapacity", 20000, 100, 100_000_000);
            stirrerHeatFactor = builder
                    .comment("传热系数倍率（运转时）")
                    .defineInRange("heatFactor", 1.5, 1.0, 20.0);
            stirrerGasFactor = builder
                    .comment("气液传质倍率（容器 ↔ 气体网格的交换速率）")
                    .defineInRange("gasFactor", 2.0, 1.0, 20.0);
            builder.pop();

            // ---- 电加热夹套（§19.29 第 2 步） ----
            builder.comment("Electric Jacket（电加热夹套）：贴在容器邻面，从侧面供热。现实电阻加热 ≤400 ℃、热效率 <85%")
                    .push("jacket");
            jacketEnergyCapacity = builder
                    .comment("内部电缓冲上限（FE）")
                    .defineInRange("energyCapacity", 200000, 1000, 1_000_000_000);
            jacketEfficiency = builder
                    .comment("热效率 η（电→热）：交付 1 J 热量需付 1/η J 电")
                    .defineInRange("efficiency", 0.85, 0.05, 1.0);
            jacketAreaBonus = builder
                    .comment("单面夹套的传热面积加成（加性：1 面 = 1+该值，2 面 = 1+2×该值；搅拌器那种改系数的部件仍按乘性）")
                    .defineInRange("areaBonus", 0.6, 0.05, 5.0);
            builder.pop();

            // ---- 反应釜残留与密封釜头（§19.30.2） ----
            builder.comment("Reactor residue & sealed head（反应釜残留 / 密封釜头）：可拆盖 vs 全焊的真实取舍")
                    .push("reactor");
            reactorResiduePerLiterMoles = builder
                    .comment("基础残留（mol/L）：取样/出料取不走的死体积（挂壁液膜）",
                            "烧杯 1 L → 0.05 mol，低于它自己的 0.1 mol 取样下限 ⇒ 烧杯行为不变")
                    .defineInRange("residuePerLiterMoles", 0.05, 0.0, 10.0);
            sealedHeadResidueFactor = builder
                    .comment("密封釜头（全焊）的残留倍率：死角冲不到，残留 = 敞口盖体的这么多倍")
                    .defineInRange("sealedHeadResidueFactor", 3.0, 1.0, 20.0);
            builder.pop();

            // ---- 腐蚀（§19.32） ----
            builder.comment("Corrosion（腐蚀）：默认关。打开后热碱吃玻璃、Cl⁻ 点蚀不锈钢、酸溶钢（冒氢）都会真的发生")
                    .push("corrosion");
            corrosionEnabled = builder
                    .comment("是否启用腐蚀（默认关：只算不做的老规矩——打开前一切照旧）")
                    .define("enabled", false);
            corrosionBaseRate = builder
                    .comment("基础速率（每秒腐蚀度）：在'1 mol/L、293 K、敏感度 1'下每秒吃掉容器的比例",
                            "标定目标：消毒液工况（稀碱 ~0.5 mol/L、≈335 K）跑一批只吃几个百分点")
                    .defineInRange("baseRatePerSecond", 0.002, 0.0, 1.0);
            corrosionWarningThreshold = builder
                    .comment("预警阈值（腐蚀度）：达到即向附近玩家提示")
                    .defineInRange("warningThreshold", 0.8, 0.1, 1.0);
            corrosionMaterialMolesPerLiter = builder
                    .comment("每升容积对应的'容器材料摩尔数'：用于把腐蚀度换算成钢溶出量（Fe + 2H⁺ → Fe²⁺ + H₂）")
                    .defineInRange("materialMolesPerLiter", 1.0, 0.01, 100.0);
            builder.pop();
        }
    }

    public static final Common COMMON;
    public static final ForgeConfigSpec COMMON_SPEC;

    static {
        final Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON_SPEC = specPair.getRight();
        COMMON = specPair.getLeft();
    }
}