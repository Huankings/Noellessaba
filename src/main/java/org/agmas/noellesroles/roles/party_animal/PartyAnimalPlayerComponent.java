package org.agmas.noellesroles.roles.party_animal;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.api.timekeeper.TimekeeperSnapshotApi;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.UUID;

/** 派对狂本人使用的标记、变声类型和标记倒计时组件。 */
public final class PartyAnimalPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<PartyAnimalPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            NoellesRolesCore.id("party_animal"), PartyAnimalPlayerComponent.class
    );

    /** 升调类型。 */
    public static final int PITCH_UP = 1;
    /** 降调类型。 */
    public static final int PITCH_DOWN = -1;

    private final PlayerEntity player;
    private int selectedType = PITCH_UP;
    private UUID markedTarget;
    private String markedTargetName;
    private int markTicksRemaining;
    private boolean markExpiredThisTick;

    public PartyAnimalPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public int getSelectedType() {
        return selectedType == PITCH_DOWN ? PITCH_DOWN : PITCH_UP;
    }

    public void setSelectedType(int type) {
        selectedType = type < 0 ? PITCH_DOWN : PITCH_UP;
        sync();
    }

    public boolean hasMarkedTarget() {
        return markedTarget != null && markTicksRemaining > 0;
    }

    public UUID getMarkedTarget() {
        return markedTarget;
    }

    public String getMarkedTargetName() {
        return markedTargetName == null ? "" : markedTargetName;
    }

    public int getMarkTicksRemaining() {
        return markTicksRemaining;
    }

    public void mark(UUID target, String name) {
        markedTarget = target;
        markedTargetName = name;
        markTicksRemaining = PartyAnimalConstants.MARK_DURATION_TICKS;
        sync();
    }

    public void clearMark() {
        markedTarget = null;
        markedTargetName = null;
        markTicksRemaining = 0;
        markExpiredThisTick = false;
        sync();
    }

    /** 回合清理只清除临时标记，默认选择仍回到升调。 */
    public void resetRound() {
        selectedType = PITCH_UP;
        markedTarget = null;
        markedTargetName = null;
        markTicksRemaining = 0;
        markExpiredThisTick = false;
        sync();
    }

    /** 读取并消费本 tick 的标记超时事件。 */
    public boolean consumeMarkExpiredThisTick() {
        boolean expired = markExpiredThisTick;
        markExpiredThisTick = false;
        return expired;
    }

    private void sync() {
        KEY.sync(player);
    }

    @Override
    public void serverTick() {
        /*
         * 时停回溯期间每个历史帧都会重新写回本组件。
         * 如果这里仍按正常时间线递减标记，回溯播放就会额外消耗标记时间，
         * 甚至在历史帧恢复后重新制造一次失败冷却；因此回溯期间只接受快照写回。
         */
        if (!(player.getWorld() instanceof ServerWorld serverWorld)
                || TimekeeperSnapshotApi.isRewinding(serverWorld)) {
            return;
        }
        if (markTicksRemaining <= 0) return;
        markTicksRemaining--;
        if (markTicksRemaining <= 0) {
            markedTarget = null;
            markedTargetName = null;
            markExpiredThisTick = true;
        }
        if (markTicksRemaining % 20 == 0 || markTicksRemaining <= 0) sync();
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        return recipient == player;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeByte(selectedType);
        buf.writeVarInt(markTicksRemaining);
        buf.writeBoolean(markedTarget != null);
        if (markedTarget != null) {
            buf.writeUuid(markedTarget);
            buf.writeString(getMarkedTargetName());
        }
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        selectedType = buf.readByte() < 0 ? PITCH_DOWN : PITCH_UP;
        markTicksRemaining = buf.readVarInt();
        if (buf.readBoolean()) {
            markedTarget = buf.readUuid();
            markedTargetName = buf.readString();
        } else {
            markedTarget = null;
            markedTargetName = null;
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup lookup) {
        tag.putByte("selectedType", (byte) selectedType);
        tag.putInt("markTicks", markTicksRemaining);
        if (markedTarget != null) tag.putUuid("markedTarget", markedTarget);
        if (markedTargetName != null) tag.putString("markedTargetName", markedTargetName);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup lookup) {
        selectedType = tag.getByte("selectedType") < 0 ? PITCH_DOWN : PITCH_UP;
        markTicksRemaining = tag.getInt("markTicks");
        /* 一次性超时闩不是历史状态；恢复快照时必须清除，避免重复写入失败冷却。 */
        markExpiredThisTick = false;
        markedTarget = tag.containsUuid("markedTarget") ? tag.getUuid("markedTarget") : null;
        markedTargetName = tag.contains("markedTargetName") ? tag.getString("markedTargetName") : null;
    }
}
