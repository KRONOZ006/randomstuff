package net.kronoz.randomstuff.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.kronoz.randomstuff.Randomstuff;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {

    public static final EntityType<OmegaEntity> OMEGA = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(Randomstuff.MOD_ID, "omega"),EntityType.Builder.create(OmegaEntity::new, SpawnGroup.MISC)
                    .dimensions(5f, 7f).build());

    public static final EntityType<SawBladeEntity> SAWBLADE = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(Randomstuff.MOD_ID, "sawblade"),EntityType.Builder.create(SawBladeEntity::new, SpawnGroup.MISC)
                    .dimensions(2.5f, 3.5f).build());
    public static void registerModEntities(){
        Randomstuff.LOGGER.info("registering mod entities for " + Randomstuff.MOD_ID);



     FabricDefaultAttributeRegistry.register(OMEGA, OmegaEntity.createAttributes());
     FabricDefaultAttributeRegistry.register(SAWBLADE, SawBladeEntity.createAttributes());

    }
}
