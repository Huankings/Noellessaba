package org.agmas.noellesroles.client.roles.starstruck;

import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

/**
 * 星界使者右下角能力提示。
 */
public final class StarstruckStatusHud {
    private StarstruckStatusHud() {
    }

    public static void register() {
        NoellesHudSupport.registerAliveRole("roles/starstruck/status", NoellesRoleRegistry.STARSTRUCK, context -> {
            AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(context.player());
            Text line = ability.cooldown > 0
                    ? Text.translatable("tip.noellesroles.cooldown", Math.max(0, (ability.cooldown + 19) / 20))
                    : Text.translatable("tip.noellesroles.starstruck", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText());

            NoellesHudSupport.drawBottomRightLine(context, line, NoellesRoleRegistry.STARSTRUCK.color());
        });
    }
}
