package org.agmas.noellesroles.roles.insane_damned_paranoid_killer;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

/**
 * 亡语杀手职业分配初始化。
 */
public final class InsaneDamnedKillerRoleAssignedHandler {
    private InsaneDamnedKillerRoleAssignedHandler() {
    }

    public static void onRoleAssigned(PlayerEntity player, Role role) {
        /*
         * 无论这次分配成什么职业，都先清掉亡语杀手的旧尸体状态。
         * 这样转职、重分配和调试指令不会把“躺尸中”的运行态残留给非亡语杀手。
         */
        InsaneDamnedKillerPlayerComponent.KEY.get(player).reset();

        if (role != NoellesRoleRegistry.THE_INSANE_DAMNED_PARANOID_KILLER_OF_DOOM_DEATH_DESTRUCTION_AND_WAFFLES) {
            return;
        }

        /*
         * 尸体伪装按 spark 版机制是纯开关，不消耗通用能力冷却。
         * 角色分配总入口会先写入全职业默认冷却，这里必须覆盖成 0，
         * 否则开局一段时间内按 G 不会响应，看起来像能力失效。
         */
        AbilityPlayerComponent.KEY.get(player).setCooldown(0);
    }
}
