package org.agmas.noellesroles.modifiers.feather;

import dev.doctor4t.wathe.api.collision.PlayerCollisionApi;
import dev.doctor4t.wathe.api.collision.PlayerCollisionMode;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.agmas.noellesroles.registry.NoellesModifierRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * FEATHER 词条的玩家碰撞 API 接入。
 */
public final class FeatherPlayerCollisionHandler {
    private static boolean initialized = false;

    private FeatherPlayerCollisionHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        PlayerCollisionApi.registerRule(
                NoellesRolesCore.id("collision/modifier/feather"),
                500,
                context -> {
                    WorldModifierComponent modifiers = WorldModifierComponent.KEY.get(context.world());
                    if (modifiers.isModifier(context.self(), NoellesModifierRegistry.FEATHER)
                            || modifiers.isModifier(context.other(), NoellesModifierRegistry.FEATHER)) {
                        /*
                         * 羽化者沿用旧 mixin 的玩法语义：只要任意一方带 FEATHER，
                         * 这对玩家就不再吃 Wathe 的实体墙阻挡，但仍保留原版玩家轻微推挤。
                         */
                        return PlayerCollisionMode.VANILLA_PUSH;
                    }
                    return PlayerCollisionMode.PASS;
                }
        );
    }
}
