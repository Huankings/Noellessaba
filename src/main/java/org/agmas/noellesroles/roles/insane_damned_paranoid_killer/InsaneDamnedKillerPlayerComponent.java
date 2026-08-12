package org.agmas.noellesroles.roles.insane_damned_paranoid_killer;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

/**
 * 亡语杀手局内状态。
 *
 * <p>目前只保存尸体伪装开关。它必须同步给所有客户端，因为其他玩家客户端需要用这份状态来：</p>
 * <p>1. 把亡语杀手渲染成躺尸姿态；</p>
 * <p>2. 隐藏准心名字；</p>
 * <p>3. 阻止本地准心把“躺着的活人”识别成可锁定目标。</p>
 *
 * <p>这里不写回放字段，也不持有持续时间 / 冷却。spark 版尸体模式就是单纯开关，
 * 本需求也明确要求避免回放刷屏，所以组件只做权威状态同步。</p>
 */
public class InsaneDamnedKillerPlayerComponent implements AutoSyncedComponent {
    public static final ComponentKey<InsaneDamnedKillerPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            NoellesRolesCore.id("insane_damned_paranoid_killer"),
            InsaneDamnedKillerPlayerComponent.class
    );

    private final PlayerEntity player;
    private boolean corpseMode = false;

    public InsaneDamnedKillerPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    /**
     * 便捷读取，供移动 / 碰撞 / 渲染 / HUD 等多个入口共用同一份判断。
     */
    public static boolean isCorpseMode(@NotNull PlayerEntity player) {
        return KEY.get(player).isCorpseMode();
    }

    /**
     * 读取“当前真正应该生效的尸体伪装”。
     *
     * <p>组件本身只保存布尔值，原因是时停者回溯、CCA 同步和 NBT 都更适合保存纯状态。
     * 但移动、碰撞、目标隐藏和客户端姿态这些实际效果必须额外确认当前职业：
     * 如果玩家被重分配、转职或某次快照恢复出了旧的布尔值，非亡语杀手也不应继承尸体模式效果。</p>
     */
    public static boolean isActiveCorpseMode(@NotNull PlayerEntity player) {
        return isCorpseMode(player)
                && GameWorldComponent.KEY.get(player.getWorld()).isRole(
                player,
                NoellesRoleRegistry.THE_INSANE_DAMNED_PARANOID_KILLER_OF_DOOM_DEATH_DESTRUCTION_AND_WAFFLES
        );
    }

    public boolean isCorpseMode() {
        return this.corpseMode;
    }

    /**
     * 能力键调用的开关入口。
     *
     * <p>这里故意不碰 {@code AbilityPlayerComponent} 冷却，也不写 GameRecordManager。
     * 这样它和 spark 版一样可以随时开关，同时满足“不记录回放事件”的要求。</p>
     */
    public void toggleCorpseMode() {
        setCorpseMode(!this.corpseMode);
    }

    /**
     * 设置尸体伪装状态并同步。
     *
     * <p>所有真实变更都从这里走，避免有的清理路径忘记同步，
     * 造成客户端还把玩家渲染成尸体或继续隐藏准心目标。</p>
     */
    public void setCorpseMode(boolean corpseMode) {
        if (this.corpseMode == corpseMode) {
            return;
        }
        this.corpseMode = corpseMode;
        sync();
    }

    /**
     * 回合重置、角色重分配、死亡确认后统一清理。
     */
    public void reset() {
        setCorpseMode(false);
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        /*
         * 其他玩家必须知道目标是否处于尸体伪装，才能在本地正确处理渲染、准心和交互。
         * 因此这里对所有客户端公开同步，不做“只同步给本人”的限制。
         */
        return true;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeBoolean(this.corpseMode);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.corpseMode = buf.readBoolean();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("corpseMode", this.corpseMode);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.corpseMode = tag.contains("corpseMode") && tag.getBoolean("corpseMode");
    }
}
