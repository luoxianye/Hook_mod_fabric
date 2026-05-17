package com.lxy.hook.item;

import com.lxy.hook.util.HookMath;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
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
    private static final double VERTICAL_BOOST = 0.45;
    private static final double MAX_PULL_VELOCITY = 2.5;
    private static final int SUCCESS_COOLDOWN_TICKS = 20;
    private static final int FAIL_COOLDOWN_TICKS = 5;
    private static final int DURABILITY_COST = 1;

    public HookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();

        if (player == null) {
            return InteractionResult.PASS;
        }

        // 客户端只返回成功信号，真正逻辑在服务端执行
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockPos blockPos = context.getClickedPos();
        BlockState state = level.getBlockState(blockPos);

        // 检查方块是否可抓取
        if (state.isAir()) {
            return InteractionResult.PASS;
        }

        if (state.getCollisionShape(level, blockPos).isEmpty()) {
            return InteractionResult.PASS;
        }

        Vec3 hitPos = context.getClickLocation();
        Vec3 playerPos = player.position();
        double distance = playerPos.distanceTo(hitPos);

        // 距离检查
        if (distance > MAX_DISTANCE || distance < MIN_DISTANCE) {
            player.getCooldowns().addCooldown(context.getItemInHand(), FAIL_COOLDOWN_TICKS);
            return InteractionResult.CONSUME;
        }

        // 计算拉动速度
        Vec3 velocity = HookMath.calculatePullVelocity(
                playerPos,
                hitPos,
                BLOCK_PULL_STRENGTH,
                VERTICAL_BOOST,
                MAX_PULL_VELOCITY
        );

        // 应用速度
        player.setDeltaMovement(velocity);
        player.hurtMarked = true;
        player.resetFallDistance();

        // 冷却与耐久消耗
        ItemStack stack = context.getItemInHand();
        player.getCooldowns().addCooldown(stack, SUCCESS_COOLDOWN_TICKS);
        EquipmentSlot slot = player.getEquipmentSlotForItem(stack);
        stack.hurtAndBreak(DURABILITY_COST, player, slot);

        return InteractionResult.CONSUME;
    }
}
