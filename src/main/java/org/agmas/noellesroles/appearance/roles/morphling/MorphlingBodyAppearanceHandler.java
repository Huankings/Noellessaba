package org.agmas.noellesroles.appearance.roles.morphling;

import dev.doctor4t.wathe.api.appearance.BodyAppearanceApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.morphling.MorphBodyDisguiseWorldComponent;
import org.agmas.noellesroles.roles.morphling.MorphMarkPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 变形试剂死亡尸体外观。
 *
 * <p>Wathe 的 BodyAppearanceApi 在尸体生成前询问“这具尸体应该看起来像谁”。
 * 这里如果发现受害者正处于试剂伪装，就返回样本 UUID，让尸体默认显示为样本玩家；
 * 同时在世界组件里记录来源，方便客户端在杀手/旁观者本能视角下把这类尸体显回真实死者。</p>
 */
public final class MorphlingBodyAppearanceHandler {
    private static final int PRIORITY = 100;

    private MorphlingBodyAppearanceHandler() {
    }

    public static void register() {
        BodyAppearanceApi.register(
                NoellesRolesCore.id("appearance/body/morphling_reagent"),
                PRIORITY,
                MorphlingBodyAppearanceHandler::resolveAppearanceUuid
        );
    }

    private static @Nullable UUID resolveAppearanceUuid(
            @NotNull PlayerEntity victim,
            @Nullable PlayerEntity killer,
            @NotNull Identifier deathReason
    ) {
        GameWorldComponent.GameStatus status = GameWorldComponent.KEY.get(victim.getWorld()).getGameStatus();
        if (status != GameWorldComponent.GameStatus.ACTIVE && status != GameWorldComponent.GameStatus.STOPPING) {
            return null;
        }

        MorphMarkPlayerComponent component = MorphMarkPlayerComponent.KEY.get(victim);
        UUID sampleUuid = component.sampleUuid();
        if (!component.isActive() || sampleUuid == null) {
            return null;
        }

        MorphBodyDisguiseWorldComponent.KEY.get(victim.getWorld())
                .recordBodyDisguise(victim.getUuid(), sampleUuid, component.sampleName());
        return sampleUuid;
    }
}
