package net.kronoz.randomstuff.entity.client.renderer;

import net.kronoz.randomstuff.Randomstuff;
import net.kronoz.randomstuff.entity.OmegaEntity;
import net.kronoz.randomstuff.entity.client.model.OmegaModel;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.renderer.specialty.DynamicGeoEntityRenderer;
import software.bernie.geckolib.util.Color;

public class OmegaRenderer extends DynamicGeoEntityRenderer<OmegaEntity> {

    private static final Identifier TEXTURE = Identifier.of(Randomstuff.MOD_ID, "textures/entity/omega.png");
    private static final RenderLayer GLOW_LAYER = RenderLayer.getEyes(TEXTURE);

    private static final int FULL_LIGHT = 15728640;
    private static final int WHITE_COLOR = Color.WHITE.getColor();

    public OmegaRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new OmegaModel());


    }



    @Override
    protected boolean boneRenderOverride(MatrixStack poseStack, GeoBone bone,
                                         VertexConsumerProvider bufferSource, VertexConsumer buffer,
                                         float partialTick, int packedLight, int packedOverlay, int colour) {
        if (bone.isHidden()) return false;

        VertexConsumer vc = bufferSource.getBuffer(GLOW_LAYER);
        for (GeoCube cube : bone.getCubes()) {
            poseStack.push();
            renderCube(poseStack, cube, vc, FULL_LIGHT, OverlayTexture.DEFAULT_UV, WHITE_COLOR);
            poseStack.pop();
        }
        return true;
    }

    @Override
    public Identifier getTextureLocation(OmegaEntity animatable) {
        return TEXTURE;
    }

    @Override
    public void render(OmegaEntity entity, float entityYaw, float partialTick,
                       MatrixStack poseStack, VertexConsumerProvider bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}