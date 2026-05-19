package com.lxy.hook.client;

import com.lxy.hook.HookMod;
import com.lxy.hook.client.render.HookRopeRenderer;
import com.lxy.hook.entity.ModEntityTypes;
import com.lxy.hook.network.HookActionPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class HookModClient implements ClientModInitializer {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath(HookMod.MOD_ID, "controls")
	);

	private static KeyMapping useHookKey;
	private static KeyMapping toggleModeKey;

	/**
	 * 用于检测跳跃键从“未按下”变成“按下”的瞬间。
	 * 避免玩家按住空格时每 tick 都发包。
	 */
	private static boolean wasJumpKeyDown = false;

	@Override
	public void onInitializeClient() {
		EntityRenderers.register(
				ModEntityTypes.HOOK_PROJECTILE,
				context -> new ThrownItemRenderer<>(context, 1.0F, true)
		);

		HookRopeRenderer.register();

		registerKeyMappings();
	}

	private static void registerKeyMappings() {
		useHookKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.hook.use",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_R,
				CATEGORY
		));

		toggleModeKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.hook.toggle_mode",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_G,
				CATEGORY
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null || client.level == null) {
				wasJumpKeyDown = false;
				return;
			}

			while (useHookKey.consumeClick()) {
				ClientPlayNetworking.send(new HookActionPayload(HookActionPayload.USE_OR_RELEASE));
			}

			while (toggleModeKey.consumeClick()) {
				ClientPlayNetworking.send(new HookActionPayload(HookActionPayload.TOGGLE_MODE));
			}

			boolean isJumpKeyDown = client.options.keyJump.isDown();

			if (isJumpKeyDown && !wasJumpKeyDown) {
				ClientPlayNetworking.send(new HookActionPayload(HookActionPayload.RELEASE_ANCHOR_ONLY));
			}

			wasJumpKeyDown = isJumpKeyDown;
		});
	}
}