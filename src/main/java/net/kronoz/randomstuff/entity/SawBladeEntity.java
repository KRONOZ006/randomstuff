package net.kronoz.randomstuff.entity;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.light.data.PointLightData;
import foundry.veil.api.client.render.light.renderer.LightRenderHandle;
import net.kronoz.randomstuff.particle.ModParticles;
import net.kronoz.randomstuff.sound.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.StopSoundS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.*;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public class SawBladeEntity extends AnimalEntity implements GeoEntity {
    public final AnimationState idleAnimationState = new AnimationState();
    private int idleAnimationTimeout = 0;

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    private static final float BODY_LIGHT_BRIGHTNESS = 1.5f;
    private static final float BODY_LIGHT_RADIUS     = 5f;
    private static final float BODY_R = 2.55f, BODY_G = 0.61f, BODY_B = 1.41f;

    private PointLightData bodyLight;
    private LightRenderHandle<PointLightData> bodyLightHandle;

    private int spawnAge = -1;
    private int maxLifeTicks = 0;
    private UUID ownerUuid = null;

    private double lastAttackerVelocity = 0;




    // FOR DAMAGE DELAY
    private int delayedTicks = -1;

    private boolean delayedBareHand;
    private float delayedAmount;
    private LivingEntity delayedAttacker;

    private boolean frozen = false;


    private double hitboxSize = 1.0;
    private final double growthRate = 0.12;
    private final double minSize = 0.6;
    private final double maxSize = 2.5;

    private Vec3d frozenPosition = null;

    private double lastX;
    private double lastZ;

    public float bladeSpin = 0f;        // current rotation
    public float bladeSpinSpeed = 1.5f;

    float minRandom = 1.3f;
    float maxRandom = 2.0f;

    Random random = new Random();

    float pitch = minRandom + random.nextFloat() * (maxRandom - minRandom);

// distance fom owner




    public SawBladeEntity(EntityType<? extends AnimalEntity> type, World world) {
        super(type, world);

        this.ignoreCameraFrustum = true;






    }


    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean didDamage = super.damage(source, amount);

        Entity attacker = source.getAttacker();
        if (!this.getWorld().isClient && attacker instanceof LivingEntity living) {
            ItemStack held = living.getMainHandStack();
            boolean isBareHand =
                    held.isEmpty()
                            || !held.isDamageable()
                            || !held.isEnchantable();

            // arm delay (example: 6 ticks = 0.3s)
            this.setVelocity(0,0,0);
            this.setNoGravity(true);
            lastAttackerVelocity = attacker.getVelocity().length();
            attacker.setVelocity(Vec3d.ZERO);
//HOW TO SET DELAY \/
            this.delayedTicks = 10;
            this.delayedBareHand = isBareHand;
            this.delayedAmount = amount;
            this.delayedAttacker = living;
            frozenPosition = delayedAttacker.getPos();

            frozen = true;
            if (delayedAttacker instanceof ServerPlayerEntity sp) {

               sp.networkHandler.sendPacket(
                      new StopSoundS2CPacket(null, null)
               );

            }
        }

        return didDamage;
    }


    // helper for a random pitch
    private float randomPitch() {
        return 1.3f + random.nextFloat() * (2.0f - 1.3f);
    }
//    @Override
//    public void tick() {
//        super.tick();
//
//        if (lastDamageTaken > 0) {

//            Vec3d lookDir = this.getOwnerEntity() != null
//                    ? this.getOwnerEntity().getRotationVec(1.0F)
//                    : this.getVelocity().normalize(); // fallback
//

//            double speed = lastDamageTaken * 0.5; // tweak 0.5 to taste
//            Vec3d newVel = lookDir.multiply(speed);
//
//
//            this.setVelocity(newVel);
//

//            lastDamageTaken = 0;
//        }
//    }

//I FIGURED IT OUT IT DELAYS BECAUSE THE ATTACK IS COMING WHEN IT HITS SOMETHIGN ELSE I NEED TO MAKE SURE ITS ONLY A DIRECT MELEE ARACT


    @Override
    public void tick() {
        super.tick();

        if (delayedAttacker != null) {




            if (frozen && delayedAttacker instanceof ServerPlayerEntity sp) {
                Vec3d pos = frozenPosition;

               sp.teleport(sp.getServerWorld(), pos.x, pos.y, pos.z, sp.getYaw(), sp.getPitch());
                sp.setVelocity(0, 0, 0);
//                sp.sendMessage(Text.literal(String.valueOf(frozenPosition)));


                sp.horizontalSpeed = 0f;



            } else if (delayedAttacker instanceof ServerPlayerEntity sp) {
                frozenPosition = null;

            }
        }

        if (delayedTicks > 0) {
            delayedTicks--;
        }

        if (delayedTicks == 0) {
            delayedTicks = -1;
            applyDelayedHit();
        }

        if (!this.getWorld().isClient) {
            Vec3d start = this.getPos();
            Vec3d end = start.add(this.getVelocity());

            EntityHitResult hit = ProjectileUtil.getEntityCollision(
                    this.getWorld(),
                    this,
                    start,
                    end,
                    this.getBoundingBox().stretch(this.getVelocity()).expand(0.1),
                    entity -> entity.isAlive() && entity != this && !isOwner(entity)
            );

            if (hit != null) {
                onEntityHit(hit);
            }

            if (this.getVelocity().length() > 2.5f) {
                if (this.getWorld() instanceof ServerWorld sw) {


                    sw.spawnParticles(ModParticles.HEART_BURST_PARTICLE, getX(), getY() + 3, getZ(), 1, 0, 0, 0, 5);
                }

            }

        }
        if (this.getWorld().isClient) {
            setupAnimationStates();
            boolean alive = this.isAlive() && !this.isRemoved();

            if (alive) {
                Vec3d p = this.getPos();
                double speed = this.getVelocity().length();

                // Tune these freely
                float brightness = (float) (
                        BODY_LIGHT_BRIGHTNESS *
                                MathHelper.clamp(speed * 40.0, 0.4, 6.0)
                );

                float radius = BODY_LIGHT_RADIUS + (float)(speed * 4.0);

                if (bodyLightHandle == null || !bodyLightHandle.isValid()) {
                    bodyLight = new PointLightData()
                            .setColor(BODY_R, BODY_G, BODY_B)
                            .setBrightness(brightness)
                            .setRadius(radius)
                            .setPosition(p.x, p.y, p.z);

                    bodyLightHandle =
                            VeilRenderSystem.renderer()
                                    .getLightRenderer()
                                    .addLight(bodyLight);
                } else {
                    bodyLight
                            .setPosition(p.x, p.y, p.z)
                            .setBrightness(brightness)
                            .setRadius(radius);

                    bodyLightHandle.markDirty();
                }
            } else {
                freeLight();
            }
        }
    }


    private void applyDelayedHit() {
        if (delayedAttacker == null) return;
        frozen = false;
        if (delayedBareHand) {
            // Bare-fist hit
            this.setVelocity(Vec3d.ZERO);
            this.setNoGravity(true);
        } else {
            Vec3d lookDir = delayedAttacker.getRotationVec(1.0f).normalize();
            double attackerSpeed = lastAttackerVelocity * 3.5;
            double speed = Math.min(delayedAmount * attackerSpeed, 7);
            Vec3d newVelocity = lookDir.multiply(speed);

            this.setVelocity(newVelocity);
            this.setYaw(delayedAttacker.getYaw());
            this.setPitch(delayedAttacker.getPitch());
            this.setNoGravity(true);


            if (this.getWorld() instanceof ServerWorld sw) {
                sw.spawnParticles(
                        ModParticles.HEART_BURST_PARTICLE,
                        getX(), getY() + 1.5, getZ(),
                        1,
                        0.0, 0.0, 0.0,
                        Math.min(newVelocity.length() * 5, 6.5)
                );
                sw.playSound(
                        this,
                        getBlockPos(),
                        ModSounds.PARRY,
                        SoundCategory.AMBIENT,
                        10,
                        randomPitch()
                );
            }
        }
        if (delayedAttacker instanceof ServerPlayerEntity sp) {
            sp.getWorld().playSound(sp, sp.getBlockPos(), ModSounds.PARRY, SoundCategory.PLAYERS, 6, randomPitch());
        }

        frozen = false;
        frozenPosition = null; // clear it
        delayedAttacker = null;
    }

    @Override
    protected void onRemoval(RemovalReason reason) {
       frozen = false;
        super.onRemoval(reason);
        frozenPosition = null;
    }

    protected void onEntityHit(EntityHitResult hit) {
        Entity target = hit.getEntity();

        // Damage it
        target.damage(
                this.getDamageSources().mobAttack(this),
                6.0F
        );

        // Explosion / effect
        explodeAndRemove();
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 500.0)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.6F)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2F).add(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, 1000);
    }

    @Override
    protected void applyGravity() {
        super.applyGravity();
    }

    public void setOwner(@Nullable LivingEntity owner) {
        this.ownerUuid = owner != null ? owner.getUuid() : null;
    }


    @Override
    public boolean canHit() {
        return true;
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

//    @Override
//    public void takeKnockback(double strength, double x, double z) {
//    }



    @Override
    public boolean hasNoDrag() {
        return true;
    }


    //    @Override
//    public void tick() {
//        super.tick();
//
//        Vec3d v = this.getVelocity();
//
//        float speed = (float)Math.sqrt(v.x * v.x + v.z * v.z);
//
//        this.setYaw(this.getYaw() + speed * 20f);
//        this.prevYaw = this.getYaw();
//    }

//    @Override
//    public void tick() {
//        super.tick();

        // get current velocity
//        Vec3d vel = this.getVelocity();

        // scale factor based on age
//        float scale = (float) this.age * 0.1f; // tweak 0.1F to control speed growth

        // multiply each component by scale
//        Vec3d newVel = new Vec3d(vel.x * scale, vel.y * scale, vel.z * scale);

        // apply it
//        this.setVelocity(newVel);
//    }






// horizontal speed magnitude
//        float speed = this.age;

// optionally add idle spin so it always rotates
//        float idleSpin = 1.5f;
//
//        float maxSpinSpeed = 500f; // <-- maximum spin speed you want
//        bladeSpinSpeed = Math.min(idleSpin + speed, maxSpinSpeed);
//        bladeSpin += bladeSpinSpeed;
//    }


    //    @Override
//    public void tick() {
//        super.tick();
//
//        if (!this.getWorld().isClient) {
//            if (spawnAge < 0) {
//                spawnAge = this.age;
//                maxLifeTicks = 50 + this.random.nextInt(51); // 5s..10s
//            }
//            if (this.age - spawnAge >= maxLifeTicks) {
//                this.discard();
//                return;
//            }
//
//            List<Entity> hits = this.getWorld().getOtherEntities(this, this.getBoundingBox(), e ->
//                    e.isAlive() && e != this && !e.isSpectator() && !isOwner(e)
//            );
//
//            BlockPos pos = this.getBlockPos();
//
//            for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP}) {
//                for (int i = 1; i <= 3; i++) { // check 1 to 3 blocks away
//                    BlockPos checkPos = pos.offset(dir, i);
//                    BlockState state = this.getWorld().getBlockState(checkPos);
//                    boolean solid = !state.getCollisionShape(this.getWorld(), checkPos).isEmpty();
//                    if (solid) {
//
//

//
//                            explodeAndRemove();
//                            return;
//
//                    }
//                }
//                BlockPos below = this.getBlockPos().down();
//                BlockState belowBlock = this.getWorld().getBlockState(below);;
//                boolean solidBelow = !belowBlock.getCollisionShape(this.getWorld(), below).isEmpty();
//                if (solidBelow) {
//                    explodeAndRemove();
//                    return;
//                }
//
//
//                }
//
//
//
//
//
//
//
//
//
//
//
//            if (!hits.isEmpty()) {
//                explodeAndRemove();
//                return;
//            }
//        }
//
//
//
//        if (this.getWorld().isClient) {
//            setupAnimationStates();
//            boolean alive = this.isAlive() && !this.isRemoved();
//            if (alive) {
//                if (bodyLightHandle == null || !bodyLightHandle.isValid()) {
//                    Vec3d p = this.getPos();
//                    bodyLight = new PointLightData()
//                            .setBrightness(BODY_LIGHT_BRIGHTNESS)
//                            .setColor(BODY_R, BODY_G, BODY_B)
//                            .setRadius(BODY_LIGHT_RADIUS)
//                            .setPosition(p.x, p.y, p.z);
//                    bodyLightHandle = VeilRenderSystem.renderer().getLightRenderer().addLight(bodyLight);
//                } else {
//                    Vec3d p = this.getPos();
//                    bodyLight.setPosition(p.x, p.y, p.z);
//                    bodyLightHandle.markDirty();
//                }
//            } else {
//                freeLight();
//            }
//        }
//    }





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

//    @Override
//    public boolean damage(DamageSource source, float amount) {
//        return false;
//    }
//    @Override
//    public boolean isInvulnerableTo(DamageSource source) {
//        return true;
//    }
    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void playHurtSound(DamageSource damageSource) {


    }

    @Override
   public boolean isSilent() {
        return true;
   }



    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_ALLAY_HURT;
    }

    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_ALLAY_DEATH;
    }

    protected float getSoundVolume() {
        return 0.9F;
    }

    @Override
    protected void pushAway(Entity entity) {}

    @Override
    public EntityData initialize(
            ServerWorldAccess world,
            LocalDifficulty difficulty,
            SpawnReason spawnReason,
            @Nullable EntityData entityData

    ) {
        EntityData data = super.initialize(world, difficulty, spawnReason, entityData);
        this.setHealth(this.getMaxHealth());
        return data;
    }




    @Override public boolean isBreedingItem(ItemStack stack) { return false; }
    @Nullable @Override public PassiveEntity createChild(ServerWorld world, PassiveEntity mate) { return null; }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar reg) {
        reg.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }
    private PlayState predicate(software.bernie.geckolib.animation.AnimationState<SawBladeEntity> s) {
        s.getController().setAnimation(RawAnimation.begin().then("spin", Animation.LoopType.LOOP));


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