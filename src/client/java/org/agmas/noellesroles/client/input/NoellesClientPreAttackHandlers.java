package org.agmas.noellesroles.client.input;

import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.client.items.NoellesRolesGrenadeInputHandler;
import org.agmas.noellesroles.client.roles.timekeeper.TimekeeperWatchInputHandler;

/**
 * 左键预攻击接管入口。
 *
 * <p>这里只有“是否吞掉原版攻击”的编排；怀表、手雷和狙击枪的具体规则
 * 分别放在对应物品处理器中，避免输入机制重新堆回客户端主类。</p>
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
        return NoellesRolesGrenadeInputHandler.handleModeSwitch(player);
    }
}
