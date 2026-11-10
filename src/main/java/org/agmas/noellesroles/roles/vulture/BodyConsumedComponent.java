package org.agmas.noellesroles.roles.vulture;

import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

/**
 * 秃鹫是否已经吞噬某具尸体。
 *
 * <p>组件 ID 继续沿用历史的 {@code noellesroles:body_death_reason}，保证旧存档和时停者尸体 NBT
 * 可以无损读取；类本身不再保存死因或职业，这些通用字段已经迁移到 Wathe PlayerBodyInfoApi。</p>
 */
public final class BodyConsumedComponent implements AutoSyncedComponent {
    public static final ComponentKey<BodyConsumedComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(NoellesRolesCore.MOD_ID, "body_death_reason"), BodyConsumedComponent.class);

    public boolean consumed;
    private final PlayerBodyEntity body;

    public BodyConsumedComponent(PlayerBodyEntity body) {
        this.body = body;
    }

    public void sync() {
        KEY.sync(body);
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("vultured", consumed);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        consumed = tag.getBoolean("vultured");
    }
}
