package com.lxy.hook.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;

public final class HookMessage {
    private HookMessage() {
    }

    public static void actionBar(ServerPlayer player, Component message) {
        player.connection.send(
                new ClientboundSetActionBarTextPacket(message)
        );
    }

    public static void actionBar(ServerPlayer player, String message) {
        actionBar(player, Component.literal(message));
    }

    public static void chat(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message));
    }
}