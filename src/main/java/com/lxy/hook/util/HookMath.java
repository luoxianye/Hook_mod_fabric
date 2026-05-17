package com.lxy.hook.util;

import net.minecraft.world.phys.Vec3;

public final class HookMath {
    private HookMath() {
    }

    /**
     * 计算从 from 向 to 的拉动速度向量。
     *
     * @param from          起点（玩家位置）
     * @param to            目标点（命中位置）
     * @param strength      基础拉力强度
     * @param verticalBoost 垂直额外提升
     * @param maxVelocity   最大速度限制
     * @return 计算后的速度向量
     */
    public static Vec3 calculatePullVelocity(
            Vec3 from,
            Vec3 to,
            double strength,
            double verticalBoost,
            double maxVelocity
    ) {
        Vec3 direction = to.subtract(from);
        if (direction.lengthSqr() < 0.0001) {
            return Vec3.ZERO;
        }

        Vec3 velocity = direction.normalize().scale(strength);
        velocity = new Vec3(velocity.x, velocity.y + verticalBoost, velocity.z);

        if (velocity.length() > maxVelocity) {
            velocity = velocity.normalize().scale(maxVelocity);
        }

        return velocity;
    }
}
