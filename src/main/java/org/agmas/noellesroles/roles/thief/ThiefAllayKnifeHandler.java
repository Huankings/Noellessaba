package org.agmas.noellesroles.roles.thief;

import dev.doctor4t.wathe.api.combat.KnifeAttackApi;
import dev.doctor4t.wathe.api.combat.KnifeStabContext;
import dev.doctor4t.wathe.api.combat.KnifeStabResult;
import dev.doctor4t.wathe.api.combat.KnifeTargetContext;
import dev.doctor4t.wathe.api.combat.KnifeTargetResult;
import dev.doctor4t.wathe.api.visibility.TargetVisibilityApi;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;

/** 小偷悦灵对 Wathe 匕首客户端目标与服务端命中的公开 API 接入。 */
public final class ThiefAllayKnifeHandler {
    private static final double DISTANCE_EPSILON = 1.0E-4D;
    private static boolean initialized;

    private ThiefAllayKnifeHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        KnifeAttackApi.registerTargetRule(
                NoellesRolesCore.id("thief_allay/knife_target"),
                ThiefConstants.ALLAY_KNIFE_TARGET_MIXIN_PRIORITY,
                ThiefAllayKnifeHandler::resolveTarget
        );
        KnifeAttackApi.registerStabHandler(
                NoellesRolesCore.id("thief_allay/knife_hit"),
                ThiefConstants.ALLAY_KNIFE_MIXIN_PRIORITY,
                ThiefAllayKnifeHandler::handleStab
        );
    }

    private static @NotNull KnifeTargetResult resolveTarget(@NotNull KnifeTargetContext context) {
        HitResult allayHit = ProjectileUtil.getCollision(
                context.user(),
                entity -> entity instanceof ThiefAllayEntity allay
                        && allay.isOperationallyAlive()
                        && (context.purpose() == KnifeTargetContext.Purpose.ATTACK
                        ? TargetVisibilityApi.canAttackEntity(context.user(), allay)
                        : TargetVisibilityApi.canTargetEntity(context.user(), allay)),
                context.range()
        );
        if (!(allayHit instanceof EntityHitResult)) {
            return KnifeTargetResult.pass();
        }

        HitResult defaultTarget = context.defaultTarget();
        if (defaultTarget != null && defaultTarget.getType() != HitResult.Type.MISS) {
            double allayDistance = context.user().getEyePos().squaredDistanceTo(allayHit.getPos());
            double defaultDistance = context.user().getEyePos().squaredDistanceTo(defaultTarget.getPos());
            if (defaultDistance + DISTANCE_EPSILON < allayDistance) {
                return KnifeTargetResult.pass();
            }
        }
        return KnifeTargetResult.target(allayHit);
    }

    private static @NotNull KnifeStabResult handleStab(@NotNull KnifeStabContext context) {
        if (!(context.target() instanceof ThiefAllayEntity allay) || !allay.isOperationallyAlive()) {
            return KnifeStabResult.PASS;
        }

        allay.killByWeapon(context.attacker(), context.stack().copy(), GameConstants.DeathReasons.KNIFE);
        context.playDefaultFeedback();
        /* 悦灵不是玩家目标，沿用旧逻辑施加 Wathe 基础冷却，不进入依赖玩家死亡状态的 modifier。 */
        context.applyBaseCooldown();
        return KnifeStabResult.HANDLED;
    }
}
