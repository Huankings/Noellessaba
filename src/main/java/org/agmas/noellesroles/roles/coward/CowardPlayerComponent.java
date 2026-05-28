package org.agmas.noellesroles.roles.coward;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.roles.angel.AngelPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

/**
 * 胆小鬼玩家组件。
 *
 * <p>服务端负责：
 * 1. 30 秒解锁倒计时；
 * 2. san 掉落倍率；
 * 3. “危险靠近 / 暂时离去” 文本与回放事件。</p>
 */
public class CowardPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<CowardPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "coward"),
            CowardPlayerComponent.class
    );

    private final PlayerEntity player;

    private int activationTicksRemaining = CowardConstants.SENSE_START_DELAY_TICKS;
    private float currentSanMultiplier = 1.0f;
    private boolean dangerActive = false;
    private boolean dangerSessionOpen = false;
    private int leaveGraceTicks = 0;

    public CowardPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    public void reset() {
        this.activationTicksRemaining = CowardConstants.SENSE_START_DELAY_TICKS;
        this.currentSanMultiplier = 1.0f;
        this.dangerActive = false;
        this.dangerSessionOpen = false;
        this.leaveGraceTicks = 0;
        this.sync();
    }

    public int getActivationTicksRemaining() {
        return this.activationTicksRemaining;
    }

    public boolean isSenseUnlocked() {
        return this.activationTicksRemaining <= 0;
    }

    public float getCurrentSanMultiplier() {
        return this.currentSanMultiplier;
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return this.player.equals(player);
    }

    @Override
    public void serverTick() {
        if (!(this.player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(serverPlayer.getWorld());
        if (!gameWorld.isRole(serverPlayer, Noellesroles.COWARD) || !gameWorld.isRunning() || !GameFunctions.isPlayerAliveAndSurvival(serverPlayer)) {
            if (this.currentSanMultiplier != 1.0f || this.dangerActive || this.dangerSessionOpen || this.leaveGraceTicks != 0) {
                this.currentSanMultiplier = 1.0f;
                this.dangerActive = false;
                this.dangerSessionOpen = false;
                this.leaveGraceTicks = 0;
            }
            if (this.activationTicksRemaining != CowardConstants.SENSE_START_DELAY_TICKS) {
                this.activationTicksRemaining = CowardConstants.SENSE_START_DELAY_TICKS;
                this.sync();
            }
            return;
        }

        if (this.activationTicksRemaining > 0) {
            this.activationTicksRemaining--;
            this.currentSanMultiplier = 1.0f;
            if (this.activationTicksRemaining == 0 || this.activationTicksRemaining % 20 == 0) {
                this.sync();
            }
            return;
        }

        CowardThreatSnapshot snapshot = CowardThreatSnapshot.collect(serverPlayer, gameWorld);
        boolean suppressed = SedativePlayerComponent.KEY.get(serverPlayer).isActive()
                || AngelPlayerComponent.KEY.get(serverPlayer).isSoothed();
        this.currentSanMultiplier = snapshot.sanMultiplier();
        if (suppressed) {
            this.dangerActive = false;
            this.dangerSessionOpen = false;
            this.leaveGraceTicks = 0;
            return;
        }

        boolean effectiveDanger = snapshot.hasEffectiveDanger();
        if (effectiveDanger) {
            this.dangerActive = true;
            this.leaveGraceTicks = 0;

            if (!this.dangerSessionOpen) {
                serverPlayer.sendMessage(
                        Text.translatable("message.noellesroles.coward.danger_nearby").withColor(Noellesroles.COWARD.color()),
                        true
                );
                GameRecordManager.recordGlobalEvent(serverPlayer.getServerWorld(), Noellesroles.COWARD_DANGER_SENSED_EVENT, serverPlayer, null);
                this.dangerSessionOpen = true;
            }
            return;
        }

        this.dangerActive = false;
        if (this.dangerSessionOpen) {
            this.leaveGraceTicks++;

            if (this.leaveGraceTicks >= CowardConstants.SENSE_LEAVE_GRACE_TICKS) {
                serverPlayer.sendMessage(
                        Text.translatable("message.noellesroles.coward.danger_gone").withColor(Noellesroles.COWARD.color()),
                        true
                );
                GameRecordManager.recordGlobalEvent(serverPlayer.getServerWorld(), Noellesroles.COWARD_DANGER_LEFT_EVENT, serverPlayer, null);
                this.dangerSessionOpen = false;
                this.leaveGraceTicks = 0;
            }
            return;
        }

        this.leaveGraceTicks = 0;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("activationTicksRemaining", this.activationTicksRemaining);
        tag.putFloat("currentSanMultiplier", this.currentSanMultiplier);
        tag.putBoolean("dangerActive", this.dangerActive);
        tag.putBoolean("dangerSessionOpen", this.dangerSessionOpen);
        tag.putInt("leaveGraceTicks", this.leaveGraceTicks);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.activationTicksRemaining = Math.max(0, tag.getInt("activationTicksRemaining"));
        this.currentSanMultiplier = tag.contains("currentSanMultiplier") ? tag.getFloat("currentSanMultiplier") : 1.0f;
        this.dangerActive = tag.getBoolean("dangerActive");
        this.dangerSessionOpen = tag.contains("dangerSessionOpen")
                ? tag.getBoolean("dangerSessionOpen")
                : tag.getBoolean("dangerActive");
        this.leaveGraceTicks = Math.max(0, tag.getInt("leaveGraceTicks"));
    }
}
