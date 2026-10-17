package com.pha.trainees.machine;

import net.minecraft.core.Direction;

import java.util.Arrays;

/**
 * 机器的按面 I/O 配置（§17.1，作者 2026-10-03 定的 GT 式做法）。
 *
 * <p><b>规则（作者口述，逐条落在这里）</b>：</p>
 * <ol>
 *   <li>初始 6 个面**全部"未指定"**；一旦指定了任意一个面，**其余面自动变为"禁用"**——
 *       也就是说"未指定"与"已指定"**不能同时出现**（要么全部未指定，要么全部是
 *       指定面/禁用）。</li>
 *   <li>禁用可以再被改成指定面；提供一个 {@link #reset()} 回到"全部未指定"。</li>
 *   <li>四类端点各自**最多一个面**：物品输入 / 流体输入 / 物品输出 / 流体输出。</li>
 *   <li>把某个面指定成"已被别的面占用的端点"时，**把旧的那个面改成禁用**（移动语义，比"报错"友好）。</li>
 *   <li>未指定状态（全 UNSET）下，**任意面都接受物品/流体的输入输出**；一旦进入已指定状态，
 *       每个面**只暴露它自己被指定的角色**。</li>
 * </ol>
 *
 * <p>纯数据 + 纯规则，零 MC 依赖（除了枚举 {@link Direction} 的序号），由 {@code FaceConfigTest} 锁住。</p>
 */
public final class FaceConfig {

    /** 说明：UNSET = 未指定（只在"全部未指定"状态里出现）；DISABLED = 明确禁用该面。 */
    public enum Mode {
        UNSET,
        ITEM_IN,
        FLUID_IN,
        ITEM_OUT,
        FLUID_OUT,
        DISABLED;

        public boolean isInput() {
            return this == ITEM_IN || this == FLUID_IN;
        }

        public boolean isOutput() {
            return this == ITEM_OUT || this == FLUID_OUT;
        }

        /** 是否是"指定端点"（未指定与禁用都不算）。 */
        public boolean isEndpoint() {
            return isInput() || isOutput();
        }

        public String langKey() {
            return "face.trainees." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final int FACE_COUNT = 6;

    private final Mode[] faces = new Mode[FACE_COUNT];

    public FaceConfig() {
        reset();
    }

    /** 全部未指定（初始状态 / 重置按钮）。 */
    public void reset() {
        Arrays.fill(faces, Mode.UNSET);
    }

    public boolean isAllUnset() {
        for (Mode mode : faces) {
            if (mode != Mode.UNSET) return false;
        }
        return true;
    }

    public Mode get(Direction face) {
        return faces[face.ordinal()];
    }

    public Mode get(int faceIndex) {
        return faces[Math.max(0, Math.min(FACE_COUNT - 1, faceIndex))];
    }

    public void setRaw(int faceIndex, Mode mode) {
        faces[Math.max(0, Math.min(FACE_COUNT - 1, faceIndex))] = mode;
    }

    /**
     * 玩家点某个面的按钮：按规则改配置（见类注释）。
     *
     * @param faceIndex 0..5（{@link Direction#ordinal()}）
     * @param target    目标模式（UNSET 会被当成"重置该面为禁用"，因为"未指定"只能整体存在）
     */
    public void setMode(int faceIndex, Mode target) {
        int index = Math.max(0, Math.min(FACE_COUNT - 1, faceIndex));
        Mode mode = target == Mode.UNSET ? Mode.DISABLED : target;

        // 规则 1：首次指定 → 其余面自动禁用（"未指定/已指定"不能共存）
        if (isAllUnset() && mode.isEndpoint()) {
            Arrays.fill(faces, Mode.DISABLED);
        }

        // 规则 4：端点唯一 → 把占用同一端点的旧面改成禁用（移动）
        if (mode.isEndpoint()) {
            for (int i = 0; i < FACE_COUNT; i++) {
                if (i != index && faces[i] == mode) {
                    faces[i] = Mode.DISABLED;
                }
            }
        }
        faces[index] = mode;

        // 规则 1 的反向：如果所有面都变成未指定了（不该发生，兜一下）
        boolean anyConfigured = false;
        for (Mode current : faces) {
            if (current != Mode.UNSET) {
                anyConfigured = true;
                break;
            }
        }
        if (!anyConfigured) {
            reset();
        }
    }

    // ==================== 按面的能力判定（方块实体在 getCapability 里问这几个） ====================

    /** 该面是否接受物品输入（未指定状态：任意面；已指定状态：只有 ITEM_IN 面）。 */
    public boolean acceptsItemInput(Direction side) {
        Mode mode = get(side);
        return isAllUnset() ? true : mode == Mode.ITEM_IN;
    }

    public boolean acceptsFluidInput(Direction side) {
        Mode mode = get(side);
        return isAllUnset() ? true : mode == Mode.FLUID_IN;
    }

    public boolean allowsItemOutput(Direction side) {
        Mode mode = get(side);
        return isAllUnset() ? true : mode == Mode.ITEM_OUT;
    }

    public boolean allowsFluidOutput(Direction side) {
        Mode mode = get(side);
        return isAllUnset() ? true : mode == Mode.FLUID_OUT;
    }

    /** 该面是否对外暴露物品能力（输入或输出任一即可）。 */
    public boolean exposesItems(Direction side) {
        return acceptsItemInput(side) || allowsItemOutput(side);
    }

    /** 该面是否对外暴露流体能力。 */
    public boolean exposesFluids(Direction side) {
        return acceptsFluidInput(side) || allowsFluidOutput(side);
    }

    // ==================== 存档 ====================

    /** 打包成 6 个小整数（菜单的 ContainerData 也用同一套序号同步到客户端）。 */
    public int[] toOrdinals() {
        int[] out = new int[FACE_COUNT];
        for (int i = 0; i < FACE_COUNT; i++) {
            out[i] = faces[i].ordinal();
        }
        return out;
    }

    public void fromOrdinals(int[] ordinals) {
        Mode[] values = Mode.values();
        for (int i = 0; i < FACE_COUNT; i++) {
            int ordinal = i < ordinals.length ? ordinals[i] : Mode.UNSET.ordinal();
            faces[i] = values[Math.max(0, Math.min(values.length - 1, ordinal))];
        }
    }

    @Override
    public String toString() {
        return "FaceConfig" + Arrays.toString(faces);
    }
}
