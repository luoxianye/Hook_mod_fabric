package com.lxy.hook.client;

import com.lxy.hook.client.render.HookRopeRenderer;
import com.lxy.hook.entity.ModEntityTypes;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public class HookModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRenderers.register(
				ModEntityTypes.HOOK_PROJECTILE,
				context -> new ThrownItemRenderer<>(context, 1.0F, true)
		);

		HookRopeRenderer.register();
		HookKeyBindings.initialize();
	}
}