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
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
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
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
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
import java.util.Optional;
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

    private static final double RETURN_SPEED = 5.2;
    private static final double MIN_BOUNCE_SPEED = 2.4;
    private static final double MAX_BOUNCE_SPEED = 6.6;
    private static final double BLADE_RADIUS = 0.85;
    private static final double BLADE_THICKNESS = 0.35;
    private static final double RETURN_DISTANCE = 18.0;
    private static final int RETURN_AFTER_TICKS = 35;
    private static final int MAX_RICOCHETS = 4;
    private static final int MAX_LIFE_AFTER_THROW = 180;

    private boolean returning = false;
    private int ricochetCount = 0;
    private int soundCooldown = 0;

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


    private static final TrackedData<Optional<UUID>> OWNER =
            DataTracker.registerData(SawBladeEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(OWNER, Optional.empty());
    }

    public void setOwner(@Nullable LivingEntity owner) {
        this.ownerUuid = owner != null ? owner.getUuid() : null;
        this.dataTracker.set(OWNER, Optional.ofNullable(owner != null ? owner.getUuid() : null));
    }

    public void shootFromOwner(LivingEntity owner, float speed) {
        Vec3d look = owner.getRotationVec(1.0f).normalize();
        this.setOwner(owner);
        this.returning = false;
        this.ricochetCount = 0;
        this.spawnAge = this.age;
        this.maxLifeTicks = MAX_LIFE_AFTER_THROW;
        this.setVelocity(look.multiply(speed).add(owner.getVelocity().multiply(0.25)));
        this.alignToVelocity(this.getVelocity());
        this.setNoGravity(true);
        this.velocityModified = true;
        this.updateDynamicHitbox();
    }

    @Nullable
    public LivingEntity getOwnerClient() {
        Optional<UUID> optionalUuid = this.dataTracker.get(OWNER);
        if (optionalUuid.isEmpty()) return null;

        UUID uuid = optionalUuid.get();
        Entity entity = this.getWorld().getPlayerByUuid(uuid);
        if (entity instanceof LivingEntity living) return living;
        return null;
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
        lockFlightPhysics();

        if (soundCooldown > 0) {
            soundCooldown--;
        }

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
            tickFlight();
            if (this.isRemoved()) return;
            updateDynamicHitbox();

            Vec3d start = this.getPos();
            Vec3d velocity = this.getVelocity();
            Vec3d end = start.add(velocity);

            BlockHitResult blockHit = findBlockHit(start, velocity);

            if (blockHit != null && blockHit.getType() == HitResult.Type.BLOCK) {
                onBlockHit(blockHit);
                return;
            }

            EntityHitResult hit = ProjectileUtil.getEntityCollision(
                    this.getWorld(),
                    this,
                    start,
                    end,
                    getBladeCollisionBox(velocity).stretch(velocity).expand(0.1),
                    entity -> entity.isAlive() && entity != this && !isOwner(entity)
            );

            if (hit != null) {
                onEntityHit(hit);
            }

            if (this.getVelocity().length() > 2.5f && this.age % 2 == 0) {
                if (this.getWorld() instanceof ServerWorld sw) {


                    sw.spawnParticles(ModParticles.HEART_BURST_PARTICLE, getX(), getY() + 1.2, getZ(), 1, 0, 0, 0, 3);
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

    private void tickFlight() {
        lockFlightPhysics();

        if (spawnAge < 0) {
            spawnAge = this.age;
            maxLifeTicks = MAX_LIFE_AFTER_THROW;
        }

        LivingEntity owner = getOwnerLivingEntity();
        if (owner == null || !owner.isAlive()) {
            this.discard();
            return;
        }

        int flightTicks = this.age - spawnAge;
        double ownerDistance = this.distanceTo(owner);
        if (!returning && (flightTicks > RETURN_AFTER_TICKS || ownerDistance > RETURN_DISTANCE || ricochetCount >= MAX_RICOCHETS)) {
            startReturning();
        }

        if (returning) {
            Vec3d target = owner.getEyePos().add(owner.getRotationVec(1.0f).multiply(0.25));
            Vec3d toOwner = target.subtract(this.getPos());
            double distance = toOwner.length();

            if (distance < 1.45) {
                playFlightSound(SoundEvents.ITEM_TRIDENT_RETURN, 0.9f, 1.4f);
                this.discard();
                return;
            }

            Vec3d desired = toOwner.normalize().multiply(RETURN_SPEED);
            this.setVelocity(steerVelocity(this.getVelocity(), desired, 0.38, RETURN_SPEED * 0.8, RETURN_SPEED * 1.35));
            this.velocityModified = true;
        }

        if (flightTicks > maxLifeTicks) {
            this.discard();
            return;
        }

        alignToVelocity(this.getVelocity());
        updateDynamicHitbox();
    }

    private void lockFlightPhysics() {
        this.setNoGravity(true);
        this.fallDistance = 0.0F;
    }

    private void updateDynamicHitbox() {
        this.setBoundingBox(getBladeCollisionBox(this.getVelocity()));
    }

    private Box getBladeCollisionBox(Vec3d velocity) {
        Vec3d dir = velocity.lengthSquared() > 0.0001 ? velocity.normalize() : Vec3d.fromPolar(this.getPitch(), this.getYaw());
        double speed = velocity.length();
        double forwardReach = MathHelper.clamp(speed * 0.18, 0.25, 0.9);

        double halfX = BLADE_RADIUS + Math.abs(dir.x) * forwardReach;
        double halfY = BLADE_THICKNESS + Math.abs(dir.y) * forwardReach;
        double halfZ = BLADE_RADIUS + Math.abs(dir.z) * forwardReach;

        Vec3d center = this.getPos().add(0.0, 0.9, 0.0).add(dir.multiply(forwardReach * 0.35));
        return new Box(
                center.x - halfX,
                center.y - halfY,
                center.z - halfZ,
                center.x + halfX,
                center.y + halfY,
                center.z + halfZ
        );
    }

    private void startReturning() {
        if (returning) return;
        returning = true;
        playFlightSound(SoundEvents.ITEM_TRIDENT_RETURN, 1.0f, 0.85f + random.nextFloat() * 0.25f);
    }

    private void onBlockHit(BlockHitResult hit) {
        Direction side = hit.getSide();
        Vec3d normal = new Vec3d(side.getOffsetX(), side.getOffsetY(), side.getOffsetZ());
        Vec3d velocity = this.getVelocity();
        Vec3d bounced = reflect(velocity, normal);
        Vec3d ownerPull = getReturnDirection();

        int nextRicochet = ricochetCount + 1;
        double speed = MathHelper.clamp(velocity.length() * 1.04 + 0.35, MIN_BOUNCE_SPEED, MAX_BOUNCE_SPEED);
        double returnBias = MathHelper.clamp(0.28 + nextRicochet * 0.12, 0.36, 0.68);
        Vec3d newVelocity = bounced.normalize().multiply(1.0 - returnBias).add(ownerPull.multiply(returnBias));

        ricochetCount = nextRicochet;
        returning = true;
        this.setPosition(hit.getPos().add(normal.multiply(BLADE_RADIUS * 0.45)));
        this.setVelocity(newVelocity.normalize().multiply(speed));
        this.velocityModified = true;
        updateDynamicHitbox();
        alignToVelocity(this.getVelocity());

        playFlightSound(SoundEvents.BLOCK_ANVIL_PLACE, 0.9f, 1.75f + random.nextFloat() * 0.3f);
        spawnRicochetParticles(hit.getPos(), normal);

        playFlightSound(SoundEvents.ITEM_TRIDENT_RETURN, 0.75f, 1.15f + random.nextFloat() * 0.2f);
    }

    @Nullable
    private BlockHitResult findBlockHit(Vec3d start, Vec3d velocity) {
        if (velocity.lengthSquared() < 0.0001) return null;

        Vec3d dir = velocity.normalize();
        Vec3d side = dir.crossProduct(new Vec3d(0.0, 1.0, 0.0));
        if (side.lengthSquared() < 0.0001) {
            side = new Vec3d(1.0, 0.0, 0.0);
        } else {
            side = side.normalize();
        }

        Vec3d vertical = side.crossProduct(dir).normalize();
        Vec3d center = start.add(0.0, 0.9, 0.0);
        Vec3d[] offsets = new Vec3d[]{
                Vec3d.ZERO,
                side.multiply(BLADE_RADIUS * 0.75),
                side.multiply(-BLADE_RADIUS * 0.75),
                vertical.multiply(BLADE_RADIUS * 0.45),
                vertical.multiply(-BLADE_RADIUS * 0.45)
        };

        BlockHitResult closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (Vec3d offset : offsets) {
            Vec3d rayStart = center.add(offset);
            Vec3d rayEnd = rayStart.add(velocity);
            BlockHitResult hit = this.getWorld().raycast(new RaycastContext(
                    rayStart,
                    rayEnd,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    this
            ));

            if (hit.getType() != HitResult.Type.BLOCK) continue;

            double distance = hit.getPos().squaredDistanceTo(rayStart);
            if (distance < closestDistance) {
                closest = hit;
                closestDistance = distance;
            }
        }

        return closest;
    }

    private Vec3d reflect(Vec3d velocity, Vec3d normal) {
        return velocity.subtract(normal.multiply(2.0 * velocity.dotProduct(normal)));
    }

    private Vec3d steerVelocity(Vec3d current, Vec3d desired, double strength, double minSpeed, double maxSpeed) {
        Vec3d steered = current.multiply(1.0 - strength).add(desired.multiply(strength));
        double speed = MathHelper.clamp(steered.length(), minSpeed, maxSpeed);
        if (steered.lengthSquared() < 0.0001) {
            return desired.normalize().multiply(speed);
        }
        return steered.normalize().multiply(speed);
    }

    private void alignToVelocity(Vec3d velocity) {
        if (velocity.lengthSquared() < 0.0001) return;

        Vec3d dir = velocity.normalize();
        float yaw = (float)(MathHelper.atan2(dir.z, dir.x) * MathHelper.DEGREES_PER_RADIAN) - 90.0F;
        float pitch = (float)(-(MathHelper.atan2(dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z)) * MathHelper.DEGREES_PER_RADIAN));

        this.setYaw(yaw);
        this.setPitch(pitch);
        this.setHeadYaw(yaw);
        this.bodyYaw = yaw;
        this.prevYaw = yaw;
        this.prevPitch = pitch;
    }

    private void playFlightSound(SoundEvent sound, float volume, float pitch) {
        if (soundCooldown > 0 || !(this.getWorld() instanceof ServerWorld sw)) return;
        soundCooldown = 4;
        sw.playSound(null, this.getBlockPos(), sound, SoundCategory.PLAYERS, volume, pitch);
    }

    private void spawnRicochetParticles(Vec3d pos) {
        spawnRicochetParticles(pos, Vec3d.ZERO);
    }

    private void spawnRicochetParticles(Vec3d pos, Vec3d normal) {
        if (!(this.getWorld() instanceof ServerWorld sw)) return;
        sw.spawnParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 14, 0.12 + Math.abs(normal.x) * 0.18, 0.12 + Math.abs(normal.y) * 0.18, 0.12 + Math.abs(normal.z) * 0.18, 0.11);
        sw.spawnParticles(ModParticles.GOLDEN_BURST_PARTICLE, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
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

        Vec3d away = this.getVelocity().lengthSquared() > 0.0001
                ? this.getVelocity().normalize()
                : target.getPos().subtract(this.getPos()).normalize();
        target.addVelocity(away.x * 0.35, 0.18, away.z * 0.35);

        ricochetCount++;
        startReturning();
        this.setVelocity(this.getVelocity().multiply(-0.35).add(getReturnDirection().multiply(RETURN_SPEED * 0.65)));
        this.velocityModified = true;
        alignToVelocity(this.getVelocity());

        playFlightSound(SoundEvents.ITEM_TRIDENT_HIT, 1.0f, 1.25f + random.nextFloat() * 0.3f);
        spawnRicochetParticles(hit.getPos());
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 500.0)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.6F)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2F).add(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, 1000);
    }

    @Override
    protected void applyGravity() {
        this.setNoGravity(true);
    }

//    public void setOwner(@Nullable LivingEntity owner) {
//        this.ownerUuid = owner != null ? owner.getUuid() : null;
//    }


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

    @Nullable
    private LivingEntity getOwnerLivingEntity() {
        Entity owner = getOwnerEntity();
        return owner instanceof LivingEntity living ? living : null;
    }

    private Vec3d getReturnDirection() {
        LivingEntity owner = getOwnerLivingEntity();
        if (owner == null) return this.getVelocity().multiply(-1).normalize();
        Vec3d toOwner = owner.getEyePos().subtract(this.getPos());
        return toOwner.lengthSquared() > 0.0001 ? toOwner.normalize() : Vec3d.ZERO;
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
        nbt.putBoolean("Returning", returning);
        nbt.putInt("Ricochets", ricochetCount);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        ownerUuid = nbt.containsUuid("Owner") ? nbt.getUuid("Owner") : null;
        spawnAge = nbt.getInt("SpawnAge");
        maxLifeTicks = nbt.getInt("MaxLife");
        hitboxSize = nbt.contains("HitboxSize") ? nbt.getDouble("HitboxSize") : hitboxSize;
        returning = nbt.getBoolean("Returning");
        ricochetCount = nbt.getInt("Ricochets");
    }
}
