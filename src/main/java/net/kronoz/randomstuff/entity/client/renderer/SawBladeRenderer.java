package net.kronoz.randomstuff.entity.client.renderer;

import net.kronoz.randomstuff.Randomstuff;
import net.kronoz.randomstuff.entity.SawBladeEntity;
import net.kronoz.randomstuff.entity.client.model.SawBladeModel;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import software.bernie.geckolib.renderer.specialty.DynamicGeoEntityRenderer;

public class SawBladeRenderer extends DynamicGeoEntityRenderer<SawBladeEntity> {

    private static final Identifier BEAM_TEXTURE =
            Identifier.of(Randomstuff.MOD_ID, "textures/entity/energy_chain.png");

    private static final Vec3d BLADE_OFFSET = new Vec3d(0.0, 0.2, 0.0);
  // lower on the saw blade
    private static final Vec3d OWNER_OFFSET = new Vec3d(0.0, 1.3, 0.0);   // forward and slightly up on the player

    private static final RenderLayer BEAM_LAYER =
            RenderLayer.getEntityTranslucent(BEAM_TEXTURE);

    public SawBladeRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new SawBladeModel());
    }

    @Override
    public void render(
            SawBladeEntity entity,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider buffers,
            int light
    ) {
        // Render the GeckoLib model
        super.render(entity, yaw, tickDelta, matrices, buffers, light);

        // Render beam
        LivingEntity owner = entity.getOwnerClient();
        if (owner != null) {
            matrices.push();

            // Interpolated positions
            Vec3d bladePos = lerp(entity, 0.0F, tickDelta).add(BLADE_OFFSET);

            Vec3d forward = owner.getRotationVec(tickDelta).normalize();
            Vec3d right = forward.crossProduct(new Vec3d(0,1,0)).normalize();
            Vec3d extraOffset = forward.multiply(0.3).add(right.multiply(0.45));

            Vec3d ownerPos = lerp(owner, 0.0F, tickDelta).add(OWNER_OFFSET).add(extraOffset);

            Vec3d delta = ownerPos.subtract(bladePos);
            float length = (float) delta.length();
            delta = delta.normalize();

            // Rotation
            float pitch = (float) Math.acos(delta.y);
            float yawRad = (float) Math.atan2(delta.z, delta.x);

            matrices.translate(bladePos.x - entity.getX(), bladePos.y - entity.getY(), bladePos.z - entity.getZ());
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(((float)Math.PI / 2 - yawRad) * MathHelper.DEGREES_PER_RADIAN));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch * MathHelper.DEGREES_PER_RADIAN));

            VertexConsumer vc = buffers.getBuffer(RenderLayer.getEntityCutoutNoCull(BEAM_TEXTURE));
            MatrixStack.Entry entry = matrices.peek();

            float r = 0.2F;
            float vStart = 0.0F;
            float vEnd = length; // one unit per block; adjust for tiling speed

            vertex(vc, entry, -r, 0, 0, 255,255,255,255, 0.0F, vStart);
            vertex(vc, entry, -r, length,0, 255,255,255,255, 0.0F, vEnd);
            vertex(vc, entry, r, length,0, 255,255,255,255, 1.0F, vEnd);
            vertex(vc, entry, r, 0, 0, 255,255,255,255, 1.0F, vStart);

            matrices.pop();
        }
    }

    private static void vertex(
            VertexConsumer vc,
            MatrixStack.Entry m,
            float x, float y, float z,
            int r, int g, int b, int a,
            float u, float v
    ) {
        vc.vertex(m, x, y, z)
                .color(r, g, b, a)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(15728880)
                .normal(m, 0, 1, 0);
    }

    private Vec3d lerp(LivingEntity e, double yOffset, float delta) {
        return new Vec3d(
                MathHelper.lerp(delta, e.lastRenderX, e.getX()),
                MathHelper.lerp(delta, e.lastRenderY, e.getY()) + yOffset,
                MathHelper.lerp(delta, e.lastRenderZ, e.getZ())
        );
    }

    @Override
    public Identifier getTextureLocation(SawBladeEntity e) {
        return ((SawBladeModel) getGeoModel()).getTextureResource(e);
    }
}