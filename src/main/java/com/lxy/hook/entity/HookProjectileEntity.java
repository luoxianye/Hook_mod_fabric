package com.lxy.hook.entity;

import com.lxy.hook.config.HookConfig;
import com.lxy.hook.item.ModItems;
import com.lxy.hook.util.HookMath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

public class HookProjectileEntity extends Entity implements ItemSupplier {

    private static final int MAX_LIFE_TICKS = 100;
    private static final double SPEED = 2.0;

    private UUID ownerUuid;
    private int lifeTicks;

    public HookProjectileEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public static HookProjectileEntity shoot(Level level, Player owner) {
        HookProjectileEntity hook = new HookProjectileEntity(ModEntityTypes.HOOK_PROJECTILE, level);
        hook.ownerUuid = owner.getUUID();
        hook.setPos(owner.getEyePosition());

        Vec3 look = owner.getLookAngle();
        hook.setDeltaMovement(look.scale(SPEED));

        level.addFreshEntity(hook);
        level.playSound(null, owner.blockPosition(),
                SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 1.0F, 1.0F);

        return hook;
    }

    @Override
    public void tick() {
        super.tick();
        lifeTicks++;

        if (lifeTicks > MAX_LIFE_TICKS) {
            discard();
            return;
        }

        if (level().isClientSide()) {
            return;
        }

        Vec3 currentPos = position();
        Vec3 nextPos = currentPos.add(getDeltaMovement());

        BlockHitResult blockHit = level().clip(new net.minecraft.world.level.ClipContext(
                currentPos, nextPos,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                this
        ));

        if (blockHit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            onBlockHit(blockHit);
            return;
        }

        EntityHitResult entityHit = findHitEntity(currentPos, nextPos);
        if (entityHit != null) {
            onEntityHit(entityHit);
            return;
        }

        setPos(nextPos);
    }

    private void onBlockHit(BlockHitResult hit) {
        Player owner = getOwnerPlayer();
        if (owner == null) { discard(); return; }

        BlockPos blockPos = hit.getBlockPos();
        BlockState state = level().getBlockState(blockPos);
        if (state.isAir() || state.getCollisionShape(level(), blockPos).isEmpty()) {
            discard();
            return;
        }

        level().playSound(null, hit.getLocation().x, hit.getLocation().y, hit.getLocation().z,
                SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0F, 1.0F);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    hit.getLocation().x, hit.getLocation().y, hit.getLocation().z,
                    8, 0.15, 0.15, 0.15, 0.05);
        }

        pullPlayerToHit(owner, hit.getLocation());
        applyCooldownAndDurability(owner);
        discard();
    }

    private void onEntityHit(EntityHitResult hit) {
        Player owner = getOwnerPlayer();
        if (owner == null) { discard(); return; }

        Entity target = hit.getEntity();
        if (target == owner || !com.lxy.hook.util.HookRaycast.isValidHookTarget(owner, target)) {
            discard();
            return;
        }

        pullEntityToPlayer(target, owner);

        level().playSound(null, hit.getLocation().x, hit.getLocation().y, hit.getLocation().z,
                SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0F, 1.0F);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    hit.getLocation().x, hit.getLocation().y, hit.getLocation().z,
                    8, 0.15, 0.15, 0.15, 0.05);
        }

        applyCooldownAndDurability(owner);
        discard();
    }

    private void pullPlayerToHit(Player player, Vec3 hitPos) {
        HookConfig cfg = HookConfig.INSTANCE;
        double distance = player.position().distanceTo(hitPos);
        double distanceFactor = HookMath.calculateDistanceFactor(distance, cfg.minDistance, cfg.maxDistance);

        Vec3 velocity = HookMath.calculatePullVelocity(
                player.position(), hitPos,
                cfg.blockPullStrength, cfg.blockVerticalBoost, cfg.maxPullVelocity, distanceFactor
        );
        velocity = HookMath.clampHorizontalVelocity(velocity, cfg.maxHorizontalVelocity);
        velocity = HookMath.clampVerticalVelocity(velocity, cfg.maxVerticalVelocity);

        player.setDeltaMovement(velocity);
        player.hurtMarked = true;
        if (cfg.reduceFallDamage) {
            player.resetFallDistance();
        }
    }

    private void pullEntityToPlayer(Entity target, Player player) {
        HookConfig cfg = HookConfig.INSTANCE;
        double distance = target.position().distanceTo(player.position());
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
    }

    private void applyCooldownAndDurability(Player player) {
        HookConfig cfg = HookConfig.INSTANCE;
        ItemStack stack = findHookInHand(player);
        if (stack != null) {
            player.getCooldowns().addCooldown(stack, cfg.blockCooldownTicks);
            EquipmentSlot slot = player.getEquipmentSlotForItem(stack);
            stack.hurtAndBreak(cfg.durabilityCost, player, slot);
        }
    }

    private ItemStack findHookInHand(Player player) {
        if (player.getMainHandItem().getItem() == ModItems.HOOK) return player.getMainHandItem();
        if (player.getOffhandItem().getItem() == ModItems.HOOK) return player.getOffhandItem();
        return null;
    }

    private Player getOwnerPlayer() {
        if (ownerUuid == null) return null;
        Entity e = level().getEntity(ownerUuid);
        return e instanceof Player p ? p : null;
    }

    private EntityHitResult findHitEntity(Vec3 from, Vec3 to) {
        for (Entity entity : level().getEntities(this,
                getBoundingBox().expandTowards(getDeltaMovement()).inflate(0.5),
                e -> e != this && !e.isSpectator() && e.isAlive())) {
            if (entity.getBoundingBox().inflate(0.3).clip(from, to).isPresent()) {
                return new EntityHitResult(entity);
            }
        }
        return null;
    }

    // ====== 数据持久化 ======

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        if (ownerUuid != null) {
            out.putString("Owner", ownerUuid.toString());
        }
        out.putInt("LifeTicks", lifeTicks);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        ownerUuid = in.getString("Owner").map(UUID::fromString).orElse(null);
        lifeTicks = in.getIntOr("LifeTicks", 0);
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(ModItems.HOOK);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        // 投射物无需同步额外数据
    }

    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        discard();
        return true;
    }
}