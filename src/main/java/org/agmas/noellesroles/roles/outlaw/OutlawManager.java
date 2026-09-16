package org.agmas.noellesroles.roles.outlaw;

import dev.doctor4t.wathe.api.PlayerLifeStateApi;
import dev.doctor4t.wathe.api.task.MoodTaskApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.compat.TrainVoicePlugin;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.client.gui.RoleAnnouncementTexts;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.util.AnnounceWelcomePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameMode;
import org.agmas.harpymodloader.events.ModdedRoleAssigned;
import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.noellesroles.registry.NoellesDeathReasons;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesModifierRegistry;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.modifiers.lovers.LoversPairComponent;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperPlayerComponent;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperWorldComponent;
import org.agmas.noellesroles.roles.jason.JasonAbilityManager;
import org.agmas.harpymodloader.component.WorldModifierComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** 平民死亡后的复活倒计时、亡命倒计时、音乐状态和击杀状态总管理器。 */
public final class OutlawManager {
    private static boolean initialized;

    private OutlawManager() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            OutlawPlayerComponent component = OutlawPlayerComponent.KEY.get(player);
            if (component.isOutlawActive() && GameFunctions.isPlayerAliveAndSurvival(player)
                    && !TimekeeperPlayerComponent.KEY.get(player).isInTimeRift()) {
                GameFunctions.killPlayer(player, true, null, NoellesDeathReasons.OUTLAW_OFFLINE_DEATH_REASON);
            }
        });
    }

    public static void tickWorld(ServerWorld world) {
        if (TimekeeperWorldComponent.KEY.get(world).isRewinding()) {
            return;
        }
        OutlawWorldComponent worldState = OutlawWorldComponent.KEY.get(world);
        if (worldState.isRevivalBlackoutActive()
                && !dev.doctor4t.wathe.api.blackout.BlackoutApi.isActive(world)) {
            worldState.setRevivalBlackoutActive(false);
        }

        for (ServerPlayerEntity player : world.getPlayers()) {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
            OutlawPlayerComponent state = OutlawPlayerComponent.KEY.get(player);

            if (state.isRevivalPending()) {
                if (!TimekeeperPlayerComponent.KEY.get(player).isInTimeRift()
                        && !GameFunctions.isPlayerAliveAndSurvival(player)) {
                    if (state.getRevivalTicksLeft() > 0) {
                        state.tickRevival();
                    }
                    if (state.getRevivalTicksLeft() <= 0) {
                        transformToOutlaw(world, player, state);
                    }
                }
            }

            if (!state.isOutlawActive()) {
                continue;
            }

            if (!gameWorld.isRole(player, NoellesRoleRegistry.OUTLAW)) {
                continue;
            }
            if (GameFunctions.isPlayerAliveAndSurvival(player)
                    && !TimekeeperPlayerComponent.KEY.get(player).isInTimeRift()) {
                state.tickOutlawTime();
                player.getItemCooldownManager().remove(WatheItems.KNIFE);
                applySlowness(player, state);
                if (state.getOutlawTicksLeft() <= 0) {
                    GameFunctions.killPlayer(player, true, null, NoellesDeathReasons.OUTLAW_TIMEOUT_DEATH_REASON);
                }
            }
        }
    }

    public static void beginCommonerRevival(ServerPlayerEntity victim, ServerPlayerEntity killer, boolean pushedOut) {
        OutlawPlayerComponent state = OutlawPlayerComponent.KEY.get(victim);
        state.startRevival(victim.getPos(), killer == null ? null : killer.getUuid(), pushedOut);
        GameRecordManager.recordGlobalEvent(victim.getServerWorld(), NoellesEventIds.COMMONER_REVIVAL_STARTED_EVENT, victim, null);
    }

    private static void transformToOutlaw(ServerWorld world, ServerPlayerEntity player, OutlawPlayerComponent state) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
        if (gameWorld.isRole(player, NoellesRoleRegistry.OUTLAW)) {
            state.clearRevival();
            return;
        }

        /*
         * 亡命徒已经从“平民生命线”转成独立阵营，不能继续保留恋人这类生命绑定词条。
         * 必须在 addRole / ModdedRoleAssigned 之前完成清理：恋人胜利规则每 tick 都会读取
         * WorldModifierComponent 和 LoversPairComponent；如果等到转职事件之后再删，
         * 复活玩家可能会在同一 tick 被旧伴侣的殉情逻辑重新击杀。
         */
        clearLifeBindingModifiers(world, player);

        ServerPlayerEntity target = resolveRevivalTarget(world, state);
        double x = state.getRevivalPosition().x;
        double y = state.getRevivalPosition().y;
        double z = state.getRevivalPosition().z;
        float yaw = player.getYaw();
        float pitch = player.getPitch();
        if (state.wasRevivalPushedOut() && target != null) {
            x = target.getX();
            y = target.getY();
            z = target.getZ();
            yaw = target.getYaw();
            pitch = target.getPitch();
        }

        TimekeeperPlayerComponent.KEY.get(player).clearTimeRiftForRevival();
        PlayerLifeStateApi.clearAliveOverride(player);
        player.changeGameMode(GameMode.ADVENTURE);
        player.teleport(world, x, y, z, Collections.emptySet(), yaw, pitch);
        gameWorld.addRole(player, NoellesRoleRegistry.OUTLAW);
        ModdedRoleAssigned.EVENT.invoker().assignModdedRole(player, NoellesRoleRegistry.OUTLAW);

        int aliveCount = world.getPlayers(playerEntity ->
                GameFunctions.isPlayerAliveAndSurvival(playerEntity)
                        && !TimekeeperPlayerComponent.KEY.get(playerEntity).isInTimeRift()).size();
        int initialLayers = OutlawConstants.clampShieldLayers(
                aliveCount / OutlawConstants.ALIVE_PLAYERS_PER_INITIAL_SHIELD,
                OutlawConstants.MAX_INITIAL_SHIELD_LAYERS
        );
        state.startOutlaw(initialLayers);
        OutlawWorldComponent.KEY.get(world).setOutlawActive(player.getUuid(), true);
        /* 亡命时刻从此刻开始压过无恶不在，立即关闭全场杰森幽魂状态。 */
        JasonAbilityManager.forceExitForActiveOutlaw(world);
        /* 亡命时刻从这一行起已经生效，立即暂停两种低优先级时刻，不能等到下一次客户端 tick。 */
        OutlawMomentPriorityHandler.onOutlawStarted(world);
        clearMoodTasks(player);
        TrainVoicePlugin.resetPlayer(player.getUuid());
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.GLOWING,
                OutlawConstants.REVIVAL_GLOW_TICKS,
                0,
                false,
                true,
                true
        ));
        sendWelcomeAnnouncement(player, gameWorld);
        applySlowness(player, state);
        OutlawBlackoutHandler.restartForRevival(world);

        GameRecordManager.recordGlobalEvent(world, NoellesEventIds.OUTLAW_REVIVED_EVENT, player, null);
    }

    /**
     * 清理当前已实现的生命绑定词条，并解除恋人双向配对。
     *
     * <p>如果旧存档只有 LOVERS 词条而没有显式配对组件，也会至少清除复活玩家自己的词条；
     * 这样兼容旧版数据时不会因为找不到伴侣而跳过核心安全处理。</p>
     */
    private static void clearLifeBindingModifiers(ServerWorld world, ServerPlayerEntity player) {
        WorldModifierComponent modifiers = WorldModifierComponent.KEY.get(world);
        LoversPairComponent pairs = LoversPairComponent.KEY.get(world);
        UUID partnerUuid = pairs.getPartner(player.getUuid());

        modifiers.getModifiers(player).removeIf(NoellesModifierRegistry.LOVERS::equals);
        if (partnerUuid != null) {
            modifiers.getModifiers(partnerUuid).removeIf(NoellesModifierRegistry.LOVERS::equals);
            pairs.removePair(player.getUuid());
        }
        modifiers.sync();
    }

    /** 复活转职后重新发送亡命徒开局欢迎公告。 */
    private static void sendWelcomeAnnouncement(ServerPlayerEntity player, GameWorldComponent gameWorld) {
        var announcement = Harpymodloader.autogeneratedAnnouncements.get(NoellesRoleRegistry.OUTLAW);
        int announcementIndex = announcement == null
                ? RoleAnnouncementTexts.ROLE_ANNOUNCEMENT_TEXTS.indexOf(RoleAnnouncementTexts.KILLER)
                : RoleAnnouncementTexts.ROLE_ANNOUNCEMENT_TEXTS.indexOf(announcement);
        ServerPlayNetworking.send(player, new AnnounceWelcomePayload(
                announcementIndex,
                gameWorld.getAllKillerTeamPlayers().size(),
                0
        ));
    }

    private static ServerPlayerEntity resolveRevivalTarget(ServerWorld world, OutlawPlayerComponent state) {
        if (!state.wasRevivalPushedOut()) {
            return null;
        }
        UUID killerUuid = state.getRevivalKillerUuid();
        if (killerUuid != null) {
            ServerPlayerEntity killer = world.getServer().getPlayerManager().getPlayer(killerUuid);
            if (killer != null
                    && GameFunctions.isPlayerAliveAndSurvival(killer)
                    && !TimekeeperPlayerComponent.KEY.get(killer).isInTimeRift()) {
                return killer;
            }
        }
        List<ServerPlayerEntity> alive = new ArrayList<>(world.getPlayers(playerEntity ->
                GameFunctions.isPlayerAliveAndSurvival(playerEntity)
                        && !TimekeeperPlayerComponent.KEY.get(playerEntity).isInTimeRift()));
        if (alive.isEmpty()) {
            return null;
        }
        return alive.get(world.random.nextInt(alive.size()));
    }

    private static void clearMoodTasks(ServerPlayerEntity player) {
        for (var taskId : List.copyOf(PlayerMoodComponent.KEY.get(player).getActiveMoodTaskIds())) {
            MoodTaskApi.removeTask(player, taskId);
        }
    }

    private static void applySlowness(ServerPlayerEntity player, OutlawPlayerComponent state) {
        if (!state.isOutlawActive() || state.getSlownessAmplifier() < 0) {
            player.removeStatusEffect(StatusEffects.SLOWNESS);
            return;
        }
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.SLOWNESS,
                Math.max(1, state.getSlownessTicksLeft()),
                state.getSlownessAmplifier(),
                false,
                false,
                true
        ));
        state.tickSlowness();
    }

    public static void markOutlawDeath(ServerPlayerEntity victim) {
        OutlawPlayerComponent state = OutlawPlayerComponent.KEY.get(victim);
        if (!state.isOutlawActive()) {
            return;
        }
        state.finishOutlaw();
        victim.removeStatusEffect(StatusEffects.SLOWNESS);
        OutlawWorldComponent.KEY.get(victim.getServerWorld()).setOutlawActive(victim.getUuid(), false);
        /* 只有最后一名活跃亡命徒死亡后，协调器才会按影子小丑优先级恢复其它时刻。 */
        OutlawMomentPriorityHandler.onOutlawEnded(victim.getServerWorld());
    }

    public static void recordOutlawKill(ServerPlayerEntity killer) {
        OutlawPlayerComponent state = OutlawPlayerComponent.KEY.get(killer);
        if (!state.isOutlawActive()) {
            return;
        }
        state.incrementKillCount();
        state.incrementKillsTowardsShield();
        state.addOutlawTime(OutlawConstants.KILL_TIME_BONUS_TICKS);
        if (OutlawConstants.KILL_SHIELD_ENABLED && state.consumeKillsForShield()) {
            int before = state.getShieldLayers();
            int after = OutlawConstants.clampShieldLayers(before + 1, OutlawConstants.MAX_KILL_SHIELD_LAYERS);
            if (after > before) {
                state.addShieldLayer();
                GameRecordManager.recordGlobalEvent(killer.getServerWorld(), NoellesEventIds.OUTLAW_SHIELD_GAINED_EVENT, killer, null);
            }
        }
    }

    public static void reconcileAfterRewind(ServerWorld world) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
        OutlawWorldComponent worldState = OutlawWorldComponent.KEY.get(world);
        worldState.getActiveOutlaws().forEach(uuid -> {
            ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(uuid);
            if (player == null || !OutlawPlayerComponent.KEY.get(player).isOutlawActive()) {
                worldState.setOutlawActive(uuid, false);
            }
        });
        for (ServerPlayerEntity player : world.getPlayers()) {
            OutlawPlayerComponent state = OutlawPlayerComponent.KEY.get(player);
            if (state.isOutlawActive()) {
                if (gameWorld.getRole(player) != NoellesRoleRegistry.OUTLAW) {
                    gameWorld.getRoles().put(player.getUuid(), NoellesRoleRegistry.OUTLAW);
                }
                worldState.setOutlawActive(player.getUuid(), true);
            } else if (state.isRevivalPending() && gameWorld.getRole(player) == NoellesRoleRegistry.OUTLAW) {
                // 回溯到复活前必须静默恢复为非存活平民，让倒计时继续运行。
                gameWorld.getRoles().put(player.getUuid(), NoellesRoleRegistry.COMMONER);
                worldState.setOutlawActive(player.getUuid(), false);
            }
        }
        gameWorld.sync();
        /* 世界组件与玩家组件都完成回溯后，再统一修复三种时刻的暂停和恢复关系。 */
        OutlawMomentPriorityHandler.reconcile(world);
        if (worldState.hasActiveOutlaw()) {
            JasonAbilityManager.forceExitForActiveOutlaw(world);
        }
    }
}
