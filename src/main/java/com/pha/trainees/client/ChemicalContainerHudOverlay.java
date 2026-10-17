package com.pha.trainees.client;

import com.pha.trainees.chemistry.container.IChemicalContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * 化学容器准星提示 HUD（§19.14 点 2，原 {@code BeakerHudOverlay} 泛化而来）。
 *
 * <p>准星对准任意 {@link IChemicalContainer}（烧杯、电解槽、未来机器）时，在屏幕中央偏下显示
 * "这个方块是什么"。文本统一取**方块 id 的翻译键**（{@code Block#getDescriptionId()}）再翻译，
 * 不再写死中文，因此新增容器方块只需补语言键即可自动生效。</p>
 */
public class ChemicalContainerHudOverlay implements IGuiOverlay {

    @Override
    public void render(ForgeGui forgeGui, GuiGraphics guiGraphics, float partialTick,
                       int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!(mc.hitResult instanceof BlockHitResult blockHit)) return;

        BlockPos pos = blockHit.getBlockPos();
        if (!(mc.level.getBlockEntity(pos) instanceof IChemicalContainer)) return;

        BlockState state = mc.level.getBlockState(pos);
        Component name = Component.translatable(state.getBlock().getDescriptionId());

        int textWidth = mc.font.width(name);
        int x = (screenWidth - textWidth) / 2;
        int y = screenHeight / 2 + 30;

        int bgWidth = textWidth + 10;
        int bgHeight = mc.font.lineHeight + 6;
        guiGraphics.fill(x - 5, y - 3, x - 5 + bgWidth, y - 3 + bgHeight, 0x88000000);
        guiGraphics.drawString(mc.font, name, x, y, 0xFFFFFF, true);
    }
}
