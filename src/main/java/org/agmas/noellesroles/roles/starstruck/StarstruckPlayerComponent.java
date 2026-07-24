package org.agmas.noellesroles.roles.starstruck;

import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.NoellesRolesParticles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

/**
 * 星界使者能力状态。
 *
 * <p>这里只保存“星界能力还剩多少 tick”。客户端 HUD、本能颜色和移动速度都读取同一份同步状态，
 * 服务端则负责倒计时、粒子和能力结束回放，避免客户端各自猜时间导致显示不一致。</p>
 */
public class StarstruckPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<StarstruckPlayerComponent> KEY =
            ComponentRegistry.getOrCreate(Identifier.of(NoellesRolesCore.MOD_ID, "starstruck"), StarstruckPlayerComponent.class);

    private final PlayerEntity player;
    public int ticks = 0;

    public StarstruckPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    @Override
    public void serverTick() {
        if (this.ticks <= 0) {
            return;
        }

        --this.ticks;
        if (this.player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.getServerWorld().spawnParticles(
                    NoellesRolesParticles.STARSTRUCK_SPARKLE,
                    serverPlayer.getX(),
                    serverPlayer.getY() + 0.2D,
                    serverPlayer.getZ(),
                    serverPlayer.getRandom().nextBetween(
                            StarstruckConstants.ACTIVE_PARTICLE_MIN_COUNT,
                            StarstruckConstants.ACTIVE_PARTICLE_MAX_COUNT
                    ),
                    0.2D,
                    0.0D,
                    0.2D,
                    0.0D
            );

            if (this.ticks == 0) {
                // 能力自然耗尽时统一由服务端记回放，保证结算回放和实时回放不会漏。
                GameRecordManager.recordGlobalEvent(
                        serverPlayer.getServerWorld(),
                        NoellesEventIds.STARSTRUCK_ABILITY_END_EVENT,
                        serverPlayer,
                        null
                );
            }
        }
        this.sync();
    }

    public void setTicks(int ticks) {
        this.ticks = Math.max(0, ticks);
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
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("ticks", this.ticks);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.ticks = tag.contains("ticks") ? Math.max(0, tag.getInt("ticks")) : 0;
    }
}
