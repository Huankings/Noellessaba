package org.agmas.noellesroles.appearance;

import org.agmas.noellesroles.appearance.modifiers.dual_personality.DualPersonalityBodyAppearanceHandler;
import org.agmas.noellesroles.appearance.roles.morphling.MorphlingBodyAppearanceHandler;

/**
 * NoellesRoles 服务端尸体外观 API 注册入口。
 */
public final class NoellesBodyAppearanceHandlers {
    private NoellesBodyAppearanceHandlers() {
    }

    public static void register() {
        MorphlingBodyAppearanceHandler.register();
        DualPersonalityBodyAppearanceHandler.register();
    }
}
