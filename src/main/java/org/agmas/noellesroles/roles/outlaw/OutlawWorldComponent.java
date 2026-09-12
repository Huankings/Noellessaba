package org.agmas.noellesroles.roles.outlaw;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** 亡命徒全场音乐、复活停电和离线死亡的世界级同步状态。 */
public final class OutlawWorldComponent implements AutoSyncedComponent {
    public static final ComponentKey<OutlawWorldComponent> KEY = ComponentRegistry.getOrCreate(
            NoellesRolesCore.id("outlaw_world"),
            OutlawWorldComponent.class
    );

    private final World world;
    private final Set<UUID> activeOutlaws = new HashSet<>();
    private boolean revivalBlackoutPending;
    private boolean revivalBlackoutActive;

    public OutlawWorldComponent(World world) {
        this.world = world;
    }

    public void reset() {
        this.activeOutlaws.clear();
        this.revivalBlackoutPending = false;
        this.revivalBlackoutActive = false;
        sync();
    }

    public boolean hasActiveOutlaw() {
        return !this.activeOutlaws.isEmpty();
    }

    public Set<UUID> getActiveOutlaws() {
        return Set.copyOf(this.activeOutlaws);
    }

    public void setOutlawActive(@NotNull UUID uuid, boolean active) {
        if (active) {
            this.activeOutlaws.add(uuid);
        } else {
            this.activeOutlaws.remove(uuid);
        }
        sync();
    }

    public boolean isRevivalBlackoutPending() {
        return this.revivalBlackoutPending;
    }

    public void setRevivalBlackoutPending(boolean pending) {
        this.revivalBlackoutPending = pending;
        sync();
    }

    public boolean isRevivalBlackoutActive() {
        return this.revivalBlackoutActive;
    }

    public void setRevivalBlackoutActive(boolean active) {
        this.revivalBlackoutActive = active;
        sync();
    }

    public void sync() {
        KEY.sync(this.world);
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList list = new NbtList();
        for (UUID uuid : this.activeOutlaws) {
            NbtCompound entry = new NbtCompound();
            entry.putUuid("uuid", uuid);
            list.add(entry);
        }
        tag.put("active_outlaws", list);
        tag.putBoolean("revival_blackout_pending", this.revivalBlackoutPending);
        tag.putBoolean("revival_blackout_active", this.revivalBlackoutActive);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.activeOutlaws.clear();
        for (NbtElement element : tag.getList("active_outlaws", NbtElement.COMPOUND_TYPE)) {
            if (element instanceof NbtCompound entry && entry.containsUuid("uuid")) {
                this.activeOutlaws.add(entry.getUuid("uuid"));
            }
        }
        this.revivalBlackoutPending = tag.getBoolean("revival_blackout_pending");
        this.revivalBlackoutActive = tag.getBoolean("revival_blackout_active");
    }
}
