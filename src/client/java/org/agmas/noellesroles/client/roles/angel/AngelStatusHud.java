package org.agmas.noellesroles.client.roles.angel;

import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.angel.AngelAbility;
import org.agmas.noellesroles.roles.angel.AngelPlayerComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * 天使右下角 HUD。
 */
public final class AngelStatusHud {
    private AngelStatusHud() {
    }

    public static void register() {
        NoellesHudSupport.registerAliveRole("roles/angel/status", NoellesRoleRegistry.ANGEL, context -> {
            AngelPlayerComponent angel = AngelPlayerComponent.KEY.get(context.player());
            AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(context.player());
            boolean guardMode = AngelAbility.isGuardMode(context.player());

            List<Text> lines = new ArrayList<>();
            lines.add(Text.translatable(
                    "hud.noellesroles.angel.mode",
                    Text.translatable(guardMode ? "hud.noellesroles.angel.mode.guard" : "hud.noellesroles.angel.mode.soothe")
            ));
            lines.add(Text.translatable(
                    "hud.noellesroles.angel.guarded",
                    NoellesHudSupport.resolvePlayerName(
                            context.player(),
                            angel.getGuardedTarget(),
                            Text.translatable("hud.noellesroles.angel.none")
                    )
            ));

            if (ability.cooldown > 0) {
                lines.add(Text.translatable("tip.noellesroles.cooldown", Math.max(0, (ability.cooldown + 19) / 20)));
            } else if (guardMode) {
                lines.add(Text.translatable("hud.noellesroles.angel.use_guard", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText()));
            } else {
                lines.add(Text.translatable("hud.noellesroles.angel.use_soothe", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText()));
            }

            NoellesHudSupport.drawBottomRightLines(context, lines, NoellesRoleRegistry.ANGEL.color());
        });
    }
}
