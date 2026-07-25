package org.agmas.noellesroles.client.roles.conductor;

import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import org.agmas.noellesroles.ConfigWorldComponent;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 万能钥匙不可见状态的准心提示。
 */
public final class MasterKeyHud {
    private MasterKeyHud() {
    }

    public static void register() {
        RoleNameHudApi.registerExtraHud(
                NoellesRolesCore.id("role_name/conductor/master_key"),
                RoleNameHudApi.DEFAULT_PRIORITY,
                context -> {
                    if (!GameFunctions.isPlayerAliveAndSurvival(context.player())
                            || !context.player().getMainHandStack().isOf(ModItems.MASTER_KEY)) {
                        return;
                    }

                    ConfigWorldComponent config = ConfigWorldComponent.KEY.get(context.player().getWorld());
                    if (config.masterKeyIsVisible) {
                        return;
                    }

                    Text text = config.masterKeyVisibleCount != 0
                            ? Text.translatable("tip.master_key_invisible_count", config.masterKeyVisibleCount)
                            : Text.translatable("tip.master_key_invisible");

                    context.drawContext().getMatrices().push();
                    context.drawContext().getMatrices().translate(context.drawContext().getScaledWindowWidth() / 2.0F, context.drawContext().getScaledWindowHeight() / 2.0F + 6.0F, 0.0F);
                    context.drawContext().getMatrices().scale(0.6F, 0.6F, 1.0F);
                    context.drawContext().drawTextWithShadow(context.renderer(), text, -context.renderer().getWidth(text) / 2, 32, Colors.GRAY);
                    context.drawContext().getMatrices().pop();
                }
        );
    }
}
