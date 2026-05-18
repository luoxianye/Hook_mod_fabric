package com.lxy.hook.entity;

import com.lxy.hook.config.HookConfig;
import com.lxy.hook.item.ModItems;
import com.lxy.hook.util.HookMath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class HookProjectileEntity extends Entity implements ItemSupplier {

    private static final int MAX_LIFE_TICKS = 100;
    private static final double SPEED = 2.0;

    private static final EntityDataAccessor<String> OWNER_UUID_STRING =
            SynchedEntityData.defineId(HookProjectileEntity.class, EntityDataSerializers.STRING);

    private int lifeTicks;
    private double traveledDistance;

    public HookProjectileEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public static HookProjectileEntity shoot(Level level, Player owner) {
        HookProjectileEntity hook = new HookProjectileEntity(ModEntityTypes.HOOK_PROJECTILE, level);
        hook.setOwnerUuid(owner.getUUID());
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

        HookConfig cfg = HookConfig.INSTANCE;

        Vec3 currentPos = position();
        Vec3 velocity = getDeltaMovement();

        double remainingDistance = cfg.maxDistance - traveledDistance;
        if (remainingDistance <= 0.0D) {
            discard();
            return;
        }

        double velocityLength = velocity.length();
        if (velocityLength <= 1.0E-7D) {
            discard();
            return;
        }

        Vec3 stepVelocity = velocity;

        if (velocityLength > remainingDistance) {
            stepVelocity = velocity.normalize().scale(remainingDistance);
        }

        Vec3 nextPos = currentPos.add(stepVelocity);

        if (level().isClientSide()) {
            setPos(nextPos);
            traveledDistance += stepVelocity.length();

            if (traveledDistance >= cfg.maxDistance) {
                discard();
            }

            return;
        }

        BlockHitResult blockHit = level().clip(new ClipContext(
                currentPos,
                nextPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
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
        traveledDistance += stepVelocity.length();

        if (traveledDistance >= cfg.maxDistance) {
            discard();
        }
    }

    private boolean exceedsMaxDistance(Vec3 targetPos) {
        double distanceAfterHit = traveledDistance + position().distanceTo(targetPos);
        return distanceAfterHit > HookConfig.INSTANCE.maxDistance + 1.0E-4D;
    }
    private void onBlockHit(BlockHitResult hit) {
        Player owner = getOwnerPlayer();
        if (owner == null) {
            discard();
            return;
        }

        if (exceedsMaxDistance(hit.getLocation())) {
            discard();
            return;
        }

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
        if (owner == null) {
            discard();
            return;
        }

        if (exceedsMaxDistance(hit.getLocation())) {
            discard();
            return;
        }


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

    private void setOwnerUuid(UUID uuid) {
        this.entityData.set(OWNER_UUID_STRING, uuid.toString());
    }

    private Player getOwnerPlayer() {
        String uuidStr = this.entityData.get(OWNER_UUID_STRING);
        if (uuidStr.isEmpty()) return null;
        try {
            UUID uuid = UUID.fromString(uuidStr);
            Entity e = level().getEntity(uuid);
            return e instanceof Player p ? p : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
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

    // ====== 数据持久化与同步 ======

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER_UUID_STRING, "");
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        String uuidStr = this.entityData.get(OWNER_UUID_STRING);
        if (!uuidStr.isEmpty()) {
            out.putString("Owner", uuidStr);
        }
        out.putInt("LifeTicks", lifeTicks);
        out.putDouble("TraveledDistance", traveledDistance);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        in.getString("Owner").ifPresent(uuidStr -> this.entityData.set(OWNER_UUID_STRING, uuidStr));
        lifeTicks = in.getIntOr("LifeTicks", 0);
        traveledDistance = in.getDoubleOr("TraveledDistance", 0.0D);

    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(ModItems.THREW_HOOK);
    }

    /** 获取所有者 UUID（供客户端渲染绳索使用）。 */
    public UUID getOwnerId() {
        String uuidStr = this.entityData.get(OWNER_UUID_STRING);
        if (uuidStr.isEmpty()) return null;
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        discard();
        return true;
    }
}