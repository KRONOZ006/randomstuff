package net.kronoz.randomstuff.item;

import net.kronoz.randomstuff.entity.ModEntities;
import net.kronoz.randomstuff.entity.OmegaEntity;
import net.kronoz.randomstuff.sound.ModSounds;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.UUID;

public class AvoGaGlaiveItem extends SwordItem {
    private static final int MAX_CHARGE_TICKS = 40;

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
        return UseAction.BOW;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }

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
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity entity, int remainingUseTicks) {
        if (!(entity instanceof PlayerEntity user)) return;
        if (world.isClient) return;

        int charge = MAX_CHARGE_TICKS - remainingUseTicks;
        float p = Math.min(1.0f, charge / 20.0f);
        float speed = 0.9f + 1.6f * p;
        int cooldown = (int)(20 + p * 40);

        OmegaEntity omega = new OmegaEntity(ModEntities.OMEGA, world);
        Vec3d eye = user.getEyePos();
        Vec3d look = user.getRotationVec(1.0f);
        omega.refreshPositionAndAngles(
                eye.x + look.x * 0.6,
                eye.y - 0.15 + look.y * 0.6,
                eye.z + look.z * 0.6,
                user.getYaw(), user.getPitch()
        );
        omega.setVelocity(look.multiply(speed));
        omega.velocityModified = true;
        omega.setNoDrag(true);
        omega.setOwner(user);

        world.spawnEntity(omega);

        world.playSound(
                null,
                user.getX(), user.getY(), user.getZ(),
                SoundEvents.ITEM_TRIDENT_THROW,
                SoundCategory.PLAYERS,
                0.6f, 1.0f
        );

        user.getItemCooldownManager().set(this, cooldown);

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
