package org.agmas.noellesroles.roles.hunter;

import org.agmas.noellesroles.registry.NoellesRolesCore;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
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
 * 猎刀又额外有开局 30 秒冷却，需要让 tooltip 知道当前冷却来源。
 * 因此这些状态不能只放在物品局部变量里，必须通过玩家组件跨 tick 保存并同步给本人。</p>
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
    private int huntingKnifeStartCooldownTicks = 0;

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

        if (this.huntingKnifeStartCooldownTicks > 0) {
            this.huntingKnifeStartCooldownTicks--;
            if (this.huntingKnifeStartCooldownTicks == 0) {
                changed = true;
            }
        }

        /*
         * knifeTicks 会被客户端 tooltip 用来判断“这次是松开疾跑举刀产生的临时冷却”，
         * 开局冷却来源只需要在结束边界同步一次；releaseGraceTicks 只服务端校验用，不需要同步。
         */
        if (changed) {
            this.sync();
        }
    }

    /**
     * 开局时启动猎刀 30 秒初始冷却标记。
     *
     * <p>这里仅记录“当前冷却来源是开局冷却”，真正禁止使用的倒计时仍由
     * {@link net.minecraft.entity.player.ItemCooldownManager} 写入。
     * 这样客户端 tooltip 才能按 30 秒总长显示，而不是误按命中后的 45 秒冷却换算。</p>
     */
    public void startRoundCooldowns() {
        this.huntingKnifeStartCooldownTicks = HunterConstants.HUNTING_KNIFE_START_COOLDOWN_TICKS;
        sync();
    }

    /**
     * 提供给客户端 tooltip 使用，判断猎刀当前是否仍处于“开局 30 秒冷却”阶段。
     */
    public boolean isUsingStartCooldown(@NotNull Item item) {
        return item == ModItems.HUNTING_KNIFE && this.huntingKnifeStartCooldownTicks > 0;
    }

    /**
     * 能力或外部效果刷新猎刀冷却时，要同步清掉开局冷却来源标记。
     */
    public void clearHuntingKnifeStartCooldown() {
        if (this.huntingKnifeStartCooldownTicks <= 0) {
            return;
        }

        this.huntingKnifeStartCooldownTicks = 0;
        sync();
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
        this.huntingKnifeStartCooldownTicks = 0;
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
        tag.putInt("huntingKnifeStartCooldownTicks", this.huntingKnifeStartCooldownTicks);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.@NotNull WrapperLookup registryLookup) {
        this.knifeTicks = tag.contains("knifeTicks") ? tag.getInt("knifeTicks") : 0;
        this.isUseKnife = tag.contains("isUseKnife") && tag.getBoolean("isUseKnife");
        this.isSprinting = tag.contains("isSprinting") && tag.getBoolean("isSprinting");
        this.releaseGraceTicks = tag.contains("releaseGraceTicks") ? tag.getInt("releaseGraceTicks") : 0;
        this.huntingKnifeStartCooldownTicks = tag.contains("huntingKnifeStartCooldownTicks")
                ? tag.getInt("huntingKnifeStartCooldownTicks")
                : 0;
    }
}
