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
 * <p>Wathe 的 {@code GameTimeComponent.time} 是对局剩余时间，会从起点向下递减。
 * 扒手服务端发钱和客户端 HUD 都需要知道“本局第一次进入扒手结算逻辑时的剩余时间”，
 * 因此这里把起点放在世界组件里同步给客户端，而不是让客户端自己猜。</p>
 */
public class AvariciousPayoutComponent implements AutoSyncedComponent {
    public static final ComponentKey<AvariciousPayoutComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(NoellesRolesCore.MOD_ID, "avaricious_payout"),
            AvariciousPayoutComponent.class
    );

    private final World world;
    private int timerStartTime = -1;

    public AvariciousPayoutComponent(@NotNull World world) {
        this.world = world;
    }

    public boolean hasTimerStartTime() {
        return this.timerStartTime >= 0;
    }

    public int getTimerStartTime() {
        return this.timerStartTime;
    }

    public void setTimerStartTime(int timerStartTime) {
        this.timerStartTime = timerStartTime;
        sync();
    }

    public void reset() {
        this.timerStartTime = -1;
        sync();
    }

    public void sync() {
        KEY.sync(this.world);
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("timer_start_time", this.timerStartTime);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.timerStartTime = tag.contains("timer_start_time") ? tag.getInt("timer_start_time") : -1;
    }
}
