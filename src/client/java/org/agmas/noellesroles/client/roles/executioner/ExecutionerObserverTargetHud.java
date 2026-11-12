package org.agmas.noellesroles.client.roles.executioner;

import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.executioner.ExecutionerPlayerComponent;

import java.util.UUID;

/** 普通非存活观察者准心查看仇杀客时显示其当前仇杀目标。 */
public final class ExecutionerObserverTargetHud {
    private ExecutionerObserverTargetHud() {
    }

    public static void register() {
        RoleNameHudApi.registerAdditionalLine(
                NoellesRolesCore.id("role_name/executioner/observer_target"),
                100,
                context -> {
                    if (!GameFunctions.isPlayerSpectatingOrCreative(context.player())
                            || context.targetPlayer() == null
                            || context.nametagAlpha() <= 0.05F) {
                        return null;
                    }

                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(context.player().getWorld());
                    if (!gameWorld.isRole(context.targetPlayer(), NoellesRoleRegistry.EXECUTIONER)) {
                        return null;
                    }

                    ExecutionerPlayerComponent executioner = ExecutionerPlayerComponent.KEY.get(context.targetPlayer());
                    UUID targetUuid = executioner.target;
                    if (targetUuid == null || targetUuid.equals(context.targetPlayer().getUuid())
                            || context.player().networkHandler == null) {
                        return null;
                    }
                    PlayerListEntry entry = context.player().networkHandler.getPlayerListEntry(targetUuid);
                    if (entry == null) {
                        return null;
                    }

                    Text text = Text.translatable(
                            "hud.noellesroles.executioner.observer_target",
                            entry.getProfile().getName()
                    );
                    return RoleNameHudApi.AdditionalLine.below(text, Colors.RED, 0.9F);
                }
        );
    }
}
