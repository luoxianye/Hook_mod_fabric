package com.lxy.hook.client;

import com.lxy.hook.HookMod;
import com.lxy.hook.network.UseHookPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class HookKeyBindings {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(HookMod.MOD_ID, "controls")
    );

    private static KeyMapping useHookKey;

    private HookKeyBindings() {
    }

    public static void initialize() {
        useHookKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.hook.use_hook",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (useHookKey.consumeClick()) {
                if (client.player != null && client.level != null) {
                    ClientPlayNetworking.send(new UseHookPayload(true));
                }
            }
        });
    }
}