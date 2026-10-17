package com.pha.trainees.chemistry.item;

import com.pha.trainees.chemistry.material.IMaterial;
import com.pha.trainees.chemistry.material.SimpleMaterial;
import com.pha.trainees.chemistry.material.FixedCompositionTable;
import com.pha.trainees.chemistry.material.SubstanceBlueprint;
import com.pha.trainees.chemistry.material.SubstanceBlueprintRegistry;
import com.pha.trainees.chemistry.particle.IonType;
import com.pha.trainees.chemistry.util.IonDisplay;
import com.pha.trainees.config.ChemConfig;
import com.pha.trainees.registry.ModChemistry;
import com.pha.trainees.util.interfaces.IHoverText;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 物质基类物品
 * 支持蓝图引用（静态配方）和动态混合物两种存储模式
 */
public class SubstanceItem extends Item implements IHoverText {

    private static final DecimalFormat DF = new DecimalFormat("#0.000");

    public SubstanceItem(Properties properties) {
        super(properties);
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 从蓝图创建物品堆（蓝图模式）。
     * NBT 仅存 blueprint ID + units，成分在读取时实时计算，
     * 因此难度变更后旧物品会自动反映新难度（蓝本 §8.3）。
     */
    public static ItemStack fromBlueprint(ResourceLocation blueprintId, double units) {
        SubstanceBlueprint blueprint = SubstanceBlueprintRegistry.get(blueprintId);
        if (blueprint == null) return ItemStack.EMPTY;

        ItemStack stack = new ItemStack(ModChemistry.ModChemistryItems.SUBSTANCE.get());
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("blueprint", blueprintId.toString());
        tag.putDouble("units", units > 0 ? units : 1.0);
        return stack;
    }

    /**
     * 从成分映射创建物品堆（动态混合物）
     */
    public static ItemStack fromComposition(Map<IonType, Double> composition) {
        ItemStack stack = new ItemStack(ModChemistry.ModChemistryItems.SUBSTANCE.get());
        CompoundTag tag = stack.getOrCreateTag();
        ListTag compositionList = new ListTag();
        for (Map.Entry<IonType, Double> entry : composition.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("ion", entry.getKey().getId().toString());
            entryTag.putDouble("moles", entry.getValue());
            compositionList.add(entryTag);
        }
        tag.put("composition", compositionList);
        return stack;
    }

    /**
     * 创建纯净物（快捷方法）
     */
    public static ItemStack ofPure(IonType ion, double moles) {
        Map<IonType, Double> comp = new HashMap<>();
        comp.put(ion, moles);
        return fromComposition(comp);
    }

    // ==================== 数据提取 ====================

    /**
     * 从物品堆中提取成分映射
     */
    public static Map<IonType, Double> getComposition(ItemStack stack) {
        // 一级物质（定组成真物品）：成分来自注册表而非 NBT（§19.15）——先查表，未命中才走下面两条 NBT 路径
        Map<IonType, Double> fixed = FixedCompositionTable.compositionOf(stack.getItem()).orElse(null);
        if (fixed != null) return fixed;

        CompoundTag tag = stack.getTag();
        if (tag == null) return Map.of();

        // 蓝图模式：读取时实时解析蓝图并套用当前难度乘数
        if (tag.contains("blueprint")) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("blueprint"));
            if (id != null) {
                SubstanceBlueprint blueprint = SubstanceBlueprintRegistry.get(id);
                if (blueprint != null) {
                    double units = tag.getDouble("units");
                    if (units <= 0) units = 1.0;
                    return blueprint.createComposition(units, getValuableMultiplier(), getGangueMultiplier());
                }
            }
            return Map.of();
        }

        // 动态混合物模式
        if (tag.contains("composition")) {
            ListTag list = tag.getList("composition", Tag.TAG_COMPOUND);
            Map<IonType, Double> result = new HashMap<>();
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                String idStr = entry.getString("ion");
                double moles = entry.getDouble("moles");
                if (moles > 1e-9) {
                    ResourceLocation id = ResourceLocation.tryParse(idStr);
                    if (id != null) {
                        IonType ion = ModChemistry.ModIons.getById(id);
                        if (ion != null) {
                            result.put(ion, moles);
                        }
                    }
                }
            }
            return Map.copyOf(result);
        }

        return Map.of();
    }

    /**
     * 从物品堆中提取总摩尔数
     */
    public static double getTotalMoles(ItemStack stack) {
        return getComposition(stack).values().stream().mapToDouble(Double::doubleValue).sum();
    }

    /**
     * 从物品堆中创建 IMaterial 实例
     */
    public static IMaterial getMaterial(ItemStack stack) {
        Map<IonType, Double> comp = getComposition(stack);
        if (comp.isEmpty()) {
            return null;
        }
        return new SimpleMaterial(comp);
    }

    /**
     * 检查是否为蓝图模式
     */
    public static boolean isBlueprint(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains("blueprint");
    }

    // ==================== 难度乘数解析 ====================

    /**
     * 根据当前难度档位获取有效成分乘数。
     * 统一走档位解析入口（§7.5 方案 C）：非 CUSTOM 读预设表，CUSTOM 读配置乘数。
     */
    private static double getValuableMultiplier() {
        return ChemConfig.DifficultyPresets.valuableMultiplier();
    }

    /**
     * 根据当前难度档位获取脉石乘数。
     */
    private static double getGangueMultiplier() {
        return ChemConfig.DifficultyPresets.gangueMultiplier();
    }
    // ==================== 显示相关 ====================

    @Override
    public Component getName(ItemStack stack) {
        // 一级物质用注册名（语言键），不派生化学式
        if (FixedCompositionTable.contains(stack.getItem())) {
            return super.getName(stack);
        }
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("blueprint")) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("blueprint"));
            if (id != null) {
                SubstanceBlueprint blueprint = SubstanceBlueprintRegistry.get(id);
                if (blueprint != null) {
                    return Component.literal(blueprint.getDisplayName());
                }
            }
        }
        // 动态成分模式：单组分显示化学式；多组分显示"最大组分 等 n 种"（§19.15 物质条目命名）
        Map<IonType, Double> comp = getComposition(stack);
        if (comp.isEmpty()) {
            return super.getName(stack);
        }
        if (comp.size() == 1) {
            IonType ion = comp.keySet().iterator().next();
            return Component.literal(IonDisplay.format(ion.getId().getPath()));
        }
        Map.Entry<IonType, Double> dominant = comp.entrySet().stream()
                .max(Map.Entry.comparingByValue()).orElse(null);
        String main = dominant == null ? "?" : IonDisplay.format(dominant.getKey().getId().getPath());
        return Component.translatable("item.trainees.substance.mixture", main, comp.size());
    }

    /**
     * 物品堆的表现形态（§19.15 渲染属性）：单组分取该物质形态；多组分取最大组分；空堆兜底粉末。
     */
    public static IonType.Form getForm(ItemStack stack) {
        Map<IonType, Double> comp = getComposition(stack);
        if (comp.isEmpty()) return IonType.Form.POWDER;
        return comp.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey().getForm())
                .orElse(IonType.Form.POWDER);
    }

    /**
     * 物品堆的显示颜色（ARGB）：单组分取该物质 {@code displayColor}；
     * 多组分按摩尔分数加权——在**线性空间**加权后再做 gamma 校正（避免直接平均导致偏暗）。
     * 白色（0xFFFFFFFF，未指定颜色）视作"贴图原色"，混色时按白色参与。
     */
    public static int getDisplayColor(ItemStack stack) {
        Map<IonType, Double> comp = getComposition(stack);
        if (comp.isEmpty()) return 0xFFFFFFFF;
        if (comp.size() == 1) {
            return comp.keySet().iterator().next().getDisplayColor();
        }
        double total = comp.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total <= 0) return 0xFFFFFFFF;
        double r = 0, g = 0, b = 0;
        for (Map.Entry<IonType, Double> e : comp.entrySet()) {
            double w = e.getValue() / total;
            int argb = e.getKey().getDisplayColor();
            r += toLinear((argb >> 16) & 0xFF) * w;
            g += toLinear((argb >> 8) & 0xFF) * w;
            b += toLinear(argb & 0xFF) * w;
        }
        return 0xFF000000 | (toSrgb(r) << 16) | (toSrgb(g) << 8) | toSrgb(b);
    }

    /** sRGB 通道 → 线性（粗略 gamma 2.2） */
    private static double toLinear(int channel) {
        return Math.pow(channel / 255.0, 2.2);
    }

    /** 线性 → sRGB 通道（0~255） */
    private static int toSrgb(double linear) {
        int v = (int) Math.round(Math.pow(Math.max(0, Math.min(1, linear)), 1 / 2.2) * 255);
        return Math.max(0, Math.min(255, v));
    }

    /** 形态的语言键（供 tooltip / JEI 条目复用） */
    public static String formKey(IonType.Form form) {
        return form.getTranslationKey();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        Map<IonType, Double> comp = getComposition(stack);
        if (comp.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.trainees.substance.empty"));
            return;
        }

        double total = comp.values().stream().mapToDouble(Double::doubleValue).sum();
        tooltip.add(Component.translatable("tooltip.trainees.substance.total", DF.format(total)));
        tooltip.add(Component.translatable("tooltip.trainees.substance.form",
                Component.translatable(formKey(getForm(stack))).getString()));
        tooltip.add(Component.translatable("tooltip.trainees.substance.components"));

        // §19.37（作者 2026-10-02）：成分表整体挂在 Shift 后面。
        // 不按 Shift：名称 / 总摩尔数 / 形态 / "成分:" 这四行摘要照常显示，成分行换成一行"按住 Shift 查看详情"；
        // 按住 Shift：成分逐条展开（成分再复杂、种类再多也不会把提示撑爆）。
        if (!shiftDown()) {
            tooltip.add(getDefault());
            return;
        }

        // 显示成分（按摩尔数从大到小排序；化学式走 IonDisplay）
        comp.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .forEach(entry -> {
                    String ionName = IonDisplay.format(entry.getKey().getId().getPath());
                    double moles = entry.getValue();
                    double percentage = total > 0 ? moles / total * 100 : 0;
                    tooltip.add(Component.literal("  §8- §f" + ionName + " §7" + DF.format(moles)
                            + " mol §8(" + DF.format(percentage) + "%)"));
                });

        // 显示模式标识
        if (isBlueprint(stack)) {
            tooltip.add(Component.translatable("tooltip.trainees.substance.blueprint_mode"));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // 蓝图模式的物品显示附魔光泽
        return isBlueprint(stack);
    }
}