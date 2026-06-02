package org.agmas.noellesroles.roles.assassin;

import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

/**
 * 刺客组件。
 *
 * <p>当前它主要负责两类刺客专属状态：</p>
 * <p>1. 刺刀 / 无声左轮的开局 30 秒初始冷却；</p>
 * <p>2. “刺刀冷却刷新”这类即时商店行为。</p>
 *
 * <p>之所以不只依赖 {@link net.minecraft.entity.player.ItemCooldownManager}，
 * 是因为 tooltip 需要知道“这次冷却的总时长到底是 30 秒、35 秒还是 15 秒”。
 * 单靠冷却管理器只能拿到当前比例，无法区分是哪一种来源的冷却。</p>
 */
public class AssassinPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {

    public static final ComponentKey<AssassinPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "assassin"),
            AssassinPlayerComponent.class
    );

    public static final int ASSASSIN_START_COOLDOWN_TICKS = GameConstants.getInTicks(0, 30);

    private final PlayerEntity player;
    private int bayonetStartCooldownTicks = 0;
    private int silencedRevolverStartCooldownTicks = 0;

    public AssassinPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    /**
     * 开局时同步启动刺刀与无声左轮的 30 秒初始冷却标记。
     *
     * <p>这里只负责“标记当前冷却来源”，真正的物品冷却时间仍由
     * {@link net.minecraft.entity.player.ItemCooldownManager} 负责写入，
     * 这样 tooltip 与实际禁用时长就能保持一致。</p>
     */
    public void startRoundCooldowns() {
        this.bayonetStartCooldownTicks = ASSASSIN_START_COOLDOWN_TICKS;
        this.silencedRevolverStartCooldownTicks = ASSASSIN_START_COOLDOWN_TICKS;
        sync();
    }

    /**
     * 提供给客户端 tooltip 使用，判断某个物品当前是否还处于“开局 30 秒冷却”阶段。
     */
    public boolean isUsingStartCooldown(Item item) {
        if (item == ModItems.BAYONET) {
            return this.bayonetStartCooldownTicks > 0;
        }
        if (item == ModItems.SILENCED_REVOLVER) {
            return this.silencedRevolverStartCooldownTicks > 0;
        }
        return false;
    }

    /**
     * 当玩家购买“刺刀冷却刷新”时，如果当前刷新的正好是开局冷却，
     * 还需要把这层专属标记一起清掉，否则前端会继续按 30 秒冷却去显示比例。
     */
    public void clearBayonetStartCooldown() {
        if (this.bayonetStartCooldownTicks <= 0) {
            return;
        }

        this.bayonetStartCooldownTicks = 0;
        sync();
    }

    /**
     * 回合重置时清空刺客的专属冷却状态。
     */
    public void reset() {
        this.bayonetStartCooldownTicks = 0;
        this.silencedRevolverStartCooldownTicks = 0;
        this.player.getItemCooldownManager().remove(ModItems.BAYONET);
        this.player.getItemCooldownManager().remove(ModItems.SILENCED_REVOLVER);
        this.player.getItemCooldownManager().remove(ModItems.SILENT_GRENADE);
        sync();
    }

    /**
     * 购买刺刀冷却刷新图标时立即执行。
     *
     * <p>只有刺刀此刻真的处于冷却时才允许刷新，
     * 这样可以避免玩家白白购买一个没有实际收益的即时道具。</p>
     */
    public static boolean tryRefreshBayonetCooldown(@NotNull PlayerEntity player) {
        if (!player.getItemCooldownManager().isCoolingDown(ModItems.BAYONET)) {
            player.sendMessage(Text.translatable("shop.noellesroles.bayonet_refresh_unavailable").withColor(0xAA0000), true);
            return false;
        }

        player.getItemCooldownManager().remove(ModItems.BAYONET);

        /*
         * 刺刀可能刷新的不是普通 35 秒冷却，而是开局 30 秒冷却。
         * 这里顺手把组件里的来源标记也一并清掉，避免 tooltip 残留旧状态。
         */
        KEY.get(player).clearBayonetStartCooldown();
        return true;
    }

    @Override
    public void serverTick() {
        boolean changed = false;

        if (this.bayonetStartCooldownTicks > 0) {
            this.bayonetStartCooldownTicks--;
            if (this.bayonetStartCooldownTicks == 0) {
                changed = true;
            }
        }

        if (this.silencedRevolverStartCooldownTicks > 0) {
            this.silencedRevolverStartCooldownTicks--;
            if (this.silencedRevolverStartCooldownTicks == 0) {
                changed = true;
            }
        }

        // 只在“开局冷却已结束”这个边界同步一次，避免每 tick 都发同步包。
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
        buf.writeBoolean(this.bayonetStartCooldownTicks > 0);
        buf.writeBoolean(this.silencedRevolverStartCooldownTicks > 0);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.bayonetStartCooldownTicks = buf.readBoolean() ? 1 : 0;
        this.silencedRevolverStartCooldownTicks = buf.readBoolean() ? 1 : 0;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("bayonetStartCooldownTicks", this.bayonetStartCooldownTicks);
        tag.putInt("silencedRevolverStartCooldownTicks", this.silencedRevolverStartCooldownTicks);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.bayonetStartCooldownTicks = tag.getInt("bayonetStartCooldownTicks");
        this.silencedRevolverStartCooldownTicks = tag.getInt("silencedRevolverStartCooldownTicks");
    }
}
