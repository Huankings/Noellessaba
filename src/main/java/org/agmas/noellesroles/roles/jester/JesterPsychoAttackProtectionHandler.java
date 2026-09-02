package org.agmas.noellesroles.roles.jester;

import dev.doctor4t.wathe.api.psycho.PsychoModeApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.config.NoellesRolesConfig;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * 狂信者疯魔期间攻击杀手的兼容规则。
 *
 * <p>kinssaba 旧实现拦截“受害者拥有杀手能力、攻击者是疯魔狂信者”的死亡请求，
 * 但保留 BAT 死因例外。这里接入 NoellesRoles 死亡保护链并使用公开 PsychoModeApi。</p>
 */
public final class JesterPsychoAttackProtectionHandler {
    private JesterPsychoAttackProtectionHandler() {
    }

    public static boolean allowDeath(PlayerEntity victim, @Nullable PlayerEntity killer, Identifier deathReason) {
        if (!NoellesRolesConfig.HANDLER.instance().jesterPsychoCannotAttackKiller
                || killer == null
                || deathReason.equals(GameConstants.DeathReasons.BAT)) {
            return true;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(victim.getWorld());
        if (gameWorld.canUseKillerFeatures(victim)
                && gameWorld.isRole(killer, NoellesRoleRegistry.JESTER)
                && PsychoModeApi.isActive(killer, JesterPsychoHandler.PROFILE_ID)) {
            return false;
        }
        return true;
    }
}
