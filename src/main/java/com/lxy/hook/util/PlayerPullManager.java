package com.lxy.hook.util;

import com.lxy.hook.config.HookConfig;
import com.lxy.hook.entity.HookProjectileEntity;
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

    /**
     * 普通拉取最大持续时间。
     * 40 ticks = 2 秒。
     * 如果你觉得拉取时间太短，可以改回 60。
     */
    private static final int MAX_PULL_TICKS = 40;

    /**
     * 前几个 tick 玩家刚开始加速，不进行卡住判断，避免误判。
     */
    private static final int STUCK_CHECK_START_TICKS = 6;

    /**
     * 连续多少 tick 没有明显接近目标，就认为被方块卡住。
     */
    private static final int MAX_STUCK_TICKS = 4;

    /**
     * 有碰撞时，每 tick 距离目标减少量低于这个值，就认为没有有效前进。
     */
    private static final double COLLISION_PROGRESS_EPSILON = 0.03D;

    /**
     * 没有碰撞但距离几乎不变时，也认为可能卡住。
     */
    private static final double NO_PROGRESS_EPSILON = 0.005D;

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
        return releaseInternal(player, showMessage, false);
    }

    /**
     * R 键使用。
     *
     * 模式 A：松开时清空速度，保持原来的安全逻辑。
     * 模式 B 未到终点：松开时保留当前速度动能。
     * 模式 B 已悬挂：松开时清空速度，避免从悬挂点突然弹出。
     */
    public static boolean releaseForUseKey(ServerPlayer player, boolean showMessage) {
        PullTask task = PULLING_PLAYERS.get(player.getUUID());

        if (task == null) {
            return false;
        }

        boolean keepVelocity = task.anchorAfterArrival && !task.anchored;

        return releaseInternal(player, showMessage, keepVelocity);
    }

    /**
     * 空格 / 跳跃键专用。
     *
     * 只释放模式 B。
     * 模式 B 未到终点时释放：保留当前速度。
     * 模式 B 已悬挂时释放：清空速度。
     * 模式 A 不受影响。
     */
    public static boolean releaseModeBOnly(ServerPlayer player, boolean showMessage) {
        PullTask task = PULLING_PLAYERS.get(player.getUUID());

        if (task == null || !task.anchorAfterArrival) {
            return false;
        }

        boolean keepVelocity = !task.anchored;

        return releaseInternal(player, showMessage, keepVelocity);
    }

    private static boolean releaseInternal(ServerPlayer player, boolean showMessage, boolean keepVelocity) {
        PullTask task = PULLING_PLAYERS.remove(player.getUUID());

        if (task == null) {
            return false;
        }

        discardHookEntity(player, task);

        if (!keepVelocity) {
            player.setDeltaMovement(Vec3.ZERO);
        }

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
                            Component.keybind("key.hook.use"),
                            Component.keybind("key.jump")
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

            /*
             * 只对模式 A 生效。
             * 模式 A 如果玩家被方块碰撞卡住，并且连续几 tick 没有明显靠近目标点，
             * 就提前结束拉取，避免玩家在方块夹角中悬浮。
             */
            if (!task.anchorAfterArrival && isPlayerStuck(player, task, distance)) {
                player.setDeltaMovement(Vec3.ZERO);
                player.hurtMarked = true;

                if (cfg.reduceFallDamage) {
                    player.resetFallDistance();
                }

                iterator.remove();
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

    private static boolean isPlayerStuck(ServerPlayer player, PullTask task, double distance) {
        if (task.ticks < STUCK_CHECK_START_TICKS) {
            task.lastDistance = distance;
            return false;
        }

        double progress = task.lastDistance - distance;

        boolean hasCollision = player.horizontalCollision || player.verticalCollision;

        if (hasCollision && progress < COLLISION_PROGRESS_EPSILON) {
            task.stuckTicks++;
        } else if (progress < NO_PROGRESS_EPSILON) {
            task.stuckTicks++;
        } else {
            task.stuckTicks = 0;
        }

        task.lastDistance = distance;

        return task.stuckTicks >= MAX_STUCK_TICKS;
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

        private double lastDistance = Double.MAX_VALUE;
        private int stuckTicks = 0;

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