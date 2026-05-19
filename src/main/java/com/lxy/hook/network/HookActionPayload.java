package com.lxy.hook.network;

import com.lxy.hook.HookMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record HookActionPayload(int action) implements CustomPacketPayload {
    public static final int USE_OR_RELEASE = 0;
    public static final int TOGGLE_MODE = 1;

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(HookMod.MOD_ID, "hook_action");

    public static final Type<HookActionPayload> TYPE =
            new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, HookActionPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    HookActionPayload::action,
                    HookActionPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}