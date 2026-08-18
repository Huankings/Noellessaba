package org.agmas.noellesroles.client.roles.shadow_jester;

import net.minecraft.text.Text;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.shadow_jester.ShadowJesterComponent;
import org.agmas.noellesroles.roles.shadow_jester.ShadowJesterConstants;
import org.agmas.noellesroles.roles.shadow_jester.ShadowJesterPhase;

/**
 * 影子小丑右下角阶段 HUD。
 */
public final class ShadowJesterStatusHud {
    private ShadowJesterStatusHud() {
    }

    public static void register() {
        NoellesHudSupport.registerAliveRole("roles/shadow_jester/status", NoellesRoleRegistry.SHADOW_JESTER, context -> {
            ShadowJesterComponent component = ShadowJesterComponent.KEY.get(context.player().getWorld());
            if (!component.contains(context.player().getUuid())) {
                return;
            }

            Text line = switch (component.getPhase(context.player().getUuid())) {
                case TASKS -> Text.translatable(
                        "hud.noellesroles.shadow_jester.tasks",
                        component.getRemainingTasks(context.player().getUuid())
                );
                case CHOICE -> choiceLine(component, context.player().getUuid());
                case VOW_BOUND -> Text.translatable("hud.noellesroles.shadow_jester.vow_bound");
                case CURTAIN_CALL -> Text.translatable("hud.noellesroles.shadow_jester.curtain_call");
            };
            NoellesHudSupport.drawBottomRightLine(context, line, ShadowJesterConstants.ROLE_COLOR);
        });
    }

    private static Text choiceLine(ShadowJesterComponent component, java.util.UUID playerUuid) {
        Text abilityKey = NoellesrolesClient.abilityBind.getBoundKeyLocalizedText();
        if (component.isRequestFrom(playerUuid)) {
            /*
             * 申请方看见剩余有效期。向上取整能避免最后 1 tick 直接显示 0 秒，
             * 让玩家更容易理解“申请还没完全失效”。
             */
            int seconds = Math.max(1, (component.getRequestTicksLeft() + 19) / 20);
            return Text.translatable("hud.noellesroles.shadow_jester.request_sent", seconds);
        }
        if (component.isRequestTo(playerUuid)) {
            return Text.translatable("hud.noellesroles.shadow_jester.request_received", abilityKey);
        }
        return Text.translatable("hud.noellesroles.shadow_jester.choice", abilityKey);
    }
}
