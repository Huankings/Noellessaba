package org.agmas.noellesroles.roles.licensed_villain;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 执照恶棍时刻的世界级权威状态。
 *
 * <p>正常配置最多只会生成一名执照恶棍，但这里仍按 UUID 保存多名玩家，
 * 这样管理员强制职业测试时不会让其中一人的死亡错误关闭另一人的音乐。</p>
 *
 * <p>{@code active=false} 表示该玩家已经进入过时刻、但当前正为亡命徒或影子小丑让位。
 * 暂停和从未触发必须分开保存，才能在亡命徒死亡后恢复机制且不重复记录进入回放。</p>
 */
public final class LicensedVillainMomentWorldComponent implements AutoSyncedComponent {
    public static final ComponentKey<LicensedVillainMomentWorldComponent> KEY = ComponentRegistry.getOrCreate(
            NoellesRolesCore.id("licensed_villain_moment"),
            LicensedVillainMomentWorldComponent.class
    );

    private static final String MOMENTS_KEY = "moments";
    private static final String PLAYER_KEY = "player";
    private static final String ACTIVE_KEY = "active";

    private final World world;
    private final Map<UUID, Boolean> moments = new HashMap<>();

    public LicensedVillainMomentWorldComponent(World world) {
        this.world = world;
    }

    public boolean hasActiveMoment() {
        return this.moments.values().stream().anyMatch(Boolean.TRUE::equals);
    }

    public boolean hasStarted(UUID playerUuid) {
        return playerUuid != null && this.moments.containsKey(playerUuid);
    }

    public boolean isActive(UUID playerUuid) {
        return playerUuid != null && Boolean.TRUE.equals(this.moments.get(playerUuid));
    }

    public Set<UUID> players() {
        return Set.copyOf(this.moments.keySet());
    }

    public void setActive(@NotNull UUID playerUuid, boolean active) {
        Boolean previous = this.moments.put(playerUuid, active);
        if (previous == null || previous != active) {
            sync();
        }
    }

    public boolean remove(UUID playerUuid) {
        if (playerUuid == null || this.moments.remove(playerUuid) == null) {
            return false;
        }
        sync();
        return true;
    }

    public void reset() {
        if (this.moments.isEmpty()) {
            return;
        }
        this.moments.clear();
        sync();
    }

    public void sync() {
        KEY.sync(this.world);
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList list = new NbtList();
        for (Map.Entry<UUID, Boolean> entry : this.moments.entrySet()) {
            NbtCompound moment = new NbtCompound();
            moment.putUuid(PLAYER_KEY, entry.getKey());
            moment.putBoolean(ACTIVE_KEY, entry.getValue());
            list.add(moment);
        }
        tag.put(MOMENTS_KEY, list);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.moments.clear();
        for (NbtElement element : tag.getList(MOMENTS_KEY, NbtElement.COMPOUND_TYPE)) {
            if (element instanceof NbtCompound moment && moment.containsUuid(PLAYER_KEY)) {
                this.moments.put(moment.getUuid(PLAYER_KEY), moment.getBoolean(ACTIVE_KEY));
            }
        }
    }
}
