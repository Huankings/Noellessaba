package org.agmas.noellesroles.client.roles.myers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.myers.MyersPlayerComponent;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperPlayerComponent;

/** 迈尔斯右下角恶意吸收提示。 */
public final class MyersStatusHud {
    private MyersStatusHud() {
    }

    public static void register() {
        NoellesHudSupport.registerAliveRole("roles/myers/status", NoellesRoleRegistry.MYERS, context -> {
            MyersPlayerComponent component = MyersPlayerComponent.KEY.get(context.player());
            if (TimekeeperPlayerComponent.KEY.get(context.player()).isInTimeRift()) return;
            if (NoellesrolesClient.abilityBind == null) return;
            Text text = component.isAbsorbing()
                    ? Text.translatable("hud.noellesroles.myers.absorbing", component.getAbsorbTargetCount())
                    : Text.translatable("hud.noellesroles.myers.ready",
                    NoellesrolesClient.abilityBind.getBoundKeyLocalizedText(), component.getAbsorbTargetCount());
            NoellesHudSupport.drawBottomRightLine(context, text, org.agmas.noellesroles.roles.myers.MyersConstants.ROLE_COLOR);
        });
    }
}
