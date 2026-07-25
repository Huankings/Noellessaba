package org.agmas.noellesroles.appearance.modifiers.dual_personality;

import dev.doctor4t.wathe.api.appearance.BodyAppearanceApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.modifiers.dual_personality.DualPersonalityComponent;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 双重人格死亡尸体外观。
 */
public final class DualPersonalityBodyAppearanceHandler {
    private static final int PRIORITY = -100;

    private DualPersonalityBodyAppearanceHandler() {
    }

    public static void register() {
        BodyAppearanceApi.register(
                NoellesRolesCore.id("appearance/body/dual_personality"),
                PRIORITY,
                DualPersonalityBodyAppearanceHandler::resolveAppearanceUuid
        );
    }

    private static @Nullable UUID resolveAppearanceUuid(
            @NotNull PlayerEntity victim,
            @Nullable PlayerEntity killer,
            @NotNull net.minecraft.util.Identifier deathReason
    ) {
        GameWorldComponent.GameStatus status = GameWorldComponent.KEY.get(victim.getWorld()).getGameStatus();
        if (status != GameWorldComponent.GameStatus.ACTIVE && status != GameWorldComponent.GameStatus.STOPPING) {
            return null;
        }

        DualPersonalityComponent.PairState pair = DualPersonalityComponent.KEY.get(victim.getWorld()).getPair(victim.getUuid());
        if (pair == null || !pair.isSub(victim.getUuid())) {
            return null;
        }

        /*
         * 副人格在没有更高优先级伪装时显示为主人格。
         * 尸体这里只返回“外观看起来像谁”，Wathe 仍会保留真正死者 owner UUID，
         * 因此尸袋、验尸和回放不会被伪装外观污染。
         */
        return pair.main;
    }
}
