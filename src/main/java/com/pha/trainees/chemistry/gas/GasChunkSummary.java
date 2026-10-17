package com.pha.trainees.chemistry.gas;

import com.pha.trainees.chemistry.particle.IonType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.Map;

/**
 * 区块级粗账（Phase 9 / §19.13 两级 LOD 的"远场/存档层"）。
 * 精区（已加载区块）逐格 GasMixture 坍缩而来；成分与总质量恒守恒。
 * 每种气体记录：总量 mol + 区内加权重心（区块局部浮点坐标）。
 * 区块重载/存档重进时按重心再水合摊开（分布近似，总量守恒）。
 */
public class GasChunkSummary {

    /** 单种气体的区块粗账 */
    public static class Entry {
        public final IonType ion;
        public double totalMoles;
        /** 区内重心（区块局部坐标 0..16，浮点） */
        public double cx, cy, cz;
        public double tempWeightedSum; // 用于算平均温度（mol·K 累计）

        public Entry(IonType ion) {
            this.ion = ion;
        }

        public void absorb(double moles, double x, double y, double z, double temp) {
            double w = this.totalMoles + moles;
            if (w <= 1e-12) return;
            // 重心加权更新（避免除零：先按新总量加权合并坐标）
            double prevW = this.totalMoles;
            double total = prevW + moles;
            if (total <= 0) {
                this.totalMoles = 0;
                return;
            }
            this.cx = (this.cx * prevW + x * moles) / total;
            this.cy = (this.cy * prevW + y * moles) / total;
            this.cz = (this.cz * prevW + z * moles) / total;
            this.totalMoles = total;
            this.tempWeightedSum += moles * temp;
        }

        public double averageTemperature() {
            return totalMoles > 0 ? tempWeightedSum / totalMoles : 293.0;
        }

        public CompoundTag toNbt() {
            CompoundTag t = new CompoundTag();
            t.putString("ion", ion.getId().toString());
            t.putDouble("mol", totalMoles);
            t.putDouble("cx", cx);
            t.putDouble("cy", cy);
            t.putDouble("cz", cz);
            return t;
        }
    }

    private final Map<IonType, Entry> entries = new HashMap<>();

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public void clear() {
        entries.clear();
    }

    /** 记录一份气体（mol，世界坐标或区块局部坐标均可，须与 entry 坐标同系） */
    public void absorb(IonType ion, double moles, double x, double y, double z, double temp) {
        if (moles <= 0) return;
        entries.computeIfAbsent(ion, Entry::new).absorb(moles, x, y, z, temp);
    }

    public Map<IonType, Entry> getEntries() {
        return entries;
    }

    public double totalMoles() {
        double sum = 0;
        for (Entry e : entries.values()) {
            sum += e.totalMoles;
        }
        return sum;
    }

    public CompoundTag toNbt() {
        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();
        for (Entry e : entries.values()) {
            list.add(e.toNbt());
        }
        root.put("entries", list);
        return root;
    }

    public static GasChunkSummary fromNbt(CompoundTag root) {
        GasChunkSummary s = new GasChunkSummary();
        ListTag list = root.getList("entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            IonType ion = GasCodec.findIon(t.getString("ion"));
            if (ion == null) continue;
            Entry e = new Entry(ion);
            e.totalMoles = t.getDouble("mol");
            e.cx = t.getDouble("cx");
            e.cy = t.getDouble("cy");
            e.cz = t.getDouble("cz");
            e.tempWeightedSum = e.totalMoles * 293.0; // 温度不逐条存，取默认近似（V1）
            s.entries.put(ion, e);
        }
        return s;
    }
}
