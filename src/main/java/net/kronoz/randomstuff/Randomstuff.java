package net.kronoz.randomstuff;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.kronoz.randomstuff.entity.ModEntities;
import net.kronoz.randomstuff.event.ModEvents;
import net.kronoz.randomstuff.item.ErisItem;
import net.kronoz.randomstuff.item.ModItems;
import net.kronoz.randomstuff.particle.ModParticles;
import net.kronoz.randomstuff.sound.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
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
        ModParticles.registerParticles();
        ModEvents.register();


        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            ItemStack stack = player.getStackInHand(hand);

            if (stack.getItem() instanceof ErisItem) {
                if (entity.getType() == EntityType.IRON_GOLEM) {

                    double reach = 70.0;

                    if (player.squaredDistanceTo(entity) > reach * reach) {
                        player.sendMessage(Text.literal("fail"));
                        return ActionResult.FAIL;

                    }
                }
            }

            return ActionResult.PASS;
        });

    }

}
