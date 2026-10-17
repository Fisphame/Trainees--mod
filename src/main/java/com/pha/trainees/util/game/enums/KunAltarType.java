package com.pha.trainees.util.game.enums;


import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

public enum KunAltarType implements StringRepresentable {
    COMPLETE("complete",
            Block.box(1.0D, 0.0D, 1.0D, 15.0D, 15.5D, 15.0D)),
    HALF("half",
            Block.box(1.0D, 0.0D, 1.0D, 15.0D, 5D, 15.0D));

    private final String name;
    private final VoxelShape shape;

    KunAltarType(String name, VoxelShape shape) {
        this.name = name;
        this.shape = shape;
    }

    public VoxelShape getShape() {
        return shape;
    }

    public String getName() {
        return name;
    }

    // 获取下一个类型（循环切换）
    public KunAltarType next() {
        KunAltarType[] values = values();
        int nextIndex = (this.ordinal() + 1) % values.length;
        return values[nextIndex];
    }

    @Override
    public String getSerializedName() {
        // 返回用于序列化的字符串，建议使用小写
        return this.name;
    }
}
