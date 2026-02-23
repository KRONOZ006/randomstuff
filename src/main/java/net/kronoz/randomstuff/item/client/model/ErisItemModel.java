package net.kronoz.randomstuff.item.client.model;

import net.kronoz.randomstuff.Randomstuff;
import net.kronoz.randomstuff.item.ErisItem;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class ErisItemModel extends GeoModel<ErisItem> {
    @Override
    public Identifier getModelResource(ErisItem item) {
        return Identifier.of(Randomstuff.MOD_ID, "geo/item/eris.geo.json");
    }

    @Override
    public Identifier getTextureResource(ErisItem item) {
        return Identifier.of(Randomstuff.MOD_ID, "textures/item/eris.png");
    }

    @Override
    public Identifier getAnimationResource(ErisItem item) {
        return Identifier.of(Randomstuff.MOD_ID, "animation/item/eris.animation.json");
    }
}
