package com.lxy.hook.network;

import com.lxy.hook.util.HookUseHandler;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class ModNetwork {
    private ModNetwork() {
    }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(UseHookPayload.TYPE, UseHookPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(UseHookPayload.TYPE, (payload, context) -> {
            if (!payload.pressed()) {
                return;
            }

            context.server().execute(() -> HookUseHandler.useEquippedHook(context.player()));
        });
    }
}