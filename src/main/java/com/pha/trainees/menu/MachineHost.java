package com.pha.trainees.menu;

import com.pha.trainees.mana.IManaStorage;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 魔法机器的"宿主"接口（§17.1 魔法机器 GUI 化）：菜单只认这个接口，不认具体方块实体。
 *
 * <p>于是 {@link MachineMenu} / {@code MachineScreen} 各只有一个类，靠 机器种类 参数化——
 * 三台机器（提取/聚合/富集）共用同一套 GUI 代码，将来加第四台只要实现本接口 + 注册一个种类。</p>
 */
public interface MachineHost {

    /** 物品槽（槽位含义由机器自己定义，GUI 只按种类摆位置）。 */
    ItemStackHandler inventory();

    /** 精华缓存（GUI 画电量条、R 键也走它）。 */
    IManaStorage mana();

    /** 当前处理进度（tick）。 */
    int progress();

    /** 本次处理的总时长（tick）：0 = 没在处理，进度条按它算比例。 */
    int totalTicks();

    /** 流体处理器：没有流体的机器返回 null（富集仓返回 4 个罐拼成的处理器）。 */
    @Nullable
    default IFluidHandler fluids() {
        return null;
    }

    /** 按面 I/O 配置：没有面配置的机器返回 null（菜单就不画面按钮）。 */
    @Nullable
    default com.pha.trainees.machine.FaceConfig faces() {
        return null;
    }

    /** 某个面的模式序号（给 ContainerData 同步到客户端；没有面配置时返回 0 = 未指定）。 */
    default int faceMode(int faceIndex) {
        com.pha.trainees.machine.FaceConfig config = faces();
        return config == null ? 0 : config.get(faceIndex).ordinal();
    }

    /** 面配置被改动后的回调（方块实体借此落存档 + 同步给客户端）。 */
    default void onFaceConfigChanged() {
    }

    // ==================== 精练精华网端口专用的只读信息（GUI 显示用） ====================

    /** 连到的缓存单元数量（非端口机器返回 0）。 */
    default int networkUnits() {
        return 0;
    }

    /** 网络扫描是否因为超过上限被截断。 */
    default boolean networkTruncated() {
        return false;
    }

    /** 单次输入/输出吞吐上限（RE）。 */
    default int throughput() {
        return 0;
    }

    /** 调整吞吐（默认空实现：只有端口支持）。 */
    default void setThroughput(int value) {
    }

    /** 多方块结构是否成形（端口专用）。 */
    default boolean structureFormed() {
        return false;
    }

    /** 驱动耗是否付得起（付不起则停止对外传输）。 */
    default boolean poweredByUpkeep() {
        return true;
    }
}
