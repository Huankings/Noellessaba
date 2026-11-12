package org.agmas.noellesroles.client.roles.magician;

import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.text.Text;
import org.agmas.noellesroles.entities.MagicianPlaybackEntity;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 魔术师播放体的准心名牌接入。
 */
public final class MagicianPlaybackNameHud {
    private MagicianPlaybackNameHud() {
    }

    public static void register() {
        RoleNameHudApi.registerEntityName(
                NoellesRolesCore.id("role_name/magician/playback_name"),
                RoleNameHudApi.DEFAULT_PRIORITY,
                (viewer, target) -> {
                    /*
                     * 皮套正在伪装成魔术师选中的玩家，因此所有视角都应看到这个伪装名字。
                     * “这是皮套”的额外身份只在下方观察者专属标签中揭露，存活玩家仍会被正常欺骗。
                     */
                    return target instanceof MagicianPlaybackEntity playback
                            ? Text.literal(playback.getDisguisePlayerName())
                            : null;
                }
        );

        RoleNameHudApi.registerAdditionalLine(
                NoellesRolesCore.id("role_name/magician/playback_observer_label"),
                50,
                context -> {
                    if (!(context.targetEntity() instanceof MagicianPlaybackEntity)
                            || !GameFunctions.isPlayerSpectatingOrCreative(context.player())
                            || context.nametagAlpha() <= 0.05F) {
                        return null;
                    }

                    Text label = Text.translatable("hud.noellesroles.magician.playback_label");
                    return RoleNameHudApi.AdditionalLine.below(
                            label,
                            NoellesRoleRegistry.MAGICIAN.color(),
                            1.0F
                    );
                }
        );
    }
}
