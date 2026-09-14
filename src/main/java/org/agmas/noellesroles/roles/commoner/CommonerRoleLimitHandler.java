package org.agmas.noellesroles.roles.commoner;

import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.harpymodloader.api.assignment.RoleAssignmentPhaseContext;
import org.agmas.noellesroles.config.NoellesRolesConfig;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

/** 根据当前参与人数刷新平民是否进入 Harpy 随机职业池。 */
public final class CommonerRoleLimitHandler {
    private CommonerRoleLimitHandler() {
    }

    /** 在每局平民替换阶段开始时只进行一次人数和概率判定。 */
    public static void configureForAssignment(RoleAssignmentPhaseContext context) {
        NoellesRolesConfig config = NoellesRolesConfig.HANDLER.instance();
        int playerCount = context.players().size();
        int minimumPlayers = Math.max(0, config.commonerMinPlayerSpawn);
        double chance = Math.max(0.0D, Math.min(1.0D, config.commonerSpawnChance));
        boolean allowedByChance = playerCount >= minimumPlayers
                && context.serverWorld().random.nextDouble() < chance;
        Harpymodloader.setRoleMaximum(
                NoellesRoleRegistry.COMMONER,
                allowedByChance
                        ? CommonerConstants.MAX_ROLE_COUNT
                        : 0
        );
    }
}
