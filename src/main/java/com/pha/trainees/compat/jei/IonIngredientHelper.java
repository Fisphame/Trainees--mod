package com.pha.trainees.compat.jei;

import com.pha.trainees.chemistry.particle.IonType;
import com.pha.trainees.chemistry.util.IonDisplay;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 裸离子条目的 JEI 助手（§19.16）：告诉 JEI 如何唯一标识、显示、搜索、着色的离子。
 *
 * <p>搜索主要走 {@link #getDisplayName}（化学式，如 {@code SO₄²⁻}）与 renderer 的 tooltip；
 * 颜色搜索走 {@link #getColors}（物质外观色 displayColor）。</p>
 */
public class IonIngredientHelper implements IIngredientHelper<IonType> {

    @Override
    public IIngredientType<IonType> getIngredientType() {
        return ChemicalIngredientTypes.ION;
    }

    @Override
    public String getDisplayName(IonType ingredient) {
        return IonDisplay.format(ingredient.getId().getPath());
    }

    @Override
    public String getUniqueId(IonType ingredient, UidContext context) {
        return ingredient.getId().toString();
    }

    @Override
    public ResourceLocation getResourceLocation(IonType ingredient) {
        return ingredient.getId();
    }

    @Override
    public IonType copyIngredient(IonType ingredient) {
        return ingredient; // IonType 不可变，无需复制
    }

    /** 颜色搜索：返回物质外观色 */
    @Override
    public Iterable<Integer> getColors(IonType ingredient) {
        return List.of(ingredient.getDisplayColor());
    }

    @Override
    public String getErrorInfo(@Nullable IonType ingredient) {
        return ingredient == null ? "null ion" : ingredient.getId().toString();
    }
}
