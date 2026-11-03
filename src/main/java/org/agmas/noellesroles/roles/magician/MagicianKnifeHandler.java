package org.agmas.noellesroles.roles.magician;

import dev.doctor4t.wathe.api.combat.KnifeAttackApi;
import dev.doctor4t.wathe.api.combat.KnifeStabContext;
import dev.doctor4t.wathe.api.combat.KnifeStabResult;
import dev.doctor4t.wathe.api.combat.KnifeTargetContext;
import dev.doctor4t.wathe.api.combat.KnifeTargetResult;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.entities.MagicianPlaybackEntity;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;

/** 魔术师播放体对 Wathe 匕首目标与服务端刺击 API 的接入。 */
public final class MagicianKnifeHandler {
    private static final int TARGET_PRIORITY = 1_000;
    private static final int STAB_PRIORITY = 1_100;
    private static final double DISTANCE_EPSILON = 1.0E-4D;
    private static boolean initialized;

    private MagicianKnifeHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        KnifeAttackApi.registerTargetRule(
                NoellesRolesCore.id("magician/knife_target"),
                TARGET_PRIORITY,
                MagicianKnifeHandler::resolveTarget
        );
        KnifeAttackApi.registerStabHandler(
                NoellesRolesCore.id("magician/knife_stab"),
                STAB_PRIORITY,
                MagicianKnifeHandler::handleStab
        );
    }

    private static @NotNull KnifeTargetResult resolveTarget(@NotNull KnifeTargetContext context) {
        HitResult playbackHit = ProjectileUtil.getCollision(
                context.user(),
                entity -> entity instanceof MagicianPlaybackEntity,
                context.range()
        );
        if (!(playbackHit instanceof EntityHitResult)) {
            return KnifeTargetResult.pass();
        }

        /* 玩家默认目标更近时不抢射线，避免播放体规则改变原本的近距离命中顺序。 */
        HitResult defaultTarget = context.defaultTarget();
        if (defaultTarget != null && defaultTarget.getType() != HitResult.Type.MISS) {
            double playbackDistance = context.user().getEyePos().squaredDistanceTo(playbackHit.getPos());
            double defaultDistance = context.user().getEyePos().squaredDistanceTo(defaultTarget.getPos());
            if (defaultDistance + DISTANCE_EPSILON < playbackDistance) {
                return KnifeTargetResult.pass();
            }
        }
        return KnifeTargetResult.target(playbackHit);
    }

    private static @NotNull KnifeStabResult handleStab(@NotNull KnifeStabContext context) {
        /* 只有通过 Wathe 服务端权威校验的真实刺击请求才进入录像，避免伪造包污染播放时间线。 */
        MagicianServerHooks.recordKnifeStab(context.attacker());
        return MagicianServerHooks.stopPlaybackByWeaponTarget(
                context.target(),
                context.attacker(),
                GameConstants.DeathReasons.KNIFE,
                MagicianServerHooks.getWeaponName(context.stack())
        ) ? KnifeStabResult.CANCEL : KnifeStabResult.PASS;
    }
}
