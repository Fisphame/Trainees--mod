package com.pha.trainees.chemistry.blockentity;

import com.pha.trainees.chemistry.heat.HeatSourceTracker;
import com.pha.trainees.chemistry.heat.IHeatSource;
import com.pha.trainees.registry.ModChemistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 创造热源方块实体（§19.24）：可把目标温度设定为<b>任意值</b>（含 0 ℃ 以下的负摄氏度），
 * 用于在游戏内精确复现某一温度区间、验证温度依赖的反应（如含氯消毒液的 60~80 ℃ 窗口）。
 *
 * <p>语义为<b>设定点</b>：既能加热也能吸热降温，所以 77 K（液氮）到 3000 K（电弧）都能稳定复现。
 * 注意容器自身仍有材料上限（当前烧杯由 {@code ChemConfig.MAX_SAFE_TEMPERATURE} 兜底，默认 1800 K 封顶），
 * 这正是后续"容器材料分级"（§6.3）要接管的地方。</p>
 */
public class CreativeHeatSourceBlockEntity extends BlockEntity implements IHeatSource {

    public static final double MIN_TEMPERATURE = 1.0;
    public static final double MAX_TEMPERATURE = 3000.0;
    public static final double DEFAULT_TEMPERATURE = 293.15;

    /**
     * 档位表（K）：右键升档 / 潜行右键降档。数值取自现实控温手段的代表点（§19.24）：
     * 液氮 −196 ℃、干冰/丙酮 −78 ℃、冰盐冷冻混合物 ≈ −21 ℃、冰水浴 0 ℃、沸水浴 100 ℃、
     * 油浴 200~250 ℃、沙浴 ≈600 ℃，往上过渡到火焰/高温炉区间。
     */
    public static final double[] PRESETS = {
            77.0,     // 液氮 −196.2 ℃
            195.0,    // 干冰 + 丙酮 −78.2 ℃
            252.0,    // 冰 + 食盐冷冻混合物 ≈ −21.2 ℃
            273.15,   // 冰水浴 0 ℃
            293.15,   // 室温 20 ℃
            333.15,   // 60 ℃：含氯消毒液高温窗口下沿
            353.15,   // 80 ℃：含氯消毒液高温窗口上沿
            373.15,   // 沸水浴 100 ℃（水浴天然恒温点）
            423.15,   // 150 ℃
            473.15,   // 油浴 200 ℃
            523.15,   // 硅油浴上限 250 ℃
            673.15,   // 加热套 400 ℃
            873.15,   // 沙浴 600 ℃
            1073.15,  // 800 ℃
            1273.15,  // 1000 ℃
            1673.15,  // 1400 ℃
            1873.15,  // 1600 ℃：炼钢/玻璃熔融区间
            2273.15,  // 2000 ℃
            2773.15   // 2500 ℃：电弧炉
    };

    private double targetTemperature = DEFAULT_TEMPERATURE;

    public CreativeHeatSourceBlockEntity(BlockPos pos, BlockState state) {
        super(ModChemistry.ModChemistryBlockEntities.CREATIVE_HEAT_SOURCE.get(), pos, state);
    }

    @Override
    public double getSourceTemperature() {
        return targetTemperature;
    }

    public double getTargetTemperature() {
        return targetTemperature;
    }

    @Override
    public boolean isSettable() {
        return true;
    }

    @Override
    public boolean setSourceTemperature(double kelvin) {
        setTargetTemperature(kelvin);
        return true;
    }

    /** 设定目标温度（自动夹到 [MIN, MAX]），返回夹取后的实际值。 */
    public double setTargetTemperature(double kelvin) {
        double clamped = Mth.clamp(kelvin, MIN_TEMPERATURE, MAX_TEMPERATURE);
        if (Math.abs(clamped - targetTemperature) > 1e-6) {
            targetTemperature = clamped;
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
        // §19.38 A2：设定点变化立即反映到气体网格的登记表（含 0 ℃ 以下的冷源）
        if (level != null) {
            HeatSourceTracker.update(level, worldPosition, targetTemperature, isHeatSourceActive());
        }
        return targetTemperature;
    }

    // ==================== 生命周期（热源登记表，§19.38 A2） ====================

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null) {
            HeatSourceTracker.update(level, worldPosition, targetTemperature, isHeatSourceActive());
        }
    }

    @Override
    public void setRemoved() {
        if (level != null) HeatSourceTracker.unregister(level, worldPosition);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        if (level != null) HeatSourceTracker.unregister(level, worldPosition);
        super.onChunkUnloaded();
    }

    /**
     * 按档位表循环。当前值不在档位上时，升档取"第一个更高的档"，降档取"第一个更低的档"；
     * 到顶/到底则停在端点档位。
     */
    public double cyclePreset(int direction) {
        double next = targetTemperature;
        if (direction >= 0) {
            for (double preset : PRESETS) {
                if (preset > targetTemperature + 1e-6) {
                    next = preset;
                    break;
                }
            }
            if (Math.abs(next - targetTemperature) < 1e-6) next = PRESETS[PRESETS.length - 1];
        } else {
            for (int i = PRESETS.length - 1; i >= 0; i--) {
                if (PRESETS[i] < targetTemperature - 1e-6) {
                    next = PRESETS[i];
                    break;
                }
            }
            if (Math.abs(next - targetTemperature) < 1e-6) next = PRESETS[0];
        }
        return setTargetTemperature(next);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putDouble("TargetTemperature", targetTemperature);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("TargetTemperature")) {
            targetTemperature = Mth.clamp(tag.getDouble("TargetTemperature"), MIN_TEMPERATURE, MAX_TEMPERATURE);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putDouble("TargetTemperature", targetTemperature);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
