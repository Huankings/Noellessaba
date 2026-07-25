package org.agmas.noellesroles.client.roles.hunter;

import dev.doctor4t.wathe.cca.PlayerShopComponent;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.hunter.HunterConstants;

/**
 * 追猎者右下角能力 HUD。
 */
public final class HunterStatusHud {
    private HunterStatusHud() {
    }

    public static void register() {
        NoellesHudSupport.registerAliveRole("roles/hunter/status", NoellesRoleRegistry.HUNTER, context -> {
            if (NoellesrolesClient.abilityBind == null) {
                return;
            }

            AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(context.player());
            PlayerShopComponent shop = PlayerShopComponent.KEY.get(context.player());
            Text line;
            if (shop.balance < HunterConstants.ABILITY_PRICE) {
                line = Text.translatable("tip.noellesroles.hunter.not_enough_money", HunterConstants.ABILITY_PRICE);
            } else if (ability.cooldown > 0) {
                line = Text.translatable("tip.noellesroles.cooldown", Math.max(0, (ability.cooldown + 19) / 20));
            } else {
                line = Text.translatable("tip.noellesroles.hunter.use", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText());
            }

            NoellesHudSupport.drawBottomRightLine(context, line, NoellesRoleRegistry.HUNTER.color());
        });
    }
}
