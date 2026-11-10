package org.agmas.noellesroles.client.appearance.roles.physician;

import dev.doctor4t.wathe.api.client.gui.PlayerBodyHudApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.vulture.BodyConsumedComponent;

/** 医师只额外获得死亡时间/死因行；玩家名仍不会向任何存活职业泄露。 */
public final class PhysicianBodyHudHandler {
    private PhysicianBodyHudHandler() {
    }

    public static void register() {
        PlayerBodyHudApi.registerLineRule(
                NoellesAppearanceSupport.id("body/physician_death"),
                PlayerBodyHudApi.BodyHudLine.DEATH,
                PlayerBodyHudApi.DEFAULT_PRIORITY,
                (context, line) -> {
                    if (!GameFunctions.isPlayerAliveAndSurvival(context.viewer())
                            || !GameWorldComponent.KEY.get(context.viewer().getWorld()).isRole(context.viewer(), NoellesRoleRegistry.PHYSICIAN)) {
                        return PlayerBodyHudApi.BodyHudDecision.PASS;
                    }
                    return BodyConsumedComponent.KEY.get(context.body()).consumed
                            ? PlayerBodyHudApi.BodyHudDecision.OBFUSCATE
                            : PlayerBodyHudApi.BodyHudDecision.SHOW;
                }
        );
    }
}
