package com.pha.trainees.util.interfaces;

import com.pha.trainees.Main;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.util.List;

public interface IHoverText {
    /* 键格式：
            "tooltip.trainees.id"
            "tooltip.trainees.id.2"
            "tooltip.trainees.item.press_shift"
        */
    // 注：UI 状态信号（--[ - ]-- 等）已迁移至 TextSignals 枚举

    default void addHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents,
                              TooltipFlag flag, String id) {
        if (shiftDown()) {
            tooltipComponents.add(getTooltip(id));
            tooltipComponents.add(getTooltip(id, 2));
        } else {
            tooltipComponents.add(getDefault());
        }
    }
    default void addChemicalFormula(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents,
                                    TooltipFlag flag, String id) {
        if (shiftDown()) {
            tooltipComponents.add(getTooltip(id).withStyle(ChatFormatting.GRAY));
        } else {
            tooltipComponents.add(getDefault());
        }
    }
    default void addChemicalMolarMass(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents,
                                      TooltipFlag flag, double moss) {
        if (shiftDown()) {
            tooltipComponents.add(Component.literal(String.valueOf(moss)).withStyle(ChatFormatting.GRAY));
        } else {
            tooltipComponents.add(getDefault());
        }
    }

    /**
     * 是否展开物品详情：按住 Shift 展开（§19.34，取代原 F3+H 高级提示开关）。
     * <p>细节说明走内嵌 holder 转发到客户端：专服上 {@code FMLEnvironment.dist} 不是 CLIENT，
     * 短路后不会去解析客户端类（历史崩溃：Attempted to load class Screen for invalid dist DEDICATED_SERVER）。
     */
    static boolean shiftDownClient() {
        return net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient() && ClientShiftBridge.down();
    }

    /** 供接口内部与实现类直接调用：{@code if (shiftDown()) { ... }} */
    default boolean shiftDown() {
        return shiftDownClient();
    }

    final class ClientShiftBridge {
        private ClientShiftBridge() {
        }

        static boolean down() {
            return com.pha.trainees.client.ClientTools.isShiftPressedClient();
        }
    }

    default MutableComponent tr(String s) {
        return Component.translatable(s);
    }
    default String tr(String head, String tail, String... strings){
        return head + "." + Main.MODID + "." + String.join(".", strings) + "." + tail;
    }
    default String tr(String head, String tail){
        return head + Main.MODID + tail;
    }
    default String ky(String id) {
        return MessageFormat.format("tooltip.{0}.{1}", Main.MODID, id);
    }
    default String ky(String id, int index) {
        return MessageFormat.format("tooltip.{0}.{1}.{2}", Main.MODID, id, index);
    }
    default String ky(String id, int index1, int index2) {
        return MessageFormat.format("tooltip.{0}.{1}.{2}.{3}", Main.MODID, id, index1, index2);
    }
    default String ky(String id, String dex) {
        return MessageFormat.format("tooltip.{0}.{1}.{2}", Main.MODID, id, dex);
    }
    default String indexUp(String s, int index) {
        return MessageFormat.format("{0}.{1}", s, index);
    }
    default MutableComponent getTooltip(String id) {
        return tr(ky(id));
    }
    default MutableComponent getTooltip(String id, int index) {
        return tr(ky(id, index));
    }
    default MutableComponent getTooltip(String id, int index1, int index2) {
        return tr(ky(id, index1, index2));
    }
    default MutableComponent getTooltip(String id, String dex) {
        return tr(ky(id, dex));
    }
    default MutableComponent getDefault(){
        return Component.translatable(MessageFormat.format("tooltip.{0}.item.press_shift", Main.MODID));
    }
    default void addTip(List<Component> tooltipComponents, MutableComponent tip) {
        tooltipComponents.add(tip);
    }
    default void addTip(List<Component> tooltipComponents) {
        tooltipComponents.add(getDefault());
    }
    default void print(String s) {
        System.out.println(s);
    }

}
