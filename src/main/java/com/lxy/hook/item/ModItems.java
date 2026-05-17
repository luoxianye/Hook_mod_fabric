package com.lxy.hook.item;

import com.lxy.hook.HookMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

import java.util.function.Function;

public class ModItems {
    public static final Item HOOK = register(
            "hook",
            HookItem::new,
            new Item.Properties()
                    .stacksTo(1)
                    .durability(384)
    );

    private static <T extends Item> T register(
            String name,
            Function<Item.Properties, T> factory,
            Item.Properties properties
    ) {
        Identifier id = Identifier.fromNamespaceAndPath(HookMod.MOD_ID, name);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);

        T item = factory.apply(properties.setId(key));
        Registry.register(BuiltInRegistries.ITEM, key, item);

        return item;
    }

    public static void initialize() {
        HookMod.LOGGER.info("Registering mod items for {}", HookMod.MOD_ID);
    }
}
