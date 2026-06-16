package org.agmas.noellesroles.roles.magician;

import com.mojang.authlib.GameProfile;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheEntities;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.NoellesRolesEntities;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.entities.MagicianPlaybackEntity;
import org.agmas.noellesroles.roles.coroner.BodyDeathReasonComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 魔术师播放阶段的统一服务端管理器。
 *
 * <p>这条链统一负责：</p>
 * <p>1. 生成 / 驱动可见皮套；</p>
 * <p>2. 维护一个只存在于服务端的代理玩家，去执行回放交互；</p>
 * <p>3. 统一处理自然结束、主动结束、被玩法武器强制结束；</p>
 * <p>4. 在强制结束时按普通玩家尸体逻辑生成尸体，并覆写职业显示为魔术师。</p>
 */
public final class MagicianPlaybackManager {

    private static final Map<UUID, ActivePlayback> ACTIVE_PLAYBACKS = new ConcurrentHashMap<>();
    private static final Map<Integer, UUID> PLAYBACK_ENTITY_TO_OWNER = new ConcurrentHashMap<>();
    private static final Map<UUID, RecordingData> READY_RECORDINGS = new ConcurrentHashMap<>();
    private static boolean initialized = false;

    private MagicianPlaybackManager() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        ServerTickEvents.END_SERVER_TICK.register(MagicianPlaybackManager::tickServer);
    }

    public static void startPlayback(@NotNull ServerPlayerEntity player) {
        MagicianPlayerComponent component = MagicianPlayerComponent.KEY.get(player);
        RecordingData recordingData = RecordingData.fromComponent(component);
        if (recordingData == null) {
            recordingData = READY_RECORDINGS.get(player.getUuid());
        }
        if ((!component.isReadyPlayback() && recordingData == null) || recordingData == null) {
            return;
        }

        stopPlaybackSilently(player);

        ActivePlayback playback = ActivePlayback.create(player, component, recordingData);
        if (playback == null) {
            return;
        }

        component.beginPlaybackLock(recordingData.durationTicks());
        ACTIVE_PLAYBACKS.put(player.getUuid(), playback);
        if (!playback.spawnVisibleEntity()) {
            /*
             * 极端情况下世界可能拒绝生成实体。
             *
             * 之前 create() 会先把实体塞进世界，再继续准备代理玩家和动作表；
             * 如果后续步骤失败，就会出现“实体闪一下、没有播放事件、随后被清理”的半初始化状态。
             * 现在只有在完整上下文创建成功后才真正 spawn；若 spawn 仍失败，就立刻回滚阶段。
             */
            ACTIVE_PLAYBACKS.remove(player.getUuid());
            playback.cleanup();
            component.finishPlaybackStage();
            return;
        }
        READY_RECORDINGS.remove(player.getUuid());
        PLAYBACK_ENTITY_TO_OWNER.put(playback.visibleEntity.getId(), player.getUuid());

        NbtCompound extra = new NbtCompound();
        extra.putUuid("disguise_player", playback.disguiseUuid);
        extra.putString("disguise_name", playback.disguiseName);
        GameRecordManager.recordGlobalEvent(
                player.getServerWorld(),
                Noellesroles.MAGICIAN_PLAYBACK_STARTED_EVENT,
                player,
                extra
        );
    }

    /**
     * 录制完成后，把本轮播放所需数据放进服务端稳定缓存。
     *
     * <p>组件里的轨迹帧、动作和背包快照都是 transient 字段，
     * 理论上只存在服务端内存里；但 CCA 同步、玩家数据读写或其他 reset 链路
     * 一旦把这些字段清掉，客户端 HUD 仍可能停在“按 G 播放”，服务端却因为
     * recordedFrames 为空直接 return，表现就是按键完全没反应。
     *
     * <p>因此录制结束时额外保存一份只在服务端使用的缓存。缓存不写入存档，
     * 新录制、重置、播放成功结束时都会清理，不会跨局污染。</p>
     */
    public static void cacheFinishedRecording(@NotNull ServerPlayerEntity player, @NotNull MagicianPlayerComponent component) {
        RecordingData recordingData = RecordingData.fromComponent(component);
        if (recordingData == null) {
            return;
        }
        READY_RECORDINGS.put(player.getUuid(), recordingData);
    }

    public static boolean hasCachedRecording(@NotNull PlayerEntity player) {
        return READY_RECORDINGS.containsKey(player.getUuid());
    }

    public static void clearCachedRecording(@NotNull PlayerEntity player) {
        READY_RECORDINGS.remove(player.getUuid());
    }

    public static void stopPlaybackEarly(@NotNull ServerPlayerEntity player) {
        stopPlayback(player, StopReason.MANUAL, null);
    }

    public static void stopPlaybackSilently(@NotNull net.minecraft.entity.player.PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            stopPlayback(serverPlayer, StopReason.SILENT, null);
        }
    }

    public static void stopPlaybackByWeaponHit(
            @NotNull MagicianPlaybackEntity playbackEntity,
            @Nullable ServerPlayerEntity attacker,
            @NotNull String weaponName,
            @NotNull Identifier deathReason
    ) {
        UUID ownerUuid = playbackEntity.getMagicianOwnerUuid();
        if (ownerUuid == null || playbackEntity.getServer() == null) {
            return;
        }
        ServerPlayerEntity magician = playbackEntity.getServer().getPlayerManager().getPlayer(ownerUuid);
        if (magician == null) {
            return;
        }

        boolean activePlayback = ACTIVE_PLAYBACKS.containsKey(ownerUuid);
        if (activePlayback
                && attacker != null
                && !attacker.getUuid().equals(ownerUuid)
                && MagicianConstants.PLAYBACK_FORCED_END_REWARD_COINS > 0) {
            /*
             * 皮套被其他玩家打碎，本质上说明魔术师成功骗出了对方一次武器/攻击判断。
             * 这里给魔术师一笔小额补偿；自己打碎自己的皮套不奖励，避免自刷金币。
             */
            PlayerShopComponent.KEY.get(magician).addToBalance(MagicianConstants.PLAYBACK_FORCED_END_REWARD_COINS);
        }

        String attackerName = attacker == null ? "未知玩家" : attacker.getGameProfile().getName();
        stopPlayback(
                magician,
                StopReason.FORCED,
                new MagicianPlaybackHitInfo(
                        attacker == null ? null : attacker.getUuid(),
                        attackerName,
                        weaponName,
                        deathReason
                )
        );
    }

    public static int getPlaybackTicksRemaining(@NotNull net.minecraft.entity.player.PlayerEntity player) {
        ActivePlayback playback = ACTIVE_PLAYBACKS.get(player.getUuid());
        return playback == null ? 0 : playback.remainingTicks;
    }

    public static int getPlaybackPerformedTicks(@NotNull net.minecraft.entity.player.PlayerEntity player) {
        ActivePlayback playback = ACTIVE_PLAYBACKS.get(player.getUuid());
        return playback == null ? 0 : playback.performedTicks;
    }

    /**
     * 从魔术师玩家组件兜底推进某个拥有者的播放。
     *
     * <p>正常播放由 {@link ServerTickEvents#END_SERVER_TICK} 统一驱动。
     * 但你这次测试出现了“实体生成后定在原地、不自然结束”的现象，
     * 所以这里额外提供一个由组件 tick 调用的单人入口。
     * {@link ActivePlayback#tickAndShouldFinish()} 内部会按世界时间去重，
     * 因此同一 tick 被两个入口同时调用也不会导致播放加速。</p>
     */
    public static void tickPlaybackForOwner(@NotNull ServerPlayerEntity magician) {
        ActivePlayback playback = ACTIVE_PLAYBACKS.get(magician.getUuid());
        if (playback == null) {
            MagicianPlayerComponent component = MagicianPlayerComponent.KEY.get(magician);
            if (component.isPlaying()) {
                component.finishPlaybackStage();
            }
            cleanupOrphanPlaybackEntities(magician.getServerWorld());
            return;
        }

        if (playback.tickAndShouldFinish()) {
            stopPlayback(magician, StopReason.NATURAL, null);
        }
    }

    public static boolean isPlaybackOwner(@NotNull UUID magicianUuid) {
        return ACTIVE_PLAYBACKS.containsKey(magicianUuid);
    }

    public static boolean isPlaybackEntity(@Nullable net.minecraft.entity.Entity entity) {
        return entity instanceof MagicianPlaybackEntity;
    }

    public static @Nullable MagicianPlaybackEntity findPlaybackEntity(@Nullable net.minecraft.entity.Entity entity) {
        return entity instanceof MagicianPlaybackEntity magicianPlaybackEntity ? magicianPlaybackEntity : null;
    }

    public static @Nullable UUID findPlaybackOwner(@Nullable net.minecraft.entity.Entity entity) {
        if (entity instanceof MagicianPlaybackEntity playbackEntity) {
            return playbackEntity.getMagicianOwnerUuid();
        }
        return entity == null ? null : PLAYBACK_ENTITY_TO_OWNER.get(entity.getId());
    }

    private static void tickServer(MinecraftServer server) {
        List<UUID> finishedOwners = new ArrayList<>();
        for (Map.Entry<UUID, ActivePlayback> entry : ACTIVE_PLAYBACKS.entrySet()) {
            if (entry.getValue().tickAndShouldFinish()) {
                finishedOwners.add(entry.getKey());
            }
        }

        for (UUID ownerUuid : finishedOwners) {
            ServerPlayerEntity magician = server.getPlayerManager().getPlayer(ownerUuid);
            if (magician != null) {
                stopPlayback(magician, StopReason.NATURAL, null);
            } else {
                ActivePlayback playback = ACTIVE_PLAYBACKS.remove(ownerUuid);
                if (playback != null) {
                    PLAYBACK_ENTITY_TO_OWNER.remove(playback.visibleEntity.getId());
                    playback.cleanup();
                }
            }
        }

        /*
         * 兜底清理没有 manager 接管的“孤儿皮套”。
         *
         * 旧实现里如果播放阶段没被正常推进或清理，实体会留在世界里；
         * 因为它又拦截了原版伤害，结果就会出现 /kill 显示命中但实体仍在的幽灵皮套。
         * 这里每 tick 扫一次，修复版加载后会自动清掉旧残留。
         */
        for (ServerWorld world : server.getWorlds()) {
            cleanupOrphanPlaybackEntities(world);
        }
    }

    /**
     * 清理已经不属于任何有效播放流程的皮套实体。
     */
    public static void cleanupOrphanPlaybackEntities(@NotNull ServerWorld world) {
        for (MagicianPlaybackEntity entity : world.getEntitiesByType(
                NoellesRolesEntities.MAGICIAN_PLAYBACK_ENTITY_TYPE,
                ignored -> true
        )) {
            UUID ownerUuid = entity.getMagicianOwnerUuid();
            ActivePlayback activePlayback = ownerUuid == null ? null : ACTIVE_PLAYBACKS.get(ownerUuid);
            if (activePlayback != null) {
                if (activePlayback.visibleEntity == entity) {
                    continue;
                }
                PLAYBACK_ENTITY_TO_OWNER.remove(entity.getId());
                entity.discard();
                continue;
            }

            if (entity.age < MagicianConstants.ORPHAN_CLEANUP_GRACE_TICKS) {
                /*
                 * 刚生成的实体给一个很短的缓冲期。
                 *
                 * 这主要防止启动时序抖动：服务端生成、实体追踪同步和播放表注册
                 * 可能在同一个 tick 的不同回调里交错。旧残留实体 age 通常已经很大，
                 * 仍然会在保护期结束后被正常清掉。
                 */
                continue;
            }

            ServerPlayerEntity owner = ownerUuid == null ? null : world.getServer().getPlayerManager().getPlayer(ownerUuid);
            if (owner != null) {
                MagicianPlayerComponent component = MagicianPlayerComponent.KEY.get(owner);
                if (component.isPlaying()) {
                    component.finishPlaybackStage();
                }
            }

            PLAYBACK_ENTITY_TO_OWNER.remove(entity.getId());
            entity.discard();
        }
    }

    /**
     * 回合结束、地图重置等大清理场景使用的强制清空。
     *
     * <p>和 {@link #cleanupOrphanPlaybackEntities(ServerWorld)} 不同，这里不会保留正在播放的有效皮套。
     * 对局已经结束时，所有播放上下文、实体 id 映射和可见皮套都必须一起清掉，
     * 否则下一把就可能看到上一把留下来的“幽灵皮套”。</p>
     */
    public static void cleanupAllPlaybackEntities(@NotNull ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            READY_RECORDINGS.remove(player.getUuid());
        }

        List<UUID> ownersToRemove = new ArrayList<>();
        for (Map.Entry<UUID, ActivePlayback> entry : ACTIVE_PLAYBACKS.entrySet()) {
            ActivePlayback playback = entry.getValue();
            if (playback.visibleEntity.getWorld() == world) {
                PLAYBACK_ENTITY_TO_OWNER.remove(playback.visibleEntity.getId());
                playback.cleanup();
                ownersToRemove.add(entry.getKey());
            }
        }

        for (UUID ownerUuid : ownersToRemove) {
            ACTIVE_PLAYBACKS.remove(ownerUuid);
            ServerPlayerEntity owner = world.getServer().getPlayerManager().getPlayer(ownerUuid);
            if (owner != null) {
                MagicianPlayerComponent component = MagicianPlayerComponent.KEY.get(owner);
                if (component.isPlaying()) {
                    component.finishPlaybackStage();
                }
            }
        }

        for (MagicianPlaybackEntity entity : world.getEntitiesByType(
                NoellesRolesEntities.MAGICIAN_PLAYBACK_ENTITY_TYPE,
                ignored -> true
        )) {
            PLAYBACK_ENTITY_TO_OWNER.remove(entity.getId());
            entity.discard();
        }
    }

    private static void stopPlayback(
            @NotNull ServerPlayerEntity magician,
            @NotNull StopReason stopReason,
            @Nullable MagicianPlaybackHitInfo hitInfo
    ) {
        ActivePlayback playback = ACTIVE_PLAYBACKS.remove(magician.getUuid());
        MagicianPlayerComponent component = MagicianPlayerComponent.KEY.get(magician);
        boolean shouldCleanupStage = component.isPlaying() || playback != null;
        if (!shouldCleanupStage) {
            return;
        }

        if (playback != null) {
            PLAYBACK_ENTITY_TO_OWNER.remove(playback.visibleEntity.getId());
        }

        if (playback != null && stopReason == StopReason.FORCED && hitInfo != null) {
            spawnPlaybackCorpse(playback, hitInfo);
        }
        if (playback != null) {
            playback.cleanup();
        }

        UUID lockedDisguiseUuid = component.playbackDisguiseUuid == null ? component.getSelectedTarget() : component.playbackDisguiseUuid;
        String lockedDisguiseName = component.playbackDisguiseName == null || component.playbackDisguiseName.isBlank()
                ? component.getSelectedTargetName()
                : component.playbackDisguiseName;
        component.finishPlaybackStage();
        READY_RECORDINGS.remove(magician.getUuid());
        if (stopReason != StopReason.SILENT) {
            component.applyPlaybackCooldown();
        }

        if (stopReason == StopReason.SILENT) {
            return;
        }

        if (stopReason == StopReason.FORCED && hitInfo != null) {
            NbtCompound extra = new NbtCompound();
            extra.putUuid("disguise_player", lockedDisguiseUuid);
            extra.putString("disguise_name", lockedDisguiseName);
            if (hitInfo.attackerUuid() != null) {
                extra.putUuid("attacker_player", hitInfo.attackerUuid());
            }
            if (hitInfo.attackerName() != null) {
                extra.putString("attacker_name", hitInfo.attackerName());
            }
            extra.putString("weapon_name", hitInfo.weaponName());
            GameRecordManager.recordGlobalEvent(
                    magician.getServerWorld(),
                    Noellesroles.MAGICIAN_PLAYBACK_FORCED_END_EVENT,
                    magician,
                    extra
            );
            return;
        }

        GameRecordManager.recordGlobalEvent(
                magician.getServerWorld(),
                stopReason == StopReason.MANUAL
                        ? Noellesroles.MAGICIAN_PLAYBACK_STOPPED_EARLY_EVENT
                        : Noellesroles.MAGICIAN_PLAYBACK_FINISHED_EVENT,
                magician,
                null
        );
    }

    private static void spawnPlaybackCorpse(@NotNull ActivePlayback playback, @NotNull MagicianPlaybackHitInfo hitInfo) {
        PlayerBodyEntity body = WatheEntities.PLAYER_BODY.create(playback.magician.getWorld());
        if (body == null) {
            return;
        }

        body.setPlayerUuid(playback.disguiseUuid);
        body.refreshPositionAndAngles(
                playback.visibleEntity.getX(),
                playback.visibleEntity.getY(),
                playback.visibleEntity.getZ(),
                playback.visibleEntity.getHeadYaw(),
                0.0F
        );
        body.setYaw(playback.visibleEntity.getHeadYaw());
        body.setHeadYaw(playback.visibleEntity.getHeadYaw());
        body.prevYaw = playback.visibleEntity.getHeadYaw();
        body.prevHeadYaw = playback.visibleEntity.getHeadYaw();
        body.prevBodyYaw = playback.visibleEntity.bodyYaw;
        body.bodyYaw = playback.visibleEntity.bodyYaw;
        playback.magician.getWorld().spawnEntity(body);

        BodyDeathReasonComponent deathComponent = BodyDeathReasonComponent.KEY.get(body);
        deathComponent.deathReason = hitInfo.deathReason();
        deathComponent.playerRole = Noellesroles.MAGICIAN.identifier();
        deathComponent.sync();
    }

    private enum StopReason {
        NATURAL,
        MANUAL,
        FORCED,
        SILENT
    }

    /**
     * 录制完成后可播放的数据快照。
     *
     * <p>它刻意只保存“录制内容”，不保存皮套目标。
     * 这样魔术师仍然可以在录制完成后继续切换背包选择，播放时再读取当前选中的玩家。</p>
     */
    private static final class RecordingData {
        private final MagicianInventorySnapshot inventorySnapshot;
        private final List<MagicianReplayFrame> frames;
        private final List<MagicianRecordedAction> actions;

        private RecordingData(
                @NotNull MagicianInventorySnapshot inventorySnapshot,
                @NotNull List<MagicianReplayFrame> frames,
                @NotNull List<MagicianRecordedAction> actions
        ) {
            this.inventorySnapshot = inventorySnapshot;
            this.frames = frames;
            this.actions = actions;
        }

        private static @Nullable RecordingData fromComponent(@NotNull MagicianPlayerComponent component) {
            if (component.inventorySnapshot == null || component.recordedFrames.isEmpty()) {
                return null;
            }
            return new RecordingData(
                    component.inventorySnapshot,
                    new ArrayList<>(component.recordedFrames),
                    new ArrayList<>(component.recordedActions)
            );
        }

        private int durationTicks() {
            return this.frames.size();
        }

        private @NotNull MagicianReplayFrame firstFrame() {
            return this.frames.getFirst();
        }
    }

    /**
     * 一次进行中的播放上下文。
     */
    private static final class ActivePlayback {
        private final ServerPlayerEntity magician;
        private final MagicianPlayerComponent component;
        private final MagicianPlaybackEntity visibleEntity;
        private final MagicianPlaybackFakePlayer proxyPlayer;
        private final MagicianInventorySnapshot inventorySnapshot;
        private final List<MagicianReplayFrame> frames;
        private final Map<Integer, List<MagicianRecordedAction>> actionsByTick;
        private final UUID disguiseUuid;
        private final String disguiseName;
        private int frameIndex = 0;
        private int remainingTicks;
        private int performedTicks = 0;
        private long lastTickedWorldTime = Long.MIN_VALUE;

        private ActivePlayback(
                @NotNull ServerPlayerEntity magician,
                @NotNull MagicianPlayerComponent component,
                @NotNull MagicianPlaybackEntity visibleEntity,
                @NotNull MagicianPlaybackFakePlayer proxyPlayer,
                @NotNull MagicianInventorySnapshot inventorySnapshot,
                @NotNull List<MagicianReplayFrame> frames,
                @NotNull Map<Integer, List<MagicianRecordedAction>> actionsByTick,
                @NotNull UUID disguiseUuid,
                @NotNull String disguiseName
        ) {
            this.magician = magician;
            this.component = component;
            this.visibleEntity = visibleEntity;
            this.proxyPlayer = proxyPlayer;
            this.inventorySnapshot = inventorySnapshot;
            this.frames = frames;
            this.actionsByTick = actionsByTick;
            this.disguiseUuid = disguiseUuid;
            this.disguiseName = disguiseName;
            this.remainingTicks = frames.size();
        }

        private static @Nullable ActivePlayback create(
                @NotNull ServerPlayerEntity magician,
                @NotNull MagicianPlayerComponent component,
                @NotNull RecordingData recordingData
        ) {
            ServerWorld world = magician.getServerWorld();
            MagicianInventorySnapshot inventorySnapshot = recordingData.inventorySnapshot;

            UUID disguiseUuid = component.getSelectedTarget();
            String disguiseName = component.getSelectedTargetName();
            MagicianReplayFrame firstFrame = recordingData.firstFrame();

            MagicianPlaybackEntity visibleEntity = new MagicianPlaybackEntity(org.agmas.noellesroles.NoellesRolesEntities.MAGICIAN_PLAYBACK_ENTITY_TYPE, world);
            visibleEntity.setPlaybackIdentity(magician.getUuid(), disguiseUuid, disguiseName);
            visibleEntity.refreshPositionAndAngles(firstFrame.x(), firstFrame.y(), firstFrame.z(), firstFrame.yaw(), firstFrame.pitch());
            visibleEntity.setHeadYaw(firstFrame.headYaw());
            visibleEntity.bodyYaw = firstFrame.bodyYaw();
            visibleEntity.setReplaySitting(firstFrame.sitting());
            firstFrame.syncVisibleEquipmentTo(visibleEntity);
            applyEntityUseState(visibleEntity, firstFrame, false);

            GameProfile proxyProfile = new GameProfile(magician.getUuid(), "[MagicianReplay] " + disguiseName);
            if (component.playbackProfileSnapshot != null) {
                component.playbackProfileSnapshot.getProperties().forEach((key, property) -> proxyProfile.getProperties().put(key, property));
            } else if (component.selectedTargetProfileSnapshot != null) {
                component.selectedTargetProfileSnapshot.getProperties().forEach((key, property) -> proxyProfile.getProperties().put(key, property));
            }

            MagicianPlaybackFakePlayer proxyPlayer = new MagicianPlaybackFakePlayer(
                    world,
                    proxyProfile,
                    magician.getUuid(),
                    disguiseUuid,
                    disguiseName
            );
            proxyPlayer.refreshPositionAndAngles(firstFrame.x(), firstFrame.y(), firstFrame.z(), firstFrame.yaw(), firstFrame.pitch());
            proxyPlayer.setHeadYaw(firstFrame.headYaw());
            proxyPlayer.bodyYaw = firstFrame.bodyYaw();
            firstFrame.applyInventoryTo(proxyPlayer);
            applyPlayerUseState(proxyPlayer, firstFrame, false);
            MagicianPlaybackActionExecutor.syncTransientState(magician, proxyPlayer);

            Map<Integer, List<MagicianRecordedAction>> actionsByTick = new HashMap<>();
            for (MagicianRecordedAction action : recordingData.actions) {
                actionsByTick.computeIfAbsent(action.tick, ignored -> new ArrayList<>()).add(action);
            }

            return new ActivePlayback(
                    magician,
                    component,
                    visibleEntity,
                    proxyPlayer,
                    inventorySnapshot,
                    new ArrayList<>(recordingData.frames),
                    actionsByTick,
                    disguiseUuid,
                    disguiseName
            );
        }

        private boolean spawnVisibleEntity() {
            return this.magician.getServerWorld().spawnEntity(this.visibleEntity);
        }

        private boolean tickAndShouldFinish() {
            long worldTime = this.magician.getServerWorld().getTime();
            if (this.lastTickedWorldTime == worldTime) {
                return false;
            }
            this.lastTickedWorldTime = worldTime;

            if (this.visibleEntity.isRemoved()) {
                return true;
            }
            if (!GameFunctions.isPlayerAliveAndSurvival(this.magician)) {
                cleanup();
                return true;
            }
            if (this.frameIndex >= this.frames.size()) {
                return true;
            }

            MagicianReplayFrame frame = this.frames.get(this.frameIndex);
            List<MagicianRecordedAction> actions = this.actionsByTick.get(this.frameIndex);
            applyFrame(frame, hasReleaseUseAction(actions));

            if (actions != null) {
                for (MagicianRecordedAction action : actions) {
                    MagicianPlaybackActionExecutor.performAction(this.magician, this.proxyPlayer, this.visibleEntity, action, this.disguiseName);
                }
                syncVisibleStateFromProxy(this.proxyPlayer, this.visibleEntity);
            }

            this.frameIndex++;
            this.performedTicks = this.frameIndex;
            this.remainingTicks = Math.max(0, this.frames.size() - this.frameIndex);
            return this.frameIndex >= this.frames.size();
        }

        private void applyFrame(@NotNull MagicianReplayFrame frame, boolean deferStoppingUseUntilAction) {
            applyEntityFrame(this.visibleEntity, frame);
            applyPlayerFrame(this.proxyPlayer, frame, deferStoppingUseUntilAction);
            MagicianPlaybackActionExecutor.syncTransientState(this.magician, this.proxyPlayer);
        }

        private void cleanup() {
            this.visibleEntity.discard();
        }

        private static void applyEntityFrame(@NotNull MagicianPlaybackEntity entity, @NotNull MagicianReplayFrame frame) {
            frame.syncVisibleEquipmentTo(entity);
            entity.refreshPositionAndAngles(frame.x(), frame.y(), frame.z(), frame.yaw(), frame.pitch());
            entity.setHeadYaw(frame.headYaw());
            entity.bodyYaw = frame.bodyYaw();
            entity.setSneaking(frame.sneaking());
            entity.setSprinting(frame.sprinting());
            entity.setOnGround(frame.onGround());
            entity.setReplaySitting(frame.sitting());
            applySleepingState(entity, frame);
            entity.setPose(parsePose(frame.poseName()));
            applyEntityUseState(entity, frame, false);
            entity.noClip = true;
        }

        private static void applyPlayerFrame(
                @NotNull MagicianPlaybackFakePlayer proxyPlayer,
                @NotNull MagicianReplayFrame frame,
                boolean deferStoppingUseUntilAction
        ) {
            frame.applyInventoryTo(proxyPlayer);
            proxyPlayer.refreshPositionAndAngles(frame.x(), frame.y(), frame.z(), frame.yaw(), frame.pitch());
            proxyPlayer.setHeadYaw(frame.headYaw());
            proxyPlayer.bodyYaw = frame.bodyYaw();
            proxyPlayer.setSneaking(frame.sneaking());
            proxyPlayer.setSprinting(frame.sprinting());
            proxyPlayer.setOnGround(frame.onGround());
            applySleepingState(proxyPlayer, frame);
            proxyPlayer.setPose(parsePose(frame.poseName()));
            applyPlayerUseState(proxyPlayer, frame, deferStoppingUseUntilAction);
        }

        private static void applySleepingState(@NotNull LivingEntity entity, @NotNull MagicianReplayFrame frame) {
            if (frame.sleepingPosition() != null) {
                /*
                 * 原版/Wathe 睡觉渲染会从 sleepingPosition 推导床的朝向。
                 * 只设置 SLEEPING pose 会让模型使用默认方向，表现成横躺/反向躺。
                 */
                entity.setSleepingPosition(frame.sleepingPosition());
            } else {
                entity.clearSleepingPosition();
            }
        }

        private static void applyPlayerUseState(
                @NotNull MagicianPlaybackFakePlayer proxyPlayer,
                @NotNull MagicianReplayFrame frame,
                boolean deferStoppingUseUntilAction
        ) {
            applyUseState(proxyPlayer, frame, deferStoppingUseUntilAction);
            if (frame.usingItem() && frame.activeHand() != null) {
                proxyPlayer.setReplayItemUseTimeLeft(frame.itemUseTimeLeft());
            }
        }

        private static void applyEntityUseState(
                @NotNull MagicianPlaybackEntity entity,
                @NotNull MagicianReplayFrame frame,
                boolean deferStoppingUseUntilAction
        ) {
            /*
             * 可见皮套只负责“看起来正在使用物品”，不能真的进入 LivingEntity 的原版使用流程。
             *
             * 原版食物、药水、牛奶以及 Wathe 鸡尾酒这类物品会在使用时间走完后调用 finishUsing。
             * 如果让皮套实体 setCurrentHand，它被世界 tick 时就会真实触发 finishUsing；
             * 但皮套不是 PlayerEntity，也没有玩家组件，鸡尾酒等物品里的组件读取会直接崩服。
             *
             * 因此这里仅同步 noellesroles 自己的数据追踪状态给客户端 renderer 用，
             * 真正的服务端交互/蓄力/投掷仍交给 proxyPlayer 处理。
             */
            entity.setReplayUseState(frame.usingItem(), frame.activeHand());
            entity.setReplayItemUseTimeLeft(frame.usingItem() && frame.activeHand() != null ? frame.itemUseTimeLeft() : 0);
        }

        private static void applyUseState(
                @NotNull LivingEntity entity,
                @NotNull MagicianReplayFrame frame,
                boolean deferStoppingUseUntilAction
        ) {
            Hand activeHand = frame.activeHand();
            if (frame.usingItem() && activeHand != null && !entity.getStackInHand(activeHand).isEmpty()) {
                ItemStack currentStack = entity.getStackInHand(activeHand);
                if (!entity.isUsingItem()
                        || entity.getActiveHand() != activeHand
                        || !ItemStack.areItemsAndComponentsEqual(entity.getActiveItem(), currentStack)) {
                    entity.setCurrentHand(activeHand);
                }
                return;
            }

            if (!deferStoppingUseUntilAction && entity.isUsingItem()) {
                entity.stopUsingItem();
            }
        }

        private static void syncVisibleStateFromProxy(
                @NotNull MagicianPlaybackFakePlayer proxyPlayer,
                @NotNull MagicianPlaybackEntity visibleEntity
        ) {
            MagicianInventorySnapshot.syncVisibleEquipment(proxyPlayer, visibleEntity);
            if (!proxyPlayer.isUsingItem()) {
                visibleEntity.setReplayUseState(false, null);
                visibleEntity.setReplayItemUseTimeLeft(0);
                return;
            }

            Hand activeHand = proxyPlayer.getActiveHand();
            visibleEntity.setReplayItemUseTimeLeft(proxyPlayer.getItemUseTimeLeft());
            visibleEntity.setReplayUseState(true, activeHand);
        }

        private static boolean hasReleaseUseAction(@Nullable List<MagicianRecordedAction> actions) {
            if (actions == null) {
                return false;
            }
            for (MagicianRecordedAction action : actions) {
                if (action.type == MagicianRecordedAction.Type.RELEASE_USE_ITEM) {
                    return true;
                }
            }
            return false;
        }

        private static @NotNull EntityPose parsePose(@Nullable String poseName) {
            if (poseName == null || poseName.isBlank()) {
                return EntityPose.STANDING;
            }
            try {
                return EntityPose.valueOf(poseName);
            } catch (IllegalArgumentException exception) {
                return EntityPose.STANDING;
            }
        }
    }
}
