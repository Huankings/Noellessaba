package org.agmas.noellesroles.roles.coward;

import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
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
 * 镇静效果玩家组件。
 */
public class SedativePlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<SedativePlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "sedative"),
            SedativePlayerComponent.class
    );

    private final PlayerEntity player;
    private int sedativeTicks = 0;
    private @Nullable UUID applierUuid;

    public SedativePlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    public boolean isActive() {
        return this.sedativeTicks > 0;
    }

    public int getSedativeTicks() {
        return this.sedativeTicks;
    }

    public void reset() {
        this.sedativeTicks = 0;
        this.applierUuid = null;
        this.sync();
    }

    public void startSedative(ServerPlayerEntity player, @Nullable UUID applierUuid) {
        if (this.isActive()) {
            NbtCompound deathData = new NbtCompound();
            if (applierUuid != null) {
                deathData.putUuid("replay_actor", applierUuid);
            }
            GameFunctions.killPlayer(player, true, null, Noellesroles.DEATH_REASON_SEDATIVE_OVERDOSE, deathData);
            return;
        }

        this.sedativeTicks = CowardConstants.SEDATIVE_DURATION_TICKS;
        this.applierUuid = applierUuid;
        PlayerMoodComponent.KEY.get(player).setMoodDrainProtectionTicks(this.sedativeTicks);
        this.sync();

        player.sendMessage(
                Text.translatable("message.noellesroles.coward.sedative_started").withColor(Noellesroles.COWARD.color()),
                true
        );
        GameRecordManager.recordGlobalEvent(player.getServerWorld(), Noellesroles.SEDATIVE_STARTED_EVENT, player, null);
    }

    @Override
    public void serverTick() {
        if (!this.isActive()) {
            return;
        }

        if (!GameFunctions.isPlayerAliveAndSurvival(this.player)) {
            this.reset();
            return;
        }

        this.sedativeTicks--;

        if (this.player instanceof ServerPlayerEntity serverPlayer) {
            PlayerMoodComponent.KEY.get(serverPlayer).setMoodDrainProtectionTicks(this.sedativeTicks);

            if (this.sedativeTicks == 0) {
                serverPlayer.sendMessage(
                        Text.translatable("message.noellesroles.coward.sedative_ended").withColor(Noellesroles.COWARD.color()),
                        true
                );
                GameRecordManager.recordGlobalEvent(serverPlayer.getServerWorld(), Noellesroles.SEDATIVE_ENDED_EVENT, serverPlayer, null);
                this.reset();
                return;
            }
        }

        if (this.sedativeTicks % 20 == 0) {
            this.sync();
        }
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return this.player.equals(player);
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("sedativeTicks", this.sedativeTicks);
        if (this.applierUuid != null) {
            tag.putUuid("applierUuid", this.applierUuid);
        }
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.sedativeTicks = Math.max(0, tag.getInt("sedativeTicks"));
        this.applierUuid = tag.containsUuid("applierUuid") ? tag.getUuid("applierUuid") : null;
    }
}
