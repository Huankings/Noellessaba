package org.agmas.noellesroles.roles.timekeeper;

import dev.doctor4t.wathe.api.PlayerLifeStateApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.compat.TrainVoicePlugin;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

/**
 * 时停者玩家状态。
 *
 * <p>光阴余额本身复用 Wathe 的 {@link PlayerShopComponent} 多货币存储；
 * 本组件只保存“时停者专属且不能表达成普通货币”的运行态。
 * 这样商店、HUD、怀表物品、死亡狭缝和后续快照回滚都能读同一份数据。</p>
 */
public class TimekeeperPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<TimekeeperPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(NoellesRolesCore.MOD_ID, "timekeeper"),
            TimekeeperPlayerComponent.class
    );

    private final PlayerEntity player;

    /** 是否已经购买但尚未在下一次时间回溯中消耗的回溯保护。 */
    private boolean rewindProtectionPurchased = false;

    /** 光阴被动收入计时器；达到常量里的间隔后给时停者发放一次光阴。 */
    private int passiveTimeIncomeTicker = 0;

    /** 物品加速模式的剩余冷却。 */
    private int itemAccelerateCooldownTicks = 0;

    /** 技能加速模式的剩余冷却。 */
    private int abilityAccelerateCooldownTicks = 0;

    /** 时间回溯模式的剩余冷却。 */
    private int rewindCooldownTicks = 0;

    /** 是否处于时间狭缝。该状态只给玩家本人同步，用于 HUD、聊天、语音和外观视角判断。 */
    private boolean inTimeRift = false;

    /** 时间狭缝剩余 tick，默认等于单次回溯深度 30 秒。 */
    private int timeRiftTicksLeft = 0;

    public TimekeeperPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    @Override
    public void serverTick() {
        boolean changed = tickCooldowns();
        changed |= tickPassiveTimeIncome();
        changed |= tickTimeRift();

        if (changed) {
            sync();
        }
    }

    /**
     * 回合/职业重置入口。
     *
     * <p>这里必须同时清掉“已购买保护”和“狭缝授权”。
     * 否则上一局买过保护或处于狭缝的玩家，在下一局可能仍然免疫回滚或被判作特殊存活旁观。</p>
     */
    public void reset() {
        this.rewindProtectionPurchased = false;
        this.passiveTimeIncomeTicker = 0;
        this.itemAccelerateCooldownTicks = 0;
        this.abilityAccelerateCooldownTicks = 0;
        this.rewindCooldownTicks = 0;
        this.inTimeRift = false;
        this.timeRiftTicksLeft = 0;
        PlayerLifeStateApi.clearAliveOverride(this.player);
        sync();
    }

    /**
     * 职业分配后调用。
     *
     * <p>时停者开局没有怀表技能冷却，但需要把上一局残留的保护与狭缝状态清干净。</p>
     */
    public void onAssigned() {
        reset();
        sync();
    }

    public boolean hasRewindProtectionPurchased() {
        return this.rewindProtectionPurchased;
    }

    public boolean tryMarkRewindProtectionPurchased() {
        if (this.rewindProtectionPurchased) {
            return false;
        }
        this.rewindProtectionPurchased = true;
        sync();
        return true;
    }

    /**
     * 时间回溯真正开始时消费保护标记。
     *
     * <p>保护不是永久被动，而是“购买后等下一次回溯触发再消耗”。
     * 因此世界组件收集保护名单时会调用这里，拿到 true 的玩家会进入本次保护集合，
     * 同时把待消费标记清掉，下一次仍需重新购买。</p>
     */
    public boolean consumeRewindProtectionForRewind() {
        if (!this.rewindProtectionPurchased) {
            return false;
        }
        this.rewindProtectionPurchased = false;
        sync();
        return true;
    }

    public boolean isInTimeRift() {
        return this.inTimeRift;
    }

    public int getTimeRiftTicksLeft() {
        return this.timeRiftTicksLeft;
    }

    public int getItemAccelerateCooldownTicks() {
        return this.itemAccelerateCooldownTicks;
    }

    public int getAbilityAccelerateCooldownTicks() {
        return this.abilityAccelerateCooldownTicks;
    }

    public int getRewindCooldownTicks() {
        return this.rewindCooldownTicks;
    }

    public int getCooldownTicks(TimekeeperWatchMode mode) {
        return switch (mode) {
            case ITEM_ACCELERATE -> this.itemAccelerateCooldownTicks;
            case ABILITY_ACCELERATE -> this.abilityAccelerateCooldownTicks;
            case REWIND -> this.rewindCooldownTicks;
        };
    }

    public void setCooldown(TimekeeperWatchMode mode, int ticks) {
        int clamped = Math.max(0, ticks);
        switch (mode) {
            case ITEM_ACCELERATE -> this.itemAccelerateCooldownTicks = clamped;
            case ABILITY_ACCELERATE -> this.abilityAccelerateCooldownTicks = clamped;
            case REWIND -> this.rewindCooldownTicks = clamped;
        }
        sync();
    }

    public void reduceAllWatchCooldowns(int ticks) {
        if (ticks <= 0) {
            return;
        }
        this.itemAccelerateCooldownTicks = Math.max(0, this.itemAccelerateCooldownTicks - ticks);
        this.abilityAccelerateCooldownTicks = Math.max(0, this.abilityAccelerateCooldownTicks - ticks);
        this.rewindCooldownTicks = Math.max(0, this.rewindCooldownTicks - ticks);
        sync();
    }

    /**
     * 将死者拉入时间狭缝。
     *
     * <p>Wathe 的“特殊存活旁观”需要通过 {@link PlayerLifeStateApi} 设置，
     * 不能只调用原版 {@code changeGameMode}。只切模式会让 Wathe 把玩家当作普通死亡旁观，
     * 进而获得死亡频道与旁观本能透视，不符合时间狭缝的隔离设计。</p>
     */
    public void startTimeRift() {
        this.inTimeRift = true;
        this.timeRiftTicksLeft = TimekeeperConstants.RIFT_DURATION_TICKS;
        if (this.player instanceof ServerPlayerEntity serverPlayer) {
            PlayerLifeStateApi.changeGameModeAsGameplayAlive(serverPlayer, GameMode.SPECTATOR);
            TrainVoicePlugin.resetPlayer(serverPlayer.getUuid());
        }
        sync();
    }

    public void finishTimeRift() {
        boolean wasInRift = this.inTimeRift;
        this.inTimeRift = false;
        this.timeRiftTicksLeft = 0;

        if (this.player instanceof ServerPlayerEntity serverPlayer) {
            PlayerLifeStateApi.clearAliveOverride(serverPlayer);
            serverPlayer.changeGameMode(GameMode.SPECTATOR);
            TrainVoicePlugin.addPlayer(serverPlayer.getUuid());
            if (wasInRift) {
                serverPlayer.sendMessage(coloredActionbar("message.noellesroles.timekeeper.rift_ended"), true);
            }
        }
        sync();
    }

    /**
     * 时间回溯把“时间狭缝”中的死者拉回到仍然存活的快照时调用。
     *
     * <p>这个出口和 {@link #finishTimeRift()} 刻意分开：
     * 普通狭缝到时结束时，玩家仍然是已经死亡的观察者，需要清掉 Wathe 的特殊存活授权并加入死亡语音频道；
     * 但回溯复活时，玩家状态已经由快照恢复成目标时间点的存活状态，
     * 如果这里再切旁观或加入死亡频道，反而会把“复活”覆盖掉。
     * 因此这里仅关闭狭缝标记、解除语音隔离，并提示玩家已经脱离狭缝。</p>
     */
    public void finishTimeRiftAsRewoundAlive() {
        boolean wasInRift = this.inTimeRift;
        this.inTimeRift = false;
        this.timeRiftTicksLeft = 0;

        if (this.player instanceof ServerPlayerEntity serverPlayer) {
            TrainVoicePlugin.resetPlayer(serverPlayer.getUuid());
            if (wasInRift) {
                serverPlayer.sendMessage(coloredActionbar("message.noellesroles.timekeeper.rift_ended"), true);
            }
        }
        sync();
    }

    private boolean tickCooldowns() {
        boolean changed = false;
        if (this.itemAccelerateCooldownTicks > 0) {
            this.itemAccelerateCooldownTicks--;
            changed = true;
        }
        if (this.abilityAccelerateCooldownTicks > 0) {
            this.abilityAccelerateCooldownTicks--;
            changed = true;
        }
        if (this.rewindCooldownTicks > 0) {
            this.rewindCooldownTicks--;
            changed = true;
        }
        return changed;
    }

    private boolean tickPassiveTimeIncome() {
        if (!(this.player instanceof ServerPlayerEntity serverPlayer)) {
            return false;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(serverPlayer.getWorld());
        if (!gameWorld.isRunning()
                || !gameWorld.isRole(serverPlayer, NoellesRoleRegistry.TIMEKEEPER)
                || !GameFunctions.isPlayerAliveAndSurvival(serverPlayer)) {
            this.passiveTimeIncomeTicker = 0;
            return false;
        }

        this.passiveTimeIncomeTicker++;
        if (this.passiveTimeIncomeTicker < TimekeeperConstants.PASSIVE_TIME_INCOME_INTERVAL_TICKS) {
            return false;
        }

        this.passiveTimeIncomeTicker = 0;
        PlayerShopComponent.KEY.get(serverPlayer).addCurrencyAmount(
                TimekeeperConstants.TIME_CURRENCY_ID,
                TimekeeperConstants.PASSIVE_TIME_INCOME_AMOUNT
        );
        return false;
    }

    private boolean tickTimeRift() {
        if (!this.inTimeRift) {
            return false;
        }

        if (!(this.player instanceof ServerPlayerEntity serverPlayer)) {
            return false;
        }

        maintainTimeRiftSpectator(serverPlayer);

        this.timeRiftTicksLeft = Math.max(0, this.timeRiftTicksLeft - 1);
        int secondsLeft = Math.max(1, (this.timeRiftTicksLeft + 19) / 20);
        serverPlayer.sendMessage(
                Text.translatable("message.noellesroles.timekeeper.rift_active", secondsLeft)
                        .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(TimekeeperConstants.ROLE_COLOR))),
                true
        );

        if (this.timeRiftTicksLeft <= 0) {
            finishTimeRift();
            return false;
        }
        return this.timeRiftTicksLeft % 20 == 0;
    }

    private void maintainTimeRiftSpectator(@NotNull ServerPlayerEntity serverPlayer) {
        /*
         * 狭缝期间每 tick 重新压一次特殊存活旁观。
         * 部分死亡链、复活链或旁观同步会在同一段时间内再次改玩家模式；
         * 持续校正可以避免刚死亡的玩家还没完全进入 Wathe 旁观态时就漏出语音/本能信息。
         */
        if (!PlayerLifeStateApi.hasAliveOverride(serverPlayer) || serverPlayer.interactionManager.getGameMode() != GameMode.SPECTATOR) {
            PlayerLifeStateApi.changeGameModeAsGameplayAlive(serverPlayer, GameMode.SPECTATOR);
        }
        TrainVoicePlugin.resetPlayer(serverPlayer.getUuid());
    }

    private static Text coloredActionbar(String translationKey) {
        return Text.translatable(translationKey)
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(TimekeeperConstants.ROLE_COLOR)));
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return this.player.equals(player);
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeBoolean(this.rewindProtectionPurchased);
        buf.writeInt(this.itemAccelerateCooldownTicks);
        buf.writeInt(this.abilityAccelerateCooldownTicks);
        buf.writeInt(this.rewindCooldownTicks);
        buf.writeBoolean(this.inTimeRift);
        buf.writeInt(this.timeRiftTicksLeft);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.rewindProtectionPurchased = buf.readBoolean();
        this.itemAccelerateCooldownTicks = buf.readInt();
        this.abilityAccelerateCooldownTicks = buf.readInt();
        this.rewindCooldownTicks = buf.readInt();
        this.inTimeRift = buf.readBoolean();
        this.timeRiftTicksLeft = buf.readInt();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("rewindProtectionPurchased", this.rewindProtectionPurchased);
        tag.putInt("passiveTimeIncomeTicker", this.passiveTimeIncomeTicker);
        tag.putInt("itemAccelerateCooldownTicks", this.itemAccelerateCooldownTicks);
        tag.putInt("abilityAccelerateCooldownTicks", this.abilityAccelerateCooldownTicks);
        tag.putInt("rewindCooldownTicks", this.rewindCooldownTicks);
        tag.putBoolean("inTimeRift", this.inTimeRift);
        tag.putInt("timeRiftTicksLeft", this.timeRiftTicksLeft);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.rewindProtectionPurchased = tag.contains("rewindProtectionPurchased") && tag.getBoolean("rewindProtectionPurchased");
        this.passiveTimeIncomeTicker = tag.contains("passiveTimeIncomeTicker") ? tag.getInt("passiveTimeIncomeTicker") : 0;
        this.itemAccelerateCooldownTicks = tag.contains("itemAccelerateCooldownTicks") ? tag.getInt("itemAccelerateCooldownTicks") : 0;
        this.abilityAccelerateCooldownTicks = tag.contains("abilityAccelerateCooldownTicks") ? tag.getInt("abilityAccelerateCooldownTicks") : 0;
        this.rewindCooldownTicks = tag.contains("rewindCooldownTicks") ? tag.getInt("rewindCooldownTicks") : 0;
        this.inTimeRift = tag.contains("inTimeRift") && tag.getBoolean("inTimeRift");
        this.timeRiftTicksLeft = tag.contains("timeRiftTicksLeft") ? tag.getInt("timeRiftTicksLeft") : 0;
    }
}
