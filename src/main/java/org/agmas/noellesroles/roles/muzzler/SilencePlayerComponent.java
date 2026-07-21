package org.agmas.noellesroles.roles.muzzler;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
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
 * 静语者胶带状态。
 *
 * <p>该状态挂在“被封嘴的玩家”身上，而不是挂在静语者身上。
 * 原因是语音拦截、室外窒息、准心提示、撕胶带次数都需要从受害者视角判断。</p>
 */
public class SilencePlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<SilencePlayerComponent> KEY =
            ComponentRegistry.getOrCreate(Identifier.of(Noellesroles.MOD_ID, "silence"), SilencePlayerComponent.class);

    private final PlayerEntity player;
    private boolean silenced = false;
    private @Nullable UUID silencer = null;
    private int outsideTicks = 0;
    private int tearChecks = 0;
    private int silencedTicks = 0;

    public SilencePlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    @Override
    public void serverTick() {
        if (this.silenced) {
            this.silencedTicks++;
        } else {
            this.silencedTicks = 0;
        }

        if (this.silenced && Wathe.isSkyVisibleAdjacent(this.player)) {
            this.outsideTicks++;
        } else {
            this.outsideTicks = 0;
        }

        if (this.silenced
                && MuzzlerConstants.SUFFOCATION_TICKS > 0
                && this.outsideTicks >= MuzzlerConstants.SUFFOCATION_TICKS
                && this.player instanceof ServerPlayerEntity serverVictim) {
            /*
             * 窒息死亡的实际攻击者可以为空：静语者可能已经离线。
             * 但回放仍然需要知道“胶带是谁贴的”，所以只要 UUID 还在，就写入 extraDeathData。
             */
            NbtCompound extraDeathData = new NbtCompound();
            PlayerEntity killer = null;
            if (this.silencer != null) {
                extraDeathData.putUuid("silencer", this.silencer);
                extraDeathData.putUuid("replay_actor", this.silencer);
                killer = serverVictim.getWorld().getPlayerByUuid(this.silencer);
            }
            GameFunctions.killPlayer(
                    serverVictim,
                    true,
                    killer,
                    Noellesroles.SILENCED_OUTSIDE_DEATH_REASON,
                    extraDeathData
            );
        }

        this.sync();
    }

    public void reset() {
        this.silenced = false;
        this.silencer = null;
        this.outsideTicks = 0;
        this.tearChecks = 0;
        this.silencedTicks = 0;
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    public boolean isSilenced() {
        return this.silenced;
    }

    public void setSilenced(boolean silenced) {
        this.silenced = silenced;
    }

    public @Nullable UUID getSilencer() {
        return this.silencer;
    }

    public void setSilencer(@Nullable UUID silencer) {
        this.silencer = silencer;
    }

    public int getTearChecks() {
        return this.tearChecks;
    }

    public void setTearChecks(int tearChecks) {
        this.tearChecks = Math.max(0, tearChecks);
    }

    public int getSilencedTicks() {
        return this.silencedTicks;
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.silenced = tag.getBoolean("silenced");
        this.silencer = tag.containsUuid("silencer") ? tag.getUuid("silencer") : null;
        this.outsideTicks = Math.max(0, tag.getInt("outside_ticks"));
        this.tearChecks = Math.max(0, tag.getInt("tear_checks"));
        this.silencedTicks = Math.max(0, tag.getInt("silenced_ticks"));
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("silenced", this.silenced);
        if (this.silencer != null) {
            tag.putUuid("silencer", this.silencer);
        }
        tag.putInt("outside_ticks", this.outsideTicks);
        tag.putInt("tear_checks", this.tearChecks);
        tag.putInt("silenced_ticks", this.silencedTicks);
    }
}
