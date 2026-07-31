package org.agmas.noellesroles.roles.assassin;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 刺客隐藏尸体的可见性规则。
 */
public final class AssassinVisibility {

    private AssassinVisibility() {
    }

    /**
     * 判断某个玩家是否属于“可以看见刺客隐藏尸体”的观察者。
     *
     * <p>规则按需求收敛为：</p>
     * <p>1. Wathe 统一定义的“旁观 / 创造非存活视角”可见；</p>
     * <p>2. 验尸官始终可见；</p>
     * <p>3. NoellesRoles 内的医师可见；</p>
     * <p>4. 其他无辜阵营玩家不可见；</p>
     * <p>5. 杀手、中立等非无辜阵营可见。</p>
     */
    public static boolean canPlayerSeeHiddenBodies(@NotNull PlayerEntity player) {
        if (GameFunctions.isPlayerSpectatingOrCreative(player)) {
            return true;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        Role role = gameWorld.getRole(player);
        if (role == null) {
            return false;
        }

        if (gameWorld.isRole(player, NoellesRoleRegistry.CORONER)) {
            return true;
        }
        if (isPhysician(role)) {
            return true;
        }

        return !gameWorld.isInnocent(player);
    }

    public static boolean isPhysician(@Nullable Role role) {
        if (role == null) {
            return false;
        }
        return role == NoellesRoleRegistry.PHYSICIAN;
    }
}
