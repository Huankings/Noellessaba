package org.agmas.noellesroles.roles.spring_trap;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

/**
 * 弹簧陷阱玩家运行态。
 *
 * <p>目前只记录“血斧这次冷却是不是开局 30 秒冷却”。客户端 tooltip 只能从
 * {@link net.minecraft.entity.player.ItemCooldownManager} 拿到剩余比例，不能反推出这次冷却总长。
 * 如果没有这层同步标记，血斧开局 30 秒冷却会被客户端误按普通 45 秒冷却显示。</p>
 */
public final class SpringTrapPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<SpringTrapPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(NoellesRolesCore.MOD_ID, "spring_trap"),
            SpringTrapPlayerComponent.class
    );

    private final PlayerEntity player;
    private int bloodAxeStartCooldownTicks = 0;

    public SpringTrapPlayerComponent(@NotNull PlayerEntity player) {
        this.player = player;
    }

    /**
     * 开局时启动血斧 30 秒初始冷却来源标记。
     *
     * <p>真正阻止玩家使用血斧的是 ItemCooldownManager；这里仅告诉客户端：
     * 当前冷却应按 30 秒总长换算 tooltip 倒计时。</p>
     */
    public void startRoundCooldowns() {
        this.bloodAxeStartCooldownTicks = SpringTrapConstants.BLOOD_AXE_START_COOLDOWN_TICKS;
        sync();
    }

    /**
     * 提供给客户端 tooltip 使用，判断血斧当前是否仍处于“开局 30 秒冷却”阶段。
     */
    public boolean isUsingStartCooldown(@NotNull Item item) {
        return item == ModItems.BLOOD_AXE && this.bloodAxeStartCooldownTicks > 0;
    }

    /**
     * 血斧写入普通 45 秒冷却，或外部效果清掉血斧冷却时，应同步清掉开局来源标记。
     *
     * <p>否则玩家在开局冷却被提前刷新后立刻击杀目标，tooltip 会误把新的 45 秒普通冷却当作
     * 30 秒开局冷却显示。</p>
     */
    public void clearBloodAxeStartCooldown() {
        if (this.bloodAxeStartCooldownTicks <= 0) {
            return;
        }

        this.bloodAxeStartCooldownTicks = 0;
        sync();
    }

    /**
     * 回合重置或重新分配为弹簧陷阱时，先清掉旧的血斧冷却来源与实际物品冷却。
     */
    public void reset() {
        this.bloodAxeStartCooldownTicks = 0;
        this.player.getItemCooldownManager().remove(ModItems.BLOOD_AXE);
        sync();
    }

    @Override
    public void serverTick() {
        if (this.bloodAxeStartCooldownTicks <= 0) {
            return;
        }

        this.bloodAxeStartCooldownTicks--;
        /*
         * 开局冷却来源只需要在开始和结束两个边界同步。
         * 倒计时数字仍由客户端 ItemCooldownManager 的剩余比例连续计算，避免每 tick 同步组件。
         */
        if (this.bloodAxeStartCooldownTicks == 0) {
            sync();
        }
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
        buf.writeBoolean(this.bloodAxeStartCooldownTicks > 0);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.bloodAxeStartCooldownTicks = buf.readBoolean() ? 1 : 0;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("bloodAxeStartCooldownTicks", this.bloodAxeStartCooldownTicks);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.bloodAxeStartCooldownTicks = tag.getInt("bloodAxeStartCooldownTicks");
    }
}
