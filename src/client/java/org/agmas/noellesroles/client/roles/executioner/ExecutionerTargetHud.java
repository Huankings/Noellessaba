package org.agmas.noellesroles.client.roles.executioner;

import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.executioner.ExecutionerPlayerComponent;

/**
 * 仇杀客左下角目标 HUD。
 */
public final class ExecutionerTargetHud {
    private ExecutionerTargetHud() {
    }

    public static void register() {
        NoellesHudSupport.registerAliveRole("roles/executioner/target", NoellesRoleRegistry.EXECUTIONER, context -> {
            /*
             * 仇杀客转化成可使用杀手能力的状态后，目标 HUD 不再显示。
             * 同时这个 provider 由 registerAliveRole 包住，死亡、旁观和创造模式都会统一隐藏。
             */
            if (context.gameWorld().getRole(context.player()).canUseKiller()) {
                return;
            }

            ExecutionerPlayerComponent executioner = ExecutionerPlayerComponent.KEY.get(context.player());
            if (executioner.target == null || context.player().networkHandler == null) {
                return;
            }

            PlayerListEntry targetEntry = context.player().networkHandler.getPlayerListEntry(executioner.target);
            if (targetEntry == null) {
                return;
            }

            Text name = Text.translatable("hud.executioner.target", targetEntry.getProfile().getName());
            PlayerSkinDrawer.draw(context.drawContext(), targetEntry.getSkinTextures().texture(), 2, context.height() - 14, 12);
            context.drawContext().drawTextWithShadow(context.textRenderer(), name, 18, context.height() - 12, Colors.RED);
        });
    }
}
