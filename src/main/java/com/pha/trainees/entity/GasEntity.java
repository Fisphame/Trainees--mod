package com.pha.trainees.entity;

import com.pha.trainees.util.math.InverseProportionalFunc;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class GasEntity extends Entity {
    // 实体数据同步器
    public static final EntityDataAccessor<Float> DATA_SIZE =
            SynchedEntityData.defineId(GasEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> DATA_CONCENTRATION =
            SynchedEntityData.defineId(GasEntity.class, EntityDataSerializers.FLOAT);

    // 配置参数
    protected final float initialSize;
    protected final float initialConcentration;
    protected final float minConcentration;
    protected final float expansionRate;
    protected final float initialSizeConcentrationProduct;
    protected final InverseProportionalFunc func;

    public GasEntity(EntityType<?> entityType, Level level,
                     float initialSize, float initialConcentration,
                     float minConcentration, float expansionRate) {
        super(entityType, level);
        this.noPhysics = true;

        this.initialSize = initialSize;
        this.initialConcentration = initialConcentration;
        this.minConcentration = minConcentration;
        this.expansionRate = expansionRate;
        this.initialSizeConcentrationProduct = (float) (Math.pow(initialSize, 3)  * initialConcentration);
        this.func = new InverseProportionalFunc(initialConcentration);

        this.setSize(initialSize);
        this.setConcentration(initialConcentration);
    }

    public GasEntity(EntityType<?> entityType, Level level) {
        this(entityType, level, 1.0f, 1.0f, 0.05f, 0.05f);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_SIZE, initialSize);
        this.entityData.define(DATA_CONCENTRATION, initialConcentration);
    }

    @Override
    public void tick() {
        super.tick();

        // 只在服务器端更新逻辑
        if (!this.level().isClientSide()) {
            // 扩大尺寸
            float currentSize = getSize();
            float newSize = currentSize + expansionRate;
            setSize(newSize);

            // 根据反比关系更新浓度 (浓度 = 浓度*尺寸³ / 尺寸³)
            float newConcentration = (float) func.getY(Math.pow(newSize, 3));

            setConcentration(newConcentration);

            // 检查是否移除实体
            if (newConcentration < minConcentration || currentSize >= 32) {
                this.discard();
                return;
            }

            // 更新碰撞箱
            this.setBoundingBox(calculateBoundingBox());
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        if (compound.contains("Size")) {
            setSize(compound.getFloat("Size"));
        }
        if (compound.contains("Concentration")) {
            setConcentration(compound.getFloat("Concentration"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putFloat("Size", getSize());
        compound.putFloat("Concentration", getConcentration());
    }

    // 计算碰撞箱（边长为尺寸的正方体）
    private AABB calculateBoundingBox() {
        float size = getSize();
        float halfSize = size / 2.0F;
        return new AABB(
                this.getX() - halfSize,
                this.getY() - halfSize,
                this.getZ() - halfSize,
                this.getX() + halfSize,
                this.getY() + halfSize,
                this.getZ() + halfSize
        );
    }

    @Override
    public void setPos(double x, double y, double z) {
        super.setPos(x, y, z);
        this.setBoundingBox(calculateBoundingBox());
    }

    @Override
    public void move(MoverType moverType, Vec3 movement) {
        super.move(moverType, movement);
        this.setBoundingBox(calculateBoundingBox());
    }

    // 重写方法，确保实体不会推动其他实体
    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    // Getter 和 Setter 方法
    public float getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    public void setSize(float size) {
        this.entityData.set(DATA_SIZE, size);
    }

    public float getConcentration() {
        return this.entityData.get(DATA_CONCENTRATION);
    }

    public void setConcentration(float concentration) {
        this.entityData.set(DATA_CONCENTRATION, concentration);
    }

    public float getRadius() {
        return getSize() / 2.0F;
    }

    public float getInitialSize() {
        return initialSize;
    }

    public float getInitialConcentration() {
        return initialConcentration;
    }

    public float getMinConcentration() {
        return minConcentration;
    }

    public float getExpansionRate() {
        return expansionRate;
    }

    public float getInitialSizeConcentrationProduct() {
        return initialSizeConcentrationProduct;
    }
}
