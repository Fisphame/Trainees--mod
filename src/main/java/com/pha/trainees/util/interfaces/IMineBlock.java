package com.pha.trainees.util.interfaces;

import com.pha.trainees.util.game.ParticleHelper;
import com.pha.trainees.util.game.SoundCourse;
import com.pha.trainees.util.game.Tools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;


public interface IMineBlock {
    default void whatDoesItMeanMining(Level level, BlockPos pos) {
        if (!level.isClientSide()) {
            SoundEvent sound = SoundCourse.getIndexSound(SoundCourse.MINING_SOUNDS.get(), level);
            level.playSound(null, pos, sound, SoundSource.BLOCKS, 0.8F, 1.0F);
        }
    }

    default void finalMining(Level level, BlockPos pos) {
        if (!level.isClientSide()) {
            SoundEvent sound = SoundCourse.getIndexSound(SoundCourse.FINAL_MINING_SOUNDS.get(), level);
            level.playSound(null, pos,
                    sound, SoundSource.BLOCKS, 0.8F, 1.0F + (level.getRandom().nextFloat() - 0.5F) * 0.2F
            );
        }
        ParticleHelper.send(level, ParticleTypes.FLAME, pos.getX(), pos.getY(), pos.getZ(), Tools.randomInRange(level, 5, 10),
                0.3, 0.3, 0.3, 0.05);
    }
}
