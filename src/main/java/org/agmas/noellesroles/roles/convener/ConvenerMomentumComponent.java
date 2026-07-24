package org.agmas.noellesroles.roles.convener;

import org.agmas.noellesroles.registry.NoellesRolesCore;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

/**
 * 召集者成功召集后的短时爆发移速状态。
 */
public class ConvenerMomentumComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<ConvenerMomentumComponent> KEY =
            ComponentRegistry.getOrCreate(Identifier.of(NoellesRolesCore.MOD_ID, "convener_momentum"), ConvenerMomentumComponent.class);

    private final PlayerEntity player;
    private int ticks;

    public ConvenerMomentumComponent(PlayerEntity player) {
        this.player = player;
    }

    public int getTicks() {
        return this.ticks;
    }

    public void activate() {
        this.ticks = ConvenerConstants.SUMMON_SPEED_DURATION_TICKS;
        this.sync();
    }

    public void reset() {
        this.ticks = 0;
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public void serverTick() {
        if (this.ticks <= 0) {
            return;
        }

        if (!GameFunctions.isPlayerAliveAndSurvival(this.player)) {
            this.reset();
            return;
        }

        this.ticks--;
        this.sync();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("ticks", this.ticks);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.ticks = tag.contains("ticks") ? Math.max(0, tag.getInt("ticks")) : 0;
    }
}
