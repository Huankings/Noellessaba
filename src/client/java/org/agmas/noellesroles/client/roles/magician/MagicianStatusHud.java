package org.agmas.noellesroles.client.roles.magician;

import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.magician.MagicianPlayerComponent;
import org.agmas.noellesroles.roles.magician.MagicianStage;

import java.util.ArrayList;
import java.util.List;

/**
 * 魔术师右下角 HUD。
 */
public final class MagicianStatusHud {
    private MagicianStatusHud() {
    }

    public static void register() {
        NoellesHudSupport.registerAliveRole("roles/magician/status", NoellesRoleRegistry.MAGICIAN, context -> {
            MagicianPlayerComponent magician = MagicianPlayerComponent.KEY.get(context.player());
            AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(context.player());

            List<Text> lines = new ArrayList<>();
            lines.add(Text.translatable("hud.noellesroles.magician.selected_target", Text.literal(magician.getSelectedTargetName())));

            if (magician.stage == MagicianStage.RECORDING) {
                lines.add(Text.translatable("hud.noellesroles.magician.recording", Math.max(0, (magician.stageTicksRemaining + 19) / 20)));
                lines.add(Text.translatable("hud.noellesroles.magician.stop_recording", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText()));
            } else if (magician.stage == MagicianStage.READY_PLAYBACK) {
                lines.add(Text.translatable("hud.noellesroles.magician.start_playback", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText()));
            } else if (magician.stage == MagicianStage.PLAYING) {
                lines.add(Text.translatable("hud.noellesroles.magician.playing", Math.max(0, (magician.stageTicksRemaining + 19) / 20)));
                lines.add(Text.translatable("hud.noellesroles.magician.stop_playback", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText()));
            } else if (ability.cooldown > 0) {
                lines.add(Text.translatable("hud.noellesroles.magician.cooldown", Math.max(0, (ability.cooldown + 19) / 20)));
            } else {
                lines.add(Text.translatable("hud.noellesroles.magician.start_recording", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText()));
            }

            NoellesHudSupport.drawBottomRightLines(context, lines, NoellesRoleRegistry.MAGICIAN.color());
        });
    }
}
