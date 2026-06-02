package org.agmas.noellesroles.roles.operator;

import dev.doctor4t.wathe.cca.GameWorldComponent;
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
 * 接线员个人主动效果状态。
 *
 * <p>这里把“接线员当前挂出去的效果”统一挂在接线员本人身上，原因有三：</p>
 * <p>1. 冷却本来就挂在技能使用者自己身上，状态与冷却一起更容易理解；</p>
 * <p>2. 回放事件里的 actor 也是接线员本人，天然对齐；</p>
 * <p>3. 回合重置 / 玩家重置时，只清这一个人的组件就能把其所有主动效果一起带走。</p>
 */
public class OperatorPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<OperatorPlayerComponent> KEY =
            ComponentRegistry.getOrCreate(Identifier.of(Noellesroles.MOD_ID, "operator"), OperatorPlayerComponent.class);

    private final PlayerEntity player;

    private boolean connectionActive;
    private int connectionTicksRemaining;
    @Nullable private UUID connectionPlayerOne;
    @Nullable private UUID connectionPlayerTwo;
    private String connectionPlayerOneName = "";
    private String connectionPlayerTwoName = "";

    private boolean broadcastActive;
    private int broadcastTicksRemaining;
    @Nullable private UUID broadcastTarget;
    private String broadcastTargetName = "";

    public OperatorPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    /**
     * 回合重置、玩家重置时的无声清空。
     *
     * <p>这里不播回放，因为 resetPlayer / initializeGame 的语义是“整局重置”，
     * 不是用户应当看到的技能自然结束或被迫中断。</p>
     */
    public void reset() {
        this.connectionActive = false;
        this.connectionTicksRemaining = 0;
        this.connectionPlayerOne = null;
        this.connectionPlayerTwo = null;
        this.connectionPlayerOneName = "";
        this.connectionPlayerTwoName = "";

        this.broadcastActive = false;
        this.broadcastTicksRemaining = 0;
        this.broadcastTarget = null;
        this.broadcastTargetName = "";
        this.sync();
    }

    public void startConnection(@NotNull ServerPlayerEntity first, @NotNull ServerPlayerEntity second) {
        this.connectionActive = true;
        this.connectionTicksRemaining = OperatorConstants.CONNECTION_DURATION_TICKS;
        this.connectionPlayerOne = first.getUuid();
        this.connectionPlayerTwo = second.getUuid();
        this.connectionPlayerOneName = first.getGameProfile().getName();
        this.connectionPlayerTwoName = second.getGameProfile().getName();

        // 接线员同一时间只保留一种主动效果，开启新接线时顺带清掉旧广播。
        this.broadcastActive = false;
        this.broadcastTicksRemaining = 0;
        this.broadcastTarget = null;
        this.broadcastTargetName = "";
        this.sync();
    }

    public void startBroadcast(@NotNull ServerPlayerEntity target) {
        this.broadcastActive = true;
        this.broadcastTicksRemaining = OperatorConstants.BROADCAST_DURATION_TICKS;
        this.broadcastTarget = target.getUuid();
        this.broadcastTargetName = target.getGameProfile().getName();

        // 同理，开启广播时也清掉上一条接线。
        this.connectionActive = false;
        this.connectionTicksRemaining = 0;
        this.connectionPlayerOne = null;
        this.connectionPlayerTwo = null;
        this.connectionPlayerOneName = "";
        this.connectionPlayerTwoName = "";
        this.sync();
    }

    public boolean hasActiveConnection() {
        return this.connectionActive && this.connectionPlayerOne != null && this.connectionPlayerTwo != null;
    }

    public boolean hasActiveBroadcast() {
        return this.broadcastActive && this.broadcastTarget != null;
    }

    public @Nullable UUID getConnectionPlayerOne() {
        return this.connectionPlayerOne;
    }

    public @Nullable UUID getConnectionPlayerTwo() {
        return this.connectionPlayerTwo;
    }

    public @Nullable UUID getBroadcastTarget() {
        return this.broadcastTarget;
    }

    public String getConnectionPlayerOneName() {
        return this.connectionPlayerOneName;
    }

    public String getConnectionPlayerTwoName() {
        return this.connectionPlayerTwoName;
    }

    public String getBroadcastTargetName() {
        return this.broadcastTargetName;
    }

    @Override
    public void serverTick() {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(this.player.getWorld());
        if (!gameWorld.isRunning()
                || !gameWorld.isRole(this.player, Noellesroles.OPERATOR)
                || !GameFunctions.isPlayerAliveAndSurvival(this.player)) {
            this.reset();
            return;
        }

        if (this.hasActiveConnection()) {
            ServerPlayerEntity first = getOnlinePlayer(this.connectionPlayerOne);
            ServerPlayerEntity second = getOnlinePlayer(this.connectionPlayerTwo);

            if (!OperatorCommunicationManager.isLiveConnectionEndpoint(first)
                    || !OperatorCommunicationManager.isLiveConnectionEndpoint(second)) {
                this.finishConnection(true);
            } else {
                this.connectionTicksRemaining--;
                if (this.connectionTicksRemaining <= 0) {
                    this.finishConnection(false);
                } else if (this.connectionTicksRemaining % 20 == 0) {
                    this.sync();
                }
            }
        }

        if (this.hasActiveBroadcast()) {
            ServerPlayerEntity target = getOnlinePlayer(this.broadcastTarget);
            if (!OperatorCommunicationManager.isLiveConnectionEndpoint(target)) {
                this.finishBroadcast(true);
            } else {
                this.broadcastTicksRemaining--;
                if (this.broadcastTicksRemaining <= 0) {
                    this.finishBroadcast(false);
                } else if (this.broadcastTicksRemaining % 20 == 0) {
                    this.sync();
                }
            }
        }
    }

    private @Nullable ServerPlayerEntity getOnlinePlayer(@Nullable UUID uuid) {
        if (uuid == null || !(this.player instanceof ServerPlayerEntity serverPlayer)) {
            return null;
        }
        return serverPlayer.getServer().getPlayerManager().getPlayer(uuid);
    }

    private void finishConnection(boolean interrupted) {
        if (!(this.player instanceof ServerPlayerEntity operator) || !this.hasActiveConnection()) {
            this.connectionActive = false;
            this.connectionTicksRemaining = 0;
            this.connectionPlayerOne = null;
            this.connectionPlayerTwo = null;
            this.connectionPlayerOneName = "";
            this.connectionPlayerTwoName = "";
            this.sync();
            return;
        }

        NbtCompound extra = new NbtCompound();
        if (this.connectionPlayerOne != null) {
            extra.putUuid("player_one", this.connectionPlayerOne);
        }
        if (this.connectionPlayerTwo != null) {
            extra.putUuid("player_two", this.connectionPlayerTwo);
        }
        extra.putString("player_one_name", this.connectionPlayerOneName);
        extra.putString("player_two_name", this.connectionPlayerTwoName);
        GameRecordManager.recordGlobalEvent(
                operator.getServerWorld(),
                interrupted ? Noellesroles.OPERATOR_CONNECTION_INTERRUPTED_EVENT : Noellesroles.OPERATOR_CONNECTION_ENDED_EVENT,
                operator,
                extra
        );

        this.connectionActive = false;
        this.connectionTicksRemaining = 0;
        this.connectionPlayerOne = null;
        this.connectionPlayerTwo = null;
        this.connectionPlayerOneName = "";
        this.connectionPlayerTwoName = "";
        this.sync();
    }

    private void finishBroadcast(boolean interrupted) {
        if (!(this.player instanceof ServerPlayerEntity operator) || !this.hasActiveBroadcast()) {
            this.broadcastActive = false;
            this.broadcastTicksRemaining = 0;
            this.broadcastTarget = null;
            this.broadcastTargetName = "";
            this.sync();
            return;
        }

        NbtCompound extra = new NbtCompound();
        if (this.broadcastTarget != null) {
            extra.putUuid("target_player", this.broadcastTarget);
        }
        extra.putString("target_player_name", this.broadcastTargetName);
        GameRecordManager.recordGlobalEvent(
                operator.getServerWorld(),
                interrupted ? Noellesroles.OPERATOR_BROADCAST_INTERRUPTED_EVENT : Noellesroles.OPERATOR_BROADCAST_ENDED_EVENT,
                operator,
                extra
        );

        this.broadcastActive = false;
        this.broadcastTicksRemaining = 0;
        this.broadcastTarget = null;
        this.broadcastTargetName = "";
        this.sync();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("connectionActive", this.connectionActive);
        tag.putInt("connectionTicksRemaining", this.connectionTicksRemaining);
        if (this.connectionPlayerOne != null) {
            tag.putUuid("connectionPlayerOne", this.connectionPlayerOne);
        }
        if (this.connectionPlayerTwo != null) {
            tag.putUuid("connectionPlayerTwo", this.connectionPlayerTwo);
        }
        tag.putString("connectionPlayerOneName", this.connectionPlayerOneName);
        tag.putString("connectionPlayerTwoName", this.connectionPlayerTwoName);

        tag.putBoolean("broadcastActive", this.broadcastActive);
        tag.putInt("broadcastTicksRemaining", this.broadcastTicksRemaining);
        if (this.broadcastTarget != null) {
            tag.putUuid("broadcastTarget", this.broadcastTarget);
        }
        tag.putString("broadcastTargetName", this.broadcastTargetName);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.connectionActive = tag.getBoolean("connectionActive");
        this.connectionTicksRemaining = Math.max(0, tag.getInt("connectionTicksRemaining"));
        this.connectionPlayerOne = tag.containsUuid("connectionPlayerOne") ? tag.getUuid("connectionPlayerOne") : null;
        this.connectionPlayerTwo = tag.containsUuid("connectionPlayerTwo") ? tag.getUuid("connectionPlayerTwo") : null;
        this.connectionPlayerOneName = tag.getString("connectionPlayerOneName");
        this.connectionPlayerTwoName = tag.getString("connectionPlayerTwoName");

        this.broadcastActive = tag.getBoolean("broadcastActive");
        this.broadcastTicksRemaining = Math.max(0, tag.getInt("broadcastTicksRemaining"));
        this.broadcastTarget = tag.containsUuid("broadcastTarget") ? tag.getUuid("broadcastTarget") : null;
        this.broadcastTargetName = tag.getString("broadcastTargetName");
    }
}
