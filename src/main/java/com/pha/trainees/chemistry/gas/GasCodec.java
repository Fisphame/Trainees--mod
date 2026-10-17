package com.pha.trainees.chemistry.gas;

import com.pha.trainees.chemistry.particle.IonType;
import com.pha.trainees.registry.ModChemistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.Map;

/**
 * 气体 NBT 序列化与离子查找辅助（Phase 9）。
 * 离子以全局注册表（ModIons.ALL_IONS）为准，NBT 中存完整 id（含命名空间）。
 */
public final class GasCodec {

    private GasCodec() {}

    /** 按 id（含命名空间）在全局离子注册表查找；未找到返回 null */
    public static IonType findIon(String id) {
        for (IonType ion : ModChemistry.ModIons.ALL_IONS) {
            if (ion.getId().toString().equals(id)) return ion;
        }
        return null;
    }

    // ==================== GasMixture ↔ NBT ====================

    /** 序列化整格气体（成分 ListTag + 温度） */
    public static CompoundTag writeMixture(GasMixture gas) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("temp", gas.getTemperature());
        ListTag list = new ListTag();
        for (Map.Entry<IonType, Double> e : gas.getContents().entrySet()) {
            CompoundTag sub = new CompoundTag();
            sub.putString("ion", e.getKey().getId().toString());
            sub.putDouble("mol", e.getValue());
            list.add(sub);
        }
        tag.put("entries", list);
        return tag;
    }

    /** 反序列化整格气体 */
    public static GasMixture readMixture(CompoundTag tag) {
        GasMixture gas = new GasMixture(tag.getDouble("temp"));
        ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag sub = list.getCompound(i);
            IonType ion = findIon(sub.getString("ion"));
            if (ion != null) {
                gas.addGas(ion, sub.getDouble("mol"));
            }
        }
        return gas;
    }
}
