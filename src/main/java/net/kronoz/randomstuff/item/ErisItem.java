package net.kronoz.randomstuff.item;

import net.kronoz.randomstuff.entity.ModEntities;
import net.kronoz.randomstuff.entity.OmegaEntity;
import net.kronoz.randomstuff.entity.SawBladeEntity;
import net.kronoz.randomstuff.entity.client.renderer.ErisItemRenderer;
import net.kronoz.randomstuff.entity.client.renderer.SawBladeRenderer;
import net.kronoz.randomstuff.sound.ModSounds;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;

import java.util.function.Consumer;

public class ErisItem extends SwordItem implements GeoItem {
    public ErisItem(ToolMaterial material, float reach, Settings settings) {
        super(material, withAttributes(settings, reach));
    }

    private static final int MAX_CHARGE_TICKS = 400;

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

    }

    private static Settings withAttributes(Settings base, float reach) {
        AttributeModifiersComponent.Builder b = AttributeModifiersComponent.builder();

        b.add(
                EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE,
                new net.minecraft.entity.attribute.EntityAttributeModifier(
                        Identifier.of("randomstuff", "eris_reach"),
                        (double) reach,
                        net.minecraft.entity.attribute.EntityAttributeModifier.Operation.ADD_VALUE
                ),
                AttributeModifierSlot.MAINHAND
        );


        return base.component(DataComponentTypes.ATTRIBUTE_MODIFIERS, b.build());
    }


    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (world.isClient) return;
        if (!(user instanceof PlayerEntity player)) return;

        int chargeTicks = this.getMaxUseTime(stack, user) - remainingUseTicks;
        if (chargeTicks < 6.7f) return; // minimum charge

        float charge = MathHelper.clamp(chargeTicks / 20.0F, 0.0F, 1.0F);

        spawnOmega(world, player, remainingUseTicks);

        player.getItemCooldownManager().set(this, 20);
        player.incrementStat(Stats.USED.getOrCreateStat(this));
    }


    private void spawnOmega(World world, PlayerEntity player, int remainingUseTicks) {
        SawBladeEntity omega = new SawBladeEntity(ModEntities.SAWBLADE, world);

        int chargeTicks = MAX_CHARGE_TICKS - remainingUseTicks;

        float charge = MathHelper.clamp(
                chargeTicks / (float) MAX_CHARGE_TICKS,
                0.1f,
                3.0f
        );

        // Power curve (THIS is what makes it feel good)
        float power = charge * charge;

        float minSpeed = 0.6f;
        float maxSpeed = 4.5f;
        float speed = MathHelper.lerp(power, minSpeed, maxSpeed);

        Vec3d look = player.getRotationVec(1.0F).normalize();

        Vec3d spawnPos = player.getPos()
                .add(look.multiply(1))
                .add(0, 0.2, 0);

        omega.setPosition(spawnPos);
        Vec3d playerVel = player.getVelocity();
        omega.setVelocity(look.multiply(speed).add(playerVel.multiply(0.2)));
        omega.setOwner(player);

        world.spawnEntity(omega);

//        world.playSound(
//                null,
//                player.getBlockPos(),
//                ModSounds.CRACK,
//                SoundCategory.PLAYERS,
//                1.0F,
//                0.8F + charge * 0.6F
//        );
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private ErisItemRenderer renderer;
            @Override
            public BuiltinModelItemRenderer getGeoItemRenderer() {
                if (renderer == null) renderer = new ErisItemRenderer();
                return renderer;
            }
        });
    }



    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return MAX_CHARGE_TICKS;
    }
    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
//
        Vec3d currentVelocity = attacker.getVelocity();
        attacker.setVelocity(currentVelocity.withAxis(Direction.Axis.Y, 0.01D));

//
        if (attacker instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.networkHandler.sendPacket(
                    new EntityVelocityUpdateS2CPacket(serverPlayer)
            );
            serverPlayer.getItemCooldownManager().set(this, 10);
        }

        return true;
    }



    //    @Override
//    public void postDamageEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
//        super.postDamageEntity(stack, target, attacker);
//    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
