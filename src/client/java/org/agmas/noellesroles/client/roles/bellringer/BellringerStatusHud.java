package org.agmas.noellesroles.client.roles.bellringer;

import dev.doctor4t.wathe.cca.PlayerShopComponent;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.input.NoellesClientKeybindings;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.bellringer.BellringerConstants;

/**
 * 敲钟人右下角能力 HUD。
 */
public final class BellringerStatusHud {
    private BellringerStatusHud() {
    }

    public static void register() {
        NoellesHudSupport.registerAliveRole("roles/bellringer/status", NoellesRoleRegistry.BELLRINGER, context -> {
            if (NoellesClientKeybindings.getAbilityBind() == null) {
                return;
            }

            AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(context.player());
            PlayerShopComponent shop = PlayerShopComponent.KEY.get(context.player());
            Text line;
            if (shop.balance < BellringerConstants.ABILITY_PRICE) {
                line = Text.translatable("tip.noellesroles.bellringer.not_enough_money", BellringerConstants.ABILITY_PRICE);
            } else if (ability.cooldown > 0) {
                line = Text.translatable("tip.noellesroles.cooldown", Math.max(0, (ability.cooldown + 19) / 20));
            } else {
                line = Text.translatable("tip.noellesroles.bellringer.use", NoellesClientKeybindings.getAbilityBind().getBoundKeyLocalizedText());
            }

            NoellesHudSupport.drawBottomRightLine(context, line, NoellesRoleRegistry.BELLRINGER.color());
        });
    }
}
