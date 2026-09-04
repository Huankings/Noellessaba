package org.agmas.noellesroles.roles.hunter;

import org.agmas.noellesroles.registry.NoellesRolesCore;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModItems;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

/**
 * 追猎者猎刀状态。
 *
 * <p>猎刀的“举刀时间”同时影响客户端提示、临时冷却和服务端命中校验；
 * 这些命中校验状态需要通过玩家组件跨 tick 保存并同步给本人；开局物品冷却本身则完全交给
 * ItemCooldownManager，tooltip 直接读取同一条真实记录。</p>
 */
public class HunterPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {

    public static final ComponentKey<HunterPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(NoellesRolesCore.MOD_ID, "hunter"),
            HunterPlayerComponent.class
    );

    private final PlayerEntity player;
    public boolean isUseKnife = false;
    public boolean isSprinting = false;
    public int knifeTicks = 0;
    private int releaseGraceTicks = 0;

    public HunterPlayerComponent(@NotNull PlayerEntity player) {
        this.player = player;
    }

    @Override
    public void serverTick() {
        boolean changed = false;

        if (this.isUseKnife && this.knifeTicks <= HunterConstants.HUNTING_KNIFE_MAX_USE_TICKS) {
            this.knifeTicks++;
            changed = true;
        }

        if (this.releaseGraceTicks > 0) {
            this.releaseGraceTicks--;
        }

        // knifeTicks 同时服务客户端蓄力表现和服务端命中校验；releaseGraceTicks 只在服务端使用。
        if (changed) {
            this.sync();
        }
    }

    public void useHuntingKnife(boolean sprinting) {
        this.isUseKnife = true;
        this.isSprinting = sprinting;
        this.knifeTicks = 0;
        this.releaseGraceTicks = 0;
        this.sync();
    }

    public void markReleasedForHit() {
        this.releaseGraceTicks = HunterConstants.HUNTING_KNIFE_SERVER_RELEASE_GRACE_TICKS;
    }

    public boolean canAcceptReleasedHit() {
        return (this.isUseKnife || this.releaseGraceTicks > 0)
                && this.knifeTicks >= HunterConstants.HUNTING_KNIFE_MIN_USE_TICKS
                && this.knifeTicks <= HunterConstants.HUNTING_KNIFE_MAX_USE_TICKS;
    }

    public void stopHuntingKnife() {
        this.isSprinting = false;
        this.isUseKnife = false;
        this.sync();
    }

    public void reset() {
        this.isSprinting = false;
        this.isUseKnife = false;
        this.knifeTicks = 0;
        this.releaseGraceTicks = 0;
        this.player.getItemCooldownManager().remove(ModItems.HUNTING_KNIFE);
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return this.player.equals(player);
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.@NotNull WrapperLookup registryLookup) {
        tag.putInt("knifeTicks", this.knifeTicks);
        tag.putBoolean("isUseKnife", this.isUseKnife);
        tag.putBoolean("isSprinting", this.isSprinting);
        tag.putInt("releaseGraceTicks", this.releaseGraceTicks);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.@NotNull WrapperLookup registryLookup) {
        this.knifeTicks = tag.contains("knifeTicks") ? tag.getInt("knifeTicks") : 0;
        this.isUseKnife = tag.contains("isUseKnife") && tag.getBoolean("isUseKnife");
        this.isSprinting = tag.contains("isSprinting") && tag.getBoolean("isSprinting");
        this.releaseGraceTicks = tag.contains("releaseGraceTicks") ? tag.getInt("releaseGraceTicks") : 0;
    }
}
