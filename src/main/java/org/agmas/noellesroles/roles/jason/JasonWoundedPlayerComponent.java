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
 * 杰森投掷机制挂在玩家身上的运行态。
 *
 * <p>这里同时保存“重伤倒地”和“汽油沾染”两类状态：
 * 前者需要服务端每 tick 维护救治、失血和动作封锁，后者需要同步给客户端本能透视。
 * 这些都属于局内临时运行态，所以组件会加入时停者快照白名单。</p>
 */
public final class JasonWoundedPlayerComponent implements AutoSyncedComponent {
    public static final ComponentKey<JasonWoundedPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(NoellesRolesCore.MOD_ID, "jason_wounded"),
            JasonWoundedPlayerComponent.class
    );

    private static final String WOUNDED_KEY = "wounded";
    private static final String WOUND_COUNT_KEY = "wound_count";
    private static final String BLEED_TICKS_KEY = "bleed_ticks";
    private static final String RESCUE_TICKS_KEY = "rescue_ticks";
    private static final String RESCUER_KEY = "rescuer";
    private static final String ATTACKER_KEY = "attacker";
    private static final String WEAPON_ITEM_KEY = "weapon_item";
    private static final String GASOLINE_KEY = "gasoline";
    private static final String GASOLINE_OWNER_KEY = "gasoline_owner";
    private static final String GASOLINE_SOURCE_KEY = "gasoline_source";

    private final PlayerEntity player;
    private boolean wounded;
    private int woundCount;
    private int bleedTicks;
    private int rescueTicks;
    private @Nullable UUID rescuerUuid;
    private @Nullable UUID attackerUuid;
    private String weaponItemId = "";
    private boolean gasoline;
    private @Nullable UUID gasolineOwnerUuid;
    private @Nullable UUID gasolineSourceUuid;

    public JasonWoundedPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public boolean isWounded() {
        return this.wounded;
    }

    public int getWoundCount() {
        return this.woundCount;
    }

    public int getBleedTicks() {
        return this.bleedTicks;
    }

    public int getRescueTicks() {
        return this.rescueTicks;
    }

    public @Nullable UUID getRescuerUuid() {
        return this.rescuerUuid;
    }

    public @Nullable UUID getAttackerUuid() {
        return this.attackerUuid;
    }

    public String getWeaponItemId() {
        return this.weaponItemId;
    }

    public boolean isGasoline() {
        return this.gasoline;
    }

    public @Nullable UUID getGasolineOwnerUuid() {
        return this.gasolineOwnerUuid;
    }

    public @Nullable UUID getGasolineSourceUuid() {
        return this.gasolineSourceUuid;
    }

    public void markWounded(int woundCount, int bleedTicks, @Nullable UUID attackerUuid, @NotNull String weaponItemId) {
        /*
         * woundCount 表示“这是第几次被杰森投掷武器打倒”。
         * 第 1/2 次会倒地，第 3 次在实体命中逻辑里直接死亡，不会再调用这里。
         */
        this.wounded = true;
        this.woundCount = Math.max(this.woundCount, Math.max(1, woundCount));
        this.bleedTicks = Math.max(0, bleedTicks);
        this.rescueTicks = 0;
        this.rescuerUuid = null;
        this.attackerUuid = attackerUuid;
        this.weaponItemId = weaponItemId == null ? "" : weaponItemId;
        sync();
    }

    public void copyWoundFrom(@NotNull JasonWoundedPlayerComponent other) {
        /*
         * 双重人格两名玩家共用一套身体语义。
         * 活跃人格被打倒时，休眠人格也必须同步进入相同倒地和救治状态。
         */
        this.wounded = other.wounded;
        this.woundCount = other.woundCount;
        this.bleedTicks = other.bleedTicks;
        this.rescueTicks = other.rescueTicks;
        this.rescuerUuid = other.rescuerUuid;
        this.attackerUuid = other.attackerUuid;
        this.weaponItemId = other.weaponItemId;
        sync();
    }

    public void setBleedTicks(int bleedTicks) {
        this.bleedTicks = Math.max(0, bleedTicks);
        sync();
    }

    public void setRescueProgress(@Nullable UUID rescuerUuid, int rescueTicks) {
        this.rescuerUuid = rescuerUuid;
        this.rescueTicks = Math.max(0, rescueTicks);
        sync();
    }

    public void clearRescueProgress() {
        if (this.rescuerUuid == null && this.rescueTicks == 0) {
            return;
        }
        this.rescuerUuid = null;
        this.rescueTicks = 0;
        sync();
    }

    public void clearWound() {
        if (!this.wounded && this.bleedTicks == 0 && this.rescueTicks == 0 && this.rescuerUuid == null && this.attackerUuid == null) {
            return;
        }
        /*
         * 这里只解除当前“倒地中”状态，不能清 woundCount。
         * 同一局内第一次/第二次被救起后，下一次投掷仍需要按第 2/第 3 次处理；
         * 跨局清零由 reset() 负责，避免把局内倒地次数和局间状态重置混在一起。
         */
        this.wounded = false;
        this.bleedTicks = 0;
        this.rescueTicks = 0;
        this.rescuerUuid = null;
        this.attackerUuid = null;
        this.weaponItemId = "";
        sync();
    }

    public void markGasoline(@Nullable UUID ownerUuid, @NotNull UUID sourceUuid) {
        /*
         * 汽油标记同步给所有客户端，由本能 handler 再判断“观看者是不是杰森”。
         * 这样杰森能被动看到橙色目标，而普通玩家不会因为组件同步直接得到额外信息。
         */
        this.gasoline = true;
        this.gasolineOwnerUuid = ownerUuid;
        this.gasolineSourceUuid = sourceUuid;
        sync();
    }

    public void clearGasoline() {
        if (!this.gasoline && this.gasolineOwnerUuid == null && this.gasolineSourceUuid == null) {
            return;
        }
        this.gasoline = false;
        this.gasolineOwnerUuid = null;
        this.gasolineSourceUuid = null;
        sync();
    }

    public void reset() {
        /*
         * reset() 是回合切换、调试重置和重新分配角色时的硬清理。
         * woundCount 必须在这里归零，否则上一局被杰森击倒过的玩家会把“第几倒”
         * 带到下一局，导致新局第一次命中就按第二倒甚至第三次穿杀结算。
         */
        this.wounded = false;
        this.woundCount = 0;
        this.bleedTicks = 0;
        this.rescueTicks = 0;
        this.rescuerUuid = null;
        this.attackerUuid = null;
        this.weaponItemId = "";
        this.gasoline = false;
        this.gasolineOwnerUuid = null;
        this.gasolineSourceUuid = null;
        sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        // 倒地提示和汽油本能都需要其他客户端知道目标状态，实际显示权限在客户端 handler 里再次判断。
        return true;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean(WOUNDED_KEY, this.wounded);
        tag.putInt(WOUND_COUNT_KEY, this.woundCount);
        tag.putInt(BLEED_TICKS_KEY, this.bleedTicks);
        tag.putInt(RESCUE_TICKS_KEY, this.rescueTicks);
        tag.putString(WEAPON_ITEM_KEY, this.weaponItemId);
        if (this.rescuerUuid != null) {
            tag.putUuid(RESCUER_KEY, this.rescuerUuid);
        }
        if (this.attackerUuid != null) {
            tag.putUuid(ATTACKER_KEY, this.attackerUuid);
        }
        tag.putBoolean(GASOLINE_KEY, this.gasoline);
        if (this.gasolineOwnerUuid != null) {
            tag.putUuid(GASOLINE_OWNER_KEY, this.gasolineOwnerUuid);
        }
        if (this.gasolineSourceUuid != null) {
            tag.putUuid(GASOLINE_SOURCE_KEY, this.gasolineSourceUuid);
        }
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.wounded = tag.getBoolean(WOUNDED_KEY);
        this.woundCount = Math.max(0, tag.getInt(WOUND_COUNT_KEY));
        this.bleedTicks = Math.max(0, tag.getInt(BLEED_TICKS_KEY));
        this.rescueTicks = Math.max(0, tag.getInt(RESCUE_TICKS_KEY));
        this.weaponItemId = tag.getString(WEAPON_ITEM_KEY);
        this.rescuerUuid = tag.containsUuid(RESCUER_KEY) ? tag.getUuid(RESCUER_KEY) : null;
        this.attackerUuid = tag.containsUuid(ATTACKER_KEY) ? tag.getUuid(ATTACKER_KEY) : null;
        this.gasoline = tag.getBoolean(GASOLINE_KEY);
        this.gasolineOwnerUuid = tag.containsUuid(GASOLINE_OWNER_KEY) ? tag.getUuid(GASOLINE_OWNER_KEY) : null;
        this.gasolineSourceUuid = tag.containsUuid(GASOLINE_SOURCE_KEY) ? tag.getUuid(GASOLINE_SOURCE_KEY) : null;
    }
}
