package org.agmas.noellesroles.client.roles.dreamer;

import net.minecraft.text.Text;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.dreamer.DreamerKillerComponent;

/**
 * 梦者右下角计数 HUD。
 */
public final class DreamerStatusHud {
    private DreamerStatusHud() {
    }

    public static void register() {
        NoellesHudSupport.registerAliveRole("roles/dreamer/status", NoellesRoleRegistry.DREAMER, context -> {
            DreamerKillerComponent dreamer = DreamerKillerComponent.KEY.get(context.player());
            if (dreamer.hasBecomeKiller()) {
                return;
            }

            Text line = Text.translatable("tip.noellesroles.dreamer.counts", dreamer.dreamerCounts, dreamer.dreamerRequired);
            NoellesHudSupport.drawBottomRightLine(context, line, NoellesRoleRegistry.DREAMER.color());
        });
    }
}
