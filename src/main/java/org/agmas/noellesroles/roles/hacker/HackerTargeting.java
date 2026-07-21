package org.agmas.noellesroles.roles.hacker;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;

/**
 * 黑客破解目标过滤。
 *
 * <p>服务端破解计时、客户端准心 HUD 和本能颜色都必须使用同一套过滤规则：
 * 真杀手、黑客、梦者、Noelles 杀手侧中立和 Mimic 都不应该成为可破解目标。
 * 单独收口到这里可以避免客户端显示“可破解”，服务端却不计时的错位。</p>
 */
public final class HackerTargeting {
    private HackerTargeting() {
    }

    public static boolean countsAsFilteredKillerCohort(@NotNull GameWorldComponent gameWorld, @NotNull PlayerEntity player) {
        Role role = gameWorld.getRole(player);
        return role != null
                && (gameWorld.canUseKillerFeatures(player)
                || gameWorld.isRole(player, Noellesroles.HACKER)
                || gameWorld.isRole(player, Noellesroles.MIMIC)
                || Noellesroles.KILLER_SIDED_NEUTRALS.contains(role));
    }
}
