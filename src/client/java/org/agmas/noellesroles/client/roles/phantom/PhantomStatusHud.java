package org.agmas.noellesroles.client.roles.phantom;

import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

/**
 * 幻灵右下角能力 HUD。
 */
public final class PhantomStatusHud {
    private PhantomStatusHud() {
    }

    public static void register() {
        NoellesHudSupport.registerAliveRole("roles/phantom/status", NoellesRoleRegistry.PHANTOM, context -> {
            AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(context.player());
            Text line = Text.translatable("tip.phantom", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText());
            if (ability.cooldown > 0) {
                line = Text.translatable("tip.noellesroles.cooldown", ability.cooldown / 20);
            }

            NoellesHudSupport.drawBottomRightLine(context, line, Colors.RED);
        });
    }
}
