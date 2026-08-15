package org.agmas.noellesroles.roles.jason;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.UUID;

/**
 * 杰森“无恶不在”的玩家运行态。
 *
 * <p>该组件只保存会影响局势或客户端表现的权威状态：
 * 开局/退出冷却、进入/持续/退出阶段、惊吓倒计时和惊吓来源。
 * 红色提示粒子的“上一 tick 位置”等纯表现缓存不写进组件，避免时停者回溯时把临时渲染细节当成玩法状态。</p>
 */
public final class JasonAbilityPlayerComponent implements AutoSyncedComponent {
    public static final ComponentKey<JasonAbilityPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(NoellesRolesCore.MOD_ID, "jason_ability"),
            JasonAbilityPlayerComponent.class
    );

    private static final String PHASE_KEY = "phase";
    private static final String COOLDOWN_TICKS_KEY = "cooldown_ticks";
    private static final String COOLDOWN_KIND_KEY = "cooldown_kind";
    private static final String PHASE_TICKS_KEY = "phase_ticks";
    private static final String ACTIVE_TICKS_KEY = "active_ticks";
    private static final String SCARED_TICKS_KEY = "scared_ticks";
    private static final String SCARED_BY_KEY = "scared_by";

    private final PlayerEntity player;
    private Phase phase = Phase.IDLE;
    private int cooldownTicks = 0;
    private CooldownKind cooldownKind = CooldownKind.NONE;
    private int phaseTicks = 0;
    private int activeTicks = 0;
    private int scaredTicks = 0;
    private @Nullable UUID scaredByUuid;

    public JasonAbilityPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public @NotNull Phase getPhase() {
        return this.phase;
    }

    public int getCooldownTicks() {
        return this.cooldownTicks;
    }

    public @NotNull CooldownKind getCooldownKind() {
        return this.cooldownKind;
    }

    public int getPhaseTicks() {
        return this.phaseTicks;
    }

    public int getActiveTicks() {
        return this.activeTicks;
    }

    public int getScaredTicks() {
        return this.scaredTicks;
    }

    public @Nullable UUID getScaredByUuid() {
        return this.scaredByUuid;
    }

    public boolean isEntering() {
        return this.phase == Phase.ENTERING;
    }

    public boolean isFullyActive() {
        return this.phase == Phase.ACTIVE;
    }

    public boolean isExiting() {
        return this.phase == Phase.EXITING;
    }

    public boolean isActiveLike() {
        return this.phase == Phase.ENTERING || this.phase == Phase.ACTIVE || this.phase == Phase.EXITING;
    }

    public boolean canUseAbility() {
        return this.phase == Phase.IDLE && this.cooldownTicks <= 0;
    }

    public boolean canRequestExit() {
        /*
         * 主动解除只允许在进入过渡完成后触发。
         * activeTicks 从发动瞬间开始累计，因此 7 秒限制包含 2 秒进入过渡，贴合“发动后 7 秒”。
         */
        return this.phase == Phase.ACTIVE && this.activeTicks >= JasonConstants.ABILITY_MIN_TICKS_BEFORE_EXIT;
    }

    public int getRemainingMinExitTicks() {
        return Math.max(0, JasonConstants.ABILITY_MIN_TICKS_BEFORE_EXIT - this.activeTicks);
    }

    public void startRoundCooldown() {
        reset();
        this.cooldownTicks = JasonConstants.ABILITY_INITIAL_COOLDOWN_TICKS;
        this.cooldownKind = CooldownKind.INITIAL;
        sync();
    }

    public void startEntering() {
        this.phase = Phase.ENTERING;
        this.cooldownTicks = 0;
        this.cooldownKind = CooldownKind.NONE;
        this.phaseTicks = 0;
        this.activeTicks = 0;
        sync();
    }

    public void markActive() {
        if (this.phase != Phase.ENTERING) {
            return;
        }
        this.phase = Phase.ACTIVE;
        this.phaseTicks = 0;
        sync();
    }

    public void startExiting() {
        if (!isActiveLike() || this.phase == Phase.EXITING) {
            return;
        }
        this.phase = Phase.EXITING;
        this.phaseTicks = 0;
        sync();
    }

    public void finishExit(boolean startCooldown) {
        this.phase = Phase.IDLE;
        this.phaseTicks = 0;
        this.activeTicks = 0;
        if (startCooldown) {
            this.cooldownTicks = JasonConstants.ABILITY_EXIT_COOLDOWN_TICKS;
            this.cooldownKind = CooldownKind.AFTER_EXIT;
        } else {
            this.cooldownTicks = 0;
            this.cooldownKind = CooldownKind.NONE;
        }
        sync();
    }

    public void forceClear(boolean startExitCooldown) {
        this.phase = Phase.IDLE;
        this.phaseTicks = 0;
        this.activeTicks = 0;
        if (startExitCooldown) {
            this.cooldownTicks = JasonConstants.ABILITY_EXIT_COOLDOWN_TICKS;
            this.cooldownKind = CooldownKind.AFTER_EXIT;
        } else {
            this.cooldownTicks = 0;
            this.cooldownKind = CooldownKind.NONE;
        }
        sync();
    }

    public boolean tickCooldown() {
        if (this.cooldownTicks <= 0) {
            if (this.cooldownKind != CooldownKind.NONE) {
                this.cooldownKind = CooldownKind.NONE;
                sync();
                return true;
            }
            return false;
        }

        this.cooldownTicks--;
        if (this.cooldownTicks == 0) {
            this.cooldownKind = CooldownKind.NONE;
        }
        sync();
        return true;
    }

    public boolean tickPhase() {
        if (!isActiveLike()) {
            return false;
        }

        this.phaseTicks++;
        if (this.phase == Phase.ENTERING || this.phase == Phase.ACTIVE) {
            this.activeTicks++;
        }
        sync();
        return true;
    }

    public boolean clearAfterExitCooldownFromKill() {
        /*
         * 用户确认：杰森击杀只清“解除后的 15 秒冷却”，不能清掉开局 40 秒冷却。
         * 所以这里严格检查 cooldownKind，避免开局刚杀人就绕过技能保护期。
         */
        if (this.cooldownTicks <= 0 || this.cooldownKind != CooldownKind.AFTER_EXIT) {
            return false;
        }
        this.cooldownTicks = 0;
        this.cooldownKind = CooldownKind.NONE;
        sync();
        return true;
    }

    public void startScared(@NotNull UUID jasonUuid) {
        this.scaredTicks = JasonConstants.ABILITY_SCARE_TICKS;
        this.scaredByUuid = jasonUuid;
        sync();
    }

    public boolean tickScared() {
        if (this.scaredTicks <= 0) {
            return false;
        }

        this.scaredTicks--;
        if (this.scaredTicks == 0) {
            this.scaredByUuid = null;
        }
        sync();
        return true;
    }

    public void clearScared() {
        if (this.scaredTicks <= 0 && this.scaredByUuid == null) {
            return;
        }
        this.scaredTicks = 0;
        this.scaredByUuid = null;
        sync();
    }

    public void reset() {
        this.phase = Phase.IDLE;
        this.cooldownTicks = 0;
        this.cooldownKind = CooldownKind.NONE;
        this.phaseTicks = 0;
        this.activeTicks = 0;
        this.scaredTicks = 0;
        this.scaredByUuid = null;
        sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        /*
         * 无恶不在会影响其它客户端的玩家渲染、手持物隐藏、雾效和本能压制。
         * 因此这里同步给所有玩家；真正能不能看见信息，由各客户端 handler 再按 Wathe 存活状态过滤。
         */
        return true;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putString(PHASE_KEY, this.phase.name());
        tag.putInt(COOLDOWN_TICKS_KEY, this.cooldownTicks);
        tag.putString(COOLDOWN_KIND_KEY, this.cooldownKind.name());
        tag.putInt(PHASE_TICKS_KEY, this.phaseTicks);
        tag.putInt(ACTIVE_TICKS_KEY, this.activeTicks);
        tag.putInt(SCARED_TICKS_KEY, this.scaredTicks);
        if (this.scaredByUuid != null) {
            tag.putUuid(SCARED_BY_KEY, this.scaredByUuid);
        }
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.phase = readEnum(tag.getString(PHASE_KEY), Phase.IDLE);
        this.cooldownTicks = Math.max(0, tag.getInt(COOLDOWN_TICKS_KEY));
        this.cooldownKind = readEnum(tag.getString(COOLDOWN_KIND_KEY), CooldownKind.NONE);
        this.phaseTicks = Math.max(0, tag.getInt(PHASE_TICKS_KEY));
        this.activeTicks = Math.max(0, tag.getInt(ACTIVE_TICKS_KEY));
        this.scaredTicks = Math.max(0, tag.getInt(SCARED_TICKS_KEY));
        this.scaredByUuid = tag.containsUuid(SCARED_BY_KEY) ? tag.getUuid(SCARED_BY_KEY) : null;
        if (this.cooldownTicks <= 0) {
            this.cooldownKind = CooldownKind.NONE;
        }
        if (this.scaredTicks <= 0) {
            this.scaredByUuid = null;
        }
    }

    private static <E extends Enum<E>> E readEnum(String name, E fallback) {
        if (name == null || name.isEmpty()) {
            return fallback;
        }
        try {
            return Enum.valueOf(fallback.getDeclaringClass(), name);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    public enum Phase {
        /**
         * 未处于无恶不在状态。
         */
        IDLE,
        /**
         * 发动后的 2 秒过渡期。
         */
        ENTERING,
        /**
         * 完整幽魂状态，可在满足 7 秒限制后主动解除。
         */
        ACTIVE,
        /**
         * 主动解除后的 2 秒显形过渡期。
         */
        EXITING
    }

    public enum CooldownKind {
        /**
         * 当前没有技能冷却。
         */
        NONE,
        /**
         * 开局 40 秒冷却，杰森击杀不能清除。
         */
        INITIAL,
        /**
         * 主动/强制解除后的 15 秒冷却，杰森确认击杀可以清除。
         */
        AFTER_EXIT
    }
}
