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
    private static final double BLOCK_PULL_STRENGTH = 1.2;
    private static final double ENTITY_PULL_STRENGTH = 0.8;
    private static final double BLOCK_VERTICAL_BOOST = 0.45;
    private static final double ENTITY_VERTICAL_BOOST = 0.25;
    private static final double MAX_PULL_VELOCITY = 2.5;
    private static final int SUCCESS_COOLDOWN_TICKS = 20;
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

        BlockPos blockPos = context.getClickedPos();
        BlockState state = level.getBlockState(blockPos);
        if (state.isAir()) {
            return InteractionResult.PASS;
        }
        if (state.getCollisionShape(level, blockPos).isEmpty()) {
            return InteractionResult.PASS;
        }

        Vec3 hitPos = context.getClickLocation();
        return grappleBlock(player, context.getItemInHand(), hitPos, blockPos);
    }

    // ========== 右键空气（Raycast 命中） ==========

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        HookRaycast.HookHitResult result = HookRaycast.raycast(level, player, MAX_DISTANCE);

        if (result == null) {
            // 未命中任何目标，短冷却，不扣耐久
            onFailedUse(player, stack);
            return InteractionResult.CONSUME;
        }

        // 实体优先
        if (result.isEntity()) {
            return grappleEntity(player, stack, result.entity, result.hitPos);
        }

        // 命中方块
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
     * 方块拉动逻辑（阶段 4 提取为公共方法）。
     */
    private InteractionResult grappleBlock(Player player, ItemStack stack, Vec3 hitPos, BlockPos blockPos) {
        Vec3 playerPos = player.position();
        double distance = playerPos.distanceTo(hitPos);

        if (distance > MAX_DISTANCE || distance < MIN_DISTANCE) {
            onFailedUse(player, stack);
            return InteractionResult.CONSUME;
        }

        Vec3 velocity = HookMath.calculatePullVelocity(
                playerPos, hitPos,
                BLOCK_PULL_STRENGTH, BLOCK_VERTICAL_BOOST, MAX_PULL_VELOCITY
        );
        player.setDeltaMovement(velocity);
        player.hurtMarked = true;
        player.resetFallDistance();

        onSuccessfulUse(player, stack);
        return InteractionResult.CONSUME;
    }

    /**
     * 实体拉动逻辑。
     *
     * <p>将目标实体拉向玩家。对 LivingEntity 额外重置摔落距离。
     */
    private InteractionResult grappleEntity(Player player, ItemStack stack, Entity target, Vec3 hitPos) {
        if (!HookRaycast.isValidHookTarget(player, target)) {
            onFailedUse(player, stack);
            return InteractionResult.CONSUME;
        }

        double distance = player.position().distanceTo(hitPos);
        if (distance > MAX_DISTANCE || distance < MIN_DISTANCE) {
            onFailedUse(player, stack);
            return InteractionResult.CONSUME;
        }

        // 计算拉力：从目标位置拉向玩家位置
        Vec3 velocity = HookMath.calculatePullVelocity(
                target.position(), player.position(),
                ENTITY_PULL_STRENGTH, ENTITY_VERTICAL_BOOST, MAX_PULL_VELOCITY
        );

        // 应用速度
        target.setDeltaMovement(velocity);
        target.hurtMarked = true;

        // 对生物额外处理
        if (target instanceof LivingEntity living) {
            // 重置摔落距离，防止拉过来后摔死
            living.fallDistance = 0;
        }

        onSuccessfulUse(player, stack);
        return InteractionResult.CONSUME;
    }

    // ========== 辅助方法 ==========

    private void onSuccessfulUse(Player player, ItemStack stack) {
        player.getCooldowns().addCooldown(stack, SUCCESS_COOLDOWN_TICKS);
        EquipmentSlot slot = player.getEquipmentSlotForItem(stack);
        stack.hurtAndBreak(DURABILITY_COST, player, slot);
    }

    private void onFailedUse(Player player, ItemStack stack) {
        player.getCooldowns().addCooldown(stack, FAIL_COOLDOWN_TICKS);
    }
}
