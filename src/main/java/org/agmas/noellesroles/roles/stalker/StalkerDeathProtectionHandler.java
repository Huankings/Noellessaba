package org.agmas.noellesroles.roles.stalker;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheSounds;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

/**
 * 潜行者一阶段免死处理器。
 */
public final class StalkerDeathProtectionHandler {

    private StalkerDeathProtectionHandler() {
    }

    /**
     * 处理潜行者第一阶段的一次性免死。
     *
     * <p>只要仍处于第一阶段且这次免死还没被消耗，就会挡住本次死亡。
     * 这层保护继续放在“炸弹 / 落轨强制放行”之前，保持原行为不变。</p>
     */
    public static boolean allowDeath(PlayerEntity playerEntity, PlayerEntity killer, Identifier deathReason) {
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(playerEntity.getWorld());
        if (!gameWorldComponent.isRole(playerEntity, Noellesroles.STALKER)) {
            return true;
        }

        StalkerPlayerComponent stalkerComp = StalkerPlayerComponent.KEY.get(playerEntity);
        if (stalkerComp.phase != 1 || stalkerComp.immunityUsed) {
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
        stalkerComp.immunityUsed = true;
        stalkerComp.sync();
        return false;
    }
}
