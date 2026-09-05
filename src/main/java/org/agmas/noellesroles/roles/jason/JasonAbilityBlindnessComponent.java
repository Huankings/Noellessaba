package org.agmas.noellesroles.roles.jason;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

/**
 * 杰森无恶不在发放的失明效果归属。
 *
 * <p>失明是原版共享药水类型，StatusEffectInstance 本身没有“来源”字段。
 * 这里用组件记录 NoellesRoles 自己刷新出的短效果，从而在结束能力时只处理自己的
 * 失明，不误删外部职业或物品给予的更长效果。</p>
 */
public final class JasonAbilityBlindnessComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<JasonAbilityBlindnessComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(NoellesRolesCore.MOD_ID, "jason_ability_blindness"),
            JasonAbilityBlindnessComponent.class
    );

    private static final String OWNED_KEY = "owned";
    private static final String DURATION_KEY = "duration";

    private final PlayerEntity player;
    private boolean owned;
    private int ownedDuration;

    public JasonAbilityBlindnessComponent(PlayerEntity player) {
        this.player = player;
    }

    public boolean isOwnedAndActive() {
        StatusEffectInstance current = this.player.getStatusEffect(StatusEffects.BLINDNESS);
        return this.owned && this.ownedDuration > 0 && isMatchingShortEffect(current);
    }

    /** 每 tick 续杯一份 60 tick 的短失明。 */
    public void refreshOwnedEffect() {
        StatusEffectInstance current = this.player.getStatusEffect(StatusEffects.BLINDNESS);
        if (current != null && !isMatchingShortEffect(current)) {
            // 外部更强/更长失明优先保留，不能被杰森效果覆盖。
            clearOwnedStateOnly();
            return;
        }
        this.player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.BLINDNESS,
                JasonConstants.ABILITY_BLINDNESS_REFRESH_TICKS,
                0,
                true,
                false,
                false
        ));
        this.owned = true;
        this.ownedDuration = JasonConstants.ABILITY_BLINDNESS_REFRESH_TICKS;
        sync();
    }

    /** 强制路径立即删除本组件确认拥有的短失明。 */
    public void clearOwnedEffect() {
        if (this.owned) {
            StatusEffectInstance current = this.player.getStatusEffect(StatusEffects.BLINDNESS);
            if (isMatchingShortEffect(current)) {
                this.player.removeStatusEffect(StatusEffects.BLINDNESS);
            }
        }
        clearOwnedStateOnly();
    }

    /**
     * 正常 EXITING -> IDLE 时停止续杯，让最后一份药水自然倒计时结束。
     */
    public void releaseOwnedEffectToExpireNaturally() {
        StatusEffectInstance current = this.player.getStatusEffect(StatusEffects.BLINDNESS);
        if (!this.owned || !isMatchingShortEffect(current)) {
            clearOwnedStateOnly();
            return;
        }
        this.ownedDuration = Math.max(1, current.getDuration());
        sync();
    }

    /**
     * 开始无恶不在的主动退出倒计时。
     *
     * <p>退出过渡和失明必须从同一时刻开始计时。这里先移除旧的 60 tick 续杯，
     * 再写入精确的退出时长；之后 EXITING 阶段不再刷新本组件，药水会自然倒计时，
     * 视觉恢复正好与杰森 2 秒显形过渡同步完成。</p>
     */
    public void startNaturalExitCountdown(int ticks) {
        StatusEffectInstance current = this.player.getStatusEffect(StatusEffects.BLINDNESS);
        if (!this.owned || !isMatchingShortEffect(current)) {
            clearOwnedStateOnly();
            return;
        }

        this.player.removeStatusEffect(StatusEffects.BLINDNESS);
        int naturalDuration = Math.max(1, ticks);
        this.player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.BLINDNESS,
                naturalDuration,
                0,
                true,
                false,
                false
        ));
        this.owned = true;
        this.ownedDuration = naturalDuration;
        sync();
    }

    public void reset() {
        clearOwnedEffect();
    }

    private boolean isMatchingShortEffect(StatusEffectInstance effect) {
        return effect != null
                && effect.getAmplifier() == 0
                && effect.getDuration() <= JasonConstants.ABILITY_BLINDNESS_MAX_OWNED_DURATION_TICKS;
    }

    private void clearOwnedStateOnly() {
        if (!this.owned && this.ownedDuration <= 0) {
            return;
        }
        this.owned = false;
        this.ownedDuration = 0;
        sync();
    }

    @Override
    public void serverTick() {
        if (!this.owned || this.ownedDuration <= 0) {
            return;
        }
        this.ownedDuration--;
        if (this.ownedDuration <= 0) {
            clearOwnedStateOnly();
        }
    }

    public void sync() {
        if (!this.player.getWorld().isClient) {
            KEY.sync(this.player);
        }
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity viewer) {
        return this.player.equals(viewer);
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.@NotNull WrapperLookup registryLookup) {
        tag.putBoolean(OWNED_KEY, this.owned);
        tag.putInt(DURATION_KEY, this.ownedDuration);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.@NotNull WrapperLookup registryLookup) {
        this.owned = tag.getBoolean(OWNED_KEY);
        this.ownedDuration = Math.max(0, tag.getInt(DURATION_KEY));
    }
}
