package org.agmas.noellesroles.roles.convener;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.UUID;

/**
 * 任意玩家当前是否处于召集者伪装状态。
 *
 * <p>这个组件挂在所有玩家身上，而不是只挂召集者身上：
 * 召集成功后，普通活人也会被限时套上尸体原主的外观。</p>
 */
public class ConvenerDisguiseComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<ConvenerDisguiseComponent> KEY =
            ComponentRegistry.getOrCreate(Identifier.of(Noellesroles.MOD_ID, "convener_disguise"), ConvenerDisguiseComponent.class);

    private final PlayerEntity player;
    @Nullable
    private UUID disguiseUuid;
    private int morphTicks;
    private int syncDelay;

    public ConvenerDisguiseComponent(PlayerEntity player) {
        this.player = player;
    }

    public boolean isDisguised() {
        return this.disguiseUuid != null && this.morphTicks != 0;
    }

    public @Nullable UUID getDisguiseUuid() {
        return this.disguiseUuid;
    }

    public int getMorphTicks() {
        return this.morphTicks;
    }

    public void setTimedDisguise(UUID disguiseUuid, int ticks) {
        this.disguiseUuid = disguiseUuid;
        this.morphTicks = Math.max(1, ticks);
        this.syncDelay = 0;
        this.sync();
    }

    public void setPersistentDisguise(UUID disguiseUuid) {
        this.disguiseUuid = disguiseUuid;
        this.morphTicks = -1;
        this.syncDelay = 0;
        this.sync();
    }

    public void clearDisguise() {
        this.disguiseUuid = null;
        this.morphTicks = 0;
        this.syncDelay = 0;
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public void serverTick() {
        if (!this.isDisguised()) {
            return;
        }

        /*
         * 玩家死亡或进入旁观/创造后立即清掉伪装。
         * 否则残局观察视角可能继续保留生前那张假脸，影响复盘判断。
         */
        if (!GameFunctions.isPlayerAliveAndSurvival(this.player)) {
            this.clearDisguise();
            return;
        }

        if (this.morphTicks > 0) {
            this.morphTicks--;
            if (this.morphTicks == 0) {
                this.clearDisguise();
                return;
            }

            this.syncDelay++;
            if (this.syncDelay >= 5) {
                this.syncDelay = 0;
                this.sync();
            }
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("morph_ticks", this.morphTicks);
        if (this.disguiseUuid != null) {
            tag.putUuid("disguise_uuid", this.disguiseUuid);
        }
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.morphTicks = tag.contains("morph_ticks") ? tag.getInt("morph_ticks") : 0;
        this.disguiseUuid = tag.containsUuid("disguise_uuid") ? tag.getUuid("disguise_uuid") : null;
        if (this.morphTicks == 0) {
            this.disguiseUuid = null;
        }
    }
}
