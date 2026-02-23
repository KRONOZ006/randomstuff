package net.kronoz.randomstuff.entity.client.model;

import net.kronoz.randomstuff.Randomstuff;
import net.kronoz.randomstuff.entity.OmegaEntity;
import net.kronoz.randomstuff.entity.SawBladeEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class SawBladeModel extends GeoModel<SawBladeEntity> {
    @Override
    public Identifier getModelResource(SawBladeEntity sawBladeEntity) {
        return Identifier.of(Randomstuff.MOD_ID, "geo/entity/sawblade.geo.json");
    }

    @Override public Identifier getTextureResource(SawBladeEntity e){ return Identifier.of(Randomstuff.MOD_ID,"textures/entity/sawblade.png"); }



    @Override
    public Identifier getAnimationResource(SawBladeEntity sawBladeEntity) {
        return Identifier.of(Randomstuff.MOD_ID, "animations/entity/sawblade.animation.json");
    }



    }

