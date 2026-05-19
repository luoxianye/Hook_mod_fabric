package com.lxy.hook.network;

import com.lxy.hook.HookMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record UseHookPayload(boolean pressed) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(HookMod.MOD_ID, "use_hook");
    public static final Type<UseHookPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, UseHookPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    UseHookPayload::pressed,
                    UseHookPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}