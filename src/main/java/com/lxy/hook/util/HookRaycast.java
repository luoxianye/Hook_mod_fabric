package com.lxy.hook.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public final class HookRaycast {
    private HookRaycast() {
    }

    /**
     * 从玩家视线执行射线检测，优先返回最近的命中结果（实体优先于方块）。
     *
     * @param level       当前世界
     * @param player      使用者
     * @param maxDistance 最大检测距离
     * @return 命中结果（可为 null）
     */
    public static HookHitResult raycast(Level level, Player player, double maxDistance) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(maxDistance));

        // 1. 方块射线检测
        ClipContext context = new ClipContext(
                start, end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        );
        BlockHitResult blockHit = level.clip(context);

        // 2. 实体检测
        EntityHitResult entityHit = raycastEntities(level, player, start, end, maxDistance);

        // 3. 选择最近命中（实体优先）
        if (entityHit != null) {
            double entityDist = start.distanceToSqr(entityHit.getLocation());
            if (blockHit == null || blockHit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
                return new HookHitResult(entityHit.getEntity(), entityHit.getLocation(), null);
            }
            double blockDist = start.distanceToSqr(blockHit.getLocation());
            if (entityDist <= blockDist) {
                return new HookHitResult(entityHit.getEntity(), entityHit.getLocation(), null);
            }
        }

        if (blockHit != null && blockHit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            return new HookHitResult(null, blockHit.getLocation(), blockHit.getBlockPos());
        }

        return null;
    }

    /**
     * 检测玩家视线方向上的最近实体。
     */
    private static EntityHitResult raycastEntities(Level level, Player player, Vec3 start, Vec3 end, double maxDistance) {
        Vec3 look = player.getLookAngle();

        // 构建搜索包围盒
        AABB searchBox = player.getBoundingBox()
                .expandTowards(look.scale(maxDistance))
                .inflate(1.0);

        Predicate<Entity> filter = entity -> isValidHookTarget(player, entity);
        List<Entity> candidates = level.getEntities(player, searchBox, filter);

        EntityHitResult closest = null;
        double closestDistSqr = maxDistance * maxDistance;

        for (Entity entity : candidates) {
            AABB box = entity.getBoundingBox().inflate(0.3);
            Optional<Vec3> optionalHit = box.clip(start, end);

            if (optionalHit.isPresent()) {
                double distSqr = start.distanceToSqr(optionalHit.get());
                if (distSqr < closestDistSqr) {
                    closestDistSqr = distSqr;
                    closest = new EntityHitResult(entity, optionalHit.get());
                }
            }
        }

        return closest;
    }

    /**
     * 判断实体是否为有效的勾爪目标。
     *
     * <p>实体规则：
     * <ul>
     *   <li>排除 null、自身、已死亡实体</li>
     *   <li>排除创造模式 / 旁观模式玩家</li>
     *   <li>排除 Boss（末影龙、凋灵）</li>
     *   <li>第一版默认不允许拉其他玩家（后续通过配置开启 PvP）</li>
     *   <li>排除不可推动实体</li>
     * </ul>
     */
    public static boolean isValidHookTarget(Player player, Entity target) {
        if (target == null) {
            return false;
        }
        if (target == player) {
            return false;
        }
        if (!target.isAlive()) {
            return false;
        }

        // Boss 检查
        if (isBossEntity(target)) {
            return false;
        }

        // 玩家规则：第一版默认禁止拉其他玩家
        if (target instanceof Player targetPlayer) {
            if (targetPlayer.isSpectator() || targetPlayer.isCreative()) {
                return false;
            }
            // PvP 勾爪默认关闭，后续通过配置开启
            return false;
        }

        // 不可推动实体
        if (!target.isPushable()) {
            return false;
        }

        // 无物理实体
        if (target.noPhysics) {
            return false;
        }

        return true;
    }

    /**
     * 判断实体是否为 Boss。
     */
    private static boolean isBossEntity(Entity entity) {
        // 末影龙和凋灵是主要 Boss
        if (entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) {
            return true;
        }
        if (entity instanceof net.minecraft.world.entity.boss.wither.WitherBoss) {
            return true;
        }
        return false;
    }

    /**
     * 射线命中结果封装。
     */
    public static class HookHitResult {
        public final Entity entity;
        public final Vec3 hitPos;
        public final net.minecraft.core.BlockPos blockPos;

        public HookHitResult(Entity entity, Vec3 hitPos, net.minecraft.core.BlockPos blockPos) {
            this.entity = entity;
            this.hitPos = hitPos;
            this.blockPos = blockPos;
        }

        public boolean isEntity() {
            return entity != null;
        }

        public boolean isBlock() {
            return blockPos != null;
        }
    }
}
