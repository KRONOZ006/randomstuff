package net.kronoz.randomstuff.entity.client.model;

import net.kronoz.randomstuff.Randomstuff;
import net.kronoz.randomstuff.entity.OmegaEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class OmegaModel extends GeoModel<OmegaEntity> {
    @Override
    public Identifier getModelResource(OmegaEntity omegaEntity) {
        return Identifier.of(Randomstuff.MOD_ID, "geo/entity/omega.geo.json");
    }

    @Override
    public Identifier getTextureResource(OmegaEntity omegaEntity) {
        return Identifier.of(Randomstuff.MOD_ID, "textures/entity/omega.png");
    }

    @Override
    public Identifier getAnimationResource(OmegaEntity omegaEntity) {
        return Identifier.of(Randomstuff.MOD_ID, "animations/entity/omega.animation.json");
    }



    }

