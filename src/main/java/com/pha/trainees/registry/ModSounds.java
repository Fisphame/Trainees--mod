package com.pha.trainees.registry;

import com.pha.trainees.Main;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

@SuppressWarnings({"deprecation", "removal"})
public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Main.MODID);

    private static RegistryObject<SoundEvent> registerSound(String name) {
        return SOUNDS.register(name,
                () -> SoundEvent.createVariableRangeEvent(
                        new ResourceLocation(Main.MODID, name))
        );
    }

    public static final RegistryObject<SoundEvent> AHKUN_SOUND = registerSound("ahkun_sound");
    public static final RegistryObject<SoundEvent> MINING_SOUND_1 = registerSound("mining_sound_1");
    public static final RegistryObject<SoundEvent> MINING_SOUND_2 = registerSound("mining_sound_2");
    public static final RegistryObject<SoundEvent> MINING_SOUND_3 = registerSound("mining_sound_3");
    public static final RegistryObject<SoundEvent> MINING_SOUND_4 = registerSound("mining_sound_4");
    public static final RegistryObject<SoundEvent> MINING_SOUND_5 = registerSound("mining_sound_5");
    public static final RegistryObject<SoundEvent> MINING_SOUND_6 = registerSound("mining_sound_6");
    public static final RegistryObject<SoundEvent> RELEASING_SWORD_WIND_1 = registerSound("releasing_sword_wind_1");
    public static final RegistryObject<SoundEvent> RELEASING_SWORD_WIND_2 = registerSound("releasing_sword_wind_2");
    public static final RegistryObject<SoundEvent> RELEASING_SWORD_WIND_3 = registerSound("releasing_sword_wind_3");
    public static final RegistryObject<SoundEvent> RELEASING_SWORD_WIND_4 = registerSound("releasing_sword_wind_4");
    public static final RegistryObject<SoundEvent> FINAL_MINING_SOUND_1 = registerSound("final_mining_sound_1");
    public static final RegistryObject<SoundEvent> FINAL_MINING_SOUND_2 = registerSound("final_mining_sound_2");
    public static final RegistryObject<SoundEvent> FINAL_MINING_SOUND_3 = registerSound("final_mining_sound_3");
    public static final RegistryObject<SoundEvent> FINAL_MINING_SOUND_4 = registerSound("final_mining_sound_4");
    public static final RegistryObject<SoundEvent> FINAL_MINING_SOUND_5 = registerSound("final_mining_sound_5");
    public static final RegistryObject<SoundEvent> HIT = registerSound("hit");
    public static final RegistryObject<SoundEvent> HIT_RESET = registerSound("hit_reset");
    public static final RegistryObject<SoundEvent> HEAVY_HIT = registerSound("heavy_hit");
    public static final RegistryObject<SoundEvent> HEAVY_HIT_RESET = registerSound("heavy_hit_reset");
    public static final RegistryObject<SoundEvent> HEAVY_HITS = registerSound("heavy_hits");


}
