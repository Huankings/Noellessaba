package org.agmas.noellesroles.roles.killer_queen;

import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

/** 酸液造成的玩家个人计时状态。 */
public final class KillerQueenPlayerComponent implements AutoSyncedComponent, ServerTickingComponent, ClientTickingComponent {
    public static final ComponentKey<KillerQueenPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            NoellesRolesCore.id("killer_queen_player"), KillerQueenPlayerComponent.class);

    private final PlayerEntity player;
    private int speedTicks;
    private int dazeTicks;

    public KillerQueenPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void startSpeedBonus() {
        this.speedTicks = KillerQueenConstants.ACID_SPEED_DURATION_TICKS;
        sync();
    }

    public void startDaze() {
        this.dazeTicks = KillerQueenConstants.ACID_DAZE_DURATION_TICKS;
        sync();
    }

    public boolean hasSpeedBonus() {
        return this.speedTicks > 0;
    }

    public void reset() {
        this.speedTicks = 0;
        this.dazeTicks = 0;
        sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public void clientTick() {
        if (this.speedTicks > 0) this.speedTicks--;
        if (this.dazeTicks > 0) this.dazeTicks--;
    }

    @Override
    public void serverTick() {
        tickSpeed();
        tickDaze();
        if ((this.speedTicks > 0 && this.speedTicks % GameConstants.PLAYER_STATE_SYNC_INTERVAL_TICKS == 0)
                || (this.dazeTicks > 0 && this.dazeTicks % GameConstants.PLAYER_STATE_SYNC_INTERVAL_TICKS == 0)) {
            sync();
        }
    }

    private void tickSpeed() {
        if (this.speedTicks <= 0) return;
        this.speedTicks--;
        if (this.speedTicks == 0 && this.player instanceof ServerPlayerEntity serverPlayer) {
            GameRecordManager.recordGlobalEvent(serverPlayer.getServerWorld(), NoellesEventIds.KILLER_QUEEN_SPEED_END_EVENT, serverPlayer, null);
            sync();
        }
    }

    private void tickDaze() {
        if (this.dazeTicks <= 0) return;
        this.dazeTicks--;
        if (this.dazeTicks == 0 && this.player instanceof ServerPlayerEntity serverPlayer) {
            GameRecordManager.recordGlobalEvent(serverPlayer.getServerWorld(), NoellesEventIds.KILLER_QUEEN_DAZE_END_EVENT, serverPlayer, null);
            sync();
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("speed_ticks", this.speedTicks);
        tag.putInt("daze_ticks", this.dazeTicks);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.speedTicks = Math.max(0, tag.getInt("speed_ticks"));
        this.dazeTicks = Math.max(0, tag.getInt("daze_ticks"));
    }
}
