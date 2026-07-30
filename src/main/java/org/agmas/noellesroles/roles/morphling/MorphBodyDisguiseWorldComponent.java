package org.agmas.noellesroles.roles.morphling;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 记录本回合由变形试剂造成的尸体伪装。
 *
 * <p>当前 Wathe 已经有 {@code PlayerBodyEntity#appearanceUuid}，所以真正的尸体皮肤会直接写在尸体实体上；
 * 这个世界组件只保存“这具真实死者的尸体伪装来自变形试剂”这份来源信息。客户端需要它来区分：
 * 试剂尸体在杀手/旁观者本能下要显回原貌，而双重人格等其它尸体伪装不应该被一起改掉。</p>
 */
public final class MorphBodyDisguiseWorldComponent implements AutoSyncedComponent {
    public static final ComponentKey<MorphBodyDisguiseWorldComponent> KEY = ComponentRegistry.getOrCreate(
            NoellesRolesCore.id("morph_body_disguise_world"),
            MorphBodyDisguiseWorldComponent.class
    );

    private final World world;
    private final LinkedHashMap<UUID, BodyDisguise> bodyDisguises = new LinkedHashMap<>();

    public MorphBodyDisguiseWorldComponent(World world) {
        this.world = world;
    }

    public void recordBodyDisguise(@Nullable UUID bodyOwnerUuid, @Nullable UUID disguiseUuid, @Nullable String disguiseName) {
        if (bodyOwnerUuid == null || disguiseUuid == null) {
            return;
        }
        this.bodyDisguises.put(bodyOwnerUuid, new BodyDisguise(disguiseUuid, disguiseName == null ? "" : disguiseName));
        sync();
    }

    public @NotNull Optional<BodyDisguise> getDisguise(@Nullable UUID bodyOwnerUuid) {
        return Optional.ofNullable(this.bodyDisguises.get(bodyOwnerUuid));
    }

    public void clearBodyDisguise(@Nullable UUID bodyOwnerUuid) {
        if (bodyOwnerUuid == null || this.bodyDisguises.remove(bodyOwnerUuid) == null) {
            return;
        }
        sync();
    }

    public void reset() {
        if (this.bodyDisguises.isEmpty()) {
            return;
        }
        this.bodyDisguises.clear();
        sync();
    }

    public void sync() {
        KEY.sync(this.world);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return true;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeVarInt(this.bodyDisguises.size());
        for (Map.Entry<UUID, BodyDisguise> entry : this.bodyDisguises.entrySet()) {
            buf.writeUuid(entry.getKey());
            buf.writeUuid(entry.getValue().disguiseUuid());
            buf.writeString(entry.getValue().disguiseName());
        }
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.bodyDisguises.clear();
        int size = buf.readVarInt();
        for (int i = 0; i < size; i++) {
            UUID owner = buf.readUuid();
            UUID disguise = buf.readUuid();
            String name = buf.readString();
            this.bodyDisguises.put(owner, new BodyDisguise(disguise, name));
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        if (this.bodyDisguises.isEmpty()) {
            return;
        }

        NbtList records = new NbtList();
        for (Map.Entry<UUID, BodyDisguise> entry : this.bodyDisguises.entrySet()) {
            NbtCompound record = new NbtCompound();
            record.putUuid("BodyOwner", entry.getKey());
            record.putUuid("Disguise", entry.getValue().disguiseUuid());
            record.putString("DisguiseName", entry.getValue().disguiseName());
            records.add(record);
        }
        tag.put("BodyDisguises", records);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.bodyDisguises.clear();
        NbtList records = tag.getList("BodyDisguises", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < records.size(); i++) {
            NbtCompound record = records.getCompound(i);
            if (record.containsUuid("BodyOwner") && record.containsUuid("Disguise")) {
                this.bodyDisguises.put(
                        record.getUuid("BodyOwner"),
                        new BodyDisguise(record.getUuid("Disguise"), record.getString("DisguiseName"))
                );
            }
        }
    }

    public record BodyDisguise(@NotNull UUID disguiseUuid, @NotNull String disguiseName) {
    }
}
