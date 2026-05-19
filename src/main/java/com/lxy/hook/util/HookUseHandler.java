package com.lxy.hook.util;

import com.lxy.hook.config.HookConfig;
import com.lxy.hook.entity.HookProjectileEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class HookUseHandler {
    private HookUseHandler() {
    }

    public static void releaseOrUseEquippedHook(ServerPlayer player) {
        if (PlayerPullManager.releaseForUseKey(player, true)) {
            return;
        }

        useEquippedHook(player);
    }

    public static void useEquippedHook(ServerPlayer player) {
        ItemStack hookStack = HookEquipment.findUsableHook(player);

        if (hookStack.isEmpty()) {
            return;
        }

        if (player.getCooldowns().isOnCooldown(hookStack)) {
            return;
        }

        player.getCooldowns().addCooldown(hookStack, HookConfig.INSTANCE.useCooldownTicks);
        HookProjectileEntity.shoot(player.level(), player);
    }
}