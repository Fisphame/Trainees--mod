package com.pha.trainees.util.game;

import com.pha.trainees.registry.*;
import com.pha.trainees.util.math.LogarithmicFunc;
import com.pha.trainees.util.math.MathT;
import com.pha.trainees.util.math.Pair;
import com.pha.trainees.util.math.QuadraticFuncVertexT;
import com.pha.trainees.util.types.NumedItemEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import static java.lang.Math.max;
import static java.lang.Math.min;

// 方法类
@SuppressWarnings({"deprecation", "removal"})
public class Tools {



    public static Pair AFireflyGleamAgainstTheMoon(double a, double b){
        if (a >= b) return new Pair(b, a);
        else return new Pair(a, b);
    }

    // 随机数 （注：游戏内的随机数）
    public static double randomInRange(Level level, double min, double max) {
        Pair pair = AFireflyGleamAgainstTheMoon(min, max);
        min = pair.x1(); max = pair.x2();
        // 使用 nextDouble() 生成 [0,1) 的随机数，然后映射到 [min, max] 区间
        return min + (max - min) * level.random.nextDouble();
    }
    public static double randomInRange(RandomSource random, double min, double max) {
        Pair pair = AFireflyGleamAgainstTheMoon(min, max);
        min = pair.x1(); max = pair.x2();
        return min + (max - min) * random.nextDouble();
    }
    public static int randomInRange(Level level, int min, int max) {
        Pair pair = AFireflyGleamAgainstTheMoon(min, max);
        min = (int) pair.x1(); max = (int) pair.x2();
        // nextInt(bound) 返回 [0, bound)，需 (max - min + 1) 保证含 max
        return min + level.random.nextInt(max - min + 1);
    }
    public static int randomInRange(RandomSource random, int min, int max) {
        Pair pair = AFireflyGleamAgainstTheMoon(min, max);
        min = (int) pair.x1(); max = (int) pair.x2();
        return min + random.nextInt(max - min + 1);
    }
    public static boolean chance(Level level, double probability) {
        // 快速边界检查
        if (probability <= 0.0) return false;
        if (probability >= 1.0) return true;

        return level.random.nextDouble() < probability;
    }
    /**
     * 使用指定随机源的概率判断
     */
    public static boolean chance(RandomSource random, double probability) {
        // 参数检查
        if (probability <= 0.0) return false;
        if (probability >= 1.0) return true;

        return random.nextDouble() < probability;
    }
    /**
     * 带偏见的概率（非均匀分布）
     * @param bias 偏置因子 (0.5=均匀, <0.5=更常false, >0.5=更常true)
     */
    public static boolean biasedChance(RandomSource random, double probability, double bias) {
        if (probability <= 0.0) return false;
        if (probability >= 1.0) return true;

        // 应用偏置
        double biased = Math.pow(probability, MathT.log(bias, 0.5));
        return random.nextDouble() < biased;
    }

    /*
    参考

        效果描述	x 值范围	y 值范围	z 值范围	使用场景
        轻微散开	[-0.1, 0.1]	[0.0, 0.2]	[-0.1, 0.1]	物品轻微分散
        中等散开	[-0.3, 0.3]	[0.1, 0.4]	[-0.3, 0.3]	物品明显分散
        强力散开	[-0.8, 0.8]	[0.3, 0.8]	[-0.8, 0.8]	爆炸效果
        水平散开	[-0.5, 0.5]	[0.0, 0.1]	[-0.5, 0.5]	地面滚动
        向上抛射	[-0.2, 0.2]	[0.5, 1.0]	[-0.2, 0.2]	投掷效果
        向下掉落	[-0.1, 0.1]	[-0.5, -0.1]	[-0.1, 0.1]	重力效果
     */

    public static NumedItemEntities AddEntity(Level level, ItemEntity entity, ItemStack itemStack, int move){
        double x = entity.getX(), y = entity.getY(), z = entity.getZ();
        int count = itemStack.getCount();
        Item item = itemStack.getItem();
        // 分组计算
        int groups = calculateSafeGroups(count);
        // 数组大小和索引处理
        ItemEntity[] itemEntities = new ItemEntity[groups];
        int baseCount = count / groups;
        int remainder = count % groups;

        for (int i = 0; i < groups; i++){
            int now_count = baseCount + (i < remainder ? 1 : 0);
            if (now_count > 0) {
                ItemEntity itemEntity = new ItemEntity(level, x, y, z, new ItemStack(item, now_count));
                if (move == 1){
                    // 根据物品数量调整散开强度
                    double spreadIntensity = min(randomInRange(level, 0.42, 0.005 * count + 0.1), 0.8);
                    double verticalIntensity = min(randomInRange(level, 0.164, 0.001 * count + 0.1), 1.0);

                    itemEntity.setDeltaMovement(
                            randomInRange(level, -spreadIntensity, spreadIntensity),
                            randomInRange(level, verticalIntensity, verticalIntensity),
                            randomInRange(level, -spreadIntensity, spreadIntensity)
                    );
                } else if (move == 2) {
                    double spreadIntensity = min(randomInRange(level, 0.20, 0.005 * count + 0.1), 0.4);
                    double verticalIntensity = min(randomInRange(level, 0.08, 0.001 * count + 0.1), 0.5);

                    itemEntity.setDeltaMovement(
                            randomInRange(level, -spreadIntensity, spreadIntensity),
                            randomInRange(level, verticalIntensity, verticalIntensity),
                            randomInRange(level, -spreadIntensity, spreadIntensity)
                    );
                } else if (move == 3){
                    itemEntity.setDeltaMovement(
                            entity.getDeltaMovement().x,
                            entity.getDeltaMovement().y + 0.1,
                            entity.getDeltaMovement().z
                    );
                }

                itemEntity.setDefaultPickUpDelay();
                itemEntities[i] = itemEntity;
            }
        }
        return new NumedItemEntities(itemEntities, groups);
    }

    // 新增辅助方法：安全计算分组数量
    private static int calculateSafeGroups(int count) {
        if (count <= 0) return 1;

        // 对于小数量物品，使用较小的分组
        if (count <= 8) {
            return min(count, 3);
        }

        // 对于中等数量，使用对数分组但限制范围
        LogarithmicFunc func = new LogarithmicFunc(3);
        int groups = (int) Math.ceil(func.getY(count));
        groups = max(1, min(groups, 64)); // 限制在1-N之间

        return groups;
    }

    public static void AddNIEs(ItemEntity entity, NumedItemEntities itemEntities, boolean Invulnerable){
        for (int i = 0; i < itemEntities.num(); i++){
            ItemEntity itemEntity = itemEntities.itemEntities()[i];

            itemEntity.setInvulnerable(Invulnerable);
            entity.level().addFreshEntity(itemEntity);
        }
    }

    public static int getPowderMultiplier(Item item) {
        if (item == ModItems.POWDER_ANTI.get()) return MathT.POW_9[0];
        if (item == ModItems.POWDER_ANTI_4.get()) return 4;
        if (item == ModItems.POWDER_ANTI_9.get()) return MathT.POW_9[1];
        if (item == Something.PrankItems.POWDER_ANTI_92.get()) return MathT.POW_9[2];
        if (item == Something.PrankItems.POWDER_ANTI_93.get()) return MathT.POW_9[3];
        if (item == Something.PrankItems.POWDER_ANTI_94.get()) return MathT.POW_9[4];
        if (item == Something.PrankItems.POWDER_ANTI_95.get()) return MathT.POW_9[5];
        if (item == Something.PrankItems.POWDER_ANTI_96.get()) return MathT.POW_9[6];
        if (item == Something.PrankItems.POWDER_ANTI_97.get()) return MathT.POW_9[7];
        if (item == Something.PrankItems.POWDER_ANTI_98.get()) return MathT.POW_9[8];

        return 1;
    }

    public static Boolean isJi(Item item) {
        return item == ModItems.KUN_NUGGET.get() ||
                item == ModItems.TWO_HALF_INGOT.get() ||
                item == ModItems.TWO_HALF_INGOT_BLOCK_ITEM.get();
    }

    public static Boolean isJi(ItemStack stack) {
        return isJi(stack.getItem());
    }

    public static Boolean isElementary(Item item){
        return isJi(item) || ItemClassifier.tag(item, ModTags.POWDER_ANTI_2);
    }

    public static Boolean isElementary(ItemStack stack){
        return  isElementary(stack.getItem());
    }

    public static Item[] JiItems = new Item[]{
            ModChemistry.ModChemistryItems.IMPERFECTION.get(),

            ModChemistry.ModChemistryItems.CHE_JIOH_NUGGET.get(),
            ModChemistry.ModChemistryItems.CHE_JIOH_INGOT.get(),
            ModChemistry.ModChemistryBlockItems.CHE_JIOH_BLOCK_ITEM.get(),

            ModChemistry.ModChemistryItems.CHE_JI2O_NUGGET.get(),
            ModChemistry.ModChemistryItems.CHE_JI2O_INGOT.get(),
            ModChemistry.ModChemistryBlockItems.CHE_JI2O_BLOCK_ITEM.get(),

            ModChemistry.ModChemistryItems.CHE_JI2O2_NUGGET.get(),
            ModChemistry.ModChemistryItems.CHE_JI2O2_INGOT.get(),
            ModChemistry.ModChemistryBlockItems.CHE_JI2O2_BLOCK_ITEM.get(),
    };


    /**
     * @param n <p></p>
     *          =1     OH-
     *          <p>
     *          =2     O(-2)
     *          <p>
     *          =3     O(-1)
     */
    public static Item getWhichJiProduct(Item item, int n) {
        return item == ModItems.KUN_NUGGET.get() ?
                JiItems[ (n - 1) * 3 + 1] :
                ( item == ModItems.TWO_HALF_INGOT.get() ?
                        JiItems[(n - 1) * 3 + 2] :
                        (item == ModItems.TWO_HALF_INGOT_BLOCK_ITEM.get() ?
                                JiItems[(n - 1) * 3 + 3] :
                                JiItems[0]
                        )
                );
    }

    public static Item getWhichJiProduct(ItemStack stack, int n) {
        return getWhichJiProduct(stack.getItem(), n);
    }

    public static void DoTnt_center(Level level, double x, double y, double z, float power){
        new DoTnt.Builder(level, x, y, z)
                .setCenter(true)
                .setFuse(0)
                .setPower(power)
                .spawn();
    }
    public static void DoTnt_center(Level level, double x, double y, double z) {
        DoTnt_center(level, x, y, z, 4.0f);
    }
    public static void DoTnt_center(Level level, BlockPos pos, float power) {
        DoTnt_center(level, pos.getX(), pos.getY(), pos.getZ(), power);
    }
    public static void DoTnt_center(Level level, BlockPos pos) {
        DoTnt_center(level, pos.getX(), pos.getY(), pos.getZ(), 4.0f);
    }
    public static void DoTnt_6(Level level, double x, double y, double z, float power, int surfaces, int diffusion){
        new DoTnt.Builder(level, x, y, z)
                .setSurfaces(surfaces, diffusion)
                .setFuse(0)
                .setPower(power)
                .spawn();
    }

    public static double punishmentTimeToSunlight(ItemEntity entity) {
        if ( isInSunlight(entity)) {
            Level level = entity.level();
            long daytime = level.getDayTime() % 24000;

            return new QuadraticFuncVertexT(8 * 10e-8, 6000, 0.5).getY(daytime >= 23000 ? daytime - 24000 : daytime);
        }

        return 9999;
    }

    public static boolean isInSunlight(ItemEntity entity) {
        Level level = entity.level();
        long daytime = level.getDayTime() % 24000;

        // 检查是否为白天雨
        return (daytime >= 0 && daytime <= 13000) || (daytime >= 23000) //是否为白天
                && level.canSeeSky(entity.blockPosition())  //是否看到天空
                && !level.isRaining()
                && !level.isThundering()
//                && level.getBrightness(LightLayer.SKY, entity.blockPosition()) == 15 //天空光照等级
                ;

    }

    /*
    {
    Brain: {memories: {}}, HurtByTimestamp: 0, Attributes: [
    {Base: 0.08d, Name: "forge:entity_gravity"}, {Base: 0.0d, Name: "minecraft:generic.armor"},
    {Base: 0.0d, Name: "minecraft:generic.armor_toughness"},
    {Base: 0.0d, Name: "minecraft:generic.knockback_resistance"},
    {Base: 0.0d, Name: "forge:step_height_addition"},
    {Base: 0.699999988079071d, Name: "minecraft:generic.movement_speed"}],
    Invulnerable: 0b, FallFlying: 0b, ShowArms: 0b, PortalCooldown: 0, AbsorptionAmount: 0.0f,
    FallDistance: 0.0f, DisabledSlots: 0, CanUpdate: 1b, DeathTime: 0s, Pose: {}, Invisible: 0b,
    UUID: [I; -345649566, -227851149, -1952624807, 399656419], Motion: [0.0d, -0.0784000015258789d, 0.0d],
    Small: 0b, Health: 20.0f, Air: 300s, OnGround: 1b, Rotation: [0.0f, 0.0f], HandItems: [{}, {}],
    Pos: [5.5d, -60.0d, 23.5d], Fire: -1s,
    ArmorItems: [
    {id: "minecraft:netherite_boots", Count: 1b, tag: {
    Damage: 0, Trim: {material: "minecraft:quartz", pattern: "minecraft:snout"}
    }},
    {id: "minecraft:iron_leggings", Count: 1b, tag: {
    Damage: 0, Trim: {material: "minecraft:iron", pattern: "minecraft:vex"}
    }},
    {id: "minecraft:leather_chestplate", Count: 1b, tag: {
    Damage: 0, Trim: {material: "minecraft:quartz", pattern: "minecraft:tide"},
    display: {color: 1908001}
    }},
    {id: "minecraft:leather_helmet", Count: 1b, tag: {
    Damage: 0, Trim: {material: "minecraft:netherite", pattern: "minecraft:sentry"},
    display: {color: 10329495}
    }}
    ],
    NoBasePlate: 0b, HurtTime: 0s
    }
     */


}
