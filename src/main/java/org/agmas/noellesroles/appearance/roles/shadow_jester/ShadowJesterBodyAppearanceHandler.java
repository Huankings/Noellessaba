package org.agmas.noellesroles.appearance.roles.shadow_jester;

import dev.doctor4t.wathe.api.appearance.BodyAppearanceApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.shadow_jester.ShadowJesterComponent;
import org.agmas.noellesroles.roles.shadow_jester.ShadowJesterPhase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 影子小丑第三阶段后的尸体外观互换。
 *
 * <p>这里只改“尸体看起来像谁”，不改尸体真实归属。
 * 因此尸袋、验尸、死亡回放仍能找到真正死者，但肉眼看到的尸体皮肤会按缔结誓言显示为另一半。</p>
 */
public final class ShadowJesterBodyAppearanceHandler {
    private static final int PRIORITY = -50;

    private ShadowJesterBodyAppearanceHandler() {
    }

    public static void register() {
        BodyAppearanceApi.register(
                NoellesRolesCore.id("appearance/body/shadow_jester"),
                PRIORITY,
                ShadowJesterBodyAppearanceHandler::resolveAppearanceUuid
        );
    }

    private static @Nullable UUID resolveAppearanceUuid(
            @NotNull PlayerEntity victim,
            @Nullable PlayerEntity killer,
            @NotNull Identifier deathReason
    ) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(victim.getWorld());
        if (!gameWorld.isRole(victim, NoellesRoleRegistry.SHADOW_JESTER)) {
            return null;
        }

        ShadowJesterComponent component = ShadowJesterComponent.KEY.get(victim.getWorld());
        UUID partner = component.getPartner(victim.getUuid());
        if (partner == null || !component.getPhase(victim.getUuid()).atLeast(ShadowJesterPhase.VOW_BOUND)) {
            return null;
        }
        return partner;
    }
}
