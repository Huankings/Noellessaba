package org.agmas.noellesroles.roles.avaricious;

import org.agmas.noellesroles.registry.NoellesRolesCore;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

/**
 * 扒手世界级结算计时组件。
 *
 * <p>扒手的周期必须独立于 Wathe 的对局剩余时间。
 * Wathe 允许其它机制增加或减少对局时间，但这些操作不应该改变扒手已经开始的偷钱周期；
 * 因此这里直接保存独立的剩余 tick，并由扒手服务端处理器每 tick 推进。</p>
 */
public class AvariciousPayoutComponent implements AutoSyncedComponent {
    public static final ComponentKey<AvariciousPayoutComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(NoellesRolesCore.MOD_ID, "avaricious_payout"),
            AvariciousPayoutComponent.class
    );

    private final World world;
    /** 尚未开始计时为 -1；开始后只由扒手自己的服务端 tick 递减。 */
    private int remainingTicks = -1;

    public AvariciousPayoutComponent(@NotNull World world) {
        this.world = world;
    }

    public boolean hasTimer() {
        return this.remainingTicks >= 0;
    }

    public int getRemainingTicks() {
        return Math.max(0, this.remainingTicks);
    }

    /** 开始一个完整偷钱周期；不读取 GameTimeComponent，避免受到 Wathe 加时/减时影响。 */
    public void startTimer() {
        this.remainingTicks = AvariciousConstants.TIMER_TICKS;
        sync();
    }

    /**
     * 推进独立倒计时。
     *
     * @return 本次 tick 后是否到达结算点
     */
    public boolean tickTimer() {
        if (this.remainingTicks < 0) {
            return false;
        }
        if (this.remainingTicks > 0) {
            this.remainingTicks--;
        }
        // HUD 直接读取该组件，因此每个 tick 都同步，确保显示和实际结算完全一致。
        sync();
        return this.remainingTicks == 0;
    }

    public void reset() {
        this.remainingTicks = -1;
        sync();
    }

    public void sync() {
        KEY.sync(this.world);
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("remaining_ticks", this.remainingTicks);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.remainingTicks = tag.contains("remaining_ticks")
                ? tag.getInt("remaining_ticks")
                : -1;
    }
}
