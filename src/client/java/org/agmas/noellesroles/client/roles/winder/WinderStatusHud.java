package org.agmas.noellesroles.client.roles.winder;

import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.winder.WinderPlayerComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * 风灵师右下角 HUD。
 */
public final class WinderStatusHud {
    private static final Text UNKNOWN_PLAYER = Text.literal("未知玩家");

    private WinderStatusHud() {
    }

    public static void register() {
        NoellesHudSupport.registerAliveRole("roles/winder/status", NoellesRoleRegistry.WINDER, context -> {
            WinderPlayerComponent winder = WinderPlayerComponent.KEY.get(context.player());
            AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(context.player());

            List<Text> lines = new ArrayList<>();
            lines.add(Text.translatable(
                    "tip.winder.selected",
                    NoellesHudSupport.resolvePlayerName(context.player(), winder.getSelectedTarget(), UNKNOWN_PLAYER)
            ));

            if (winder.isFloatingActive()) {
                lines.add(Text.translatable("tip.winder.stop", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText()));
                lines.add(Text.translatable("tip.winder.active", Math.max(0, (winder.getFloatingTicksRemaining() + 19) / 20)));
            } else if (ability.cooldown > 0) {
                lines.add(Text.translatable("tip.noellesroles.cooldown", Math.max(0, (ability.cooldown + 19) / 20)));
            } else {
                lines.add(Text.translatable("tip.winder.use", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText()));
            }

            NoellesHudSupport.drawBottomRightLines(context, lines, NoellesRoleRegistry.WINDER.color());
        });
    }
}
