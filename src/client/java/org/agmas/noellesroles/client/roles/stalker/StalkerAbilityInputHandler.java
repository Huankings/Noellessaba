package org.agmas.noellesroles.client.roles.stalker;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheItems;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import org.agmas.noellesroles.client.input.NoellesClientKeybindings;
import org.agmas.noellesroles.packet.role.stalker.StalkerDashC2SPacket;
import org.agmas.noellesroles.packet.role.stalker.StalkerGazeC2SPacket;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.stalker.StalkerPlayerComponent;

/** 潜行者能力键和第三阶段刀蓄力的客户端输入处理。 */
public final class StalkerAbilityInputHandler {
    private static boolean gazePressed;
    private static boolean knifeUsing;

    private StalkerAbilityInputHandler() {
    }

    public static void tick(MinecraftClient client) {
        if (client.player == null || !GameWorldComponent.KEY.get(client.player.getWorld()).isRole(client.player, NoellesRoleRegistry.STALKER)) {
            gazePressed = false;
            knifeUsing = false;
            return;
        }
        StalkerPlayerComponent component = StalkerPlayerComponent.KEY.get(client.player);
        boolean abilityPressed = NoellesClientKeybindings.getAbilityBind() != null
                && NoellesClientKeybindings.getAbilityBind().isPressed();
        if (abilityPressed != gazePressed) {
            gazePressed = abilityPressed;
            if (component.phase < 3) {
                ClientPlayNetworking.send(new StalkerGazeC2SPacket(abilityPressed));
            } else {
                ClientPlayNetworking.send(new StalkerDashC2SPacket(abilityPressed));
            }
        }
        boolean currentKnifeUsing = component.phase == 3
                && client.player.isUsingItem()
                && client.player.getActiveItem().isOf(WatheItems.KNIFE);
        if (currentKnifeUsing != knifeUsing) {
            knifeUsing = currentKnifeUsing;
            ClientPlayNetworking.send(new StalkerDashC2SPacket(currentKnifeUsing));
        }
    }

    public static void reset() {
        gazePressed = false;
        knifeUsing = false;
    }
}
