package net.kronoz.randomstuff.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.kronoz.randomstuff.Randomstuff;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {
    public static final Item AVO_GA_GLAIVE = registerItem("avo_ga_glaive",
            new AvoGaGlaiveItem(
                    ToolMaterials.NETHERITE, 9, 0.9F,
                    new Item.Settings().maxCount(1).maxDamage(1).fireproof().attributeModifiers(SwordItem.createAttributeModifiers(ToolMaterials.NETHERITE,3, -3.5f)))

    );
    public static final Item ERIS = registerItem("eris",
            new ErisItem(
                    ToolMaterials.NETHERITE, 1.69f,
                    new Item.Settings().maxCount(1).maxDamage(1).fireproof().attributeModifiers(SwordItem.createAttributeModifiers(ToolMaterials.NETHERITE,3, -1.5f)))

    );


    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(Randomstuff.MOD_ID, name), item);
    }

    public static void registerModItems() {
        Randomstuff.LOGGER.info("Registering Mod Items for " + Randomstuff.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(AVO_GA_GLAIVE);
            entries.add(ERIS);

        });
    }



}
