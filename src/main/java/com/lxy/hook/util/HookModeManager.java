package com.lxy.hook.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class HookModeManager {
    private static final Map<UUID, HookMode> MODES = new HashMap<>();

    private HookModeManager() {
    }

    public static HookMode getMode(ServerPlayer player) {
        return MODES.getOrDefault(player.getUUID(), HookMode.NORMAL);
    }

    public static void toggleMode(ServerPlayer player) {
        HookMode next = getMode(player).next();
        MODES.put(player.getUUID(), next);
        HookMessage.actionBar(player, next.message());
    }

    public static void remove(ServerPlayer player) {
        MODES.remove(player.getUUID());
    }
}