package com.pha.trainees.entity;

import com.pha.trainees.Main;
import com.pha.trainees.registry.ModEntities;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings({"deprecation", "removal"})
public class GoldChickenEntity extends Chicken {

    public GoldChickenEntity(EntityType<? extends Chicken> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Chicken.createAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 5.0F)
                .add(Attributes.ATTACK_SPEED, 1.0F)
                ;
    }

    @Override
    protected ResourceLocation getDefaultLootTable() {
        return new ResourceLocation(Main.MODID, "entities/gold_chicken");
    }

    // 重写aiStep方法以修改下蛋行为
    @Override
    public void aiStep() {
        super.aiStep();

        // 复制父类逻辑但修改下蛋部分
        if (!this.level().isClientSide && this.isAlive() && !this.isBaby() && !this.isChickenJockey() && --this.eggTime <= 0) {
            this.playSound(SoundEvents.CHICKEN_EGG, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            //@ 黑蛋
            this.spawnAtLocation(Items.GOLD_INGOT);
            this.gameEvent(GameEvent.ENTITY_PLACE);
            this.eggTime = this.random.nextInt(1500) + 1500;
        }
    }

    // 重写食物检测方法
    @Override
    public boolean isFood(ItemStack stack) {
        return stack.getItem() == Items.GOLD_INGOT;
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
    public GoldChickenEntity getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return ModEntities.GOLD_CHICKEN.get().create(level);
    }

    // 重写经验值获取方法
    @Override
    public int getExperienceReward() {
        return 75;
    }
}