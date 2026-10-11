package org.agmas.noellesroles.client.items;

import dev.doctor4t.wathe.cca.PlayerGrenadeComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.util.GrenadeThrowModePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.agmas.noellesroles.ModItems;

/** 扩展手雷的直投/蓄力投掷模式输入和提示。 */
public final class NoellesRolesGrenadeInputHandler {
    private static boolean toggleHeld;
    private static int lastSelectedSlot = -1;

    private NoellesRolesGrenadeInputHandler() {
    }

    public static boolean handleModeSwitch(PlayerEntity player) {
        if (!isThrowableGrenade(player.getMainHandStack()) || !GameFunctions.isPlayerAliveAndSurvival(player)) {
            return false;
        }
        if (toggleHeld) {
            return true;
        }
        toggleHeld = true;
        PlayerGrenadeComponent component = PlayerGrenadeComponent.KEY.get(player);
        component.toggleLocal();
        ClientPlayNetworking.send(new GrenadeThrowModePayload(component.isDirectThrowMode()));
        WatheClient.showGrenadeThrowModeSwitchMessage(player);
        return true;
    }

    public static void tick(MinecraftClient client) {
        if (client.player == null) {
            lastSelectedSlot = -1;
            return;
        }
        PlayerEntity player = client.player;
        boolean holding = isThrowableGrenade(player.getMainHandStack());
        boolean usable = GameFunctions.isPlayerAliveAndSurvival(player);
        if (holding && usable && lastSelectedSlot != player.getInventory().selectedSlot) {
            WatheClient.showGrenadeThrowModeMessage(player, "tip.grenade.current_throw_mode");
            lastSelectedSlot = player.getInventory().selectedSlot;
        } else if (!holding || !usable) {
            lastSelectedSlot = -1;
        }
    }

    public static void releaseIfAttackNotHeld(boolean attackHeld) {
        if (!attackHeld) {
            toggleHeld = false;
        }
    }

    public static boolean isThrowableGrenade(ItemStack stack) {
        return stack.isOf(ModItems.SILENT_GRENADE) || stack.isOf(ModItems.FAKE_GRENADE);
    }

    public static void reset() {
        toggleHeld = false;
        lastSelectedSlot = -1;
    }
}
