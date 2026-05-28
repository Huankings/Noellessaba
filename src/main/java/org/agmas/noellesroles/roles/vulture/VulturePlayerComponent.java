package org.agmas.noellesroles.roles.vulture;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

public class VulturePlayerComponent implements AutoSyncedComponent, ServerTickingComponent, ClientTickingComponent {
    public static final ComponentKey<VulturePlayerComponent> KEY = ComponentRegistry.getOrCreate(Identifier.of(Noellesroles.MOD_ID, "vulture"), VulturePlayerComponent.class);
    private final PlayerEntity player;
    public int bodiesEaten = 0;
    public int bodiesRequired = 0;


    public void reset() {
        this.bodiesEaten = 0;
        this.bodiesRequired = 0;
        this.sync();
    }

    public VulturePlayerComponent(PlayerEntity player) {
        this.player = player;
        bodiesEaten = 0;
        bodiesRequired = 0;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    public void clientTick() {
    }

    public void serverTick() {
        sync();
    }


    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("bodiesEaten", this.bodiesEaten);
        tag.putInt("bodiesRequired", this.bodiesRequired);
    }

    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.bodiesEaten = tag.contains("bodiesEaten") ? tag.getInt("bodiesEaten") : 0;
        this.bodiesRequired = tag.contains("bodiesRequired") ? tag.getInt("bodiesRequired") : 0;
    }
}
