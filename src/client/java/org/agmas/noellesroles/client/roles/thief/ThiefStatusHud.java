package org.agmas.noellesroles.client.roles.thief;

import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

/**
 * 小偷右下角偷取冷却 HUD。
 */
public final class ThiefStatusHud {
    private static final int RIGHT_MARGIN = 10;
    private static final int BOTTOM_MARGIN = 10;

    private ThiefStatusHud() {
    }

    public static void register() {
        NoellesHudSupport.registerAliveRole("roles/thief/status", NoellesRoleRegistry.THIEF, context -> {
            if (context.debugHudVisible()) {
                return;
            }

            AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(context.player());
            Text keyName = context.client().options.useKey.getBoundKeyLocalizedText();
            Text displayText = ability.cooldown > 0
                    ? Text.translatable("hud.noellesroles.thief.cooldown", Math.max(0, (ability.cooldown + 19) / 20), keyName)
                    : Text.translatable("hud.noellesroles.thief.ready", keyName);

            int x = context.width() - context.textRenderer().getWidth(displayText) - RIGHT_MARGIN;
            int y = context.height() - BOTTOM_MARGIN;
            context.drawContext().drawTextWithShadow(context.textRenderer(), displayText, x, y, NoellesRoleRegistry.THIEF.color());
        });
    }
}
