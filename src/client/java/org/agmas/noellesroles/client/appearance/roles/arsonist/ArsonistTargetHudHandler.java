package org.agmas.noellesroles.client.appearance.roles.arsonist;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;
import org.agmas.noellesroles.roles.arsonist.DousedPlayerComponent;

/**
 * 纵火犯准心浇油状态提示。
 */
public final class ArsonistTargetHudHandler {
    private ArsonistTargetHudHandler() {
    }

    public static void register() {
        RoleNameHudApi.registerAdditionalLine(
                NoellesAppearanceSupport.id("role_name/arsonist_target"),
                RoleNameHudApi.DEFAULT_PRIORITY,
                context -> {
                    ClientPlayerEntity player = context.player();
                    PlayerEntity target = context.targetPlayer();
                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
                    if (target == null
                            || !gameWorld.isRole(player, NoellesRoleRegistry.ARSONIST)
                            || GameFunctions.isPlayerSpectatingOrCreative(player)) {
                        return null;
                    }

                    boolean doused = DousedPlayerComponent.KEY.get(target).isDoused();
                    Text text = Text.translatable(doused
                            ? "hud.noellesroles.arsonist.doused.true"
                            : "hud.noellesroles.arsonist.doused.false");
                    return RoleNameHudApi.AdditionalLine.below(text,
                            doused ? NoellesRoleRegistry.ARSONIST.color() : 0xAAAAAA,
                            1.0F);
                }
        );
    }
}
