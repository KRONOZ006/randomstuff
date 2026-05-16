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
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.renderer.specialty.DynamicGeoEntityRenderer;

public class SawBladeRenderer extends DynamicGeoEntityRenderer<SawBladeEntity> {

    // Textures
    private static final Identifier MODEL_TEXTURE =
            Identifier.of(Randomstuff.MOD_ID, "textures/entity/sawblade.png");
    private static final Identifier MODEL_EMISSIVE_TEXTURE =
            Identifier.of(Randomstuff.MOD_ID, "textures/entity/sawblade.png");
    private static final Identifier BEAM_TEXTURE =
            Identifier.of(Randomstuff.MOD_ID, "textures/entity/energy_chain.png");

    // Beam offsets
    private static final Vec3d BLADE_OFFSET = new Vec3d(0.0, 1, 0.0);
    private static final Vec3d OWNER_OFFSET = new Vec3d(0.0, 1.3, 0.0);


    public SawBladeRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new SawBladeModel());
    }

    // ----------------------
    // RENDER THE ENTITY
    // ----------------------
    @Override
    public void render(SawBladeEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider buffers, int light) {

        float flightPitch = MathHelper.lerp(tickDelta, entity.prevPitch, entity.getPitch());
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(flightPitch));

        // Render the normal GeckoLib model
        super.render(entity, yaw, tickDelta, matrices, buffers, light);
        matrices.pop();

        LivingEntity owner = entity.getOwnerClient();
        if (owner == null) return;

        matrices.push();


        // Interpolated positions
        Vec3d bladePos = lerp(entity, 0.0F, tickDelta).add(BLADE_OFFSET);

        Vec3d forward = owner.getRotationVec(tickDelta).normalize();
        Vec3d right = forward.crossProduct(new Vec3d(0,1,0)).normalize();
        Vec3d extraOffset = forward.multiply(0.6).add(right.multiply(0.45));

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

        // Animation factor (spins the quads around Z)
        float ticks = entity.age + tickDelta;
        float rotation = ticks * 0.1F; // adjust speed

        VertexConsumer vc = buffers.getBuffer(RenderLayer.getEyes(BEAM_TEXTURE));
        MatrixStack.Entry entry = matrices.peek();

        int segments = 4; // number of crossed quads (like Guardian)
        float radius = 0.2F; // half-width of beam
        float textureScale = 1F; // how many units per texture repeat

        for (int i = 0; i < segments; i++) {
            double angle = rotation + i * Math.PI / segments * 2; // evenly spaced
            float xOffset = (float) Math.cos(angle) * radius;
            float yOffset = (float) Math.sin(angle) * radius;

            // Quad vertices along Z axis
            vertex(vc, entry, -xOffset, 0, -yOffset, 255,255,255,255, 0, 0, 15728880);
            vertex(vc, entry, -xOffset, length, -yOffset, 255,255,255,255, 0, length / textureScale, 15728880);
            vertex(vc, entry, xOffset, length, yOffset, 255,255,255,255, 1, length / textureScale, 15728880);
            vertex(vc, entry, xOffset, 0, yOffset, 255,255,255,255, 1, 0, 15728880);
        }

        matrices.pop();
    }

    // Helper: draw vertex
    private static void vertex(VertexConsumer vc, MatrixStack.Entry m,
                               float x, float y, float z,
                               int r, int g, int b, int a,
                               float u, float v, int packedLight) {
        vc.vertex(m, x, y, z)
                .color(r, g, b, a)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(packedLight)
                .normal(m, 0, 1, 0);
    }

    // Interpolated positions
    private Vec3d lerp(LivingEntity e, double yOffset, float delta) {
        return new Vec3d(
                MathHelper.lerp(delta, e.lastRenderX, e.getX()),
                MathHelper.lerp(delta, e.lastRenderY, e.getY()) + yOffset,
                MathHelper.lerp(delta, e.lastRenderZ, e.getZ())
        );
    }

    // ----------------------
    // EMISSIVE MODEL BONE OVERRIDE
    // ----------------------
    @Override
    protected boolean boneRenderOverride(MatrixStack poseStack,
                                         GeoBone bone,
                                         VertexConsumerProvider bufferSource,
                                         VertexConsumer buffer,
                                         float partialTick,
                                         int packedLight,
                                         int packedOverlay,
                                         int colour) {

        // Example: make "emissiveBone" glow
        boolean isEmissive = bone.getName().equals("bone2");

        VertexConsumer vertexConsumer;
        if (isEmissive) {
            vertexConsumer = bufferSource.getBuffer(RenderLayer.getEyes(MODEL_EMISSIVE_TEXTURE));
            packedLight = 15728880; // max brightness
        } else {
            vertexConsumer = bufferSource.getBuffer(RenderLayer.getEntityCutout(MODEL_TEXTURE));
        }

        if (!bone.isHidden()) {
            poseStack.push();
            for (GeoCube cube : bone.getCubes()) {
                renderCube(poseStack, cube, vertexConsumer, packedLight, OverlayTexture.DEFAULT_UV, colour);
            }
            poseStack.pop();
        }

        return true; // we've handled the bone
    }

    // ----------------------


    @Override
    public Identifier getTextureLocation(SawBladeEntity e) {
        return MODEL_TEXTURE;
    }
}
