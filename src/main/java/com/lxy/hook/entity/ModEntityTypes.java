package com.lxy.hook.entity;

import com.lxy.hook.HookMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntityTypes {
    private static final Identifier ID = Identifier.fromNamespaceAndPath(HookMod.MOD_ID, "hook_projectile");
    private static final ResourceKey<EntityType<?>> KEY = ResourceKey.create(Registries.ENTITY_TYPE, ID);

    public static final EntityType<HookProjectileEntity> HOOK_PROJECTILE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ID,
            EntityType.Builder.<HookProjectileEntity>of(HookProjectileEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(KEY)
    );

    public static void initialize() {
        HookMod.LOGGER.info("Registering entity types for {}", HookMod.MOD_ID);
    }
}
