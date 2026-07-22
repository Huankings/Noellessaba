package org.agmas.noellesroles.roles.drugmaker;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

/**
 * 制毒师物品冷却来源状态。
 *
 * <p>吹矢和毒液注射器的普通使用冷却是 45 秒，但开局保护冷却只有 30 秒。
 * 客户端 tooltip 只能从 {@link net.minecraft.entity.player.ItemCooldownManager} 拿到“剩余比例”，
 * 不知道这次冷却究竟是 30 秒还是 45 秒，所以需要用组件额外同步“当前是否仍是开局冷却”。</p>
 */
public class DrugmakerPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<DrugmakerPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "drugmaker"),
            DrugmakerPlayerComponent.class
    );

    private final PlayerEntity player;
    private int blowgunStartCooldownTicks = 0;
    private int poisonInjectorStartCooldownTicks = 0;

    public DrugmakerPlayerComponent(@NotNull PlayerEntity player) {
        this.player = player;
    }

    /**
     * 开局时启动两件制毒师专属物品的 30 秒来源标记。
     *
     * <p>真正的禁用时间仍由 ItemCooldownManager 写入；这里仅负责告诉客户端这次冷却的总长，
     * 避免 UI 误按普通 45 秒冷却去换算倒计时。</p>
     */
    public void startRoundCooldowns() {
        this.blowgunStartCooldownTicks = DrugmakerConstants.START_COOLDOWN_TICKS;
        this.poisonInjectorStartCooldownTicks = DrugmakerConstants.START_COOLDOWN_TICKS;
        sync();
    }

    /**
     * 提供给 tooltip 使用，判断某个物品当前是否仍处于“开局 30 秒冷却”阶段。
     */
    public boolean isUsingStartCooldown(@NotNull Item item) {
        if (item == ModItems.BLOWGUN) {
            return this.blowgunStartCooldownTicks > 0;
        }
        if (item == ModItems.POISON_INJECTOR) {
            return this.poisonInjectorStartCooldownTicks > 0;
        }
        return false;
    }

    /**
     * 外部效果如果提前清掉物品冷却，也必须同步清掉来源标记。
     *
     * <p>否则玩家在开局 30 秒内被刷新后立刻使用道具，新写入的 45 秒普通冷却
     * 仍会被前端误识别为开局冷却。</p>
     */
    public void clearStartCooldowns() {
        boolean changed = this.blowgunStartCooldownTicks > 0 || this.poisonInjectorStartCooldownTicks > 0;
        this.blowgunStartCooldownTicks = 0;
        this.poisonInjectorStartCooldownTicks = 0;
        if (changed) {
            sync();
        }
    }

    /**
     * 回合重置或重新分配身份时，清掉制毒师专属冷却状态和实际物品冷却。
     */
    public void reset() {
        this.blowgunStartCooldownTicks = 0;
        this.poisonInjectorStartCooldownTicks = 0;
        this.player.getItemCooldownManager().remove(ModItems.BLOWGUN);
        this.player.getItemCooldownManager().remove(ModItems.POISON_INJECTOR);
        sync();
    }

    @Override
    public void serverTick() {
        boolean changed = false;

        if (this.blowgunStartCooldownTicks > 0) {
            this.blowgunStartCooldownTicks--;
            changed |= this.blowgunStartCooldownTicks == 0;
        }

        if (this.poisonInjectorStartCooldownTicks > 0) {
            this.poisonInjectorStartCooldownTicks--;
            changed |= this.poisonInjectorStartCooldownTicks == 0;
        }

        // 只在“开局来源标记消失”的边界同步，避免每 tick 给客户端刷包。
        if (changed) {
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
        buf.writeBoolean(this.blowgunStartCooldownTicks > 0);
        buf.writeBoolean(this.poisonInjectorStartCooldownTicks > 0);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.blowgunStartCooldownTicks = buf.readBoolean() ? 1 : 0;
        this.poisonInjectorStartCooldownTicks = buf.readBoolean() ? 1 : 0;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.@NotNull WrapperLookup registryLookup) {
        tag.putInt("blowgunStartCooldownTicks", this.blowgunStartCooldownTicks);
        tag.putInt("poisonInjectorStartCooldownTicks", this.poisonInjectorStartCooldownTicks);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.@NotNull WrapperLookup registryLookup) {
        this.blowgunStartCooldownTicks = tag.contains("blowgunStartCooldownTicks")
                ? tag.getInt("blowgunStartCooldownTicks")
                : 0;
        this.poisonInjectorStartCooldownTicks = tag.contains("poisonInjectorStartCooldownTicks")
                ? tag.getInt("poisonInjectorStartCooldownTicks")
                : 0;
    }
}
