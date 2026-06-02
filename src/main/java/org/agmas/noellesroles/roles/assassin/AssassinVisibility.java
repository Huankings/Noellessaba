package org.agmas.noellesroles.roles.assassin;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 刺客隐藏尸体的可见性规则。
 */
public final class AssassinVisibility {

    private static final Identifier PHYSICIAN_ROLE_ID = Identifier.of("kinswathe", "physician");

    private AssassinVisibility() {
    }

    /**
     * 判断某个玩家是否属于“可以看见刺客隐藏尸体”的观察者。
     *
     * <p>规则按需求收敛为：</p>
     * <p>1. 旁观者 / 创造模式始终可见；</p>
     * <p>2. 验尸官始终可见；</p>
     * <p>3. 若加载了 KinsWathe，则医师可见；</p>
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

        if (gameWorld.isRole(player, Noellesroles.CORONER)) {
            return true;
        }
        if (isPhysician(role)) {
            return true;
        }

        return !gameWorld.isInnocent(player);
    }

    public static boolean isPhysician(@Nullable Role role) {
        return role != null
                && FabricLoader.getInstance().isModLoaded("kinswathe")
                && PHYSICIAN_ROLE_ID.equals(role.identifier());
    }
}
