package org.agmas.noellesroles.client.roles.recaller;

import dev.doctor4t.wathe.cca.PlayerShopComponent;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.recaller.RecallerConstants;
import org.agmas.noellesroles.roles.recaller.RecallerPlayerComponent;

/**
 * 回溯者右下角能力 HUD。
 */
public final class RecallerStatusHud {
    private RecallerStatusHud() {
    }

    public static void register() {
        NoellesHudSupport.registerAliveRole("roles/recaller/status", NoellesRoleRegistry.RECALLER, context -> {
            AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(context.player());
            RecallerPlayerComponent recaller = RecallerPlayerComponent.KEY.get(context.player());
            PlayerShopComponent shop = PlayerShopComponent.KEY.get(context.player());

            Text line = Text.translatable("tip.recaller.teleport", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText());
            if (!recaller.placed) {
                line = Text.translatable("tip.recaller.place", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText());
            } else if (shop.balance < RecallerConstants.TELEPORT_COST) {
                line = Text.translatable("tip.recaller.not_enough_money", RecallerConstants.TELEPORT_COST);
            }

            if (ability.cooldown > 0) {
                line = Text.translatable("tip.noellesroles.cooldown", ability.cooldown / 20);
            }

            NoellesHudSupport.drawBottomRightLine(context, line, NoellesRoleRegistry.RECALLER.color());
        });
    }
}
