package com.pha.trainees.util.game;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;

public class DoTnt {
    private DoTnt() {}


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
    // 注意：surface_diffusion[i] 表示第 i 个 TNT 相对中心在 x/y/z 三个轴上的偏移量（可任意组合）
    boolean[] surface_is = new boolean[8];  //设置第i面是否生成tnt
    double[] surface_diffusion = new double[8];  //设置第i个TNT相对中心的偏移量（x/y/z 三轴同量）
    int[] surface_fuse = new int[8];    //设置第i面的引爆时长
    float[] surface_power = new float[8];   //设置第i面的威力


    surface_is[1] = true;
    surface_diffusion[1] = 3.0;
    surface_fuse[1] = 40;
    surface_power[1] = 2.0f;
    //最终效果： 生成面i=1，其距离中心点3格，引爆时间40tick，威力为2.0f

    new DoTnt.Builder(level, x, y, z)
        .setCustomSurfaces(surface_is, surface_diffusion, surface_fuse, surface_power) //传参
        .spawn();


*/

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

        public Builder(Level level, double x, double y, double z) {
            this.level = level;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Builder setFuse(int fuse) {
            this.fuse = fuse;
            return this;
        }

        public Builder setPower(float power) {
            this.power = power;
            return this;
        }

        public Builder setCenter(boolean center) {
            this.center = center;
            return this;
        }

        public Builder setSurfaces(int surfaces, int diffusion) {
            this.surfaces = surfaces;
            this.diffusion = diffusion;
            return this;
        }

        public Builder setCustomSurfaces(boolean[] surface_is, double[] surface_diffusion, int[] surface_fuse, float[] surface_power) {
            this.surface_is = surface_is;
            this.surface_diffusion = surface_diffusion;
            this.surface_fuse = surface_fuse;
            this.surface_power = surface_power;
            return this;
        }

        public void spawn() {
            DoTnt.spawn(level, x, y, z, fuse, power, center, surfaces, diffusion, surface_is, surface_diffusion, surface_fuse, surface_power);
        }
    }



    public static void spawn(Level level, double x, double y, double z, int fuse, float power,
                             boolean center, int surfaces, int diffusion,
                             boolean[] surface_is, double[] surface_diffusion, int[] surface_fuse, float[] surface_power) {



        if (center) {
            explode(level, x, y, z, fuse, power);
            return;
        }

        if (surfaces != 0 && diffusion != 0) {
            for (int i = 1; i <= surfaces ; i ++){
                double xn = (i == 1 ? x + diffusion : (i == 2 ? x - diffusion : x)),
                        yn = (i == 3 ? y + diffusion : (i == 4 ? y - diffusion : y)),
                        zn = (i == 5 ? z + diffusion : (i == 6 ? z - diffusion : z));
                explode(level, xn, yn, zn, fuse, power);
            }
            return;
        }

        // 自定义多面：下标 i ∈ [1,7]，数组长度必须 ≥ 8，越界/空数组时安全跳过
        if (surface_is != null && surface_is.length >= 8
                && surface_diffusion != null && surface_diffusion.length >= 8
                && surface_fuse != null && surface_fuse.length >= 8
                && surface_power != null && surface_power.length >= 8) {
            for (int j = 1; j <= 7; j++) {
                if (!surface_is[j]) continue;
                double xn = x + surface_diffusion[j],
                        yn = y + surface_diffusion[j],
                        zn = z + surface_diffusion[j];
                explode(level, xn, yn, zn, surface_fuse[j], surface_power[j]);
            }
            return;
        }

        explode(level, x, y, z, 0, 0f);

    }

    public static void explode(Level level, double x, double y, double z, int fuse, float power){
        PrimedTnt tnt = new PrimedTnt(level, x, y, z, null) {
            @Override
            protected void explode() {
                level.explode(this, this.getX(), this.getY(0.0625D), this.getZ(),
                        power, Level.ExplosionInteraction.TNT);
            }
        };
        level.playSound(null, x, y, z, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
        tnt.setFuse(fuse);
        level.addFreshEntity(tnt);
    }
}
