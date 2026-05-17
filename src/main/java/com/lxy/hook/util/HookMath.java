package com.lxy.hook.util;

import net.minecraft.world.phys.Vec3;

public final class HookMath {
    private HookMath() {
    }

    /**
     * 计算从 from 向 to 的拉动速度向量。
     *
     * @param from           起点位置
     * @param to             目标位置
     * @param strength       基础拉力强度
     * @param verticalBoost  垂直额外提升
     * @param maxVelocity    最大总速度限制
     * @param distanceFactor 距离衰减因子（0.25 ~ 1.0）
     * @return 计算后的速度向量
     */
    public static Vec3 calculatePullVelocity(
            Vec3 from,
            Vec3 to,
            double strength,
            double verticalBoost,
            double maxVelocity,
            double distanceFactor
    ) {
        Vec3 direction = to.subtract(from);
        if (direction.lengthSqr() < 0.0001) {
            return Vec3.ZERO;
        }

        // 应用基础强度和距离衰减
        double effectiveStrength = strength * distanceFactor;
        Vec3 velocity = direction.normalize().scale(effectiveStrength);
        velocity = new Vec3(velocity.x, velocity.y + verticalBoost * distanceFactor, velocity.z);

        if (velocity.length() > maxVelocity) {
            velocity = velocity.normalize().scale(maxVelocity);
        }

        return velocity;
    }

    /**
     * 计算距离衰减因子。
     * <ul>
     *   <li>近距离（≤ MIN）→ 0.25（弱拉力）</li>
     *   <li>远距离（≥ MAX）→ 1.0（满拉力）</li>
     *   <li>中等距离 → 线性插值</li>
     * </ul>
     *
     * @param distance    当前距离
     * @param minDistance 最小距离
     * @param maxDistance 最大距离
     * @return 衰减因子 [0.25, 1.0]
     */
    public static double calculateDistanceFactor(double distance, double minDistance, double maxDistance) {
        if (distance <= minDistance) {
            return 0.25;
        }
        if (distance >= maxDistance) {
            return 1.0;
        }
        // 线性插值：近距离弱，远距离强
        double t = (distance - minDistance) / (maxDistance - minDistance);
        return 0.25 + t * 0.75;
    }

    /**
     * 将速度向量的水平分量限制在指定范围内。
     */
    public static Vec3 clampHorizontalVelocity(Vec3 velocity, double maxHorizontal) {
        double h = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (h > maxHorizontal) {
            double scale = maxHorizontal / h;
            return new Vec3(velocity.x * scale, velocity.y, velocity.z * scale);
        }
        return velocity;
    }

    /**
     * 将速度向量的垂直分量限制在指定范围内。
     */
    public static Vec3 clampVerticalVelocity(Vec3 velocity, double maxVertical) {
        if (Math.abs(velocity.y) > maxVertical) {
            return new Vec3(velocity.x, Math.signum(velocity.y) * maxVertical, velocity.z);
        }
        return velocity;
    }
}
