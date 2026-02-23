package net.kronoz.randomstuff.item;

import net.kronoz.randomstuff.entity.ModEntities;
import net.kronoz.randomstuff.entity.OmegaEntity;
import net.kronoz.randomstuff.sound.ModSounds;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageSources;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.UUID;

public class AvoGaGlaiveItem extends SwordItem {
    private static final int MAX_CHARGE_TICKS = 400;

    public AvoGaGlaiveItem(ToolMaterial material, int attackDamage, float attackSpeed, Settings settings) {
        super(material, withAttributes(settings, attackDamage, attackSpeed));
    }

    private static Settings withAttributes(Settings base, int attackDamage, float attackSpeed) {
        AttributeModifiersComponent.Builder b = AttributeModifiersComponent.builder();

        b.add(
                net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE,
                new net.minecraft.entity.attribute.EntityAttributeModifier(
                        Identifier.of("randomstuff", "avogaglaive_damage"),
                        (double) attackDamage,
                        net.minecraft.entity.attribute.EntityAttributeModifier.Operation.ADD_VALUE
                ),
                AttributeModifierSlot.MAINHAND
        );

        b.add(
                net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_SPEED,
                new net.minecraft.entity.attribute.EntityAttributeModifier(
                        Identifier.of("randomstuff", "avogaglaive_speed"),
                        (double) attackSpeed,
                        net.minecraft.entity.attribute.EntityAttributeModifier.Operation.ADD_VALUE
                ),
                AttributeModifierSlot.MAINHAND
        );

        return base.component(DataComponentTypes.ATTRIBUTE_MODIFIERS, b.build());
    }



    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return MAX_CHARGE_TICKS;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.CROSSBOW;
    }



    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }

        PigEntity pig = new PigEntity(EntityType.PIG, world);


        Vec3d forward = user.getRotationVec(1.0F).normalize();
        Vec3d right = forward.crossProduct(new Vec3d(0, 1, 0)).normalize();
        Vec3d left = right.multiply(-1);
        Vec3d up = new Vec3d(0, 1, 0);
        Vec3d look = user.getRotationVec(1.0f).normalize();
        Vec3d offset = forward.multiply(0).add(left.multiply(3)).add(up.multiply(0.5));

        Vec3d spawnPos = user.getPos().add(offset);
        double dx = look.x;
        double dy = look.y;
        double dz = look.z;


        pig.refreshPositionAndAngles(
                spawnPos.x ,
                spawnPos.y,
                spawnPos.z,
                user.getYaw(), user.getPitch()
        );


        HitResult hit = user.raycast(20.0D, 0.0F, false);

        Vec3d targetVec;
        if (hit.getType() == HitResult.Type.BLOCK) {

            targetVec = Vec3d.ofCenter(((BlockHitResult) hit).getBlockPos());
        } else {

            targetVec = hit.getPos();
        }

        Vec3d dir = targetVec.subtract(pig.getPos()).normalize();


        float yaw = (float)(MathHelper.atan2(dir.z, dir.x) * (180F / Math.PI)) - 90F;
        float pitch = (float)(-(MathHelper.atan2(dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z)) * (180F / Math.PI)));


        pig.setYaw(yaw);
        pig.setPitch(pitch);
        pig.setHeadYaw(yaw);
        pig.headYaw = yaw;
        pig.prevHeadYaw = yaw;



        world.spawnEntity(pig);





        world.spawnEntity(pig);



        user.setCurrentHand(hand);

        world.playSound(
                null,
                user.getX(), user.getY(), user.getZ(),
                ModSounds.CRACK,
                SoundCategory.PLAYERS,
                0.6f, 0.1f
        );



        user.incrementStat(Stats.USED.getOrCreateStat(this));
        return TypedActionResult.consume(stack);




    }
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!(entity instanceof PlayerEntity player)) return;
        if (!player.isUsingItem() || player.getActiveItem() != stack) return;

//        if (!world.isClient) {
//            RegistryEntry<DamageType> crammingType = world.getRegistryManager()
//                    .get(RegistryKeys.DAMAGE_TYPE)
//                    .entryOf(DamageTypes.OUT_OF_WORLD);
//
//            DamageSource source = new DamageSource(crammingType);
//            if (!player.isCreative()) {
//                player.damage(source, 0.5f);
//            }
//        }
    }





    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity entity, int remainingUseTicks) {
        if (!(entity instanceof PlayerEntity user)) return;
        if (world.isClient) return;

        int charge = MAX_CHARGE_TICKS - remainingUseTicks;
        float p = Math.min(1.0f, charge / 20.0f);
        float speed = 0.9f + 1.6f * p;
        int cooldown = (int)(20 + p * 40);

        OmegaEntity omega = new OmegaEntity(ModEntities.SAWBLADE, world);
        Vec3d eye = user.getEyePos();
        Vec3d look = user.getRotationVec(1.0f);
        omega.refreshPositionAndAngles(
                eye.x + look.x * 0.6,
                eye.y + 0.1 + look.y * 0.6,
                eye.z + look.z * 0.6,
                user.getYaw(), user.getPitch()
        );
        omega.setVelocity(look.multiply(speed));
        omega.velocityModified = true;
        omega.setNoDrag(true);
        omega.setOwner(user);
        omega.setNoGravity(true);




        world.spawnEntity(omega);



        world.playSound(
                null,
                user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENTITY_WITHER_SHOOT,
                SoundCategory.PLAYERS,
                0.6f, 1.0f
        );

        if (!user.isCreative()) {
            user.getItemCooldownManager().set(this, cooldown);
        }

        EquipmentSlot slot = user.getActiveHand() == Hand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
        stack.damage(1, user, slot);
    }




    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 30, 0));
            target.takeKnockback(0.3, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
        }
        EquipmentSlot slot = (attacker.getMainHandStack() == stack) ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        stack.damage(1, attacker, slot);
        return true;
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        user.playSound(SoundEvents.ITEM_SPYGLASS_STOP_USING, 1.0F, 1.0F);
        return stack;
    }
}
