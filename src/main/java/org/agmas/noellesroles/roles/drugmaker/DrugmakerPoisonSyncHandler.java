package org.agmas.noellesroles.roles.drugmaker;

import dev.doctor4t.wathe.api.sync.PlayerComponentSyncApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 制毒师读取目标真实中毒状态所需的选择性组件同步规则。
 */
public final class DrugmakerPoisonSyncHandler {
    private static boolean initialized;

    private DrugmakerPoisonSyncHandler() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        PlayerComponentSyncApi.registerObserverRule(
                NoellesRolesCore.id("sync/drugmaker_poison_observer"),
                PlayerPoisonComponent.KEY,
                PlayerComponentSyncApi.DEFAULT_PRIORITY,
                context -> {
                    /*
                     * 制毒师本能在客户端读取目标 poisonTicks 和 poisoner，用于显示真毒并排除幻觉标记。
                     * 因此只向局内存活制毒师开放目标组件；死亡、停局或换职后由 Wathe 撤权并清除旧缓存。
                     */
                    if (!GameFunctions.isPlayerAliveAndSurvival(context.recipient())) return false;
                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(context.recipient().getWorld());
                    return gameWorld.isRunning()
                            && gameWorld.isRole(context.recipient(), NoellesRoleRegistry.DRUGMAKER);
                }
        );
    }
}
