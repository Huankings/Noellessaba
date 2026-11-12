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
        RoleNameHudApi.registerAdditionalLine(
                NoellesRolesCore.id("role_name/conductor/master_key"),
                RoleNameHudApi.DEFAULT_PRIORITY,
                context -> {
                    if (!GameFunctions.isPlayerAliveAndSurvival(context.player())
                            || !context.player().getMainHandStack().isOf(ModItems.MASTER_KEY)) {
                        return null;
                    }

                    ConfigWorldComponent config = ConfigWorldComponent.KEY.get(context.player().getWorld());
                    if (config.masterKeyIsVisible) {
                        return null;
                    }

                    Text text = config.masterKeyVisibleCount != 0
                            ? Text.translatable("tip.master_key_invisible_count", config.masterKeyVisibleCount)
                            : Text.translatable("tip.master_key_invisible");

                    return RoleNameHudApi.AdditionalLine.below(text, Colors.GRAY, 1.0F);
                }
        );
    }
}
