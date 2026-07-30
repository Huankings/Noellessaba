package org.agmas.noellesroles.roles.morphling;

import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.UUID;

/**
 * 保存“某个玩家身上的变形试剂标记”。
 *
 * <p>标记挂在被标记玩家身上，而不是挂在变形怪身上。这样客户端渲染任意玩家时，
 * 可以直接读取该玩家自己的组件判断“我现在是否要显示成样本玩家”，不用遍历全服变形怪反查，
 * 也能自然支持一个变形怪同时遥控多个已标记目标。</p>
 *
 * <p>同步包会做可见性裁剪：待触发阶段只有标记者本人能看到，触发后才让所有客户端知道。
 * 但 {@link #shouldSyncWith(ServerPlayerEntity)} 仍允许向所有人发送空状态，因为激活结束时
 * 非标记者客户端也必须收到“清空伪装”的包，否则会残留上一帧的皮肤/名字缓存。</p>
 */
public class MorphMarkPlayerComponent implements AutoSyncedComponent, ServerTickingComponent, ClientTickingComponent {
    public static final Identifier ID = NoellesRolesCore.id("morph_mark_player");
    public static final ComponentKey<MorphMarkPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            ID,
            MorphMarkPlayerComponent.class
    );

    private final PlayerEntity player;
    private @Nullable UUID markerUuid;
    private @Nullable UUID sampleUuid;
    private String sampleName = "";
    private String markedName = "";
    private int activeTicks;

    public MorphMarkPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void setPending(@NotNull ServerPlayerEntity marker, @NotNull UUID sampleUuid, @NotNull String sampleName, @NotNull String markedName) {
        this.markerUuid = marker.getUuid();
        this.sampleUuid = sampleUuid;
        this.sampleName = sampleName;
        this.markedName = markedName;
        this.activeTicks = 0;
        sync();
    }

    public boolean activate() {
        if (this.markerUuid == null || this.sampleUuid == null) {
            return false;
        }
        this.activeTicks = MorphlingConstants.REAGENT_ACTIVE_DURATION_TICKS;
        sync();
        return true;
    }

    public void clear() {
        this.markerUuid = null;
        this.sampleUuid = null;
        this.sampleName = "";
        this.markedName = "";
        this.activeTicks = 0;
        sync();
    }

    public boolean hasMark() {
        return this.markerUuid != null && this.sampleUuid != null;
    }

    public boolean isPending() {
        return hasMark() && this.activeTicks <= 0;
    }

    public boolean isActive() {
        return hasMark() && this.activeTicks > 0;
    }

    public boolean isMarkedBy(@Nullable UUID uuid) {
        return uuid != null && uuid.equals(this.markerUuid) && hasMark();
    }

    public @Nullable UUID markerUuid() {
        return this.markerUuid;
    }

    public @Nullable UUID sampleUuid() {
        return this.sampleUuid;
    }

    public @NotNull String sampleName() {
        return this.sampleName;
    }

    public @NotNull String markedName() {
        return this.markedName;
    }

    public int activeTicks() {
        return this.activeTicks;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        return recipient != null;
    }

    @Override
    public void serverTick() {
        if (!isActive()) {
            return;
        }

        if (!(this.player instanceof ServerPlayerEntity serverPlayer)
                || !GameFunctions.isPlayerAliveAndSurvival(serverPlayer)) {
            clear();
            return;
        }

        this.activeTicks--;
        if (this.activeTicks <= 0) {
            recordNaturalEnd(serverPlayer);
            clear();
            return;
        }

        if (this.activeTicks % 20 == 0) {
            sync();
        }
    }

    @Override
    public void clientTick() {
        if (this.activeTicks > 0) {
            this.activeTicks--;
        }
    }

    private void recordNaturalEnd(@NotNull ServerPlayerEntity target) {
        if (target.getWorld() instanceof ServerWorld serverWorld) {
            GameRecordManager.recordGlobalEvent(
                    serverWorld,
                    NoellesEventIds.MORPH_MARK_ENDED_EVENT,
                    target,
                    null
            );
        }
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        boolean visibleToRecipient = isActive()
                || (recipient != null && this.markerUuid != null && recipient.getUuid().equals(this.markerUuid));
        boolean hasVisibleData = visibleToRecipient && this.markerUuid != null && this.sampleUuid != null;
        buf.writeBoolean(hasVisibleData);
        if (!hasVisibleData) {
            return;
        }

        buf.writeUuid(this.markerUuid);
        buf.writeUuid(this.sampleUuid);
        buf.writeString(this.sampleName);
        buf.writeString(this.markedName);
        buf.writeVarInt(this.activeTicks);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        if (!buf.readBoolean()) {
            this.markerUuid = null;
            this.sampleUuid = null;
            this.sampleName = "";
            this.markedName = "";
            this.activeTicks = 0;
            return;
        }

        this.markerUuid = buf.readUuid();
        this.sampleUuid = buf.readUuid();
        this.sampleName = buf.readString();
        this.markedName = buf.readString();
        this.activeTicks = buf.readVarInt();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        if (this.markerUuid != null) {
            tag.putUuid("Marker", this.markerUuid);
        }
        if (this.sampleUuid != null) {
            tag.putUuid("Sample", this.sampleUuid);
        }
        tag.putString("SampleName", this.sampleName);
        tag.putString("MarkedName", this.markedName);
        tag.putInt("ActiveTicks", this.activeTicks);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.markerUuid = tag.containsUuid("Marker") ? tag.getUuid("Marker") : null;
        this.sampleUuid = tag.containsUuid("Sample") ? tag.getUuid("Sample") : null;
        this.sampleName = tag.getString("SampleName");
        this.markedName = tag.getString("MarkedName");
        this.activeTicks = tag.getInt("ActiveTicks");
    }
}
