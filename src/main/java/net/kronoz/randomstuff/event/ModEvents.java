package net.kronoz.randomstuff.event;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.kronoz.randomstuff.item.ErisItem;
import net.kronoz.randomstuff.sound.ModSounds;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ModEvents {
    public static void register() {
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            ItemStack stack = player.getStackInHand(hand);




            if (stack.getItem() instanceof ErisItem){
                if (player.getAttackCooldownProgress(0.5f) >= 1.0f) {



//                        world.playSound(null, pos, ModSounds.PARRY, player.getSoundCategory(), 1f, 2f);

                        Vec3d look = player.getRotationVec(1.0f).normalize();
                        Vec3d playerVel = player.getVelocity();

                        player.setVelocity(look.multiply(-1.6).add(playerVel.multiply(0.2)));

                        return ActionResult.SUCCESS;

                    }


                }



            return ActionResult.PASS;
        });
    }
}
