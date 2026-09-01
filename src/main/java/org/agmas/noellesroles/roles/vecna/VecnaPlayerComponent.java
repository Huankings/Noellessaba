package org.agmas.noellesroles.roles.vecna;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import dev.doctor4t.wathe.record.GameRecordManager;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.UUID;

/**
 * 维克那标记状态。
 * 每个玩家都拥有本组件：被标记者保存施加者和剩余时间，维克那本人保存自己的开局状态。
 */
public final class VecnaPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<VecnaPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(NoellesRolesCore.MOD_ID, "vecna"), VecnaPlayerComponent.class);
    private final PlayerEntity player;
    /** 能力键施加的颠倒标记剩余时间。 */
    private int markTicks;
    /** 能力键颠倒标记的施加者。 */
    private UUID marker;
    /** 颠倒疯魔造成的全场逆转状态；与上面的能力标记完全独立。 */
    private boolean psychoInverted;

    public VecnaPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public boolean isMarked() { return markTicks > 0 && marker != null; }
    public UUID getMarker() { return marker; }
    public int getMarkTicks() { return markTicks; }
    public boolean isPsychoInverted() { return psychoInverted; }

    /** 只切换疯魔逆转状态，不修改能力标记目标。 */
    public void setPsychoInverted(boolean inverted) {
        if (psychoInverted == inverted) return;
        psychoInverted = inverted;
        sync();
    }

    public void applyMark(ServerPlayerEntity applier) {
        markTicks = VecnaConstants.MARK_DURATION_TICKS;
        marker = applier.getUuid();
        sync();
    }

    public void reset() {
        if (markTicks == 0 && marker == null && !psychoInverted) return;
        UUID oldMarker = marker;
        markTicks = 0;
        marker = null;
        psychoInverted = false;
        if (player instanceof ServerPlayerEntity serverPlayer && oldMarker != null) {
            NbtCompound extra = new NbtCompound();
            extra.putUuid("target_player", serverPlayer.getUuid());
            extra.putUuid("marker_player", oldMarker);
            GameRecordManager.recordGlobalEvent(serverPlayer.getServerWorld(), NoellesEventIds.VECNA_MARK_ENDED_EVENT, serverPlayer, extra);
        }
        sync();
    }

    @Override
    public void serverTick() {
        if (psychoInverted && !GameFunctions.isPlayerAliveAndSurvival(player)) {
            psychoInverted = false;
            sync();
        }
        if (!isMarked()) return;
        if (!GameFunctions.isPlayerAliveAndSurvival(player)) {
            reset();
            return;
        }
        markTicks--;
        if (markTicks <= 0) reset(); else sync();
    }

    public void sync() { KEY.sync(player); }

    @Override
    public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup lookup) {
        tag.putInt("markTicks", markTicks);
        if (marker != null) tag.putUuid("marker", marker);
        tag.putBoolean("psychoInverted", psychoInverted);
    }

    @Override
    public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup lookup) {
        markTicks = Math.max(0, tag.getInt("markTicks"));
        marker = tag.containsUuid("marker") ? tag.getUuid("marker") : null;
        psychoInverted = tag.getBoolean("psychoInverted");
    }
}
