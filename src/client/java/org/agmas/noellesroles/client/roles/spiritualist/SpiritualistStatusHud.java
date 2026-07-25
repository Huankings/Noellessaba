package org.agmas.noellesroles.client.roles.spiritualist;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistConstants;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistPlayerComponent;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistTargeting;

import java.util.ArrayList;
import java.util.List;

/**
 * 灵术师右下角 HUD。
 */
public final class SpiritualistStatusHud {
    private SpiritualistStatusHud() {
    }

    public static void register() {
        NoellesHudSupport.registerAliveRole("roles/spiritualist/status", NoellesRoleRegistry.SPIRITUALIST, context -> {
            SpiritualistPlayerComponent spiritualist = SpiritualistPlayerComponent.KEY.get(context.player());
            AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(context.player());
            List<Text> lines = new ArrayList<>();

            lines.add(Text.translatable(
                    "hud.noellesroles.spiritualist.state",
                    Text.translatable(getStateTranslationKey(spiritualist))
            ));

            if (ability.cooldown > 0) {
                lines.add(Text.translatable("tip.noellesroles.cooldown", Math.max(1, ability.cooldown / 20)));
            } else if (spiritualist.isProjecting()) {
                lines.add(Text.translatable(
                        "hud.noellesroles.spiritualist.end_projection",
                        NoellesrolesClient.abilityBind.getBoundKeyLocalizedText()
                ));
            } else if (spiritualist.isPossessing()) {
                lines.add(Text.translatable(
                        "hud.noellesroles.spiritualist.end_possession",
                        NoellesrolesClient.abilityBind.getBoundKeyLocalizedText()
                ));
            } else if (isPossessionAim(context.player())) {
                lines.add(Text.translatable(
                        "hud.noellesroles.spiritualist.start_possession",
                        NoellesrolesClient.abilityBind.getBoundKeyLocalizedText()
                ));
            } else {
                lines.add(Text.translatable(
                        "hud.noellesroles.spiritualist.start_projection",
                        NoellesrolesClient.abilityBind.getBoundKeyLocalizedText()
                ));
            }

            int y = context.height();
            for (int index = lines.size() - 1; index >= 0; index--) {
                MutableText line = lines.get(index).copy().withColor(SpiritualistConstants.ROLE_COLOR);
                y -= context.textRenderer().fontHeight;
                context.drawContext().drawTextWithShadow(
                        context.textRenderer(),
                        line,
                        context.width() - context.textRenderer().getWidth(line),
                        y,
                        SpiritualistConstants.ROLE_COLOR
                );
            }
        });
    }

    private static boolean isPossessionAim(PlayerEntity player) {
        return SpiritualistTargeting.isPossessionAim(player);
    }

    private static String getStateTranslationKey(SpiritualistPlayerComponent component) {
        if (component.isProjecting()) {
            return "hud.noellesroles.spiritualist.state.projecting";
        }
        if (component.isPossessing()) {
            return "hud.noellesroles.spiritualist.state.possessing";
        }
        return "hud.noellesroles.spiritualist.state.normal";
    }
}
