package org.agmas.noellesroles.roles.muzzler;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;

/**
 * 静语者疯魔音效屏蔽的共用判定。
 *
 * <p>服务端 bat_hit 音效和客户端 psycho_drone 背景音都需要同一套规则：
 * 静语者自己的疯魔应静音，但其他职业正常疯魔时不能被误伤。</p>
 */
public final class MuzzlerPsychoUtil {
    private MuzzlerPsychoUtil() {
    }

    public static boolean isMuzzlerPsycho(PlayerEntity player) {
        if (player == null) {
            return false;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        return game.isRole(player, Noellesroles.MUZZLER)
                && PlayerPsychoComponent.KEY.get(player).getPsychoTicks() > 0;
    }

    public static boolean hasNonMuzzlerPsycho(World world) {
        if (world == null) {
            return false;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        for (PlayerEntity player : world.getPlayers()) {
            if (PlayerPsychoComponent.KEY.get(player).getPsychoTicks() <= 0) {
                continue;
            }
            if (!game.isRole(player, Noellesroles.MUZZLER)) {
                return true;
            }
        }
        return false;
    }
}
