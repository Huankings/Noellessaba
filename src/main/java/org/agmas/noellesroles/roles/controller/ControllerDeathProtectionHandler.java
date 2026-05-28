package org.agmas.noellesroles.roles.controller;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheSounds;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

/**
 * 附体师护甲免死处理器。
 */
public final class ControllerDeathProtectionHandler {

    private ControllerDeathProtectionHandler() {
    }

    /**
     * 处理附体师的一次性护甲。
     *
     * <p>旧实现里这层护甲优先级非常高：
     * 只要附体师本人身上还有护甲，就会在 Jester、试剂护盾、先知免伤之前先消耗掉。</p>
     */
    public static boolean allowDeath(PlayerEntity playerEntity, PlayerEntity killer, Identifier deathReason) {
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(playerEntity.getWorld());
        if (!gameWorldComponent.isRole(playerEntity, Noellesroles.CONTROLLER)) {
            return true;
        }

        ControllerPlayerComponent controllerComp = ControllerPlayerComponent.KEY.get(playerEntity);
        if (!controllerComp.hasArmor) {
            return true;
        }

        playerEntity.getWorld().playSound(
                playerEntity,
                playerEntity.getBlockPos(),
                WatheSounds.ITEM_PSYCHO_ARMOUR,
                SoundCategory.MASTER,
                5.0F,
                1.0F
        );
        controllerComp.hasArmor = false;
        controllerComp.sync();
        return false;
    }
}
