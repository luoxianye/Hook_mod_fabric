package com.lxy.hook.network;

import com.lxy.hook.util.HookModeManager;
import com.lxy.hook.util.HookUseHandler;
import com.lxy.hook.util.PlayerPullManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class HookNetworking {
    private HookNetworking() {
    }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(HookActionPayload.TYPE, HookActionPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(HookActionPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                if (payload.action() == HookActionPayload.USE_OR_RELEASE) {
                    HookUseHandler.releaseOrUseEquippedHook(context.player());
                } else if (payload.action() == HookActionPayload.TOGGLE_MODE) {
                    HookModeManager.toggleMode(context.player());
                } else if (payload.action() == HookActionPayload.RELEASE_ANCHOR_ONLY) {
                    PlayerPullManager.releaseModeBOnly(context.player(), true);
                }
            });
        });
    }
}