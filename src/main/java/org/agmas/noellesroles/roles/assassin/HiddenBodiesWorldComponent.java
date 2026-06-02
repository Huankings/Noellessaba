package org.agmas.noellesroles.roles.assassin;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 世界级隐藏尸体组件。
 *
 * <p>这里记录的是“尸体实体本身的 UUID”，而不是死者玩家 UUID。
 * 这样即便以后出现伪造尸体、同一玩家多次生成尸体等扩展场景，
 * 也不会把隐藏状态错误地串到别的实体上。</p>
 */
public class HiddenBodiesWorldComponent implements AutoSyncedComponent {

    public static final ComponentKey<HiddenBodiesWorldComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "hidden_bodies"),
            HiddenBodiesWorldComponent.class
    );

    private final World world;
    private final Set<UUID> hiddenBodies = new HashSet<>();

    public HiddenBodiesWorldComponent(World world) {
        this.world = world;
    }

    public void addHiddenBody(@NotNull UUID bodyUuid) {
        if (this.hiddenBodies.add(bodyUuid)) {
            KEY.sync(this.world);
        }
    }

    public boolean isHidden(@NotNull UUID bodyUuid) {
        return this.hiddenBodies.contains(bodyUuid);
    }

    public void reset() {
        if (!this.hiddenBodies.isEmpty()) {
            this.hiddenBodies.clear();
            KEY.sync(this.world);
        }
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeInt(this.hiddenBodies.size());
        for (UUID bodyUuid : this.hiddenBodies) {
            buf.writeUuid(bodyUuid);
        }
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.hiddenBodies.clear();
        int count = buf.readInt();
        for (int i = 0; i < count; i++) {
            this.hiddenBodies.add(buf.readUuid());
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        NbtCompound hiddenBodiesTag = new NbtCompound();
        int index = 0;
        for (UUID bodyUuid : this.hiddenBodies) {
            hiddenBodiesTag.putUuid("body_" + index, bodyUuid);
            index++;
        }
        hiddenBodiesTag.putInt("count", this.hiddenBodies.size());
        tag.put("hiddenBodies", hiddenBodiesTag);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.hiddenBodies.clear();
        if (!tag.contains("hiddenBodies")) {
            return;
        }

        NbtCompound hiddenBodiesTag = tag.getCompound("hiddenBodies");
        int count = hiddenBodiesTag.getInt("count");
        for (int i = 0; i < count; i++) {
            if (hiddenBodiesTag.contains("body_" + i)) {
                this.hiddenBodies.add(hiddenBodiesTag.getUuid("body_" + i));
            }
        }
    }
}
