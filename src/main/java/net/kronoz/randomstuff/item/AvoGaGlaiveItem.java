package net.kronoz.randomstuff.item;

import net.kronoz.randomstuff.entity.ModEntities;
import net.kronoz.randomstuff.entity.OmegaEntity;
import net.kronoz.randomstuff.sound.ModSounds;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class AvoGaGlaiveItem extends SwordItem {
    public AvoGaGlaiveItem(ToolMaterial material, int attackDamage, float attackSpeed, Settings settings) {
        super(material, settings);
    }




    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        world.playSound(null, user.getBlockPos(),
                 ModSounds.CRACK,
                SoundCategory.PLAYERS, 0.6f, 0.1f);
        if (!world.isClient) {
            OmegaEntity pig = new OmegaEntity(ModEntities.OMEGA, world);

            // Set position in front of the player
            pig.setYaw(user.getHeadYaw());
            pig.updatePosition(user.getX(), user.getY() , user.getZ());



            // Calculate forward motion from player's rotation
            float yaw = user.getYaw();
            float pitch = user.getPitch();

            double x = -Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));
            double y = -Math.sin(Math.toRadians(pitch));
            double z = Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));

            double speed = 1.5; // Adjust how fast it flies
            pig.setVelocity(x * speed, 0, z * speed);

            pig.velocityModified = true;// Apply the velocity

            pig.setNoDrag(true);



            world.spawnEntity(pig);

            }







        return TypedActionResult.success(itemStack, world.isClient());
    }



    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        this.playStopUsingSound(user);
        return stack;
    }

    private void playStopUsingSound(LivingEntity user) {
        user.playSound(SoundEvents.ITEM_SPYGLASS_STOP_USING, 1.0F, 1.0F);
    }
}
