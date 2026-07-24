package org.agmas.noellesroles.roles.arsonist;

import org.agmas.noellesroles.registry.NoellesRolesCore;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

/**
 * 记录玩家是否已经被纵火犯浇油。
 *
 * <p>这个状态需要同步到客户端，因为纵火犯本能会根据它把玩家染成
 * “已浇油/未浇油”两种颜色。</p>
 */
public class DousedPlayerComponent implements AutoSyncedComponent, ServerTickingComponent, ClientTickingComponent {
    public static final ComponentKey<DousedPlayerComponent> KEY =
            ComponentRegistry.getOrCreate(Identifier.of(NoellesRolesCore.MOD_ID, "doused"), DousedPlayerComponent.class);

    private final PlayerEntity player;
    private boolean doused;
    private int syncDelay;

    public DousedPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public boolean isDoused() {
        return this.doused;
    }

    public void setDoused(boolean doused) {
        this.doused = doused;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    public void reset() {
        this.doused = false;
        this.syncDelay = 0;
    }

    @Override
    public void clientTick() {
    }

    @Override
    public void serverTick() {
        this.syncDelay++;
        if (this.syncDelay >= 20) {
            this.syncDelay = 0;
            this.sync();
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("doused", this.doused);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.doused = tag.contains("doused") && tag.getBoolean("doused");
    }
}
