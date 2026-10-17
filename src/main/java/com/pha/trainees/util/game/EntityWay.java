package com.pha.trainees.util.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class EntityWay {

    // ==================== 核心方法 ====================

    /**
     * 获取玩家前方扇形区域内最近的实体
     *
     * @param player 玩家实体
     * @param maxDistance 最大距离（米/方块）
     * @param angleDegrees 角度范围（度数），例如 30 表示玩家前方左右各30度
     * @return 符合条件的最近实体，如果没有则返回 null
     */
    @Nullable
    public static Entity getNearestEntityInFront(Player player, double maxDistance, float angleDegrees) {
        return getNearestEntityInFront(player, maxDistance, angleDegrees, null);
    }

    /**
     * 获取玩家前方扇形区域内最近的实体（带过滤条件）
     *
     * @param player 玩家实体
     * @param maxDistance 最大距离
     * @param angleDegrees 角度范围
     * @param filter 实体过滤器，可以按类型、队伍等过滤
     * @return 符合条件的最近实体
     */
    @Nullable
    public static Entity getNearestEntityInFront(Player player, double maxDistance, float angleDegrees,
                                                 @Nullable Predicate<Entity> filter) {
        if (player == null) {
            return null;
        }

        // 构建扇形区域AABB（性能优化）
        AABB searchArea = createSectorAABB(player, maxDistance);

        // 收集区域内的所有实体
        List<Entity> entities = player.level().getEntities(player, searchArea, getDefaultFilter(player, filter));

        // 如果没有实体，直接返回null
        if (entities.isEmpty()) {
            return null;
        }

        // 如果有多个实体，按规则排序选择
        return selectBestEntity(player, entities, maxDistance, angleDegrees);
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建扇形区域的AABB（用于快速筛选）
     */
    private static AABB createSectorAABB(Player player, double maxDistance) {
        Vec3 playerPos = player.position();
        double radius = maxDistance;

        // 创建一个以玩家为中心，半径为maxDistance的立方体区域
        // 这是第一次粗略筛选，后续会进行精确的扇形和距离判断
        return new AABB(
                playerPos.x - radius, playerPos.y - radius, playerPos.z - radius,
                playerPos.x + radius, playerPos.y + radius, playerPos.z + radius
        );
    }

    /**
     * 获取默认过滤器（排除玩家自己）
     */
    private static Predicate<Entity> getDefaultFilter(Player player, @Nullable Predicate<Entity> customFilter) {
        Predicate<Entity> baseFilter = entity ->
                entity != null &&
                        entity != player &&
                        entity.isAlive() &&
                        !entity.isSpectator();

        if (customFilter != null) {
            return baseFilter.and(customFilter);
        }

        return baseFilter;
    }

    /**
     * 从实体列表中选择最佳实体
     */
    @Nullable
    private static Entity selectBestEntity(Player player, List<Entity> entities,
                                           double maxDistance, float angleDegrees) {
        // 转换为流，进行筛选和排序
        Optional<Entity> bestEntity = entities.stream()
                // 筛选在扇形区域内的实体
                .filter(entity -> isInSector(player, entity, maxDistance, angleDegrees))
                // 按照规则排序
                .min(createEntityComparator(player));

        return bestEntity.orElse(null);
    }

    /**
     * 判断实体是否在玩家前方的扇形区域内
     */
    private static boolean isInSector(Player player, Entity target, double maxDistance, float angleDegrees) {
        // 1. 检查距离
        double distance = player.distanceTo(target);
        if (distance > maxDistance) {
            return false;
        }

        // 2. 检查角度
        float angle = calculateAngleBetween(player, target);
        return angle <= angleDegrees / 2.0f; // 除以2是因为参数是总角度
    }

    /**
     * 计算玩家看向与目标方向的夹角（度数）
     */
    private static float calculateAngleBetween(Player player, Entity target) {
        // 玩家视线方向向量（水平方向）
        Vec3 lookVec = player.getLookAngle();
        Vec3 lookVecHorizontal = new Vec3(lookVec.x, 0, lookVec.z).normalize();

        // 玩家到目标的向量（水平方向）
        Vec3 toTarget = target.position().subtract(player.position());
        Vec3 toTargetHorizontal = new Vec3(toTarget.x, 0, toTarget.z).normalize();

        // 如果目标就在玩家位置，返回0度
        if (toTargetHorizontal.lengthSqr() < 1e-6 || lookVecHorizontal.lengthSqr() < 1e-6) {
            return 0.0f;
        }

        // 计算点积并限制在[-1, 1]范围内
        double dot = lookVecHorizontal.dot(toTargetHorizontal);
        dot = Math.max(-1.0, Math.min(1.0, dot));

        // 计算夹角（弧度转度数）
        return (float) Math.toDegrees(Math.acos(dot));
    }

    /**
     * 创建实体比较器（按距离和角度排序）
     */
    private static Comparator<Object> createEntityComparator(Player player) {
        return Comparator
                // 首先按距离排序
                .comparingDouble(entity -> player.distanceToSqr((Entity) entity))
                // 如果距离相等（在很小误差范围内），按角度排序
                .thenComparingDouble(entity -> calculateAngleBetween(player, (Entity) entity));
    }

    // ==================== 高级功能 ====================

    /**
     * 获取玩家前方最近的生物实体（排除其他实体类型）
     */
    @Nullable
    public static LivingEntity getNearestLivingEntityInFront(Player player, double maxDistance, float angleDegrees) {
        return getNearestLivingEntityInFront(player, maxDistance, angleDegrees, null);
    }

    /**
     * 获取玩家前方最近的生物实体（带过滤条件）
     */
    @Nullable
    public static LivingEntity getNearestLivingEntityInFront(Player player, double maxDistance, float angleDegrees,
                                                             @Nullable Predicate<LivingEntity> filter) {
        Predicate<Entity> entityFilter = entity -> {
            if (!(entity instanceof LivingEntity living)) {
                return false;
            }
            return filter == null || filter.test(living);
        };

        Entity result = getNearestEntityInFront(player, maxDistance, angleDegrees, entityFilter);
        return result instanceof LivingEntity ? (LivingEntity) result : null;
    }

    /**
     * 获取玩家前方最近的敌对生物
     */
    @Nullable
    public static LivingEntity getNearestHostileInFront(Player player, double maxDistance, float angleDegrees) {
        return getNearestLivingEntityInFront(player, maxDistance, angleDegrees,
                living -> !living.isAlliedTo(player) && living.canBeSeenByAnyone());
    }

    /**
     * 获取玩家前方最近的友好生物
     */
    @Nullable
    public static LivingEntity getNearestFriendlyInFront(Player player, double maxDistance, float angleDegrees) {
        return getNearestLivingEntityInFront(player, maxDistance, angleDegrees,
                living -> living.isAlliedTo(player));
    }

    // ==================== 调试和可视化 ====================

    /**
     * 调试：获取扇形区域内的所有实体列表（按优先级排序）
     */
    public static List<Entity> getAllEntitiesInFront(Player player, double maxDistance, float angleDegrees) {
        if (player == null || player.level() == null) {
            return List.of();
        }

        AABB searchArea = createSectorAABB(player, maxDistance);
        List<Entity> entities = player.level().getEntities(player, searchArea,
                entity -> entity != null && entity != player && entity.isAlive());

        return entities.stream()
                .filter(entity -> isInSector(player, entity, maxDistance, angleDegrees))
                .sorted(createEntityComparator(player))
                .toList();
    }

    /**
     * 获取实体的详细信息（用于调试）
     */
    public static String getEntityInfo(Player player, Entity target) {
        if (player == null || target == null) {
            return "Invalid entities";
        }

        double distance = player.distanceTo(target);
        float angle = calculateAngleBetween(player, target);
        String type = target.getType().getDescription().getString();

        return String.format("%s - 距离: %.2f, 角度: %.1f°, 位置: (%.1f, %.1f, %.1f)",
                type, distance, angle,
                target.getX(), target.getY(), target.getZ());
    }



    /**
     *  获取两实体的偏航角差值
     */
    public static float getYawDiffer(Entity entity1, Entity entity2){
        return Math.abs(getYaw(entity1) - getYaw(entity2));
    }

    /**
     *  获取两实体的俯仰角差值
     */
    public static float getPitchDiffer(Entity entity1, Entity entity2){
        return Math.abs(getPitch(entity1) - getPitch(entity2));
    }

    /**
     * 获取实体偏航角（规范化到0-360）
     */
    public static float getYaw(Entity entity) {
        return Mth.wrapDegrees(entity.getYRot());
    }

    /**
     * 获取实体俯仰角（规范化到-90到90）
     */
    public static float getPitch(Entity entity) {
        return Mth.clamp(entity.getXRot(), -90.0f, 90.0f);
    }

    /**
     * 获取旋转角度（返回数组：[yaw, pitch]）
     */
    public static float[] getRotation(Entity entity) {
        return new float[] {
                getYaw(entity),
                getPitch(entity)
        };
    }

    /**
     * 获取视线方向向量（3D）
     */
    public static Vec3 getLookVector(Entity entity) {
        return entity.getLookAngle();
    }

    /**
     * 获取水平方向向量（2D）
     */
    public static Vec3 getHorizontalLookVector(Entity entity) {
        Vec3 lookVec = entity.getLookAngle();
        return new Vec3(lookVec.x, 0, lookVec.z).normalize();
    }

    /**
     * 获取背后方向向量
     */
    public static Vec3 getBackwardVector(Entity entity) {
        return getHorizontalLookVector(entity).reverse();
    }

    /**
     * 获取右侧方向向量
     */
    public static Vec3 getRightVector(Entity entity) {
        Vec3 forward = getHorizontalLookVector(entity);
        // 叉积：forward × up = right
        return forward.cross(new Vec3(0, 1, 0)).normalize();
    }

    /**
     * 获取左侧方向向量
     */
    public static Vec3 getLeftVector(Entity entity) {
        return getRightVector(entity).reverse();
    }

    // ==================== 方向枚举 ====================

    /**
     * 获取实体朝向的Direction（4方向）
     */
    public static Direction getCardinalDirection(Entity entity) {
        float yaw = getYaw(entity);

        if (yaw >= 315 || yaw < 45) return Direction.SOUTH;
        if (yaw >= 45 && yaw < 135) return Direction.WEST;
        if (yaw >= 135 && yaw < 225) return Direction.NORTH;
        return Direction.EAST; // 225-315
    }

    /**
     * 获取实体朝向的Direction（8方向）
     */
    public static Direction getOrdinalDirection(Entity entity) {
        float yaw = getYaw(entity) + 22.5f;

        if (yaw < 0) yaw += 360;
        int index = (int)(yaw / 45.0f) % 8;

        return Direction.from2DDataValue(index);
    }

    /**
     * 判断实体是否朝向上/下
     */
    public static boolean isLookingUp(Entity entity) {
        return entity.getXRot() < -45.0f;
    }

    public static boolean isLookingDown(Entity entity) {
        return entity.getXRot() > 45.0f;
    }

    public static boolean isLookingHorizontal(Entity entity) {
        float pitch = Math.abs(entity.getXRot());
        return pitch <= 30.0f;
    }

    // ==================== 方向判断 ====================

    /**
     * 判断实体是否朝向某个方向（容忍角度）
     * @param tolerance 容忍角度（度）
     */
    public static boolean isFacingDirection(Entity entity, Direction targetDir, float tolerance) {
        Direction currentDir = getCardinalDirection(entity);

        if (currentDir == targetDir) return true;

        // 检查相邻方向
        float yaw = getYaw(entity);
        float targetYaw = getYawFromDirection(targetDir);

        float diff = Math.abs(Mth.wrapDegrees(yaw - targetYaw));
        return diff <= tolerance;
    }

    /**
     * 判断实体是否看向某个位置
     */
    public static boolean isLookingAt(Entity entity, Vec3 targetPos, float toleranceDegrees) {
        Vec3 eyePos = entity.getEyePosition();
        Vec3 toTarget = targetPos.subtract(eyePos).normalize();
        Vec3 lookVec = entity.getLookAngle();

        double dot = lookVec.dot(toTarget);
        double angleRad = Math.acos(dot);
        double angleDeg = Math.toDegrees(angleRad);

        return angleDeg <= toleranceDegrees;
    }

    // ==================== 方向计算 ====================

    /**
     * 计算从实体位置到目标位置的方向向量
     */
    public static Vec3 getDirectionTo(Entity entity, Vec3 targetPos) {
        Vec3 entityPos = entity.position();
        return targetPos.subtract(entityPos).normalize();
    }

    /**
     * 计算从实体到目标的yaw角度
     */
    public static float getYawTo(Entity entity, Vec3 targetPos) {
        Vec3 entityPos = entity.position();
        double dx = targetPos.x - entityPos.x;
        double dz = targetPos.z - entityPos.z;

        // Minecraft的角度系统：0=南，90=西，180=北，270=东
        double yaw = Math.toDegrees(Math.atan2(dx, dz));

        // 转换为Minecraft的yaw系统
        return (float)Mth.wrapDegrees(yaw);
    }

    /**
     * 计算从实体到目标的pitch角度
     */
    public static float getPitchTo(Entity entity, Vec3 targetPos) {
        Vec3 entityPos = entity.position();
        double dx = targetPos.x - entityPos.x;
        double dy = targetPos.y - (entityPos.y + entity.getEyeHeight());
        double dz = targetPos.z - entityPos.z;

        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        double pitch = -Math.toDegrees(Math.atan2(dy, horizontalDistance));

        return (float)Mth.clamp(pitch, -90.0, 90.0);
    }

    // ==================== 方向转换 ====================

    /**
     * 将Direction转换为yaw角度
     */
    public static float getYawFromDirection(Direction direction) {
        return switch (direction) {
            case SOUTH -> 0.0f;
            case WEST -> 90.0f;
            case NORTH -> 180.0f;
            case EAST -> 270.0f;
            default -> 0.0f;
        };
    }

    /**
     * 将yaw转换为方向字符串
     */
    public static String getDirectionName(float yaw) {
        Direction dir = getCardinalDirectionFromYaw(yaw);

        return switch (dir) {
            case SOUTH -> "南";
            case WEST -> "西";
            case NORTH -> "北";
            case EAST -> "东";
            default -> "未知";
        };
    }

    private static Direction getCardinalDirectionFromYaw(float yaw) {
        float normalized = Mth.wrapDegrees(yaw);

        if (normalized >= 315 || normalized < 45) return Direction.SOUTH;
        if (normalized >= 45 && normalized < 135) return Direction.WEST;
        if (normalized >= 135 && normalized < 225) return Direction.NORTH;
        return Direction.EAST;
    }

    // ==================== 运动方向 ====================

    /**
     * 获取实体运动方向
     */
    public static Vec3 getMovementDirection(Entity entity) {
        Vec3 motion = entity.getDeltaMovement();
        if (motion.lengthSqr() < 0.0001) {
            return Vec3.ZERO;
        }
        return motion.normalize();
    }

    /**
     * 判断实体是否在移动
     */
    public static boolean isMoving(Entity entity) {
        return entity.getDeltaMovement().lengthSqr() > 0.001;
    }

    /**
     * 获取实体移动速度
     */
    public static double getMovementSpeed(Entity entity) {
        return entity.getDeltaMovement().length();
    }

    // ==================== 应用场景 ====================

    /**
     * 使实体看向目标位置
     */
    public static void lookAt(Entity entity, Vec3 targetPos) {
        float yaw = getYawTo(entity, targetPos);
        float pitch = getPitchTo(entity, targetPos);

        entity.setYRot(yaw);
        entity.setXRot(pitch);
        entity.yRotO = yaw;
        entity.xRotO = pitch;
    }

    /**
     * 使实体看向另一个实体
     */
    public static void lookAtEntity(Entity source, Entity target) {
        lookAt(source, target.position());
    }

    /**
     * 平滑转向（插值）
     */
    public static void smoothLookAt(Entity entity, Vec3 targetPos, float delta) {
        float currentYaw = entity.getYRot();
        float currentPitch = entity.getXRot();

        float targetYaw = getYawTo(entity, targetPos);
        float targetPitch = getPitchTo(entity, targetPos);

        // 插值计算
        float newYaw = Mth.rotLerp(delta, currentYaw, targetYaw);
        float newPitch = Mth.rotLerp(delta, currentPitch, targetPitch);

        entity.setYRot(newYaw);
        entity.setXRot(newPitch);
    }


    public static void spawnItemEntity(Level level, Vec3 pos, ItemStack itemStack){
        ItemEntity itemEntity = new ItemEntity(level, pos.x, pos.y, pos.z, itemStack);
        spawnItemEntity(level, itemEntity);
    }
    public static void spawnItemEntity(Level level, BlockPos pos, ItemStack itemStack){
        ItemEntity itemEntity = new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), itemStack);
        spawnItemEntity(level, itemEntity);
    }
    public static void spawnItemEntity(Level level, ItemEntity itemEntity){
        itemEntity.setDefaultPickUpDelay();
        level.addFreshEntity(itemEntity);
    }

    /**
     * 检查两个实体是否接触（边界框相交）
     */
    public static boolean areEntitiesColliding(Entity entity1, Entity entity2) {
        return entity1.getBoundingBox().intersects(entity2.getBoundingBox());
    }

    /**
     * 检查两个实体是否在指定距离内
     */
    public static boolean areEntitiesWithinDistance(Entity entity1, Entity entity2, double distance) {
        return entity1.distanceTo(entity2) <= distance;
    }

    /**
     * 检查实体是否与另一个实体的膨胀边界框相交
     */
    public static boolean isEntityTouchingWithMargin(Entity entity1, Entity entity2, double margin) {
        AABB expandedBox1 = entity1.getBoundingBox().inflate(margin);
        AABB expandedBox2 = entity2.getBoundingBox().inflate(margin);
        return expandedBox1.intersects(expandedBox2);
    }
}
