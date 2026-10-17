package com.pha.trainees.menu;

import com.pha.trainees.mana.IManaStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 魔法机器的统一菜单（§17.1 魔法机器 GUI 化）：**一个类，按 {@link Kind} 参数化**。
 *
 * <p>要点（与作者确认过的三条一致）：</p>
 * <ul>
 *   <li>**能量与进度走 {@code ContainerData}** → 不需要自定义同步包，客户端自动看到电量条与进度条</li>
 *   <li>**流体容器槽**：插入装流体的桶 → 自动倒进输入罐、留下空桶；插入空桶 → 从输出罐接满
 *       （用 {@code FluidUtil}，与桶右键是同一套逻辑）</li>
 *   <li>**Shift + 点击 = 快速转移**（机器 ↔ 玩家背包）</li>
 *   <li>不做 JEI → GUI 联动（作者否掉了）</li>
 * </ul>
 *
 * <p>槽位布局（含流体容器槽）按机器的 {@code Kind} 摆；富集仓的流体容器槽是它物品槽的最后两格
 * （见 {@code EnrichmentChamberBlockEntity} 的 SLOT_FLUID_IN / SLOT_FLUID_OUT）。</p>
 */
public class MachineMenu extends AbstractContainerMenu {

    /** 机器种类：决定槽位布局与 GUI 尺寸。 */
    public enum Kind {
        /** 提取机：输入 + 2 输出 + 电量 + 进度 */
        EXTRACTOR,
        /** 聚合机：模板 + 输出 + 电量 + 进度 */
        AGGREGATOR,
        /** 富集仓：4 物品槽 + 2 流体容器槽 + 4 罐液面 + 电量 + 进度 */
        ENRICHMENT,
        /** 精练精华网端口（§17.1 ④）：没有物品槽，只有能量与网络信息（视觉由作者微调） */
        PORT;

        /** 玩家背包区的起始 Y（决定 GUI 高度）。 */
        public int playerInvY() {
            return this == ENRICHMENT ? 140 : 84;
        }
    }

    // ContainerData 索引（客户端读它画条）
    public static final int DATA_PROGRESS = 0;
    public static final int DATA_TOTAL = 1;
    public static final int DATA_ENERGY = 2;
    public static final int DATA_CAPACITY = 3;
    /** 6 个面的模式序号（§17.1 按面 I/O）：4..9，客户端据此画按钮。 */
    public static final int DATA_FACE_0 = 4;
    /** 端口专用的网络信息（10..14）。 */
    public static final int DATA_NETWORK_UNITS = 10;
    public static final int DATA_NETWORK_TRUNCATED = 11;
    public static final int DATA_THROUGHPUT = 12;
    public static final int DATA_STRUCTURE = 13;
    public static final int DATA_POWERED = 14;
    public static final int DATA_COUNT = 15;

    /** 菜单按钮 id：0..5 = 六个面（点击循环），6 = 重置为全部未指定。 */
    public static final int BUTTON_RESET_FACES = com.pha.trainees.machine.FaceConfig.FACE_COUNT;

    /** 端口吞吐调节按钮（避开面按钮的 0..6）：10 = 减半，11 = 加倍，12 = 恢复默认。 */
    public static final int BUTTON_THROUGHPUT_HALF = 10;
    public static final int BUTTON_THROUGHPUT_DOUBLE = 11;
    public static final int BUTTON_THROUGHPUT_DEFAULT = 12;

    private final Kind kind;
    @Nullable
    private final MachineHost host;
    private final ContainerData data;

    /**
     * 服务端/客户端共用构造：坐标由 {@code NetworkHooks.openScreen(.., pos)} 写进额外数据，
     * 机器种类由 MenuType 决定（见 {@code ModMenus}）。
     */
    public MachineMenu(int id, Inventory playerInventory, BlockPos pos, Kind kind) {
        super(com.pha.trainees.registry.ModMenus.menuType(kind).get(), id);
        this.kind = kind;

        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        this.host = MachineHosts.of(be);

        // 机器槽位
        IItemHandler machineInventory = host != null ? host.inventory() : new ItemStackHandler(1);
        switch (kind) {
            case EXTRACTOR -> {
                addSlot(new SlotItemHandler(machineInventory, 0, 56, 35));   // 输入
                addSlot(new SlotItemHandler(machineInventory, 1, 116, 17));  // 产物 A
                addSlot(new SlotItemHandler(machineInventory, 2, 116, 53));  // 产物 B
            }
            case AGGREGATOR -> {
                addSlot(new SlotItemHandler(machineInventory, 0, 56, 35));   // 模板
                addSlot(new SlotItemHandler(machineInventory, 1, 116, 35));  // 产物
            }
            case ENRICHMENT -> {
                addSlot(new SlotItemHandler(machineInventory, 0, 44, 20));   // 消耗
                addSlot(new SlotItemHandler(machineInventory, 1, 44, 52));   // 催化剂
                addSlot(new SlotItemHandler(machineInventory, 2, 116, 20));  // 产物 A
                addSlot(new SlotItemHandler(machineInventory, 3, 116, 52));  // 产物 B
                // 流体是**离散纯流体槽**（作者定）：不走物品槽中转，由 GUI 自己画 + 走流体能力
            }
            case PORT -> {
                // 端口没有物品槽：只有能量与网络信息
            }
        }

        // 玩家背包 + 快捷栏
        int invY = kind.playerInvY();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, 9 + row * 9 + col, 8 + col * 18, invY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, invY + 58));
        }

        // 电量/进度：ContainerData（每 tick 自动同步，无需自定义包）
        this.data = host == null ? new SimpleContainerData(DATA_COUNT) : new SimpleContainerData(DATA_COUNT) {
            @Override
            public int get(int index) {
                return switch (index) {
                    case DATA_PROGRESS -> host.progress();
                    case DATA_TOTAL -> host.totalTicks();
                    case DATA_ENERGY -> host.mana().getManaStored();
                    case DATA_CAPACITY -> host.mana().getMaxManaStored();
                    case DATA_NETWORK_UNITS -> host.networkUnits();
                    case DATA_NETWORK_TRUNCATED -> host.networkTruncated() ? 1 : 0;
                    case DATA_THROUGHPUT -> host.throughput();
                    case DATA_STRUCTURE -> host.structureFormed() ? 1 : 0;
                    case DATA_POWERED -> host.poweredByUpkeep() ? 1 : 0;
                    default -> index >= DATA_FACE_0
                            && index < DATA_FACE_0 + com.pha.trainees.machine.FaceConfig.FACE_COUNT
                            ? host.faceMode(index - DATA_FACE_0)
                            : 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // 只读：数值都由服务端的机器状态算出来
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
        addDataSlots(data);
    }

    public Kind kind() {
        return kind;
    }

    /**
     * 进度条的"满值"：机器没有对外暴露本次总时长，所以**在客户端侧观察**——
     * 看到进度往上跳时把当前值记成满值即可。好处是不必为了一根进度条去改三台机器的存档结构。
     */
    private int observedMaxTicks;

    public int progress() {
        int progress = data.get(DATA_PROGRESS);
        if (progress > observedMaxTicks) {
            observedMaxTicks = progress;
        }
        return progress;
    }

    public int totalTicks() {
        return Math.max(data.get(DATA_TOTAL), observedMaxTicks);
    }

    public int energy() {
        return data.get(DATA_ENERGY);
    }

    public int capacity() {
        return Math.max(1, data.get(DATA_CAPACITY));
    }

    /** 某个面的模式（0 = 未指定；见 {@code FaceConfig.Mode} 的序号）。 */
    public int faceMode(int faceIndex) {
        return data.get(DATA_FACE_0 + Math.max(0, Math.min(
                com.pha.trainees.machine.FaceConfig.FACE_COUNT - 1, faceIndex)));
    }

    /** 这台机器有没有面配置（没有就不画面按钮）。 */
    public boolean hasFaceConfig() {
        return host != null && host.faces() != null;
    }

    // ==================== 端口专用的网络信息（GUI 显示） ====================

    public int networkUnits() {
        return data.get(DATA_NETWORK_UNITS);
    }

    public boolean networkTruncated() {
        return data.get(DATA_NETWORK_TRUNCATED) != 0;
    }

    public int throughput() {
        return data.get(DATA_THROUGHPUT);
    }

    public boolean structureFormed() {
        return data.get(DATA_STRUCTURE) != 0;
    }

    public boolean poweredByUpkeep() {
        return data.get(DATA_POWERED) != 0;
    }

    // ==================== 面按钮（走原版菜单按钮，不需要自定义网络包） ====================

    /**
     * 处理原版菜单按钮点击（客户端用 {@code gameMode.handleInventoryButtonClick(containerId, id)} 发过来）。
     *
     * <p>id 0..5 = 六个面，点击**循环**：未指定/禁用 → 物品输入 → 流体输入 → 物品输出 → 流体输出 → 禁用
     * （"未指定"只能整体存在，见 {@code FaceConfig} 的规则）；id 6 = 重置为全部未指定。</p>
     */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        // 端口吞吐调节（与面配置无关，所以放在面配置判断之前）
        if (host != null && kind == Kind.PORT) {
            int next = switch (id) {
                case BUTTON_THROUGHPUT_HALF -> Math.max(1, host.throughput() / 2);
                case BUTTON_THROUGHPUT_DOUBLE -> host.throughput() > Integer.MAX_VALUE / 2
                        ? Integer.MAX_VALUE
                        : host.throughput() * 2;
                case BUTTON_THROUGHPUT_DEFAULT ->
                        com.pha.trainees.blockentity.EssenceNetworkPortBlockEntity.DEFAULT_THROUGHPUT;
                default -> -1;
            };
            if (next > 0) {
                host.setThroughput(next);
                broadcastChanges();
                return true;
            }
        }

        if (!hasFaceConfig()) return false;
        com.pha.trainees.machine.FaceConfig config = host.faces();

        if (id == BUTTON_RESET_FACES) {
            config.reset();
        } else if (id >= 0 && id < com.pha.trainees.machine.FaceConfig.FACE_COUNT) {
            config.setMode(id, nextFaceMode(config.get(id)));
        } else {
            return false;
        }
        host.onFaceConfigChanged();
        broadcastChanges();
        return true;
    }

    private static com.pha.trainees.machine.FaceConfig.Mode nextFaceMode(
            com.pha.trainees.machine.FaceConfig.Mode current) {
        return switch (current) {
            case UNSET, DISABLED -> com.pha.trainees.machine.FaceConfig.Mode.ITEM_IN;
            case ITEM_IN -> com.pha.trainees.machine.FaceConfig.Mode.FLUID_IN;
            case FLUID_IN -> com.pha.trainees.machine.FaceConfig.Mode.ITEM_OUT;
            case ITEM_OUT -> com.pha.trainees.machine.FaceConfig.Mode.FLUID_OUT;
            case FLUID_OUT -> com.pha.trainees.machine.FaceConfig.Mode.DISABLED;
        };
    }

    @Nullable
    public MachineHost host() {
        return host;
    }

    // ==================== 快速转移（Shift + 点击） ====================

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        int machineSlots = switch (kind) {
            case ENRICHMENT -> 4;
            case EXTRACTOR -> 3;
            case AGGREGATOR -> 2;
            case PORT -> 0;
        };
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < machineSlots) {
            // 机器 → 玩家背包
            if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            // 玩家背包 → 机器：按种类给一个"最可能正确"的目标区间
            boolean moved = switch (kind) {
                case EXTRACTOR -> moveItemStackTo(stack, 0, 1, false);
                case AGGREGATOR -> moveItemStackTo(stack, 0, 1, false);
                case ENRICHMENT -> moveItemStackTo(stack, 0, 2, false);   // 消耗/催化剂槽
                case PORT -> false;                                       // 端口没有物品槽
            };
            if (!moved) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    // ==================== 流体：离散纯流体槽（不走物品槽中转） ====================
    // 作者 2026-10-03：富集仓的输入/催化/输出流体都是**离散流体槽**（一槽一种、整份进出），
    // 由 IFluidHandler 能力进出（管道/线缆），GUI 里画成流体槽。所以菜单这边**没有**流体容器物品槽。

    @Override
    public boolean stillValid(Player player) {
        return true;   // 机器没有"玩家走远就关"的需求；真要加就按方块距离判
    }
}
