package com.pha.trainees.chemistry.blockentity;

import com.pha.trainees.chemistry.container.IChemicalContainer;
import com.pha.trainees.chemistry.particle.IonType;
import com.pha.trainees.chemistry.product.PackOutcome;
import com.pha.trainees.chemistry.product.PackerService;
import com.pha.trainees.chemistry.product.ProductSpecs;
import com.pha.trainees.chemistry.spec.ProductSpec;
import com.pha.trainees.network.PackerStatePacket;
import com.pha.trainees.registry.ModChemistry.ModIons;
import com.pha.trainees.registry.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 打包机方块实体（§19.18 / §19.25 自动化的第一段）。
 *
 * <p><b>为什么现在才有方块实体</b>：V1 的打包机是"玩家右键触发的一次性判定"，没有状态；要做**自动输入输出**，
 * 机器必须能**自驱**（tick）并且**有地方放产物**（输出槽 + {@code IItemHandler}），因此补上本 BE。</p>
 *
 * <p><b>自动化链路（§19.25 路线 1：能力路线，零票据）</b></p>
 * <pre>
 * 外部补料 → 上方容器（溶液留在容器里，无需搬运 Map） → 打包机每 N tick 自动判定并打包
 *          → 输出槽（IItemHandler，对 ME/漏斗/管道可提取） → 外部取走
 * </pre>
 * <p>关键：**输入侧不需要搬 Map**——打包机读的是上方容器的状态，"溶液"从不离开容器。这正是
 * §19.25「同一台机器边上只做一次投影」的落地。</p>
 *
 * <p><b>FE 门槛</b>：每瓶固定电费（默认 20 kJ = 20000 FE，1 FE = 1 J，与电解槽同口径）。
 * 电不够 → 不打包、**不扣料**；输出槽满 → 同样不打包、不扣料（避免自动化白吃料）。</p>
 *
 * <p><b>安全默认</b>：自驱永远走 **auto**（不合格不产不扣）；"强制灌装"只能人工（潜行右键），
 * 否则无人值守会把整锅料吃成危险次品。</p>
 */
public class PackerBlockEntity extends BlockEntity {

    /** 输出槽：外部只能抽、不能塞（isItemValid 恒 false）。 */
    private final ItemStackHandler output = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return false;
        }
    };
    private final LazyOptional<ItemStackHandler> outputCap = LazyOptional.of(() -> output);

    private final PackerEnergy energy = new PackerEnergy();
    private final LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> energy);

    /** V1 只有一个产品；留着索引是为了 ⑥ 多产品时直接接 GUI。 */
    private int targetIndex = 0;
    private int cooldown = 0;
    /** 最近一次失败/拒收的语言键（供 GUI 显示"最近结果"）；成功时清空 */
    private String lastReasonKey = "";

    public PackerBlockEntity(BlockPos pos, BlockState state) {
        super(com.pha.trainees.registry.ModChemistry.ModChemistryBlockEntities.PACKER.get(), pos, state);
        cooldown = interval();
    }

    // ==================== tick：自驱 ====================

    public static void tick(Level level, BlockPos pos, BlockState state, PackerBlockEntity packer) {
        if (level.isClientSide) return;
        if (packer.cooldown > 0) {
            packer.cooldown--;
            return;
        }
        packer.cooldown = interval();
        packer.packOnce(level, pos, false, null);   // 自驱永远是 auto 模式
    }

    private static int interval() {
        return Math.max(1, ModConfig.COMMON.packerCheckInterval.get());
    }

    // ==================== 打包一次 ====================

    /**
     * 尝试打包一次。
     *
     * @param forced   人工强制灌装（可产危险次品）；自驱必须传 false
     * @param feedback 为 null 时静默（自动化）；非 null 时把原因/结果发给玩家
     * @return 是否真的产出并扣料
     */
    public boolean packOnce(Level level, BlockPos pos, boolean forced, @Nullable Player feedback) {
        if (!(level.getBlockEntity(pos.above()) instanceof IChemicalContainer container)) {
            return reject(feedback, "message.trainees.packer.no_container");
        }

        ProductSpec spec = ProductSpecs.ALL.get(Math.min(targetIndex, ProductSpecs.ALL.size() - 1));
        Map<String, Double> contents = toIdKeyedMap(container.getContents());
        double usableMoles = container.getTotalMoles();

        PackOutcome outcome = forced
                ? PackerService.evaluateForced(spec, ProductSpecs.adapterFor(spec.id()),
                        contents, PackerBlockEntity::molarMassOf, usableMoles)
                : PackerService.evaluateAuto(spec, ProductSpecs.adapterFor(spec.id()),
                        contents, PackerBlockEntity::molarMassOf, usableMoles);

        if (!outcome.producesItem()) {
            String key = outcome.reasonKey() == null || outcome.reasonKey().isEmpty()
                    ? "message.trainees.packer.rejected"
                    : outcome.reasonKey();
            return reject(feedback, key);
        }

        int cost = Math.max(0, ModConfig.COMMON.packerEnergyPerBottle.get());
        if (energy.stored() < cost) {
            return reject(feedback, "message.trainees.packer.no_power");
        }

        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(outcome.productItemId()));
        if (item == null) {
            // 规格声明了产出但物品尚未注册：报错而不是静默扣料
            if (feedback != null) {
                feedback.displayClientMessage(Component.translatable(
                        "message.trainees.packer.missing_item", outcome.productItemId()), true);
            }
            return false;
        }

        ItemStack product = new ItemStack(item);
        ItemStack remainder = output.insertItem(0, product, true);
        if (!remainder.isEmpty()) {
            // 输出槽满：不扣料、不耗电（否则自动化会白吃料）
            return reject(feedback, "message.trainees.packer.output_full");
        }
        output.insertItem(0, product, false);

        energy.consume(cost);
        consumeAliquot(container, spec.bottleMoles(), usableMoles);

        // 成功：清掉"最近失败原因"（GUI 的最近结果回到空白）
        if (!lastReasonKey.isEmpty()) {
            lastReasonKey = "";
        }
        if (feedback != null) {
            feedback.displayClientMessage(Component.translatable(
                    outcome.isDefective() ? "message.trainees.packer.defective" : "message.trainees.packer.packed",
                    product.getHoverName()), true);
        }
        setChanged();
        return true;
    }

    private boolean reject(@Nullable Player feedback, String langKey) {
        if (feedback != null) {
            feedback.displayClientMessage(Component.translatable(langKey), true);
        }
        // 记下原因：GUI 的"最近结果"要显示它（与是否有人看着无关，自动化停产的排查也靠它）
        if (!langKey.equals(lastReasonKey)) {
            lastReasonKey = langKey;
            setChanged();
        }
        return false;
    }

    /** 取走输出槽产物（GUI 按钮 / 潜行外的快捷路径）。@return 是否真的取到了东西 */
    public boolean takeOutput(Player player) {
        ItemStack current = output.getStackInSlot(0);
        if (current.isEmpty()) return false;
        ItemStack taken = output.extractItem(0, current.getCount(), false);
        if (!player.getInventory().add(taken)) {
            player.drop(taken, false);
        }
        setChanged();
        return true;
    }

    // ==================== GUI 状态（§19.18.5） ====================

    /** 当前目标产品 id（V1 只有含氯消毒液；索引越界时夹到最后一个）。 */
    public String currentProductId() {
        return ProductSpecs.ALL.get(Math.min(targetIndex, ProductSpecs.ALL.size() - 1)).id();
    }

    /** 最近一次失败/拒收的语言键（空串 = 上次成功或尚无记录）。 */
    public String getLastReasonKey() {
        return lastReasonKey;
    }

    /**
     * 现在能不能打一瓶——**只读判定**（§19.18.5），供 GUI 显示"就绪 / 为什么没就绪"。
     *
     * <p>它跑的是与 {@link #packOnce} 同一套 {@code PackerService.evaluateAuto}，但**不扣料、不耗电、
     * 不写 {@code lastReasonKey}**，所以可以安全地反复调用。</p>
     *
     * <p>⚠ <b>性能红线</b>：本方法只允许在 <b>REQUEST_STATE</b>（面板刷新）与动作执行后组装状态包时调用，
     * <b>绝不允许放进 {@code tick()} 或每帧路径</b>——它要遍历容器内容物并做一次完整规格判定。</p>
     */
    public DryRun dryRun(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos.above()) instanceof IChemicalContainer container)) {
            return new DryRun(false, "message.trainees.packer.no_container", "");
        }
        int cost = Math.max(0, ModConfig.COMMON.packerEnergyPerBottle.get());
        if (energy.stored() < cost) {
            return new DryRun(false, "message.trainees.packer.no_power", "");
        }
        if (!output.getStackInSlot(0).isEmpty()) {
            return new DryRun(false, "message.trainees.packer.output_full", "");
        }

        ProductSpec spec = ProductSpecs.ALL.get(Math.min(targetIndex, ProductSpecs.ALL.size() - 1));
        PackOutcome outcome = PackerService.evaluateAuto(spec, ProductSpecs.adapterFor(spec.id()),
                toIdKeyedMap(container.getContents()), PackerBlockEntity::molarMassOf,
                container.getTotalMoles());
        if (!outcome.producesItem()) {
            String key = outcome.reasonKey() == null || outcome.reasonKey().isEmpty()
                    ? "message.trainees.packer.rejected"
                    : outcome.reasonKey();
            return new DryRun(false, key, "");
        }
        return new DryRun(true, "", outcome.productItemId());
    }

    /**
     * 只读判定结果（§19.18.5）。
     *
     * @param canPack       此刻能否打一瓶（含规格判定，不只是"电够 + 槽空"）
     * @param reasonKey     不能打包时的语言键；能打包时为空串
     * @param productItemId 能打包时的产物 item id；不能时为空串
     */
    public record DryRun(boolean canPack, String reasonKey, String productItemId) {
    }

    /** 组装状态包（S2C）：GUI 显示所需的全部信息（内部跑一次只读判定，调用点见 {@link #dryRun} 的红线） */
    public PackerStatePacket buildStatePacket() {
        DryRun dry = level == null
                ? new DryRun(false, "message.trainees.packer.no_container", "")
                : dryRun(level, worldPosition);
        return new PackerStatePacket(worldPosition, energy.stored(),
                ModConfig.COMMON.packerEnergyCapacity.get(),
                output.getStackInSlot(0).copy(), currentProductId(),
                dry.canPack(), dry.reasonKey(), lastReasonKey);
    }

    // ==================== 能力（自动化接口） ====================
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return outputCap.cast();
        }
        if (cap == ForgeCapabilities.ENERGY) {
            return energyCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        outputCap.invalidate();
        energyCap.invalidate();
    }

    // ==================== 持久化 ====================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Output", output.serializeNBT());
        tag.putInt("TargetIndex", targetIndex);
        tag.putInt("Energy", energy.stored());
        tag.putString("LastReason", lastReasonKey);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Output")) output.deserializeNBT(tag.getCompound("Output"));
        targetIndex = tag.getInt("TargetIndex");
        energy.setStored(tag.getInt("Energy"));
        lastReasonKey = tag.getString("LastReason");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.put("Output", output.serializeNBT());
        tag.putInt("TargetIndex", targetIndex);
        tag.putInt("Energy", energy.stored());
        tag.putString("LastReason", lastReasonKey);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ==================== FE：只进不出的内部缓冲 ====================

    /** 打包机电费缓冲：外部只能充、不能抽；内部打包时按瓶扣。1 FE = 1 J（与电解槽同口径）。 */
    private class PackerEnergy implements IEnergyStorage {
        private int stored = 0;

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int capacity = ModConfig.COMMON.packerEnergyCapacity.get();
            int accepted = Math.min(Math.max(0, maxReceive), Math.max(0, capacity - stored));
            if (!simulate && accepted > 0) {
                stored += accepted;
                setChanged();
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;   // 只进不出：外部无法抽走打包机的电
        }

        @Override
        public int getEnergyStored() {
            return stored;
        }

        @Override
        public int getMaxEnergyStored() {
            return ModConfig.COMMON.packerEnergyCapacity.get();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }

        int stored() {
            return stored;
        }

        void setStored(int value) {
            stored = Math.max(0, Math.min(value, ModConfig.COMMON.packerEnergyCapacity.get()));
        }

        /** 内部扣费（打包电费）。 */
        void consume(int amount) {
            int used = Math.min(stored, Math.max(0, amount));
            if (used > 0) {
                stored -= used;
                setChanged();
            }
        }
    }

    // ==================== 静态工具（判定口径） ====================

    /** 容器内容物（IonType → mol）转成规格判定用的 id 键（{@code namespace:path}）。 */
    private static Map<String, Double> toIdKeyedMap(Map<IonType, Double> contents) {
        Map<String, Double> map = new LinkedHashMap<>();
        for (Map.Entry<IonType, Double> entry : contents.entrySet()) {
            map.put(entry.getKey().getId().toString(), entry.getValue());
        }
        return map;
    }

    /** 判定口径的摩尔质量查询：未知 id 返回 ≤0，由 SpecMatcher 判为 UNKNOWN_MASS（失败关闭）。 */
    private static double molarMassOf(String ionId) {
        ResourceLocation id = ResourceLocation.tryParse(ionId);
        IonType ion = id == null ? null : ModIons.getById(id);
        return ion == null ? -1.0 : ion.getMolarMass();
    }

    /**
     * 取走一瓶的量：按比例（bottleMoles / totalMoles）从**所有**物种中取走相同比例，
     * 保持容器内成分比例不变（理想搅拌不变量）。先在快照上算，避免边遍历边改。
     */
    private static void consumeAliquot(IChemicalContainer container, double bottleMoles, double usableMoles) {
        if (usableMoles <= 0 || bottleMoles <= 0) return;
        double fraction = Math.min(1.0, bottleMoles / usableMoles);
        List<Map.Entry<IonType, Double>> snapshot = new ArrayList<>(container.getContents().entrySet());
        for (Map.Entry<IonType, Double> entry : snapshot) {
            double take = entry.getValue() * fraction;
            if (take > 1e-12) {
                container.removeIon(entry.getKey(), take, false);
            }
        }
        container.resetBalancedRules();
        container.setChanged();
    }
}
