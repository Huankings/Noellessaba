package org.agmas.noellesroles.roles.winder;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.UUID;

/**
 * 被烙印玩家自己的状态组件。
 *
 * <p>这样做的好处是：
 * 1. 透视判断直接看目标自己是否有烙印；
 * 2. 危险检测直接围绕目标玩家附近做扫描；
 * 3. 死亡、停局和下把重置也都能自然跟着目标玩家走。
 */
public class WindMarkPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<WindMarkPlayerComponent> KEY =
            ComponentRegistry.getOrCreate(Identifier.of(Noellesroles.MOD_ID, "wind_mark"), WindMarkPlayerComponent.class);

    private final PlayerEntity player;
    private int remainingTicks = 0;
    private UUID applierUuid = null;

    public WindMarkPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    public boolean hasActiveMark() {
        return this.remainingTicks > 0;
    }

    public void applyMark(PlayerEntity applier) {
        this.remainingTicks = WinderConstants.MARK_DURATION_TICKS;
        this.applierUuid = applier.getUuid();
        this.sync();
    }

    public void reset() {
        if (this.remainingTicks == 0) {
            return;
        }
        this.remainingTicks = 0;
        this.applierUuid = null;
        this.sync();
    }

    @Override
    public void serverTick() {
        if (this.remainingTicks <= 0) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(this.player.getWorld());
        if (!gameWorld.isRunning() || !GameFunctions.isPlayerAliveAndSurvival(this.player)) {
            this.reset();
            return;
        }

        // 只要附近有人举起匕首，就立即消耗烙印并把目标抬升出去。
        PlayerEntity knifeUser = findNearbyKnifeUser();
        if (knifeUser != null) {
            this.player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.LEVITATION,
                    WinderConstants.MARK_ESCAPE_LEVITATION_TICKS,
                    WinderConstants.MARK_ESCAPE_LEVITATION_AMPLIFIER,
                    true,
                    true,
                    true
            ));
            if (this.player instanceof ServerPlayerEntity serverPlayer) {
                NbtCompound extra = new NbtCompound();
                extra.putUuid("victim", serverPlayer.getUuid());
                extra.putUuid("knife_user", knifeUser.getUuid());
                GameRecordManager.recordGlobalEvent(serverPlayer.getServerWorld(), Noellesroles.WINDER_WIND_MARK_TRIGGERED_EVENT, null, extra);
            }
            this.reset();
            return;
        }

        this.remainingTicks--;
        if (this.remainingTicks <= 0) {
            if (this.player instanceof ServerPlayerEntity serverPlayer) {
                NbtCompound extra = new NbtCompound();
                extra.putUuid("victim", serverPlayer.getUuid());
                GameRecordManager.recordGlobalEvent(serverPlayer.getServerWorld(), Noellesroles.WINDER_WIND_MARK_EXPIRED_EVENT, null, extra);
            }
            this.reset();
        }
    }

    /**
     * 自己举刀也会触发，因此这里不排除 self。
     */
    private PlayerEntity findNearbyKnifeUser() {
        for (PlayerEntity other : this.player.getWorld().getPlayers()) {
            if (!GameFunctions.isPlayerAliveAndSurvival(other)) {
                continue;
            }

            if (other.squaredDistanceTo(this.player) > WinderConstants.MARK_KNIFE_TRIGGER_RADIUS_SQUARED) {
                continue;
            }

            if (other.isUsingItem() && other.getActiveItem().isOf(WatheItems.KNIFE)) {
                return other;
            }
        }
        return null;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("remainingTicks", this.remainingTicks);
        if (this.applierUuid != null) {
            tag.putUuid("applierUuid", this.applierUuid);
        }
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.remainingTicks = Math.max(0, tag.getInt("remainingTicks"));
        this.applierUuid = tag.containsUuid("applierUuid") ? tag.getUuid("applierUuid") : null;
    }
}
