package net.kronoz.randomstuff.entity;

import foundry.veil.api.client.render.VeilRenderSystem;


import foundry.veil.api.client.render.light.data.PointLightData;
import foundry.veil.api.client.render.light.renderer.LightRenderHandle;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.*;

import java.util.List;


public class OmegaEntity extends AnimalEntity implements GeoEntity {
    public final AnimationState idleAnimationState = new AnimationState();
    private int idleAnimationTimeout = 0;

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    private static final float BODY_LIGHT_BRIGHTNESS = 5.5f;
    private static final float BODY_LIGHT_RADIUS     = 30f;
    private static final float BODY_R = 1.00f, BODY_G = 1.00f, BODY_B = 0.00f;

    private double hitboxSize = 1.0; // initial size
    private final double growthRate = 0.1; // how much it grows each tick

    private PointLightData bodyLight;
    private LightRenderHandle<PointLightData> bodyLightHandle;

    public OmegaEntity(EntityType<? extends AnimalEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected void initGoals() {


    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 15.0)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.6F)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2F);
    }

    private void setupAnimationStates() {
        if (this.idleAnimationTimeout <= 0) {
            this.idleAnimationTimeout = 15;
            this.idleAnimationState.start(this.age);
        } else {
            --this.idleAnimationTimeout;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient) return;

        this.setupAnimationStates();

        boolean alive = this.isAlive() && !this.isRemoved();

        if (alive) {
            if (bodyLightHandle == null || !bodyLightHandle.isValid()) {
                Vec3d p = this.getPos();
                bodyLight = new PointLightData()
                        .setBrightness(BODY_LIGHT_BRIGHTNESS)
                        .setColor(BODY_R, BODY_G, BODY_B)
                        .setRadius(BODY_LIGHT_RADIUS)
                        .setPosition(p.x, p.y, p.z);
                bodyLightHandle = VeilRenderSystem.renderer().getLightRenderer().addLight(bodyLight);
            } else {
                Vec3d p = this.getPos();
                bodyLight.setPosition(p.x, p.y, p.z);
                bodyLightHandle.markDirty();
            }
        } else {
            freeLight();
        }

        // Increase hitbox size every tick
        if (hitboxSize  <= 10) {
            hitboxSize += growthRate;
        }

        // Update the bounding box dynamically

            double half = hitboxSize / 2.0;
            this.setBoundingBox(new Box(
                    this.getX() - half, this.getY(), this.getZ() - half,
                    this.getX() + half, this.getY() + hitboxSize, this.getZ() + half // height stays 2 blocks
            ));
        }






    private void freeLight() {
        if (bodyLightHandle != null && bodyLightHandle.isValid()) {
            bodyLightHandle.free();
        }
        bodyLightHandle = null;
        bodyLight = null;
    }

    @Override public boolean isBreedingItem(ItemStack stack) { return false; }
    @Nullable @Override public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) { return null; }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar reg) {
        reg.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private PlayState predicate(software.bernie.geckolib.animation.AnimationState<OmegaEntity> s) {

         s.getController().setAnimation(RawAnimation.begin().then("animation.omega.idle", Animation.LoopType.HOLD_ON_LAST_FRAME));
        return PlayState.CONTINUE;
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override
    public void onRemoved() {
        if (this.getWorld().isClient) freeLight();
        super.onRemoved();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (this.getWorld().isClient) freeLight();
        super.remove(reason);
    }
    }
