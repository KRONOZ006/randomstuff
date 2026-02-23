package net.kronoz.randomstuff.entity.client.renderer;

import net.kronoz.randomstuff.Randomstuff;
import net.kronoz.randomstuff.entity.OmegaEntity;
import net.kronoz.randomstuff.entity.SawBladeEntity;
import net.kronoz.randomstuff.entity.client.model.OmegaModel;
import net.kronoz.randomstuff.entity.client.model.SawBladeModel;
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

public class SawBladeRenderer extends DynamicGeoEntityRenderer<SawBladeEntity> {



    public SawBladeRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new SawBladeModel());


    }


    @Override
    protected boolean boneRenderOverride(MatrixStack poseStack,
                                         GeoBone bone,
                                         VertexConsumerProvider bufferSource,
                                         VertexConsumer buffer,
                                         float partialTick,
                                         int packedLight,
                                         int packedOverlay,
                                         int colour) {

        if (bone.getName().equals("full")) {
            SawBladeEntity entity = this.getAnimatable();
            if (entity.age < 2) {


                bone.setHidden(true);
            }
            else {
                bone.setHidden(false);
            }
        }


        boolean isEmissive = bone.getName().equals("bone2");






        VertexConsumer vertexConsumer;
        if (isEmissive) {
            vertexConsumer = bufferSource.getBuffer(
                    RenderLayer.getEyes(Identifier.of(Randomstuff.MOD_ID, "textures/entity/sawblade.png"))
            );
            packedLight = 15728880;
        } else {
            vertexConsumer = bufferSource.getBuffer(RenderLayer.getEntityCutout(getTextureLocation(this.getAnimatable())));
        }

        if (!bone.isHidden()) {
            poseStack.push();
            for (GeoCube cube : bone.getCubes()) {
                renderCube(
                        poseStack,
                        cube,
                        vertexConsumer,
                        packedLight,
                        OverlayTexture.DEFAULT_UV,
                        colour
                );
            }
            poseStack.pop();
        }


        return true;
    }

    @Override
    public Identifier getTextureLocation(SawBladeEntity e) {
        return ((SawBladeModel)this.getGeoModel()).getTextureResource(e);
    }
}