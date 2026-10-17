package com.pha.trainees.entity;

import com.pha.trainees.util.game.BlockCourse;
import com.pha.trainees.util.game.ParticleHelper;
import com.pha.trainees.util.game.Tools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class CalledSwordEntity extends Entity {

    Player offPlayer = null;
    private Vec3 moveDirection;  // 使用Vec3代替Direction
    private float damage;
    private int lifespan = 20 * 4;
    private final float speed = 0.75f;
    private boolean enchantBrokenBlockDrop = false;
    private boolean enchantBreakBlock = false;


    public CalledSwordEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.damage = 0.0f;
        this.moveDirection = new Vec3(1, 0, 0); // 默认方向
        this.setDeltaMovement(Vec3.ZERO);
    }

    // 传入伤害
    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setOffPlayer(Player player) {
        this.offPlayer = player;
    }

    public void setEnchantBrokenBlockDrop(Boolean blockDrop){
        this.enchantBrokenBlockDrop = blockDrop;
    }

    public void setEnchantBreakBlock(Boolean breakBlock){
        this.enchantBreakBlock = breakBlock;
    }

    // 传入移动方向 (Vec3)
    public void setMoveDirection(Vec3 direction) {
        // 归一化并乘以速度
        this.moveDirection = direction.normalize().scale(speed);
        this.setDeltaMovement(this.moveDirection);
    }

    @Override
    protected void defineSynchedData() {
        // 不需要同步数据，留空
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        // 移动逻辑
        if (lifespan-- <= 0) {
            this.discard();
            return;
        }
        this.setPos(this.position().add(this.getDeltaMovement()));

        // 检测碰撞并造成伤害
        checkHitEntities();
        // 破坏方块
        destroyBlocks();

        if (Tools.chance(level(), 0.5f)){
            // 1. 基础轨迹粒子（服务器广播）
            ParticleHelper.send(
                    level(),
                    ParticleTypes.SOUL_FIRE_FLAME,
                    getX(), getY(), getZ(),
                    2,          // 数量
                    0.1, 0.1, 0.1,  // 偏移
                    0.01       // 速度
            );
        }


//        // 2. 本地增强轨迹（客户端生成）
//        // 圆形轨迹
//        ParticleHelper.spawnCircle(
//                level(),
//                ParticleTypes.ENCHANT,
//                getX(), getY() + 0.5, getZ(),
//                8,      // 数量
//                0.3,    // 半径
//                0.02    // 速度
//        );
//
//        // 自定义颜色粒子
//        ParticleHelper.spawnColoredDust(
//                level(),
//                getX(), getY(), getZ(),
//                0.2f, 0.8f, 1.0f,  // 青色
//                1.5f                // 大小
//        );
    }

    private void checkHitEntities() {
        List<LivingEntity> entities = this.level().getEntitiesOfClass(
                LivingEntity.class,
                this.getBoundingBox().inflate(1.0)
        );
        for (LivingEntity entity : entities) {
            if (entity == offPlayer) continue; // 不伤害玩家
            entity.hurt(this.damageSources().magic(), damage);
            if (Tools.chance(level(), 0.5f)){
                for (int i = 0; i < Tools.randomInRange(level(), 1, 3); i++){
                    ParticleHelper.sendGradientDust(
                            level(),
                            this.getX() + Tools.randomInRange(level(), -1f, 1f),
                            this.getY() + Tools.randomInRange(level(), -1f, 1f),
                            this.getZ() + Tools.randomInRange(level(), -1f, 1f),
                            this.getX() + Tools.randomInRange(level(), -0.5f, 0.5f),
                            this.getY() + Tools.randomInRange(level(), -0.5f, 0.5f),
                            this.getZ() + Tools.randomInRange(level(), -0.5f, 0.5f),
                            Tools.randomInRange(level(), 5, 10),
                            ParticleHelper.getRandomColor().x,
                            ParticleHelper.getRandomColor().y,
                            ParticleHelper.getRandomColor().z,
                            ParticleHelper.getRandomColor().x,
                            ParticleHelper.getRandomColor().y,
                            ParticleHelper.getRandomColor().z,
                            1.5f,
                            0.5f
                    );
                }
            }
        }
    }

    private void destroyBlocks() {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    double xn = this.getX() + dx, yn = this.getY() + dy, zn = this.getZ() + dz;
                    pos.set(xn, yn, zn);
                    BlockState blockState = level().getBlockState(pos);
                    Block block = blockState.getBlock();
                    if (enchantBreakBlock && BlockCourse.canBreak(block)) {
                        this.level().destroyBlock(pos, enchantBrokenBlockDrop); // 生成掉落物
                        if (Tools.chance(level(), 0.2f)){
                            ParticleHelper.spawnExplosionEffect(
                                    level(),
                                    xn, yn, zn,
                                    0.05f
                            );
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // 从 NBT 标签读取数据（若需持久化数据）
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // 将数据写入 NBT 标签（若需持久化数据）
    }
}