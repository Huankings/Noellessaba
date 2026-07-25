package org.agmas.noellesroles.client.roles.vulture;

import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.vulture.VulturePlayerComponent;

/**
 * 秃鹫右下角能力 HUD。
 */
public final class VultureStatusHud {
    private VultureStatusHud() {
    }

    public static void register() {
        NoellesHudSupport.registerAliveRole("roles/vulture/status", NoellesRoleRegistry.VULTURE, context -> {
            AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(context.player());
            VulturePlayerComponent vulture = VulturePlayerComponent.KEY.get(context.player());
            Text line = Text.translatable("tip.vulture", vulture.bodiesEaten, vulture.bodiesRequired);
            if (ability.cooldown > 0) {
                line = Text.translatable("tip.noellesroles.cooldown", ability.cooldown / 20);
            }

            NoellesHudSupport.drawBottomRightLine(context, line, NoellesRoleRegistry.VULTURE.color());
        });
    }
}
