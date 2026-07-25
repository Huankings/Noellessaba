package org.agmas.noellesroles.client.roles.convener;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.convener.ConvenerConstants;
import org.agmas.noellesroles.roles.convener.ConvenerDisguiseComponent;
import org.agmas.noellesroles.roles.convener.ConvenerPlayerComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 召集者右下角状态 HUD。
 */
public final class ConvenerStatusHud {
    private ConvenerStatusHud() {
    }

    public static void register() {
        NoellesHudSupport.registerAliveRole("roles/convener/status", NoellesRoleRegistry.CONVENER, context -> {
            ConvenerPlayerComponent convener = ConvenerPlayerComponent.KEY.get(context.player());
            ConvenerDisguiseComponent disguise = ConvenerDisguiseComponent.KEY.get(context.player());

            List<Text> lines = new ArrayList<>();
            if (ConvenerConstants.COUNTER_SHIELD_ENABLED) {
                lines.add(Text.translatable("hud.noellesroles.convener.counter_shield_layers", convener.getCounterShieldLayers()));
            }

            lines.add(convener.hasUnlockedMorphs()
                    ? Text.translatable("hud.noellesroles.convener.current_disguise", resolveHudDisguiseName(context.player(), disguise.getDisguiseUuid()))
                    : Text.translatable("hud.noellesroles.convener.locked"));
            lines.add(Text.translatable("hud.noellesroles.convener.progress", convener.getSummonCount(), convener.getRequiredSummons()));

            if (ConvenerConstants.COUNTER_SHIELD_ENABLED) {
                lines.add(Text.translatable("hud.noellesroles.convener.tasks_to_next_shield", convener.getTasksRemainingForNextShield()));
            }

            NoellesHudSupport.drawBottomRightLines(context, lines, NoellesRoleRegistry.CONVENER.color());
        });
    }

    private static Text resolveHudDisguiseName(ClientPlayerEntity player, UUID disguiseUuid) {
        Text disguiseName = ConvenerDisguiseResolver.resolveDisguiseName(player, disguiseUuid);
        return disguiseName != null ? disguiseName : Text.translatable("hud.noellesroles.convener.waiting");
    }
}
