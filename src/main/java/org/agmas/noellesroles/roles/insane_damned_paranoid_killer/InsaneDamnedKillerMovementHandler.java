package org.agmas.noellesroles.roles.insane_damned_paranoid_killer;

import dev.doctor4t.wathe.api.movement.PlayerMovementApi;
import dev.doctor4t.wathe.game.GameFunctions;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 亡语杀手尸体伪装的移动速度接入。
 *
 * <p>spark 版通过反复刷新缓慢效果来压低速度；当前自改版要求使用 Wathe 公开移动 API。
 * 这样速度修正会和其它职业 / 词条按同一条链路叠加，不需要再 mixin 玩家移动或药水效果。</p>
 */
public final class InsaneDamnedKillerMovementHandler {
    private static boolean initialized = false;

    private InsaneDamnedKillerMovementHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        PlayerMovementApi.registerSpeedModifier(
                NoellesRolesCore.id("movement/insane_damned_killer_corpse"),
                InsaneDamnedKillerConstants.CORPSE_MOVEMENT_PRIORITY,
                context -> {
                    /*
                     * 只处理仍处于正式游玩状态的亡语杀手。
                     * 尸体伪装本身是活人欺骗行为；死亡、旁观或转职后即使组件布尔值被旧快照带回来，
                     * 也不应该继续影响移动速度。
                     */
                    if (!GameFunctions.isPlayerAliveAndSurvival(context.player())
                            || !InsaneDamnedKillerPlayerComponent.isActiveCorpseMode(context.player())) {
                        return PlayerMovementApi.MovementSpeedResult.pass();
                    }

                    /*
                     * 使用 MULTIPLY 而不是 OVERRIDE：
                     * 其它职业或词条可能已经通过公开 API 改过基础速度，尸体伪装只负责把“当前结果”折半，
                     * 更接近需求里的“速度变成原来的一半”，也避免覆盖别人的显式速度接口。
                     */
                    return PlayerMovementApi.MovementSpeedResult.multiply(InsaneDamnedKillerConstants.CORPSE_SPEED_MULTIPLIER);
                }
        );
    }
}
