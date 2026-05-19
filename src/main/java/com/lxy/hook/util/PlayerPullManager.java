package com.lxy.hook.util;

import com.lxy.hook.config.HookConfig;
import com.lxy.hook.entity.HookProjectileEntity;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class PlayerPullManager {
    private static final Map<UUID, PullTask> PULLING_PLAYERS = new HashMap<>();

    private static final double ARRIVE_DISTANCE = 1.15D;
    private static final int MAX_PULL_TICKS = 60;

    private PlayerPullManager() {
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(PlayerPullManager::tick);
    }

    public static void pullPlayerTo(ServerPlayer player, Vec3 targetPos) {
        PULLING_PLAYERS.put(player.getUUID(), PullTask.normal(targetPos));
        player.resetFallDistance();
    }

    public static void pullPlayerToAndAnchor(ServerPlayer player, Vec3 targetPos, int hookEntityId) {
        PULLING_PLAYERS.put(player.getUUID(), PullTask.anchor(targetPos, hookEntityId));
        player.resetFallDistance();
    }

    public static boolean release(ServerPlayer player, boolean showMessage) {
        PullTask task = PULLING_PLAYERS.remove(player.getUUID());

        if (task == null) {
            return false;
        }

        discardHookEntity(player, task);
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        player.resetFallDistance();

        if (showMessage) {
            HookMessage.actionBar(player, "勾爪已松开");
        }

        return true;
    }

    public static boolean isAnchored(ServerPlayer player) {
        PullTask task = PULLING_PLAYERS.get(player.getUUID());
        return task != null && task.anchored;
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

            if (task.anchored) {
                tickAnchoredPlayer(player, task, iterator);
                continue;
            }

            task.ticks++;

            Vec3 playerPos = player.position();
            Vec3 toTarget = task.targetPos.subtract(playerPos);
            double distance = toTarget.length();

            if (distance <= ARRIVE_DISTANCE) {
                if (task.anchorAfterArrival) {
                    task.anchored = true;
                    task.anchorPos = player.position();
                    player.setDeltaMovement(Vec3.ZERO);
                    player.hurtMarked = true;
                    player.resetFallDistance();
                    HookMessage.actionBar(player, Component.translatable(
                            "message.hook.anchored",
                            Component.keybind("key.hook.use")
                    ));
                } else {
                    player.setDeltaMovement(player.getDeltaMovement().scale(0.25D));
                    player.hurtMarked = true;
                    if (cfg.reduceFallDamage) {
                        player.resetFallDistance();
                    }
                    iterator.remove();
                }

                continue;
            }

            if (task.ticks > MAX_PULL_TICKS) {
                discardHookEntity(player, task);
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

    private static void tickAnchoredPlayer(
            ServerPlayer player,
            PullTask task,
            Iterator<Map.Entry<UUID, PullTask>> iterator
    ) {
        Entity hookEntity = player.level().getEntity(task.hookEntityId);

        if (!(hookEntity instanceof HookProjectileEntity) || !hookEntity.isAlive()) {
            iterator.remove();
            return;
        }

        Vec3 correction = task.anchorPos.subtract(player.position());

        if (correction.lengthSqr() > 0.01D) {
            player.setDeltaMovement(correction.scale(0.45D));
        } else {
            player.setDeltaMovement(Vec3.ZERO);
        }

        player.hurtMarked = true;
        player.resetFallDistance();
    }

    private static void discardHookEntity(ServerPlayer player, PullTask task) {
        if (task.hookEntityId < 0) {
            return;
        }

        Entity entity = player.level().getEntity(task.hookEntityId);

        if (entity instanceof HookProjectileEntity hook) {
            hook.discard();
        }
    }

    private static class PullTask {
        private final Vec3 targetPos;
        private final boolean anchorAfterArrival;
        private final int hookEntityId;

        private Vec3 anchorPos;
        private int ticks;
        private boolean anchored;

        private PullTask(Vec3 targetPos, boolean anchorAfterArrival, int hookEntityId) {
            this.targetPos = targetPos;
            this.anchorAfterArrival = anchorAfterArrival;
            this.hookEntityId = hookEntityId;
            this.anchorPos = targetPos;
            this.ticks = 0;
            this.anchored = false;
        }

        private static PullTask normal(Vec3 targetPos) {
            return new PullTask(targetPos, false, -1);
        }

        private static PullTask anchor(Vec3 targetPos, int hookEntityId) {
            return new PullTask(targetPos, true, hookEntityId);
        }
    }
}