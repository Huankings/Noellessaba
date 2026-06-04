package org.agmas.noellesroles.roles.angel;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 天使主动能力。
 *
 * <p>G 键逻辑分成两种模式：
 * 1. 准心未对准 2 格内存活玩家时，释放安抚；
 * 2. 准心对准 2 格内存活玩家时，切换 / 设定守护目标。
 */
public final class AngelAbility {
    private AngelAbility() {
    }

    public static void handle(ServerPlayerEntity player) {
        handle(player, -1);
    }

    /**
     * 处理天使能力键。
     *
     * <p>这里允许客户端把“这一帧自己真正瞄准到的玩家实体 id”一起发给服务端。
     * 这样服务端就不用再重新做一次容易受网络时差影响的射线判定，
     * 对于横向移动中的目标，守护命中会稳定很多。</p>
     *
     * @param clientTargetId 客户端这一帧锁定到的目标实体 id；没有则传 -1
     */
    public static void handle(ServerPlayerEntity player, int clientTargetId) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.ANGEL)
                || !gameWorld.isRunning()
                || !GameFunctions.isPlayerAliveAndSurvival(player)) {
            return;
        }

        AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(player);
        AngelPlayerComponent angelComponent = AngelPlayerComponent.KEY.get(player);

        ServerPlayerEntity targetedPlayer = resolveGuardTarget(player, clientTargetId);
        if (ability.cooldown > 0) {
            /*
             * 天使的两种主动模式共用同一条能力冷却。
             *
             * 这里必须在“守护 / 安抚”分流之前就先拦住，
             * 否则玩家只要在冷却期间对准别人，就仍然能反复改守护目标，
             * 不仅能绕过 90 秒安抚冷却，还会把冷却强行刷回 30 秒。
             *
             * 唯一保留的例外是：
             * 冷却期间如果对准的就是当前已经守护的玩家，允许继续看到
             * “你已经守护了该玩家” 这条纯提示信息，但不会产生任何状态变化。
             */
            if (targetedPlayer != null) {
                UUID previousGuard = angelComponent.getGuardedTarget();
                if (previousGuard != null && previousGuard.equals(targetedPlayer.getUuid())) {
                    handleGuard(player, targetedPlayer, ability, angelComponent);
                }
            }
            return;
        }

        if (targetedPlayer != null) {
            handleGuard(player, targetedPlayer, ability, angelComponent);
            return;
        }
        handleSoothe(player, ability);
    }

    private static void handleGuard(
            ServerPlayerEntity player,
            ServerPlayerEntity target,
            AbilityPlayerComponent ability,
            AngelPlayerComponent angelComponent
    ) {
        if (!GameFunctions.isPlayerAliveAndSurvival(target) || player.getUuid().equals(target.getUuid())) {
            return;
        }

        UUID previousGuard = angelComponent.getGuardedTarget();
        if (target.getUuid().equals(previousGuard)) {
            player.sendMessage(
                    Text.translatable("message.noellesroles.angel.already_guarding")
                            .withColor(Noellesroles.ANGEL.color()),
                    true
            );
            return;
        }

        angelComponent.setGuardedTarget(target.getUuid());
        ability.setCooldown(AngelConstants.GUARD_COOLDOWN_TICKS);

        player.sendMessage(
                Text.translatable("message.noellesroles.angel.guard_selected", target.getDisplayName())
                        .withColor(Noellesroles.ANGEL.color()),
                true
        );

        NbtCompound extra = new NbtCompound();
        extra.putUuid("target_player", target.getUuid());
        if (previousGuard != null) {
            extra.putUuid("previous_target", previousGuard);
        }
        GameRecordManager.recordGlobalEvent(player.getServerWorld(), Noellesroles.ANGEL_GUARD_SELECTED_EVENT, player, extra);
    }

    private static void handleSoothe(ServerPlayerEntity player, AbilityPlayerComponent ability) {
        List<ServerPlayerEntity> soothedPlayers = new ArrayList<>();
        for (ServerPlayerEntity other : player.getServerWorld().getPlayers()) {
            if (other.getUuid().equals(player.getUuid())) {
                continue;
            }
            if (!GameFunctions.isPlayerAliveAndSurvival(other)) {
                continue;
            }
            if (other.squaredDistanceTo(player) > AngelConstants.SOOTHE_RADIUS_SQUARED) {
                continue;
            }

            PlayerMoodComponent mood = PlayerMoodComponent.KEY.get(other);
            mood.setMoodDrainProtectionTicks(AngelConstants.SOOTHE_PROTECTION_TICKS);
            AngelPlayerComponent.KEY.get(other).applySoothe(AngelConstants.SOOTHE_PROTECTION_TICKS);
            other.sendMessage(
                    Text.translatable("message.noellesroles.angel.soothed")
                            .withColor(Noellesroles.ANGEL.color()),
                    true
            );
            soothedPlayers.add(other);

            NbtCompound soothedExtra = new NbtCompound();
            soothedExtra.putUuid("target_player", other.getUuid());
            GameRecordManager.recordGlobalEvent(player.getServerWorld(), Noellesroles.ANGEL_SOOTHED_EVENT, player, soothedExtra);
        }

        Vec3d center = player.getPos().add(0, 1.0, 0);
        AngelPlayerComponent.KEY.get(player).startSootheParticles(center);
        ability.setCooldown(AngelConstants.SOOTHE_COOLDOWN_TICKS);

        player.getWorld().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                SoundCategory.PLAYERS,
                1.0f,
                0.9f
        );
        player.getWorld().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENTITY_CAT_AMBIENT,
                SoundCategory.PLAYERS,
                0.9f,
                1.1f
        );

        GameRecordManager.recordGlobalEvent(player.getServerWorld(), Noellesroles.ANGEL_SOOTHE_CAST_EVENT, player, null);
    }

    /**
     * 返回当前准心真正对准的 2 格内存活玩家。
     */
    public static @Nullable ServerPlayerEntity getGuardTarget(@NotNull ServerPlayerEntity player) {
        PlayerEntity genericTarget = getGenericGuardTarget(player);
        return genericTarget instanceof ServerPlayerEntity serverTarget ? serverTarget : null;
    }

    /**
     * 优先使用客户端已经锁定好的目标，再退回旧的服务端射线判定。
     *
     * <p>这样做的目的不是完全信任客户端，而是把“瞄准谁”这一步尽量交给玩家本地视角，
     * 然后服务端只负责做：
     * 1. 目标实体是否还存在；
     * 2. 是否仍然是存活玩家；
     * 3. 是否仍然在 2 格守护距离内；
     * 4. 是否不是自己。
     *
     * <p>如果客户端没带目标，或者带来的目标当前已经不合法，
     * 才退回旧的服务端射线方案，这样能兼容老调用路径，也能保证行为平滑升级。</p>
     */
    private static @Nullable ServerPlayerEntity resolveGuardTarget(@NotNull ServerPlayerEntity player, int clientTargetId) {
        if (clientTargetId >= 0 && player.getServerWorld().getEntityById(clientTargetId) instanceof ServerPlayerEntity serverTarget) {
            if (!player.getUuid().equals(serverTarget.getUuid())
                    && GameFunctions.isPlayerAliveAndSurvival(serverTarget)
                    && player.squaredDistanceTo(serverTarget) <= AngelConstants.GUARD_RANGE_SQUARED) {
                return serverTarget;
            }
        }
        return getGuardTarget(player);
    }

    public static @Nullable PlayerEntity getGenericGuardTarget(@NotNull PlayerEntity player) {
        HitResult hitResult = ProjectileUtil.getCollision(
                player,
                entity -> entity instanceof PlayerEntity target
                        && !target.getUuid().equals(player.getUuid())
                        && GameFunctions.isPlayerAliveAndSurvival(target),
                AngelConstants.GUARD_RANGE
        );
        if (hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof PlayerEntity target) {
            return target;
        }
        return null;
    }

    /**
     * 当前 HUD 所使用的技能模式。
     */
    public static boolean isGuardMode(@NotNull PlayerEntity player) {
        return getGenericGuardTarget(player) != null;
    }
}
