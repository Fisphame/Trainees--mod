package com.pha.trainees.util.game.enums;

public enum AbsorbWorkModel {
    HINDERING("hindering"),
    DROPPING("dropping");

    private final String id;

    AbsorbWorkModel(String id){
        this.id = id;
    }

    public String getId() {
        return id;
    }

    // 获取下一个类型（循环切换）
    public AbsorbWorkModel next() {
        AbsorbWorkModel[] values = values();
        int nextIndex = (this.ordinal() + 1) % values.length;
        return values[nextIndex];
    }
}
