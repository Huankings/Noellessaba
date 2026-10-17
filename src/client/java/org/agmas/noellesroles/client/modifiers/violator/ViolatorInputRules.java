package org.agmas.noellesroles.client.modifiers.violator;

import dev.doctor4t.wathe.api.client.input.ClientInputApi;
import dev.doctor4t.wathe.client.WatheClient;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.agmas.noellesroles.registry.NoellesModifierRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.engineer.StunnedPlayerComponent;

/** 违禁者只对自己放开 Wathe 默认的局内跳跃禁用。 */
public final class ViolatorInputRules {
    private static final int PRIORITY = 6000;

    private ViolatorInputRules() {
    }

    public static void register() {
        ClientInputApi.registerKeyBindingRule(NoellesRolesCore.id("input/violator/jump"), PRIORITY, context -> {
            var client = context.client();
            if (client.player == null
                    || client.world == null
                    || !context.keyBinding().equals(client.options.jumpKey)
                    || !WatheClient.isPlayerAliveAndInSurvival()
                    || StunnedPlayerComponent.KEY.get(client.player).isStunned()
                    || !WorldModifierComponent.KEY.get(client.world).isModifier(client.player, NoellesModifierRegistry.VIOLATOR)) {
                return ClientInputApi.KeyResult.pass();
            }

            return context.query() == ClientInputApi.KeyQuery.IS_PRESSED
                    ? ClientInputApi.KeyResult.override(ClientInputApi.isPhysicallyPressed(client, context.keyBinding()))
                    : ClientInputApi.KeyResult.allow();
        });
    }
}
