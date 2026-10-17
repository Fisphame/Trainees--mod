package com.pha.trainees.util.game;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;

public class DiedCode {
    public static class Die{

//    private static final String startTime = "start_time";
//    private static final String requiredTime = "required_time";
//    private static final String waterStartTime = "water_start_time";
//    private static final String requiredDuration = "required_duration";

        // 2HBpO =光照= 2HBp + O2↑
    /*

    public static boolean HBpODecompose(ItemStack stack, ItemEntity entity){
        if (!entity.level().isClientSide) {
            Level level = entity.level();
            // 获取或创建实体标签来存储计时信息
            var tag = entity.getPersistentData();


            // 检查是否已经开始计时
            if (!tag.contains(startTime)) {
                // 记录开始存在时间和随机持续时间
                tag.putLong(startTime, entity.level().getGameTime());

                // 随机0-2s + 惩罚时间
                double randomTime = entity.level().random.nextInt(20);
                double punishTime = Ways.Use.punishmentTimeToSunlight(entity);
                // punishTime ∈ { [1,6.92] ∪ [9999,9999] }
                tag.putDouble(requiredTime, randomTime + punishTime);
                return false;
            }

            // 获取开始时间和要求的持续时间
            long start = tag.getLong(startTime);
            double require = tag.getDouble(ChemicalReaction.requiredTime);
            long currentTime = entity.level().getGameTime();

            // 检查是否已经达到要求的持续时间
            if (currentTime - start >= require) {
                int count = stack.getCount() / 2;
                entity.discard();
                if (count != 0) {
                    // 创建新的物品实体
                    CheWays.AddEntity(level, entity, ModChemistry.ModChemistryItems.CHE_HBP_POWDER.get(), count * 2, true );

                    // 清除标签数据
                    tag.remove(startTime);
                    tag.remove(ChemicalReaction.requiredTime);

                    return true;
                }
            }
        }
        return false;
    }
*/


        // 2Ji + 2H2O == 2JiOH + H2↑
    /*
    public static boolean JiAndH2O(int number, ItemStack stack, ItemEntity entity) {
        if (!entity.level().isClientSide && entity.isInWater()) {
            Level level = entity.level();
            double x = entity.getX(), y = entity.getY(), z = entity.getZ();
            Ways.Use.DoTnt_center(level, x, y, z);

            //移除物品实体 创建物品 获取原物品堆叠个数 创建物品堆叠 创建掉落物实体 设置无敌 设置默认拾取延迟 生成实体
            entity.discard();
            var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(Main.MODID, number==1 ? "che_jioh_nugget" : (number == 2 ?  "che_jioh" : "che_jioh_block" )));
            if (item != null) {
                int count = stack.getCount();
                ItemStack itemStack = new ItemStack(item, count);
                ItemEntity itemEntity = new ItemEntity(level, x, y, z, itemStack);
                itemEntity.setInvulnerable(true);
                itemEntity.setDefaultPickUpDelay();
                level.addFreshEntity(itemEntity);
            }


            return true;
        }
        return false;
    }

     */

        // Bp2 + H2O == HBp + HBpO
    /*
    public static boolean Bp2AndH2O(int number, ItemStack stack, ItemEntity entity, int simpleSubstance) {
        if (!entity.level().isClientSide && entity.isInWater()) {
            Level level = entity.level();
            double x = entity.getX(), y = entity.getY(), z = entity.getZ();
            // 获取或创建实体标签来存储计时信息
            var tag = entity.getPersistentData();

            // 检查是否已经开始计时
            if (!tag.contains(waterStartTime)) {
                // 第一次进入水中，记录开始时间和随机持续时间
                tag.putLong(waterStartTime, entity.level().getGameTime());
                // 随机5-10s 即100-200tick
                tag.putInt(requiredDuration, 100 + entity.level().random.nextInt(100));
                return false;
            }

            // 获取开始时间和要求的持续时间
            long startTime = tag.getLong(waterStartTime);
            int requiredDuration = tag.getInt(ChemicalReaction.requiredDuration);
            long currentTime = entity.level().getGameTime();

            // 检查是否已经达到要求的持续时间
            if (currentTime - startTime >= requiredDuration) {
                int count = stack.getCount() * number * simpleSubstance / 2;
                if (count != 0) {

                    entity.discard();
                    CheWays.AddEntity(level, entity, ModChemistry.ModChemistryItems.CHE_HBP_POWDER.get(), count , true);
                    CheWays.AddEntity(level, entity, ModChemistry.ModChemistryItems.CHE_HBPO_POWDER.get(), count , true);

//                    int rest = stack.getCount() * number * simpleSubstance % 2;
//                    if (rest != 0){
//                        CheWays.AddEntity(level, entity,ModItems.POWDER_ANTI.get(), rest, false );
//                    }

                    // 清除标签数据
                    tag.remove(waterStartTime);
                    tag.remove(ChemicalReaction.requiredDuration);

                    return true;
                }
            }
        } else {
            // 不在水中，清除计时数据
            var tag = entity.getPersistentData();
            if (tag.contains(waterStartTime)) {
                tag.remove(waterStartTime);
                tag.remove(requiredDuration);
            }
        }
        return false;
    }

     */

    /*
public class AbilityHandler {

        private static final int COOLDOWN_TICKS = 10 * 20;
        private static final int HURT_BREAK = 5;

        @SubscribeEvent
        public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
            Level level = event.getLevel();
            Player player = event.getEntity();
            ItemStack stack = player.getMainHandItem();

            // 检查是否持有剑且附魔存在
            if (stack.getItem() instanceof SwordItem &&
                    stack.getEnchantmentLevel(ModEnchantments.ten_thousand_sword.get()) > 0) {
                Item item = stack.getItem();
                float basicDamage = ((SwordItem) stack.getItem()).getDamage();
                float enchantDamage = EnchantmentHelper.getDamageBonus(stack, MobType.UNDEFINED);
                if (Tools.isInstanceofScythe(item)){
                    basicDamage *= 2;
                }

                float damage = basicDamage + enchantDamage;
                if (player.getCooldowns().isOnCooldown(item)){
                    player.displayClientMessage(Component.translatable("msg.trainees.isOnCooldown"), true);
                    return;
                }
                level.playSound(null, new BlockPos(player.getBlockX(), player.getBlockY(), player.getBlockZ()),
                        SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.5F, 1.0F);
                // 触发技能
                int degree = stack.getEnchantmentLevel(ModEnchantments.ten_thousand_sword.get());
                activateAbility(player, damage, degree);

                if (!player.isCreative()) {
                    player.getCooldowns().addCooldown(item, COOLDOWN_TICKS);
                    stack.hurtAndBreak(HURT_BREAK, player, (e) -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
                }
            }
        }

        private static void activateAbility(Player player, float damage, int degree) {
            spawnSwords(player, degree, damage);
        }


        private static void spawnSwords(Player player, int degree, float damage) {
            Direction direction = player.getDirection(); // 玩家朝向
            int totalSwords = degree * 4; // N=附魔等级×n
            double baseX = player.getX();
            double baseY = player.getY() + 1;
            double baseZ = player.getZ();



            // 计算伤害
//            float baseDamage = 0.0f;
//            Multimap<net.minecraft.world.entity.ai.attributes.Attribute, AttributeModifier> modifiers =
//                    stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
//            for (Map.Entry<Attribute, AttributeModifier> entry : modifiers.entries()) {
//                if (entry.getKey() == Attributes.ATTACK_DAMAGE) {
//                    baseDamage = (float) entry.getValue().getAmount();
//                    break;
//                }
//            }
            float re_damage = damage * 2.0f;

            //计算方向
            Direction direction1=player.getDirection();
//            float pitch = player.getXRot();
//            if( -30.0F < pitch && pitch < 30.0F){
//
//            }

            //计算坐标
            for (int i = 0; i < totalSwords; i++) {
                // 计算每对剑的偏移量
                int pairIndex = i / 2;
                //double offset = ( (pairIndex % 8) + 1 ) * 0.5; // 每对距离递增0.5，超过4.0后重置
                //double yOffset = (pairIndex / 8) * 1; // 每8对Y轴增加1

                double offset = pairIndex * 0.5; // 每对距离递增n，无上限
                double yOffset = 0; // Y轴不增加
                ///注：弧形其实是两条等边

/*
                offset:x z的共同偏移
                要使最终效果如下：
                   ·      ·
                  ·        ·
                 ·          ·
                ·            ·
                    player

                东 +x  西-x
                南 +z  北-z
 */
/*
    // 根据朝向计算初始位置
    // 左右分布
    double sideOffset = (i % 2 == 0) ? -offset : offset; //弧形
    sideOffset = sideOffset * 1.25;
    //当剑[i]为奇数数，向正方向；偶数，向负方向
    //double sideOffset = (i % 2 == 0) ? -1 : 1;//直线
    double x, y, z;
    float constIndex = 1; //额定偏移

                switch (direction) {
        case NORTH -> { // 朝北（-Z方向）
            x = baseX + sideOffset;
            z = baseZ - constIndex + offset / 2;
        }
        case SOUTH -> { // 朝南（+Z方向）
            x = baseX + sideOffset;
            //x = baseX * sideOffset;
            z = baseZ + constIndex - offset / 2;
        }
        case EAST -> { // 朝东（+X方向）
            x = baseX + constIndex - offset / 2;
            z = baseZ + sideOffset;
            //z = baseX * sideOffset;
        }
        case WEST -> { // 朝西（-X方向）
            x = baseX - constIndex + offset / 2;
            z = baseZ + sideOffset;
            //z = baseX * sideOffset;
        }
        default -> { // 默认朝南
            x = baseX;
            z = baseZ;
        }
    }

    y = baseY + yOffset;



    // 生成剑实体
    CalledSwordEntity swordEntity = new CalledSwordEntity(
            ModEntities.CALLED_SWORD.get(),
            player.level()

    );

    //传参
                swordEntity.setDamage(re_damage);
                swordEntity.setDirection(direction1);
                swordEntity.setPos(x, y, z);
                swordEntity.setOffPlayer(player);
                player.level().addFreshEntity(swordEntity);
}
        }
                }







 */
    }

    public static class CodeMuseumAndShit {
        // 1 - tnt数组和神秘for循环
        public static void DoTnt(Level level, int x, int y, int z, float power, int surface, int distance){
            int O = 0;
            level.playSound(null, x, y, z, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);

            PrimedTnt[] tnts = new PrimedTnt[]{
                    new PrimedTnt(level, x, y+distance, z, null) {
                        @Override
                        protected void explode() {
                            level.explode(this, this.getX(), this.getY(0.0625D), this.getZ(),
                                    power, Level.ExplosionInteraction.TNT);
                        }
                    },
                    new PrimedTnt(level, x, y-distance, z, null) {
                        @Override
                        protected void explode() {
                            level.explode(this, this.getX(), this.getY(0.0625D), this.getZ(),
                                    power, Level.ExplosionInteraction.TNT);
                        }
                    },
                    new PrimedTnt(level, x+distance, y, z, null) {
                        @Override
                        protected void explode() {
                            level.explode(this, this.getX(), this.getY(0.0625D), this.getZ(),
                                    power, Level.ExplosionInteraction.TNT);
                        }
                    },
                    new PrimedTnt(level, x-distance, y, z, null) {
                        @Override
                        protected void explode() {
                            level.explode(this, this.getX(), this.getY(0.0625D), this.getZ(),
                                    power, Level.ExplosionInteraction.TNT);
                        }
                    },
                    new PrimedTnt(level, x, y, z+distance, null) {
                        @Override
                        protected void explode() {
                            level.explode(this, this.getX(), this.getY(0.0625D), this.getZ(),
                                    power, Level.ExplosionInteraction.TNT);
                        }
                    },
                    new PrimedTnt(level, x, y, z-distance, null) {
                        @Override
                        protected void explode() {
                            level.explode(this, this.getX(), this.getY(0.0625D), this.getZ(),
                                    power, Level.ExplosionInteraction.TNT);
                        }
                    },
            };


            //欸嘿^v^
            for (int i = O; !(i >= surface) ; i -= -1) {
                tnts[i].setFuse(O);
                level.addFreshEntity(tnts[i]);
            }
        }

        // 2 - DoTnt加强版!
        // 战绩：干出60+个警告
        public static class RealDoTnt {


            /// 用法如下： 仅中心  快捷多面  自定义爆炸
/*
    // 1. 仅中心爆炸
    new DoTnt.Builder(level, x, y, z)
        .setCenter(true)  //必须设置为true设置为中心
        .setFuse(80)     //设置引爆时间为80tick
        .setPower(4.0f)   //设置威力为4.0f
        .spawn();       //触发爆炸

    // 2. 快捷多面爆炸
    new DoTnt.Builder(level, x, y, z)
        .setSurfaces(6, 2)   //设置面数为6，相对于中心点(x,y,z)的扩散距离为2
        //面数 ∈ [1,7] 分别对应 (x+n,y,z), (x-n,y,z), (x,y+n,z), (x,y-n,z), (x,y,z+n), (x,y,z-n), (x,y,z)
        .setFuse(80)
        .setPower(4.0f)
        .spawn();

    // 3. 自定义多面爆炸
    // 所有数据下标使用 i ∈ [1,7]，可对应7个任意面
    boolean[] surface_is = new boolean[8];  //设置第i面是否生成tnt
    double[] surface_diffusion = new int[8];  //设置第i面相对于中心的扩散距离
    int[] surface_fuse = new int[8];    //设置第i面的引爆时长
    float[] surface_power = new int[8];   //设置第i面的威力


    surface_is[1] = true;
    surface_diffusion[1] = 3.0;
    surface_fuse[1] = 40;
    surface_power[1] = 2.0f;
    //最终效果： 生成面i=1，其距离中心点3格，引爆时间40tick，威力为2.0f

    new DoTnt.Builder(level, x, y, z)
        .setCustomSurfaces(surface_is, surface_diffusion, surface_fuse, surface_power) //传参
        .spawn();


*/

            /*
            private RealDoTnt() {
                if ( checkIfIfTure(checkIfIfTure(!true || checkIfIfTure(checkIfIfTure(!!!false)) && ( !false || !true ) || !( !!!!!true && !!!false ) && makeSureItsInt(1) != -1
                        || checkIfTrue(!false)? true : (checkIfIfTure(checkIfIfTure(checkIfTrue((false)))) ? false
                        : (checkIfTrue( checkIfTrue(true) && checkIfTrue(!false)) ? true : !false))
                        && checkIfTrue(getVar(1) == makeSureItsInt( getAbsoluteValue(-1) ) )))
                ) {
                    throw new RuntimeException("这个类不能被实例化，因为我说不能");
                }
            }

            public static int makeSureItsInt(int number) {
                Integer temp = Integer.valueOf(number);
                return temp.intValue();
            }

            public static boolean checkIfTrue(boolean value) {
                return value == true ? true : false;
            }

            public static int getVar(int var){
                return var;
            }

            public static double getDouble(double value) {
                return value;
            }

            public static boolean checkIfIfTure(boolean a){
                if (a==true){
                    return !a;
                }
                else if (!a==true){
                    return (Boolean) null;
                }
                if (checkIfIfTure(a)){
                    return a;
                }
                return !a;
            }

            public static int getAbsoluteValue(int b) {
                String input = Integer.toString(b);
                if (input == null) {
                    System.out.print("fuck you");
                    return 1145141919;
                }

                StringBuilder result = new StringBuilder();
                for (char c : input.toCharArray()) {
                    if (c != '-') {
                        result.append(c);
                    }
                }

                int cnm = Integer.parseInt(result.toString());
                return cnm;
            }

            public static class Builder {
                private final Level level;
                private final double x, y, z;
                private int fuse = 0;
                private float power = 4.0f;
                private boolean center = false;
                private int surfaces = 0;
                private int diffusion = 1;
                private boolean[] surface_is;
                private double[] surface_diffusion;
                private int[] surface_fuse;
                private float[] surface_power;

                private int Counter = 0;

                public Builder(Level level, double x, double y, double z) {
                    this.level = level;
                    this.x = x + 0.0;
                    this.y = y * 1.0;
                    this.z = z / 1.0;

                    for (int i = 0; i < 1; i++) {
                        Counter++;
                    }
                }

                public Builder setFuse(int fuse) {
                    this.fuse =  makeSureItsInt( getVar( getAbsoluteValue( getVar( makeSureItsInt(fuse) ) ) ) );
                    return this;
                }

                public Builder setPower(float power) {
                    this.power = power * (float) getDouble(1.0f);
                    return this;
                }

                public Builder setCenter(boolean center) {
                    this.center = checkIfTrue(center);
                    return this;
                }

                public Builder setSurfaces(int surfaces, int diffusion) {
                    if (surfaces >= 0) {
                        this.surfaces = surfaces;
                    } else {
                        this.surfaces = getAbsoluteValue(surfaces);
                    }

                    this.diffusion = diffusion + getVar(1);
                    return this;
                }

                public Builder setCustomSurfaces(boolean[] surface_is, double[] surface_diffusion, int[] surface_fuse, float[] surface_power) {
                    for (int i = 0; i < surface_is.length; i++) {
                        if (i < this.surface_is.length) {
                            this.surface_is[i] = surface_is[i];
                        }
                    }

                    for (int i = 0; i < surface_diffusion.length; i++) {
                        if (i < this.surface_diffusion.length) {
                            this.surface_diffusion[i] = surface_diffusion[i];
                        }
                    }

                    for (int i = 0; i < surface_fuse.length; i++) {
                        if (i < this.surface_fuse.length) {
                            this.surface_fuse[i] = surface_fuse[i];
                        }
                    }

                    for (int i = 0; i < surface_power.length; i++) {
                        if (i < this.surface_power.length) {
                            this.surface_power[i] = surface_power[i];
                        }
                    }
                    return this;
                }

                public void spawn() {
                    if (level != null) {
                        if (!(level == null)) {
                            RealDoTnt.spawn(level, x, y, z,
                                    fuse, power, center, surfaces, diffusion,
                                    surface_is, surface_diffusion, surface_fuse, surface_power);
                        }
                    }
                }
            }



            public static void spawn(Level level, double x, double y, double z, int fuse, float power,
                                     boolean center, int surfaces, int diffusion,
                                     boolean[] surface_is, double[] surface_diffusion, int[] surface_fuse, float[] surface_power) {

                if (center == true && center != false) {
                    explode(level, x, y, z, fuse, power);
                    return;
                }

                if (surfaces != 0 && diffusion != 0 && !(surfaces == 0 || diffusion == 0) && !checkIfTrue(center)) {
                    int counter = 1;
                    while (counter <= surfaces) {
                        double xn = x, yn = y, zn = z;

                        switch (counter) {
                            case 2: xn = x - diffusion;    break;
                            case  5: zn = z  + diffusion ; break ;
                            case 1  : xn  = x + diffusion;  break;
                            case 6  : zn  = z- diffusion ;   break;
                            case  4: yn =  y  - diffusion;break  ;
                            case 3 : yn  = y +   diffusion;  break;

                            default                                  :
                                //f u c k  y o u
                        }

                        explode(level, xn, yn, zn, fuse, power);
                        counter = counter + makeSureItsInt(3) - ( checkIfTrue(true) ? 2 : -2 ) ;
                        if(checkIfTrue(true) != !false){
                            counter--;
                            counter--;
                            counter--;
                            counter--;
                        }

                    }
                    return                 ;
                }

                boolean hasCustomSurfaces = false;
                if (surface_power != null) {
                    if (surface_power.length > 0) {
                        hasCustomSurfaces = true;
                    }
                }

                if (hasCustomSurfaces && surface_fuse != null && surface_fuse.length > 0 && surface_diffusion != null && surface_diffusion.length > 0) {
                    for (int j = 1; j <= 7; j++) {
                        if (surface_is != null && j < surface_is.length) {
                            if (surface_is[j] == true) {
                                double xn = x;
                                double yn = y;
                                double zn = z;

                                xn = xn + surface_diffusion[j];
                                yn = yn + surface_diffusion[j];
                                zn = zn + surface_diffusion[j];

                                explode(level, getDouble(xn), getDouble(yn), getDouble(zn), surface_fuse[j], surface_power[j]);
                            }
                        }
                    }
                    return;
                }

                if (true) {
                    explode(level, x, y, z, 0, 0f);
                }
            }

            public static void explode(Level level, double x, double y, double z, int fuse, float power){

                PrimedTnt tnt = null;
                if (level != null) {
                    tnt = new PrimedTnt(level, x, y, z, null) {
                        @Override
                        protected void explode() {
                            level.explode(this, this.getX(), this.getY(0.0625D), this.getZ(),
                                    power, Level.ExplosionInteraction.TNT);
                        }
                    };
                }

                if (level != null) {
                    level.playSound(null, x, y, z, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
                }

                if (tnt != null && checkIfTrue( true ) && makeSureItsInt(fuse) != -2147483648 ) {
                    tnt.setFuse(makeSureItsInt( getAbsoluteValue(getVar(fuse    )     )    )    );
                }

                if (level != null && tnt != null) {
                    level.addFreshEntity(tnt);
                }
            }

        */
        }

        // 3 - Lazy嵌套！
        // 战绩：使编译器出bug！ 所以只好注释了……
        /*
    public static final
    Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<
            Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<
                    Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<
                            Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<
                                    Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<
                                            Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<
                                                    Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<
                                                            Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<
                                                                    Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<
                                                                            Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<
                                                                                    Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<
                                                                                            Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<
                                                                                                    Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<
                                                                                                            Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<
                                                                                                                    Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<
                                                                                                                            Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<
            ReactionSystem.RCondition
            >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
            IS_LAZY_JI =
            Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(
                    Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(
                            Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(
                                    Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(
                                            Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(
                                                    Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(
                                                            Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(
                                                                    Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(
                                                                            Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(
                                                                                    Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(
                                                                                            Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(
                                                                                                    Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(
                                                                                                            Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(
                                                                                                                    Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(
                                                                                                                            Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(
                                                                                                                                    Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(Lazy.of(
                    () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () ->
                            () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () ->
                                    () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () ->
                                            () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () ->
                                                    () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () ->
                                                            () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () ->
                                                                    () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () ->
                                                                            () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () ->
                                                                                    () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () ->
                                                                                            () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () ->
                                                                                                    () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () ->
                                                                                                            () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () ->
                                                                                                                    () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () ->
                                                                                                                            () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () ->
                                                                                                                                    () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () ->
                                                                                                                                            () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () -> () ->
                                                                                                                                                    (stack, entity) -> Tools.isJi(stack)
            ))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))
            ;

     */
    }

    public static class uvu{

    }
}
