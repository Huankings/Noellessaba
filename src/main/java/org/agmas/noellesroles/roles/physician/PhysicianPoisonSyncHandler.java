package org.agmas.noellesroles.roles.physician;

import dev.doctor4t.wathe.api.sync.PlayerComponentSyncApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 医师读取目标真实中毒状态所需的选择性组件同步规则。
 */
public final class PhysicianPoisonSyncHandler {
    private static boolean initialized;

    private PhysicianPoisonSyncHandler() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        PlayerComponentSyncApi.registerObserverRule(
                NoellesRolesCore.id("sync/physician_poison_observer"),
                PlayerPoisonComponent.KEY,
                PlayerComponentSyncApi.DEFAULT_PRIORITY,
                context -> {
                    // 医师客户端用 poisonTicks 合并判断真毒与幻觉异常，普通玩家无需接收目标毒药组件。
                    if (!GameFunctions.isPlayerAliveAndSurvival(context.recipient())) return false;
                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(context.recipient().getWorld());
                    return gameWorld.isRunning()
                            && gameWorld.isRole(context.recipient(), NoellesRoleRegistry.PHYSICIAN);
                }
        );
    }
}
