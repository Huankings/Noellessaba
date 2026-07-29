package org.agmas.noellesroles.roles.timekeeper;

import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.item.TimekeeperWatchItem;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.engineer.StunnedPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 时停者世界级状态。
 *
 * <p>时间回溯会影响整局世界，而不是某一个玩家自己的背包状态。
 * 因此快照历史、回溯播放游标、保护名单都放在世界组件里；
 * 玩家组件只保存个人冷却、光阴收入计时和是否已购买保护。</p>
 */
public class TimekeeperWorldComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<TimekeeperWorldComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(NoellesRolesCore.MOD_ID, "timekeeper_world"),
            TimekeeperWorldComponent.class
    );

    private final World world;
    private boolean rewinding = false;
    private final List<TimekeeperSnapshots.GlobalSnapshot> snapshots = new ArrayList<>();
    private final Set<UUID> protectedPlayers = new HashSet<>();
    private int snapshotTicker = 0;
    private int playbackCursor = -1;
    private int targetIndex = -1;
    private RewindActorPostUseState actorPostUseState = null;

    public TimekeeperWorldComponent(World world) {
        this.world = world;
    }

    public boolean isRewinding() {
        return this.rewinding;
    }

    public boolean isProtectedFromCurrentRewind(@NotNull ServerPlayerEntity player) {
        return this.rewinding && this.protectedPlayers.contains(player.getUuid());
    }

    public boolean isProtectedFromCurrentRewind(@NotNull UUID playerUuid) {
        return this.rewinding && this.protectedPlayers.contains(playerUuid);
    }

    public boolean shouldBlockCommunication(@NotNull ServerPlayerEntity player) {
        TimekeeperPlayerComponent playerComponent = TimekeeperPlayerComponent.KEY.get(player);
        if (playerComponent.isInTimeRift()) {
            return true;
        }

        /*
         * protectedPlayers 表示“本次回溯不参与倒放”的最终名单。
         * 它既包含已购买并消费回溯保护的玩家，也包含未参局玩家、以及本次回溯区间无法复活的普通死者。
         * 没有进入名单、并且当前仍作为活人参与回溯的玩家，回溯播放期间不能说话也不能听语音。
         */
        return this.rewinding
                && !this.protectedPlayers.contains(player.getUuid())
                && dev.doctor4t.wathe.game.GameFunctions.isPlayerAliveAndSurvival(player);
    }

    public boolean canStartRewind(@NotNull ServerPlayerEntity player) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        return gameWorld.isRunning() && !this.rewinding && !this.snapshots.isEmpty();
    }

    /**
     * 启动一次全局时间回溯。
     *
     * <p>这里把“历史缓存长度”和“单次回溯深度”拆开：
     * 快照列表最多保留 120 秒；每次发动只把游标从最新快照倒到 30 秒前。
     * 回溯结束后只删除到达点之后的快照，保留到达点之前的旧历史前缀，
     * 后续新快照继续接在这个前缀后面，允许之后的回溯继续跨过第一次到达点往更早处倒。</p>
     */
    public boolean tryStartRewind(@NotNull ServerPlayerEntity player, boolean elegantWatch) {
        if (!canStartRewind(player)) {
            player.sendMessage(coloredActionbar("message.noellesroles.timekeeper.rewind_blocked"), true);
            return false;
        }

        int latestIndex = this.snapshots.size() - 1;
        this.targetIndex = Math.max(0, latestIndex - TimekeeperConstants.SINGLE_REWIND_SNAPSHOTS);
        this.playbackCursor = latestIndex;
        this.rewinding = true;
        this.protectedPlayers.clear();
        collectConsumedProtectionPlayers(player.getServerWorld());
        freezeUnprotectedPlayers(player.getServerWorld());
        sync();
        return true;
    }

    /**
     * 记录发动者在“成功扣费、写冷却、可能破碎”之后的时停者专属状态。
     *
     * <p>发动者如果没有买回溯保护，位置、背包和状态仍会被快照回滚；
     * 但发动回溯本身的代价不能被 30 秒前的快照抵消。
     * 因此世界组件每次应用快照后都会把这份后置状态重新压回去：
     * 光阴已扣、怀表冷却已进、普通濒毁怀表已破碎。</p>
     */
    public void rememberActorPostUseState(@NotNull ServerPlayerEntity actor, @NotNull ItemStack usedWatch) {
        this.actorPostUseState = RewindActorPostUseState.capture(actor, usedWatch);
    }

    public void finishRewind() {
        /*
         * 明确以 targetIndex 作为播放终点，而不是靠 snapshots 是否清空判断。
         * 删除到达点之后的历史可以切断“已经被改写的未来”，同时保留到达点之前的旧前缀，
         * 这正是用户要求的：旧历史前缀 <= 回溯到达点 + 回溯后的新时间线。
         */
        if (this.targetIndex >= 0 && this.targetIndex < this.snapshots.size()) {
            while (this.snapshots.size() > this.targetIndex + 1) {
                this.snapshots.removeLast();
            }
        }

        this.rewinding = false;
        this.playbackCursor = -1;
        this.targetIndex = -1;
        this.protectedPlayers.clear();
        this.actorPostUseState = null;
        sync();
    }

    public void reset() {
        this.rewinding = false;
        this.snapshots.clear();
        this.protectedPlayers.clear();
        this.snapshotTicker = 0;
        this.playbackCursor = -1;
        this.targetIndex = -1;
        this.actorPostUseState = null;
        sync();
    }

    @Override
    public void serverTick() {
        if (!(this.world instanceof ServerWorld serverWorld)) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(serverWorld);
        if (!gameWorld.isRunning()) {
            if (!this.snapshots.isEmpty() || this.rewinding) {
                reset();
            }
            return;
        }

        if (this.rewinding) {
            tickRewind(serverWorld);
            return;
        }

        this.snapshotTicker++;
        if (this.snapshotTicker >= TimekeeperConstants.SNAPSHOT_INTERVAL_TICKS) {
            this.snapshotTicker = 0;
            this.snapshots.add(TimekeeperSnapshots.capture(serverWorld));
            while (this.snapshots.size() > TimekeeperConstants.MAX_HISTORY_SNAPSHOTS) {
                this.snapshots.removeFirst();
            }
        }
    }

    public static void reduceWatchCooldownsForAliveTimekeepers(@NotNull ServerWorld world, int ticks) {
        if (ticks <= 0) {
            return;
        }
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (!gameWorld.isRole(player, NoellesRoleRegistry.TIMEKEEPER)
                    || !dev.doctor4t.wathe.game.GameFunctions.isPlayerAliveAndSurvival(player)) {
                continue;
            }
            TimekeeperPlayerComponent.KEY.get(player).reduceAllWatchCooldowns(ticks);
        }
    }

    private void tickRewind(@NotNull ServerWorld serverWorld) {
        if (this.playbackCursor < 0 || this.targetIndex < 0 || this.playbackCursor >= this.snapshots.size()) {
            finishRewind();
            return;
        }

        TimekeeperSnapshots.GlobalSnapshot snapshot = this.snapshots.get(this.playbackCursor);
        snapshot.apply(serverWorld, this.protectedPlayers);
        if (this.actorPostUseState != null) {
            this.actorPostUseState.restore(serverWorld);
        }
        /*
         * 冻结必须在快照恢复之后刷新。
         * 玩家组件快照里包含 StunnedPlayerComponent；如果先冻结再回写快照，
         * 30 秒前的旧定身状态会把“回溯期间强制冻结”覆盖掉。
         */
        freezeUnprotectedPlayers(serverWorld);

        if (this.playbackCursor <= this.targetIndex) {
            finishRewind();
            return;
        }
        this.playbackCursor--;
    }

    private void collectConsumedProtectionPlayers(@NotNull ServerWorld serverWorld) {
        boolean timekeeperProtectionConsumed = false;
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(serverWorld);

        collectAutomaticRewindExclusionPlayers(serverWorld);

        for (ServerPlayerEntity player : serverWorld.getPlayers()) {
            if (this.protectedPlayers.contains(player.getUuid())) {
                continue;
            }

            TimekeeperPlayerComponent component = TimekeeperPlayerComponent.KEY.get(player);
            if (!component.consumeRewindProtectionForRewind()) {
                continue;
            }

            this.protectedPlayers.add(player.getUuid());
            if (gameWorld.isRole(player, NoellesRoleRegistry.TIMEKEEPER)) {
                timekeeperProtectionConsumed = true;
            }
        }

        if (!timekeeperProtectionConsumed) {
            return;
        }

        /*
         * 敲钟人联动：只要时停者购买的本次回溯保护被消费，
         * 场上仍存活的敲钟人也会一起进入保护名单。
         * 这里不要求敲钟人自己购买商店项，避免联动福利变成额外操作负担。
         */
        for (ServerPlayerEntity player : serverWorld.getPlayers()) {
            if (gameWorld.isRole(player, NoellesRoleRegistry.BELLRINGER)
                    && dev.doctor4t.wathe.game.GameFunctions.isPlayerAliveAndSurvival(player)) {
                this.protectedPlayers.add(player.getUuid());
            }
        }
    }

    private void collectAutomaticRewindExclusionPlayers(@NotNull ServerWorld serverWorld) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(serverWorld);

        for (ServerPlayerEntity player : serverWorld.getPlayers()) {
            UUID playerUuid = player.getUuid();
            TimekeeperPlayerComponent component = TimekeeperPlayerComponent.KEY.get(player);

            /*
             * 先排除未参局玩家。
             * 快照捕获的是 serverWorld.getPlayers()，因此管理员旁观、调试账号或中途进服但没有职业的人
             * 也可能被写进历史。如果他们不在本轮 GameWorldComponent 的角色表里，就不应该被时停者回溯拉动、
             * 冻结、切语音或从旁观状态“复活”。
             */
            if (gameWorld.getRole(player) == null) {
                this.protectedPlayers.add(playerUuid);
                continue;
            }

            /*
             * 时间狭缝玩家不能自动保护。
             * 狭缝里的死者虽然通常是 spectator，但他们正是时停者回溯要优先救回的对象；
             * 如果把 inTimeRift 玩家加入 protectedPlayers，快照恢复会跳过他们，30 秒内死亡就无法被回溯救回。
             */
            if (component.isInTimeRift()) {
                continue;
            }

            /*
             * 当前仍被 Wathe 判定为存活的玩家会正常参与本次回溯。
             * 他们是否有商店回溯保护，后面的 consumeRewindProtectionForRewind() 会单独处理。
             */
            if (dev.doctor4t.wathe.game.GameFunctions.isPlayerAliveAndSurvival(player)) {
                continue;
            }

            /*
             * 普通死亡旁观是否参与本次回溯，不再看“现在是不是已经死透”，而是看本次播放区间
             * [targetIndex, playbackCursor] 里是否存在该玩家的“真实存活快照”。
             *
             * - 如果区间内没有真实存活快照：这次回溯无论怎么倒都救不回他，继续倒放他的死亡旁观相机、
             *   背包和状态只会造成体验干扰，所以本次把他当作自动保护跳过。
             * - 如果区间内存在真实存活快照：说明这次回溯能直接跨过他的死亡点，把他拉回活人状态；
             *   因此不能加入 protectedPlayers，必须让 TimekeeperSnapshots.apply(...) 一帧帧恢复他，
             *   等播放游标抵达那张真实存活快照时自然复活。
             *
             * 这里的“真实存活”排除了特殊存活旁观、创造和旁观模式，避免把时间狭缝本身误判成复活点。
             */
            if (!canCurrentRewindRestorePlayableAlive(playerUuid)) {
                this.protectedPlayers.add(playerUuid);
            }
        }
    }

    private boolean canCurrentRewindRestorePlayableAlive(@NotNull UUID playerUuid) {
        if (this.targetIndex < 0 || this.playbackCursor < 0 || this.snapshots.isEmpty()) {
            return false;
        }

        int startIndex = Math.max(0, this.targetIndex);
        int endIndex = Math.min(this.playbackCursor, this.snapshots.size() - 1);
        if (startIndex > endIndex) {
            return false;
        }

        /*
         * 只扫描“本次已经确定要播放”的 30 秒区间。
         * 第一次回溯如果只倒到某个死者“下一次也许能被救”的位置，但本次区间里还没有他的真实活人快照，
         * 那么这一次仍然保护他；等下一次回溯启动时，再按新的 [targetIndex, playbackCursor] 重新判断。
         *
         * 从 endIndex 往 startIndex 扫描，顺序和实际回溯播放方向一致。这里只需要知道区间里“有没有”
         * 可复活点，因此找到第一张真实存活快照后即可返回。
         */
        for (int i = endIndex; i >= startIndex; i--) {
            if (this.snapshots.get(i).hasPlayableAliveSnapshot(playerUuid)) {
                return true;
            }
        }
        return false;
    }

    private void freezeUnprotectedPlayers(@NotNull ServerWorld serverWorld) {
        for (ServerPlayerEntity player : serverWorld.getPlayers()) {
            if (this.protectedPlayers.contains(player.getUuid())
                    || !dev.doctor4t.wathe.game.GameFunctions.isPlayerAliveAndSurvival(player)) {
                continue;
            }

            /*
             * 回溯开始前玩家可能已经处于“持续使用物品”的状态，例如怀表蓄力、Derringer/REVOLVER
             * 的右键使用链路或其他长按道具。未受保护玩家在回溯期间会被快照不断覆盖，
             * 如果服务端还保留 active item，回溯结束后 Minecraft 可能把这段旧蓄力当成一次新的松手/短按。
             *
             * clearActiveItem() 只清除“正在使用”的标记，不调用 onStoppedUsing / finishUsing，
             * 因此不会像 stopUsingItem() 那样触发投掷、开枪或完成蓄力效果；它只是把回溯冻结期间
             * 不应该继续存在的输入状态归零，和客户端的按键清理配套防止结束后连点。
             */
            if (player.isUsingItem()) {
                player.clearActiveItem();
            }
            StunnedPlayerComponent.KEY.get(player).stun(TimekeeperConstants.REWIND_FREEZE_REFRESH_TICKS, null);
        }
    }

    private static Text coloredActionbar(String translationKey) {
        return Text.translatable(translationKey)
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(TimekeeperConstants.ROLE_COLOR)));
    }

    public void sync() {
        KEY.sync(this.world);
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("rewinding", this.rewinding);

        /*
         * 保护名单原本只服务端使用；现在客户端也需要知道“本地玩家是否不参与本次回溯倒放”，
         * 用来决定是否要清掉攻击/使用键。只同步 UUID，不同步购买待消费标记：
         * 购买状态仍由 TimekeeperPlayerComponent 管，避免客户端误把“已购买但本次尚未消费”
         * 当成本次回溯保护。未参局玩家和本次不可复活死者也会出现在这里，避免客户端输入锁误拦他们。
         */
        NbtList protectedPlayerList = new NbtList();
        for (UUID protectedPlayer : this.protectedPlayers) {
            NbtCompound entry = new NbtCompound();
            entry.putUuid("uuid", protectedPlayer);
            protectedPlayerList.add(entry);
        }
        tag.put("protectedPlayers", protectedPlayerList);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.rewinding = tag.contains("rewinding") && tag.getBoolean("rewinding");
        this.snapshotTicker = 0;
        this.playbackCursor = -1;
        this.targetIndex = -1;
        this.protectedPlayers.clear();
        if (this.rewinding && tag.contains("protectedPlayers")) {
            NbtList protectedPlayerList = tag.getList("protectedPlayers", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < protectedPlayerList.size(); i++) {
                NbtCompound entry = protectedPlayerList.getCompound(i);
                if (entry.containsUuid("uuid")) {
                    this.protectedPlayers.add(entry.getUuid("uuid"));
                }
            }
        }
        this.actorPostUseState = null;
    }

    private static final class RewindActorPostUseState {
        private final UUID actorUuid;
        private final NbtCompound timekeeperData;
        private final int timeBalance;
        private final int selectedSlot;
        private final ItemStack usedWatch;

        private RewindActorPostUseState(
                @NotNull UUID actorUuid,
                @NotNull NbtCompound timekeeperData,
                int timeBalance,
                int selectedSlot,
                @NotNull ItemStack usedWatch
        ) {
            this.actorUuid = actorUuid;
            this.timekeeperData = timekeeperData;
            this.timeBalance = timeBalance;
            this.selectedSlot = selectedSlot;
            this.usedWatch = usedWatch;
        }

        private static @NotNull RewindActorPostUseState capture(@NotNull ServerPlayerEntity actor, @NotNull ItemStack usedWatch) {
            NbtCompound timekeeperData = new NbtCompound();
            TimekeeperPlayerComponent.KEY.get(actor).writeToNbt(timekeeperData, actor.getRegistryManager());
            return new RewindActorPostUseState(
                    actor.getUuid(),
                    timekeeperData,
                    PlayerShopComponent.KEY.get(actor).getCurrencyAmount(TimekeeperConstants.TIME_CURRENCY_ID),
                    actor.getInventory().selectedSlot,
                    usedWatch.copy()
            );
        }

        private void restore(@NotNull ServerWorld world) {
            ServerPlayerEntity actor = world.getServer().getPlayerManager().getPlayer(this.actorUuid);
            if (actor == null) {
                return;
            }

            TimekeeperPlayerComponent timekeeperComponent = TimekeeperPlayerComponent.KEY.get(actor);
            timekeeperComponent.readFromNbt(this.timekeeperData.copy(), actor.getRegistryManager());
            timekeeperComponent.sync();

            PlayerShopComponent.KEY.get(actor).setCurrencyAmount(TimekeeperConstants.TIME_CURRENCY_ID, this.timeBalance);

            if (!this.usedWatch.isEmpty() && this.usedWatch.isOf(ModItems.DYING_WATCH)) {
                int slot = Math.max(0, Math.min(this.selectedSlot, actor.getInventory().size() - 1));
                actor.getInventory().setStack(slot, this.usedWatch.copy());
                TimekeeperWatchItem.setState(actor.getInventory().getStack(slot), TimekeeperWatchItem.getState(this.usedWatch));
                TimekeeperWatchItem.setMode(actor.getInventory().getStack(slot), TimekeeperWatchItem.getMode(this.usedWatch));
                actor.getInventory().selectedSlot = Math.min(slot, 8);
                actor.getInventory().markDirty();
                actor.currentScreenHandler.sendContentUpdates();
            }
        }
    }
}
