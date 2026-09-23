package org.agmas.noellesroles.roles.myers;

import dev.doctor4t.wathe.api.psycho.PsychoModeApi;
import dev.doctor4t.wathe.api.visibility.TargetVisibilityApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.block_entity.DoorBlockEntity;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.index.WatheSounds;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;

import java.util.UUID;

/** 迈尔斯的吸收、屠刀蓄力和冲刺运行态。 */
public final class MyersPlayerComponent implements AutoSyncedComponent, ServerTickingComponent, ClientTickingComponent {
    public static final ComponentKey<MyersPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(NoellesRolesCore.MOD_ID, "myers"), MyersPlayerComponent.class
    );

    private final PlayerEntity player;
    private int passiveTicker;
    private int absorbTicker;
    private int targetSyncTicker;
    private boolean absorbing;
    private int absorbTargetCount;
    private int chargeTicks;
    private boolean charging;
    private boolean forcedHold;
    private boolean dashing;
    private int dashTicksLeft;
    private Vec3d dashDirection = Vec3d.ZERO;
    private UUID releaseTarget;
    private boolean dashStartedInEvilPossess;
    private int initialParticipantCount = 1;
    private boolean psychoWasActive;

    public MyersPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public boolean isAbsorbing() { return absorbing; }
    public int getAbsorbTargetCount() { return absorbTargetCount; }
    public boolean isCharging() { return charging; }
    public boolean isForcedHold() { return forcedHold; }
    public boolean isDashing() { return dashing; }
    public int getChargeTicks() { return chargeTicks; }
    public int getInitialParticipantCount() { return Math.max(1, initialParticipantCount); }

    public void setInitialParticipantCount(int count) {
        initialParticipantCount = Math.max(1, count);
        sync();
    }

    @Override
    public void serverTick() {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        boolean isMyers = gameWorld.isRole(player, NoellesRoleRegistry.MYERS);
        boolean usable = isMyers
                && !TimekeeperPlayerComponent.KEY.get(player).isInTimeRift()
                && (GameFunctions.isPlayerAliveAndSurvival(player)
                || GameFunctions.isPlayerSpectatingOrCreative(player));

        if (!isMyers) {
            clearRuntime(false);
            return;
        }

        boolean psychoActive = MyersPsychoHandler.isActive(player);
        if (psychoWasActive && !psychoActive && player instanceof ServerPlayerEntity serverPlayer) {
            MyersPsychoHandler.playReleaseSound(serverPlayer);
        }
        psychoWasActive = psychoActive;

        if (GameFunctions.isPlayerAliveAndSurvival(player)
                && !TimekeeperPlayerComponent.KEY.get(player).isInTimeRift()) {
            passiveTicker++;
            if (passiveTicker >= MyersConstants.PASSIVE_MALICE_INTERVAL_TICKS) {
                passiveTicker = 0;
                PlayerShopComponent.KEY.get(player).addCurrencyAmount(
                        MyersConstants.MALICE_CURRENCY_ID, MyersConstants.PASSIVE_MALICE_AMOUNT
                );
            }
        } else {
            passiveTicker = 0;
        }

        if (charging) {
            chargeTicks = Math.min(MyersConstants.BUTCHER_KNIFE_MAX_CHARGE_TICKS, chargeTicks + 1);
            if (player.isSprinting() && GameFunctions.isPlayerAliveAndSurvival(player)) {
                player.setSprinting(false);
            }
            if (player.getWorld().getTime() % MyersConstants.CHARGE_SYNC_INTERVAL_TICKS == 0) sync();
        }

        /*
         * 客户端松开右键时，原版服务端会先结束本次 use 回调；
         * 强制冲刺阶段必须在后续 tick 重新压回使用状态，否则第三人称和服务端状态会立即放下屠刀。
         */
        if (usable && absorbing) {
            absorbTicker++;
            updateAbsorbTargets();
            if (absorbTicker >= MyersConstants.ABSORB_INTERVAL_TICKS) {
                absorbTicker = 0;
                int amount = absorbTargetCount * (player.isSneaking()
                        ? MyersConstants.ABSORB_SNEAKING_MALICE_PER_TARGET
                        : MyersConstants.ABSORB_MALICE_PER_TARGET);
                if (amount > 0) {
                    PlayerShopComponent.KEY.get(player).addCurrencyAmount(MyersConstants.MALICE_CURRENCY_ID, amount);
                }
            }
        } else if (absorbing) {
            stopAbsorbing();
        } else if (usable && player.getWorld().getTime() % MyersConstants.ABSORB_TARGET_SYNC_INTERVAL_TICKS == 0) {
            // 未按住 G 时也刷新目标数量，保证“按下前”HUD 不是固定 0。
            updateAbsorbTargets();
        }

        if (dashing) {
            tickDash();
        }
    }

    /** 客户端维持释放后屠刀举起动作，避免本地释放包先清掉第一人称使用状态。 */
    @Override
    public void clientTick() {
    }

    public void startAbsorbing() {
        if (!GameWorldComponent.KEY.get(player.getWorld()).isRole(player, NoellesRoleRegistry.MYERS)) return;
        absorbing = true;
        absorbTicker = 0;
        updateAbsorbTargets();
        sync();
    }

    public void stopAbsorbing() {
        absorbing = false;
        absorbTicker = 0;
        absorbTargetCount = 0;
        sync();
    }

    public void beginCharge() {
        if (dashing || player.getItemCooldownManager().isCoolingDown(ModItems.BUTCHER_KNIFE)
                && !GameFunctions.isPlayerSpectatingOrCreative(player)) return;
        charging = true;
        chargeTicks = 0;
        sync();
    }

    public void releaseCharge() {
        if (!charging || dashing) return;
        charging = false;
        int effective = Math.min(chargeTicks, MyersConstants.BUTCHER_KNIFE_MAX_CHARGE_TICKS);
        chargeTicks = 0;
        if (effective < MyersConstants.BUTCHER_KNIFE_MIN_CHARGE_TICKS) {
            forcedHold = false;
            sync();
            return;
        }

        Vec3d look = player.getRotationVector();
        dashDirection = new Vec3d(look.x, 0.0D, look.z).normalize();
        if (dashDirection.lengthSquared() < 0.001D) {
            dashDirection = Vec3d.fromPolar(0.0F, player.getYaw());
        }
        PlayerEntity targetAtRelease = MyersTargeting.findTarget(player, MyersConstants.BUTCHER_KNIFE_TARGET_RANGE);
        releaseTarget = targetAtRelease == null ? null : targetAtRelease.getUuid();
        dashTicksLeft = Math.max(1, (int) Math.ceil(effective * MyersConstants.BUTCHER_KNIFE_DASH_DURATION_MULTIPLIER));
        dashing = true;
        forcedHold = true;
        dashStartedInEvilPossess = PsychoModeApi.isActive(player, MyersPsychoHandler.PROFILE_ID);
        player.setSprinting(true);
        sync();
    }

    private void tickDash() {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        if (dashTicksLeft <= 0) {
            finishDash();
            return;
        }

        Vec3d step = dashDirection.multiply(MyersConstants.BUTCHER_KNIFE_DASH_STEP_BLOCKS);
        Vec3d next = player.getPos().add(step);
        // 只扩大水平 X/Z，不扩大到地面 Y 轴，否则每一步都会把脚下方块误判成碰撞。
        Box nextBox = player.getBoundingBox().offset(step).expand(
                MyersConstants.BUTCHER_KNIFE_COLLISION_EXPANSION,
                0.05D,
                MyersConstants.BUTCHER_KNIFE_COLLISION_EXPANSION
        );

        DoorBlockEntity door = findDoor(nextBox);
        if (door != null && !door.isBlasted()) {
            player.getWorld().playSound(null, door.getPos(), WatheSounds.ITEM_CROWBAR_PRY,
                    net.minecraft.sound.SoundCategory.BLOCKS, 2.5F, 1.0F);
            door.blast();
            GameRecordManager.recordGlobalEvent(serverPlayer.getServerWorld(), NoellesEventIds.MYERS_DOOR_BROKEN_EVENT, serverPlayer, null);
            finishDash();
            return;
        }

        for (PlayerEntity target : player.getWorld().getPlayers()) {
            if (target == player || !GameFunctions.isPlayerAliveAndSurvival(target)
                    || TimekeeperPlayerComponent.KEY.get(target).isInTimeRift()) continue;
            if (nextBox.intersects(target.getBoundingBox()) && TargetVisibilityApi.canAttackPlayer(player, target)) {
                executeButcherKnife(target);
                finishDash();
                return;
            }
        }

        Vec3d beforeMove = player.getPos();
        player.move(MovementType.SELF, step);
        double movedHorizontally = Math.sqrt(
                Math.pow(player.getX() - beforeMove.x, 2.0D)
                        + Math.pow(player.getZ() - beforeMove.z, 2.0D)
        );
        /* 让原版 move 负责台阶/楼梯的高度修正，只有水平位移明显不足才算撞墙。 */
        if (movedHorizontally < MyersConstants.BUTCHER_KNIFE_DASH_STEP_BLOCKS * 0.4D) {
            finishDash();
            return;
        }
        dashTicksLeft--;
        if (dashTicksLeft <= 0) {
            if (releaseTarget != null && player.getWorld().getServer() != null) {
                PlayerEntity target = player.getWorld().getPlayers().stream()
                        .filter(candidate -> candidate.getUuid().equals(releaseTarget))
                        .findFirst().orElse(null);
                if (target != null && GameFunctions.isPlayerAliveAndSurvival(target)
                        && !TimekeeperPlayerComponent.KEY.get(target).isInTimeRift()
                        && target.distanceTo(player) <= MyersConstants.BUTCHER_KNIFE_TARGET_RANGE
                        && TargetVisibilityApi.canAttackPlayer(player, target)) {
                    executeButcherKnife(target);
                }
            }
            finishDash();
        }
    }

    private void executeButcherKnife(PlayerEntity target) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        ItemStack stack = player.getMainHandStack();
        if (target instanceof ServerPlayerEntity serverTarget) {
            GameRecordManager.recordItemHit(serverPlayer, stack, GameConstants.DeathReasons.KNIFE, serverTarget, null);
        }
        GameFunctions.killPlayer(target, true, player, GameConstants.DeathReasons.KNIFE);
        if (GameFunctions.isPlayerAliveAndSurvival(target)) {
            GameFunctions.killPlayer(target, true, player, GameConstants.DeathReasons.KNIFE);
        }
        if (!GameFunctions.isPlayerAliveAndSurvival(target)) {
            serverPlayer.getWorld().playSound(
                    null,
                    target.getBlockPos(),
                    MyersPsychoHandler.randomHitSound(target.getRandom()),
                    net.minecraft.sound.SoundCategory.PLAYERS,
                    1.0F,
                    1.0F
            );
        }
    }

    private void finishDash() {
        boolean wasEvil = dashStartedInEvilPossess;
        dashing = false;
        forcedHold = false;
        dashTicksLeft = 0;
        releaseTarget = null;
        player.setSprinting(false);
        if (wasEvil) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,
                    MyersConstants.EVIL_POSSESS_DASH_SLOWNESS_TICKS,
                    MyersConstants.EVIL_POSSESS_DASH_SLOWNESS_AMPLIFIER, false, false));
        }
        if (!GameFunctions.isPlayerSpectatingOrCreative(player)) {
            player.getItemCooldownManager().set(ModItems.BUTCHER_KNIFE, MyersConstants.BUTCHER_KNIFE_COOLDOWN_TICKS);
        }
        player.stopUsingItem();
        sync();
    }

    private @Nullable DoorBlockEntity findDoor(Box box) {
        int minX = (int) Math.floor(box.minX), maxX = (int) Math.floor(box.maxX);
        int minY = (int) Math.floor(box.minY), maxY = (int) Math.floor(box.maxY);
        int minZ = (int) Math.floor(box.minZ), maxZ = (int) Math.floor(box.maxZ);
        for (int x = minX; x <= maxX; x++) for (int y = minY; y <= maxY; y++) for (int z = minZ; z <= maxZ; z++) {
            BlockEntity entity = player.getWorld().getBlockEntity(new BlockPos(x, y, z));
            if (entity instanceof DoorBlockEntity door) return door;
        }
        return null;
    }

    private void updateAbsorbTargets() {
        int previous = absorbTargetCount;
        absorbTargetCount = MyersTargeting.countAbsorbablePlayers(player);
        targetSyncTicker++;
        if (previous != absorbTargetCount || targetSyncTicker >= MyersConstants.ABSORB_TARGET_SYNC_INTERVAL_TICKS) {
            targetSyncTicker = 0;
            sync();
        }
    }

    private void clearRuntime(boolean clearCurrency) {
        absorbing = false;
        charging = false;
        forcedHold = false;
        dashing = false;
        chargeTicks = 0;
        dashTicksLeft = 0;
        absorbTargetCount = 0;
        releaseTarget = null;
        if (clearCurrency) PlayerShopComponent.KEY.get(player).setCurrencyAmount(MyersConstants.MALICE_CURRENCY_ID, 0);
        sync();
    }

    public void reset() {
        passiveTicker = 0;
        absorbTicker = 0;
        targetSyncTicker = 0;
        initialParticipantCount = 1;
        psychoWasActive = false;
        clearRuntime(true);
        player.getItemCooldownManager().remove(ModItems.BUTCHER_KNIFE);
        player.getItemCooldownManager().remove(ModItems.EVIL_POSSESS);
    }

    /** 时停者回溯恢复组件后，重新压回服务端动作状态，避免只恢复 NBT 而丢失举刀表现。 */
    public void reconcileAfterSnapshotRestore() {
        if (dashing) {
            player.setSprinting(true);
        }
        if (forcedHold && player.getMainHandStack().isOf(ModItems.BUTCHER_KNIFE)) {
            player.setCurrentHand(net.minecraft.util.Hand.MAIN_HAND);
        }
        sync();
    }

    public void sync() { KEY.sync(player); }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) { return recipient.equals(player); }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup lookup) {
        tag.putInt("passiveTicker", passiveTicker);
        tag.putInt("absorbTicker", absorbTicker);
        tag.putInt("absorbTargetCount", absorbTargetCount);
        tag.putBoolean("absorbing", absorbing);
        tag.putInt("chargeTicks", chargeTicks);
        tag.putBoolean("charging", charging);
        tag.putBoolean("forcedHold", forcedHold);
        tag.putBoolean("dashing", dashing);
        tag.putInt("dashTicksLeft", dashTicksLeft);
        tag.putDouble("dashX", dashDirection.x);
        tag.putDouble("dashY", dashDirection.y);
        tag.putDouble("dashZ", dashDirection.z);
        if (releaseTarget != null) tag.putUuid("releaseTarget", releaseTarget);
        tag.putBoolean("dashStartedInEvilPossess", dashStartedInEvilPossess);
        tag.putInt("initialParticipantCount", initialParticipantCount);
        tag.putBoolean("psychoWasActive", psychoWasActive);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup lookup) {
        passiveTicker = tag.getInt("passiveTicker");
        absorbTicker = tag.getInt("absorbTicker");
        absorbTargetCount = tag.getInt("absorbTargetCount");
        absorbing = tag.getBoolean("absorbing");
        chargeTicks = tag.getInt("chargeTicks");
        charging = tag.getBoolean("charging");
        forcedHold = tag.getBoolean("forcedHold");
        dashing = tag.getBoolean("dashing");
        dashTicksLeft = tag.getInt("dashTicksLeft");
        dashDirection = new Vec3d(tag.getDouble("dashX"), tag.getDouble("dashY"), tag.getDouble("dashZ"));
        releaseTarget = tag.containsUuid("releaseTarget") ? tag.getUuid("releaseTarget") : null;
        dashStartedInEvilPossess = tag.getBoolean("dashStartedInEvilPossess");
        initialParticipantCount = Math.max(1, tag.getInt("initialParticipantCount"));
        psychoWasActive = tag.getBoolean("psychoWasActive");
    }
}
