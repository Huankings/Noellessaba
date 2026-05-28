package org.agmas.noellesroles.roles.jester;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

/**
 * 狂信者 psycho 无敌时间的死亡保护处理器。
 */
public final class JesterDeathProtectionHandler {

    private JesterDeathProtectionHandler() {
    }

    /**
     * 处理 Jester psycho 状态下的免死时间。
     *
     * <p>旧逻辑里它位于“炸弹 / 落轨强制放行”之后，
     * 也就是说这层保护不会拦截那两类强制死亡。
     * 当前由死亡引导器负责保持该顺序。</p>
     */
    public static boolean allowDeath(PlayerEntity playerEntity, PlayerEntity killer, Identifier deathReason) {
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(playerEntity.getWorld());
        if (!gameWorldComponent.isRole(playerEntity, Noellesroles.JESTER)) {
            return true;
        }

        PlayerPsychoComponent component = PlayerPsychoComponent.KEY.get(playerEntity);
        return component.getPsychoTicks() <= GameConstants.getInTicks(0, 44);
    }
}
