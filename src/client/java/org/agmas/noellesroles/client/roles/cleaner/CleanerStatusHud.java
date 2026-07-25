package org.agmas.noellesroles.client.roles.cleaner;

import dev.doctor4t.wathe.cca.PlayerShopComponent;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.cleaner.CleanerConstants;

/**
 * 清道夫右下角能力 HUD。
 */
public final class CleanerStatusHud {
    private CleanerStatusHud() {
    }

    public static void register() {
        NoellesHudSupport.registerAliveRole("roles/cleaner/status", NoellesRoleRegistry.CLEANER, context -> {
            if (NoellesrolesClient.abilityBind == null) {
                return;
            }

            AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(context.player());
            PlayerShopComponent shop = PlayerShopComponent.KEY.get(context.player());
            Text line;
            if (shop.balance < CleanerConstants.ABILITY_PRICE) {
                line = Text.translatable("tip.noellesroles.cleaner.not_enough_money", CleanerConstants.ABILITY_PRICE);
            } else if (ability.cooldown > 0) {
                line = Text.translatable("tip.noellesroles.cooldown", Math.max(0, (ability.cooldown + 19) / 20));
            } else {
                line = Text.translatable("tip.noellesroles.cleaner.use", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText());
            }

            NoellesHudSupport.drawBottomRightLine(context, line, NoellesRoleRegistry.CLEANER.color());
        });
    }
}
