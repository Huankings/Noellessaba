package org.agmas.noellesroles.client.roles.morphling;

import net.minecraft.text.Text;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.morphling.MorphlingPlayerComponent;

/**
 * 变形怪右下角 HUD。
 */
public final class MorphlingStatusHud {
    private MorphlingStatusHud() {
    }

    public static void register() {
        NoellesHudSupport.registerAliveRole("roles/morphling/status", NoellesRoleRegistry.MORPHLING, context -> {
            MorphlingPlayerComponent morph = MorphlingPlayerComponent.KEY.get(context.player());
            Text line;
            if (morph.getMorphTicks() > 0) {
                line = Text.translatable("hud.noellesroles.morphling.active", Math.max(1, (int) Math.ceil(morph.getMorphTicks() / 20.0)));
            } else if (morph.getMorphTicks() < 0) {
                line = Text.translatable("hud.noellesroles.morphling.cooldown", Math.max(1, (int) Math.ceil((-morph.getMorphTicks()) / 20.0)));
            } else {
                line = Text.translatable("hud.noellesroles.morphling.ready");
            }

            NoellesHudSupport.drawBottomRightLine(context, line, NoellesRoleRegistry.MORPHLING.color());
        });
    }
}
