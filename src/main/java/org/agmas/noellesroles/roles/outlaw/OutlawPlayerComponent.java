package org.agmas.noellesroles.roles.outlaw;

import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.UUID;

/**
 * 平民转亡命徒流程的玩家级时间线状态。
 *
 * <p>这里同时保存“死亡后等待复活”和“复活后的亡命时刻”，是为了让时停者快照能够
 * 在两种状态之间完整来回恢复，而不会出现角色表已经变成亡命徒、倒计时却仍停在平民死亡前的错位。</p>
 */
public final class OutlawPlayerComponent implements AutoSyncedComponent {
    public static final ComponentKey<OutlawPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            NoellesRolesCore.id("outlaw_player"),
            OutlawPlayerComponent.class
    );

    private static final String REVIVAL_PENDING_KEY = "revival_pending";
    private static final String REVIVAL_TICKS_KEY = "revival_ticks";
    private static final String REVIVAL_X_KEY = "revival_x";
    private static final String REVIVAL_Y_KEY = "revival_y";
    private static final String REVIVAL_Z_KEY = "revival_z";
    private static final String REVIVAL_KILLER_KEY = "revival_killer";
    private static final String REVIVAL_PUSHED_KEY = "revival_pushed";
    private static final String OUTLAW_ACTIVE_KEY = "outlaw_active";
    private static final String OUTLAW_TICKS_KEY = "outlaw_ticks";
    private static final String KILL_COUNT_KEY = "kill_count";
    private static final String KILLS_TO_SHIELD_KEY = "kills_to_shield";
    private static final String SHIELD_LAYERS_KEY = "shield_layers";
    private static final String SLOWNESS_AMPLIFIER_KEY = "slowness_amplifier";
    private static final String SLOWNESS_TICKS_KEY = "slowness_ticks";

    private final PlayerEntity player;
    private boolean revivalPending;
    private int revivalTicksLeft;
    private Vec3d revivalPosition = Vec3d.ZERO;
    private UUID revivalKillerUuid;
    private boolean revivalWasPushedOut;
    private boolean outlawActive;
    private int outlawTicksLeft;
    private int killCount;
    private int killsTowardsShield;
    private int shieldLayers;
    private int slownessAmplifier = -1;
    private int slownessTicksLeft;

    public OutlawPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        this.revivalPending = false;
        this.revivalTicksLeft = 0;
        this.revivalPosition = Vec3d.ZERO;
        this.revivalKillerUuid = null;
        this.revivalWasPushedOut = false;
        this.outlawActive = false;
        this.outlawTicksLeft = 0;
        this.killCount = 0;
        this.killsTowardsShield = 0;
        this.shieldLayers = 0;
        this.slownessAmplifier = -1;
        this.slownessTicksLeft = 0;
        sync();
    }

    public void startRevival(@NotNull Vec3d position, @Nullable UUID killerUuid, boolean pushedOut) {
        this.revivalPending = true;
        this.revivalTicksLeft = CommonerBridge.revivalDelayTicks();
        this.revivalPosition = position;
        this.revivalKillerUuid = killerUuid;
        this.revivalWasPushedOut = pushedOut;
        this.outlawActive = false;
        sync();
    }

    public boolean isRevivalPending() {
        return this.revivalPending;
    }

    public int getRevivalTicksLeft() {
        return this.revivalTicksLeft;
    }

    public void setRevivalTicksLeft(int ticks) {
        this.revivalTicksLeft = Math.max(0, ticks);
        sync();
    }

    public void tickRevival() {
        if (this.revivalTicksLeft > 0) {
            this.revivalTicksLeft--;
            sync();
        }
    }

    public @NotNull Vec3d getRevivalPosition() {
        return this.revivalPosition;
    }

    public @Nullable UUID getRevivalKillerUuid() {
        return this.revivalKillerUuid;
    }

    public boolean wasRevivalPushedOut() {
        return this.revivalWasPushedOut;
    }

    public void clearRevival() {
        this.revivalPending = false;
        this.revivalTicksLeft = 0;
        this.revivalPosition = Vec3d.ZERO;
        this.revivalKillerUuid = null;
        this.revivalWasPushedOut = false;
        sync();
    }

    public void startOutlaw(int shieldLayers) {
        this.revivalPending = false;
        this.revivalTicksLeft = 0;
        this.outlawActive = true;
        this.outlawTicksLeft = OutlawConstants.OUTLAW_TIME_TICKS;
        this.killCount = 0;
        this.killsTowardsShield = 0;
        this.shieldLayers = Math.max(0, shieldLayers);
        this.slownessAmplifier = OutlawConstants.INITIAL_SLOWNESS_AMPLIFIER;
        this.slownessTicksLeft = OutlawConstants.SLOWNESS_LEVEL_DURATION_TICKS;
        sync();
    }

    public boolean isOutlawActive() {
        return this.outlawActive;
    }

    public int getOutlawTicksLeft() {
        return this.outlawTicksLeft;
    }

    public void addOutlawTime(int ticks) {
        this.outlawTicksLeft = Math.max(0, this.outlawTicksLeft + Math.max(0, ticks));
        sync();
    }

    public void tickOutlawTime() {
        if (this.outlawTicksLeft > 0) {
            this.outlawTicksLeft--;
            sync();
        }
    }

    public void finishOutlaw() {
        this.outlawActive = false;
        this.outlawTicksLeft = 0;
        this.slownessAmplifier = -1;
        this.slownessTicksLeft = 0;
        sync();
    }

    public int getKillCount() {
        return this.killCount;
    }

    public int incrementKillCount() {
        return ++this.killCount;
    }

    public int getKillsTowardsShield() {
        return this.killsTowardsShield;
    }

    public void incrementKillsTowardsShield() {
        this.killsTowardsShield++;
        sync();
    }

    public boolean consumeKillsForShield() {
        if (this.killsTowardsShield < OutlawConstants.KILLS_PER_SHIELD) {
            return false;
        }
        this.killsTowardsShield -= OutlawConstants.KILLS_PER_SHIELD;
        sync();
        return true;
    }

    public int getShieldLayers() {
        return this.shieldLayers;
    }

    public boolean consumeShield() {
        if (this.shieldLayers <= 0) {
            return false;
        }
        this.shieldLayers--;
        sync();
        return true;
    }

    public void addShieldLayer() {
        this.shieldLayers++;
        sync();
    }

    public int getSlownessAmplifier() {
        return this.slownessAmplifier;
    }

    public int getSlownessTicksLeft() {
        return this.slownessTicksLeft;
    }

    public void tickSlowness() {
        if (this.slownessAmplifier < 0) {
            return;
        }
        if (this.slownessTicksLeft > 0) {
            this.slownessTicksLeft--;
        }
        if (this.slownessTicksLeft <= 0) {
            this.slownessAmplifier--;
            this.slownessTicksLeft = this.slownessAmplifier >= 0
                    ? OutlawConstants.SLOWNESS_LEVEL_DURATION_TICKS
                    : 0;
        }
        sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        return this.player.equals(recipient);
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean(REVIVAL_PENDING_KEY, this.revivalPending);
        tag.putInt(REVIVAL_TICKS_KEY, this.revivalTicksLeft);
        tag.putDouble(REVIVAL_X_KEY, this.revivalPosition.x);
        tag.putDouble(REVIVAL_Y_KEY, this.revivalPosition.y);
        tag.putDouble(REVIVAL_Z_KEY, this.revivalPosition.z);
        if (this.revivalKillerUuid != null) {
            tag.putUuid(REVIVAL_KILLER_KEY, this.revivalKillerUuid);
        }
        tag.putBoolean(REVIVAL_PUSHED_KEY, this.revivalWasPushedOut);
        tag.putBoolean(OUTLAW_ACTIVE_KEY, this.outlawActive);
        tag.putInt(OUTLAW_TICKS_KEY, this.outlawTicksLeft);
        tag.putInt(KILL_COUNT_KEY, this.killCount);
        tag.putInt(KILLS_TO_SHIELD_KEY, this.killsTowardsShield);
        tag.putInt(SHIELD_LAYERS_KEY, this.shieldLayers);
        tag.putInt(SLOWNESS_AMPLIFIER_KEY, this.slownessAmplifier);
        tag.putInt(SLOWNESS_TICKS_KEY, this.slownessTicksLeft);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.revivalPending = tag.getBoolean(REVIVAL_PENDING_KEY);
        this.revivalTicksLeft = Math.max(0, tag.getInt(REVIVAL_TICKS_KEY));
        this.revivalPosition = new Vec3d(
                tag.getDouble(REVIVAL_X_KEY),
                tag.getDouble(REVIVAL_Y_KEY),
                tag.getDouble(REVIVAL_Z_KEY)
        );
        this.revivalKillerUuid = tag.containsUuid(REVIVAL_KILLER_KEY) ? tag.getUuid(REVIVAL_KILLER_KEY) : null;
        this.revivalWasPushedOut = tag.getBoolean(REVIVAL_PUSHED_KEY);
        this.outlawActive = tag.getBoolean(OUTLAW_ACTIVE_KEY);
        this.outlawTicksLeft = Math.max(0, tag.getInt(OUTLAW_TICKS_KEY));
        this.killCount = Math.max(0, tag.getInt(KILL_COUNT_KEY));
        this.killsTowardsShield = Math.max(0, tag.getInt(KILLS_TO_SHIELD_KEY));
        this.shieldLayers = Math.max(0, tag.getInt(SHIELD_LAYERS_KEY));
        this.slownessAmplifier = tag.contains(SLOWNESS_AMPLIFIER_KEY) ? tag.getInt(SLOWNESS_AMPLIFIER_KEY) : -1;
        this.slownessTicksLeft = Math.max(0, tag.getInt(SLOWNESS_TICKS_KEY));
    }

    /** 避免常量类之间形成反向依赖，只提供复活秒数到 tick 的最小桥接。 */
    private static final class CommonerBridge {
        private static int revivalDelayTicks() {
            return GameConstants.getInTicks(0, org.agmas.noellesroles.roles.commoner.CommonerConstants.REVIVAL_DELAY_SECONDS);
        }
    }
}
