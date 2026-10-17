package com.pha.trainees.entity;

import com.pha.trainees.Main;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

/**
 * 黑坤霸主 - 巨型黑色鸡 Boss
 * 阶段1 (血量 ≥ 50%)：仅近战扑击 (A)
 * 阶段2 (血量 < 50%)：循环 C → B → A → A
 *   A = 近战扑击
 *   B = 冲锋撞击 (附带黑雾粒子)
 *   C = 失明凝视 (对周围所有玩家施加失明)
 */
public class KunAntiBossEntity extends Monster {

    // ===== 可调整数值常量 =====
    public static final float MAX_HEALTH = 250.0F;
    public static final float MOVEMENT_SPEED = 0.4F;
    public static final float ATTACK_DAMAGE_A = 6.0F;      // 扑击伤害
    public static final float ATTACK_DAMAGE_B = 16.0F;     // 冲锋伤害
    public static final float KNOCKBACK_RESISTANCE = 0.7F;

    // 冷却时间 (刻, 20刻 = 1秒)
    public static final int COOLDOWN_MIN_A = 5;           // A 最小冷却
    public static final int COOLDOWN_MIN_B = 50;           // B 最小冷却
    public static final int COOLDOWN_MIN_C = 30;           // C 最小冷却
    public static final int COOLDOWN_VARIATION = 10;       // 冷却波动范围
    public static final int CYCLE_COOLDOWN = 20;           // 轮间冷却

    // 失明效果
    public static final int BLINDNESS_DURATION = 60;      // 失明持续 (刻, 5秒)
    public static final float BLINDNESS_RADIUS = 16.0F;    // 影响半径

    // 冲锋
    public static final float MAX_CHARGE_RANGE = 20.0F;   // 最大冲锋距离（超过则不造成伤害，仅靠近）
    public static final float MIN_CHARGE_RANGE = 2.5F;    // 小于此距离则立即造成伤害
    public static final float CHARGE_SPEED = 1.5F;        // 每刻移动格数（用于计算所需时间）
    private boolean isCharging = false;
    private Vec3 chargeTargetPos = null;   // 冲锋目标位置
    private int chargeRemainingTicks = 0;  // 剩余冲锋刻数
    private boolean chargeWillHit = false; // 本次冲锋是否会命中（用于结束时是否造成伤害）

    // 阶段阈值
    public static final float PHASE_HEALTH_RATIO = 0.5F;   // 血量阈值 (50%)

    // ===== Boss 血条 =====
    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.trainees.kun_anti_boss"),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.PROGRESS
    );

    // ===== 攻击状态 =====
    private int attackCooldown = 0;
    private int cycleCooldown = 0;
    private int currentCycleIndex = 0;          // 循环游标：0=C, 1=B, 2=A, 3=A
    private boolean isPhaseTwo = false;


    // 记录最后一次攻击此Boss的玩家（实例字段，避免多个Boss共享同一攻击者）
    private Player lastAttacker = null;

    public KunAntiBossEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 50;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE_A)
                .add(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    // ===== 同步数据 =====
    private static final EntityDataAccessor<Boolean> DATA_PHASE_TWO =
            SynchedEntityData.defineId(KunAntiBossEntity.class, EntityDataSerializers.BOOLEAN);

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_PHASE_TWO, false);
    }

    // ===== AI 目标 =====
    @Override
    protected void registerGoals() {
        // 基础 AI
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        // 攻击目标
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.goalSelector.addGoal(4, new MoveToTargetGoal());
    }

    // ===== 自定义 AI 任务 (通过 tick 驱动) =====
    @Override
    public void aiStep() {
        super.aiStep();

        Level level = this.level();
        if (level.isClientSide) return;

        if (isCharging) {
            if (chargeTargetPos == null) {
                isCharging = false;
                return;
            }
            // 向目标位置移动
            Vec3 currentPos = this.position();
            Vec3 direction = chargeTargetPos.subtract(currentPos).normalize();
            double distance = currentPos.distanceTo(chargeTargetPos);
            if (distance < 0.5 || chargeRemainingTicks <= 0) {
                // 到达目标或超时，执行命中效果（如果允许）
                if (chargeWillHit) {
                    // 对目标造成伤害和击退
                    Player target = getCurrentTarget();
                    if (target != null && target.isAlive() && distanceToSqr(target) < 9.0) {
                        target.hurt(this.damageSources().mobAttack(this), ATTACK_DAMAGE_B);
                        target.disableShield(true);
                        target.playSound(SoundEvents.SHIELD_BREAK, 1.0F, 1.0F);
                        Vec3 knockback = target.position().subtract(this.position()).normalize().scale(2.0);
                        target.setDeltaMovement(target.getDeltaMovement().add(knockback));
                        target.hurtMarked = true;
                        // 冲锋命中粒子特效
                        if (level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                                    target.getX(), target.getY() + 0.5, target.getZ(),
                                    10, 1.0, 1.0, 1.0, 0.0);
                        }
                    }
                }
                // 结束冲锋状态
                isCharging = false;
                chargeTargetPos = null;
                chargeRemainingTicks = 0;
                chargeWillHit = false;
                // 设置冷却（B的冷却）
                attackCooldown = getRandomCooldown(COOLDOWN_MIN_B);
                return;
            }
            // 继续冲锋：向目标移动
            double moveSpeed = CHARGE_SPEED;
            Vec3 moveVec = direction.scale(Math.min(moveSpeed, distance));
            this.setDeltaMovement(moveVec);
            this.hurtMarked = true;

            // 减少剩余时间（每刻减1）
            chargeRemainingTicks--;
            return; // 冲锋状态下不执行其他攻击
        }

        // 更新血条
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());


        // 检查阶段切换
        boolean phaseTwo = this.getHealth() / this.getMaxHealth() < PHASE_HEALTH_RATIO;
        if (phaseTwo != isPhaseTwo) {
            isPhaseTwo = phaseTwo;
            this.entityData.set(DATA_PHASE_TWO, isPhaseTwo);
            // 阶段切换时重置循环状态
            if (isPhaseTwo) {
                currentCycleIndex = 0;
                cycleCooldown = 0;
            }
        }



        // 获取当前目标 (最近攻击此Boss的玩家，或最近玩家)
        Player target = getCurrentTarget();
        if (target == null) return;



        // 更新攻击状态
        if (attackCooldown > 0) {
            attackCooldown--;
            return;
        }
        if (cycleCooldown > 0) {
            cycleCooldown--;
            return;
        }

        // 阶段1: 仅使用 A (近战扑击)
        if (!isPhaseTwo) {
            performAttackA(target);
            attackCooldown = getRandomCooldown(COOLDOWN_MIN_A);
            return;
        }

        // 阶段2: 循环 C -> B -> A -> A
        performCycleAttack(target);
    }

    // ===== 获取当前目标 =====
    private Player getCurrentTarget() {
        // 优先使用最后一次攻击此Boss的玩家
        if (lastAttacker != null && lastAttacker.isAlive() && distanceToSqr(lastAttacker) < 64.0) {
            if (!lastAttacker.isCreative()) {
                return lastAttacker;
            }
        }
        // 否则找最近的玩家
        return this.level().getEntitiesOfClass(Player.class,
                        new AABB(this.blockPosition()).inflate(32.0))
                .stream()
                .filter(p -> !p.isCreative() && p.isAlive())
                .min((p1, p2) -> Double.compare(distanceToSqr(p1), distanceToSqr(p2)))
                .orElse(null);
    }

    // ===== 执行循环攻击 =====
    private void performCycleAttack(Player target) {
        // 阶段2攻击循环：C(失明) → B(冲锋) → A(扑击) → A(扑击)
        int action = currentCycleIndex % 4;

        switch (action) {
            case 0 -> {
                performAttackC(target);
                attackCooldown = getRandomCooldown(COOLDOWN_MIN_C);
                Main.LOGGER.debug("[Boss] Executed C (Blind), cooldown={}", attackCooldown);
            }
            case 1 -> {
                performAttackB(target);
                attackCooldown = getRandomCooldown(COOLDOWN_MIN_B);
                Main.LOGGER.debug("[Boss] Executed B (Charge), cooldown={}", attackCooldown);
            }
            case 2, 3 -> {
                performAttackA(target);
                attackCooldown = getRandomCooldown(COOLDOWN_MIN_A);
                Main.LOGGER.debug("[Boss] Executed A (Melee), cooldown={}", attackCooldown);
            }
        }

        currentCycleIndex++;

        // 检查是否完成一轮（4次攻击）
        if (currentCycleIndex % 4 == 0) {
            // 轮间冷却
            cycleCooldown = CYCLE_COOLDOWN;
            Main.LOGGER.debug("[Boss] Cycle complete, cycleCooldown={}", CYCLE_COOLDOWN);
        }
    }

    // ===== 攻击A: 近战扑击 =====
    private void performAttackA(Player target) {
        if (target == null) return;
        if (distanceToSqr(target) > 8.0) return; // 攻击范围2.4格左右
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        target.hurt(this.damageSources().mobAttack(this), damage);

        // 击退效果
        Vec3 knockback = target.position().subtract(this.position()).normalize().scale(0.5);
        target.setDeltaMovement(target.getDeltaMovement().add(knockback));
        target.hurtMarked = true;

        // 粒子效果 (普通攻击尘土)
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + 1, target.getZ(),
                    10, 0.5, 0.5, 0.5, 0.1);
        }
    }

    // ===== 攻击B: 冲锋撞击 =====
    private void performAttackB(Player target) {
        if (target == null || isCharging) return;

        double dist = this.distanceToSqr(target);
        double actualDist = Math.sqrt(dist);

        // 情况1：距离大于最大冲锋距离 → 仅靠近，不造成伤害
        if (actualDist > MAX_CHARGE_RANGE) {
            // 设置导航走向目标，但不造成伤害
            getNavigation().moveTo(target, 1.2);
            // 直接进入冷却，不设置冲锋状态
            attackCooldown = getRandomCooldown(COOLDOWN_MIN_B);
            return;
        }

        // 情况2：距离小于最小冲锋距离 → 立即造成伤害和击退
        if (actualDist < MIN_CHARGE_RANGE) {
            // 直接命中
            target.hurt(this.damageSources().mobAttack(this), ATTACK_DAMAGE_B);
            target.disableShield(true);
            target.playSound(SoundEvents.SHIELD_BREAK, 1.0F, 1.0F);
            Vec3 knockback = target.position().subtract(this.position()).normalize().scale(1.2);
            target.setDeltaMovement(target.getDeltaMovement().add(knockback));
            target.hurtMarked = true;
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                        target.getX(), target.getY() + 0.5, target.getZ(),
                        10, 1.0, 1.0, 1.0, 0.0);
            }
            attackCooldown = getRandomCooldown(COOLDOWN_MIN_B);
            return;
        }

        // 情况3：距离在有效范围内 → 开始冲锋
        // 计算所需刻数（向上取整）
        int ticksNeeded = (int) Math.ceil(actualDist / CHARGE_SPEED);
        // 限制最大刻数（防止过长）
        ticksNeeded = Math.min(ticksNeeded, 40); // 最多2秒

        isCharging = true;
        chargeTargetPos = target.position(); // 记录目标位置（在冲锋期间目标可能移动，但我们可以采用动态跟踪，但你要求的是直线冲，所以锁定位置更好）
        chargeRemainingTicks = ticksNeeded;
        chargeWillHit = true; // 这次冲锋会命中

        // 冲锋开始时播发粒子效果（黑雾）
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    this.getX(), this.getY() + 0.5, this.getZ(),
                    20, 0.5, 0.5, 0.5, 0.1);
        }
        // 不设置冷却，由冲锋结束后的 aiStep 设置
    }

    // ===== 攻击C: 失明凝视 =====
    private void performAttackC(Player target) {
        if (target == null) return;

        // 对视线内所有玩家施加失明
        AABB aabb = new AABB(
                this.getX() - BLINDNESS_RADIUS,
                this.getY() - BLINDNESS_RADIUS,
                this.getZ() - BLINDNESS_RADIUS,
                this.getX() + BLINDNESS_RADIUS,
                this.getY() + BLINDNESS_RADIUS,
                this.getZ() + BLINDNESS_RADIUS
        );

        List<Player> players = this.level().getEntitiesOfClass(Player.class, aabb);
        for (Player player : players) {
            if (player.hasLineOfSight(this)) {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_DURATION, 0, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BLINDNESS_DURATION, 0,
                        false, true));

            }
        }

        // 施法粒子特效 (黑雾爆发)
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    this.getX(), this.getY() + 1.5, this.getZ(),
                    30, 3.0, 1.5, 3.0, 0.2);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    this.getX(), this.getY() + 1.5, this.getZ(),
                    20, 2.0, 1.0, 2.0, 0.1);
        }

        // 播放音效
        this.playSound(SoundEvents.WITHER_SPAWN, 2.0F, 0.8F);
    }

    // ===== 获取随机冷却 =====
    private int getRandomCooldown(int min) {
        return min + this.random.nextInt(COOLDOWN_VARIATION);
    }

    // ===== 记录最后一次攻击者 =====
    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity sourceEntity = source.getEntity();
        if (sourceEntity instanceof Player player) {
            this.lastAttacker = player;
        }
        return super.hurt(source, amount);
    }

    // ===== Boss 血条管理 =====

    // 玩家开始看到此实体时添加血条
    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    // 玩家停止看到此实体时移除血条
    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    // 实体被移除时清理血条
    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        this.bossEvent.removeAllPlayers();
    }

    // ===== 音效 =====
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.CHICKEN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.CHICKEN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CHICKEN_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.CHICKEN_STEP, 0.15F, 1.0F);
    }

    // ===== 战利品与经验 =====
    @Override
    public int getExperienceReward() {
        return 200;
    }

    // ===== NBT 持久化 =====
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("PhaseTwo", isPhaseTwo);
        tag.putInt("CycleIndex", currentCycleIndex);
        tag.putInt("AttackCooldown", attackCooldown);
        tag.putInt("CycleCooldown", cycleCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        isPhaseTwo = tag.getBoolean("PhaseTwo");
        currentCycleIndex = tag.getInt("CycleIndex");
        attackCooldown = tag.getInt("AttackCooldown");
        cycleCooldown = tag.getInt("CycleCooldown");
        this.entityData.set(DATA_PHASE_TWO, isPhaseTwo);
    }

    private class MoveToTargetGoal extends Goal {
        @Override
        public boolean canUse() {
            // 当有目标且不在攻击冷却中时，且目标距离大于攻击范围（3格）时使用
            Player target = getCurrentTarget();
            if (target == null) return false;
            return distanceToSqr(target) > 9.0; // 大于3格
        }

        @Override
        public void tick() {
            Player target = getCurrentTarget();
            if (target != null) {
                // 设置移动速度
                getNavigation().moveTo(target, 1.0);
            }
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }
}