package org.agmas.noellesroles.client.roles.magician;

import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.text.Text;
import org.agmas.noellesroles.entities.MagicianPlaybackEntity;
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
                     * 播放体名牌本质仍是局内职业 HUD。
                     * 这里跟随 Wathe 的存活定义统一拦截，避免死亡/旁观视角继续显示旧 mixin 的播放体名字。
                     */
                    if (!GameFunctions.isPlayerAliveAndSurvival(viewer)) {
                        return null;
                    }
                    return target instanceof MagicianPlaybackEntity playback
                            ? Text.literal(playback.getDisguisePlayerName())
                            : null;
                }
        );
    }
}
