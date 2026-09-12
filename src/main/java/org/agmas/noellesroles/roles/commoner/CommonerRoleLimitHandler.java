package org.agmas.noellesroles.roles.commoner;

import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

/** 根据当前参与人数刷新平民是否进入 Harpy 随机职业池。 */
public final class CommonerRoleLimitHandler {
    private CommonerRoleLimitHandler() {
    }

    public static void refresh(int playerCount) {
        Harpymodloader.setRoleMaximum(
                NoellesRoleRegistry.COMMONER,
                playerCount >= CommonerConstants.RANDOM_POOL_MIN_PLAYER_COUNT
                        ? CommonerConstants.MAX_ROLE_COUNT
                        : 0
        );
    }
}
