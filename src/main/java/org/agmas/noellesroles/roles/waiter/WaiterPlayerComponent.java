package org.agmas.noellesroles.roles.waiter;

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

/**
 * 存在每个玩家身上的“可被服务员短暂透视”状态。
 *
 * <p>这个组件不是给服务员本人存能力，而是给“刚完成心情任务的目标玩家”存倒计时。
 * 客户端的 WaiterInstinctHandler 会读取目标玩家的 visibleTicks，只有观看者是服务员时才显示职业色透视。</p>
 */
public class WaiterPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<WaiterPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "waiter"),
            WaiterPlayerComponent.class
    );

    private final PlayerEntity player;
    private int visibleTicks = 0;

    public WaiterPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void revealToWaiters() {
        // 多次完成任务时刷新到至少 4 秒，而不是叠加无限时长。
        this.visibleTicks = Math.max(this.visibleTicks, WaiterConstants.VISIBLE_TICKS);
        this.sync();
    }

    public boolean isVisibleToWaiters() {
        return this.visibleTicks > 0;
    }

    public void reset() {
        this.visibleTicks = 0;
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public void serverTick() {
        // 倒计时只在服务端推进；客户端通过组件同步拿到当前是否可见。
        if (this.visibleTicks <= 0) {
            return;
        }

        this.visibleTicks--;
        if (this.visibleTicks == 0) {
            this.sync();
        }
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        // 同步给所有玩家，再由客户端 instinct handler 判断“观看者是不是服务员”。
        return true;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("visibleTicks", this.visibleTicks);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.visibleTicks = Math.max(0, tag.getInt("visibleTicks"));
    }
}
