package org.agmas.noellesroles.roles.winder;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.UUID;

/**
 * 风灵师个人状态。
 *
 * <p>这里同时维护：
 * 1. 当前背包里“选中的玩家”是谁；
 * 2. 当前是否正在执行一段漂浮；
 * 3. 这段漂浮真正锁定的对象是谁。
 */
public class WinderPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<WinderPlayerComponent> KEY =
            ComponentRegistry.getOrCreate(Identifier.of(Noellesroles.MOD_ID, "winder"), WinderPlayerComponent.class);

    private final PlayerEntity player;

    public UUID selectedTarget;
    @Nullable
    public UUID activeTarget;
    public boolean floatingActive = false;
    public int floatingTicksRemaining = 0;
    public int floatingTicksUsed = 0;

    public WinderPlayerComponent(PlayerEntity player) {
        this.player = player;
        this.selectedTarget = player.getUuid();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    /**
     * 风灵师重置时回到“默认选中自己”的初始状态。
     */
    public void reset() {
        this.selectedTarget = this.player.getUuid();
        this.clearFloatingState();
        this.sync();
    }

    public void setSelectedTarget(@NotNull UUID targetUuid) {
        this.selectedTarget = targetUuid;
        this.sync();
    }

    public UUID getSelectedTarget() {
        return this.selectedTarget != null ? this.selectedTarget : this.player.getUuid();
    }

    public boolean isFloatingActive() {
        return this.floatingActive;
    }

    public int getFloatingTicksRemaining() {
        return this.floatingTicksRemaining;
    }

    /**
     * 开始一次新的漂浮。
     * 当前所选目标会在这里被锁定为 activeTarget，避免中途改选影响本次效果。
     */
    public void startFloating() {
        this.floatingActive = true;
        this.activeTarget = this.getSelectedTarget();
        this.floatingTicksRemaining = WinderConstants.FLOAT_DURATION_TICKS;
        this.floatingTicksUsed = 0;
        if (this.player instanceof ServerPlayerEntity serverPlayer) {
            NbtCompound extra = new NbtCompound();
            extra.putUuid("target_player", this.activeTarget);
            GameRecordManager.recordGlobalEvent(serverPlayer.getServerWorld(), Noellesroles.WINDER_FLOAT_STARTED_EVENT, serverPlayer, extra);
        }
        this.sync();
    }

    /**
     * 手动提前结束或自然结束后，按照“实际持续时间 * 倍率”结算冷却。
     */
    public void stopFloatingWithCooldown() {
        stopFloatingWithCooldown(false);
    }

    /**
     * 统一处理漂浮结束。
     *
     * <p>为了让回放能区分“自然结束”与“提前结束”，这里增加一个明确的入参。
     * 结束后再统一结算冷却，避免两套逻辑分叉。</p>
     */
    public void stopFloatingWithCooldown(boolean stoppedEarly) {
        if (!this.floatingActive) {
            return;
        }

        UUID targetUuid = this.activeTarget;
        int usedTicks = Math.max(this.floatingTicksUsed, 1);
        if (this.player instanceof ServerPlayerEntity serverPlayer && targetUuid != null) {
            NbtCompound extra = new NbtCompound();
            extra.putUuid("target_player", targetUuid);
            GameRecordManager.recordGlobalEvent(
                    serverPlayer.getServerWorld(),
                    stoppedEarly ? Noellesroles.WINDER_FLOAT_STOPPED_EARLY_EVENT : Noellesroles.WINDER_FLOAT_ENDED_EVENT,
                    stoppedEarly ? serverPlayer : null,
                    extra
            );
        }
        this.clearFloatingState();
        this.sync();

        // 风灵师的冷却仍然以“实际漂浮时长 * 倍率”为基础，
        // 但为了避免玩家通过极短时间反复开启/提前结束来刷出过短冷却，
        // 这里额外增加一个“最小冷却时间”下限。
        int dynamicCooldown = usedTicks * WinderConstants.FLOAT_COOLDOWN_MULTIPLIER;
        int finalCooldown = dynamicCooldown;

        // 当最小冷却常量小于等于 0 时，视为关闭该限制，不额外干预原有动态冷却。
        if (WinderConstants.FLOAT_MINIMUM_COOLDOWN_TICKS > 0) {
            finalCooldown = Math.max(dynamicCooldown, WinderConstants.FLOAT_MINIMUM_COOLDOWN_TICKS);
        }

        AbilityPlayerComponent.KEY.get(this.player).setCooldown(finalCooldown);
    }

    @Override
    public void serverTick() {
        if (!this.floatingActive) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(this.player.getWorld());
        if (!gameWorld.isRunning()
                || !gameWorld.isRole(this.player, Noellesroles.WINDER)
                || !GameFunctions.isPlayerAliveAndSurvival(this.player)) {
            // 风灵师本人一旦死亡、掉局或对局结束，就直接停掉本次效果，不再结算冷却。
            this.reset();
            return;
        }

        applyLevitationToActiveTarget();

        this.floatingTicksRemaining--;
        this.floatingTicksUsed++;
        this.sync();

        if (this.floatingTicksRemaining <= 0) {
            this.stopFloatingWithCooldown(false);
        }
    }

    /**
     * 每 tick 短时间刷新一次漂浮。
     *
     * <p>这么做是为了让“提前结束”可以自然失效，
     * 不需要粗暴 removeStatusEffect，从而避免误删别的来源给目标加上的更高等级漂浮。
     */
    private void applyLevitationToActiveTarget() {
        if (this.activeTarget == null) {
            return;
        }

        PlayerEntity target = this.player.getWorld().getPlayerByUuid(this.activeTarget);
        if (target == null || !GameFunctions.isPlayerAliveAndSurvival(target)) {
            return;
        }

        StatusEffectInstance currentLevitation = target.getStatusEffect(StatusEffects.LEVITATION);
        if (currentLevitation != null
                && currentLevitation.getAmplifier() > WinderConstants.FLOAT_LEVITATION_AMPLIFIER) {
            return;
        }

        target.addStatusEffect(new StatusEffectInstance(
                StatusEffects.LEVITATION,
                WinderConstants.FLOAT_EFFECT_REFRESH_TICKS,
                WinderConstants.FLOAT_LEVITATION_AMPLIFIER,
                true,
                false,
                true
        ));
    }

    private void clearFloatingState() {
        this.floatingActive = false;
        this.activeTarget = null;
        this.floatingTicksRemaining = 0;
        this.floatingTicksUsed = 0;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        if (this.selectedTarget != null) {
            tag.putUuid("selectedTarget", this.selectedTarget);
        }
        if (this.activeTarget != null) {
            tag.putUuid("activeTarget", this.activeTarget);
        }
        tag.putBoolean("floatingActive", this.floatingActive);
        tag.putInt("floatingTicksRemaining", this.floatingTicksRemaining);
        tag.putInt("floatingTicksUsed", this.floatingTicksUsed);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.selectedTarget = tag.contains("selectedTarget") ? tag.getUuid("selectedTarget") : this.player.getUuid();
        this.activeTarget = tag.contains("activeTarget") ? tag.getUuid("activeTarget") : null;
        this.floatingActive = tag.contains("floatingActive") && tag.getBoolean("floatingActive");
        this.floatingTicksRemaining = Math.max(0, tag.getInt("floatingTicksRemaining"));
        this.floatingTicksUsed = Math.max(0, tag.getInt("floatingTicksUsed"));
    }
}
