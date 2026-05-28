package org.agmas.noellesroles.framing;

import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.api.event.DelusionEvents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.UUID;

/**
 * 幻觉试剂的独立玩家组件。
 *
 * <p>它故意不复用 wathe 的 PlayerPoisonComponent：
 * 1. 不会死亡；
 * 2. 不会被当成真实中毒去触发毒药收益 / 解毒 / 旧 mixin；
 * 3. 仍然保留“心跳声 + 视角脉冲 + 开始 / 结束回放事件”的假中毒表现。</p>
 */
public class DelusionPlayerComponent implements AutoSyncedComponent, ServerTickingComponent, ClientTickingComponent {
    public static final ComponentKey<DelusionPlayerComponent> KEY = ComponentRegistry.getOrCreate(Identifier.of(Noellesroles.MOD_ID, "delusion"), DelusionPlayerComponent.class);
    private final PlayerEntity player;
    public int delusionTicks = -1;
    private int initialDelusionTicks = 0;
    private int pulseCooldown = 0;
    public float pulseProgress = 0f;
    public boolean pulsing = false;
    private @Nullable UUID applierUuid;

    public DelusionPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    public boolean isActive() {
        return this.delusionTicks > 0;
    }

    public void reset() {
        this.delusionTicks = -1;
        this.initialDelusionTicks = 0;
        this.pulseCooldown = 0;
        this.pulseProgress = 0f;
        this.pulsing = false;
        this.applierUuid = null;
        this.sync();
    }

    public void startDelusion(ServerPlayerEntity player, @Nullable UUID applierUuid) {
        int newTicks = dev.doctor4t.wathe.cca.PlayerPoisonComponent.clampTime.getLeft()
                + player.getWorld().getRandom().nextInt(dev.doctor4t.wathe.cca.PlayerPoisonComponent.clampTime.getRight() - dev.doctor4t.wathe.cca.PlayerPoisonComponent.clampTime.getLeft());
        boolean wasActive = this.isActive();

        this.delusionTicks = wasActive ? Math.max(this.delusionTicks, newTicks) : newTicks;
        this.initialDelusionTicks = this.delusionTicks;
        this.applierUuid = applierUuid;
        this.sync();

        if (!wasActive) {
            NbtCompound extra = new NbtCompound();
            extra.putUuid("victim", player.getUuid());
            if (applierUuid != null) {
                extra.putUuid("applier", applierUuid);
            }
            GameRecordManager.recordGlobalEvent(player.getServerWorld(), Noellesroles.DELUSION_STARTED_EVENT, null, extra);
            DelusionEvents.STARTED.invoker().onStarted(player, applierUuid);
        }
    }

    @Override
    public void clientTick() {
        if (this.delusionTicks > -1) {
            this.delusionTicks--;
        }
        if (this.delusionTicks > 0) {
            int ticksSinceStart = this.initialDelusionTicks - this.delusionTicks;
            if (ticksSinceStart < 200) {
                return;
            }

            int minCooldown = 10;
            int maxCooldown = 60;
            int dynamicCooldown = minCooldown + (int) ((maxCooldown - minCooldown) * ((float) this.delusionTicks / dev.doctor4t.wathe.cca.PlayerPoisonComponent.clampTime.getRight()));

            if (this.pulseCooldown <= 0) {
                this.pulseCooldown = dynamicCooldown;
                this.pulsing = true;

                float minVolume = 0.5f;
                float maxVolume = 1f;
                float volume = minVolume + (maxVolume - minVolume) * (1f - ((float) this.delusionTicks / dev.doctor4t.wathe.cca.PlayerPoisonComponent.clampTime.getRight()));
                this.player.playSoundToPlayer(SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.PLAYERS, volume, 1f);
            } else {
                this.pulseCooldown--;
            }
        } else {
            this.pulseCooldown = 0;
        }
    }

    @Override
    public void serverTick() {
        if (!GameFunctions.isPlayerAliveAndSurvival(this.player)) {
            if (this.delusionTicks > 0) {
                this.reset();
            }
            return;
        }

        if (this.delusionTicks > -1) {
            this.delusionTicks--;
            if (this.delusionTicks == 0) {
                if (this.player instanceof ServerPlayerEntity serverPlayer) {
                    NbtCompound extra = new NbtCompound();
                    extra.putUuid("victim", serverPlayer.getUuid());
                    if (this.applierUuid != null) {
                        extra.putUuid("applier", this.applierUuid);
                    }
                    GameRecordManager.recordGlobalEvent(serverPlayer.getServerWorld(), Noellesroles.DELUSION_ENDED_EVENT, null, extra);
                    DelusionEvents.ENDED.invoker().onEnded(serverPlayer);
                }
                this.reset();
            }
        }
    }

    public static float getFovMultiplier(float tickDelta, DelusionPlayerComponent component) {
        if (!component.pulsing) {
            return 1f;
        }

        component.pulseProgress += tickDelta * 0.1f;
        if (component.pulseProgress >= 1f) {
            component.pulsing = false;
            component.pulseProgress = 0f;
            return 1f;
        }

        float maxAmplitude = 0.1f;
        float minAmplitude = 0.025f;
        float amplitude = minAmplitude + (maxAmplitude - minAmplitude) * (1f - ((float) component.delusionTicks / 1200f));

        if (component.pulseProgress < 0.25f) {
            return 1f - amplitude * (float) Math.sin(Math.PI * (component.pulseProgress / 0.25f));
        } else if (component.pulseProgress < 0.5f) {
            return 1f - amplitude * (float) Math.sin(Math.PI * ((component.pulseProgress - 0.25f) / 0.25f));
        }
        return 1f;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("delusionTicks", this.delusionTicks);
        tag.putInt("initialDelusionTicks", this.initialDelusionTicks);
        if (this.applierUuid != null) {
            tag.putUuid("applierUuid", this.applierUuid);
        }
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.delusionTicks = tag.contains("delusionTicks") ? tag.getInt("delusionTicks") : -1;
        this.initialDelusionTicks = tag.contains("initialDelusionTicks") ? tag.getInt("initialDelusionTicks") : 0;
        this.applierUuid = tag.containsUuid("applierUuid") ? tag.getUuid("applierUuid") : null;
    }
}
