package com.lxy.hook.item;

import com.lxy.hook.util.HookMath;
import com.lxy.hook.util.HookRaycast;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class HookItem extends Item {
    // === 默认参数（后续阶段迁移至配置文件） ===
    private static final double MAX_DISTANCE = 32.0;
    private static final double MIN_DISTANCE = 2.0;

    private static final double BLOCK_PULL_STRENGTH = 3.6;
    private static final double ENTITY_PULL_STRENGTH = 2.4;

    private static final double BLOCK_VERTICAL_BOOST = 1.35;
    private static final double ENTITY_VERTICAL_BOOST = 0.75;

    private static final double MAX_PULL_VELOCITY = 7.5;
    private static final double MAX_HORIZONTAL_VELOCITY = 6.6;
    private static final double MAX_VERTICAL_VELOCITY = 3.6;

    private static final int BLOCK_COOLDOWN_TICKS = 20;
    private static final int ENTITY_COOLDOWN_TICKS = 25;
    private static final int FAIL_COOLDOWN_TICKS = 5;

    private static final int DURABILITY_COST = 1;

    public HookItem(Properties properties) {
        super(properties);
    }

    // ========== 右键方块（直接点击） ==========

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = context.getItemInHand();
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.PASS;
        }

        BlockPos blockPos = context.getClickedPos();
        BlockState state = level.getBlockState(blockPos);
        if (state.isAir()) {
            return InteractionResult.PASS;
        }
        if (state.getCollisionShape(level, blockPos).isEmpty()) {
            return InteractionResult.PASS;
        }

        Vec3 hitPos = context.getClickLocation();
        return grappleBlock(player, stack, hitPos, blockPos);
    }

    // ========== 右键空气（Raycast 命中） ==========

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // 冷却检查：防止连发绕过
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.PASS;
        }

        HookRaycast.HookHitResult result = HookRaycast.raycast(level, player, MAX_DISTANCE);

        if (result == null) {
            onFailedUse(player, stack);
            return InteractionResult.CONSUME;
        }

        if (result.isEntity()) {
            return grappleEntity(player, stack, result.entity, result.hitPos);
        }

        if (result.isBlock()) {
            BlockState state = level.getBlockState(result.blockPos);
            if (state.isAir() || state.getCollisionShape(level, result.blockPos).isEmpty()) {
                onFailedUse(player, stack);
                return InteractionResult.CONSUME;
            }
            return grappleBlock(player, stack, result.hitPos, result.blockPos);
        }

        return InteractionResult.PASS;
    }

    // ========== 核心逻辑 ==========

    /**
     * 方块拉动逻辑。
     */
    private InteractionResult grappleBlock(Player player, ItemStack stack, Vec3 hitPos, BlockPos blockPos) {
        Vec3 playerPos = player.position();
        double distance = playerPos.distanceTo(hitPos);

        if (!isDistanceValid(distance)) {
            onFailedUse(player, stack);
            return InteractionResult.CONSUME;
        }

        // 距离衰减：近距离弱拉力，远距离强拉力
        double distanceFactor = HookMath.calculateDistanceFactor(distance, MIN_DISTANCE, MAX_DISTANCE);

        Vec3 velocity = HookMath.calculatePullVelocity(
                playerPos, hitPos,
                BLOCK_PULL_STRENGTH, BLOCK_VERTICAL_BOOST, MAX_PULL_VELOCITY, distanceFactor
        );

        // 分轴限速
        velocity = HookMath.clampHorizontalVelocity(velocity, MAX_HORIZONTAL_VELOCITY);
        velocity = HookMath.clampVerticalVelocity(velocity, MAX_VERTICAL_VELOCITY);

        player.setDeltaMovement(velocity);
        player.hurtMarked = true;
        player.resetFallDistance();

        onSuccessfulUse(player, stack, BLOCK_COOLDOWN_TICKS);
        return InteractionResult.CONSUME;
    }

    /**
     * 实体拉动逻辑。
     */
    private InteractionResult grappleEntity(Player player, ItemStack stack, Entity target, Vec3 hitPos) {
        if (!HookRaycast.isValidHookTarget(player, target)) {
            onFailedUse(player, stack);
            return InteractionResult.CONSUME;
        }

        double distance = player.position().distanceTo(hitPos);
        if (!isDistanceValid(distance)) {
            onFailedUse(player, stack);
            return InteractionResult.CONSUME;
        }

        // 距离衰减
        double distanceFactor = HookMath.calculateDistanceFactor(distance, MIN_DISTANCE, MAX_DISTANCE);

        Vec3 velocity = HookMath.calculatePullVelocity(
                target.position(), player.position(),
                ENTITY_PULL_STRENGTH, ENTITY_VERTICAL_BOOST, MAX_PULL_VELOCITY, distanceFactor
        );

        // 分轴限速
        velocity = HookMath.clampHorizontalVelocity(velocity, MAX_HORIZONTAL_VELOCITY);
        velocity = HookMath.clampVerticalVelocity(velocity, MAX_VERTICAL_VELOCITY);

        target.setDeltaMovement(velocity);
        target.hurtMarked = true;

        // 对生物：重置摔落距离，防止拉过来后摔死
        if (target instanceof LivingEntity living) {
            living.fallDistance = 0;
        }

        onSuccessfulUse(player, stack, ENTITY_COOLDOWN_TICKS);
        return InteractionResult.CONSUME;
    }

    // ========== 辅助方法 ==========

    /**
     * 距离是否在有效范围内。
     */
    private boolean isDistanceValid(double distance) {
        return distance >= MIN_DISTANCE && distance <= MAX_DISTANCE;
    }

    private void onSuccessfulUse(Player player, ItemStack stack, int cooldownTicks) {
        player.getCooldowns().addCooldown(stack, cooldownTicks);
        EquipmentSlot slot = player.getEquipmentSlotForItem(stack);
        stack.hurtAndBreak(DURABILITY_COST, player, slot);
    }

    private void onFailedUse(Player player, ItemStack stack) {
        player.getCooldowns().addCooldown(stack, FAIL_COOLDOWN_TICKS);
    }
}
