package org.agmas.noellesroles.roles.bartender;

import dev.doctor4t.wathe.api.sync.PlayerComponentSyncApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 酒保读取目标真实中毒状态所需的选择性组件同步规则。
 */
public final class BartenderPoisonSyncHandler {
    private static boolean initialized;

    private BartenderPoisonSyncHandler() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        PlayerComponentSyncApi.registerObserverRule(
                NoellesRolesCore.id("sync/bartender_poison_observer"),
                PlayerPoisonComponent.KEY,
                PlayerComponentSyncApi.DEFAULT_PRIORITY,
                context -> {
                    // 酒保客户端把 poisonTicks 与饮品护甲、发光、幻觉状态共同转换成本能透视颜色。
                    if (!GameFunctions.isPlayerAliveAndSurvival(context.recipient())) return false;
                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(context.recipient().getWorld());
                    return gameWorld.isRunning()
                            && gameWorld.isRole(context.recipient(), NoellesRoleRegistry.BARTENDER);
                }
        );
    }
}
