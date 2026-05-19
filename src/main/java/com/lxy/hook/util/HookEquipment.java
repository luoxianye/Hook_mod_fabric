package com.lxy.hook.util;

import com.lxy.hook.item.ModItems;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class HookEquipment {
    private HookEquipment() {
    }

    public static ItemStack findUsableHook(Player player) {
        ItemStack equipped = findEquippedHook(player);

        if (!equipped.isEmpty()) {
            return equipped;
        }

        if (player.getMainHandItem().getItem() == ModItems.HOOK) {
            return player.getMainHandItem();
        }

        if (player.getOffhandItem().getItem() == ModItems.HOOK) {
            return player.getOffhandItem();
        }

        return ItemStack.EMPTY;
    }

    private static ItemStack findEquippedHook(Player player) {
        return TrinketsApi.getAttachment(player)
                .findFirst(ModItems.HOOK)
                .map(TrinketSlotAccess::get)
                .orElse(ItemStack.EMPTY);
    }
}