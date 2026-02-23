package net.kronoz.randomstuff.sound;

import net.kronoz.randomstuff.Randomstuff;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {
    public static final SoundEvent CRACK = registerSoundEvent("crack");
    public static final SoundEvent PARRY = registerSoundEvent("parry");



    private static SoundEvent registerSoundEvent(String name) {
        Identifier id = Identifier.of(Randomstuff.MOD_ID,name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void registerSounds() {
        Randomstuff.LOGGER.info("Registering ModSounds for " + Randomstuff.MOD_ID);

    }
}
