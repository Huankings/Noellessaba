package org.agmas.noellesroles.client.roles.prophet;

import dev.doctor4t.wathe.cca.PlayerShopComponent;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.prophet.ProphetConstants;
import org.agmas.noellesroles.roles.prophet.ProphetPlayerComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * 先知右下角 HUD。
 */
public final class ProphetStatusHud {
    private ProphetStatusHud() {
    }

    public static void register() {
        NoellesHudSupport.registerAliveRole("roles/prophet/status", NoellesRoleRegistry.PROPHET, context -> {
            ProphetPlayerComponent prophet = ProphetPlayerComponent.KEY.get(context.player());
            AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(context.player());
            PlayerShopComponent shop = PlayerShopComponent.KEY.get(context.player());

            Text unknown = Text.translatable("message.noellesroles.prophet.unknown_player");
            List<Text> lines = new ArrayList<>();
            if (prophet.hasMarkedTarget()) {
                lines.add(Text.translatable(
                        "tip.prophet.target_marked",
                        NoellesHudSupport.resolvePlayerName(context.player(), prophet.getMarkedTarget(), unknown)
                ));
            } else {
                lines.add(Text.translatable("tip.prophet.random_reveal"));
            }

            if (ability.cooldown > 0) {
                lines.add(Text.translatable("tip.noellesroles.cooldown", Math.max(0, (ability.cooldown + 19) / 20)));
            } else if (shop.balance < ProphetConstants.REVEAL_COST) {
                lines.add(Text.translatable("tip.prophet.not_enough_money", ProphetConstants.REVEAL_COST));
            } else {
                lines.add(Text.translatable("tip.prophet.use", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText()));
            }

            NoellesHudSupport.drawBottomRightLines(context, lines, NoellesRoleRegistry.PROPHET.color());
        });
    }
}
