package org.agmas.noellesroles.client.roles.thief;

import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.client.input.NoellesClientKeybindings;
import org.agmas.noellesroles.roles.thief.ThiefAllayState;
import org.agmas.noellesroles.roles.thief.ThiefConstants;
import org.agmas.noellesroles.roles.thief.ThiefPlayerComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * 小偷右下角偷取冷却 HUD。
 */
public final class ThiefStatusHud {
    private ThiefStatusHud() {
    }

    public static void register() {
        NoellesHudSupport.registerAliveRole("roles/thief/status", NoellesRoleRegistry.THIEF, context -> {
            if (context.debugHudVisible()) {
                return;
            }

            AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(context.player());
            Text keyName = context.client().options.useKey.getBoundKeyLocalizedText();
            Text displayText = ability.cooldown > 0
                    ? Text.translatable("hud.noellesroles.thief.cooldown", Math.max(0, (ability.cooldown + 19) / 20), keyName)
                    : Text.translatable("hud.noellesroles.thief.ready", keyName);

            /*
             * 行顺序固定为：悦灵状态（1号到3号）→ G键主动清理提示 → 原空手偷窃提示。
             * 先把实际需要显示的行放进列表，再统一从底部向上布局；某个号数消失时不会留下空行。
             */
            List<Text> lines = new ArrayList<>();
            ThiefPlayerComponent thief = ThiefPlayerComponent.KEY.get(context.player());
            for (int number = 1; number <= ThiefConstants.ALLAY_MAX_COUNT; number++) {
                ThiefAllayState state = thief.getState(number);
                if (state != ThiefAllayState.NONE) {
                    lines.add(Text.translatable(
                            "hud.noellesroles.thief.allay.status",
                            number,
                            Text.translatable(statusKey(state))
                    ));
                }
            }
            if (thief.hasLivingAllayState() && NoellesClientKeybindings.getAbilityBind() != null) {
                lines.add(Text.translatable(
                        "hud.noellesroles.thief.allay.kill_prompt",
                        NoellesClientKeybindings.getAbilityBind().getBoundKeyLocalizedText()
                ));
            }
            lines.add(displayText);

            int lineHeight = context.textRenderer().fontHeight;
            int y = context.height() - ThiefConstants.HUD_BOTTOM_MARGIN - lineHeight * (lines.size() - 1);
            for (Text line : lines) {
                int x = context.width() - context.textRenderer().getWidth(line) - ThiefConstants.HUD_RIGHT_MARGIN;
                context.drawContext().drawTextWithShadow(
                        context.textRenderer(), line, x, y, NoellesRoleRegistry.THIEF.color()
                );
                y += lineHeight;
            }
        });
    }

    private static String statusKey(ThiefAllayState state) {
        return switch (state) {
            case SEEKING_TARGET -> "hud.noellesroles.thief.allay.state.seeking";
            case RETURNING_SUCCESS -> "hud.noellesroles.thief.allay.state.return_success";
            case RETURNING_FAILURE -> "hud.noellesroles.thief.allay.state.return_failure";
            case DELIVERY_SUCCESS -> "hud.noellesroles.thief.allay.state.delivery_success";
            case DEAD -> "hud.noellesroles.thief.allay.state.dead";
            case NONE -> "hud.noellesroles.thief.allay.state.seeking";
        };
    }
}
