package net.kronoz.randomstuff.entity.client.renderer;



import net.kronoz.randomstuff.Randomstuff;
import net.kronoz.randomstuff.item.ErisItem;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.specialty.DynamicGeoItemRenderer;
import software.bernie.geckolib.util.Color;

public class ErisItemRenderer extends DynamicGeoItemRenderer<ErisItem> {

    private static final Identifier MODEL_ID = Identifier.of(Randomstuff.MOD_ID, "eris");


    public ErisItemRenderer() {
        super(new DefaultedItemGeoModel<>(MODEL_ID));
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
        ItemStack stack = getCurrentItemStack();
        if (stack.isEmpty()) return false;






        return false;
    }
}

