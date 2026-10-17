package com.pha.trainees.chemistry.blockentity;

import com.pha.trainees.chemistry.container.AttachmentCorrosion;
import com.pha.trainees.chemistry.container.ICorrodibleAttachment;
import com.pha.trainees.registry.ModChemistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * 阀门的方块实体（§19.32 的"附着件腐蚀"收尾：阀门）。
 *
 * <p><b>为什么不和通/闭状态放一起</b>：阀门的开/关是**方块状态**（{@code ValveBlock.OPEN}）——
 * 那份设计理由（自动进存档、自动同步、无 NBT、无 tick）依然成立，不改。
 * 这里只加**腐蚀度**这一件"跟着方块走的状态量"，它天然属于方块实体。</p>
 *
 * <p>阀门按 {@link ICorrodibleAttachment} 参与容器腐蚀：它装在最靠近内容物的那一格（容器的口），
 * 所以默认材质取耐热钢，与搅拌轴/夹套一致。</p>
 */
public class ValveBlockEntity extends BlockEntity implements ICorrodibleAttachment {

    /** 阀芯/阀体的腐蚀度 0~1（持久化在本方块实体里，跟着方块走）。 */
    private double attachmentCorrosion = 0.0;

    public ValveBlockEntity(BlockPos pos, BlockState state) {
        super(ModChemistry.ModChemistryBlockEntities.VALVE.get(), pos, state);
    }

    @Override
    public double getAttachmentCorrosion() {
        return attachmentCorrosion;
    }

    @Override
    public void applyAttachmentCorrosion(Level level, BlockPos pos, double delta) {
        attachmentCorrosion = AttachmentCorrosion.apply(level, pos, attachmentCorrosion, delta,
                attachmentMaterial().id());
        setChanged();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putDouble(AttachmentCorrosion.NBT_KEY, attachmentCorrosion);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        attachmentCorrosion = tag.getDouble(AttachmentCorrosion.NBT_KEY);
    }
}
