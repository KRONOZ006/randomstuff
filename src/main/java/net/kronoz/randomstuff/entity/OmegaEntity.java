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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.*;

import java.util.List;
import java.util.UUID;

public class OmegaEntity extends AnimalEntity implements GeoEntity {
    public final AnimationState idleAnimationState = new AnimationState();
    private int idleAnimationTimeout = 0;

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    private static final float BODY_LIGHT_BRIGHTNESS = 5.5f;
    private static final float BODY_LIGHT_RADIUS     = 30f;
    private static final float BODY_R = 1.00f, BODY_G = 1.00f, BODY_B = 0.00f;

    private PointLightData bodyLight;
    private LightRenderHandle<PointLightData> bodyLightHandle;

    private int spawnAge = -1;
    private int maxLifeTicks = 0;
    private UUID ownerUuid = null;

    private double hitboxSize = 1.0;
    private final double growthRate = 0.12;
    private final double minSize = 0.6;
    private final double maxSize = 2.5;

    public OmegaEntity(EntityType<? extends AnimalEntity> type, World world) {
        super(type, world);
        this.ignoreCameraFrustum = true;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 15.0)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.6F)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2F);
    }

    public void setOwner(@Nullable LivingEntity owner) {
        this.ownerUuid = owner != null ? owner.getUuid() : null;
    }

    @Nullable
    public Entity getOwnerEntity() {
        if (ownerUuid == null) return null;
        if (!(this.getWorld() instanceof ServerWorld sw)) return null;
        return sw.getEntity(ownerUuid);
    }

    @Override
    protected void initGoals() {}

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

        if (!this.getWorld().isClient) {
            if (spawnAge < 0) {
                spawnAge = this.age;
                maxLifeTicks = 50 + this.random.nextInt(51); // 5s..10s
            }
            if (this.age - spawnAge >= maxLifeTicks) {
                this.discard();
                return;
            }

            hitboxSize = MathHelper.clamp(hitboxSize + growthRate, minSize, maxSize);
            double half = hitboxSize / 2.0;
            this.setBoundingBox(new Box(
                    this.getX() - half, this.getY(), this.getZ() - half,
                    this.getX() + half, this.getY() + hitboxSize, this.getZ() + half
            ));

            List<Entity> hits = this.getWorld().getOtherEntities(this, this.getBoundingBox(), e ->
                    e.isAlive() && e != this && !e.isSpectator() && !isOwner(e)
            );
            if (!hits.isEmpty()) {
                explodeAndRemove();
                return;
            }
        }

        if (this.getWorld().isClient) {
            setupAnimationStates();
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
        }
    }

    private boolean isOwner(Entity e) {
        return ownerUuid != null && ownerUuid.equals(e.getUuid());
    }

    private void explodeAndRemove() {
        if (this.getWorld() instanceof ServerWorld sw) {
            sw.createExplosion(this, this.getX(), this.getY(), this.getZ(), 4.5f, World.ExplosionSourceType.TNT);
        }
        this.discard();
    }

    private void freeLight() {
        if (bodyLightHandle != null && bodyLightHandle.isValid()) {
            bodyLightHandle.free();
        }
        bodyLightHandle = null;
        bodyLight = null;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        return false;
    }
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }
    @Override
    public boolean isPushable() {
        return false;
    }
    @Override
    protected void pushAway(Entity entity) {}

    @Override public boolean isBreedingItem(ItemStack stack) { return false; }
    @Nullable @Override public PassiveEntity createChild(ServerWorld world, PassiveEntity mate) { return null; }

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

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (ownerUuid != null) nbt.putUuid("Owner", ownerUuid);
        nbt.putInt("SpawnAge", spawnAge);
        nbt.putInt("MaxLife", maxLifeTicks);
        nbt.putDouble("HitboxSize", hitboxSize);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        ownerUuid = nbt.containsUuid("Owner") ? nbt.getUuid("Owner") : null;
        spawnAge = nbt.getInt("SpawnAge");
        maxLifeTicks = nbt.getInt("MaxLife");
        hitboxSize = nbt.contains("HitboxSize") ? nbt.getDouble("HitboxSize") : hitboxSize;
    }
}
