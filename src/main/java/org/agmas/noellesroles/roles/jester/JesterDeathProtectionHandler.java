package org.agmas.noellesroles.roles.jester;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.psycho.PsychoModeApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

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
        if (!gameWorldComponent.isRole(playerEntity, NoellesRoleRegistry.JESTER)) {
            return true;
        }

        if (JesterPsychoHandler.tryTriggerFromDeath(playerEntity, killer)) {
            /*
             * 狂信者第一次被无辜者杀死时，死亡事件本身要被取消，
             * 由专属 profile 启动疯魔；持续时间、护盾、物品和结束回放都交给 Wathe API。
             */
            return false;
        }

        if (!PsychoModeApi.isActive(playerEntity, JesterPsychoHandler.PROFILE_ID)) {
            return true;
        }

        return PsychoModeApi.getRemainingTicks(playerEntity) <= JesterPsychoHandler.JESTER_INVULNERABLE_END_TICKS;
    }
}
