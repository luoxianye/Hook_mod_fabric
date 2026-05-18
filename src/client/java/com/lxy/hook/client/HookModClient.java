package com.lxy.hook.client;

import com.lxy.hook.client.render.HookRopeRenderer;
import com.lxy.hook.entity.ModEntityTypes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public class HookModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntityTypes.HOOK_PROJECTILE,
				ctx -> new ThrownItemRenderer<>(ctx, 1.0F, true));
		HookRopeRenderer.register();
	}
}