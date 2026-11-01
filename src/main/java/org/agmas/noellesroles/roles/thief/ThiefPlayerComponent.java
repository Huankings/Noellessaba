package org.agmas.noellesroles.roles.thief;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.UUID;

/**
 * 小偷悦灵能力的玩家运行态。
 *
 * <p>悦灵召唤冷却必须与现有空手偷窃冷却相互独立，因此不能继续复用
 * {@code AbilityPlayerComponent.cooldown}。本组件还保存1～3号悦灵的 HUD 状态，
 * 由服务端实体状态机写入，并且只同步给小偷本人。</p>
 */
public final class ThiefPlayerComponent implements AutoSyncedComponent, ServerTickingComponent, ClientTickingComponent {
    public static final ComponentKey<ThiefPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            NoellesRolesCore.id("thief_player"),
            ThiefPlayerComponent.class
    );

    private final PlayerEntity player;
    private final ThiefAllayState[] states = new ThiefAllayState[ThiefConstants.ALLAY_MAX_COUNT];
    private final UUID[] entityUuids = new UUID[ThiefConstants.ALLAY_MAX_COUNT];
    private final int[] terminalTicks = new int[ThiefConstants.ALLAY_MAX_COUNT];
    private int summonCooldownTicks;

    public ThiefPlayerComponent(PlayerEntity player) {
        this.player = player;
        for (int index = 0; index < this.states.length; index++) {
            this.states[index] = ThiefAllayState.NONE;
        }
    }

    /** 清空本局悦灵状态；普通空手偷窃冷却不属于本组件。 */
    public void reset() {
        this.summonCooldownTicks = 0;
        for (int index = 0; index < this.states.length; index++) {
            this.states[index] = ThiefAllayState.NONE;
            this.entityUuids[index] = null;
            this.terminalTicks[index] = 0;
        }
        sync();
    }

    /** 真正分配成小偷时写入80秒悦灵开局冷却。 */
    public void initializeForRole() {
        reset();
        setSummonCooldownTicks(ThiefConstants.ALLAY_INITIAL_COOLDOWN_TICKS);
    }

    public int getSummonCooldownTicks() {
        return this.summonCooldownTicks;
    }

    public void setSummonCooldownTicks(int ticks) {
        this.summonCooldownTicks = Math.max(0, ticks);
        sync();
    }

    /**
     * 召集者封控等外部机制只允许把冷却延长到某个下限，不能缩短小偷本来更长的120/180秒冷却。
     */
    public void ensureMinimumSummonCooldown(int ticks) {
        int safeTicks = Math.max(0, ticks);
        if (this.summonCooldownTicks < safeTicks) {
            this.summonCooldownTicks = safeTicks;
            sync();
        }
    }

    /** 任务奖励按增量减少冷却，并在0处钳制。 */
    public void reduceSummonCooldown(int ticks) {
        int next = Math.max(0, this.summonCooldownTicks - Math.max(0, ticks));
        if (next != this.summonCooldownTicks) {
            this.summonCooldownTicks = next;
            sync();
        }
    }

    public ThiefAllayState getState(int number) {
        int index = index(number);
        return index < 0 ? ThiefAllayState.NONE : this.states[index];
    }

    public @Nullable UUID getEntityUuid(int number) {
        int index = index(number);
        return index < 0 ? null : this.entityUuids[index];
    }

    /** 实体进入普通存活状态时更新号数、实体 UUID 和 HUD 文本。 */
    public void setLivingState(int number, @NotNull UUID entityUuid, @NotNull ThiefAllayState state) {
        int index = index(number);
        if (index < 0 || !state.isLiving()) {
            return;
        }
        this.states[index] = state;
        this.entityUuids[index] = entityUuid;
        this.terminalTicks[index] = 0;
        sync();
    }

    /**
     * 悦灵交付或死亡后实体可以立即消失，但该号数的终态文字仍需保留两秒。
     */
    public void setTerminalState(int number, @NotNull ThiefAllayState state) {
        int index = index(number);
        if (index < 0 || !state.isTerminal()) {
            return;
        }
        this.states[index] = state;
        this.entityUuids[index] = null;
        this.terminalTicks[index] = ThiefConstants.ALLAY_TERMINAL_HUD_TICKS;
        sync();
    }

    /** 偷取失败的悦灵回到主人身边后直接消失，不需要额外保留终态文字。 */
    public void clearState(int number) {
        int index = index(number);
        if (index < 0) {
            return;
        }
        this.states[index] = ThiefAllayState.NONE;
        this.entityUuids[index] = null;
        this.terminalTicks[index] = 0;
        sync();
    }

    /** 召唤事务中途失败时只清悦灵号数，不改已经存在的背包冷却。 */
    public void clearAllayStates() {
        for (int index = 0; index < this.states.length; index++) {
            this.states[index] = ThiefAllayState.NONE;
            this.entityUuids[index] = null;
            this.terminalTicks[index] = 0;
        }
        sync();
    }

    public boolean hasLivingAllayState() {
        for (ThiefAllayState state : this.states) {
            if (state.isLiving()) {
                return true;
            }
        }
        return false;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public void serverTick() {
        boolean syncNeeded = false;
        if (this.summonCooldownTicks > 0) {
            this.summonCooldownTicks--;
            syncNeeded = this.summonCooldownTicks == 0
                    || this.summonCooldownTicks % ThiefConstants.ALLAY_COOLDOWN_SYNC_INTERVAL_TICKS == 0;
        }
        for (int index = 0; index < this.terminalTicks.length; index++) {
            if (this.terminalTicks[index] <= 0) {
                continue;
            }
            this.terminalTicks[index]--;
            if (this.terminalTicks[index] == 0) {
                this.states[index] = ThiefAllayState.NONE;
                this.entityUuids[index] = null;
                syncNeeded = true;
            }
        }
        if (syncNeeded) {
            sync();
        }
    }

    @Override
    public void clientTick() {
        if (this.summonCooldownTicks > 0) {
            this.summonCooldownTicks--;
        }
        for (int index = 0; index < this.terminalTicks.length; index++) {
            if (this.terminalTicks[index] <= 0) {
                continue;
            }
            this.terminalTicks[index]--;
            if (this.terminalTicks[index] == 0) {
                this.states[index] = ThiefAllayState.NONE;
                this.entityUuids[index] = null;
            }
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("summon_cooldown", this.summonCooldownTicks);
        for (int index = 0; index < this.states.length; index++) {
            NbtCompound entry = new NbtCompound();
            entry.putInt("state", this.states[index].ordinal());
            entry.putInt("terminal_ticks", this.terminalTicks[index]);
            if (this.entityUuids[index] != null) {
                entry.putUuid("entity_uuid", this.entityUuids[index]);
            }
            tag.put("allay_" + (index + 1), entry);
        }
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.summonCooldownTicks = Math.max(0, tag.getInt("summon_cooldown"));
        for (int index = 0; index < this.states.length; index++) {
            String key = "allay_" + (index + 1);
            NbtCompound entry = tag.contains(key) ? tag.getCompound(key) : new NbtCompound();
            this.states[index] = ThiefAllayState.byOrdinal(entry.getInt("state"));
            this.terminalTicks[index] = Math.max(0, entry.getInt("terminal_ticks"));
            this.entityUuids[index] = entry.containsUuid("entity_uuid") ? entry.getUuid("entity_uuid") : null;
            if (this.states[index] == ThiefAllayState.NONE) {
                this.entityUuids[index] = null;
                this.terminalTicks[index] = 0;
            }
        }
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        return this.player.equals(recipient);
    }

    private static int index(int number) {
        return number >= ThiefConstants.ALLAY_MIN_COUNT && number <= ThiefConstants.ALLAY_MAX_COUNT
                ? number - ThiefConstants.ALLAY_MIN_COUNT
                : -1;
    }
}
