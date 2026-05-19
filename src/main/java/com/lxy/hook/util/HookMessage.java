package com.lxy.hook.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerPlayer;

public final class HookMessage {
    private HookMessage() {
    }

    public static void actionBar(ServerPlayer player, String message) {
        player.connection.send(
                new ClientboundSystemChatPacket(Component.literal(message), true)
        );
    }

    public static void chat(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message));
    }
}