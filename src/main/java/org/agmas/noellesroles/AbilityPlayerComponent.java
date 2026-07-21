package org.agmas.noellesroles;

import dev.doctor4t.wathe.game.GameConstants;
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

import java.util.UUID;

public class AbilityPlayerComponent implements AutoSyncedComponent, ServerTickingComponent, ClientTickingComponent {
    public static final ComponentKey<AbilityPlayerComponent> KEY = ComponentRegistry.getOrCreate(Identifier.of(Noellesroles.MOD_ID, "ability"), AbilityPlayerComponent.class);
    private final PlayerEntity player;
    public int cooldown = 0;

    public void reset() {
        this.cooldown = 0;
        this.sync();
    }

    public AbilityPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    public void clientTick() {
    }

    public void serverTick() {
        if (this.cooldown > 0) {
            --this.cooldown;

            this.sync();
        }
    }

    public void setCooldown(int ticks) {
        this.cooldown = ticks;
        this.sync();
    }

    /**
     * 按增量调整能力冷却，并把结果压到 0 以上。
     *
     * <p>星界使者完成任务会减少当前剩余冷却。这里放在通用能力组件里，
     * 是因为 NoellesRoles 已经让多个职业共用同一个能力冷却栏；
     * 由组件自己负责同步和下限钳制，可以避免每个职业各自手写一遍时出现负冷却。</p>
     */
    public void changeCooldown(int ticks) {
        this.cooldown = Math.max(0, this.cooldown + ticks);
        this.sync();
    }

    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("cooldown", this.cooldown);
    }

    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.cooldown = tag.contains("cooldown") ? tag.getInt("cooldown") : 0;
    }
}
