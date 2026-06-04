package org.agmas.noellesroles.roles.spiritualist;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * 灵术师的统一准心目标判定工具。
 *
 * <p>灵魂出窍与灵魂附身共用同一枚能力键，
 * 所以“当前准心到底有没有对准 2 格内玩家”必须在服务端与客户端使用同一套逻辑，
 * 避免 HUD 提示和实际技能触发分流不一致。</p>
 */
public final class SpiritualistTargeting {
    private SpiritualistTargeting() {
    }

    public static @Nullable ServerPlayerEntity getPossessionTarget(@NotNull ServerPlayerEntity player, int clientTargetId) {
        if (clientTargetId >= 0 && player.getServerWorld().getEntityById(clientTargetId) instanceof ServerPlayerEntity target) {
            if (isValidPossessionTarget(player, target)) {
                return target;
            }
        }

        PlayerEntity fallbackTarget = getPossessionTarget(player);
        return fallbackTarget instanceof ServerPlayerEntity serverTarget ? serverTarget : null;
    }

    public static @Nullable PlayerEntity getPossessionTarget(@NotNull PlayerEntity player) {
        return getPossessionTarget(player, target -> true);
    }

    public static @Nullable PlayerEntity getPossessionTarget(
            @NotNull PlayerEntity player,
            @NotNull Predicate<PlayerEntity> extraPredicate
    ) {
        HitResult hitResult = ProjectileUtil.getCollision(
                player,
                entity -> entity instanceof PlayerEntity target
                        && !target.getUuid().equals(player.getUuid())
                        && GameFunctions.isPlayerAliveAndSurvival(target)
                        && extraPredicate.test(target),
                SpiritualistConstants.POSSESSION_RANGE
        );

        if (hitResult instanceof EntityHitResult entityHitResult
                && entityHitResult.getEntity() instanceof PlayerEntity target) {
            return target;
        }
        return null;
    }

    public static boolean isPossessionAim(@NotNull PlayerEntity player) {
        return getPossessionTarget(player) != null;
    }

    private static boolean isValidPossessionTarget(@NotNull ServerPlayerEntity player, @NotNull ServerPlayerEntity target) {
        return !target.getUuid().equals(player.getUuid())
                && GameFunctions.isPlayerAliveAndSurvival(target)
                && player.squaredDistanceTo(target) <= SpiritualistConstants.POSSESSION_RANGE_SQUARED;
    }
}
