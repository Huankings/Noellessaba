package org.agmas.noellesroles.client.roles.rememberer;

import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

/**
 * 追忆者右下角提示 HUD。
 */
public final class RemembererStatusHud {
    private RemembererStatusHud() {
    }

    public static void register() {
        NoellesHudSupport.registerAliveRole("roles/rememberer/status", NoellesRoleRegistry.REMEMBERER, context -> {
            if (!RemembererClientEffects.shouldRenderRemembererHud(context.player())) {
                return;
            }

            AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(context.player());
            Text line = ability.cooldown > 0
                    ? Text.translatable("tip.noellesroles.cooldown", Math.max(0, (ability.cooldown + 19) / 20))
                    : Text.translatable("hud.noellesroles.rememberer.use", context.client().options.useKey.getBoundKeyLocalizedText());

            NoellesHudSupport.drawBottomRightLine(context, line, NoellesRoleRegistry.REMEMBERER.color());
        });
    }
}
