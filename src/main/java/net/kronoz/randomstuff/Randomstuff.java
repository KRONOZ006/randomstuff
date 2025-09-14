package net.kronoz.randomstuff;

import net.fabricmc.api.ModInitializer;
import net.kronoz.randomstuff.entity.ModEntities;
import net.kronoz.randomstuff.item.ModItems;
import net.kronoz.randomstuff.sound.ModSounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Randomstuff implements ModInitializer {
    public static final String MOD_ID = "randomstuff";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);


    @Override
    public void onInitialize() {
        ModItems.registerModItems();
        ModEntities.registerModEntities();
        ModSounds.registerSounds();
    }
}
