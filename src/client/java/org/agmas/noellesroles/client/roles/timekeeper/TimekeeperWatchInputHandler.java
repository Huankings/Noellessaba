package org.agmas.noellesroles.client.roles.timekeeper;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.client.input.NoellesClientKeybindings;
import org.agmas.noellesroles.item.TimekeeperWatchItem;
import org.agmas.noellesroles.packet.role.timekeeper.TimekeeperWatchModeC2SPacket;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperConstants;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperWatchMode;

/** 时停者怀表左键切换模式处理。 */
public final class TimekeeperWatchInputHandler {
    private static boolean toggleHeld;

    private TimekeeperWatchInputHandler() {
    }

    public static boolean handleModeSwitch(PlayerEntity player) {
        if (!player.getMainHandStack().isOf(ModItems.DYING_WATCH)
                || !GameFunctions.isPlayerAliveAndSurvival(player)
                || !GameWorldComponent.KEY.get(player.getWorld()).isRole(player, NoellesRoleRegistry.TIMEKEEPER)) {
            return false;
        }
        if (toggleHeld) {
            return true;
        }
        toggleHeld = true;
        TimekeeperWatchMode nextMode = TimekeeperWatchItem.getMode(player.getMainHandStack()).next();
        TimekeeperWatchItem.setMode(player.getMainHandStack(), nextMode);
        player.sendMessage(Text.translatable("message.noellesroles.timekeeper.current_watch_mode", nextMode.text())
                .withColor(TimekeeperConstants.ROLE_COLOR), true);
        ClientPlayNetworking.send(new TimekeeperWatchModeC2SPacket(nextMode.ordinal()));
        return true;
    }

    public static void releaseIfAttackNotHeld(boolean attackHeld) {
        if (!attackHeld) {
            toggleHeld = false;
        }
    }

    public static void reset() {
        toggleHeld = false;
    }
}
