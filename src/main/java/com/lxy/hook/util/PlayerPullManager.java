package com.lxy.hook.util;

import com.lxy.hook.config.HookConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class PlayerPullManager {
    private static final Map<UUID, PullTask> PULLING_PLAYERS = new HashMap<>();

    private static final double ARRIVE_DISTANCE = 1.15D;
    private static final int MAX_PULL_TICKS = 40;

    private PlayerPullManager() {
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(PlayerPullManager::tick);
    }

    public static void pullPlayerTo(ServerPlayer player, Vec3 targetPos) {
        PULLING_PLAYERS.put(player.getUUID(), new PullTask(targetPos));
        player.resetFallDistance();
    }

    private static void tick(MinecraftServer server) {
        HookConfig cfg = HookConfig.INSTANCE;

        Iterator<Map.Entry<UUID, PullTask>> iterator = PULLING_PLAYERS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, PullTask> entry = iterator.next();

            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            PullTask task = entry.getValue();

            if (player == null || !player.isAlive()) {
                iterator.remove();
                continue;
            }

            task.ticks++;

            Vec3 playerPos = player.position();
            Vec3 toTarget = task.targetPos.subtract(playerPos);
            double distance = toTarget.length();

            if (distance <= ARRIVE_DISTANCE || task.ticks > MAX_PULL_TICKS) {
                player.setDeltaMovement(player.getDeltaMovement().scale(0.25D));
                player.hurtMarked = true;

                if (cfg.reduceFallDamage) {
                    player.resetFallDistance();
                }

                iterator.remove();
                continue;
            }

            double distanceFactor = HookMath.calculateDistanceFactor(
                    distance,
                    cfg.minDistance,
                    cfg.maxDistance
            );

            double speed = cfg.blockPullStrength * distanceFactor;

            Vec3 velocity = toTarget.normalize().scale(speed);

            velocity = HookMath.clampHorizontalVelocity(velocity, cfg.maxHorizontalVelocity);
            velocity = HookMath.clampVerticalVelocity(velocity, cfg.maxVerticalVelocity);

            player.setDeltaMovement(velocity);
            player.hurtMarked = true;

            if (cfg.reduceFallDamage) {
                player.resetFallDistance();
            }
        }
    }

    private static class PullTask {
        private final Vec3 targetPos;
        private int ticks;

        private PullTask(Vec3 targetPos) {
            this.targetPos = targetPos;
            this.ticks = 0;
        }
    }
}