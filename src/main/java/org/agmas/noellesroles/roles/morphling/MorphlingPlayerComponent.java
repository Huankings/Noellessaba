package org.agmas.noellesroles.roles.morphling;

import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.record.GameRecordTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.UUID;

public class MorphlingPlayerComponent implements AutoSyncedComponent, ServerTickingComponent, ClientTickingComponent {
    public static final ComponentKey<MorphlingPlayerComponent> KEY = ComponentRegistry.getOrCreate(Identifier.of(Noellesroles.MOD_ID, "morphling"), MorphlingPlayerComponent.class);
    private final PlayerEntity player;
    public UUID disguise;
    public int morphTicks = 0;
    /**
     * 独立记录“当前是否真的处于一次变形流程中”。
     *
     * <p>不能只靠 morphTicks > 0 判断，
     * 因为自然结束时 morphTicks 会先减到 0，再进入 stopMorph(true)。
     * 如果结束事件也用 morphTicks > 0 作为前提，就会把这条正常结束漏掉。</p>
     */
    private boolean morphActive = false;

    public void reset() {
        this.clearMorphStateSilently();
        this.sync();
    }

    public MorphlingPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    public void clientTick() {
    }

    public void serverTick() {
        if (this.morphTicks > 0 && disguise != null) {
        //    if (player.getWorld().getPlayerByUuid(disguise) != null) {
        //       if (((ServerPlayerEntity)player.getWorld().getPlayerByUuid(disguise)).interactionManager.getGameMode() == GameMode.SPECTATOR) {
        //          stopMorph();
        //       }
        //   } else {
        //       stopMorph();
        //   }
            if (!player.isAlive() || player.isSpectator()) {
                this.stopMorph(true);
            }

            /*
             * 这里不能再写成“先 -- 再判断 == 0”后才去 stopMorph，
             * 因为 stopMorph(true) 内部会检查当前 morphTicks > 0 才记录结束事件。
             * 如果先减成 0，再调用 stopMorph，就会把这次正常结束误判成“无需记录”，
             * 从而导致你测试时完全看不到“变形时间结束”的回放。
             *
             * 因此这里改成：
             * 1. 先把 tick 减掉；
             * 2. 只要减完小于等于 0，就立刻按“真实结束”结算。
             */
            this.morphTicks--;
            if (this.morphTicks <= 0) {
                this.stopMorph(true);
            }

            this.sync();
        }
        if (this.morphTicks < 0) {
            this.morphTicks++;
            this.sync();
        }
    }

    public boolean startMorph(UUID id) {
        setMorphTicks(MorphlingConstants.MORPH_DURATION_TICKS);
        disguise = id;
        this.morphActive = true;
        if (this.player instanceof ServerPlayerEntity serverPlayer) {
            GameRecordManager.event(GameRecordTypes.GLOBAL_EVENT)
                    .actor(serverPlayer)
                    .put("event", Noellesroles.MORPHLING_MORPH_STARTED_EVENT.toString())
                    .putUuid("target_player", id)
                    .record();
        }
        this.sync();
        return true;
    }

    public void stopMorph() {
        this.stopMorph(true);
    }

    /**
     * 停止变形并进入冷却。
     *
     * <p>recordReplay 为 true 时，表示这是一次真实的局中结束；
     * false 则表示 reset / 开局清状态之类的静默清理，不应该播回放。</p>
     */
    public void stopMorph(boolean recordReplay) {
        if (recordReplay && this.morphActive && this.player instanceof ServerPlayerEntity serverPlayer) {
            GameRecordManager.recordGlobalEvent(
                    serverPlayer.getServerWorld(),
                    Noellesroles.MORPHLING_MORPH_ENDED_EVENT,
                    serverPlayer,
                    null
            );
        }
        this.morphActive = false;
        this.morphTicks = -MorphlingConstants.MORPH_COOLDOWN_TICKS;
        this.sync();
    }

    /**
     * 用于 reset 等场景的静默清理。
     */
    public void clearMorphStateSilently() {
        this.morphActive = false;
        this.morphTicks = 0;
        this.disguise = this.player.getUuid();
    }

    public int getMorphTicks() {
        return this.morphTicks;
    }

    public void setMorphTicks(int ticks) {
        this.morphTicks = ticks;
        this.sync();
    }

    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("morphActive", this.morphActive);
        tag.putInt("morphTicks", this.morphTicks);
        if (disguise == null) disguise = player.getUuid();
        tag.putUuid("disguise", this.disguise);
    }

    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.morphActive = tag.getBoolean("morphActive");
        this.morphTicks = tag.contains("morphTicks") ? tag.getInt("morphTicks") : 0;
        this.disguise = tag.contains("disguise") ? tag.getUuid("disguise") : player.getUuid();
    }
}
