package org.agmas.noellesroles.client.input;

import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.client.roles.timekeeper.TimekeeperWatchInputHandler;

/**
 * 左键预攻击接管入口。
 *
 * <p>这里只有“是否吞掉原版攻击”的编排；扩展手雷已经由 Wathe GrenadeApi 统一识别，
 * 因此这里只保留怀表和狙击枪自己的特殊左键逻辑。</p>
 */
public final class NoellesClientPreAttackHandlers {
    private NoellesClientPreAttackHandlers() {
    }

    public static void register() {
        ClientPreAttackCallback.EVENT.register((client, player, clickCount) -> handle(player));
    }

    private static boolean handle(PlayerEntity player) {
        if (player.getMainHandStack().isOf(ModItems.SNIPER_RIFLE)) {
            // 存活狙击枪左键用于开镜；旁观/创造要保留原版观战附身和交互。
            return !GameFunctions.isPlayerSpectatingOrCreative(player);
        }
        if (TimekeeperWatchInputHandler.handleModeSwitch(player)) {
            return true;
        }
        return false;
    }
}
