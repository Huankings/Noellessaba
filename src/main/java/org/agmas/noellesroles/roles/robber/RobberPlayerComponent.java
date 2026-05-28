package org.agmas.noellesroles.roles.robber;

import dev.doctor4t.wathe.game.GameConstants;
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
 * 强盗玩家组件。
 * 这里只负责管理“开局 30 秒专属冷却”这件事，
 * 用来给客户端 tooltip 提供正确的总时长判断，避免显示成匕首的 60 秒比例冷却。
 */
public class RobberPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {

    public static final ComponentKey<RobberPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "robber"),
            RobberPlayerComponent.class
    );

    public static final int ROBBER_START_COOLDOWN_TICKS = GameConstants.getInTicks(0, 30);

    private final PlayerEntity player;
    private int throwingAxeStartCooldownTicks = 0;
    private int robberPistolStartCooldownTicks = 0;

    public RobberPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    /**
     * 开局时同时启动飞斧与强盗手枪的 30 秒初始冷却标记。
     */
    public void startRoundCooldowns() {
        this.throwingAxeStartCooldownTicks = ROBBER_START_COOLDOWN_TICKS;
        this.robberPistolStartCooldownTicks = ROBBER_START_COOLDOWN_TICKS;
        sync();
    }

    /**
     * 回合重置时清空强盗的特殊冷却状态。
     */
    public void reset() {
        this.throwingAxeStartCooldownTicks = 0;
        this.robberPistolStartCooldownTicks = 0;
        this.player.getItemCooldownManager().remove(ModItems.THROWING_AXE);
        this.player.getItemCooldownManager().remove(ModItems.ROBBER_PISTOL);
        sync();
    }

    /**
     * 提供给客户端 tooltip 使用，判断当前是否仍处于“开局 30 秒冷却”阶段。
     */
    public boolean isUsingStartCooldown(Item item) {
        if (item == ModItems.THROWING_AXE) {
            return this.throwingAxeStartCooldownTicks > 0;
        }
        if (item == ModItems.ROBBER_PISTOL) {
            return this.robberPistolStartCooldownTicks > 0;
        }
        return false;
    }

    @Override
    public void serverTick() {
        boolean changed = false;

        if (this.throwingAxeStartCooldownTicks > 0) {
            this.throwingAxeStartCooldownTicks--;
            if (this.throwingAxeStartCooldownTicks == 0) {
                changed = true;
            }
        }

        if (this.robberPistolStartCooldownTicks > 0) {
            this.robberPistolStartCooldownTicks--;
            if (this.robberPistolStartCooldownTicks == 0) {
                changed = true;
            }
        }

        // 只在状态切换时同步一次，避免每 tick 无意义刷包。
        if (changed) {
            sync();
        }
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return this.player.equals(player);
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeBoolean(this.throwingAxeStartCooldownTicks > 0);
        buf.writeBoolean(this.robberPistolStartCooldownTicks > 0);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.throwingAxeStartCooldownTicks = buf.readBoolean() ? 1 : 0;
        this.robberPistolStartCooldownTicks = buf.readBoolean() ? 1 : 0;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("throwingAxeStartCooldownTicks", this.throwingAxeStartCooldownTicks);
        tag.putInt("robberPistolStartCooldownTicks", this.robberPistolStartCooldownTicks);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.throwingAxeStartCooldownTicks = tag.getInt("throwingAxeStartCooldownTicks");
        this.robberPistolStartCooldownTicks = tag.getInt("robberPistolStartCooldownTicks");
    }
}
