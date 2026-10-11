package org.agmas.noellesroles.client.roles.myers;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import org.agmas.noellesroles.packet.role.myers.MyersAbsorbC2SPacket;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.client.input.NoellesClientKeybindings;

/** Myers 能力键的按下/释放边沿处理。 */
public final class MyersAbilityInputHandler {
    private static boolean pressed;

    private MyersAbilityInputHandler() {
    }

    public static void tick(MinecraftClient client) {
        if (client.player == null || !GameWorldComponent.KEY.get(client.player.getWorld()).isRole(client.player, NoellesRoleRegistry.MYERS)) {
            pressed = false;
            return;
        }
        boolean current = NoellesClientKeybindings.getAbilityBind() != null
                && NoellesClientKeybindings.getAbilityBind().isPressed();
        if (current != pressed) {
            pressed = current;
            ClientPlayNetworking.send(new MyersAbsorbC2SPacket(current));
        }
    }

    public static void reset() {
        pressed = false;
    }
}
