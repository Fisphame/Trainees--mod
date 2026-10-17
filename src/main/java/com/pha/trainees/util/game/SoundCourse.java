package com.pha.trainees.util.game;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.Lazy;

import java.util.List;

import static com.pha.trainees.registry.ModSounds.*;

public class SoundCourse {
    private static Lazy<List<SoundEvent>> registerSoundList(SoundEvent... events) {
        return Lazy.of(() -> List.of(events));
    }

    public static final Lazy<List<SoundEvent>> MINING_SOUNDS = registerSoundList(
            MINING_SOUND_1.get(), MINING_SOUND_2.get(), MINING_SOUND_3.get(),
            MINING_SOUND_4.get(), MINING_SOUND_5.get(), MINING_SOUND_6.get()
    );

    public static final Lazy<List<SoundEvent>> RELEASING_SWORD_WIND_SOUNDS = registerSoundList(
            RELEASING_SWORD_WIND_1.get(), RELEASING_SWORD_WIND_2.get(), RELEASING_SWORD_WIND_3.get(),
            RELEASING_SWORD_WIND_4.get()
    );

    public static final Lazy<List<SoundEvent>> FINAL_MINING_SOUNDS = registerSoundList(
            FINAL_MINING_SOUND_1.get(), FINAL_MINING_SOUND_2.get(), FINAL_MINING_SOUND_3.get(),
            FINAL_MINING_SOUND_4.get(), FINAL_MINING_SOUND_5.get()
    );

    public static final Lazy<List<SoundEvent>> HIT_SOUNDS = registerSoundList(
            HIT.get(), HEAVY_HIT.get(), HIT_RESET.get(), HEAVY_HIT_RESET.get()
    );

    public static SoundEvent getIndexSound(List<SoundEvent> soundEventList, Player player){
        int index = player.getRandom().nextInt(soundEventList.size());
        return soundEventList.get(index);
    }
    public static SoundEvent getIndexSound(List<SoundEvent> soundEventList, Level level){
        int index = level.getRandom().nextInt(soundEventList.size());
        return soundEventList.get(index);
    }
}
