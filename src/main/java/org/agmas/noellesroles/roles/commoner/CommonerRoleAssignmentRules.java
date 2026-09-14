package org.agmas.noellesroles.roles.commoner;

import org.agmas.harpymodloader.api.assignment.RoleAssignmentApi;
import org.agmas.harpymodloader.api.assignment.RoleAssignmentPhase;
import org.agmas.harpymodloader.api.assignment.RoleAssignmentPhaseContext;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/** 平民进入随机职业池的每局一次性人数与概率门控。 */
public final class CommonerRoleAssignmentRules {
    private static boolean initialized;

    private CommonerRoleAssignmentRules() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        RoleAssignmentApi.registerBeforePhaseHandler(
                NoellesRolesCore.id("commoner_spawn_gate"),
                RoleAssignmentPhase.CIVILIAN_REPLACEMENT,
                500,
                CommonerRoleAssignmentRules::configureCommonerPool
        );
    }

    private static void configureCommonerPool(RoleAssignmentPhaseContext context) {
        CommonerRoleLimitHandler.configureForAssignment(context);
    }
}
