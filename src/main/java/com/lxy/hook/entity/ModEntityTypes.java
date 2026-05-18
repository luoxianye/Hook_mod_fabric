package com.lxy.hook.entity;

import com.lxy.hook.HookMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntityTypes {
    public static final EntityType<HookProjectileEntity> HOOK_PROJECTILE = register(
            "hook_projectile",
            EntityType.Builder.<HookProjectileEntity>of(HookProjectileEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
    );

    private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(
                Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(HookMod.MOD_ID, name)
        );

        return Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                key,
                builder.build(key)
        );
    }

    public static void initialize() {
        HookMod.LOGGER.info("Registering entity types for {}", HookMod.MOD_ID);
    }
}