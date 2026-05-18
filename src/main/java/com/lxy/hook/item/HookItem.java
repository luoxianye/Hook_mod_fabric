package com.lxy.hook.item;

import com.lxy.hook.config.HookConfig;
import com.lxy.hook.entity.HookProjectileEntity;
import com.lxy.hook.tag.ModBlockTags;
import com.lxy.hook.util.HookMath;
import com.lxy.hook.util.HookRaycast;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

public class HookItem extends Item {

    public HookItem(Properties properties) {
        super(properties);
    }

    // ========== Tooltip ==========

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltipAdder, TooltipFlag flag) {
        tooltipAdder.accept(Component.translatable("item.hook.hook.tooltip"));
        tooltipAdder.accept(Component.translatable("item.hook.hook.tooltip.distance"));
        tooltipAdder.accept(Component.translatable("item.hook.hook.tooltip.cooldown"));
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
        player.getCooldowns().addCooldown(stack, HookConfig.INSTANCE.useCooldownTicks);

        BlockPos blockPos = context.getClickedPos();
        BlockState state = level.getBlockState(blockPos);

        if (!canGrappleBlock(level, blockPos, state)) {
            return InteractionResult.PASS;
        }

        Vec3 hitPos = context.getClickLocation();
        return grappleBlock(player, stack, hitPos, blockPos, level);
    }

    // ========== 右键空气（发射投射物） ==========

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.PASS;
        }

        // 右键使用瞬间进入冷却
        player.getCooldowns().addCooldown(stack, HookConfig.INSTANCE.useCooldownTicks);

        // 发射投射物
        HookProjectileEntity.shoot(level, player);

        return InteractionResult.CONSUME;
    }

    // ========== 核心逻辑 ==========

    private InteractionResult grappleBlock(Player player, ItemStack stack, Vec3 hitPos,
                                           BlockPos blockPos, Level level) {
        HookConfig cfg = HookConfig.INSTANCE;
        Vec3 playerPos = player.position();
        double distance = playerPos.distanceTo(hitPos);

        if (!isDistanceValid(distance)) {
            playFailEffects(level, player);
            onFailedUse(player, stack);
            return InteractionResult.CONSUME;
        }

        double distanceFactor = HookMath.calculateDistanceFactor(distance, cfg.minDistance, cfg.maxDistance);
        Vec3 velocity = HookMath.calculatePullVelocity(
                playerPos, hitPos,
                cfg.blockPullStrength, cfg.blockVerticalBoost, cfg.maxPullVelocity, distanceFactor
        );
        velocity = HookMath.clampHorizontalVelocity(velocity, cfg.maxHorizontalVelocity);
        velocity = HookMath.clampVerticalVelocity(velocity, cfg.maxVerticalVelocity);

        player.setDeltaMovement(velocity);
        player.hurtMarked = true;
        if (cfg.reduceFallDamage) {
            player.resetFallDistance();
        }

        playSuccessEffects(level, hitPos);
        onSuccessfulUse(player, stack, cfg.blockCooldownTicks);
        return InteractionResult.CONSUME;
    }

    private InteractionResult grappleEntity(Player player, ItemStack stack, Entity target,
                                            Vec3 hitPos, Level level) {
        HookConfig cfg = HookConfig.INSTANCE;

        if (!HookRaycast.isValidHookTarget(player, target, cfg.allowPullPlayers, cfg.allowPullBosses)) {
            playFailEffects(level, player);
            onFailedUse(player, stack);
            return InteractionResult.CONSUME;
        }

        double distance = player.position().distanceTo(hitPos);
        if (!isDistanceValid(distance)) {
            playFailEffects(level, player);
            onFailedUse(player, stack);
            return InteractionResult.CONSUME;
        }

        double distanceFactor = HookMath.calculateDistanceFactor(distance, cfg.minDistance, cfg.maxDistance);
        Vec3 velocity = HookMath.calculatePullVelocity(
                target.position(), player.position(),
                cfg.entityPullStrength, cfg.entityVerticalBoost, cfg.maxPullVelocity, distanceFactor
        );
        velocity = HookMath.clampHorizontalVelocity(velocity, cfg.maxHorizontalVelocity);
        velocity = HookMath.clampVerticalVelocity(velocity, cfg.maxVerticalVelocity);

        target.setDeltaMovement(velocity);
        target.hurtMarked = true;

        if (target instanceof LivingEntity living && cfg.reduceFallDamage) {
            living.fallDistance = 0;
        }

        playSuccessEffects(level, hitPos);
        onSuccessfulUse(player, stack, cfg.entityCooldownTicks);
        return InteractionResult.CONSUME;
    }

    // ========== 方块抓取判断 ==========

    private boolean canGrappleBlock(Level level, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return false;
        }
        // 黑名单优先
        if (state.is(ModBlockTags.HOOK_BLACKLIST)) {
            return false;
        }
        // 白名单
        if (state.is(ModBlockTags.HOOK_GRABBABLE)) {
            return true;
        }
        // 默认：有碰撞形状即可抓取
        return !state.getCollisionShape(level, pos).isEmpty();
    }

    // ========== 反馈效果 ==========

    private void playSuccessEffects(Level level, Vec3 hitPos) {
        // 音效
        level.playSound(null, hitPos.x, hitPos.y, hitPos.z,
                SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0F, 1.0F);

        // 粒子
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT, hitPos.x, hitPos.y, hitPos.z,
                    8, 0.15, 0.15, 0.15, 0.05);
        }
    }

    private void playFailEffects(Level level, Player player) {
        level.playSound(null, player.blockPosition(),
                SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6F, 1.2F);
    }

    // ========== 辅助方法 ==========

    private boolean isDistanceValid(double distance) {
        HookConfig cfg = HookConfig.INSTANCE;
        return distance >= cfg.minDistance && distance <= cfg.maxDistance;
    }

    private void onSuccessfulUse(Player player, ItemStack stack, int cooldownTicks) {
        HookConfig cfg = HookConfig.INSTANCE;
        EquipmentSlot slot = player.getEquipmentSlotForItem(stack);
        stack.hurtAndBreak(cfg.durabilityCost, player, slot);
    }

    private void onFailedUse(Player player, ItemStack stack) {
    }
}
