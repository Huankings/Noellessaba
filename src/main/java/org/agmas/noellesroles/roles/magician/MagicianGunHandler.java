package org.agmas.noellesroles.roles.magician;

import dev.doctor4t.wathe.api.combat.GunShotApi;
import dev.doctor4t.wathe.api.combat.GunShotContext;
import dev.doctor4t.wathe.api.combat.GunShotResult;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.tag.WatheItemTags;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 魔术师对枪击动作的录制，以及“枪击命中皮套就打断播放”的处理。
 */
public final class MagicianGunHandler {
    private static boolean initialized = false;

    private MagicianGunHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        /*
         * 旧 mixin priority 为 1100；这里保持比职业自定义枪接管更高，
         * 使录制和皮套打断先于强盗/无声左轮自己的开火结算执行。
         */
        GunShotApi.registerShotHandler(
                NoellesRolesCore.id("magician_gun_record_and_break"),
                1100,
                MagicianGunHandler::handleShot
        );
    }

    private static GunShotResult handleShot(GunShotContext context) {
        /*
         * 魔术师只关心“能被记录进播放时间线的枪”。
         * Wathe 默认枪械走标签；Noelles 自定义强盗手枪/无声左轮没有必要强塞进 Wathe 标签，
         * 所以这里额外列出，保证录制层能先看到它们。
         */
        boolean supportedGun = context.stack().isIn(WatheItemTags.GUNS)
                || context.stack().isOf(ModItems.ROBBER_PISTOL)
                || context.stack().isOf(ModItems.SILENCED_REVOLVER);
        if (!supportedGun) {
            return GunShotResult.PASS;
        }

        if (!context.isCoolingDown()) {
            /*
             * 只在武器可开火时记录动作。
             * 如果冷却中也记录，播放体会回放出原本没有发生的枪击。
             */
            MagicianServerHooks.recordGunShoot(context.shooter());
        }

        if (context.targetEntityId() >= 0
                && MagicianServerHooks.stopPlaybackByWeaponTarget(
                context.targetEntity(),
                context.shooter(),
                GameConstants.DeathReasons.GUN,
                MagicianServerHooks.getWeaponName(context.stack())
        )) {
            /*
             * 命中皮套时，旧逻辑会在包接收器 HEAD 直接取消。
             * 因此这里也必须用 CANCEL，而不是 HANDLED：
             * 不应播放枪声、枪口动画或给武器挂冷却。
             * 这样玩家打断播放体时，不会被当作正常开枪击杀某个真实玩家。
             */
            return GunShotResult.CANCEL;
        }
        return GunShotResult.PASS;
    }
}
