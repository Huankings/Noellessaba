package org.agmas.noellesroles.client.appearance.roles.hacker;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;
import org.agmas.noellesroles.roles.hacker.HackerComponent;
import org.agmas.noellesroles.roles.hacker.HackerConstants;
import org.agmas.noellesroles.roles.hacker.HackerSafeTimeComponent;
import org.agmas.noellesroles.roles.hacker.HackerTargeting;

import java.awt.Color;

/**
 * 黑客准心破解 HUD。
 */
public final class HackerTargetHudHandler {
    private HackerTargetHudHandler() {
    }

    public static void register() {
        RoleNameHudApi.registerAdditionalLine(
                NoellesAppearanceSupport.id("role_name/hacker_target"),
                40,
                context -> {
                    ClientPlayerEntity player = context.player();
                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
                    if (!gameWorld.isRole(player, NoellesRoleRegistry.HACKER) || !GameFunctions.isPlayerAliveAndSurvival(player)) {
                        return null;
                    }

                    PlayerEntity target = aliveTarget(context.targetPlayer());
                    if (target == null
                            || gameWorld.getRole(target) == null
                            || HackerTargeting.countsAsFilteredKillerCohort(gameWorld, target)) {
                        return null;
                    }

                    Text targetInfo;
                    if (HackerSafeTimeComponent.KEY.get(player.getWorld()).isSafe()) {
                        targetInfo = Text.translatable("hud.noellesroles.hacker.target_safe").withColor(NoellesRoleRegistry.HACKER.color());
                    } else {
                        HackerComponent targetHack = HackerComponent.KEY.get(target);
                        if (targetHack.hackingTime < HackerConstants.HACKING_TIME_TICKS) {
                            targetInfo = Text.translatable("hud.noellesroles.hacker.target")
                                    .styled(style -> style.withColor(NoellesRoleRegistry.HACKER.color()))
                                    .append(Text.literal(" [ " + (int) (((float) targetHack.hackingTime / HackerConstants.HACKING_TIME_TICKS) * 100) + "% ]")
                                            .styled(style -> style.withColor(Color.GREEN.getRGB())));
                        } else {
                            targetInfo = Text.translatable("hud.noellesroles.hacker.target_hacked")
                                    .styled(style -> style.withColor(Color.GREEN.getRGB()));
                        }
                    }

                    return RoleNameHudApi.AdditionalLine.below(
                            targetInfo,
                            NoellesRoleRegistry.HACKER.color(),
                            1.0F
                    );
                }
        );
    }

    private static PlayerEntity aliveTarget(PlayerEntity target) {
        return target != null && GameFunctions.isPlayerAliveAndSurvival(target) ? target : null;
    }

}
