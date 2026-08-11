package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.api.visibility.TargetVisibilityApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 弹簧陷阱斧类武器共用的准星目标检测。
 *
 * <p>客户端只用它决定是否显示准心或发包；服务端收到包后仍会重新检查距离、手持物、存活状态和目标可攻击性。</p>
 */
public final class SpringTrapTargeting {
    private SpringTrapTargeting() {
    }

    public static @Nullable EntityHitResult getPlayerTarget(PlayerEntity player, double range) {
        HitResult hitResult = ProjectileUtil.getCollision(
                player,
                entity -> entity instanceof PlayerEntity target
                        && GameFunctions.isPlayerAliveAndSurvival(target)
                        && TargetVisibilityApi.canAttackPlayer(player, target),
                range
        );
        return hitResult instanceof EntityHitResult entityHitResult
                && entityHitResult.getEntity() instanceof PlayerEntity
                ? entityHitResult
                : null;
    }
}
