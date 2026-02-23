package net.kronoz.randomstuff.entity;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.light.data.PointLightData;
import foundry.veil.api.client.render.light.renderer.LightRenderHandle;
import net.kronoz.randomstuff.particle.ModParticles;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.particle.BubblePopParticle;
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
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.*;

import java.util.List;
import java.util.Map;
import java.util.Random;
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


    float minRandom = 1.3f;
    float maxRandom = 2.0f;

    Random random = new Random();

    float pitch = minRandom + random.nextFloat() * (maxRandom - minRandom);

// distance fom owner




    public OmegaEntity(EntityType<? extends AnimalEntity> type, World world) {
        super(type, world);
        this.ignoreCameraFrustum = true;

        this.setInvulnerable(true);




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
    public void takeKnockback(double strength, double x, double z) {
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

            BlockPos pos = this.getBlockPos();

            for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP}) {
                for (int i = 1; i <= 3; i++) { // check 1 to 3 blocks away
                    BlockPos checkPos = pos.offset(dir, i);
                    BlockState state = this.getWorld().getBlockState(checkPos);
                    boolean solid = !state.getCollisionShape(this.getWorld(), checkPos).isEmpty();
                    if (solid) {


                            // Block is solid (has a hitbox)

                            explodeAndRemove();
                            return;

                    }
                }
                BlockPos below = this.getBlockPos().down();
                BlockState belowBlock = this.getWorld().getBlockState(below);;
                boolean solidBelow = !belowBlock.getCollisionShape(this.getWorld(), below).isEmpty();
                if (solidBelow) {
                    explodeAndRemove();
                    return;
                }


                }











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


            sw.spawnParticles(ModParticles.GOLDEN_BURST_PARTICLE, getX(), getY() + 3, getZ(), 1, 0, 0, 0, 0);
            sw.playSound(this, getBlockPos(), SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.AMBIENT, 10, pitch
            );



            Explosion explosion = new Explosion(
                    sw,
                    this.getOwnerEntity(), // source entity
                    null, // use default DamageSource
                    null, // use default ExplosionBehavior
                    getX(), getY(), getZ(),
                    2.5f,
                    false, // no fire
                    Explosion.DestructionType.DESTROY,
                    ParticleTypes.ASH,   // normal explosion particle
                    ParticleTypes.ASH,             // emitter particle for big explosions
                    SoundEvents.ITEM_TRIDENT_THUNDER                // <- proper RegistryEntry<SoundEvent>
            );

            explosion.collectBlocksAndDamageEntities();
            explosion.affectWorld(false);
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
