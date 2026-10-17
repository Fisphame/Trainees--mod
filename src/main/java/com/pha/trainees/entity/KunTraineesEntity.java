package com.pha.trainees.entity;

import com.pha.trainees.Main;
import com.pha.trainees.registry.ModEntities;
import com.pha.trainees.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

@SuppressWarnings({"deprecation", "removal"})
public class KunTraineesEntity extends Chicken {
    public KunTraineesEntity(EntityType<? extends Chicken> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Chicken.createAttributes()
                .add(Attributes.MAX_HEALTH, 6.0) // 可调整属性值
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 2.5F)
                .add(Attributes.ATTACK_SPEED, 2.5F)
                ;
    }

    @Override
    protected ResourceLocation getDefaultLootTable() {
        return new ResourceLocation(Main.MODID, "entities/kun_trainees"); // 指向自定义战利品表
    }

    // 重写aiStep方法以修改下蛋行为
    @Override
    public void aiStep() {
        super.aiStep();

        // 复制父类逻辑但修改下蛋部分
        if (!this.level().isClientSide && this.isAlive() && !this.isBaby() && !this.isChickenJockey() && --this.eggTime <= 0) {
            this.playSound(SoundEvents.CHICKEN_EGG, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            //@ 只因蛋
            this.spawnAtLocation(ModItems.KUN_EGG.get());
            this.gameEvent(GameEvent.ENTITY_PLACE);
            this.eggTime = this.random.nextInt(6000) + 6000;
        }
    }

    // 重写食物检测方法
    @Override
    public boolean isFood(ItemStack stack) {
        var item = stack.getItem();
        return item == ModItems.KUN_NUGGET.get();
    }

    // 重写环境音效方法（自定义音效待补充：ogg 资源后续添加后在此返回专属音效）
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.CHICKEN_AMBIENT;
    }

    // 重写受伤音效方法（自定义音效待补充）
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.CHICKEN_HURT;
    }

    // 重写死亡音效方法（自定义音效待补充）
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CHICKEN_DEATH;
    }

    // 重写踏步音效方法（自定义音效待补充）
    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        this.playSound(SoundEvents.CHICKEN_STEP, 0.15F, 1.0F);
    }

    // 重写繁殖方法，生成同类型实体
    @Override
    public KunTraineesEntity getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return ModEntities.KUN_TRAINEES.get().create(level);
    }
}
